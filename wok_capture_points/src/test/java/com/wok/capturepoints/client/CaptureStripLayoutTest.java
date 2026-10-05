package com.wok.capturepoints.client;

import com.wok.capturepoints.client.CaptureStripLayout.Plate;
import com.wok.capturepoints.client.CaptureStripLayout.Row;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureStripLayoutTest {
    /** Window sizes of the acceptance tiers: {guiWidth, guiHeight, guiScale}. */
    private static final List<int[]> TIERS = List.of(
            new int[]{320, 240, 3}, new int[]{960, 720, 1}, new int[]{640, 336, 2},
            new int[]{480, 270, 2}, new int[]{854, 480, 1});

    @Test
    void factorFollowsTheCoreHudsTwoTimesRule() {
        assertEquals(2, CaptureStripLayout.factor(1.0, 960, 720), "960×720 at GUI 1 → 480×360");
        assertEquals(2, CaptureStripLayout.factor(1.0, 854, 480), "854×480 at GUI 1 → 427×240");
        assertEquals(1, CaptureStripLayout.factor(1.0, 600, 400), "half would be under 320×240");
        assertEquals(1, CaptureStripLayout.factor(2.0, 640, 336));
        assertEquals(1, CaptureStripLayout.factor(3.0, 320, 240));
    }

    /** {@link CaptureStripLayout#place} without boss bars. */
    private static Plate place(int guiWidth, int guiHeight, int factor, int[] slot,
                               boolean core) {
        return CaptureStripLayout.place(guiWidth, guiHeight, factor, slot, core, false,
                Integer.MAX_VALUE, 0);
    }

    /**
     * {@link CaptureStripLayout#place} under one vanilla boss bar at y 12 moved down
     * {@code shift}, with a core older than 0.5.0-beta.2 (or none).
     */
    private static Plate placeUnderBoss(int guiWidth, int guiHeight, int factor, int[] slot,
                                        boolean core, int shift) {
        return CaptureStripLayout.place(guiWidth, guiHeight, factor, slot, core, false,
                CaptureStripLayout.bossTop(12, shift), CaptureStripLayout.bossBottom(12, shift));
    }

    /** A core 0.5.0-beta.2+ moves the bars below a strip that starts in the first boss row. */
    @Test
    void aNewCoreClearsTheFirstBossRowItself() {
        int[] top = {170, 4, 300, 160};
        int one = 12;
        assertEquals(4, CaptureStripLayout.place(640, 336, 1, top, true, true,
                CaptureStripLayout.bossTop(one, 0), CaptureStripLayout.bossBottom(one, 0)).top(),
                "bars still in place for one frame: the core moves them, the strip stays");
        int[] belowIcons = {170, 29, 300, 140};
        assertEquals(59, CaptureStripLayout.place(640, 336, 1, belowIcons, true, true,
                CaptureStripLayout.bossTop(one, 0), CaptureStripLayout.bossBottom(50, 0)).top(),
                "a strip under the first row (status-effect icons): the core leaves the bars, "
                        + "so the strip goes under the third");
    }

    @Test
    void narrowScreenTakesTheCoreSlotInOneRow() {
        // core 0.5.0-beta.1 at 320×240: top_center_next = {123, 22, 195, 98}
        Plate plate = place(320, 240, 1, new int[]{123, 22, 195, 98}, true);
        assertEquals(new Plate(1, 123, 22, 318, 36, false), plate);
        assertArrayEquals(new int[]{123, 22, 195, 14}, plate.guiRect());
    }

    @Test
    void wideScreenAtGuiOneIsDrawnAtTwiceWithTheStatusRow() {
        // the 2× core HUD: strip column [184, 476) layout = [368, 952) GUI, next row at 25 → 50
        Plate plate = place(960, 720, 2, new int[]{368, 50, 584, 310}, true);
        assertEquals(new Plate(2, 230, 25, 430, 48, true), plate, "200 wide, centred in the slot");
        assertArrayEquals(new int[]{460, 50, 400, 46}, plate.guiRect());
        assertEquals(CaptureStripLayout.HEIGHT_WIDE, plate.height());
    }

    @Test
    void withoutASlotItKeepsItsOwnPlace() {
        assertEquals(new Plate(1, 220, 4, 420, 27, true),
                place(640, 336, 1, null, false), "alone: top edge");
        assertEquals(new Plate(1, 60, 4, 260, 18, false), place(320, 240, 1, null, false));
        assertEquals(14, place(960, 720, 2, null, true).top(),
                "a core without InfantryHudApi: under its old banner (GUI y 28)");
        assertEquals(new Plate(1, 60, 4, 260, 18, false),
                place(320, 240, 1, new int[]{0, 0, 0, 0}, false), "an empty slot counts as none");
    }

    /**
     * Review fix: a core without the slot API on a narrow screen keeps its roster at the top left
     * (x 8–128); like alpha.3's compact panel the strip then starts at x 140, y 26.
     */
    @Test
    void narrowScreenWithAnOldCoreAndNoSlotStaysRightOfTheRoster() {
        assertEquals(new Plate(1, 140, 26, 312, 40, false), place(320, 240, 1, null, true));
        assertEquals(new Plate(1, 140, 26, 340, 40, false), place(360, 240, 1, null, true),
                "200 wide at most");
        assertEquals(new Plate(1, 80, 28, 280, 42, false), place(361, 240, 1, null, true),
                "wider screens: centred under the old banner");
    }

    @Test
    void itMovesBelowVanillaBossBarsItWouldCover() {
        // one boss bar in place: name row from y 3, bar 12..17
        assertEquals(21, placeUnderBoss(640, 336, 1, null, false, 0).top(),
                "4px under the bar");
        assertEquals(11, placeUnderBoss(960, 720, 2, null, false, 0).top(),
                "ceil(21 / 2) at the 2x size");
        assertEquals(40, placeUnderBoss(320, 240, 1, new int[]{123, 40, 195, 80}, true, 0).top(),
                "already below the bars: stays");
        assertEquals(4, placeUnderBoss(1200, 600, 1, new int[]{900, 4, 200, 80}, true, 19).top(),
                "outside the boss column: stays");
        int[] slot = {170, 4, 300, 160};
        assertEquals(4, placeUnderBoss(640, 336, 1, slot, true, 28).top(),
                "bars a core moved 4px under the strip (name row at 4 + 23 + 4): stays");
        assertEquals(30, placeUnderBoss(640, 336, 1, slot, true, 9).top(),
                "bars moved only part of the way still cover it: under them");
    }

    @Test
    void everyTierStaysOnScreenAndAtMostTwoHundredWide() {
        for (int[] tier : TIERS) {
            int factor = CaptureStripLayout.factor(tier[2], tier[0], tier[1]);
            for (boolean core : new boolean[]{false, true}) {
                Plate plate = place(tier[0], tier[1], factor, null, core);
                int[] gui = plate.guiRect();
                String where = tier[0] + "x" + tier[1] + "@" + tier[2] + " core=" + core;
                assertTrue(plate.width() <= CaptureStripLayout.MAX_WIDTH, where);
                assertTrue(gui[0] >= 0 && gui[0] + gui[2] <= tier[0], where + ": on screen");
                assertTrue(plate.height() <= CaptureStripLayout.HEIGHT_WIDE, where);
                assertEquals(plate.wide(), tier[0] / factor >= 400 && tier[1] / factor >= 280,
                        where + ": the status row only on the core HUD's non-tight tier");
            }
        }
    }

    @Test
    void rowGivesTheBarItsMinimumBeforeTheName() {
        Plate plate = new Plate(1, 123, 22, 318, 36, false);
        Row row = CaptureStripLayout.row(plate, 24, 6, 6);
        assertEquals(new Row(127, 140, 155, 160, 169, 305, 237, 308, 25, 34, 187), row);
        Row cramped = CaptureStripLayout.row(plate, 300, 6, 6);
        assertEquals(140, cramped.nameRoom(), "the name is shortened");
        assertEquals(CaptureStripLayout.BAR_MIN, cramped.barRight() - cramped.barLeft());
        Row tiny = CaptureStripLayout.row(new Plate(1, 0, 0, 40, 14, false), 24, 6, 6);
        assertEquals(0, tiny.nameRoom(), "no room for a name at all");
        assertEquals(4, tiny.blueX());
        assertTrue(tiny.barRight() >= tiny.barLeft());
        assertTrue(row.barTop() > row.textY() && row.barBottom() < row.textY() + 8);
    }

    @Test
    void barFillsFromTheCentreTowardsTheLeadingSide() {
        Row row = CaptureStripLayout.row(new Plate(1, 123, 22, 318, 36, false), 24, 6, 6);
        assertArrayEquals(new int[]{203, 237}, CaptureStripLayout.fill(row, 0.5), "blue: left");
        assertArrayEquals(new int[]{237, 254}, CaptureStripLayout.fill(row, -0.25), "red: right");
        assertArrayEquals(new int[]{169, 237}, CaptureStripLayout.fill(row, 1.0));
        int[] none = CaptureStripLayout.fill(row, 0.0);
        assertFalse(none[1] > none[0]);
        int[] nan = CaptureStripLayout.fill(row, Double.NaN);
        assertFalse(nan[1] > nan[0]);
    }
}
