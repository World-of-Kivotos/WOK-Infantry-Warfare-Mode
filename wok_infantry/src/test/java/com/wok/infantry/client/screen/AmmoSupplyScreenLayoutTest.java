package com.wok.infantry.client.screen;

import com.wok.infantry.ammo.AmmoSupplyView;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * player-01: the section header of a small or medium crate used to be painted over the
 * remaining-points readout. Every tier and every crate kind must now stack its blocks without
 * any overlap and keep the list above the footer.
 */
final class AmmoSupplyScreenLayoutTest {
    private static final int[][] TIERS = {
            {320, 240}, {480, 270}, {640, 360}, {960, 540}, {960, 720}
    };
    private static final AmmoSupplyView.TargetKind[] CRATES = {
            AmmoSupplyView.TargetKind.SMALL_CRATE,
            AmmoSupplyView.TargetKind.MEDIUM_CRATE,
            AmmoSupplyView.TargetKind.LARGE_STATION
    };
    /** Roughly "剩余补给：100 / 100 点" and "Supply remaining: 1500 / 1500 points". */
    private static final int[] POINTS_TEXT_WIDTHS = {96, 204};

    @Test
    void everyTierAndCrateStacksBlocksWithoutOverlap() {
        for (int[] tier : TIERS) {
            for (AmmoSupplyView.TargetKind kind : CRATES) {
                for (int textWidth : POINTS_TEXT_WIDTHS) {
                    assertStacked(tier[0], tier[1], kind, false, textWidth);
                    if (AmmoSupplyLayout.hasModeTabs(kind)) {
                        assertStacked(tier[0], tier[1], kind, true, textWidth);
                    }
                }
            }
        }
    }

    @Test
    void smallAndMediumCratePointsStayVisibleAboveTheSectionHeader() {
        for (int[] tier : TIERS) {
            for (AmmoSupplyView.TargetKind kind : new AmmoSupplyView.TargetKind[]{
                    AmmoSupplyView.TargetKind.SMALL_CRATE,
                    AmmoSupplyView.TargetKind.MEDIUM_CRATE}) {
                AmmoSupplyLayout layout = AmmoSupplyLayout.compute(tier[0], tier[1], kind,
                        false, 96);
                String where = describe(tier[0], tier[1], kind, false, 96);
                assertFalse(layout.modeTabs(), where + ": crates have no mode tabs");
                assertTrue(layout.pointsText().bottom() <= layout.sectionHeader().top(),
                        where + ": points readout must stay above the section header");
                assertTrue(layout.meterBar().bottom() <= layout.sectionHeader().top(),
                        where + ": meter must stay above the section header");
                // The old fixed coordinates put the header at y=52 over points drawn at 51..60.
                assertTrue(layout.sectionHeader().top() >= layout.pointsText().top()
                        + AmmoSupplyLayout.TEXT_HEIGHT, where);
            }
        }
    }

    @Test
    void largeStationShowsBothModeTabsBetweenMeterAndHeader() {
        for (int[] tier : TIERS) {
            for (AmmoSupplyView.TargetKind kind : new AmmoSupplyView.TargetKind[]{
                    AmmoSupplyView.TargetKind.LARGE_STATION,
                    AmmoSupplyView.TargetKind.LARGE_BLOCK}) {
                AmmoSupplyLayout layout = AmmoSupplyLayout.compute(tier[0], tier[1], kind,
                        true, 204);
                String where = describe(tier[0], tier[1], kind, true, 204);
                assertTrue(layout.modeTabs(), where);
                assertEquals(AmmoSupplyLayout.TAB_HEIGHT, layout.infantryTab().height(), where);
                assertEquals(layout.infantryTab().width(), layout.vehicleTab().width(), where);
                assertTrue(layout.infantryTab().right() < layout.vehicleTab().left(), where);
                assertTrue(layout.meterBar().bottom() <= layout.infantryTab().top(), where);
                assertTrue(layout.infantryTab().bottom() <= layout.sectionHeader().top(), where);
                assertEquals(AmmoSupplyLayout.VEHICLE_ROW_HEIGHT, layout.rowHeight(), where);
            }
        }
    }

    @Test
    void meterStartsAfterTheMeasuredTextAndWrapsWhenTooNarrow() {
        AmmoSupplyLayout roomy = AmmoSupplyLayout.compute(320, 240,
                AmmoSupplyView.TargetKind.LARGE_STATION, false, 204);
        assertEquals(roomy.pointsText().top(), roomy.meterBar().top(),
                "a meter that fits shares the points line");
        assertTrue(roomy.meterBar().left() > roomy.pointsText().right());
        assertTrue(roomy.meterBar().width() >= AmmoSupplyLayout.MIN_METER_BAR_WIDTH);

        AmmoSupplyLayout crowded = AmmoSupplyLayout.compute(320, 240,
                AmmoSupplyView.TargetKind.SMALL_CRATE, false, 270);
        assertTrue(crowded.meterBar().top() >= crowded.pointsText().bottom(),
                "a meter without room moves below the text instead of covering it");
        assertEquals(crowded.pointsText().left(), crowded.meterBar().left());
        assertStacked(320, 240, AmmoSupplyView.TargetKind.SMALL_CRATE, false, 270);

        AmmoSupplyLayout overlong = AmmoSupplyLayout.compute(320, 240,
                AmmoSupplyView.TargetKind.MEDIUM_CRATE, false, 10_000);
        assertTrue(overlong.pointsText().right() <= overlong.panel().right(),
                "an over-long translation is clipped to the panel");
        assertStacked(320, 240, AmmoSupplyView.TargetKind.MEDIUM_CRATE, false, 10_000);
    }

