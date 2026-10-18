package com.wok.infantry.client.screen;

import com.wok.infantry.client.hud.TacticalHud;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The formation page's own drawing in the P3 liveries (0.5.0-beta.3): colours are read while a
 * frame's palette is applied, never kept from the moment a page was built.
 */
class FormationPaletteTest {
    @Test
    void theBrowsingFactionKeyIsWashedInTheFramesSelectionColour() {
        int outside = FormationFactionButton.browsingFill(false);
        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.NEUTRAL)) {
            assertEquals(TacticalHud.mix(TacticalPalette.NEUTRAL.get(PaletteToken.CARD),
                            TacticalPalette.NEUTRAL.get(PaletteToken.SELECT_BAR), 0.3D),
                    FormationFactionButton.browsingFill(false));
            assertNotEquals(FormationFactionButton.browsingFill(false),
                    FormationFactionButton.browsingFill(true), "hover deepens the wash");
        }
        assertEquals(outside, FormationFactionButton.browsingFill(false), "A again after the frame");
    }

    @Test
    void theBrowsingLabelClearsItsSelectionBarLikeASelectedKey() {
        assertEquals(TacticalButtonStyle.barWidth(18) + 3,
                FormationFactionButton.browsingPadLeft(18));
        assertEquals(6, FormationFactionButton.browsingPadLeft(16));
        assertEquals(5, FormationFactionButton.browsingPadLeft(14), "tight keys keep the 2px bar");
    }

    @Test
    void aHoveredSelectedFormationRowLightsUpOneStep() {
        // Dimmed rows (停用 / 容量不足) stay selectable: the list marks them disabled, the row
        // background does not.
        TacticalDraw.RowState hovered = new TacticalDraw.RowState(true, true, true, false);
        TacticalDraw.RowState background = FormationListWidget.background(hovered);
        assertFalse(background.disabled());
        assertTrue(background.brightensSelection());
        assertFalse(FormationListWidget.background(null).selected());

        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.ACADEMY)) {
            int bar = TacticalPalette.ACADEMY.get(PaletteToken.SELECT_BAR);
            assertEquals(TacticalHud.mix(TacticalPalette.ACADEMY.get(PaletteToken.SELECT_HOVER),
                    bar, 0.3D), FormationListWidget.shareTrack(background),
                    "the vote-share track follows the brighter fill");
            assertEquals(TacticalHud.mix(TacticalPalette.ACADEMY.get(PaletteToken.SELECT),
                    bar, 0.3D), FormationListWidget.shareTrack(
                    new TacticalDraw.RowState(true, false, false, false)));
            assertEquals(TacticalPalette.ACADEMY.get(PaletteToken.WELL),
                    FormationListWidget.shareTrack(TacticalDraw.RowState.NORMAL));
        }
    }

    @Test
    void voteInfoRowsAreColouredWhenDrawnNotWhenPlanned() {
        FormationVotePanel.Data open = new FormationVotePanel.Data(true, true, true,
                Component.literal("学院军"), 1, 18, null, List.of(), Component.empty());
        FormationVotePanel.Data waiting = new FormationVotePanel.Data(true, false, true,
                Component.literal("学院军"), 0, 18, null, List.of(), Component.empty());
        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.NEUTRAL)) {
            int text = TacticalPalette.NEUTRAL.get(PaletteToken.TEXT);
            int muted = TacticalPalette.NEUTRAL.get(PaletteToken.MUTED);
            assertEquals(text, FormationVotePanel.InfoRow.MINE.of(open).color());
            assertEquals(muted, FormationVotePanel.InfoRow.MINE.of(waiting).color());
            assertEquals(muted, FormationVotePanel.InfoRow.DUE.of(open).color());
            assertEquals(text, FormationVotePanel.InfoRow.EXTRA.of(open).color());
        }
    }
}
