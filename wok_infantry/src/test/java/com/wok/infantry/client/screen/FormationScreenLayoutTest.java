package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.FormationScreenLayout.Mode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationScreenLayoutTest {
    private static final int LIST_NEED = 2 * 13 + 4 * 18 + 2;
    private static final int JOIN_WIDTH = 150;

    /**
     * The acceptance sizes: 320×240, 427×240 (854×480 at GUI 1, laid out at 2x; narrow), 480×270,
     * the user's 1920×1008 at GUI 3 (640×336), 640×360, 960×540, the 960×720 GUI-1 window laid out
     * at 2x (480×360) and a raw 960×720.
     */
    @ParameterizedTest(name = "{0}x{1} -> {2}")
    @CsvSource({
            "320, 240, NARROW_LIST",
            "427, 240, NARROW_LIST",
            "480, 270, WIDE",
            "640, 336, WIDE",
            "640, 360, WIDE",
            "960, 540, WIDE",
            "480, 360, WIDE",
            "960, 720, WIDE"
    })
    void everyStateFitsTheBoardWithoutOverlaps(int width, int height, Mode listMode) {
        for (boolean joined : new boolean[]{false, true}) {
            for (boolean admin : new boolean[]{false, true}) {
                if (admin && !joined) {
                    continue;
                }
                FormationScreenLayout layout = FormationScreenLayout.compute(width, height, 2,
                        joined, admin, false, false, LIST_NEED, joined ? 0 : JOIN_WIDTH);
                assertEquals(listMode, layout.mode());
                checkCommon(layout, width + "x" + height + " joined=" + joined
                        + " admin=" + admin);
                if (listMode == Mode.NARROW_LIST) {
                    FormationScreenLayout detail = FormationScreenLayout.compute(width, height,
                            2, joined, admin, true, false, LIST_NEED, 0);
                    assertEquals(Mode.NARROW_DETAIL, detail.mode());
                    checkCommon(detail, width + "x" + height + " detail page");
                }
            }
        }
        FormationScreenLayout waiting = FormationScreenLayout.compute(width, height, 0, false,
                false, false, true, 0, 0);
        assertEquals(Mode.WAITING, waiting.mode());
        assertTrue(waiting.shell().body().contains(waiting.waitingPanel()));
    }

    private static void checkCommon(FormationScreenLayout layout, String label) {
        TacticalShellLayout.Metrics m = layout.shell().metrics();
        UiRect body = layout.shell().body();
        for (UiRect part : layout.parts()) {
            assertTrue(body.contains(part), label + ": " + part + " outside " + body);
        }
        List<UiRect> panels = layout.parts();
        for (int a = 0; a < panels.size(); a++) {
            for (int b = a + 1; b < panels.size(); b++) {
                assertFalse(panels.get(a).intersects(panels.get(b)),
                        label + ": " + panels.get(a) + " overlaps " + panels.get(b));
            }
        }
        if (!layout.strip().isEmpty()) {
            assertEquals(m.buttonHeight(), layout.strip().height(), label + " strip height");
            List<UiRect> keys = new ArrayList<>(layout.factionKeys());
            if (!layout.joinKey().isEmpty()) {
                keys.add(layout.joinKey());
            }
            for (UiRect key : keys) {
                assertTrue(layout.strip().contains(key), label + ": key " + key);
            }
            noOverlap(label + " strip keys", keys);
            if (!layout.status().isEmpty()) {
                for (UiRect key : keys) {
                    assertFalse(key.intersects(layout.status()), label + ": status under " + key);
                }
            }
        }
        if (!layout.listPanel().isEmpty()) {
            List<UiRect> inside = new ArrayList<>();
            for (UiRect part : new UiRect[]{layout.well(), layout.summary(), layout.preview(),
                    layout.admin(), layout.actionBar()}) {
                if (!part.isEmpty()) {
                    assertTrue(layout.listPanel().contains(part), label + ": " + part
                            + " outside the list panel");
                    inside.add(part);
                }
            }
            noOverlap(label + " list panel", inside);
            assertTrue(layout.well().height() >= 2 * FormationScreenLayout.rowHeight(m),
                    label + ": the well shows at least two rows");
            if (!layout.adminKey().isEmpty()) {
                assertEquals(m.buttonHeight(), layout.adminKey().height());
                assertTrue(layout.admin().contains(layout.adminKey()));
            }
            if (!layout.actionBar().isEmpty()) {
                assertEquals(m.buttonHeight(), layout.actionBar().height());
                assertTrue(layout.actionBar().contains(layout.detailsKey()));
                assertTrue(layout.actionBar().contains(layout.mainKey()));
                assertFalse(layout.detailsKey().intersects(layout.mainKey()));
            }
        }
        if (!layout.detailPanel().isEmpty()) {
            List<UiRect> inside = new ArrayList<>();
            for (UiRect part : new UiRect[]{layout.identity(), layout.content(),
                    layout.detailAction()}) {
                assertTrue(layout.detailPanel().contains(part), label + ": " + part
                        + " outside the detail panel");
                inside.add(part);
            }
            noOverlap(label + " detail panel", inside);
            assertEquals(m.buttonHeight(), layout.detailAction().height(), label + " action");
            assertEquals(FormationScreenLayout.emblemSize(m), layout.identity().height(),
                    label + " emblem row");
            assertTrue(layout.content().height() >= TacticalDraw.LINE_HEIGHT * 2,
                    label + ": the detail content shows at least two lines");
            assertTrue(layout.detailSeparatorY() > layout.content().bottom()
                    && layout.detailSeparatorY() < layout.detailAction().top(),
                    label + ": separator between content and action bar");
        }
        if (!layout.crumb().isEmpty()) {
            List<UiRect> crumb = List.of(layout.crumbBack(), layout.crumbText(),
                    layout.crumbPager());
            for (UiRect part : crumb) {
                assertTrue(layout.crumb().contains(part), label + ": crumb " + part);
            }
            noOverlap(label + " crumb", crumb);
        }
    }

    private static void noOverlap(String label, List<UiRect> rects) {
        for (int a = 0; a < rects.size(); a++) {
            for (int b = a + 1; b < rects.size(); b++) {
                assertFalse(rects.get(a).intersects(rects.get(b)),
                        label + ": " + rects.get(a) + " overlaps " + rects.get(b));
            }
        }
    }

    @Test
    void userResolutionUsesTheWideLayoutWithJoinKeyAndStatus() {
        FormationScreenLayout layout = FormationScreenLayout.compute(640, 336, 2, false, false,
                false, false, LIST_NEED, JOIN_WIDTH);

        assertEquals(Mode.WIDE, layout.mode());
        assertFalse(layout.joinKey().isEmpty(), "green join key in the strip");
        assertTrue(layout.status().width() >= 80, "room for the step text next to it");
        assertTrue(layout.listPanel().width() >= FormationScreenLayout.LIST_MIN);
        assertTrue(layout.detailPanel().width() > layout.listPanel().width());
        assertEquals(32, layout.identity().height());
    }

    @Test
    void roomyWindowsUseTheLargeEmblemAndTwoLineRows() {
        FormationScreenLayout layout = FormationScreenLayout.compute(960, 720, 2, true, true,
                false, false, LIST_NEED, 0);

        assertEquals(64, layout.identity().height());
        assertEquals(24, FormationScreenLayout.rowHeight(layout.shell().metrics()));
        assertFalse(layout.summary().isEmpty(), "a short list leaves room for the summary");
    }

    @Test
    void compactAdministratorAreaSitsAboveTheActionBarWithoutCoveringTheWell() {
        FormationScreenLayout layout = FormationScreenLayout.compute(320, 240, 2, true, true,
                false, false, LIST_NEED, 0);

        assertEquals(Mode.NARROW_LIST, layout.mode());
        assertEquals(FormationScreenLayout.adminHeight(layout.shell().metrics()),
                layout.admin().height());
        assertTrue(layout.admin().bottom() < layout.actionBar().top());
        assertTrue(layout.well().bottom() < layout.admin().top());
    }

    @Test
    void longListsScrollInsteadOfPushingPanelsAway() {
        FormationScreenLayout layout = FormationScreenLayout.compute(640, 336, 2, true, true,
                false, false, 2000, 0);

        assertTrue(layout.summary().isEmpty(), "no summary when the well needs all room");
        assertTrue(layout.well().bottom() < layout.admin().top());
    }

    @Test
    void manyFactionsShareTheStripWithoutOverlap() {
        FormationScreenLayout layout = FormationScreenLayout.compute(640, 336, 6, false, false,
                false, false, LIST_NEED, JOIN_WIDTH);

        assertEquals(6, layout.factionKeys().size());
        for (UiRect key : layout.factionKeys()) {
            assertTrue(layout.strip().contains(key));
            assertFalse(key.intersects(layout.joinKey()));
        }
    }
}
