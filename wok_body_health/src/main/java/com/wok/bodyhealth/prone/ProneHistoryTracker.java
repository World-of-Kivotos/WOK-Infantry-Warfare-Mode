package com.wok.bodyhealth.prone;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.LogicalSide;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Per-player snapshot history for prone rewinds (spec 10). Recorded at PlayerTickEvent END with
 * LOWEST priority, after TaCZ, SBW and TAA have recorded theirs, so history index 0 is "the end
 * of the previous server tick" for all of them when bullets resolve in the next levels phase.
 * Every map here is touched on the server thread only; the ACTIVE set is published through a
 * volatile reference for the cheap checks in mixin hooks.
 */
public final class ProneHistoryTracker {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Horizontal movement per tick that counts as crawling. */
    private static final double CRAWL_MOVE_THRESHOLD = 0.002D;
    private static final float CRAWL_WEIGHT_STEP = 1.0F / 6.0F;

    private static final Map<ServerPlayer, TrackerState> STATES = new WeakHashMap<>();
    private static volatile Set<ServerPlayer> active = Set.of();
    private static volatile boolean hasActive;
    private static boolean recordFailureLogged;

    private static final class TrackerState {
        final ProneHistory history = new ProneHistory();
        Vec3 prev1;
        Vec3 prev2;
        float crawlWeight;
        float bodyYaw = Float.NaN;
        long activeUntil = Long.MIN_VALUE;
        ResourceKey<Level> dim;

        /** Stricter than TaCZ on purpose: positions from another dimension are meaningless here. */
        void resetMotion() {
            history.clear();
            prev1 = null;
            prev2 = null;
            bodyYaw = Float.NaN;
        }
    }

