package com.wok.infantry.client.ui;

import com.wok.infantry.client.screen.TacticalShellLayout;
import com.wok.infantry.client.screen.UiRect;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiTierMatrixTest {
    @Test
    void tiersResolveToThePreviewLayoutSizes() {
        assertEquals("320x240", size(UiTierMatrix.T320));
        assertEquals("480x270", size(UiTierMatrix.T480));
        assertEquals("640x360", size(UiTierMatrix.T640));
        assertEquals("640x336", size(UiTierMatrix.T640_USER));
        assertEquals("960x540", size(UiTierMatrix.T960X540));
        assertEquals("480x360", size(UiTierMatrix.T960),
                "960×720 at GUI 1 lays out as 480×360 under the minimum 2x");
        assertEquals(2, UiTierMatrix.T960.factor());
        assertEquals(1, UiTierMatrix.T320.factor());
        assertEquals("427x240", size(UiTierMatrix.T427),
                "the default 854×480 window at GUI 1 lays out as 427×240 under the minimum 2x");
        assertEquals(2, UiTierMatrix.T427.factor());
    }

    @Test
    void shellRegionsNeverOverlapOnAnyTier() {
        for (UiTierMatrix.Tier tier : UiTierMatrix.ALL) {
            TacticalShellLayout shell = TacticalShellLayout.compute(tier.layoutWidth(),
                    tier.layoutHeight());
            UiRect screen = new UiRect(0, 0, tier.layoutWidth(), tier.layoutHeight());
            // The status bar, the board (with its 1px outline) and the bezel share the device.
            Map<String, UiRect> regions = new LinkedHashMap<>();
            regions.put("status", shell.status());
            regions.put("body", shell.body().inset(-1));
            regions.put("bezel", shell.bezel());
            UiTierMatrix.assertNoSolidOverlap(tier.toString(), regions);
            UiTierMatrix.assertInside(tier.toString(), shell.device(), regions);
            // The case with its bumpers and side keys floats on the world, inside the screen.
            int bump = shell.deviceMetrics().bump();
            UiTierMatrix.assertInside(tier.toString(), screen,
                    Map.of("device", shell.device().inset(-bump)));
            // The glass holds the status bar and the board; the bezel stays below its lip.
            UiTierMatrix.assertInside(tier.toString(), shell.display(),
                    Map.of("status", shell.status(), "body", shell.body().inset(-1)));
            UiTierMatrix.assertNoSolidOverlap(tier.toString(),
                    Map.of("glass", shell.glass().inset(0, 0, 0, -1), "bezel", shell.bezel()));
        }
    }

    @Test
    void overlapAssertionNamesBothRegions() {
        Map<String, UiRect> regions = new LinkedHashMap<>();
        regions.put("a", new UiRect(0, 0, 10, 10));
        regions.put("b", new UiRect(9, 0, 20, 10));
        regions.put("empty", UiRect.EMPTY);
        AssertionFailedError error = assertThrows(AssertionFailedError.class,
                () -> UiTierMatrix.assertNoSolidOverlap("ctx", regions));
        assertEquals(true, error.getMessage().contains("a ") && error.getMessage().contains("b "));
    }

    private static String size(UiTierMatrix.Tier tier) {
        return tier.layoutWidth() + "x" + tier.layoutHeight();
    }
}
