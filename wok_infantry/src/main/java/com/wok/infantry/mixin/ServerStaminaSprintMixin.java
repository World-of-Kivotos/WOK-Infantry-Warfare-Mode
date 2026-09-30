package com.wok.infantry.mixin;

import com.wok.infantry.stamina.StaminaEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Reject sprint activation before vanilla installs its movement-speed modifier. */
@Mixin(value = LivingEntity.class, remap = false)
public abstract class ServerStaminaSprintMixin {
    @ModifyVariable(method = {"setSprinting", "m_6858_"}, at = @At("HEAD"),
            argsOnly = true, ordinal = 0, remap = false)
    private boolean wokInfantry$guardServerSprint(boolean requested) {
        return requested && !((Object) this instanceof ServerPlayer player
                && StaminaEvents.isSprintBlocked(player));
    }
}
