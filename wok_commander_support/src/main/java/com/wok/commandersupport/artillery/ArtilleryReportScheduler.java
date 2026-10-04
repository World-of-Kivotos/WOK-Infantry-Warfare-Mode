package com.wok.commandersupport.artillery;

import com.wok.commandersupport.WokCommanderSupportMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Purely cosmetic delayed gun reports of the howitzer barrages.
 *
 * <p>Later waves are fired while the core does not call the provider at all (the inbound delay)
 * or between two mission steps, so {@link ArtilleryBarrageProvider#accepted} queues the whole
 * timetable here once. Cues are grouped by call id, driven by the server tick on the same clock
 * as the support scheduler (overworld game time), never persisted, and cleared whenever a server
 * starts or stops. A failure is only logged and drops the rest of that call's reports; it never
 * reaches the mission.</p>
 *
 * <p>Cancellation: the provider cancels a call's reports from {@code abandon} and whenever its
 * mission ends early or completes. A mission the core cancels before step 0 gets no cleanup call;
 * its reports then stop once the lease runs out ({@link #LEASE_GRACE_TICKS} after the expected
 * step 0), so only the reports of waves fired during the inbound delay can still be heard.</p>
 *
 * <p>Registers itself on the Forge event bus through {@link Mod.EventBusSubscriber}; nothing else
 * may hook {@link #onServerTick} again, or every report would be advanced twice per tick.</p>
 */
@Mod.EventBusSubscriber(modid = WokCommanderSupportMod.MOD_ID)
public final class ArtilleryReportScheduler {
    /** Slack past the next expected mission step before a silent mission's reports are dropped. */
    static final long LEASE_GRACE_TICKS = 20L;
    /** Far above the eight missions the core runs at once. */
    static final int MAX_GROUPS = 32;

    private static final ReportQueue<ReportCue> QUEUE = new ReportQueue<>(MAX_GROUPS);

    private ArtilleryReportScheduler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        QUEUE.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        QUEUE.clear();
    }

    /** Plays every report due now. Never throws into the server tick. */
    public static void tick(MinecraftServer server) {
        if (server == null || QUEUE.isEmpty()) {
            return;
        }
        try {
            ReportQueue.Drain<ReportCue> drained = QUEUE.drain(gameTime(server));
            for (UUID expired : drained.expired()) {
                WokCommanderSupportMod.LOGGER.info(
                        "Howitzer barrage {} stopped stepping; dropped its remaining gun reports",
                        expired);
            }
            Set<UUID> failed = new HashSet<>();
            for (ReportQueue.Due<ReportCue> due : drained.due()) {
                if (!failed.contains(due.callId()) && !play(server, due)) {
                    failed.add(due.callId());
                    QUEUE.cancel(due.callId());
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            QUEUE.clear();
            WokCommanderSupportMod.LOGGER.error(
                    "Howitzer report scheduler failed; dropped every pending report", failure);
        }
    }

    /** Overworld game time: the clock the core support scheduler runs missions on. */
    static long gameTime(MinecraftServer server) {
        return Math.max(0L, server.overworld().getGameTime());
    }

    static long gameTime(ServerLevel level) {
        return gameTime(level.getServer());
    }

    /** Lease granted at acceptance: up to the expected step 0 plus the grace period. */
    static long acceptanceLease(long acceptedAt, long inboundTicks) {
        return saturatingAdd(saturatingAdd(acceptedAt, inboundTicks), LEASE_GRACE_TICKS);
    }

    /** Lease renewed by a completed step: up to the next expected step plus the grace period. */
    static long stepLease(long now) {
        return saturatingAdd(saturatingAdd(now, ArtilleryProfile.STEP_INTERVAL_TICKS),
                LEASE_GRACE_TICKS);
    }

    static void schedule(UUID callId, Collection<ReportQueue.Timed<ReportCue>> cues,
                         long leaseUntil) {
        QUEUE.schedule(callId, cues, leaseUntil);
    }

    static void renew(UUID callId, long leaseUntil) {
        QUEUE.renew(callId, leaseUntil);
    }

    /** Drops the call's remaining reports. Safe to call for a call with nothing pending. */
    static void cancel(UUID callId) {
        QUEUE.cancel(callId);
    }

    static int pending(UUID callId) {
        return QUEUE.pending(callId);
    }

    private static boolean play(MinecraftServer server, ReportQueue.Due<ReportCue> due) {
        ReportCue cue = due.cue();
        try {
            ServerLevel level = server.getLevel(cue.dimension());
            if (level == null) {
                WokCommanderSupportMod.LOGGER.info(
                        "Howitzer barrage {} lost its dimension {}; dropped its gun reports",
                        due.callId(), cue.dimension().location());
                return false;
            }
            ArtillerySounds.play(level, cue.x(), cue.y(), cue.z(),
                    ArtillerySounds.report(cue.caliber()),
                    ArtilleryProfile.REPORT_VOLUME, 1.0F);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            WokCommanderSupportMod.LOGGER.warn(
                    "Howitzer barrage {} could not play a gun report; dropped the rest",
                    due.callId(), failure);
            return false;
        }
    }

    private static long saturatingAdd(long left, long right) {
        long sum = left + right;
        return ((left ^ sum) & (right ^ sum)) < 0L
                ? (left < 0L ? Long.MIN_VALUE : Long.MAX_VALUE) : sum;
    }

    /** One gun report at the virtual battery of a call. */
    record ReportCue(ResourceKey<Level> dimension, double x, double y, double z,
                     ArtilleryProfile.Caliber caliber) {
        ReportCue {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(caliber, "caliber");
        }
    }
}
