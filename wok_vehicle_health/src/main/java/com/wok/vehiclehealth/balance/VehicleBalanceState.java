package com.wok.vehiclehealth.balance;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.nbt.CompoundTag;

/** One-time, ratio-preserving migration from addon-native health pools to WOK tier profiles. */
public final class VehicleBalanceState {
    private static final String ROOT_KEY = "WokVehicleBalance";
    private static final String VERSION_KEY = "ProfileVersion";
    private static final int PROFILE_VERSION = 1;
    private static final float LEGACY_PART_MAX = 50.0F;

    public static void applyProfile(VehicleEntity vehicle) {
        VehicleBalanceProfile profile = VehicleBalanceProfiles.find(vehicle).orElse(null);
        if (profile == null) {
            return;
        }
        CompoundTag root = vehicle.getPersistentData().getCompound(ROOT_KEY);
        if (root.getInt(VERSION_KEY) >= PROFILE_VERSION) {
            return;
        }

        float legacyHullMax = Math.max(1.0F, vehicle.computed().getMaxHealth());
        vehicle.setHealth(scale(vehicle.getHealth(), legacyHullMax, profile.hullHealth()));
        vehicle.setLeftWheelHealth(scale(
                vehicle.getLeftWheelHealth(), LEGACY_PART_MAX, profile.runningGearHealth()));
        vehicle.setRightWheelHealth(scale(
                vehicle.getRightWheelHealth(), LEGACY_PART_MAX, profile.runningGearHealth()));
        vehicle.setMainEngineHealth(scale(
                vehicle.getMainEngineHealth(), LEGACY_PART_MAX, profile.engineHealth()));
        vehicle.setSubEngineHealth(scale(
                vehicle.getSubEngineHealth(), LEGACY_PART_MAX, profile.engineHealth()));
        vehicle.setTurretHealth(scale(
                vehicle.getTurretHealth(), LEGACY_PART_MAX, profile.turretHealth()));

        root.putInt(VERSION_KEY, PROFILE_VERSION);
        vehicle.getPersistentData().put(ROOT_KEY, root);
    }

    static float scale(float current, float previousMaximum, float nextMaximum) {
        if (!(nextMaximum > 0.0F)) {
            return 0.0F;
        }
        float ratio = VehicleBalanceMathClamp.clamp(
                current / Math.max(1.0F, previousMaximum), 0.0F, 1.0F);
        return nextMaximum * ratio;
    }

    private static final class VehicleBalanceMathClamp {
        private static float clamp(float value, float minimum, float maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }
    }

    private VehicleBalanceState() {
    }
}
