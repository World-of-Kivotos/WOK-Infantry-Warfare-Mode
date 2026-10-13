package com.wok.infantry.battle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

/** Optional downed integration without linking classes from another module. */
public final class CombatantStatus {
    private CombatantStatus() {}

    public static boolean isDowned(LivingEntity entity) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(
                ResourceLocation.fromNamespaceAndPath("wok_downed", "downed"));
        return entity.getPersistentData().getBoolean("wok_downed.active")
                || effect != null && entity.hasEffect(effect);
    }

    public static void resetForNewLife(net.minecraft.server.level.ServerPlayer player) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("wok_downed")) {
            try {
                Class.forName("com.wok.downed.state.DownedService")
                        .getMethod("resetForDeployment", net.minecraft.server.level.ServerPlayer.class)
                        .invoke(null, player);
            } catch (ReflectiveOperationException exception) {
                com.wok.infantry.WokInfantryMod.LOGGER.error("Downed lifecycle bridge requires an updated module", exception);
            }
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("wok_body_health")) {
            try {
                Class.forName("com.wok.bodyhealth.api.BodyHealthApi")
                        .getMethod("setAllPartsHealth", LivingEntity.class, float.class)
                        .invoke(null, player, Float.MAX_VALUE);
            } catch (NoSuchMethodException ignored) {
                // Earlier independent body-health versions have no absolute-health API.
            } catch (ReflectiveOperationException exception) {
                com.wok.infantry.WokInfantryMod.LOGGER.error("Could not reset body health for new deployment", exception);
            }
        }
    }
}
