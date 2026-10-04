package com.wok.infantry.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StaminaHudOverlayTest {
    @Test
    void keepsTheHistoricalPositionOnWideScreens() {
        assertEquals(43, StaminaHudOverlay.fallbackLeft(960, 52));
        assertEquals(43, StaminaHudOverlay.fallbackLeft(480, 52));
    }

    @Test
    void neverReachesTheHotbarWithoutBodyHealth() {
        for (int width = 320; width <= 1920; width++) {
            int left = StaminaHudOverlay.fallbackLeft(width, 52);
            assertTrue(left >= 3, "left edge at width " + width);
            assertTrue(left + 52 <= width / 2 - 91 - 4, "hotbar overlap at width " + width);
        }
        assertEquals(13, StaminaHudOverlay.fallbackLeft(320, 52));
    }
}
