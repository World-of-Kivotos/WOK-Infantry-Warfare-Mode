package com.wok.bodyhealth.prone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BodyYawSimTest {
    @Test
    void movingTurnsTheBodyTowardsTheMotion() {
        // Moving +X is a heading of -90; the body follows by 30% per tick.
        assertEquals(-27.0F, BodyYawSim.step(0.0F, 0.0F, 0.065D, 0.0D, false), 1.0E-4F);
    }

    @Test
    void standingStillOnlyDragsTheBodyWithinFiftyDegrees() {
        assertEquals(30.0F, BodyYawSim.step(0.0F, 80.0F, 0.0D, 0.0D, false), 1.0E-4F);
        assertEquals(-30.0F, BodyYawSim.step(0.0F, -80.0F, 0.0D, 0.0D, false), 1.0E-4F);
        assertEquals(10.0F, BodyYawSim.step(10.0F, 30.0F, 0.0D, 0.0D, false), 1.0E-4F);
    }

    @Test
    void tinyMovesCountAsStandingStill() {
        // 0.05^2 = 0.0025 is not above vanilla's 0.0025000002 threshold.
        assertEquals(10.0F, BodyYawSim.step(10.0F, 30.0F, 0.05D, 0.0D, false), 1.0E-4F);
        assertEquals(30.0F, BodyYawSim.step(10.0F, 80.0F, 0.0D, 0.05D, false), 1.0E-4F);
    }

    @Test
    void walkingBackwardsKeepsFacingForwards() {
        // Heading -90 is 210 degrees from yRot 120, so the target flips to -270 (= 90).
        assertEquals(111.0F, BodyYawSim.step(120.0F, 120.0F, 0.065D, 0.0D, false), 1.0E-4F);
        // Sideways (90 degrees off) does not flip.
        assertEquals(-27.0F, BodyYawSim.step(0.0F, 0.0F, 0.065D, 0.0D, false), 1.0E-4F);
    }

    @Test
    void swingingFacesTheView() {
        assertEquals(12.0F, BodyYawSim.step(0.0F, 40.0F, 0.065D, 0.0D, true), 1.0E-4F);
        assertEquals(27.0F, BodyYawSim.step(0.0F, 40.0F, 0.065D, 0.0D, false), 1.0E-4F);
    }

    @Test
    void wrapsAcrossTheSeam() {
        // Body at 170 following a view of -170 takes the short way.
        float next = BodyYawSim.step(170.0F, -170.0F, 0.0D, 0.0D, true);
        assertEquals(176.0F, next, 1.0E-4F);
    }
}
