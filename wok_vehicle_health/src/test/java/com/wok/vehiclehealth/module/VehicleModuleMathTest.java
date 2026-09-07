package com.wok.vehiclehealth.module;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehicleModuleMathTest {
    @Test
    void fullHealthKeepsNormalTurretSpeed() {
        assertEquals(1.0F,
                VehicleModuleMath.rotationMultiplier(100.0F, 100.0F, 0.25F, 0.08F));
    }

    @Test
    void meaningfulRingDamageSlowsTurretSharply() {
        assertEquals(0.25F,
                VehicleModuleMath.rotationMultiplier(90.0F, 100.0F, 0.25F, 0.08F));
    }

    @Test
    void destroyedRingUsesEmergencyTraverseRate() {
        assertEquals(0.08F,
                VehicleModuleMath.rotationMultiplier(0.0F, 100.0F, 0.25F, 0.08F));
    }

    @Test
    void wrappedRotationTakesShortestPath() {
        assertEquals(182.5F,
                VehicleModuleMath.limitWrappedDegrees(179.0F, -171.0F, 0.35F),
                0.0001F);
    }

    @Test
    void cameraAnglesFollowActualWorldSpaceTurretVector() {
        assertEquals(0.0F, VehicleModuleMath.cameraYaw(0.0D, 1.0D), 0.0001F);
        assertEquals(-90.0F, VehicleModuleMath.cameraYaw(1.0D, 0.0D), 0.0001F);
        assertEquals(-45.0F,
                VehicleModuleMath.cameraPitch(0.0D, 1.0D, 1.0D), 0.0001F);
    }

    @Test
    void classifiesTrackedEngineAtRearAndWheeledEngineAtFront() {
        assertEquals(VehicleModulePart.ENGINE_MAIN,
                VehicleModuleMath.classifyGroundFallback(
                        false, false, 0.0D, -3.0D, 0.5D, 2.0D, 4.0D));
        assertEquals(VehicleModulePart.NONE,
                VehicleModuleMath.classifyGroundFallback(
                        false, false, 0.0D, 3.0D, 0.5D, 2.0D, 4.0D));
        assertEquals(VehicleModulePart.ENGINE_MAIN,
                VehicleModuleMath.classifyGroundFallback(
                        true, false, 0.0D, 3.0D, 0.5D, 2.0D, 4.0D));
    }

    @Test
    void prioritizesLowSideRunningGearAndHighTurret() {
        assertEquals(VehicleModulePart.TRACK_LEFT,
                VehicleModuleMath.classifyGroundFallback(
                        true, true, 1.5D, 2.8D, 0.2D, 2.0D, 4.0D));
        assertEquals(VehicleModulePart.TRACK_RIGHT,
                VehicleModuleMath.classifyGroundFallback(
                        false, true, -1.5D, -2.8D, 0.2D, 2.0D, 4.0D));
        assertEquals(VehicleModulePart.TURRET_RING,
                VehicleModuleMath.classifyGroundFallback(
                        false, true, 0.0D, 0.0D, 0.7D, 2.0D, 4.0D));
    }
}
