package com.wok.infantry.integration.tacz;

import com.wok.infantry.WokInfantryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

/** Optional TaCZ 1.1.8+ API boundary; no TaCZ class is linked into WOK's public API. */
public final class TaczAmmoAdapter {
    private static final String MOD_ID = "tacz";
    private static final String GUN_INTERFACE = "com.tacz.guns.api.item.IGun";
    private static final String AMMO_INTERFACE = "com.tacz.guns.api.item.IAmmo";
    private static final String API_CLASS = "com.tacz.guns.api.TimelessAPI";
    private static final String GUN_INDEX_CLASS = "com.tacz.guns.resource.index.CommonGunIndex";
    private static final String GUN_DATA_CLASS = "com.tacz.guns.resource.pojo.data.gun.GunData";
    private static final String AMMO_BUILDER_CLASS =
            "com.tacz.guns.api.item.builder.AmmoItemBuilder";

    private static volatile boolean bridgeInitialized;
    private static volatile Bridge bridge;
    private static boolean failureLogged;

    private TaczAmmoAdapter() {
    }

    public static boolean available() {
        return bridge() != null;
    }

    /** Returns the configured ammunition for any resolvable TaCZ gun. */
    public static Optional<ResourceLocation> ammunitionForGun(ItemStack stack) {
        Bridge current = bridge();
        if (current == null || stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        try {
            Object gun = current.getGun.invoke(null, stack);
            if (gun == null) {
                return Optional.empty();
            }
            Object gunId = current.getGunId.invoke(gun, stack);
            if (!(gunId instanceof ResourceLocation resourceLocation)) {
                return Optional.empty();
            }
            Object optionalIndex = current.getCommonGunIndex.invoke(null, resourceLocation);
            if (!(optionalIndex instanceof Optional<?> optional) || optional.isEmpty()) {
                return Optional.empty();
            }
            Object gunData = current.getGunData.invoke(optional.get());
            Object ammoId = current.getAmmoIdFromGunData.invoke(gunData);
            return ammoId instanceof ResourceLocation id ? Optional.of(id) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce(exception);
            return Optional.empty();
        }
    }

    public static Optional<ResourceLocation> ammoId(ItemStack stack) {
        Bridge current = bridge();
        if (current == null || stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        try {
            Object ammo = current.getAmmo.invoke(null, stack);
            if (ammo == null) {
                return Optional.empty();
            }
            Object ammoId = current.getAmmoId.invoke(ammo, stack);
            return ammoId instanceof ResourceLocation id ? Optional.of(id) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce(exception);
            return Optional.empty();
        }
    }

    public static ItemStack createAmmo(ResourceLocation ammoId, int count) {
        Bridge current = bridge();
        if (current == null || ammoId == null || count < 1) {
            return ItemStack.EMPTY;
        }
        try {
            Object builder = current.createBuilder.invoke(null);
            current.setBuilderId.invoke(builder, ammoId);
            current.setBuilderCount.invoke(builder, count);
            Object built = current.buildAmmo.invoke(builder);
            return built instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce(exception);
            return ItemStack.EMPTY;
        }
    }

    private static Bridge bridge() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return null;
        }
        if (!bridgeInitialized) {
            synchronized (TaczAmmoAdapter.class) {
                if (!bridgeInitialized) {
                    bridge = createBridge();
                    bridgeInitialized = true;
                }
            }
        }
        return bridge;
    }

    private static Bridge createBridge() {
        try {
            Class<?> gunType = Class.forName(GUN_INTERFACE);
            Class<?> ammoType = Class.forName(AMMO_INTERFACE);
            Class<?> apiType = Class.forName(API_CLASS);
            Class<?> gunIndexType = Class.forName(GUN_INDEX_CLASS);
            Class<?> gunDataType = Class.forName(GUN_DATA_CLASS);
            Class<?> builderType = Class.forName(AMMO_BUILDER_CLASS);
            return new Bridge(
                    gunType.getMethod("getIGunOrNull", ItemStack.class),
                    gunType.getMethod("getGunId", ItemStack.class),
                    apiType.getMethod("getCommonGunIndex", ResourceLocation.class),
                    gunIndexType.getMethod("getGunData"),
                    gunDataType.getMethod("getAmmoId"),
                    ammoType.getMethod("getIAmmoOrNull", ItemStack.class),
                    ammoType.getMethod("getAmmoId", ItemStack.class),
                    builderType.getMethod("create"),
                    builderType.getMethod("setId", ResourceLocation.class),
                    builderType.getMethod("setCount", int.class),
                    builderType.getMethod("build"));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce(exception);
            return null;
        }
    }

    private static synchronized void logFailureOnce(Exception exception) {
        if (!failureLogged) {
            failureLogged = true;
            WokInfantryMod.LOGGER.error(
                    "TaCZ ammunition API integration failed; physical ammo supply is disabled",
                    exception);
        }
    }

    private record Bridge(Method getGun, Method getGunId,
                          Method getCommonGunIndex, Method getGunData,
                          Method getAmmoIdFromGunData, Method getAmmo, Method getAmmoId,
                          Method createBuilder, Method setBuilderId, Method setBuilderCount,
                          Method buildAmmo) {
    }
}
