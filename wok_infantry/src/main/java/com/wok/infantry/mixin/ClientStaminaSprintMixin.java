package com.wok.infantry.mixin;

import com.wok.infantry.client.ClientStaminaController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Stops held sprint, double-tap sprint and mod requests before local movement is calculated. */
@Mixin(value = LivingEntity.class, remap = false)
public abstract class ClientStaminaSprintMixin {
    @ModifyVariable(method = {"setSprinting", "m_6858_"}, at = @At("HEAD"),
            argsOnly = true, ordinal = 0, remap = false)
    private boolean wokInfantry$guardLocalSprint(boolean requested) {
        return requested && !((Object) this instanceof LocalPlayer player
                && ClientStaminaController.isSprintBlocked(player));
    }
}
