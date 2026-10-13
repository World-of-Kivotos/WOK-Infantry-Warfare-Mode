package com.wok.bodyhealth.prone;

import net.minecraft.world.entity.Entity;

/**
 * A gun mod's real bullet type, registered by its compat class. The Level hook uses it to decide
 * whether a broad-phase query belongs to a bullet whose prone targets it should widen.
 */
public interface ProneProjectileKind {
    /** TaCZ: an {@code EntityKineticBullet}; SBW: a {@code ProjectileEntity}. */
    boolean matches(Entity projectile);

    /** Whose getHitResult hook tests this projectile against prone players. */
    ProneConsumer consumer();

    /** Whose latency the gun mod rewinds with, or null. */
    Entity shooter(Entity projectile);

    /** Rewind and sweep rules for this projectile, or null to leave it alone (e.g. SBW beast rounds). */
    RewindPolicy policy(Entity projectile);
}
