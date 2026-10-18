package com.wok.vehiclehealth.balance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Authoritative WOK Infantry category-and-tier assignments for fielded vehicles. */
public final class VehicleBalanceProfiles {
    private static final Map<String, VehicleBalanceProfile> PROFILES = createProfiles();

    public static Optional<VehicleBalanceProfile> find(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return find(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    public static Optional<VehicleBalanceProfile> find(String entityId) {
        return Optional.ofNullable(PROFILES.get(entityId));
    }

    public static Map<String, VehicleBalanceProfile> all() {
        return PROFILES;
    }

    private static Map<String, VehicleBalanceProfile> createProfiles() {
        Map<String, VehicleBalanceProfile> profiles = new LinkedHashMap<>();

        // Tanks: T2 Leopard 2A4 and T3 M1A2 SEP V2 are the current formation anchors.
        register(profiles, new VehicleBalanceProfile(
                "dragonrise_reforge:leopard2a4", VehicleCategory.TANK, VehicleTier.T2,
                650.0F, 130.0F, 150.0F, 190.0F, 60.0F, 260.0F));
        register(profiles, new VehicleBalanceProfile(
                "dragonrise_reforge:m1a2sepv2", VehicleCategory.TANK, VehicleTier.T3,
                800.0F, 160.0F, 180.0F, 240.0F, 75.0F, 340.0F));

        // Armored fighting vehicles. MGS keeps a T1 hull but receives a T2 gun profile.
        register(profiles, new VehicleBalanceProfile(
                "fcp:stryker_dragoon", VehicleCategory.ARMORED_FIGHTING_VEHICLE, VehicleTier.T1,
                180.0F, 60.0F, 70.0F, 75.0F, 35.0F, 0.0F));
        register(profiles, new VehicleBalanceProfile(
                "fcp:stryker_mgs", VehicleCategory.ARMORED_FIGHTING_VEHICLE, VehicleTier.T1,
                180.0F, 60.0F, 70.0F, 75.0F, 35.0F, 260.0F));
        register(profiles, new VehicleBalanceProfile(
                "dragonrise_reforge:m3a3", VehicleCategory.ARMORED_FIGHTING_VEHICLE, VehicleTier.T2,
                440.0F, 110.0F, 120.0F, 145.0F, 55.0F, 0.0F));
        register(profiles, new VehicleBalanceProfile(
                "dragonrise_reforge:cv90", VehicleCategory.ARMORED_FIGHTING_VEHICLE, VehicleTier.T2,
                300.0F, 85.0F, 95.0F, 110.0F, 45.0F, 0.0F));

        // Light tactical vehicles.
        register(profiles, new VehicleBalanceProfile(
                "fcp:hmmwv_unarmored_unarmed", VehicleCategory.LIGHT_TACTICAL, VehicleTier.T1,
                80.0F, 25.0F, 30.0F, 0.0F, 0.0F, 0.0F));
        register(profiles, new VehicleBalanceProfile(
                "fcp:hmmwv_armored_unarmed", VehicleCategory.LIGHT_TACTICAL, VehicleTier.T2,
                120.0F, 35.0F, 45.0F, 0.0F, 0.0F, 0.0F));
        register(profiles, new VehicleBalanceProfile(
                "fcp:hmmwv_armored_m2", VehicleCategory.LIGHT_TACTICAL, VehicleTier.T2,
                120.0F, 35.0F, 45.0F, 40.0F, 25.0F, 0.0F));

        // The armed Little Bird is a T1 helicopter, not a flying armored vehicle.
        register(profiles, new VehicleBalanceProfile(
                "fcp:littlebird_armed", VehicleCategory.HELICOPTER, VehicleTier.T1,
                120.0F, 0.0F, 45.0F, 0.0F, 25.0F, 0.0F));

        return Map.copyOf(profiles);
    }

    private static void register(Map<String, VehicleBalanceProfile> profiles,
                                 VehicleBalanceProfile profile) {
        if (profiles.put(profile.entityId(), profile) != null) {
            throw new IllegalStateException("Duplicate vehicle balance profile: " + profile.entityId());
        }
    }

    private VehicleBalanceProfiles() {
    }
}
