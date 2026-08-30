package com.wok.infantry.integration.tacz;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.loadout.LoadoutEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

/** Optional reflection boundary for TaCZ's gun builder; no TaCZ class enters the production API. */
public final class TaczLoadoutAdapter {
    private static final String MOD_ID = "tacz";
    private static final String GUN_ITEM_ID = "tacz:modern_kinetic_gun";
    private static final String BUILDER_CLASS = "com.tacz.guns.api.item.builder.GunItemBuilder";
    private static final String API_CLASS = "com.tacz.guns.api.TimelessAPI";
    private static boolean reflectionFailureLogged;

    private TaczLoadoutAdapter() {
    }

    /** Empty Optional means this is not a TaCZ gun entry; an optional empty stack is fail-closed. */
    public static Optional<ItemStack> createGun(LoadoutEntry entry, CompoundTag configuredTag) {
        if (entry == null || !GUN_ITEM_ID.equals(entry.itemId())) {
            return Optional.empty();
        }
        if (!ModList.get().isLoaded(MOD_ID) || configuredTag == null) {
            return Optional.of(ItemStack.EMPTY);
        }
        ResourceLocation gunId = ResourceLocation.tryParse(configuredTag.getString("GunId"));
        if (gunId == null || !gunExists(gunId)) {
            return Optional.of(ItemStack.EMPTY);
        }
        try {
            Class<?> builderType = Class.forName(BUILDER_CLASS);
            Object builder = builderType.getMethod("create").invoke(null);
            Method setId = builderType.getMethod("setId", ResourceLocation.class);
            Method setCount = builderType.getMethod("setCount", int.class);
            setId.invoke(builder, gunId);
            setCount.invoke(builder, entry.count());
            Object built = builderType.getMethod("build").invoke(builder);
            if (!(built instanceof ItemStack stack) || stack.isEmpty()) {
                return Optional.of(ItemStack.EMPTY);
            }
            // Builder supplies TaCZ's runtime defaults (including fire mode). Explicit admin SNBT
            // is then overlaid, preserving the existing loadout-file contract.
            stack.getOrCreateTag().merge(configuredTag.copy());
            stack.setCount(entry.count());
            return Optional.of(stack);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce(exception);
            return Optional.of(ItemStack.EMPTY);
        }
    }

    private static boolean gunExists(ResourceLocation gunId) {
        try {
            Class<?> apiType = Class.forName(API_CLASS);
            Object result = apiType.getMethod("getCommonGunIndex", ResourceLocation.class)
                    .invoke(null, gunId);
            return result instanceof Optional<?> optional && optional.isPresent();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce(exception);
            return false;
        }
    }

    private static synchronized void logFailureOnce(Exception exception) {
        if (!reflectionFailureLogged) {
            reflectionFailureLogged = true;
            WokInfantryMod.LOGGER.error(
                    "TaCZ API integration failed; TaCZ gun issuance is disabled fail-closed",
                    exception);
        }
    }
}
