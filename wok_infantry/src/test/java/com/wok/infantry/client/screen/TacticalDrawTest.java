package com.wok.infantry.client.screen;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalDrawTest {
    private static final TacticalShellLayout.Metrics STANDARD =
            TacticalShellLayout.Metrics.of(TacticalShellLayout.Density.STANDARD);

    @Test
    void panelContentSitsBelowTheSectionStripAndInsideThePadding() {
        UiRect bounds = new UiRect(0, 0, 200, 100);

        assertEquals(new UiRect(5, 6, 195, 95),
                TacticalDraw.panelContent(bounds, STANDARD, TacticalDraw.PanelStyle.plain()));
        assertEquals(new UiRect(5, 20, 195, 95), TacticalDraw.panelContent(bounds, STANDARD,
                TacticalDraw.PanelStyle.titled(Component.literal("能力"))),
                "1 + 14px strip + 1, then the pad (preview UI.panel)");
        assertEquals(new UiRect(2, 3, 198, 98), TacticalDraw.panelContent(bounds, STANDARD,
                TacticalDraw.PanelStyle.plain().withPad(2)));
        assertFalse(TacticalDraw.PanelStyle.titled(Component.empty()).hasTitle());
    }

    @Test
    void rowLayoutFollowsThePreviewRow() {
        TacticalDraw.RowLayout plain = TacticalDraw.rowLayout(new UiRect(0, 0, 200, 18), false,
                false, false, 0);
        assertEquals(5, plain.contentLeft());
        assertEquals(5, plain.titleY(), "8px glyphs centred in 18px");
        assertEquals(187, plain.titleRoom(), "8px clear of the right edge");
        assertFalse(plain.twoLines());
        assertEquals(4, plain.iconY());
        assertEquals(1, plain.itemY());

        assertEquals(17, TacticalDraw.rowLayout(new UiRect(0, 0, 200, 18), true, false, false, 0)
                .contentLeft(), "9×9 icon + 3px");
        assertEquals(23, TacticalDraw.rowLayout(new UiRect(0, 0, 200, 18), true, true, false, 0)
                .contentLeft(), "an item wins over the icon: 16px + 2px");

        TacticalDraw.RowLayout right = TacticalDraw.rowLayout(new UiRect(0, 0, 200, 18), false,
                false, false, 60);
        assertEquals(60, right.rightRoom());
        assertEquals(136, right.rightX());
        assertEquals(127, right.titleRoom());
        assertEquals(90, TacticalDraw.rowLayout(new UiRect(0, 0, 200, 18), false, false, false,
                150).rightRoom(), "the right value takes at most 45%");
    }

    @Test
    void secondLineOnlyWhenTheRowIsAtLeast20Tall() {
        TacticalDraw.RowLayout tall = TacticalDraw.rowLayout(new UiRect(0, 10, 200, 32), false,
                false, true, 0);
        assertTrue(tall.twoLines());
        assertEquals(12, tall.titleY());
        assertEquals(21, tall.subY());
        assertFalse(TacticalDraw.rowLayout(new UiRect(0, 0, 200, 18), false, false, true, 0)
                .twoLines());
    }

    @Test
    void rowFillFollowsSelectionHoverEmptyAndStripes() {
        TacticalDraw.RowState normal = TacticalDraw.RowState.NORMAL;

        assertEquals(TacticalBoardTheme.SELECT, TacticalDraw.rowFill(normal.withSelected(true)
                .withHovered(true), false));
        assertEquals(TacticalBoardTheme.ROW_HOVER, TacticalDraw.rowFill(normal.withHovered(true),
                false));
        assertEquals(TacticalBoardTheme.WELL_ROW, TacticalDraw.rowFill(normal.withHovered(true)
                .withDisabled(true), false), "disabled rows do not react to hover");
        assertEquals(TacticalBoardTheme.WELL, TacticalDraw.rowFill(normal, true));
        assertEquals(TacticalBoardTheme.WELL_ROW_ALT, TacticalDraw.rowFill(normal.withAlt(true),
                false));
        assertEquals(TacticalBoardTheme.WELL_ROW, TacticalDraw.rowFill(null, false));
    }

    @Test
    void disabledReasonDisablesARowSpec() {
        TacticalDraw.RowSpec spec = TacticalDraw.RowSpec.of("突击兵")
                .withRight(Component.literal("2/4"), TacticalBoardTheme.ACCENT_B)
                .withLead(TacticalBoardTheme.SUCCESS_B);

        assertFalse(spec.disabled());
        assertTrue(spec.withDisabledReason(Component.literal("名额已满")).disabled());
        assertFalse(spec.withDisabledReason(Component.literal("名额已满"))
                .withDisabledReason(null).disabled());
        assertEquals(TacticalBoardTheme.SUCCESS_B, spec.lead());
        assertEquals("", TacticalDraw.RowSpec.of((String) null).title().getString());
    }

    @Test
    void scrollThumbIsProportionalClampedAndNeverTooSmall() {
        UiRect track = new UiRect(0, 0, 2, 100);

        assertTrue(TacticalDraw.scrollThumb(track, 100, 100, 0).isEmpty(), "everything visible");
        assertEquals(new UiRect(0, 0, 2, 50), TacticalDraw.scrollThumb(track, 200, 100, 0));
        assertEquals(new UiRect(0, 50, 2, 100), TacticalDraw.scrollThumb(track, 200, 100, 100));
        assertEquals(new UiRect(0, 50, 2, 100), TacticalDraw.scrollThumb(track, 200, 100, 999));
        assertEquals(new UiRect(0, 0, 2, 50), TacticalDraw.scrollThumb(track, 200, 100, -5));
        assertEquals(TacticalDraw.MIN_THUMB,
                TacticalDraw.scrollThumb(track, 100_000, 100, 0).height());
        assertEquals(4, TacticalDraw.scrollThumb(new UiRect(0, 0, 2, 4), 1000, 10, 0).height(),
                "never taller than the track");
    }

    @Test
    void pagerKeysAreSquareishAndHitTested() {
        TacticalDraw.PagerLayout pager = TacticalDraw.pagerLayout(new UiRect(0, 0, 100, 14));

        assertEquals(new UiRect(0, 0, 16, 14), pager.back(), "min(h + 2, 18)");
        assertEquals(new UiRect(84, 0, 100, 14), pager.next());
        assertEquals(new UiRect(16, 0, 84, 14), pager.label());
        assertEquals(-1, TacticalDraw.pagerHit(new UiRect(0, 0, 100, 14), 5, 5));
        assertEquals(1, TacticalDraw.pagerHit(new UiRect(0, 0, 100, 14), 90, 5));
        assertEquals(0, TacticalDraw.pagerHit(new UiRect(0, 0, 100, 14), 50, 5));
        assertEquals(18, TacticalDraw.pagerLayout(new UiRect(0, 0, 100, 30)).back().width());
        assertEquals(5, TacticalDraw.pagerLayout(new UiRect(0, 0, 10, 14)).back().width(),
                "two keys never overlap");
    }

    @Test
    void emptyStateCutsHintLinesThatDoNotFit() {
        assertEquals(0, TacticalDraw.emptyHintCapacity(new UiRect(0, 0, 100, 34)));
        assertEquals(1, TacticalDraw.emptyHintCapacity(new UiRect(0, 0, 100, 35)));
        assertEquals(3, TacticalDraw.emptyHintCapacity(new UiRect(0, 0, 100, 60)));

        TacticalDraw.EmptyLayout centred = TacticalDraw.emptyLayout(new UiRect(0, 0, 100, 100), 2);
        assertEquals(new TacticalDraw.EmptyLayout(46, 29, 41, 52, 2), centred);
        TacticalDraw.EmptyLayout cramped = TacticalDraw.emptyLayout(new UiRect(0, 0, 100, 30), 5);
        assertEquals(0, cramped.hintLines(), "no hint line fits under icon and title");
        assertEquals(4, cramped.iconY(), "at least 4px from the top");
    }

    @Test
    void slotAndInputEdgesPutErrorsFirst() {
        assertEquals(TacticalBoardTheme.DANGER_B, TacticalDraw.slotEdge(true, true));
        assertEquals(TacticalBoardTheme.SELECT_BAR, TacticalDraw.slotEdge(true, false));
        assertEquals(TacticalBoardTheme.CELL_EDGE, TacticalDraw.slotEdge(false, false));

        assertEquals(TacticalBoardTheme.DANGER_B, TacticalDraw.inputEdge(true, true, false));
        assertEquals(TacticalBoardTheme.WELL_EDGE, TacticalDraw.inputEdge(true, false, false));
        assertEquals(TacticalBoardTheme.SELECT_B, TacticalDraw.inputEdge(true, false, true));
        assertEquals(TacticalBoardTheme.INPUT_EDGE, TacticalDraw.inputEdge(false, false, true));
    }
}
