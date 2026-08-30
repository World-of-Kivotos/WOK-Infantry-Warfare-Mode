package com.wok.infantry.mixin;

import com.wok.infantry.ammo.AmmoSupplyService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks the common Superb Warfare vehicle damage boundary so non-living DragonRise stations
 * react to bullets, melee, projectiles and explosions alike. The pseudo target preserves the
 * core's independent-install behavior when Superb Warfare is absent.
 */
@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity",
        remap = false)
public abstract class VehicleEntityDamageMixin {
    @Inject(method = {"hurt", "m_6469_"}, at = @At("HEAD"), remap = false)
    private void wokInfantry$explodeAmmoStationWhenAttacked(
            DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        if (amount > 0.0F) {
            AmmoSupplyService.explodeLargeStation((Entity) (Object) this);
        }
    }
}
