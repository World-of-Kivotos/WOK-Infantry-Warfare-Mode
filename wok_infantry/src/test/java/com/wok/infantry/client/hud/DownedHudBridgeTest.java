package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.UiRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the mirrored WOK步战附属-倒地 0.1.0-alpha.3 panel (DownedOverlay lines 26–30) the squad
 * roster keeps clear of while the viewer is down (整体审查修正 compat-01). When the add-on moves
 * its panel, update {@link DownedHudBridge#mirroredPanel} and this test together, or let the
 * add-on ask for {@link InfantryHudApi#CENTER_LOW}.
 */
class DownedHudBridgeTest {
    @Test
    void panelSitsAtTheBottomCentre() {
        assertEquals(UiRect.of(83, 164, 343, 210), DownedHudBridge.mirroredPanel(427, 240));
        assertEquals(UiRect.of(30, 164, 290, 210), DownedHudBridge.mirroredPanel(320, 240));
        assertEquals(UiRect.of(350, 644, 610, 690), DownedHudBridge.mirroredPanel(960, 720));
    }

    @Test
    void panelKeepsItsMinimumWidthAndTop() {
        assertEquals(UiRect.of(5, 8, 195, 54), DownedHudBridge.mirroredPanel(200, 60),
                "min(260, max(190, 200 − 20)) = 190, top max(8, 60 − 76) = 8");
        assertEquals(UiRect.of(10, 124, 220, 170), DownedHudBridge.mirroredPanel(230, 200),
                "w − 20 between 190 and 260");
    }
}
