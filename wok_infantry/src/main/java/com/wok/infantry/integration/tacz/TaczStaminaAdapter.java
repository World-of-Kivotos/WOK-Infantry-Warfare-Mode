package com.wok.infantry.integration.tacz;

import com.wok.infantry.WokInfantryMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;

/** Optional TaCZ 1.1.8 reflection boundary used by the stamina system on both logical sides. */
public final class TaczStaminaAdapter {
    private static final String MOD_ID = "tacz";
    private static final String GUN_INTERFACE = "com.tacz.guns.api.item.IGun";
    private static final String COMMON_OPERATOR = "com.tacz.guns.api.entity.IGunOperator";
    private static final String CLIENT_OPERATOR =
            "com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator";

    private static volatile Access access;
    private static volatile boolean resolutionAttempted;
    private static volatile boolean failureLogged;

    private TaczStaminaAdapter() {
    }

    public static boolean isHoldingGun(Player player) {
        if (player == null || !isAvailable()) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        Access current = resolve();
        return current != null && !held.isEmpty()
                && current.gunInterface().isInstance(held.getItem());
    }

    public static boolean isAiming(Player player) {
        if (!isHoldingGun(player)) {
            return false;
        }
        Access current = resolve();
        if (current == null) {
            return false;
        }
        try {
            if (player.level().isClientSide && current.clientFromPlayer() != null) {
                Object operator = current.clientFromPlayer().invoke(null, player);
                if (operator != null) {
                    return Boolean.TRUE.equals(current.clientIsAim().invoke(operator));
                }
            }
            Object operator = current.commonFromLiving().invoke(null, player);
            return operator != null
                    && Boolean.TRUE.equals(current.commonIsAiming().invoke(operator));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce("TaCZ aiming-state integration failed", exception);
            return false;
        }
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded(MOD_ID);
    }

    private static Access resolve() {
        Access current = access;
        if (current != null || resolutionAttempted || !isAvailable()) {
            return current;
        }
        synchronized (TaczStaminaAdapter.class) {
            if (access != null || resolutionAttempted) {
                return access;
            }
            resolutionAttempted = true;
            try {
                Class<?> gun = Class.forName(GUN_INTERFACE);
                Class<?> common = Class.forName(COMMON_OPERATOR);
                Method commonFrom = findStaticSingleArgument(common, "fromLivingEntity");
                Method commonAim = common.getMethod("getSynIsAiming");

                Method clientFrom = null;
                Method clientAim = null;
                try {
                    if (FMLEnvironment.dist != Dist.CLIENT) {
                        access = new Access(gun, commonFrom, commonAim, null, null);
                        WokInfantryMod.LOGGER.info(
                                "Enabled server-side TaCZ arm-stamina integration");
                        return access;
                    }
                    Class<?> client = Class.forName(CLIENT_OPERATOR);
                    clientFrom = findStaticSingleArgument(client, "fromLocalPlayer");
                    clientAim = client.getMethod("isAim");
                } catch (ClassNotFoundException ignored) {
                    // Dedicated servers deliberately have no client operator class.
                }
                access = new Access(gun, commonFrom, commonAim, clientFrom, clientAim);
                WokInfantryMod.LOGGER.info("Enabled TaCZ arm-stamina and aiming integration");
            } catch (ReflectiveOperationException | LinkageError exception) {
                logFailureOnce("TaCZ stamina API integration is unavailable", exception);
            }
            return access;
        }
    }

    private static Method findStaticSingleArgument(Class<?> type, String name)
            throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    private static void logFailureOnce(String message, Throwable exception) {
        if (!failureLogged) {
            failureLogged = true;
            WokInfantryMod.LOGGER.warn(message, exception);
        }
    }

    private record Access(Class<?> gunInterface, Method commonFromLiving,
                          Method commonIsAiming, Method clientFromPlayer,
                          Method clientIsAim) {
    }
}
