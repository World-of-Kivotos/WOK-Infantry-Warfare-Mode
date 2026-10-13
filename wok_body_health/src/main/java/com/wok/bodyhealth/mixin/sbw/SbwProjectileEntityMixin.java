package com.wok.bodyhealth.mixin.sbw;

import com.wok.bodyhealth.compat.sbw.SbwProneHooks;
import com.wok.bodyhealth.prone.ProneDecision;
import com.wok.bodyhealth.prone.ProneMixinGuard;
import com.wok.bodyhealth.prone.ProneScope;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Segmented prone hitboxes for Superb Warfare bullets (spec 4.2); applied only with SBW installed.
 * SBW is not on the compile classpath, so the target is named by string and every SBW member is
 * reached through {@code SbwAccess} reflection instead of shadows.
 */
@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity", remap = false)
public abstract class SbwProjectileEntityMixin {
    private static final String GET_HIT_RESULT = "getHitResult(Lnet/minecraft/world/entity/Entity;"
            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
            + "Lcom/atsuishio/superbwarfare/world/phys/EntityResult;";
    private static final String FIND_ALL =
            "findEntitiesOnPath(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;";
    private static final String FIND_ONE = "findEntityOnPath(Lnet/minecraft/world/phys/Vec3;"
            + "Lnet/minecraft/world/phys/Vec3;)Lcom/atsuishio/superbwarfare/world/phys/EntityResult;";

    @Inject(method = {FIND_ALL, FIND_ONE}, at = @At("HEAD"), remap = false, require = 0, expect = 2)
    private void wokBodyHealth$scopeEnter(Vec3 start, Vec3 end, CallbackInfoReturnable<?> cir) {
        try {
            ProneScope.enter((Object) this);
        } catch (Throwable t) {
            ProneMixinGuard.report("sbw.scope", t);
        }
    }

    @Inject(method = {FIND_ALL, FIND_ONE}, at = @At("RETURN"), remap = false, require = 0)
    private void wokBodyHealth$scopeExit(Vec3 start, Vec3 end, CallbackInfoReturnable<?> cir) {
        try {
            ProneScope.exit((Object) this);
        } catch (Throwable t) {
            ProneMixinGuard.report("sbw.scope", t);
        }
    }

    @Inject(method = GET_HIT_RESULT, at = @At("HEAD"), cancellable = true, remap = false, require = 0, expect = 1)
    private void wokBodyHealth$proneHead(Entity target, Vec3 start, Vec3 end, CallbackInfoReturnable<Object> cir) {
        if (ProneMixinGuard.off("sbw.head")) {
            return;
        }
        try {
            ProneDecision decision = SbwProneHooks.onHead((Entity) (Object) this, target, start, end);
            if (decision.cancels()) {
                cir.setReturnValue(decision.result());
            }
        } catch (Throwable t) {
            ProneMixinGuard.report("sbw.head", t);
        }
    }
}
