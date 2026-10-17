package com.wok.downed.mixin;

import com.wok.downed.client.ClientDownedEvents;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raw key events are not cancellable through Forge's movement-input event. */
@Pseudo
@Mixin(targets = {
        "com.mafuyu404.moveslikemafuyu.event.CrawEvent",
        "com.mafuyu404.moveslikemafuyu.event.SlideEvent",
        "com.mafuyu404.moveslikemafuyu.event.SwimEvent"
}, remap = false)
public abstract class MafuyuMovementKeysMixin {
    @Inject(method = "onAction", at = @At("HEAD"), cancellable = true, remap = false)
    private static void wokDowned$blockMovementKeys(InputEvent.Key event, CallbackInfo callback) {
        if (ClientDownedEvents.isLocalPlayerDowned()) {
            callback.cancel();
        }
    }
}
