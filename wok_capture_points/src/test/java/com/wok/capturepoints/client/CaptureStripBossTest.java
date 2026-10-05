package com.wok.capturepoints.client;

import com.wok.capturepoints.client.CaptureStripLayout.Plate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Review fix (0.1.0-alpha.4): the thin strip and a core that moves the vanilla boss bars below
 * it must settle instead of chasing each other. WOK步战核心 moves the bars below its top-centre
 * plates and below the strip it is told about ({@code panelRect}, one frame old); the strip moves
 * below bars that cover it. Before the fix the strip also moved below bars the core had already
 * put under it, the core then moved them under the moved strip, and so on: with core
 * 0.5.0-beta.1 and its battle strip on, one boss bar sent the strip off the screen in a few
 * frames. The core's boss shift is copied here (GUI pixels) so the add-on test needs no core.
 */
class CaptureStripBossTest {
    private static final int FRAMES = 12;

    /** WOK步战核心's boss shift: 0.5.0-beta.1, 0.5.0-beta.2, or none (no core). */
    private enum Core {
        BETA_1, BETA_2, NONE;

        int shift(int guiWidth, int topCenterBottomGui, int[] capture) {
            if (this == NONE) {
                return 0;
            }
            boolean inColumn = capture != null && capture[0] < guiWidth / 2 + 91
                    && capture[0] + capture[2] > guiWidth / 2 - 91;
            if (topCenterBottomGui <= 0) {
                if (this == BETA_1 || !inColumn || capture[1] >= 12 + 5 + 4) {
                    return 0;
                }
                return capture[1] + capture[3] + 4 - 3;
            }
            int clear = topCenterBottomGui;
            if (inColumn) {
                clear = Math.max(clear, capture[1] + capture[3]);
            }
            return Math.max(0, clear + 4 - 3);
        }
    }

    /** Frame by frame; returns the last drawn strip after checking every frame and the end. */
    private static Plate run(Core core, int guiWidth, int guiHeight, int factor, int[] slot,
                             int topCenterBottomGui, List<Integer> bars, boolean barsBefore) {
        int lastTop = Integer.MAX_VALUE;
        int lastBottom = 0;
        if (barsBefore) {
            // the bars were on screen before the viewer walked into the point
            int shift = core.shift(guiWidth, topCenterBottomGui, null);
            lastTop = CaptureStripLayout.bossTop(bars.get(0), shift);
            lastBottom = CaptureStripLayout.bossBottom(bars.get(bars.size() - 1), shift);
        }
        Plate previous = null;
        int previousShift = -1;
        for (int frame = 0; frame < FRAMES; frame++) {
            String where = core + " " + guiWidth + "x" + guiHeight + " frame " + frame;
            int[] reported = CaptureStripLayout.place(guiWidth, guiHeight, factor, slot,
                    core != Core.NONE, core == Core.BETA_2, lastTop, lastBottom).guiRect();
            int shift = core.shift(guiWidth, topCenterBottomGui, reported);
            int top = CaptureStripLayout.bossTop(bars.get(0), shift);
            int bottom = CaptureStripLayout.bossBottom(bars.get(bars.size() - 1), shift);
            Plate drawn = CaptureStripLayout.place(guiWidth, guiHeight, factor, slot,
                    core != Core.NONE, core == Core.BETA_2, top, bottom);
            int[] gui = drawn.guiRect();
            assertTrue(gui[1] >= 0 && gui[1] + gui[3] <= guiHeight / 2, where + ": " + drawn);
            assertFalse(gui[1] < bottom && gui[1] + gui[3] > top,
                    where + ": strip " + drawn + " under bars " + top + ".." + bottom);
            if (frame >= 3) {
                assertEquals(previous, drawn, where + ": the strip settles");
                assertEquals(previousShift, shift, where + ": the bars settle");
            }
            previous = drawn;
            previousShift = shift;
            lastTop = top;
            lastBottom = bottom;
        }
        return previous;
    }

