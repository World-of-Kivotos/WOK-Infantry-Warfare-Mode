package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.UiRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the WOK步战附属-部位血量 0.1.0-beta.10 figure column the squad roster keeps clear of
 * (整体审查修正 compat-01). Body health lays out {@code figureY = h − 48 − 25} and puts its
 * companion slot at {@code figureY + 48 + 25 − 11 − 2}; the head label chip starts at
 * {@code figureY − 1}; every label stays in {@code [2, w / 2 − 91 − 4)}. When the add-on changes
 * that geometry, update {@link BodyHealthHudBridge#columnAbove} and this test together, or let the
 * add-on provide {@code BodyHealthHudApi.hudRect}.
 */
class BodyHealthHudBridgeTest {
    @Test
    void columnReachesFromTheHeadChipToTheBottomLeftOfTheHotbar() {
        // 427×240, SHORT tier: figure at x50, y167; slot centred under it
        assertEquals(UiRect.of(0, 166, 118, 240),
                BodyHealthHudBridge.columnAbove(new int[]{43, 227, 52, 11}, 427, 240));
        assertEquals(UiRect.of(0, 646, 385, 720),
                BodyHealthHudBridge.columnAbove(new int[]{43, 707, 52, 11}, 960, 720));
    }

    @Test
    void headChipIsTheTopOfTheFigureColumn() {
        for (int height = 200; height <= 1080; height += 7) {
            int figureY = height - 48 - 25;
            int companionTop = figureY + 48 + 25 - 11 - 2;
            UiRect column = BodyHealthHudBridge.columnAbove(new int[]{10, companionTop, 52, 11},
                    640, height);
            assertEquals(figureY - 1, column.top(), "height " + height);
            assertEquals(height, column.bottom(), "height " + height);
        }
    }

    @Test
    void columnCoversTheCompanionSlotOnNarrowScreens() {
        // COMPACT tier on a very narrow screen: the slot may reach past w / 2 − 95
        UiRect column = BodyHealthHudBridge.columnAbove(new int[]{20, 187, 52, 11}, 240, 200);
        assertTrue(column.right() >= 72, column.toString());
        assertNull(BodyHealthHudBridge.columnAbove(null, 427, 240));
        assertNull(BodyHealthHudBridge.columnAbove(new int[]{1, 2, 3}, 427, 240));
        assertNull(BodyHealthHudBridge.hudRect(427, 240, null), "hidden body-health HUD");
    }
}
