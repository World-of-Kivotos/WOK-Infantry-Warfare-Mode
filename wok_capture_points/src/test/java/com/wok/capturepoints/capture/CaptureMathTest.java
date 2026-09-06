package com.wok.capturepoints.capture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaptureMathTest {
    @Test
    void fullEnemyPointTakesConfiguredDurationAtOnePlayer() {
        CaptureMath.Result result = CaptureMath.step(-1.0D, 1, 0,
                true, true, 20 * 90, 90, 4, true);
        assertEquals(1.0D, result.control(), 0.000001D);
        assertEquals(CaptureTeam.BLUE, result.activeTeam());
    }

    @Test
    void advantageModeUsesPlayerDifferenceAndCapsMultiplier() {
        CaptureMath.Result result = CaptureMath.step(0.0D, 7, 2,
                true, true, 20, 100, 3, true);
        assertEquals(3, result.speedMultiplier());
        assertEquals(0.06D, result.control(), 0.000001D);
    }

    @Test
    void freezeModeStopsWhenBothTeamsArePresent() {
        CaptureMath.Result result = CaptureMath.step(0.25D, 4, 1,
                true, true, 20, 60, 4, false);
        assertEquals(0.25D, result.control(), 0.0D);
        assertEquals(CaptureTeam.NEUTRAL, result.activeTeam());
    }

    @Test
    void lockedTeamDoesNotContribute() {
        CaptureMath.Result result = CaptureMath.step(0.0D, 3, 1,
                false, true, 20, 100, 4, true);
        assertEquals(CaptureTeam.RED, result.activeTeam());
        assertEquals(-0.02D, result.control(), 0.000001D);
    }
}
