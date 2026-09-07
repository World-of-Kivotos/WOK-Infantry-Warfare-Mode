package com.wok.vehiclehealth.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehicleBalanceStateTest {
    @Test
    void preservesHealthRatioDuringProfileMigration() {
        assertEquals(400.0F, VehicleBalanceState.scale(250.0F, 500.0F, 800.0F));
        assertEquals(80.0F, VehicleBalanceState.scale(25.0F, 50.0F, 160.0F));
        assertEquals(0.0F, VehicleBalanceState.scale(50.0F, 50.0F, 0.0F));
    }
}
