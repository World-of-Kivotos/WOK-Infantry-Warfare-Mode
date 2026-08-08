package com.wok.trauma.mixin.client;

import com.wok.trauma.registry.ModEffects;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayer.class, remap = false)
public abstract class LocalPlayerMixin {
    @Inject(
            method = {"hurtTo", "m_108760_"},
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void wokTrauma$suppressBleedingHurtAnimation(float health, CallbackInfo callback) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (health < player.getHealth()
                && (player.hasEffect(ModEffects.BLEEDING.get())
                || player.hasEffect(ModEffects.MAJOR_BLEEDING.get()))) {
            player.setHealth(health);
            callback.cancel();
        }
    }
}
