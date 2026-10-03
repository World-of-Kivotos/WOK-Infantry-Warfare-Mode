package com.wok.bodyhealth.mixin.tacz;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.EntityUtil;
import com.wok.bodyhealth.compat.tacz.ProneTaczHooks;
import com.wok.bodyhealth.prone.ProneDecision;
import com.wok.bodyhealth.prone.ProneMixinGuard;
import com.wok.bodyhealth.prone.ProneScope;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Segmented prone hitboxes for TaCZ bullets (spec 4.1); applied only with TaCZ installed (see
 * {@code WokBodyHealthMixinPlugin}). HEAD of getHitResult decides HIT/MISS for prone players; a
 * MISS is turned into TaCZ's own "clip found nothing" return by the clip wrapper so RETURN hooks
 * of other mods still run. The find* HEAD/RETURN pair opens the broad-phase scope the Level hook
 * widens. Remapping is off and no refmap is shipped: TaCZ's own method names never change, and the
 * one vanilla member, {@code AABB.clip}, is listed by SRG and by official name.
 */
// A class literal, not a string: Mixin rejects a public target named in "targets" when its strict
// target checks are on. The plugin filters by name before the class is looked up.
@Mixin(value = EntityUtil.class, remap = false)
public abstract class TaczEntityUtilMixin {
    private static final String GET_HIT_RESULT =
            "getHitResult(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
                    + "Lcom/tacz/guns/entity/EntityKineticBullet$EntityResult;";
    private static final String FIND_ONE =
            "findEntityOnPath(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/Vec3;)Lcom/tacz/guns/entity/EntityKineticBullet$EntityResult;";
    private static final String FIND_ALL =
            "findEntitiesOnPath(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;";

    @Inject(method = {FIND_ONE, FIND_ALL}, at = @At("HEAD"), remap = false, require = 0, expect = 2)
    private static void wokBodyHealth$scopeEnter(Projectile projectile, Vec3 start, Vec3 end,
                                                 CallbackInfoReturnable<?> cir) {
        try {
            ProneScope.enter(projectile);
        } catch (Throwable t) {
            ProneMixinGuard.report("tacz.scope", t);
        }
    }

    @Inject(method = {FIND_ONE, FIND_ALL}, at = @At("RETURN"), remap = false, require = 0)
    private static void wokBodyHealth$scopeExit(Projectile projectile, Vec3 start, Vec3 end,
                                                CallbackInfoReturnable<?> cir) {
        try {
            ProneScope.exit(projectile);
        } catch (Throwable t) {
            ProneMixinGuard.report("tacz.scope", t);
        }
    }

    @Inject(method = GET_HIT_RESULT, at = @At("HEAD"), cancellable = true, remap = false, require = 0, expect = 1)
    private static void wokBodyHealth$proneHead(Projectile projectile, Entity target, Vec3 start, Vec3 end,
                                                CallbackInfoReturnable<EntityKineticBullet.EntityResult> cir) {
        if (ProneMixinGuard.off("tacz.head")) {
            return;
        }
        try {
            ProneDecision decision = ProneTaczHooks.onHead(projectile, target, start, end);
            if (decision.cancels()) {
                // MISS carries a null result: getHitResult returns null like an empty clip.
                cir.setReturnValue((EntityKineticBullet.EntityResult) decision.result());
            }
        } catch (Throwable t) {
            ProneMixinGuard.report("tacz.head", t);
        }
    }

    @WrapOperation(method = GET_HIT_RESULT, remap = false, require = 0, expect = 1, at = {
            @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/world/phys/AABB;m_82371_(Lnet/minecraft/world/phys/Vec3;"
                            + "Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"),
            @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;"
                            + "Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;")})
    private static Optional<Vec3> wokBodyHealth$clipOrForcedMiss(AABB box, Vec3 from, Vec3 to,
                                                                 Operation<Optional<Vec3>> original,
                                                                 @Local(argsOnly = true) Entity target) {
        boolean miss = false;
        try {
            miss = ProneTaczHooks.consumeForcedMiss(target);
        } catch (Throwable t) {
            ProneMixinGuard.report("tacz.wrap", t);
        }
        return miss ? Optional.empty() : original.call(box, from, to);
    }
}
