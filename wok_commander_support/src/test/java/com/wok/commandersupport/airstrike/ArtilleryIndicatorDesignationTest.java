package com.wok.commandersupport.airstrike;

import com.wok.commandersupport.airstrike.ArtilleryIndicatorDesignation.BlockRay;
import com.wok.commandersupport.airstrike.ArtilleryIndicatorDesignation.RayEnd;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtilleryIndicatorDesignationTest {
    private static final double PERMIT = 64.0D;

    @Test
    void reachCoversTheFarEdgeOfThePermitPlusMargin() {
        assertEquals(16.0D, ArtilleryIndicatorDesignation.RAY_MARGIN);
        // Standing on the permit centre: radius + margin.
        assertEquals(80.0D, ArtilleryIndicatorDesignation.horizontalReach(
                10.0D, -20.0D, 10.0D, -20.0D, PERMIT), 1.0E-9D);
        // 50 blocks away (3-4-5): distance + radius + margin.
        assertEquals(130.0D, ArtilleryIndicatorDesignation.horizontalReach(
                0.0D, 0.0D, 30.0D, 40.0D, PERMIT), 1.0E-9D);
    }

    @Test
    void reachNeverExceedsTheIndicatorRange() {
        assertEquals(ArtilleryIndicatorDesignation.MAX_RANGE,
                ArtilleryIndicatorDesignation.horizontalReach(
                        0.0D, 0.0D, 432.0D, 0.0D, PERMIT));
        assertEquals(ArtilleryIndicatorDesignation.MAX_RANGE,
                ArtilleryIndicatorDesignation.horizontalReach(
                        0.0D, 0.0D, 5_000.0D, 0.0D, PERMIT));
    }

    @Test
    void invalidInputMeansNoRay() {
        assertEquals(0.0D, ArtilleryIndicatorDesignation.horizontalReach(
                Double.NaN, 0.0D, 0.0D, 0.0D, PERMIT));
        assertEquals(0.0D, ArtilleryIndicatorDesignation.horizontalReach(
                0.0D, 0.0D, Double.POSITIVE_INFINITY, 0.0D, PERMIT));
        assertEquals(0.0D, ArtilleryIndicatorDesignation.horizontalReach(
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
        assertEquals(0.0D, ArtilleryIndicatorDesignation.horizontalReach(
                0.0D, 0.0D, 0.0D, 0.0D, Double.NaN));
        assertEquals(0.0D, ArtilleryIndicatorDesignation.rayLength(
                new Vec3(1.0D, 0.0D, 0.0D), 0.0D));
        assertEquals(0.0D, ArtilleryIndicatorDesignation.rayLength(
                new Vec3(1.0D, 0.0D, 0.0D), Double.NaN));
        assertEquals(0.0D, ArtilleryIndicatorDesignation.rayLength(Vec3.ZERO, 80.0D));
        assertEquals(0.0D, ArtilleryIndicatorDesignation.rayLength(null, 80.0D));
        assertEquals(0.0D, ArtilleryIndicatorDesignation.rayLength(
                new Vec3(Double.NaN, 0.0D, 0.0D), 80.0D));
    }

    @Test
    void levelLookTravelsExactlyTheReach() {
        assertEquals(80.0D, ArtilleryIndicatorDesignation.rayLength(
                new Vec3(0.0D, 0.0D, 1.0D), 80.0D), 1.0E-9D);
        // A non-unit direction gives the same length.
        assertEquals(80.0D, ArtilleryIndicatorDesignation.rayLength(
                new Vec3(0.0D, 0.0D, -3.0D), 80.0D), 1.0E-9D);
    }

    @Test
    void steepLookStaysInTheSameColumnsAndStillReachesTheGroundBelow() {
        // 45 degrees down: 80 blocks sideways means 80 * sqrt(2) along the ray.
        assertEquals(80.0D * Math.sqrt(2.0D), ArtilleryIndicatorDesignation.rayLength(
                new Vec3(1.0D, -1.0D, 0.0D).normalize(), 80.0D), 1.0E-9D);
        // Straight down or up crosses no chunk border, so the full range is allowed.
        assertEquals(ArtilleryIndicatorDesignation.MAX_RANGE,
                ArtilleryIndicatorDesignation.rayLength(new Vec3(0.0D, -1.0D, 0.0D), 80.0D));
        assertEquals(ArtilleryIndicatorDesignation.MAX_RANGE,
                ArtilleryIndicatorDesignation.rayLength(new Vec3(0.0D, 1.0D, 0.0D), 80.0D));
        assertEquals(ArtilleryIndicatorDesignation.MAX_RANGE,
                ArtilleryIndicatorDesignation.rayLength(
                        new Vec3(0.01D, -1.0D, 0.0D).normalize(), 80.0D));
    }

    @Test
    void horizontalTravelNeverExceedsTheReachInAnyDirection() {
        double[] reaches = {16.0D, 80.0D, 200.0D, ArtilleryIndicatorDesignation.MAX_RANGE};
        for (int yaw = 0; yaw < 360; yaw += 15) {
            for (int pitch = -90; pitch <= 90; pitch += 5) {
                double yawRadians = Math.toRadians(yaw);
                double pitchRadians = Math.toRadians(pitch);
                Vec3 direction = new Vec3(
                        -Math.sin(yawRadians) * Math.cos(pitchRadians),
                        -Math.sin(pitchRadians),
                        Math.cos(yawRadians) * Math.cos(pitchRadians));
                for (double reach : reaches) {
                    double length = ArtilleryIndicatorDesignation.rayLength(direction, reach);
                    Vec3 travel = direction.normalize().scale(length);
                    double horizontal = Math.sqrt(travel.x * travel.x + travel.z * travel.z);
                    String where = "yaw " + yaw + " pitch " + pitch + " reach " + reach;
                    assertTrue(horizontal <= reach + 1.0E-6D, where);
                    assertTrue(length >= reach - 1.0E-6D, where);
                    assertTrue(length <= ArtilleryIndicatorDesignation.MAX_RANGE + 1.0E-9D,
                            where);
                }
            }
        }
    }

    @Test
    void onlyABlockStrikeOrAnEntityInFrontIsALaserSpot() {
        Vec3 block = new Vec3(5.0D, 64.0D, 5.0D);
        Vec3 entity = new Vec3(3.0D, 65.0D, 3.0D);

        assertEquals(Optional.of(block), ArtilleryIndicatorDesignation.laserSpot(
                new BlockRay(RayEnd.BLOCK, block), null));
        assertEquals(Optional.of(entity), ArtilleryIndicatorDesignation.laserSpot(
                new BlockRay(RayEnd.BLOCK, block), entity));
        // Open sky is not a spot: the old ray end in mid-air no longer counts.
        assertTrue(ArtilleryIndicatorDesignation.laserSpot(
                new BlockRay(RayEnd.OPEN, block), null).isEmpty());
        // A ray cut by an unloaded chunk has no spot of its own...
        assertTrue(ArtilleryIndicatorDesignation.laserSpot(
                new BlockRay(RayEnd.UNLOADED, block), null).isEmpty());
        // ...but an entity hit before the cut still counts.
        assertEquals(Optional.of(entity), ArtilleryIndicatorDesignation.laserSpot(
                new BlockRay(RayEnd.UNLOADED, block), entity));
        assertEquals(Optional.of(entity), ArtilleryIndicatorDesignation.laserSpot(
                new BlockRay(RayEnd.OPEN, block), entity));
    }
}
