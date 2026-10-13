package com.wok.commandersupport.airstrike;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PavewayGuidancePlanTest {
    @Test
    void definitionUsesTenSecondInboundAndThreeSecondTickGuidance() {
        SupportDefinition definition = WokCommanderSupportMod.f16cPavewayDefinition();

        assertEquals(WokCommanderSupportMod.F16C_PAVEWAY_ID, definition.id());
        assertEquals(SupportTargetMode.POINT, definition.targetMode());
        assertEquals(12_000L, definition.cooldownTicks());
        assertEquals(200L, definition.inboundTicks());
        assertEquals(61, definition.stepCount());
        assertEquals(1, definition.stepIntervalTicks());
        assertEquals(80.0D, definition.radius());
        assertTrue(definition.requiresLoadedFootprint());
    }

    @Test
    void dangerRadiusIsThePermitPlusTheBlastRadius() {
        assertEquals(64.0D, WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS);
        assertEquals(WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS
                        + F16PavewayProvider.EXPLOSION_RADIUS,
                WokCommanderSupportMod.F16C_PAVEWAY_DANGER_RADIUS);
        assertEquals(WokCommanderSupportMod.F16C_PAVEWAY_DANGER_RADIUS,
                WokCommanderSupportMod.f16cPavewayDefinition().radius());
    }

    @Test
    void missingDesignationReturnsTheCooldownWithoutBreakingTheProvider() {
        SupportSpawnException failure = F16PavewayProvider.noDesignation();

        assertEquals("许可区内没有友军持续照射", failure.getMessage());
        assertTrue(failure.refundCooldown());
        assertFalse(failure.providerBroken());
    }

    @Test
    void diagonalFlightIsDragCompensatedForSixtyTicks() {
        Vec3 target = new Vec3(100.5D, 70.25D, -20.5D);
        PavewayGuidancePlan plan = PavewayGuidancePlan.fromDesignation(target, 60);
        Vec3 destination = PavewayGuidancePlan.guidancePoint(target);
        Vec3 velocity = PavewayGuidancePlan.velocityTo(plan.spawn(), destination, 60);
        Vec3 simulated = plan.spawn();
        for (int tick = 0; tick < 60; tick++) {
            simulated = simulated.add(velocity);
            velocity = velocity.scale(1.0D - PavewayGuidancePlan.CBC_LINEAR_DRAG);
        }
        assertEquals(destination.x, simulated.x, 1.0E-8D);
        assertEquals(destination.y, simulated.y, 1.0E-8D);
        assertEquals(destination.z, simulated.z, 1.0E-8D);
        assertNotEquals(0.0D, plan.spawn().x - target.x);
        assertNotEquals(0.0D, plan.spawn().z - target.z);
    }

    @Test
    void releaseCandidatesCoverAllFourDiagonalsNearestTheDesignatorFirst() {
        Vec3 spot = new Vec3(100.0D, 64.0D, 200.0D);
        // Designator 40 blocks south-east of the spot, as in a typical permit-edge laser.
        List<PavewayGuidancePlan> candidates = PavewayGuidancePlan.releaseCandidates(spot,
                new Vec3(130.0D, 70.0D, 230.0D), 60);

        assertEquals(4, candidates.size());
        assertEquals(new Vec3(196.0D, 184.0D, 296.0D), candidates.get(0).spawn(),
                "the south-east release point is tried first");
        assertEquals(new Vec3(4.0D, 184.0D, 104.0D), candidates.get(3).spawn(),
                "the opposite north-west point is the last resort");
        Set<Vec3> spawns = new HashSet<>();
        for (PavewayGuidancePlan candidate : candidates) {
            spawns.add(candidate.spawn());
            assertEquals(60, candidate.flightTicks());
            assertEquals(96.0D, Math.abs(candidate.spawn().x - spot.x), 1.0E-9D);
            assertEquals(120.0D, candidate.spawn().y - spot.y, 1.0E-9D);
            assertEquals(96.0D, Math.abs(candidate.spawn().z - spot.z), 1.0E-9D);
        }
        assertEquals(4, spawns.size());
    }

    @Test
    void withoutADesignatorPositionTheOriginalNorthWestApproachComesFirst() {
        Vec3 spot = new Vec3(0.0D, 64.0D, 0.0D);
        List<PavewayGuidancePlan> unordered = PavewayGuidancePlan.releaseCandidates(spot,
                null, 60);
        List<PavewayGuidancePlan> nonFinite = PavewayGuidancePlan.releaseCandidates(spot,
                new Vec3(Double.NaN, 0.0D, 0.0D), 60);

        assertEquals(PavewayGuidancePlan.fromDesignation(spot, 60).spawn(),
                unordered.get(0).spawn());
        assertEquals(unordered, nonFinite);
        assertEquals(unordered, PavewayGuidancePlan.releaseCandidates(spot, spot, 60),
                "a designator at the spot is equidistant and keeps the fixed order");
        assertThrows(IllegalArgumentException.class,
                () -> PavewayGuidancePlan.releaseCandidates(null, spot, 60));
    }

    @Test
    void aMovedLaserSpotProducesANewCorrection() {
        Vec3 current = new Vec3(0.0D, 100.0D, 0.0D);
        Vec3 first = PavewayGuidancePlan.velocityTo(current,
                PavewayGuidancePlan.guidancePoint(new Vec3(20, 64, 20)), 30);
        Vec3 moved = PavewayGuidancePlan.velocityTo(current,
                PavewayGuidancePlan.guidancePoint(new Vec3(35, 64, 20)), 30);

        assertNotEquals(first.x, moved.x);
        assertEquals(first.y, moved.y, 1.0E-9D);
        assertEquals(first.z, moved.z, 1.0E-9D);
    }

    @Test
    void contractsUseExactThirdPartyIdsAndFiveHundredPoundExplosion() {
        assertEquals("superbwarfare:artillery_indicator",
                ArtilleryIndicatorDesignation.ITEM_ID.toString());
        assertEquals(512.0D, ArtilleryIndicatorDesignation.MAX_RANGE);
        assertEquals(500.0F, F16PavewayProvider.EXPLOSION_DAMAGE);
        assertEquals(16.0F, F16PavewayProvider.EXPLOSION_RADIUS);
        assertEquals("LARGE", F16PavewayProvider.EXPLOSION_PARTICLE);
    }

    @Test
    void invalidGuidanceGeometryFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> PavewayGuidancePlan.fromDesignation(
                        new Vec3(Double.NaN, 64, 0), 60));
        assertThrows(IllegalArgumentException.class,
                () -> PavewayGuidancePlan.velocityTo(Vec3.ZERO, Vec3.ZERO, 0));
    }
}
