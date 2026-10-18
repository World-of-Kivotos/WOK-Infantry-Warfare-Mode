package com.wok.commandersupport.drone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconDroneAcousticsTest {
    private static final float EPSILON = 1.0E-6F;

    @Test
    void theEngineIsHeardForFlightHeightPlus64Blocks() {
        assertEquals(128.0D, ReconDroneAcoustics.engineRange(64.0D));
        assertEquals(170.0D, ReconDroneAcoustics.engineRange(106.0D));
        assertEquals(64.0D, ReconDroneAcoustics.engineRange(-3.0D));
        assertEquals(128.0D, ReconDroneAcoustics.engineRange(Double.NaN),
                "an unknown height falls back to the minimum flight height");
    }

    @Test
    void volumeFadesLinearlyToSilenceAtTheRange() {
        double range = ReconDroneAcoustics.engineRange(64.0D);
        assertEquals(1.0F, ReconDroneAcoustics.engineVolume(0.0D, range, -1.0D), EPSILON);
        // Standing right under the lowest orbit.
        assertEquals(0.5F, ReconDroneAcoustics.engineVolume(64.0D, range, -1.0D), EPSILON);
        assertEquals(0.25F, ReconDroneAcoustics.engineVolume(96.0D, range, -1.0D), EPSILON);
        assertEquals(0.0F, ReconDroneAcoustics.engineVolume(range, range, -1.0D));
        assertEquals(0.0F, ReconDroneAcoustics.engineVolume(500.0D, range, -1.0D));
        assertEquals(0.0F, ReconDroneAcoustics.engineVolume(Double.NaN, range, -1.0D));
        assertEquals(0.0F, ReconDroneAcoustics.engineVolume(10.0D, 0.0D, -1.0D));
    }

    @Test
    void theEngineWindsDownAfterAShootDown() {
        assertEquals(1.5F, ReconDroneAcoustics.ENGINE_PITCH, EPSILON);
        assertEquals(1.5F, ReconDroneAcoustics.enginePitch(-1.0D), EPSILON);
        assertEquals(0.5F, ReconDroneAcoustics.engineVolume(0.0D, 128.0D,
                ReconDroneAcoustics.CRASH_FADE_TICKS / 2.0D), EPSILON);
        assertEquals(0.0F, ReconDroneAcoustics.engineVolume(0.0D, 128.0D,
                ReconDroneAcoustics.CRASH_FADE_TICKS));
        assertTrue(ReconDroneAcoustics.enginePitch(10.0D) < ReconDroneAcoustics.ENGINE_PITCH);
        assertEquals(ReconDroneAcoustics.CRASH_MIN_PITCH,
                ReconDroneAcoustics.enginePitch(1_000.0D), EPSILON);
        assertFalse(ReconDroneAcoustics.engineSilenced(-1.0D));
        assertFalse(ReconDroneAcoustics.engineSilenced(5.0D));
        assertTrue(ReconDroneAcoustics.engineSilenced(ReconDroneAcoustics.CRASH_FADE_TICKS));
    }
}
