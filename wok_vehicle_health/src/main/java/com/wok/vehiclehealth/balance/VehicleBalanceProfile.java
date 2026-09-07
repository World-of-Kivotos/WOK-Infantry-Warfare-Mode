package com.wok.vehiclehealth.balance;

public record VehicleBalanceProfile(
        String entityId,
        VehicleCategory category,
        VehicleTier tier,
        float hullHealth,
        float runningGearHealth,
        float engineHealth,
        float turretHealth,
        float opticsHealth,
        float tankGunArmorDamage) {

    public VehicleBalanceProfile {
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("entityId must not be blank");
        }
        if (category == null || tier == null) {
            throw new IllegalArgumentException("category and tier are required");
        }
        if (!(hullHealth > 0.0F)) {
            throw new IllegalArgumentException("hullHealth must be positive");
        }
        if (runningGearHealth < 0.0F || engineHealth < 0.0F || turretHealth < 0.0F
                || opticsHealth < 0.0F || tankGunArmorDamage < 0.0F) {
            throw new IllegalArgumentException("module and weapon values must not be negative");
        }
    }

    public boolean hasNormalizedTankGun() {
        return tankGunArmorDamage > 0.0F;
    }
}
