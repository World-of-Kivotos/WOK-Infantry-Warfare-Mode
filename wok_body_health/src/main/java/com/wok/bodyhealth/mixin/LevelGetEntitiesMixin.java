package com.wok.bodyhealth.mixin;

import com.wok.bodyhealth.prone.ProneCandidateInjector;
import com.wok.bodyhealth.prone.ProneMixinGuard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Adds prone players to a bullet's broad-phase query (spec 4.3); applied only with TaCZ or SBW.
 * Effective only inside a gun mod's find* scope for that bullet, once; everything else returns
 * after a null check and a volatile read. Lists both the official and the SRG method name.
 */
@Mixin(value = Level.class, remap = false)
public abstract class LevelGetEntitiesMixin {
    @Inject(method = {
            "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;"
                    + "Ljava/util/function/Predicate;)Ljava/util/List;",
            "m_6249_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;"
                    + "Ljava/util/function/Predicate;)Ljava/util/List;"},
            at = @At("RETURN"), cancellable = true, remap = false, require = 0, expect = 1)
    private void wokBodyHealth$addProneCandidates(Entity except, AABB box, Predicate<? super Entity> predicate,
                                                  CallbackInfoReturnable<List<Entity>> cir) {
        if (except == null || ProneMixinGuard.off("level")) {
            return;
        }
        try {
            ProneCandidateInjector.afterGetEntities((Level) (Object) this, except, box, predicate, cir);
        } catch (Throwable t) {
            ProneMixinGuard.report("level", t);
        }
    }
}
