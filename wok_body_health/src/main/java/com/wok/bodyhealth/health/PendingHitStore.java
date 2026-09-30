package com.wok.bodyhealth.health;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class PendingHitStore {
    private static final Map<Player, PendingHit> PENDING = new WeakHashMap<>();

    /**
     * Records the part for the damage sources of one hit. Entries only live
     * for the tick they were recorded in: a TaCZ bullet records and hurts
     * synchronously, and a segment that never reaches the hurt pipeline (for
     * example a zero-damage armor-piercing share) must not leak into the
     * next, unrelated hit.
     */
    public static void record(Player player, BodyPart part, long gameTime,
                              Entity directEntity, DamageSource... damageSources) {
        IdentityHashMap<DamageSource, Integer> sourceUses = new IdentityHashMap<>();
        for (DamageSource source : damageSources) {
            if (source != null) {
                sourceUses.merge(source, 1, Integer::sum);
            }
        }
        if (!sourceUses.isEmpty()) {
            UUID directEntityId = directEntity == null ? null : directEntity.getUUID();
            PENDING.put(player, new PendingHit(part, gameTime, directEntityId, sourceUses));
        }
    }

    public static BodyPart consume(Player player, DamageSource damageSource, long gameTime) {
        PendingHit hit = validHit(player, gameTime);
        if (hit == null) {
            return null;
        }

        Integer uses = hit.sourceUses().get(damageSource);
        if (uses == null) {
            // A mod may wrap the source between the TaCZ event and the hurt
            // call; the projectile identity still ties it to this hit.
            return hit.matchesProjectile(damageSource) ? hit.part() : null;
        }
        if (uses <= 1) {
            hit.sourceUses().remove(damageSource);
        } else {
            hit.sourceUses().put(damageSource, uses - 1);
        }
        if (hit.sourceUses().isEmpty()) {
            PENDING.remove(player);
        }
        return hit.part();
    }

    /** Looks up the recorded part without consuming it; used by the optional armor hook. */
    public static BodyPart peek(Player player, DamageSource damageSource, long gameTime) {
        PendingHit hit = validHit(player, gameTime);
        if (hit == null) {
            return null;
        }
        return hit.sourceUses().containsKey(damageSource) || hit.matchesProjectile(damageSource)
                ? hit.part() : null;
    }

    private static PendingHit validHit(Player player, long gameTime) {
        PendingHit hit = PENDING.get(player);
        if (hit != null && hit.gameTime() != gameTime) {
            PENDING.remove(player);
            return null;
        }
        return hit;
    }

    private record PendingHit(BodyPart part, long gameTime, UUID directEntityId,
                              IdentityHashMap<DamageSource, Integer> sourceUses) {
        boolean matchesProjectile(DamageSource source) {
            Entity direct = source.getDirectEntity();
            return directEntityId != null && direct != null
                    && directEntityId.equals(direct.getUUID());
        }
    }

    private PendingHitStore() {
    }
}
