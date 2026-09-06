package com.wok.commandersupport.airstrike;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PavewayGuidancePlanTest {
    @Test
    void definitionUsesTenSecondInboundAndThreeSecondTickGuidance() {
        SupportDefinition definition = WokCommanderSupportMod.f16cPavewayDefinition();

        assertEquals(WokCommanderSupportMod.F16C_PAVEWAY_ID, definition.id());
        assertEquals(SupportTargetMode.POINT, definition.targetMode());
        assertEquals(18_000L, definition.cooldownTicks());
        assertEquals(200L, definition.inboundTicks());
        assertEquals(61, definition.stepCount());
        assertEquals(1, definition.stepIntervalTicks());
        assertEquals(64.0D, definition.radius());
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
