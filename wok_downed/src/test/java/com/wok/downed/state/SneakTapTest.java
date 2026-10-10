package com.wok.downed.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SneakTapTest {
    @Test
    void sneakHeldFromTheStartGestureIsNotATap() {
        SneakTap tap = new SneakTap();
        for (int i = 0; i < 3; i++) {
            assertFalse(tap.update(true));
        }
        assertFalse(tap.update(false));
    }

    @Test
    void shortPressAndReleaseIsATap() {
        SneakTap tap = new SneakTap();
        assertFalse(tap.update(false));
        assertFalse(tap.update(true));
        assertFalse(tap.update(true));
        assertTrue(tap.update(false));
    }

    @Test
    void tapAfterTheStartGestureIsLetGo() {
        SneakTap tap = new SneakTap();
        assertFalse(tap.update(true));
        assertFalse(tap.update(false));
        assertFalse(tap.update(true));
        assertTrue(tap.update(false));
    }

    @Test
    void halfASecondPressStillCounts() {
        SneakTap tap = new SneakTap();
        assertFalse(tap.update(false));
        for (int i = 0; i < SneakTap.MAX_TAP_TICKS; i++) {
            assertFalse(tap.update(true));
        }
        assertTrue(tap.update(false));
    }

    @Test
    void crouchingLongerThanHalfASecondIsNotATap() {
        SneakTap tap = new SneakTap();
        assertFalse(tap.update(false));
        for (int i = 0; i <= SneakTap.MAX_TAP_TICKS; i++) {
            assertFalse(tap.update(true));
        }
        assertFalse(tap.update(false));
        // A later short tap still works after a long crouch.
        assertFalse(tap.update(true));
        assertTrue(tap.update(false));
    }
}
