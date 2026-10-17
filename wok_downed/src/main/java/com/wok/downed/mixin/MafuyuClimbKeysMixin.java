package com.wok.downed.mixin;

import com.wok.downed.client.ClientDownedEvents;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.mafuyu404.moveslikemafuyu.event.ClimbEvent", remap = false)
public abstract class MafuyuClimbKeysMixin {
    @Inject(method = "jumpOnClimbable", at = @At("HEAD"), cancellable = true, remap = false)
    private static void wokDowned$blockClimbKeys(InputEvent.Key event, CallbackInfo callback) {
        if (ClientDownedEvents.isLocalPlayerDowned()) {
            callback.cancel();
        }
    }
}
