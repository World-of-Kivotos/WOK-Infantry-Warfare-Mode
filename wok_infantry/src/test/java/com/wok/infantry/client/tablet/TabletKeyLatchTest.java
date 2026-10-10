package com.wok.infantry.client.tablet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DESIGN 2.2: one press of the terminal key opens or closes once, however long it is held. */
class TabletKeyLatchTest {
    @Test
    void heldKeyAndItsRepeatsTriggerOnce() {
        TabletKeyLatch latch = new TabletKeyLatch();
        assertTrue(latch.press(false), "the first press opens");
        assertTrue(latch.latched());
        assertFalse(latch.press(true), "repeats are ignored");
        assertFalse(latch.press(false), "a press before the release is ignored");
        latch.poll(true);
        assertTrue(latch.latched(), "still held");
        latch.release();
        assertTrue(latch.press(false), "a new press after the release closes");
        assertFalse(latch.press(true));
    }

    @Test
    void repeatsNeverTriggerEvenUnlatched() {
        TabletKeyLatch latch = new TabletKeyLatch();
        assertFalse(latch.press(true));
        assertFalse(latch.latched());
    }

    @Test
    void pollClearsTheLatchWhenNothingIsHeld() {
        // uiTest clicks mappings and calls screen.keyReleased directly: no Forge release event (D7)
        TabletKeyLatch latch = new TabletKeyLatch();
        latch.latch();
        assertTrue(latch.latched());
        latch.poll(false);
        assertFalse(latch.latched());
        assertTrue(latch.press(false));
    }
}
