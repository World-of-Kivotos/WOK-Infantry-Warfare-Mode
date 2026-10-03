package com.wok.bodyhealth.compat.sbw;

import com.wok.bodyhealth.prone.ProneConsumer;
import com.wok.bodyhealth.prone.ProneDecision;
import com.wok.bodyhealth.prone.ProneHistoryTracker;
import com.wok.bodyhealth.prone.ProneHitLedger;
import com.wok.bodyhealth.prone.ProneHitMemory;
import com.wok.bodyhealth.prone.ProneHitResult;
import com.wok.bodyhealth.prone.ProneHitService;
import com.wok.bodyhealth.prone.RewindPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Entry point of {@code SbwProjectileEntityMixin} (spec 4.2). Side effects come after every step
 * that can fail; once the hit is committed (ledger, memory, detonation) it is never rolled back,
 * so one round cannot explode twice.
 */
public final class SbwProneHooks {
    /** HEAD of SBW {@code ProjectileEntity.getHitResult}; both callers treat null as "no hit". */
    public static ProneDecision onHead(Entity projectile, Entity target, Vec3 start, Vec3 end) {
        UUID targetId = null;
        UUID projectileId = null;
        long now = 0L;
        ProneHitResult result;
        float explosionDamage;
        Object hit;
        try {
            if (!(target instanceof ServerPlayer player)
                    || !(player.level() instanceof ServerLevel level)
                    || !level.getServer().isSameThread()
                    || !ProneHistoryTracker.isCandidate(player)
                    || !SbwAccess.ready(projectile)
                    || SbwAccess.isBeast(projectile)) {
                return ProneDecision.PASS;
            }

            targetId = player.getUUID();
            projectileId = projectile.getUUID();
            now = level.getGameTime();
            ProneHitLedger.SERVER.clear(targetId, projectileId, now);
            // Read the shooter field, not the owner: SBW rewinds with it, NPC rounds only set the owner.
            Entity shooter = SbwAccess.shooter(projectile);
            if (ProneHitMemory.SERVER.hasHit(projectile, targetId)) {
                ProneHitService.alreadyHit(ProneConsumer.SBW, player, shooter);
                return ProneDecision.MISS;
            }

            result = ProneHitService.test(
                    ProneConsumer.SBW, player, projectile, shooter, start, end, RewindPolicy.SBW);
            switch (result.kind()) {
                case MISS -> {
                    return ProneDecision.MISS;
                }
                case NOT_APPLICABLE -> {
                    return ProneDecision.PASS;
                }
                default -> {
                }
            }

            explosionDamage = SbwAccess.explosionDamage(projectile);
            hit = SbwAccess.newEntityResult(target, result.point(), result.headshot(),
                    ProneHitService.settings().sbwLegShots() && result.legSegment());
        } catch (Throwable throwable) {
            if (targetId != null && projectileId != null) {
                ProneHitLedger.SERVER.clear(targetId, projectileId, now);
            }
            ProneHitService.reportFailure(ProneConsumer.SBW, throwable);
            return ProneDecision.PASS;
        }

        // Committed from here on: no failure below may hand the hit back to SBW's own test.
        ProneHitLedger.SERVER.put(targetId, projectileId, now, result.part(), result.point());
        ProneHitMemory.SERVER.markHit(projectile, targetId);
        if (explosionDamage > 0.0F) {
            // SBW's own getHitResult detonates explosive rounds on a hit; keep that.
            try {
                SbwAccess.explosionBullet(projectile, result.point());
            } catch (Throwable throwable) {
                ProneHitService.reportFailure(ProneConsumer.SBW, throwable);
            }
        }
        return ProneDecision.hit(hit);
    }

    private SbwProneHooks() {
    }
}
