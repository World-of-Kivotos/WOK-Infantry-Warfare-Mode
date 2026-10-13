package com.wok.vehiclehealth.mixin;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.wok.vehiclehealth.balance.VehicleBalanceProfile;
import com.wok.vehiclehealth.balance.VehicleBalanceProfiles;
import com.wok.vehiclehealth.balance.VehicleDamageBalancer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies WOK category/tier values only to explicitly registered formation vehicles. */
@Mixin(value = VehicleEntity.class, remap = false)
public abstract class VehicleEntityBalanceMixin {
    @Inject(method = "getMaxHealth", at = @At("RETURN"), cancellable = true, remap = false)
    private void wokVehicleHealth$maxHealth(CallbackInfoReturnable<Float> callback) {
        profile().ifPresent(profile -> callback.setReturnValue(profile.hullHealth()));
    }

    @Inject(method = "getWheelMaxHealth", at = @At("RETURN"), cancellable = true, remap = false)
    private void wokVehicleHealth$wheelHealth(CallbackInfoReturnable<Float> callback) {
        profile().ifPresent(profile -> callback.setReturnValue(profile.runningGearHealth()));
    }

    @Inject(method = "getEngineMaxHealth", at = @At("RETURN"), cancellable = true, remap = false)
    private void wokVehicleHealth$engineHealth(CallbackInfoReturnable<Float> callback) {
        profile().ifPresent(profile -> callback.setReturnValue(profile.engineHealth()));
    }

    @Inject(method = "getTurretMaxHealth", at = @At("RETURN"), cancellable = true, remap = false)
    private void wokVehicleHealth$turretHealth(CallbackInfoReturnable<Float> callback) {
        profile().ifPresent(profile -> callback.setReturnValue(profile.turretHealth()));
    }

    @Redirect(
            method = "m_6469_",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/atsuishio/superbwarfare/entity/vehicle/damage/DamageModifier;compute(Lnet/minecraft/world/damagesource/DamageSource;F)F"),
            remap = false)
    private float wokVehicleHealth$normalizeDamage(DamageModifier modifier,
                                                    DamageSource source,
                                                    float rawAmount) {
        float addonComputedAmount = modifier.compute(source, rawAmount);
        return VehicleDamageBalancer.normalize(
                (VehicleEntity) (Object) this, source, rawAmount, addonComputedAmount);
    }

    private java.util.Optional<VehicleBalanceProfile> profile() {
        return VehicleBalanceProfiles.find((VehicleEntity) (Object) this);
    }
}
