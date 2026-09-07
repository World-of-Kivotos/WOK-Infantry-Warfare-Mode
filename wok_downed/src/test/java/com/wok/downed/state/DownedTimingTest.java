package com.wok.downed.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DownedTimingTest {
    @Test
    void convertsSecondsToGameTicks() {
        assertEquals(160, DownedTiming.secondsToTicks(8));
        assertEquals(20, DownedTiming.secondsToTicks(0));
    }

    @Test
    void clampsProgressToAStablePercentage() {
        assertEquals(0, DownedTiming.progressPercent(160, 160));
        assertEquals(50, DownedTiming.progressPercent(160, 80));
        assertEquals(100, DownedTiming.progressPercent(160, -10));
    }

    @Test
    void comparesSquaredMovementAgainstTolerance() {
        assertFalse(DownedTiming.movedTooFar(0.5625D, 0.75D));
        assertTrue(DownedTiming.movedTooFar(0.57D, 0.75D));
    }
}
