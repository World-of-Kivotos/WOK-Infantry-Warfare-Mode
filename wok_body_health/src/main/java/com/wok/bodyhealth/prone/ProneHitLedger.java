package com.wok.bodyhealth.prone;

import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Same-tick handover of the segment a projectile hit, from the hit test to the damage handlers.
 * Keys are (target, projectile, gameTime); writing at a new gameTime first drops the whole table,
 * so nothing outlives the tick it was recorded in. Lookups do not consume, letting the normal
 * and armor-piercing shares of one bullet resolve to the same part. Server thread only.
 */
public final class ProneHitLedger {
    public static final ProneHitLedger SERVER = new ProneHitLedger();

    private final Map<Key, Entry> entries = new HashMap<>();
    private long gameTime = Long.MIN_VALUE;

    public void put(UUID target, UUID projectile, long gameTime, BodyPart part, Vec3 point) {
        if (target == null || projectile == null || part == null) {
            return;
        }
        roll(gameTime);
        entries.put(new Key(target, projectile), new Entry(part, point));
    }

    public void clear(UUID target, UUID projectile, long gameTime) {
        roll(gameTime);
        if (target != null && projectile != null) {
            entries.remove(new Key(target, projectile));
        }
    }

    /** Recorded part, or null; {@code projectile} may be null (then nothing matches). */
    public BodyPart find(UUID target, UUID projectile, long gameTime) {
        Entry entry = entry(target, projectile, gameTime);
        return entry == null ? null : entry.part();
    }

    /** Recorded entry point, or null; for debug output. */
    public Vec3 findPoint(UUID target, UUID projectile, long gameTime) {
        Entry entry = entry(target, projectile, gameTime);
        return entry == null ? null : entry.point();
    }

    public void clearAll() {
        entries.clear();
        gameTime = Long.MIN_VALUE;
    }

    private Entry entry(UUID target, UUID projectile, long gameTime) {
        if (target == null || projectile == null || gameTime != this.gameTime) {
            return null;
        }
        return entries.get(new Key(target, projectile));
    }

    private void roll(long gameTime) {
        if (gameTime != this.gameTime) {
            entries.clear();
            this.gameTime = gameTime;
        }
    }

    private record Key(UUID target, UUID projectile) {
    }

    private record Entry(BodyPart part, Vec3 point) {
    }
}
