package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationDetailPanelTest {
    @Test
    void linesAreDrawnOnlyWhenTheirWholeGlyphBoxFits() {
        // View of 40px: lines at 0/10/20/30 fit (30 + 8 <= 40), a line at 40 does not.
        assertTrue(FormationDetailPanel.visible(30, 1, 0, 40));
        assertFalse(FormationDetailPanel.visible(40, 1, 0, 40));
        assertFalse(FormationDetailPanel.visible(33, 1, 0, 40), "no half-cut glyphs");
        // A wrapped two-line entry is all or nothing.
        assertTrue(FormationDetailPanel.visible(20, 2, 0, 40));
        assertFalse(FormationDetailPanel.visible(30, 2, 0, 40));
        // Scrolled by one line: the first line is gone, the fifth one appears.
        assertFalse(FormationDetailPanel.visible(0, 1, 10, 40));
        assertTrue(FormationDetailPanel.visible(40, 1, 10, 40));
    }

    @Test
    void sectionsFlowIntoOneToThreeColumns() {
        assertEquals(1, FormationDetailPanel.columns(0));
        assertEquals(1, FormationDetailPanel.columns(299));
        assertEquals(2, FormationDetailPanel.columns(300));
        assertEquals(2, FormationDetailPanel.columns(479));
        assertEquals(3, FormationDetailPanel.columns(480));
    }

    /**
     * Detail columns on the D2 tablet (plan 4.8): the 960×720 GUI-1 window (480×360) stays wide,
     * but with the inner width down to 416 its detail is a single scrolling column, like 480×270;
     * 640×336 gets two and 960×540 three.
     */
    @ParameterizedTest(name = "{0}x{1} -> {2} column(s)")
    @CsvSource({"480, 360, 1", "480, 270, 1", "640, 336, 2", "640, 360, 2", "960, 540, 3"})
    void wideTiersGetTheirDetailColumns(int width, int height, int columns) {
        FormationScreenLayout layout = FormationScreenLayout.compute(width, height, 2, true, true,
                false, false, 2 * 13 + 4 * 18 + 2, 0);
        assertEquals(FormationScreenLayout.Mode.WIDE, layout.mode());
        assertEquals(columns, FormationDetailPanel.columns(layout.content().width()),
                "detail content " + layout.content());
    }

    @Test
    void preScaledEmblemsSitNextToTheSource() {
        assertEquals("wok_infantry:textures/gui/formations/millennium_seminar_mobile_32.png",
                FormationDetailPanel.preScaledPath(
                        "wok_infantry:textures/gui/formations/millennium_seminar_mobile.png", 32));
        assertNull(FormationDetailPanel.preScaledPath("wok_infantry:textures/gui/x.jpg", 64));
        assertNull(FormationDetailPanel.preScaledPath(null, 64));
    }
}
