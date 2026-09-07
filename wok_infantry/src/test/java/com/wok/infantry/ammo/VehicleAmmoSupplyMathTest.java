package com.wok.infantry.ammo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VehicleAmmoSupplyMathTest {
    @Test
    void alignsRequestedRoundsToWholePackages() {
        assertEquals(new VehicleAmmoSupplyMath.Plan(2, 8, 400),
                VehicleAmmoSupplyMath.plan(5, 4, 50, 1_500, 20));
    }

    @Test
    void respectsPointBalance() {
        assertEquals(new VehicleAmmoSupplyMath.Plan(1, 4, 200),
                VehicleAmmoSupplyMath.plan(12, 4, 50, 399, 20));
    }

    @Test
    void respectsVehiclePackageCapacity() {
        assertEquals(new VehicleAmmoSupplyMath.Plan(2, 200, 200),
                VehicleAmmoSupplyMath.plan(500, 100, 1, 1_500, 2));
    }

    @Test
    void rejectsInvalidOrUnaffordableTransactions() {
        assertEquals(new VehicleAmmoSupplyMath.Plan(0, 0, 0),
                VehicleAmmoSupplyMath.plan(1, 4, 50, 199, 20));
        assertEquals(new VehicleAmmoSupplyMath.Plan(0, 0, 0),
                VehicleAmmoSupplyMath.plan(0, 4, 50, 1_500, 20));
    }
}
