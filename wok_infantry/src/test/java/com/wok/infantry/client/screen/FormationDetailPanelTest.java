package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

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
    void preScaledEmblemsSitNextToTheSource() {
        assertEquals("wok_infantry:textures/gui/formations/millennium_seminar_mobile_32.png",
                FormationDetailPanel.preScaledPath(
                        "wok_infantry:textures/gui/formations/millennium_seminar_mobile.png", 32));
        assertNull(FormationDetailPanel.preScaledPath("wok_infantry:textures/gui/x.jpg", 64));
        assertNull(FormationDetailPanel.preScaledPath(null, 64));
    }
}
