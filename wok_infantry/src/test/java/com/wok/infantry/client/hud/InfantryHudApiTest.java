package com.wok.infantry.client.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfantryHudApiTest {
    @Test
    void slotsAreReturnedInGuiPixels() {
        WokHudLayout.Layout narrow = WokHudLayout.compute(WokHudLayout.Input.screen(320, 240, 1)
                .withRoster(8, false).withStrip(true));
        assertArrayEquals(new int[]{60, 136, 200, 48},
                InfantryHudApi.slot(narrow, InfantryHudApi.CENTER_LOW));
        // since the stamina bar moved above the hotbar the vitals slot only ends 4px left of its
        // narrow left ear [59, 69)
        assertArrayEquals(new int[]{2, 203, 53, 35},
                InfantryHudApi.slot(narrow, InfantryHudApi.VITALS));
        assertArrayEquals(new int[]{123, 22, 195, 120 - 22},
                InfantryHudApi.slot(narrow, InfantryHudApi.TOP_CENTER_NEXT),
                "under the strip, as wide as its column, down to the middle");
        assertEquals(19, InfantryHudApi.topCenterBottom(narrow));

        WokHudLayout.Layout scaled = WokHudLayout.compute(WokHudLayout.Input.screen(960, 720, 2)
                .withStrip(true).withToasts(List.of(40)));
        int[] next = InfantryHudApi.slot(scaled, InfantryHudApi.TOP_CENTER_NEXT);
        assertEquals((21 + 5 + 12 + 4) * 2, next[1], "doubled at the 2x HUD");
        assertEquals(38 * 2, InfantryHudApi.topCenterBottom(scaled));
        assertArrayEquals(new int[]{8, 682, 220, 30},
                InfantryHudApi.slot(scaled, InfantryHudApi.VITALS));
    }

    /**
     * WOK步战附属-占点 0.1.0-alpha.4 draws its own strip unless this is true: only with the strip
     * on and a readable add-on point.
     */
    @Test
    void capturePointsAreShownByTheCoreOnlyWithTheStripOnAndAReadablePoint() {
        assertTrue(InfantryHudApi.rendersCapturePoints(true, true));
        assertFalse(InfantryHudApi.rendersCapturePoints(false, true), "hud.showBattleStrip off");
        assertFalse(InfantryHudApi.rendersCapturePoints(true, false),
                "an add-on without currentPoint, or one whose map broke the contract");
        assertFalse(InfantryHudApi.rendersCapturePoints(false, false));
    }

    @Test
    void unknownSlotsAndEmptyTopCentre() {
        WokHudLayout.Layout empty = WokHudLayout.compute(WokHudLayout.Input.screen(960, 540, 1));
        assertNull(InfantryHudApi.slot(empty, "nope"));
        assertNull(InfantryHudApi.slot(empty, null));
        assertNull(InfantryHudApi.slot(null, InfantryHudApi.VITALS));
        assertEquals(0, InfantryHudApi.topCenterBottom(empty));
        assertEquals(4, InfantryHudApi.slot(empty, InfantryHudApi.TOP_CENTER_NEXT)[1],
                "nothing at the top centre: the slot starts at the edge");
    }
}
