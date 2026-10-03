package com.wok.bodyhealth.prone;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Widens a bullet's broad-phase entity query with prone players whose model reaches into it
 * (spec 4.3, 9). TaCZ and SBW pre-filter with the target's current 0.6 box plus 1.0, which a
 * transition pose or a rewound, swept limb can leave. Only the first query inside a
 * {@code find*OnPath} scope of that very bullet is touched; explosions and other lookups that
 * also pass the bullet as {@code except} stay unchanged.
 */
public final class ProneCandidateInjector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile boolean broken;

    /** Checks are ordered from cheapest to most expensive; Level.getEntities is a hot path. */
    public static void afterGetEntities(Level level, Entity except, AABB box,
                                        Predicate<? super Entity> pred, CallbackInfoReturnable<List<Entity>> cir) {
        if (broken || !ProneHistoryTracker.hasActiveTargets() || !ProneScope.consume(except)) {
            return;
        }
        try {
            if (!(level instanceof ServerLevel serverLevel) || !serverLevel.getServer().isSameThread()) {
                return;
            }
            ProneProjectileKind kind = ProneHitService.kindOf(except);
            // A consumer that falls back to its own hitbox keeps its own broad phase as well.
            if (kind == null || !ProneHitService.segmentedTestLive(kind.consumer())) {
                return;
            }
            RewindPolicy policy = kind.policy(except);
            if (policy == null) {
                return;
            }
            ProneHitService.count(ProneHitService.Counter.SCOPED_QUERIES);
            ProneHitSettings cfg = ProneHitService.settings();
            if (!cfg.enabled()) {
                return;
            }
            Entity shooter = kind.shooter(except);
            List<Entity> list = cir.getReturnValue();
            for (ServerPlayer player : ProneHistoryTracker.activeTargets(serverLevel)) {
                if (player == except || list.contains(player) || (pred != null && !pred.test(player))
                        || !ProneHitService.mayReach(player, shooter, policy, cfg, box)) {
                    continue;
                }
                AABB reach = ProneHitService.reachBox(player, shooter, policy, cfg);
                if (reach == null || !reach.intersects(box)) {
                    continue;
                }
                try {
                    list.add(player);
                } catch (UnsupportedOperationException immutable) {
                    list = new ArrayList<>(list);
                    list.add(player);
                    cir.setReturnValue(list);
                }
                ProneHitService.count(ProneHitService.Counter.CANDIDATES_ADDED);
            }
        } catch (Throwable throwable) {
            broken = true;
            LOGGER.error("Adding prone players to bullet broad-phase queries failed and is now disabled; "
                    + "bullets keep their original candidate search.", throwable);
        }
    }

    private ProneCandidateInjector() {
    }
}
