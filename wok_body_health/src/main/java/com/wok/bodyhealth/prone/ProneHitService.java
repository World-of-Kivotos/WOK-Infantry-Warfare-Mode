package com.wok.bodyhealth.prone;

import com.mojang.logging.LogUtils;
import com.wok.bodyhealth.compat.sbw.SbwProneKind;
import com.wok.bodyhealth.config.BodyHealthConfig;
import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Predicate;

/**
 * Glue between the gun-mod hooks and the pure prone layer: builds the snapshot a hit is tested
 * against, runs the segment test with a real block-occlusion check, and hands the hit part to the
 * damage handlers through {@link ProneHitLedger}. References no optional mod, so the damage
 * handlers may call {@link #findPart} whether or not TaCZ/SBW are installed.
 */
public final class ProneHitService {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** After this many failures of one consumer its prone handling is switched off. */
    private static final int FAILURE_LIMIT = 20;
    private static final int DEBUG_LOGS_PER_SECOND = 40;
    private static final double TACZ_TWEAKS_FORWARD_SHIFT = -0.4D;
    private static final String TACZ_KIND = "com.wok.bodyhealth.compat.tacz.TaczProneKind";

    public enum Outcome { HIT, MISS, NOT_APPLICABLE, FAILURE }

    /** Event counters shown by {@code /wokbodyhealth prone_hitbox stats}. */
    public enum Counter {
        OCCLUDED_SEGMENTS,
        ALREADY_HIT,
        FORCED_MISS_REQUESTED,
        FORCED_MISS_CONSUMED,
        SCOPED_QUERIES,
        CANDIDATES_ADDED
    }

    private static volatile List<ProneProjectileKind> kinds = List.of();
    private static volatile Predicate<ItemStack> gunProbe = stack -> false;
    private static volatile boolean bootstrapped;
    private static volatile boolean taczPresent;
    private static volatile boolean sbwPresent;
    private static volatile Boolean taczTweaksLoaded;
    private static volatile Boolean debugOverride;

    private static final AtomicIntegerArray FAILURES = new AtomicIntegerArray(ProneConsumer.values().length);
    private static final AtomicLongArray OUTCOMES =
            new AtomicLongArray(ProneConsumer.values().length * Outcome.values().length);
    private static final AtomicLongArray COUNTERS = new AtomicLongArray(Counter.values().length);

    private static final Object LOG_LOCK = new Object();
    private static long logWindowStart;
    private static int logWindowCount;

    /** Registers the tracker, debug command and projectile kinds. Called once by the mod constructor. */
    public static void bootstrap(IEventBus forgeBus, boolean tacz, boolean sbw) {
        if (bootstrapped) {
            return;
        }
        taczPresent = tacz;
        sbwPresent = sbw;

        // LOWEST: after TaCZ, SBW and TAA have updated their own per-tick state.
        forgeBus.addListener(EventPriority.LOWEST, ProneHistoryTracker::onPlayerTick);
        forgeBus.addListener(ProneHistoryTracker::onServerTick);
        forgeBus.addListener(ProneHistoryTracker::onServerStarting);
        forgeBus.addListener(ProneHistoryTracker::onLoggedOut);
        forgeBus.addListener(ProneHistoryTracker::onChangedDimension);
        forgeBus.addListener(ProneHistoryTracker::onClone);
        forgeBus.addListener(ProneHistoryTracker::onRespawn);
        forgeBus.addListener(ProneHistoryTracker::onServerStopped);
        forgeBus.addListener(ProneHitboxDebug::onRegisterCommands);
        forgeBus.addListener(ProneHitboxDebug::onServerTick);

        if (tacz) {
            try {
                // Same reflective entry as TaczCompat: no class here links against TaCZ.
                Class.forName(TACZ_KIND).getMethod("register").invoke(null);
            } catch (ReflectiveOperationException | LinkageError exception) {
                LOGGER.error("TaCZ is installed, but segmented prone hitboxes for TaCZ bullets could not "
                        + "start; TaCZ keeps its original prone hitbox.", exception);
            }
        }
        if (sbw) {
            try {
                SbwProneKind.register();
            } catch (LinkageError | RuntimeException exception) {
                LOGGER.error("Superb Warfare is installed, but segmented prone hitboxes for SBW bullets "
                        + "could not start; SBW keeps its original prone hitbox.", exception);
            }
        }
        bootstrapped = true;
    }

