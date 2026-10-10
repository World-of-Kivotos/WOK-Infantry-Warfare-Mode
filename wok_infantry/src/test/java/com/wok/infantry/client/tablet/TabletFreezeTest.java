package com.wok.infantry.client.tablet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The uiTest freeze seam: a frozen motion stays put and emits nothing; unfreezing continues. */
class TabletFreezeTest {
    @Test
    void frozenMotionHoldsItsProgressAndStaysQuiet() {
        TabletMotion m = new TabletMotion();
        m.open(0.0D);
        m.drainEvents();
        m.freeze(0.87D);
        assertTrue(m.frozen());
        assertEquals(0.87D, m.p(), 0.0D);
        for (double t = 0.0D; t < 5000.0D; t += 100.0D) {
            m.update(t);
            assertEquals(0.87D, m.p(), 0.0D);
            assertEquals(TabletMotion.State.OPENING, m.state());
        }
        assertTrue(m.drainEvents().isEmpty(), "no sounds or state changes while frozen");
    }

    @Test
    void unfreezingContinuesWithTheRemainingTime() {
        TabletMotion m = new TabletMotion();
        m.open(0.0D);
        m.freeze(0.5D);
        m.update(3000.0D);
        m.unfreeze(3000.0D);
        double dur = m.durMs();
        m.update(3000.0D + 0.25D * dur);
        assertEquals(0.75D, m.p(), 1e-9, "a quarter of the map later");
        m.update(3000.0D + 0.5D * dur);
        assertEquals(TabletMotion.State.SHOWN, m.state());
    }

    @Test
    void freezingACloseHoldsTheCloseFrame() {
        TabletMotion m = new TabletMotion();
        m.open(0.0D);
        for (double t = 0.0D; t <= 1400.0D; t += TabletVectors.DT) {
            m.update(t);
        }
        m.close(1500.0D);
        m.freeze(0.66D);
        m.update(4000.0D);
        assertEquals(TabletMotion.State.CLOSING, m.state());
        assertEquals(0.66D, m.p(), 0.0D);
        assertEquals(1.0D, m.poseContext().captureFromP(), 0.0D, "the replay still comes from p = 1");
    }
}
