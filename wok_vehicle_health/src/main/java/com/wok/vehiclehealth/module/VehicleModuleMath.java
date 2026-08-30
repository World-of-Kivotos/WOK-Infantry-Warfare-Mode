package com.wok.vehiclehealth.module;

public final class VehicleModuleMath {
    public static float rotationMultiplier(float currentHealth, float maxHealth,
                                           float damagedMultiplier, float destroyedMultiplier) {
        if (!(maxHealth > 0.0F)) {
            return 1.0F;
        }
        if (currentHealth <= 0.0F) {
            return clamp01(destroyedMultiplier);
        }
        if (currentHealth < maxHealth * 0.95F) {
            return clamp01(damagedMultiplier);
        }
        return 1.0F;
    }

    public static float limitWrappedDegrees(float before, float after, float multiplier) {
        float delta = wrapDegrees(after - before);
        return before + delta * clamp01(multiplier);
    }

    public static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static float positiveLoss(float before, float after) {
        return Math.max(0.0F, before - after);
    }

    /** Minecraft camera yaw for a world-space forward vector. */
    public static float cameraYaw(double x, double z) {
        return (float) -Math.toDegrees(Math.atan2(x, z));
    }

    /** Minecraft camera pitch for a world-space forward vector. */
    public static float cameraPitch(double x, double y, double z) {
        double horizontal = Math.sqrt(x * x + z * z);
        return (float) -Math.toDegrees(Math.atan2(y, horizontal));
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    private static float clamp01(float value) {
        return clamp(value, 0.0F, 1.0F);
    }

    private VehicleModuleMath() {
    }
}
