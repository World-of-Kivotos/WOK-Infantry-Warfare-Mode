package com.wok.capturepoints.capture;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

final class CaptureEligibility {
    private CaptureEligibility() {}

    static boolean canCount(ServerPlayer player) {
        MobEffect downed = ForgeRegistries.MOB_EFFECTS.getValue(
                ResourceLocation.fromNamespaceAndPath("wok_downed", "downed"));
        return player.isAlive() && !player.isSpectator()
                && !player.getPersistentData().getBoolean("wok_downed.active")
                && (downed == null || !player.hasEffect(downed))
                && InfantryFactionBridge.isActive(player);
    }
}
