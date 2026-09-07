package com.wok.vehiclehealth.module;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.wok.vehiclehealth.config.VehicleModuleConfig;
import com.wok.vehiclehealth.balance.VehicleBalanceProfiles;
import net.minecraft.nbt.CompoundTag;

public final class VehicleOpticsData {
    private static final String ROOT_KEY = "WokVehicleHealth";
    private static final String CURRENT_KEY = "OpticsHealth";
    private static final String MAX_KEY = "OpticsMaxHealth";

    public static float current(VehicleEntity vehicle) {
        CompoundTag data = normalize(vehicle);
        return data.getFloat(CURRENT_KEY);
    }

    public static float maximum(VehicleEntity vehicle) {
        return normalize(vehicle).getFloat(MAX_KEY);
    }

    public static float damage(VehicleEntity vehicle, float amount) {
        CompoundTag data = normalize(vehicle);
        float next = VehicleModuleMath.clamp(
                data.getFloat(CURRENT_KEY) - Math.max(0.0F, amount),
                0.0F,
                data.getFloat(MAX_KEY));
        data.putFloat(CURRENT_KEY, next);
        save(vehicle, data);
        return next;
    }

    public static void repairTick(VehicleEntity vehicle) {
        if (vehicle.isWreck()) {
            return;
        }
        CompoundTag data = normalize(vehicle);
        float maximum = data.getFloat(MAX_KEY);
        float current = data.getFloat(CURRENT_KEY);
        if (current >= maximum) {
            return;
        }
        float repair = (float) (maximum * VehicleModuleConfig.OPTICS_PASSIVE_REPAIR_RATE.get());
        data.putFloat(CURRENT_KEY, Math.min(maximum, current + repair));
        save(vehicle, data);
    }

    private static CompoundTag normalize(VehicleEntity vehicle) {
        CompoundTag persistent = vehicle.getPersistentData();
        CompoundTag data = persistent.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)
                ? persistent.getCompound(ROOT_KEY)
                : new CompoundTag();

        float desiredMaximum = VehicleBalanceProfiles.find(vehicle)
                .map(profile -> profile.opticsHealth())
                .orElseGet(() -> Math.max(
                        VehicleModuleConfig.OPTICS_MIN_MAX_HEALTH.get().floatValue(),
                        vehicle.getTurretMaxHealth()
                                * VehicleModuleConfig.OPTICS_MAX_HEALTH_RATIO.get().floatValue()));
        float storedMaximum = data.getFloat(MAX_KEY);
        if (!(storedMaximum > 0.0F)) {
            data.putFloat(MAX_KEY, desiredMaximum);
            data.putFloat(CURRENT_KEY, desiredMaximum);
        } else if (Math.abs(storedMaximum - desiredMaximum) > 0.001F) {
            float ratio = VehicleModuleMath.clamp(data.getFloat(CURRENT_KEY) / storedMaximum, 0.0F, 1.0F);
            data.putFloat(MAX_KEY, desiredMaximum);
            data.putFloat(CURRENT_KEY, desiredMaximum * ratio);
        } else {
            data.putFloat(CURRENT_KEY,
                    VehicleModuleMath.clamp(data.getFloat(CURRENT_KEY), 0.0F, storedMaximum));
        }
        save(vehicle, data);
        return data;
    }

    private static void save(VehicleEntity vehicle, CompoundTag data) {
        vehicle.getPersistentData().put(ROOT_KEY, data);
    }

    private VehicleOpticsData() {
    }
}
