package com.wok.bodyhealth.health;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class PendingHitStore {
    private static final Map<Player, PendingHit> PENDING = new WeakHashMap<>();

    public static void record(Player player, BodyPart part, long gameTime,
                              DamageSource... damageSources) {
        IdentityHashMap<DamageSource, Integer> sourceUses = new IdentityHashMap<>();
        for (DamageSource source : damageSources) {
            if (source != null) {
                sourceUses.merge(source, 1, Integer::sum);
            }
        }
        if (!sourceUses.isEmpty()) {
            PENDING.put(player, new PendingHit(part, gameTime, sourceUses));
        }
    }

    public static BodyPart consume(Player player, DamageSource damageSource, long gameTime) {
        PendingHit hit = PENDING.get(player);
        if (hit == null) {
            return null;
        }
        if (Math.abs(gameTime - hit.gameTime()) > 1L) {
            PENDING.remove(player);
            return null;
        }

        Integer uses = hit.sourceUses().get(damageSource);
        if (uses == null) {
            return null;
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

    public static BodyPart peek(Player player, DamageSource damageSource, long gameTime) {
        PendingHit hit = validHit(player, gameTime);
        return hit != null && hit.sourceUses().containsKey(damageSource) ? hit.part() : null;
    }

    /** Used by the optional armor hook during the same TaCZ Pre event if a source wrapper is recreated. */
    public static BodyPart peekLatest(Player player, long gameTime) {
        PendingHit hit = validHit(player, gameTime);
        return hit == null ? null : hit.part();
    }

    private static PendingHit validHit(Player player, long gameTime) {
        PendingHit hit = PENDING.get(player);
        if (hit != null && Math.abs(gameTime - hit.gameTime()) > 1L) {
            PENDING.remove(player);
            return null;
        }
        return hit;
    }

    private record PendingHit(BodyPart part, long gameTime,
                              IdentityHashMap<DamageSource, Integer> sourceUses) {
    }

    private PendingHitStore() {
    }
}
