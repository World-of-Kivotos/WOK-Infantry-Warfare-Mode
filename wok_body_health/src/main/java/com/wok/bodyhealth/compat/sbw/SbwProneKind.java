package com.wok.bodyhealth.compat.sbw;

import com.wok.bodyhealth.prone.ProneHitService;
import com.wok.bodyhealth.prone.ProneProjectileKind;
import com.wok.bodyhealth.prone.RewindPolicy;
import net.minecraft.world.entity.Entity;

/** SBW's {@code ProjectileEntity}; beast rounds are left alone. Uses reflection only. */
public final class SbwProneKind implements ProneProjectileKind {
    public static void register() {
        ProneHitService.registerKind(new SbwProneKind());
    }

    @Override
    public boolean matches(Entity projectile) {
        return SbwAccess.isProjectile(projectile);
    }

    @Override
    public Entity shooter(Entity projectile) {
        return SbwAccess.shooter(projectile);
    }

    @Override
    public RewindPolicy policy(Entity projectile) {
        return SbwAccess.ready(projectile) && !SbwAccess.isBeast(projectile) ? RewindPolicy.SBW : null;
    }

    private SbwProneKind() {
    }
}
