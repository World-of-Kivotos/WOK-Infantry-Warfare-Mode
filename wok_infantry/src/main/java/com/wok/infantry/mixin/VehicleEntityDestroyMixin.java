package com.wok.infantry.mixin;

import com.wok.infantry.ammo.AmmoSupplyService;
import com.wok.infantry.ammo.LargeSupplyStationDetonation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks Superb Warfare's actual vehicle-destruction boundary. The pseudo target keeps the core
 * independently installable when Superb Warfare is absent, while the runtime entity-id check
 * limits the cinematic detonation to DragonRise's large ammunition station.
 */
@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity",
        remap = false)
public abstract class VehicleEntityDestroyMixin {
    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true, remap = false)
    private void wokInfantry$igniteDestroyedAmmoStation(CallbackInfo callback) {
        Entity station = (Entity) (Object) this;
        if (!station.level().isClientSide() && AmmoSupplyService.isLargeStation(station)) {
            LargeSupplyStationDetonation.begin(station);
            // SBW would otherwise queue its own explosion immediately. Keep the zero-health
            // station visible during the warning burn; the staged effect removes it on the
            // authored primary-blast frame.
            callback.cancel();
        }
    }
}