    @Test
    void rowsFillTheListAndTheModeOnlyChangesTheRowHeight() {
        AmmoSupplyLayout infantry = AmmoSupplyLayout.compute(320, 240,
                AmmoSupplyView.TargetKind.LARGE_STATION, false, 96);
        AmmoSupplyLayout vehicle = AmmoSupplyLayout.compute(320, 240,
                AmmoSupplyView.TargetKind.LARGE_STATION, true, 96);
        assertEquals(AmmoSupplyLayout.GUN_ROW_HEIGHT, infantry.rowHeight());
        assertEquals(AmmoSupplyLayout.VEHICLE_ROW_HEIGHT, vehicle.rowHeight());
        assertEquals(infantry.list(), vehicle.list());
        assertTrue(infantry.visibleRows() > vehicle.visibleRows());
        // The vehicle flag means nothing for a crate without the vehicle list.
        assertEquals(AmmoSupplyLayout.GUN_ROW_HEIGHT, AmmoSupplyLayout.compute(320, 240,
                AmmoSupplyView.TargetKind.SMALL_CRATE, true, 96).rowHeight());
        assertEquals(infantry.list().top(), infantry.rowTop(0));
        assertEquals(infantry.list().top() + 2 * AmmoSupplyLayout.GUN_ROW_HEIGHT,
                infantry.rowTop(2));
    }

    @Test
    void tinyWindowStillOffersOneRow() {
        AmmoSupplyLayout layout = AmmoSupplyLayout.compute(100, 80,
                AmmoSupplyView.TargetKind.LARGE_STATION, true, 96);
        assertEquals(1, layout.visibleRows());
        assertTrue(layout.list().height() >= layout.rowHeight());
    }

    private static void assertStacked(int width, int height, AmmoSupplyView.TargetKind kind,
                                      boolean vehicleMode, int textWidth) {
        AmmoSupplyLayout layout = AmmoSupplyLayout.compute(width, height, kind, vehicleMode,
                textWidth);
        String where = describe(width, height, kind, vehicleMode, textWidth);
        TacticalMapLayout.Layout board = layout.board();
        TacticalMapLayout.Rect panel = layout.panel();

        assertTrue(panel.left() >= 0 && panel.right() <= width, where + ": panel width");
        assertTrue(panel.top() >= board.header().bottom(), where + ": panel below header");
        assertTrue(panel.bottom() <= board.footer().top(), where + ": panel above footer");

        Map<String, TacticalMapLayout.Rect> blocks = new LinkedHashMap<>();
        blocks.put("points", layout.pointsText());
        blocks.put("meter", layout.meterBar());
        if (layout.modeTabs()) {
            blocks.put("infantryTab", layout.infantryTab());
            blocks.put("vehicleTab", layout.vehicleTab());
        }
        blocks.put("sectionHeader", layout.sectionHeader());
        blocks.put("list", layout.list());
        assertEquals(AmmoSupplyLayout.hasModeTabs(kind), layout.modeTabs(), where);

        List<String> names = new ArrayList<>(blocks.keySet());
        for (String name : names) {
            TacticalMapLayout.Rect block = blocks.get(name);
            assertTrue(block.width() > 0 && block.height() > 0, where + ": empty " + name);
            assertTrue(block.left() >= panel.left() && block.right() <= panel.right()
                            && block.top() >= panel.top() && block.bottom() <= panel.bottom(),
                    where + ": " + name + " " + block + " escapes panel " + panel);
        }
        for (int first = 0; first < names.size(); first++) {
            for (int second = first + 1; second < names.size(); second++) {
                TacticalMapLayout.Rect a = blocks.get(names.get(first));
                TacticalMapLayout.Rect b = blocks.get(names.get(second));
                assertFalse(a.intersects(b), where + ": " + names.get(first) + " " + a
                        + " overlaps " + names.get(second) + " " + b);
            }
        }
        assertEquals(AmmoSupplyLayout.SECTION_HEADER_HEIGHT, layout.sectionHeader().height(),
                where);
        assertTrue(layout.sectionHeader().bottom() <= layout.list().top(), where);
        assertTrue(layout.visibleRows() >= 1, where);
        assertTrue(layout.rowTop(layout.visibleRows()) <= layout.list().bottom(),
                where + ": the last visible row must end inside the list");
    }

    private static String describe(int width, int height, AmmoSupplyView.TargetKind kind,
                                   boolean vehicleMode, int textWidth) {
        return width + "x" + height + " " + kind + (vehicleMode ? " vehicle" : " infantry")
                + " text=" + textWidth;
    }
}
