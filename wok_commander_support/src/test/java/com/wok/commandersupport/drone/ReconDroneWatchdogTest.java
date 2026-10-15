package com.wok.commandersupport.drone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The backstop that removes drones without relying on their own tick: expiry applies whether or
 * not the drone still ticks, and a drone the server stopped ticking is removed after a short grace.
 */
class ReconDroneWatchdogTest {
    private static final long LAUNCH = 48_000L;
    private static final long EXPIRE = ReconDroneFlight.expireGameTime(LAUNCH,
            ReconDroneProvider.STEP_COUNT, ReconDroneProvider.STEP_INTERVAL_TICKS);

    @Test
    void aDroneThatKeepsTickingIsLeftAloneUntilItsExpiry() {
        for (long now = LAUNCH; now < EXPIRE; now++) {
            assertFalse(ReconDroneWatchdog.overdue(now, now, EXPIRE, false), "tick " + now);
        }
        assertTrue(ReconDroneWatchdog.overdue(EXPIRE, EXPIRE, EXPIRE, false));
        assertEquals(LAUNCH + 59L * 40L + 100L, EXPIRE);
    }

    @Test
    void theExpiryAppliesEvenWhenTheDroneNoLongerTicks() {
        // Frozen a few ticks before its expiry: the expiry wins over the stall grace.
        long lastTick = EXPIRE - 3L;
        assertFalse(ReconDroneWatchdog.overdue(EXPIRE - 1L, lastTick, EXPIRE, false));
        assertTrue(ReconDroneWatchdog.overdue(EXPIRE, lastTick, EXPIRE, false));
    }

    @Test
    void aDroneTheServerStoppedTickingIsRemovedAfterTheGrace() {
        long frozenAt = LAUNCH + 600L;
        for (long now = frozenAt; now <= frozenAt + ReconDroneWatchdog.STALL_GRACE_TICKS; now++) {
            assertFalse(ReconDroneWatchdog.overdue(now, frozenAt, EXPIRE, false), "tick " + now);
        }
        assertTrue(ReconDroneWatchdog.overdue(
                frozenAt + ReconDroneWatchdog.STALL_GRACE_TICKS + 1L, frozenAt, EXPIRE, false));
        // Right after launch the drone has not ticked yet, which is no stall.
        assertFalse(ReconDroneWatchdog.overdue(LAUNCH, LAUNCH, EXPIRE, false));
    }

    @Test
    void theGraceEndsBeforeTheMissionChecksTwice() {
        assertTrue(ReconDroneWatchdog.STALL_GRACE_TICKS > 1L);
        assertTrue(ReconDroneWatchdog.STALL_GRACE_TICKS < ReconDroneProvider.STEP_INTERVAL_TICKS);
    }

    @Test
    void aFallingWreckOutlivesTheExpiryButNotAStall() {
        long crashAt = EXPIRE - 10L;
        for (long now = crashAt; now < crashAt + ReconDroneFlight.CRASH_MAX_TICKS; now++) {
            assertFalse(ReconDroneWatchdog.overdue(now, now, EXPIRE, true), "tick " + now);
        }
        assertTrue(ReconDroneWatchdog.overdue(
                crashAt + ReconDroneWatchdog.STALL_GRACE_TICKS + 1L, crashAt, EXPIRE, true));
    }

    @Test
    void aDroneNoMissionLaunchedIsRemovedAtOnce() {
        assertTrue(ReconDroneWatchdog.overdue(0L, Long.MIN_VALUE, Long.MIN_VALUE, false));
        assertTrue(ReconDroneWatchdog.overdue(LAUNCH, Long.MIN_VALUE, Long.MIN_VALUE, false));
        // Even a launched drone that never ticked: the overflowing gap still counts as a stall.
        assertTrue(ReconDroneWatchdog.overdue(Long.MAX_VALUE - 1L, Long.MIN_VALUE + 1L,
                Long.MAX_VALUE, false));
    }

    @Test
    void aClockThatDidNotMoveIsNeverAStall() {
        assertFalse(ReconDroneWatchdog.overdue(LAUNCH, LAUNCH + 5L, EXPIRE, false));
        assertFalse(ReconDroneWatchdog.overdue(LAUNCH, LAUNCH + 5L, EXPIRE, true));
    }
}
