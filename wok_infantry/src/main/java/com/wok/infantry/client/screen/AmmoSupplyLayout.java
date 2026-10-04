package com.wok.infantry.client.screen;

import com.wok.infantry.ammo.AmmoSupplyView;

/**
 * Pure geometry of the ammunition supply board (player-01, points part of player-14).
 *
 * <p>Every block is stacked by one vertical cursor below the board header: the remaining-points
 * readout and its meter, the infantry/vehicle mode tabs (large stations only), the section header
 * and the option list. Nothing sits at a fixed y any more, so the opaque section header can no
 * longer cover the remaining points of a small or medium crate. The meter starts after the
 * measured points text and moves to its own line when the remaining width is too narrow, so it
 * never paints over the text either.</p>
 */
record AmmoSupplyLayout(TacticalMapLayout.Layout board,
                        TacticalMapLayout.Rect panel,
                        TacticalMapLayout.Rect pointsText,
                        TacticalMapLayout.Rect meterBar,
                        TacticalMapLayout.Rect infantryTab,
                        TacticalMapLayout.Rect vehicleTab,
                        TacticalMapLayout.Rect sectionHeader,
                        TacticalMapLayout.Rect list,
                        int rowHeight) {
    static final int GUN_ROW_HEIGHT = 34;
    static final int VEHICLE_ROW_HEIGHT = 48;
    static final int TEXT_HEIGHT = 9;
    static final int TAB_HEIGHT = 20;
    /** Height painted by {@link TacticalBoardTheme#sectionHeader}. */
    static final int SECTION_HEADER_HEIGHT = 14;
    static final int MIN_METER_BAR_WIDTH = 40;
    /** The panel sits flush under the header, as before, so 320×240 keeps four gun rows. */
    private static final int PANEL_GAP = 0;
    private static final int PANEL_PADDING = 4;
    private static final int TEXT_INSET = 8;
    private static final int METER_TEXT_GAP = 8;
    private static final int LINE_GAP = 2;
    private static final int BLOCK_GAP = 4;
    private static final int TAB_GAP = 4;
    private static final int LIST_GAP = 2;
    private static final int FOOTER_GAP = 7;
    private static final TacticalMapLayout.Rect NONE = new TacticalMapLayout.Rect(0, 0, 0, 0);

    /**
     * @param vehicleMode     whether the vehicle list is shown; only large stations have it
     * @param pointsTextWidth measured width of the remaining-points line in GUI pixels
     */
    static AmmoSupplyLayout compute(int width, int height, AmmoSupplyView.TargetKind kind,
                                    boolean vehicleMode, int pointsTextWidth) {
        TacticalMapLayout.Layout board = TacticalMapLayout.compute(width, height);
        int screenWidth = Math.max(240, width);
        int margin = board.rich() ? 16 : 8;
        int panelLeft = margin;
        int panelRight = Math.max(panelLeft + 120, screenWidth - margin);
        int panelTop = board.header().bottom() + PANEL_GAP;
        int contentLeft = panelLeft + PANEL_PADDING;
        int contentRight = panelRight - PANEL_PADDING;

        int textLeft = panelLeft + TEXT_INSET;
        int meterRight = panelRight - TEXT_INSET;
        int lineTop = panelTop + PANEL_PADDING;
        int textWidth = Math.max(0, Math.min(pointsTextWidth, meterRight - textLeft));
        TacticalMapLayout.Rect pointsText = new TacticalMapLayout.Rect(textLeft, lineTop,
                textLeft + textWidth, lineTop + TEXT_HEIGHT);
        int barLeft = pointsText.right() + METER_TEXT_GAP;
        TacticalMapLayout.Rect meterBar;
        if (meterRight - barLeft >= MIN_METER_BAR_WIDTH) {
            meterBar = new TacticalMapLayout.Rect(barLeft, lineTop, meterRight,
                    lineTop + TEXT_HEIGHT);
        } else {
            int barTop = pointsText.bottom() + LINE_GAP;
            meterBar = new TacticalMapLayout.Rect(textLeft, barTop, meterRight,
                    barTop + TEXT_HEIGHT);
        }
        int cursor = Math.max(pointsText.bottom(), meterBar.bottom()) + BLOCK_GAP;

        boolean tabs = hasModeTabs(kind);
        TacticalMapLayout.Rect infantryTab = NONE;
        TacticalMapLayout.Rect vehicleTab = NONE;
        if (tabs) {
            int tabWidth = Math.max(50, Math.min(112,
                    (contentRight - contentLeft - TAB_GAP) / 2));
            infantryTab = new TacticalMapLayout.Rect(contentLeft, cursor,
                    contentLeft + tabWidth, cursor + TAB_HEIGHT);
            vehicleTab = new TacticalMapLayout.Rect(infantryTab.right() + TAB_GAP, cursor,
                    infantryTab.right() + TAB_GAP + tabWidth, cursor + TAB_HEIGHT);
            cursor = infantryTab.bottom() + BLOCK_GAP;
        }

        TacticalMapLayout.Rect sectionHeader = new TacticalMapLayout.Rect(contentLeft, cursor,
                contentRight, cursor + SECTION_HEADER_HEIGHT);
        int listTop = sectionHeader.bottom() + LIST_GAP;
        int rowHeight = vehicleMode && tabs ? VEHICLE_ROW_HEIGHT : GUN_ROW_HEIGHT;
        int listBottom = Math.max(listTop + rowHeight, board.footer().top() - FOOTER_GAP);
        TacticalMapLayout.Rect list = new TacticalMapLayout.Rect(contentLeft, listTop,
                contentRight, listBottom);
        TacticalMapLayout.Rect panel = new TacticalMapLayout.Rect(panelLeft, panelTop,
                panelRight, listBottom + 1);
        return new AmmoSupplyLayout(board, panel, pointsText, meterBar, infantryTab,
                vehicleTab, sectionHeader, list, rowHeight);
    }

    /** Only large stations and blocks offer the infantry/vehicle switch. */
    static boolean hasModeTabs(AmmoSupplyView.TargetKind kind) {
        return kind == AmmoSupplyView.TargetKind.LARGE_STATION
                || kind == AmmoSupplyView.TargetKind.LARGE_BLOCK;
    }

    boolean modeTabs() {
        return infantryTab.width() > 0;
    }

    /** Whole rows that fit in the list; at least one so a tiny window still offers a choice. */
    int visibleRows() {
        return Math.max(1, list.height() / rowHeight);
    }

    int rowTop(int visibleIndex) {
        return list.top() + visibleIndex * rowHeight;
    }
}