    /** Copy-on-write so the mixin hooks can read the list without locking. */
    public static synchronized void registerKind(ProneProjectileKind kind) {
        List<ProneProjectileKind> next = new ArrayList<>(kinds);
        next.add(kind);
        kinds = List.copyOf(next);
    }

    /** The registered real-bullet kind of {@code projectile}, or null (e.g. gsl's rangefinder marker). */
    public static ProneProjectileKind kindOf(Entity projectile) {
        for (ProneProjectileKind kind : kinds) {
            if (kind.matches(projectile)) {
                return kind;
            }
        }
        return null;
    }

    public static void setGunProbe(Predicate<ItemStack> probe) {
        gunProbe = probe;
    }

    /** Whether TAA would draw the item as a TaCZ gun; always false without TaCZ. */
    public static boolean isGun(ItemStack stack) {
        try {
            return stack != null && !stack.isEmpty() && gunProbe.test(stack);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    static boolean taczPresent() {
        return taczPresent;
    }

    static boolean sbwPresent() {
        return sbwPresent;
    }

    /** Current config as the pure layer sees it; an unloaded config counts as switched off. */
    public static ProneHitSettings settings() {
        try {
            Boolean override = debugOverride;
            return new ProneHitSettings(
                    BodyHealthConfig.ENABLE_SEGMENTED_PRONE_HITBOX.get(),
                    BodyHealthConfig.PRONE_HEAD_MARGIN.get(),
                    BodyHealthConfig.PRONE_TORSO_MARGIN.get(),
                    BodyHealthConfig.PRONE_LIMB_MARGIN.get(),
                    BodyHealthConfig.PRONE_TRANSITION_LAG_TICKS.get(),
                    vanillaForwardShift(BodyHealthConfig.PRONE_VANILLA_CRAWL_MODEL.get()),
                    BodyHealthConfig.PRONE_SBW_LEG_SEGMENTS_ARE_LEG_SHOTS.get(),
                    override != null ? override : BodyHealthConfig.PRONE_DEBUG_LOGGING.get());
        } catch (IllegalStateException notLoaded) {
            return ProneHitSettings.DISABLED;
        }
    }

    /** Temporary {@code debugLogging} override from the debug command; null restores the config. */
    static void setDebugOverride(Boolean enabled) {
        debugOverride = enabled;
    }

    /**
     * Segment test of one bullet ray against a prone player (spec 8.1). NOT_APPLICABLE hands the
     * hit back to the gun mod's own hitbox. Throws on unexpected errors; the hooks catch them.
     */
    public static ProneHitResult test(ProneConsumer c, ServerPlayer target, Entity projectile,
                                      Entity shooter, Vec3 start, Vec3 end, RewindPolicy policy) {
        ProneHitSettings cfg = settings();
        if (!cfg.enabled() || isDisabled(c)) {
            return outcome(c, ProneHitResult.NOT_APPLICABLE);
        }
        if (!isFinite(start) || !isFinite(end) || start.equals(end)) {
            return outcome(c, ProneHitResult.NOT_APPLICABLE);
        }

        Placement placed = place(target, shooter, policy, cfg);
        if (placed.segments() == null) {
            ProneHitResult result = outcome(c, ProneHitResult.NOT_APPLICABLE);
            debugLog(cfg, c, target, shooter, placed, result);
            return result;
        }

        ProneSample s = placed.choice().sample();
        List<ProneSegmentClip.SegmentHit> hits = ProneSegmentClip.clipAll(
                placed.segments(), start, end, placed.choice().velocity(), policy.shiftFactor());
        ProneSegmentClip.Selection selection = ProneSegmentClip.select(
                hits, ProneGeometry.core(s.x(), s.y(), s.z()), occlusion(target.level(), projectile));
        if (selection.occluded() > 0) {
            COUNTERS.addAndGet(Counter.OCCLUDED_SEGMENTS.ordinal(), selection.occluded());
        }
        ProneHitResult result = outcome(c, ProneSegmentClip.toResult(selection));
        debugLog(cfg, c, target, shooter, placed, result);
        return result;
    }

    /** MISS for a projectile that already hit this target (piercing rounds, repeated sub-rays). */
    public static ProneHitResult alreadyHit(ProneConsumer c, ServerPlayer target, Entity shooter) {
        count(Counter.ALREADY_HIT);
        ProneHitResult result = outcome(c, ProneHitResult.miss("already_hit"));
        ProneHitSettings cfg = settings();
        if (cfg.debugLog() && allowDebugLog()) {
            LOGGER.info("[prone] {} target={} shooter={} result=MISS note=already_hit",
                    c, target.getGameProfile().getName(), describe(shooter));
        }
        return result;
    }

    /**
     * World box containing every point the bullet can hit the target at, computed from the same
     * snapshot and velocity {@link #test} will use; null when the target is not prone there.
     */
    public static AABB reachBox(ServerPlayer target, Entity shooter, RewindPolicy policy) {
        ProneHitSettings cfg = settings();
        if (!cfg.enabled()) {
            return null;
        }
        Placement placed = place(target, shooter, policy, cfg);
        return placed.segments() == null ? null
                : ProneSegmentClip.reach(placed.segments(), placed.choice().velocity(), policy.shiftFactor());
    }

    /** Part a prone segment test recorded for this bullet this tick, or null. */
    public static BodyPart findPart(Player target, Entity direct, long gameTime) {
        if (!bootstrapped || target == null || direct == null || target.level().isClientSide()) {
            return null;
        }
        return ProneHitLedger.SERVER.find(target.getUUID(), direct.getUUID(), gameTime);
    }

    /** First failure prints a stack trace; the twentieth switches the consumer off. */
    public static void reportFailure(ProneConsumer c, Throwable t) {
        OUTCOMES.incrementAndGet(index(c, Outcome.FAILURE));
        int failures = FAILURES.incrementAndGet(c.ordinal());
        if (failures == 1) {
            LOGGER.error("Segmented prone hit test for {} failed; this shot uses the original hitbox.", c, t);
        } else if (failures == FAILURE_LIMIT) {
            LOGGER.error("Segmented prone hit test for {} failed {} times and is now disabled until restart; "
                    + "last error: {}", c, failures, t.toString());
        }
    }

    static boolean isDisabled(ProneConsumer c) {
        return FAILURES.get(c.ordinal()) >= FAILURE_LIMIT;
    }

    public static void count(Counter counter) {
        COUNTERS.incrementAndGet(counter.ordinal());
    }

    static void count(Counter counter, int amount) {
        COUNTERS.addAndGet(counter.ordinal(), amount);
    }

    static long counter(Counter counter) {
        return COUNTERS.get(counter.ordinal());
    }

    static long outcomes(ProneConsumer c, Outcome outcome) {
        return OUTCOMES.get(index(c, outcome));
    }

    static void resetStats() {
        for (int i = 0; i < OUTCOMES.length(); i++) {
            OUTCOMES.set(i, 0L);
        }
        for (int i = 0; i < COUNTERS.length(); i++) {
            COUNTERS.set(i, 0L);
        }
    }

    /** The rewind choice and the placed segments for it; segments are null when not prone there. */
    record Placement(ProneRewind.Choice choice, ProneLayouts.BodyPose pose, WorldObb[] segments) {
    }

    static Placement place(ServerPlayer target, Entity shooter, RewindPolicy policy, ProneHitSettings cfg) {
        long now = target.level().getGameTime();
        ProneSample live = ProneHistoryTracker.liveSample(target, now);
        int latency = shooter instanceof ServerPlayer player ? player.latency : -1;
        ProneRewind.Choice choice = ProneRewind.select(ProneHistoryTracker.history(target),
                live, live.velocity(), latency, true, policy);
        ProneSample s = choice.sample();
        ProneLayouts.BodyPose pose = ProneLayouts.resolve(s, cfg, ProneSegmentTables.shared());
        if (pose == null) {
            return new Placement(choice, null, null);
        }
        return new Placement(choice, pose, ProneGeometry.place(pose, s.x(), s.y(), s.z(), cfg));
    }

    /** A block between a segment hit and the body core hides that segment. */
    private static OcclusionTest occlusion(Level level, Entity projectile) {
        return (from, to) -> level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, projectile)).getType() == HitResult.Type.BLOCK;
    }

