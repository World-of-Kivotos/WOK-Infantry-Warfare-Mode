package com.wok.vehiclehealth.integration.sbw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SbwVehicleModuleEventsTest {
    @Test
    void recognizesTaczProjectileExplosionWithoutHardRuntimeDependency() {
        assertTrue(SbwVehicleModuleEvents.isTaczExplosionClass(
                "com.tacz.guns.util.block.ProjectileExplosion"));
        assertFalse(SbwVehicleModuleEvents.isTaczExplosionClass(
                "net.minecraft.world.level.Explosion"));
        assertFalse(SbwVehicleModuleEvents.isTaczExplosionClass(null));
    }
}
