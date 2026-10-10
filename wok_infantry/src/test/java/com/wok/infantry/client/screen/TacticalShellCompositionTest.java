package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalShellLayout.Density;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The merged shell layers agree with each other: the device art ({@link DeviceArt}), the shell's
 * bezel plan ({@link TacticalBoardChrome#planBezel}) and the hardware keys
 * ({@link TacticalBezelPlan}, {@link BezelKey}, the {@link TacticalTabStrip.Skin#BEZEL} strip)
 * share one geometry on every tier and livery.
 */
class TacticalShellCompositionTest {
    /** "Esc 关闭" and "R 刷新" in the game font: Esc 18px, R 6px, two CJK glyphs 18px. */
    private static final int ESC_TEXT = 18;
    private static final int R_TEXT = 6;
    private static final int ACTION_TEXT = 18;
    /** The battle terminal's six page names are two CJK glyphs each. */
    private static final int PAGE_LABEL = 18;
    private static final int PAGE_COUNT = BattleTab.values().length;

    private static final Set<Integer> NOT_CASE = Set.of(DeviceArt.GLASS, DeviceArt.SCREW,
            DeviceArt.SCREW_HEAD, DeviceArt.SCREW_SLOT, DeviceArt.LENS, DeviceArt.LENS_HI);

    private static TacticalBezelPlan keys(TacticalShellLayout layout) {
        Density density = layout.density();
        return TacticalBezelPlan.plan(layout.bezel(), density,
                TacticalBezelPlan.pairWidth(density, ESC_TEXT, ACTION_TEXT),
                TacticalBezelPlan.pairWidth(density, R_TEXT, ACTION_TEXT));
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "427, 240", "480, 270", "480, 360", "640, 336", "640, 360",
            "960, 540"})
    void theShellsBezelPlanIsTheKeysBezelPlan(int width, int height) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        TacticalBoardChrome.BezelPlan shell = TacticalBoardChrome.planBezel(layout);
        TacticalBezelPlan keys = keys(layout);
        UiRect row = shell.keyRow();

        assertEquals(row.left(), keys.esc().left(), "Esc starts the key row");
        assertEquals(row.right(), keys.refresh().right(), "R ends it");
        assertEquals(row.top(), keys.keyTop());
        assertEquals(row.bottom(), keys.keyBottom());
        assertEquals(shell.ledTop(), keys.ledTop());
        assertEquals(shell.ledHeight(), keys.ledHeight());
        assertTrue(keys.hasLeds(), "every tier has room for the page LEDs");
        assertTrue(keys.esc().right() <= keys.pages().left()
                && keys.pages().right() <= keys.refresh().left(), "page keys between Esc and R");
        assertTrue(keys.esc().top() > layout.glass().bottom(), "below the glass and its lip");
        assertTrue(layout.bezel().contains(keys.esc()) && layout.bezel().contains(keys.refresh()));
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "427, 240", "480, 270", "480, 360", "640, 336", "640, 360",
            "960, 540"})
    void theSixTerminalPagesFitBetweenEscAndR(int width, int height) {
        TacticalBezelPlan keys = keys(TacticalShellLayout.compute(width, height));
        int keyWidth = keys.keyWidth(PAGE_LABEL);

        assertTrue(keys.rowWidth(PAGE_COUNT, keyWidth) <= keys.pages().width(),
                "full names, no pager on " + width + "x" + height);
        for (UiRect key : keys.keys(PAGE_COUNT, keyWidth)) {
            assertTrue(keys.pages().contains(key), key + " inside " + keys.pages());
        }
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "480, 360", "640, 336", "960, 540"})
    void everyKeyAndLedSitsOnThePaintedCase(int width, int height) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        TacticalBezelPlan keys = keys(layout);
        List<UiRect> parts = new ArrayList<>(List.of(keys.esc(), keys.refresh()));
        for (UiRect key : keys.keys(PAGE_COUNT, keys.keyWidth(PAGE_LABEL))) {
            parts.add(key);
            parts.add(keys.ledGlow(key));
        }
        for (TacticalLivery.Livery livery : TacticalLivery.Livery.values()) {
            int[] art = DeviceArt.rasterize(width, height, layout.density(), livery);
            for (UiRect part : parts) {
                assertFalse(part.isEmpty());
                for (int y = part.top(); y < part.bottom(); y++) {
                    for (int x = part.left(); x < part.right(); x++) {
                        int pixel = art[y * width + x];
                        String where = livery + " " + width + "x" + height + " (" + x + ", " + y
                                + ") in " + part;
                        assertEquals(0xFF, pixel >>> 24, "opaque case under " + where);
                        assertFalse(NOT_CASE.contains(pixel), "no glass or screw under " + where);
                    }
                }
            }
        }
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "480, 360", "640, 336", "960, 540"})
    void theGlassOverlayStaysOnTheGlass(int width, int height) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        DeviceArt.Runs glass = DeviceArt.glassOverlay(layout);

        assertTrue(glass.size() > 0);
        for (int index = 0; index < glass.size(); index++) {
            UiRect run = new UiRect(glass.left(index), glass.top(index), glass.right(index),
                    glass.bottom(index));
            assertTrue(layout.glass().contains(run), run + " inside " + layout.glass()
                    + ": the bezel keys and the case never get the sheen");
        }
    }
}
