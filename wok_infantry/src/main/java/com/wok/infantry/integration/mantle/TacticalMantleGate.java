package com.wok.infantry.integration.mantle;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.stamina.StaminaEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Server-side rules for the optional Tactical Mantle ledge climb. The climb
 * moves the player with teleports, so jump-event penalties never see it.
 */
public final class TacticalMantleGate {
    private static final String BODY_HEALTH_MOD_ID = "wok_body_health";
    private static final String BODY_HEALTH_API = "com.wok.bodyhealth.api.BodyHealthApi";
    private static Method destroyedLegCount;
    private static boolean bodyHealthResolved;

    private TacticalMantleGate() {
    }

    public static boolean allows(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        // Prone and crawling both use the swimming pose outside water.
        if (player.getPose() == Pose.SWIMMING && !player.isSwimming()) {
            return false;
        }
        return destroyedLegs(player) == 0 && StaminaEvents.canAffordMantle(player);
    }

    public static void started(ServerPlayer player) {
        StaminaEvents.onMantleStarted(player);
    }

    private static int destroyedLegs(ServerPlayer player) {
        Method method = bodyHealthMethod();
        if (method == null) {
            return 0;
        }
        try {
            return (int) method.invoke(null, player);
        } catch (ReflectiveOperationException exception) {
            WokInfantryMod.LOGGER.error("Could not read destroyed legs for Tactical Mantle", exception);
            return 0;
        }
    }

    private static Method bodyHealthMethod() {
        if (!bodyHealthResolved) {
            bodyHealthResolved = true;
            if (ModList.get().isLoaded(BODY_HEALTH_MOD_ID)) {
                try {
                    destroyedLegCount = Class.forName(BODY_HEALTH_API)
                            .getMethod("destroyedLegCount", LivingEntity.class);
                } catch (ReflectiveOperationException ignored) {
                    // Body health before 0.1.0-beta.5 has no leg query.
                }
            }
        }
        return destroyedLegCount;
    }
}
