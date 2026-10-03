package com.wok.bodyhealth.prone;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Targets a projectile has already hit. The original 0.6 box stopped a piercing bullet from
 * hitting the same player twice because a start inside the box misses; separate segments cannot
 * do that, so the projectile remembers its targets instead. Weak keys: an entry disappears with
 * its projectile. Server thread only.
 */
public final class ProneHitMemory {
    public static final ProneHitMemory SERVER = new ProneHitMemory();

    private final Map<Object, Set<UUID>> hits = new WeakHashMap<>();

    public boolean hasHit(Object projectile, UUID target) {
        if (projectile == null || target == null) {
            return false;
        }
        Set<UUID> targets = hits.get(projectile);
        return targets != null && targets.contains(target);
    }

    public void markHit(Object projectile, UUID target) {
        if (projectile == null || target == null) {
            return;
        }
        hits.computeIfAbsent(projectile, key -> new HashSet<>(2)).add(target);
    }

    public void clearAll() {
        hits.clear();
    }
}