    private static double vanillaForwardShift(BodyHealthConfig.VanillaCrawlModel model) {
        return switch (model) {
            case VANILLA -> 0.0D;
            case TACZ_TWEAKS -> TACZ_TWEAKS_FORWARD_SHIFT;
            case AUTO -> taczTweaksLoaded() ? TACZ_TWEAKS_FORWARD_SHIFT : 0.0D;
        };
    }

    private static boolean taczTweaksLoaded() {
        Boolean cached = taczTweaksLoaded;
        if (cached == null) {
            ModList mods = ModList.get();
            if (mods == null) {
                return false;
            }
            cached = mods.isLoaded("tacztweaks");
            taczTweaksLoaded = cached;
        }
        return cached;
    }

    private static ProneHitResult outcome(ProneConsumer c, ProneHitResult result) {
        Outcome outcome = switch (result.kind()) {
            case HIT -> Outcome.HIT;
            case MISS -> Outcome.MISS;
            case NOT_APPLICABLE -> Outcome.NOT_APPLICABLE;
        };
        OUTCOMES.incrementAndGet(index(c, outcome));
        return result;
    }

    private static int index(ProneConsumer c, Outcome outcome) {
        return c.ordinal() * Outcome.values().length + outcome.ordinal();
    }

    private static boolean isFinite(Vec3 v) {
        return v != null && Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private static void debugLog(ProneHitSettings cfg, ProneConsumer c, ServerPlayer target, Entity shooter,
                                 Placement placed, ProneHitResult result) {
        if (!cfg.debugLog() || !allowDebugLog()) {
            return;
        }
        ProneSample s = placed.choice().sample();
        String yaw = placed.pose() == null ? "-" : format(placed.pose().yawDeg());
        String turn = s.mode() == ProneMode.TAA_PRONE
                ? "heading=" + format(s.taaHeading()) : "bodyYaw=" + format(s.bodyYaw());
        String progress = s.mode().isTaa() && s.mode() != ProneMode.TAA_ASSUMED && s.mode() != ProneMode.TAA_PRONE
                ? format(ProneLayouts.progress(s, cfg)) : "-";
        LOGGER.info("[prone] {} target={} shooter={} rewind={} mode={} t={} yaw={} {} result={} segment={} part={} "
                        + "point={} note={}",
                c, target.getGameProfile().getName(), describe(shooter), placed.choice().index(), s.mode(),
                progress, yaw, turn, result.kind(), result.segment(), result.part(),
                result.point() == null ? "-" : String.format(Locale.ROOT, "(%.3f, %.3f, %.3f)",
                        result.point().x, result.point().y, result.point().z),
                result.note());
    }

    private static String describe(Entity entity) {
        return entity == null ? "-" : entity.getName().getString();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /** At most {@link #DEBUG_LOGS_PER_SECOND} debug lines per second. */
    private static boolean allowDebugLog() {
        long now = System.nanoTime();
        synchronized (LOG_LOCK) {
            if (now - logWindowStart >= 1_000_000_000L) {
                logWindowStart = now;
                logWindowCount = 0;
            }
            return logWindowCount++ < DEBUG_LOGS_PER_SECOND;
        }
    }

    private ProneHitService() {
    }
}
