package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.UiRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StaminaHudOverlayTest {
    @Test
    void standalonePlateNeverReachesTheHotbarOffhandOrAttackIndicator() {
        for (int width = 320; width <= 1920; width++) {
            for (boolean offhand : new boolean[]{false, true}) {
                for (boolean indicator : new boolean[]{false, true}) {
                    WokHudLayout.Input input = WokHudLayout.Input.screen(width, 240, 1)
                            .withStamina(true).withHotbarNeighbours(offhand, indicator);
                    UiRect plate = WokHudLayout.compute(input).staminaPlate();
                    int hotbar = width / 2 - 91;
                    String where = "width " + width + " offhand=" + offhand
                            + " indicator=" + indicator;
                    assertTrue(plate.left() >= 2, where);
                    assertTrue(plate.right() <= hotbar - 4, "hotbar at " + where);
                    if (offhand) {
                        assertTrue(plate.right() <= width / 2 - 120 - 4, "off-hand slot at " + where);
                    }
                    if (indicator) {
                        assertTrue(plate.right() <= width / 2 - 113 - 4,
                                "attack indicator at " + where);
                    }
                    assertEquals(27, plate.height());
                    assertEquals(238, plate.bottom(), "2px above the bottom on tight screens");
                }
            }
        }
    }

    @Test
    void standalonePlateKeepsRoomForBothLabelledRows() {
        // narrowest case: 320 wide with an off-hand item -> [2, 36)
        UiRect plate = WokHudLayout.compute(WokHudLayout.Input.screen(320, 240, 1)
                .withStamina(true).withHotbarNeighbours(true, false)).staminaPlate();
        assertEquals(UiRect.of(2, 211, 36, 238), plate);
        int textRight = plate.left() + StaminaHudOverlay.STACK_LEFT_INSET
                + HudTestSupport.width("手") + StaminaHudOverlay.LABEL_GAP;
        assertTrue(plate.right() - StaminaHudOverlay.STACK_RIGHT_INSET - textRight >= 10,
                "a readable bar remains after the label");
    }

    @Test
    void companionRowFitsLabelledBarsInTheBodyHealthStrip() {
        // body-health companion strip: 52×11, centred under the figure at h − 13
        StaminaHudOverlay.CompanionRow row = StaminaHudOverlay.companionRow(43, 227, 52, 11,
                HudTestSupport.width("手"), HudTestSupport.width("腿"), true);
        assertTrue(row.labelled());
        assertEquals(228, row.textY());
        assertEquals(47, row.armsLabelX());
        assertTrue(row.armsBar().width() >= StaminaHudOverlay.MIN_LABELLED_BAR);
        assertTrue(row.legsBar().width() >= StaminaHudOverlay.MIN_LABELLED_BAR);
        assertTrue(row.armsBar().right() + 3 <= row.legsLabelX(), "3px between the pairs");
        assertTrue(row.legsBar().right() <= 43 + 52 - 3, "inside the strip");
        assertEquals(3, row.armsBar().height());

        StaminaHudOverlay.CompanionRow english = StaminaHudOverlay.companionRow(43, 227, 52, 11,
                HudTestSupport.width("A"), HudTestSupport.width("L"), true);
        assertTrue(english.labelled());
        assertTrue(english.armsBar().width() > row.armsBar().width());
    }

    @Test
    void companionRowFallsBackToTwoBareBars() {
        StaminaHudOverlay.CompanionRow tooLong = StaminaHudOverlay.companionRow(0, 0, 52, 11,
                HudTestSupport.width("手部"), HudTestSupport.width("腿部"), true);
        assertFalse(tooLong.labelled(), "labels that leave less than 10px fall back");
        StaminaHudOverlay.CompanionRow scaled = StaminaHudOverlay.companionRow(0, 0, 52, 11,
                HudTestSupport.width("手"), HudTestSupport.width("腿"), false);
        assertFalse(scaled.labelled(), "no 1x CJK labels under the 2x HUD");
        assertEquals(UiRect.of(4, 2, 49, 5), scaled.armsBar());
        assertEquals(UiRect.of(4, 7, 49, 10), scaled.legsBar());
    }
}
