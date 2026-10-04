package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.UiRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the mirrored WOK步战附属-占点 0.1.0-alpha.3 panel geometry (CaptureHudOverlay with the core
 * installed). When the add-on changes its panel, update {@link CaptureHudBridge#mirroredPanel}
 * and this test together, or let the add-on provide CaptureHudApi.panelRect.
 */
class CaptureHudBridgeTest {
    @Test
    void compactPanelOnScreensUpTo360Wide() {
        assertEquals(UiRect.of(140, 26, 312, 61), CaptureHudBridge.mirroredPanel(320));
        assertEquals(UiRect.of(140, 26, 352, 61), CaptureHudBridge.mirroredPanel(360));
    }

    @Test
    void widePanelCentredAboveThat() {
        assertEquals(UiRect.of(48, 28, 378, 80), CaptureHudBridge.mirroredPanel(427));
        assertEquals(UiRect.of(75, 28, 405, 80), CaptureHudBridge.mirroredPanel(480));
        assertEquals(UiRect.of(155, 28, 485, 80), CaptureHudBridge.mirroredPanel(640));
        assertEquals(UiRect.of(315, 28, 645, 80), CaptureHudBridge.mirroredPanel(960));
        assertEquals(UiRect.of(15, 28, 345, 80), CaptureHudBridge.mirroredPanel(361),
                "first wide width: min(330, max(190, 361 − 24)) = 330");
    }
}
