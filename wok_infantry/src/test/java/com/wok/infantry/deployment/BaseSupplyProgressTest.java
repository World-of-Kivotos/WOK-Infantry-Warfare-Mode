package com.wok.infantry.deployment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BaseSupplyProgressTest {
    @Test void requiresEveryTickOfFifteenSecondsAndCompletesOnce() {
        BaseSupplyProgress progress = new BaseSupplyProgress(100);
        for (long tick = 100; tick < 400; tick++) {
            progress.tick(tick);
            assertFalse(progress.ready(tick));
        }
        progress.tick(400);
        assertTrue(progress.ready(400));
        assertEquals(0, progress.remainingSeconds(400));
        progress.complete();
        assertFalse(progress.ready(401));
    }
    @Test void disconnectOrClockRollbackRestartsFullCountdown() {
        BaseSupplyProgress progress = new BaseSupplyProgress(100);
        for (long tick = 100; tick < 380; tick++) progress.tick(tick);
        progress.tick(410);
        assertEquals(15, progress.remainingSeconds(410));
        assertFalse(progress.ready(410));
        progress.tick(20);
        assertEquals(15, progress.remainingSeconds(20));
    }
}
