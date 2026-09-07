package com.wok.commandersupport.airstrike;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdamFlightPlanTest {
    @Test
    void definitionUsesTenSecondInboundOneSecondDropAndTwoSecondFuse() {
        SupportDefinition definition = WokCommanderSupportMod.millenniumJdamDefinition();

        assertEquals(WokCommanderSupportMod.MILLENNIUM_JDAM_ID, definition.id());
        assertEquals(SupportTargetMode.POINT, definition.targetMode());
        assertEquals(18_000L, definition.cooldownTicks());
        assertEquals(200L, definition.inboundTicks());
        assertEquals(4, definition.stepCount());
        assertEquals(20, definition.stepIntervalTicks());
        assertEquals(32.0D, definition.radius());
    }

    @Test
    void flightIsVerticalAndDragCompensatedForTwentyTicks() {
        JdamFlightPlan plan = JdamFlightPlan.fromImpact(100.5D, 70.25D,
                -20.5D, 20);

        assertEquals(0.0D, plan.spawn().x - plan.impact().x, 1.0E-9D);
        assertEquals(200.0D, plan.spawn().y - plan.impact().y, 1.0E-9D);
        assertEquals(0.0D, plan.spawn().z - plan.impact().z, 1.0E-9D);
        assertEquals(0.0D, plan.velocity().x, 1.0E-9D);
        assertEquals(0.0D, plan.velocity().z, 1.0E-9D);
        assertTrue(plan.velocity().y < -10.0D,
                "JDAM must make a visibly violent vertical high-altitude drop");

        Vec3 simulated = plan.spawn();
        Vec3 velocity = plan.velocity();
        for (int tick = 0; tick < plan.flightTicks(); tick++) {
            simulated = simulated.add(velocity);
            velocity = velocity.scale(1.0D - JdamFlightPlan.CBC_LINEAR_DRAG);
        }
        assertEquals(plan.impact().x, simulated.x, 1.0E-8D);
        assertEquals(plan.impact().y, simulated.y, 1.0E-8D);
        assertEquals(plan.impact().z, simulated.z, 1.0E-8D);
    }

    @Test
    void integrationUsesTheConfirmedCreateBigCannonsHeShellId() {
        assertEquals("createbigcannons:he_shell",
                MillenniumJdamProvider.CBC_HE_SHELL_ID.toString());
        assertEquals(200L,
                WokCommanderSupportMod.MILLENNIUM_JDAM_INBOUND_TICKS);
        assertEquals(20, WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS);
        assertEquals(40,
                WokCommanderSupportMod.MILLENNIUM_JDAM_DELAY_FUSE_TICKS);
    }

    @Test
    void invalidGeometryAndDragFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> JdamFlightPlan.fromImpact(Double.NaN, 64, 0, 20));
        assertThrows(IllegalArgumentException.class,
                () -> JdamFlightPlan.geometricTravelFactor(1.0D, 20));
        assertEquals(20.0D, JdamFlightPlan.geometricTravelFactor(0.0D, 20));
    }
}