    static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER
                || !(e.player instanceof ServerPlayer p)) {
            return;
        }
        try {
            record(p);
        } catch (Throwable throwable) {
            // Never fail the player tick: forget this player's snapshots and start afresh next tick.
            STATES.remove(p);
            if (!recordFailureLogged) {
                recordFailureLogged = true;
                LOGGER.error("Recording a prone snapshot failed; further failures are not logged.", throwable);
            }
        }
    }

    private static void record(ServerPlayer p) {
        if (p.isSpectator()) {
            // TaCZ and SBW drop a spectator's history as well.
            STATES.remove(p);
            return;
        }

        TrackerState state = STATES.computeIfAbsent(p, key -> new TrackerState());
        ResourceKey<Level> dim = p.level().dimension();
        if (state.dim != dim) {
            state.resetMotion();
            state.dim = dim;
        }

        Vec3 pos = p.position();
        double dx = state.prev1 == null ? 0.0D : pos.x - state.prev1.x;
        double dz = state.prev1 == null ? 0.0D : pos.z - state.prev1.z;
        // Simulated for standing players too: observers see the body turn continuously into prone.
        // Read the attackAnim field like LivingEntity.tick does; getAttackAnim(1) is still 1.0 on
        // the tick a swing ends and would pull the body one tick too long.
        state.bodyYaw = Float.isNaN(state.bodyYaw)
                ? p.getYRot()
                : BodyYawSim.step(state.bodyYaw, p.getYRot(), dx, dz, p.attackAnim > 0.0F);

        Vec3 velocity = ProneRewind.velocity2(pos, state.prev1, state.prev2);
        state.prev2 = state.prev1;
        state.prev1 = pos;

        boolean moving = dx * dx + dz * dz > CRAWL_MOVE_THRESHOLD * CRAWL_MOVE_THRESHOLD;
        state.crawlWeight = Math.max(0.0F, Math.min(1.0F,
                state.crawlWeight + (moving ? CRAWL_WEIGHT_STEP : -CRAWL_WEIGHT_STEP)));

        long gameTime = p.level().getGameTime();
        ProneSample sample = ProneSampler.capture(
                p, gameTime, velocity, state.crawlWeight, state.bodyYaw, state.history.latest());
        state.history.push(sample);
        if (sample.mode() != ProneMode.NONE) {
            state.activeUntil = gameTime + ProneHistory.CAPACITY + 2;
        }
    }

    /** START: rebuilds the ACTIVE set and closes any leaked broad-phase scope. */
    static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) {
            return;
        }
        ProneScope.reset();
        MinecraftServer server = e.getServer();
        if (server == null) {
            return;
        }
        long now = server.overworld().getGameTime();
        Set<ServerPlayer> next = null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TrackerState state = STATES.get(player);
            if (player.getPose() == Pose.SWIMMING || (state != null && state.activeUntil >= now)) {
                if (next == null) {
                    next = Collections.newSetFromMap(new IdentityHashMap<>());
                }
                next.add(player);
            }
        }
        active = next == null ? Set.of() : Collections.unmodifiableSet(next);
        hasActive = next != null;
    }

    static void onServerStarting(ServerStartingEvent e) {
        // Parse the 480 KB table now instead of on the first shot at a prone player.
        if (ProneSegmentTables.shared() == null) {
            LOGGER.warn("The TAA prone segment table could not be loaded; TAA prone players keep the "
                    + "original hitbox, vanilla crawling still uses segments.");
        }
        ProneMixinStatus.logSummary(ProneHitService.taczPresent(), ProneHitService.sbwPresent());
    }

    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player);
        }
    }

    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) {
            TrackerState state = STATES.get(player);
            if (state != null) {
                state.resetMotion();
                state.dim = e.getTo();
            }
        }
    }

    static void onClone(PlayerEvent.Clone e) {
        if (e.getOriginal() instanceof ServerPlayer original) {
            STATES.remove(original);
        }
        if (e.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player);
        }
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player);
        }
    }

    /** Integrated servers are reused between saves; drop everything that could leak across. */
    static void onServerStopped(ServerStoppedEvent e) {
        STATES.clear();
        active = Set.of();
        hasActive = false;
        ProneHitLedger.SERVER.clearAll();
        ProneHitMemory.SERVER.clearAll();
        ProneScope.reset();
        ProneHitboxDebug.reset();
        ProneHitService.resetStats();
    }

    /** The player's snapshots, or null before the first tick was recorded. */
    public static ProneHistory history(ServerPlayer p) {
        TrackerState state = STATES.get(p);
        return state == null ? null : state.history;
    }

    public static boolean hasActiveTargets() {
        return hasActive;
    }

    /** Prone now, or prone recently enough that a rewind can land on a prone snapshot. */
    public static boolean isCandidate(ServerPlayer p) {
        return p.getPose() == Pose.SWIMMING || active.contains(p);
    }

    public static Iterable<ServerPlayer> activeTargets(ServerLevel level) {
        Set<ServerPlayer> snapshot = active;
        if (snapshot.isEmpty()) {
            return List.of();
        }
        List<ServerPlayer> out = new ArrayList<>(snapshot.size());
        for (ServerPlayer player : snapshot) {
            if (!player.isRemoved() && player.level() == level) {
                out.add(player);
            }
        }
        return out;
    }

    /**
     * Snapshot of the player right now, at hit time. Velocity is {@code pos - (xOld, yOld, zOld)}
     * like TaCZ and SBW use without a rewind; crawl weight and body yaw come from the tracker, and
     * the heading and aim filters step once more from the newest history entry.
     */
    public static ProneSample liveSample(ServerPlayer p, long gameTime) {
        TrackerState state = STATES.get(p);
        float crawlWeight = state == null ? 0.0F : state.crawlWeight;
        float bodyYaw = state == null || Float.isNaN(state.bodyYaw) ? p.getYRot() : state.bodyYaw;
        ProneSample prev = state == null ? null : state.history.latest();
        Vec3 velocity = new Vec3(p.getX() - p.xOld, p.getY() - p.yOld, p.getZ() - p.zOld);
        return ProneSampler.capture(p, gameTime, velocity, crawlWeight, bodyYaw, prev);
    }

    private ProneHistoryTracker() {
    }
}
