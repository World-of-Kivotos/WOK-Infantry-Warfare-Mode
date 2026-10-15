package com.wok.downed.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DragLeashTest {
    @Test
    void slackRopeLeavesTheCasualtyWhereItLies() {
        assertNull(DragLeash.follow(0.0D, 0.0D, 0.0D, -1.0D, 1.15D));
        assertNull(DragLeash.follow(0.0D, 0.0D, 0.0D, -1.15D, 1.15D));
    }

    @Test
    void tautRopePullsAlongTheCarrierToCasualtyLine() {
        assertArrayEquals(new double[]{0.0D, -1.15D},
                DragLeash.follow(0.0D, 0.0D, 0.0D, -3.0D, 1.15D), 1.0E-9D);
        assertArrayEquals(new double[]{-0.6D, -0.8D},
                DragLeash.follow(0.0D, 0.0D, -3.0D, -4.0D, 1.0D), 1.0E-9D);
    }

    @Test
    void turningAroundDoesNotSwingTheCasualtyBehindTheCarrier() {
        // The old drag pinned the casualty behind the carrier's look direction every tick, so it
        // could never be aimed at. The rope ignores where the carrier looks: facing the casualty
        // without walking leaves it in front of the carrier.
        assertNull(DragLeash.follow(5.0D, 5.0D, 5.0D, 3.85D, 1.15D));
    }

    @Test
    void walkingBackwardsKeepsTheCasualtyInFront() {
        double[] pulled = DragLeash.follow(0.0D, 1.3D, 0.0D, 0.0D, 1.15D);
        assertArrayEquals(new double[]{0.0D, 0.15D}, pulled, 1.0E-9D);
    }

    @Test
    void yawFacesTheCarrier() {
        assertEquals(0.0F, DragLeash.yawToward(0.0D, 0.0D, 0.0D, 2.0D), 1.0E-4F);
        assertEquals(90.0F, Math.floorMod(Math.round(
                DragLeash.yawToward(0.0D, 0.0D, -2.0D, 0.0D)), 360), 1.0E-4F);
        assertEquals(180.0F, Math.floorMod(Math.round(
                DragLeash.yawToward(0.0D, 0.0D, 0.0D, -2.0D)), 360), 1.0E-4F);
    }

    @Test
    void ropeSnapsOnlyBeyondRescueRangePlusOneBlock() {
        assertFalse(DragLeash.snapped(16.0D, 1.15D, 3.0D));
        assertTrue(DragLeash.snapped(16.01D, 1.15D, 3.0D));
        assertFalse(DragLeash.snapped(12.25D, 2.5D, 1.0D));
        assertTrue(DragLeash.snapped(12.26D, 2.5D, 1.0D));
    }
}
