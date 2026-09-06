package com.wok.vehiclehealth.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VehicleBalanceProfilesTest {
    @Test
    void assignsCurrentFormationVehiclesToCategorySpecificTiers() {
        VehicleBalanceProfile leopard = profile("dragonrise_reforge:leopard2a4");
        assertEquals(VehicleCategory.TANK, leopard.category());
        assertEquals(VehicleTier.T2, leopard.tier());
        assertEquals(650.0F, leopard.hullHealth());
        assertEquals(260.0F, leopard.tankGunArmorDamage());

        VehicleBalanceProfile abrams = profile("dragonrise_reforge:m1a2sepv2");
        assertEquals(VehicleTier.T3, abrams.tier());
        assertEquals(800.0F, abrams.hullHealth());
        assertEquals(340.0F, abrams.tankGunArmorDamage());

        VehicleBalanceProfile cv90 = profile("dragonrise_reforge:cv90");
        assertEquals(VehicleCategory.ARMORED_FIGHTING_VEHICLE, cv90.category());
        assertEquals(VehicleTier.T2, cv90.tier());

        VehicleBalanceProfile littleBird = profile("fcp:littlebird_armed");
        assertEquals(VehicleCategory.HELICOPTER, littleBird.category());
        assertEquals(120.0F, littleBird.hullHealth());
    }

    @Test
    void leavesUnknownThirdPartyVehiclesUntouched() {
        assertFalse(VehicleBalanceProfiles.find("third_party:unknown_vehicle").isPresent());
    }

    private static VehicleBalanceProfile profile(String id) {
        return VehicleBalanceProfiles.find(id).orElseThrow();
    }
}
