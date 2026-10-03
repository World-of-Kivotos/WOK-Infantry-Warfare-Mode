package com.wok.bodyhealth.compat.tacz;

import com.tacz.guns.entity.EntityKineticBullet;
import com.wok.bodyhealth.health.BodyPart;
import com.wok.bodyhealth.prone.ProneConsumer;
import com.wok.bodyhealth.prone.ProneDecision;
import com.wok.bodyhealth.prone.ProneHistoryTracker;
import com.wok.bodyhealth.prone.ProneHitLedger;
import com.wok.bodyhealth.prone.ProneHitMemory;
import com.wok.bodyhealth.prone.ProneHitResult;
import com.wok.bodyhealth.prone.ProneHitService;
import com.wok.bodyhealth.prone.ProneMixinStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Entry points of {@code TaczEntityUtilMixin} (spec 4.1). Only referenced from that mixin, which
 * is applied only with TaCZ installed; no static state depends on TaCZ.
 */
public final class ProneTaczHooks {
    /** Target whose next {@code AABB.clip} in getHitResult must miss; set by a MISS decision. */
    private static final ThreadLocal<Entity> FORCED_MISS = new ThreadLocal<>();
    /**
     * Set once the clip wrapper has actually run. MixinExtras applies {@code @WrapOperation} after
     * the config plugin's post-apply check, so the plugin can only prove the wrapper was prepared;
     * until it has been seen executing, a MISS returns null at HEAD rather than trusting it.
     */
    private static volatile boolean wrapSeen;

    /** HEAD of {@code EntityUtil.getHitResult}. PASS runs TaCZ's own test; MISS/HIT replace it. */
    public static ProneDecision onHead(Projectile projectile, Entity target, Vec3 start, Vec3 end) {
        FORCED_MISS.remove();
        boolean real = false;
        UUID targetId = null;
        UUID projectileId = null;
        long now = 0L;
        try {
            if (!(target instanceof ServerPlayer player)
                    || !(player.level() instanceof ServerLevel level)
                    || !level.getServer().isSameThread()
                    || !ProneHistoryTracker.isCandidate(player)) {
                return ProneDecision.PASS;
            }
            // gsl's rangefinder marker is one shared Projectile per level: test it, record nothing.
            real = TaczProneKind.isBullet(projectile);
            targetId = player.getUUID();
            projectileId = projectile.getUUID();
            now = level.getGameTime();
            if (real) {
                ProneHitLedger.SERVER.clear(targetId, projectileId, now);
            }

            Entity shooter = projectile.getOwner();
            ProneHitResult result = real && ProneHitMemory.SERVER.hasHit(projectile, targetId)
                    ? ProneHitService.alreadyHit(ProneConsumer.TACZ, player, shooter)
                    : ProneHitService.test(ProneConsumer.TACZ, player, projectile, shooter, start, end,
                    TaczProneKind.policy());
            switch (result.kind()) {
                case HIT -> {
                    EntityKineticBullet.EntityResult hit = new EntityKineticBullet.EntityResult(
                            target, result.point(), result.part() == BodyPart.HEAD);
                    if (real) {
                        ProneHitLedger.SERVER.put(targetId, projectileId, now, result.part(), result.point());
                        ProneHitMemory.SERVER.markHit(projectile, targetId);
                    }
                    return ProneDecision.hit(hit);
                }
                case MISS -> {
                    if (wrapSeen && ProneMixinStatus.taczWrap()) {
                        // Let TaCZ reach its own "clip found nothing" return so RETURN hooks such as
                        // taczexpands' proximity fuse still see the miss.
                        FORCED_MISS.set(target);
                        ProneHitService.count(ProneHitService.Counter.FORCED_MISS_REQUESTED);
                        return ProneDecision.PASS;
                    }
                    return ProneDecision.MISS;
                }
                default -> {
                    return ProneDecision.PASS;
                }
            }
        } catch (Throwable throwable) {
            FORCED_MISS.remove();
            if (real && targetId != null && projectileId != null) {
                ProneHitLedger.SERVER.clear(targetId, projectileId, now);
            }
            ProneHitService.reportFailure(ProneConsumer.TACZ, throwable);
            return ProneDecision.PASS;
        }
    }

    /** Called by the clip wrapper: true exactly when the pending forced miss is for this target. */
    public static boolean consumeForcedMiss(Entity target) {
        if (!wrapSeen) {
            wrapSeen = true;
        }
        Entity pending = FORCED_MISS.get();
        if (pending == null) {
            return false;
        }
        FORCED_MISS.remove();
        if (pending != target) {
            return false;
        }
        ProneHitService.count(ProneHitService.Counter.FORCED_MISS_CONSUMED);
        return true;
    }

    private ProneTaczHooks() {
    }
}
