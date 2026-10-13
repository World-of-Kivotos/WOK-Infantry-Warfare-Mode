package com.wok.infantry.mixin;

import com.wok.infantry.client.ClientProneStability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only replaces TaCZ's per-shot prone factor; no gun data or attachment cache is mutated. */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.event.CameraSetupEvent", remap = false)
public abstract class TaczProneRecoilMixin {
    @Redirect(method = "initialCameraRecoil", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getCrawlRecoilMultiplier()F"), remap = false)
    private static float wokInfantry$gradualProneRecoil(@Coerce Object gunData) {
        return ClientProneStability.crawlMultiplier(gunData);
    }
}
