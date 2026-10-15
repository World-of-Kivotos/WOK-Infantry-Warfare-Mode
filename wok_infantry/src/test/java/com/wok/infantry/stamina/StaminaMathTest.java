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
        assertEquals(0.0F, StaminaMath.swayIntensity(50.0F, 50.0F));
        assertEquals(0.0F, StaminaMath.swayIntensity(100.0F, 100.0F));
        assertTrue(StaminaMath.swayIntensity(49.0F, 100.0F) > 0.0F);
        assertTrue(StaminaMath.swayIntensity(100.0F, 49.0F) > 0.0F);
    }

    @Test
    void swayGetsStrictlyStrongerAsEitherReserveFallsBelowFifty() {
        float previousArms = 0.0F;
        float previousLegs = 0.0F;
        for (float reserve : new float[] {49.0F, 40.0F, 25.0F, 10.0F, 0.0F}) {
            float arms = StaminaMath.swayIntensity(reserve, 100.0F);
            float legs = StaminaMath.swayIntensity(100.0F, reserve);
            assertTrue(arms > previousArms, "decreasing arm stamina increases sway");
            assertTrue(legs > previousLegs, "decreasing leg stamina increases sway");
            previousArms = arms;
            previousLegs = legs;
        }
    }

    @Test
    void exhaustionStaysLockedUntilTheServersRecoveryThreshold() {
        assertTrue(StaminaMath.shouldBlockSprint(0.0F, false, 15.0F));
        assertTrue(StaminaMath.shouldBlockSprint(14.99F, true, 15.0F));
        assertTrue(!StaminaMath.shouldBlockSprint(15.0F, true, 15.0F));
        assertTrue(!StaminaMath.shouldBlockSprint(5.0F, false, 15.0F));
        assertTrue(StaminaMath.shouldBlockSprint(25.0F, true, 30.0F));
        assertTrue(!StaminaMath.shouldBlockSprint(30.0F, true, 30.0F));
        assertTrue(StaminaMath.shouldBlockSprint(0.0F, true, 0.0F));
    }

    @Test
    void snapshotCarriesRecoveryLockButDisabledPlayersNeverRemainBlocked() {
        assertTrue(new StaminaSnapshot(100.0F, 10.0F, true, true).sprintBlocked());
        assertTrue(new StaminaSnapshot(100.0F, 0.0F, true).sprintBlocked());
        assertTrue(!new StaminaSnapshot(100.0F, 0.0F, false, true).sprintBlocked());
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
