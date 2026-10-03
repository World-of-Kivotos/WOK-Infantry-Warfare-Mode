package com.wok.infantry.mixin;

import com.wok.infantry.integration.mantle.TacticalMantleGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Tactical Mantle compatibility; applied only when that mod is
 * present (see {@link WokInfantryMixinPlugin}). The start packet is sent
 * only after a climb has actually begun, so stamina is charged there.
 */
@Pseudo
@Mixin(targets = "com.codex.tacticalmantle.server.MantleServer", remap = false)
public abstract class TacticalMantleServerMixin {
    @Inject(method = "tryStartMantle", at = @At("HEAD"), cancellable = true,
            require = 0, remap = false)
    private static void wokInfantry$gateMantle(ServerPlayer player, BlockPos pos,
                                               CallbackInfo ci) {
        if (!TacticalMantleGate.allows(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "tryStartMantle", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/network/simple/SimpleChannel;send("
                    + "Lnet/minecraftforge/network/PacketDistributor$PacketTarget;"
                    + "Ljava/lang/Object;)V"),
            require = 0, remap = false)
    private static void wokInfantry$chargeMantle(ServerPlayer player, BlockPos pos,
                                                 CallbackInfo ci) {
        TacticalMantleGate.started(player);
    }
}
