package com.wok.infantry.stamina;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaminaMathTest {
    @Test
    void drainAndRecoveryStayInsideTheAuthoritativeRange() {
        assertEquals(0.0F, StaminaMath.drain(5.0F, 10.0F));
        assertEquals(100.0F, StaminaMath.recover(98.0F, 10.0F));
        assertEquals(50.0F, StaminaMath.drain(50.0F, -1.0F));
    }

    @Test
    void swayStartsOnlyBelowTheFatigueThreshold() {
        assertEquals(0.0F, StaminaMath.swayIntensity(35.0F, 35.0F));
        assertEquals(0.0F, StaminaMath.swayIntensity(100.0F, 100.0F));
        assertTrue(StaminaMath.swayIntensity(20.0F, 100.0F) > 0.0F);
    }

    @Test
    void armsDominateButExhaustedLegsStillCauseBreathingInstability() {
        float armExhaustion = StaminaMath.swayIntensity(0.0F, 100.0F);
        float legExhaustion = StaminaMath.swayIntensity(100.0F, 0.0F);
        assertEquals(1.0F, armExhaustion);
        assertTrue(legExhaustion > 0.0F);
        assertTrue(legExhaustion < armExhaustion);
    }

    @Test
    void nonFiniteNetworkValuesFailSafeToFullInsteadOfPoisoningTheClient() {
        assertEquals(1.0F, StaminaMath.ratio(Float.NaN));
        assertEquals(100.0F, new StaminaSnapshot(Float.POSITIVE_INFINITY,
                Float.NaN, true).arms());
    }

    @Test
    void serverPositionTrackerDetectsMovementButRejectsJitterAndTeleports() {
        StaminaMovementTracker tracker = new StaminaMovementTracker();
        assertTrue(!tracker.sample(10.0D, 20.0D));
        assertTrue(!tracker.sample(10.005D, 20.0D));
        assertTrue(tracker.sample(10.135D, 20.0D));
        assertTrue(!tracker.sample(30.0D, 40.0D));
    }
}