    @Test
    void oldCoreWithItsBattleStripOnKeepsTheStripInItsSlot() {
        // core 0.5.0-beta.1, strip 4..21 at 640×336, slot from 25
        int[] slot = {170, 25, 300, 143};
        for (boolean before : new boolean[]{false, true}) {
            assertEquals(25, run(Core.BETA_1, 640, 336, 1, slot, 21, List.of(12), before).top());
            assertEquals(25, run(Core.BETA_1, 640, 336, 1, slot, 21, List.of(12, 31, 50),
                    before).top(), "three bars");
        }
        // 960×720 at the 2× HUD: strip 4..21 layout = 8..42 GUI, slot from 50 GUI
        assertEquals(25, run(Core.BETA_1, 960, 720, 2, new int[]{368, 50, 584, 310}, 42,
                List.of(12), false).top());
    }

    @Test
    void oldCoreWithNothingOnTopLeavesTheBarsAndTheStripGoesUnderThem() {
        int[] slot = {170, 4, 300, 164};
        assertEquals(21, run(Core.BETA_1, 640, 336, 1, slot, 0, List.of(12), false).top());
        assertEquals(59, run(Core.BETA_1, 640, 336, 1, slot, 0, List.of(12, 31, 50), true)
                .top(), "under the lowest of three bars");
    }

    /**
     * With its strip off and nothing on top, core 0.5.0-beta.2 moves the bars below the thin
     * strip, whether the bars or the strip came first: the strip stays on top, so on narrow
     * screens the core can still move the bars right of the squad roster (a strip under the bars
     * would sit beside them as an obstacle and pin them over the roster).
     */
    @Test
    void newCoreWithItsStripOffKeepsTheStripOnTop() {
        int[] slot = {170, 4, 300, 164};
        int[] narrow = {123, 2, 195, 118};
        for (boolean before : new boolean[]{false, true}) {
            assertEquals(4, run(Core.BETA_2, 640, 336, 1, slot, 0, List.of(12), before).top());
            assertEquals(4, run(Core.BETA_2, 640, 336, 1, slot, 0, List.of(12, 31, 50), before)
                    .top(), "three bars");
            assertEquals(2, run(Core.BETA_2, 320, 240, 1, narrow, 0, List.of(12), before).top());
            assertEquals(4, run(Core.BETA_2, 960, 720, 2, new int[]{368, 8, 584, 352}, 0,
                    List.of(12), before).top());
        }
        // a notice 4..16 above the slot: the core moves the bars under everything
        int[] underNotice = {170, 20, 300, 148};
        for (boolean before : new boolean[]{false, true}) {
            assertEquals(20, run(Core.BETA_2, 640, 336, 1, underNotice, 16, List.of(12),
                    before).top());
        }
        // status-effect icons pushed the column below the first boss row: the core leaves the
        // bars, the strip goes under the lowest of them
        int[] belowIcons = {170, 29, 300, 139};
        for (boolean before : new boolean[]{false, true}) {
            assertEquals(29, run(Core.BETA_2, 640, 336, 1, belowIcons, 0, List.of(12), before)
                    .top(), "one bar ends above it");
            assertEquals(59, run(Core.BETA_2, 640, 336, 1, belowIcons, 0, List.of(12, 31, 50),
                    before).top());
        }
    }

    @Test
    void withoutTheCoreItGoesUnderTheBars() {
        for (boolean before : new boolean[]{false, true}) {
            assertEquals(21, run(Core.NONE, 640, 336, 1, null, 0, List.of(12), before).top());
            assertEquals(21, run(Core.NONE, 320, 240, 1, null, 0, List.of(12), before).top());
            assertEquals(11, run(Core.NONE, 960, 720, 2, null, 0, List.of(12), before).top(),
                    "ceil(21 / 2) at the 2× size");
        }
    }
}
