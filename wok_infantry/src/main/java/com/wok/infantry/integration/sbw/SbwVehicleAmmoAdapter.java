package com.wok.infantry.integration.sbw;

import com.wok.infantry.WokInfantryMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Optional reflection boundary for the exact public SBW 0.8.9 vehicle-ammunition model. */
public final class SbwVehicleAmmoAdapter {
    private static final String MOD_ID = "superbwarfare";
    private static final String VEHICLE_CLASS =
            "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity";
    private static final String GUN_DATA_CLASS =
            "com.atsuishio.superbwarfare.data.gun.GunData";
    private static final String DEFAULT_GUN_DATA_CLASS =
            "com.atsuishio.superbwarfare.data.gun.DefaultGunData";
    private static final String AMMO_CONSUMER_CLASS =
            "com.atsuishio.superbwarfare.data.gun.AmmoConsumer";

    private static volatile boolean initialized;
    private static volatile Bridge bridge;
    private static boolean failureLogged;

    private SbwVehicleAmmoAdapter() {
    }

    public static boolean isVehicle(Entity entity) {
        Bridge current = bridge();
        return current != null && entity != null && current.vehicleType.isInstance(entity);
    }

    public static List<AmmoTarget> ammunition(Entity vehicle) {
        Bridge current = bridge();
        if (current == null || !current.vehicleType.isInstance(vehicle)) {
            return List.of();
        }
        IItemHandler handler = vehicle.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve().orElse(null);
        if (handler == null) {
            return List.of();
        }
        try {
            Object rawMap = current.getGunDataMap.invoke(vehicle);
            if (!(rawMap instanceof Map<?, ?> map)) {
                return List.of();
            }
            List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
            entries.sort(Comparator.comparing(entry -> String.valueOf(entry.getKey())));
            List<AmmoTarget> result = new ArrayList<>();
            for (Map.Entry<?, ?> entry : entries) {
                String weaponKey = String.valueOf(entry.getKey());
                Object gunData = entry.getValue();
                if (gunData == null || !current.gunDataType.isInstance(gunData)) {
                    continue;
                }
                Object computed = current.computeGunData.invoke(gunData);
                String weaponTranslation = String.valueOf(current.getWeaponName.invoke(computed));
                Component weaponName = weaponTranslation.isBlank()
                        ? Component.literal(weaponKey)
                        : Component.translatable(weaponTranslation);
                Object rawConsumers = current.getAmmoConsumers.invoke(computed);
                if (!(rawConsumers instanceof List<?> consumers)) {
                    continue;
                }
                for (int index = 0; index < consumers.size(); index++) {
                    Object consumer = consumers.get(index);
                    AmmoTarget target = createTarget(current, handler, weaponKey, weaponName,
                            index, gunData, consumer);
                    if (target != null) {
                        result.add(target);
                    }
                }
            }
            return List.copyOf(result);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce(exception);
            return List.of();
        }
    }

    public static AmmoTarget find(Entity vehicle, String weaponKey, int consumerIndex) {
        if (weaponKey == null || consumerIndex < 0) {
            return null;
        }
        return ammunition(vehicle).stream()
                .filter(target -> target.weaponKey.equals(weaponKey)
                        && target.consumerIndex == consumerIndex)
                .findFirst().orElse(null);
    }

