package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.UiRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the mirrored WOK步战附属-占点 0.1.0-alpha.3 panel geometry (CaptureHudOverlay with the core
 * installed), still used while an alpha.3 add-on draws its panel. Later add-ons report their own
 * strip through CaptureHudApi.panelRect and the point through CaptureHudApi.currentPoint.
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

    @Test
    void withoutTheAddOnNothingIsReadAndNothingAvoided() {
        // unit tests run without FML: no mod list, so no add-on
        assertNull(CaptureHudBridge.objective());
        assertFalse(CaptureHudBridge.readsObjective());
        assertNull(CaptureHudBridge.panelRect(320, 240));
        assertFalse(InfantryHudApi.rendersCapturePoints(), "nothing to show");
    }

    @Test
    void theAcceptancePinActsLikeANewAddOnInsideAPoint() {
        CaptureObjective b = CaptureObjective.fromMap(CaptureObjectiveTest.pointB());
        try {
            CaptureHudBridge.pinForAcceptance(b);
            assertSame(b, CaptureHudBridge.objective());
            assertTrue(CaptureHudBridge.readsObjective());
            assertNull(CaptureHudBridge.panelRect(320, 240), "the add-on draws nothing then");
            assertTrue(InfantryHudApi.rendersCapturePoints(),
                    "strip on by default (config not loaded) and the point is readable");
        } finally {
            CaptureHudBridge.pinForAcceptance(null);
        }
        assertNull(CaptureHudBridge.objective());
    }
}
