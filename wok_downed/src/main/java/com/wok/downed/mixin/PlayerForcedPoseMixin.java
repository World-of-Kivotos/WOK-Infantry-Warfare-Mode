package com.wok.downed.mixin;

import com.wok.downed.state.DownedService;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Forge's forcedPose is local state, and movement mods can clear it between player ticks. */
@Mixin(Player.class)
public abstract class PlayerForcedPoseMixin {
    // This method is added by Forge and has the same name in development and production.
    @ModifyVariable(method = "setForcedPose", at = @At("HEAD"), argsOnly = true,
            remap = false)
    private Pose wokDowned$keepCasualtyProne(Pose requested) {
        return DownedService.isDowned((Player) (Object) this) ? Pose.SWIMMING : requested;
    }
}