    public static int insertPackages(AmmoTarget target, int requestedPackages) {
        if (target == null || requestedPackages < 1) {
            return 0;
        }
        int remaining = Math.min(requestedPackages, target.packageCapacity);
        int inserted = 0;
        while (remaining > 0) {
            ItemStack stack = target.template.copy();
            stack.setCount(Math.min(remaining, stack.getMaxStackSize()));
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                    target.handler, stack, false);
            int moved = stack.getCount() - remainder.getCount();
            if (moved < 1) {
                break;
            }
            inserted += moved;
            remaining -= moved;
        }
        return inserted;
    }

    private static AmmoTarget createTarget(Bridge current, IItemHandler handler,
                                           String weaponKey, Component weaponName,
                                           int consumerIndex, Object gunData, Object consumer)
            throws ReflectiveOperationException {
        if (consumer == null || !current.ammoConsumerType.isInstance(consumer)) {
            return null;
        }
        current.initializeConsumer.invoke(consumer);
        String type = String.valueOf(current.getConsumerType.invoke(consumer));
        if (!"ITEM".equals(type) && !"PLAYER_AMMO".equals(type)) {
            return null;
        }
        Object rawTemplate = current.getConsumerStack.invoke(consumer);
        if (!(rawTemplate instanceof ItemStack template) || template.isEmpty()) {
            return null;
        }
        int roundsPerPackage = Math.max(1,
                ((Number) current.getLoadAmount.invoke(consumer)).intValue());
        int storedPackages = Math.max(0,
                ((Number) current.countPackages.invoke(consumer, gunData, handler)).intValue());
        int packageCapacity = packageCapacity(handler, template);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(template.getItem());
        String descriptor = String.valueOf(current.getAmmoDescriptor.invoke(consumer));
        String classificationKey = itemId + ":" + descriptor + ":" + weaponKey;
        long storedRounds = (long) storedPackages * roundsPerPackage;
        return new AmmoTarget(weaponKey, consumerIndex, weaponName,
                template.getHoverName(), classificationKey, roundsPerPackage,
                (int) Math.min(Integer.MAX_VALUE, storedRounds), packageCapacity,
                handler, template.copy());
    }

    private static int packageCapacity(IItemHandler handler, ItemStack template) {
        long capacity = 0L;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack existing = handler.getStackInSlot(slot);
            int limit = Math.min(handler.getSlotLimit(slot), template.getMaxStackSize());
            if (existing.isEmpty()) {
                if (handler.isItemValid(slot, template)) {
                    capacity += limit;
                }
            } else if (ItemStack.isSameItemSameTags(existing, template)) {
                capacity += Math.max(0, limit - existing.getCount());
            }
            if (capacity >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) capacity;
    }

    private static Bridge bridge() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return null;
        }
        if (!initialized) {
            synchronized (SbwVehicleAmmoAdapter.class) {
                if (!initialized) {
                    bridge = createBridge();
                    initialized = true;
                }
            }
        }
        return bridge;
    }

    private static Bridge createBridge() {
        try {
            Class<?> vehicleType = Class.forName(VEHICLE_CLASS);
            Class<?> gunDataType = Class.forName(GUN_DATA_CLASS);
            Class<?> defaultGunDataType = Class.forName(DEFAULT_GUN_DATA_CLASS);
            Class<?> ammoConsumerType = Class.forName(AMMO_CONSUMER_CLASS);
            return new Bridge(vehicleType, gunDataType, ammoConsumerType,
                    vehicleType.getMethod("getGunDataMap"),
                    gunDataType.getMethod("compute"),
                    defaultGunDataType.getMethod("getProcessedAmmoConsumers"),
                    defaultGunDataType.getMethod("getName"),
                    ammoConsumerType.getMethod("init"),
                    ammoConsumerType.getMethod("getType"),
                    ammoConsumerType.getMethod("stack"),
                    ammoConsumerType.getMethod("getAmmo"),
                    ammoConsumerType.getMethod("getLoadAmount"),
                    ammoConsumerType.getMethod("count", gunDataType,
                            IItemHandler.class));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce(exception);
            return null;
        }
    }

    private static synchronized void logFailureOnce(Exception exception) {
        if (!failureLogged) {
            failureLogged = true;
            WokInfantryMod.LOGGER.error(
                    "SBW 0.8.9 vehicle ammunition integration failed; manual vehicle supply disabled",
                    exception);
        }
    }

    public record AmmoTarget(String weaponKey, int consumerIndex, Component weaponName,
                             Component ammunitionName, String classificationKey,
                             int roundsPerPackage, int storedRounds, int packageCapacity,
                             IItemHandler handler, ItemStack template) {
    }

    private record Bridge(Class<?> vehicleType, Class<?> gunDataType,
                          Class<?> ammoConsumerType, Method getGunDataMap,
                          Method computeGunData, Method getAmmoConsumers,
                          Method getWeaponName, Method initializeConsumer,
                          Method getConsumerType, Method getConsumerStack,
                          Method getAmmoDescriptor, Method getLoadAmount,
                          Method countPackages) {
    }
}
