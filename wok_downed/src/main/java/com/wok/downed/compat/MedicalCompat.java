package com.wok.downed.compat;

import com.wok.downed.WokDownedMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;

public final class MedicalCompat {
    private static Method bodyHealthHealAllParts;
    private static boolean bodyHealthBridgeResolved;

    public static void stabilize(ServerPlayer player, float bodyPartHealing) {
        removeEffect(player, "wok_trauma", "bleeding");
        removeEffect(player, "wok_trauma", "major_bleeding");
        healBodyParts(player, bodyPartHealing);
    }

    private static void removeEffect(ServerPlayer player, String namespace, String path) {
        if (!ModList.get().isLoaded(namespace)) {
            return;
        }
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(
                ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (effect != null) {
            player.removeEffect(effect);
        }
    }

    private static void healBodyParts(ServerPlayer player, float amount) {
        if (amount <= 0.0F || !ModList.get().isLoaded("wok_body_health")) {
            return;
        }
        try {
            Method bridge = resolveBodyHealthBridge();
            if (bridge != null) {
                bridge.invoke(null, player, amount);
            }
        } catch (ReflectiveOperationException exception) {
            WokDownedMod.LOGGER.warn(
                    "WOK Body Health revive bridge failed; vanilla revival will continue.",
                    exception);
        }
    }

    private static Method resolveBodyHealthBridge() throws ReflectiveOperationException {
        if (!bodyHealthBridgeResolved) {
            bodyHealthBridgeResolved = true;
            Class<?> api = Class.forName("com.wok.bodyhealth.api.BodyHealthApi");
            bodyHealthHealAllParts = api.getMethod(
                    "healAllParts", LivingEntity.class, float.class);
        }
        return bodyHealthHealAllParts;
    }

    private MedicalCompat() {
    }
}
