package com.wok.commandersupport.drone;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import com.wok.infantry.support.adapter.SupportIntelPublisher;
import com.wok.infantry.support.adapter.SupportSpawnException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconDroneProviderTest {
    @Test
    void definitionMatchesTheSpecifiedSortie() {
        SupportDefinition definition = ReconDroneProvider.definition();

        assertEquals(WokCommanderSupportMod.RECON_DRONE_ID, definition.id());
        assertEquals("wok_commander_support:recon_drone", definition.id().toString());
        assertEquals("support.wok_commander_support.recon_drone", definition.translationKey());
        assertEquals("无人机侦察", definition.fallbackName());
        assertEquals("无人机侦察", definition.shortName());
        assertEquals(SupportTargetMode.POINT, definition.targetMode());
        // 5 minutes from 《步战模式公示表》, immediate launch, 60 steps of 2 seconds.
        assertEquals(6_000L, definition.cooldownTicks());
        assertEquals(0L, definition.inboundTicks());
        assertEquals(40, definition.stepIntervalTicks());
        assertEquals(60, definition.stepCount());
        assertEquals(96.0D, definition.radius());
        // Scans only read loaded entities; the launch itself checks the orbit chunks.
        assertFalse(definition.requiresLoadedFootprint());
    }

    @Test
    void contactsStayOnTheMapBetweenScansAndPastTheLastOne() {
        int scanInterval = ReconDroneMissionPolicy.SCAN_EVERY_STEPS
                * ReconDroneProvider.STEP_INTERVAL_TICKS;
        assertEquals(240, ReconDroneProvider.CONTACT_TTL_TICKS);
        assertTrue(ReconDroneProvider.CONTACT_TTL_TICKS > scanInterval);
        assertTrue(ReconDroneProvider.CONTACT_TTL_TICKS >= SupportIntelPublisher.MIN_TTL_TICKS);
        assertTrue(ReconDroneProvider.CONTACT_TTL_TICKS <= SupportIntelPublisher.MAX_TTL_TICKS);
        // The last scan (step 55) is still on the map when the drone leaves at step 59.
        int lastScanTick = 55 * ReconDroneProvider.STEP_INTERVAL_TICKS;
        int departureTick = (ReconDroneProvider.STEP_COUNT - 1)
                * ReconDroneProvider.STEP_INTERVAL_TICKS;
        assertTrue(lastScanTick + ReconDroneProvider.CONTACT_TTL_TICKS > departureTick);
    }

    @Test
    void theOrbitFitsInsideTheScannedArea() {
        double farthestHitBox = ReconDroneFlight.ORBIT_RADIUS + ReconDroneFlight.AIRFRAME_MARGIN;
        assertTrue(farthestHitBox < ReconDroneProvider.SCAN_RADIUS,
                "the core's world-border check of the scan footprint also covers the orbit");
        assertEquals(24.0D, ReconDroneFlight.ORBIT_RADIUS);
        assertEquals(0.6D, ReconDroneFlight.CRUISE_SPEED);
        assertEquals(64, ReconDroneFlight.HEIGHT_ABOVE_TARGET);
        assertEquals(40, ReconDroneFlight.CLEARANCE_ABOVE_ORBIT);
        assertEquals(16, ReconDroneFlight.CEILING_MARGIN);
        assertEquals(16, ReconDroneFlight.HEIGHT_SAMPLES);
        assertEquals(200.0F, ReconDroneEntity.MAX_HEALTH);
    }

    @Test
    void missingContextEndsTheMissionWithoutTrippingTheCircuit() {
        ReconDroneProvider provider = new ReconDroneProvider();
        assertEquals(WokCommanderSupportMod.RECON_DRONE_ID, provider.supportId());

        SupportSpawnException failure = assertThrows(SupportSpawnException.class,
                () -> provider.executeStep(null));
        assertFalse(failure.providerBroken());
        assertFalse(failure.refundCooldown());
        // Cleanup without a context is a no-op.
        provider.abandon(null);
    }
}
