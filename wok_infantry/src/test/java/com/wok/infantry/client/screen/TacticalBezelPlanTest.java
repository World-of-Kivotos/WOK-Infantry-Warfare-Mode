package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalBoardChrome.KeyHint;
import com.wok.infantry.client.screen.TacticalShellLayout.Density;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bottom bezel layout against the preview's {@code pageKeys} ({@code 17-device.js}, D2). The
 * expected rectangles are worked out by hand from the preview's {@code geom(w, h, 2)}.
 */
class TacticalBezelPlanTest {
    /** 640×360 (standard): D = (12, 6, 628, 355), S = (22, 16, 618, 331). */
    private static final UiRect STANDARD_BEZEL = new UiRect(22, 332, 618, 355);
    /** 320×240 (tight): D = (2, 2, 318, 238), S = (7, 8, 313, 220). */
    private static final UiRect TIGHT_BEZEL = new UiRect(7, 221, 313, 238);
    /** 960×540 (roomy): D = (28, 12, 932, 530), S = (43, 26, 917, 500). */
    private static final UiRect ROOMY_BEZEL = new UiRect(43, 501, 917, 530);

    @Test
    void standardBezelMatchesThePreview() {
        // "Esc 返回" and "R 刷新" with 18px names and 18px actions.
        int esc = TacticalBezelPlan.pairWidth(Density.STANDARD, 18, 18);
        int refresh = TacticalBezelPlan.pairWidth(Density.STANDARD, 6, 18);
        assertEquals(52, esc);
        assertEquals(40, refresh);
        TacticalBezelPlan plan = TacticalBezelPlan.plan(STANDARD_BEZEL, Density.STANDARD, esc,
                refresh);

        assertEquals(new UiRect(34, 339, 86, 352), plan.esc(), "12px in, 3px above the case edge");
        assertEquals(new UiRect(566, 339, 606, 352), plan.refresh());
        assertEquals(335, plan.ledTop());
        assertEquals(2, plan.ledHeight());
        assertEquals(new UiRect(98, 334, 554, 352), plan.pages(),
                "12px from Esc and R, from the LED glow down to the key bottom");

        // Six pages, widest full label 36px ("战术地图"): 48px keys, 4px gaps, centred under S.
        int keyWidth = plan.keyWidth(36);
        assertEquals(48, keyWidth);
        assertEquals(308, plan.rowWidth(6, keyWidth));
        List<UiRect> keys = plan.keys(6, keyWidth);
        assertEquals(new UiRect(166, 339, 214, 352), keys.get(0));
        assertEquals(new UiRect(218, 339, 266, 352), keys.get(1));
        assertEquals(new UiRect(426, 339, 474, 352), keys.get(5));
        assertEquals(new UiRect(185, 335, 195, 337), plan.led(keys.get(0)), "10px LED, centred");
        assertEquals(new UiRect(184, 334, 196, 338), plan.ledGlow(keys.get(0)));
        assertTrue(plan.pages().contains(plan.ledGlow(keys.get(0))), "the glow stays in the strip");
    }

    @Test
    void tightBezelMatchesThePreview() {
        int esc = TacticalBezelPlan.pairWidth(Density.COMPACT, 18, 18);
        int refresh = TacticalBezelPlan.pairWidth(Density.COMPACT, 6, 18);
        assertEquals(48, esc);
        TacticalBezelPlan plan = TacticalBezelPlan.plan(TIGHT_BEZEL, Density.COMPACT, esc,
                refresh);

        assertEquals(new UiRect(10, 225, 58, 237), plan.esc(), "3px in, 1px above the case edge");
        assertEquals(new UiRect(274, 225, 310, 237), plan.refresh());
        assertEquals(new UiRect(63, 222, 269, 237), plan.pages(), "5px from Esc and R");
        assertEquals(223, plan.ledTop());
        assertEquals(1, plan.ledHeight());
        assertEquals(12, plan.keyBottom() - plan.keyTop());

        // Short labels 18px wide: the 28px minimum wins; 2px gaps.
        int keyWidth = plan.keyWidth(18);
        assertEquals(28, keyWidth);
        List<UiRect> keys = plan.keys(6, keyWidth);
        assertEquals(new UiRect(71, 225, 99, 237), keys.get(0));
        assertEquals(new UiRect(221, 225, 249, 237), keys.get(5));
        assertEquals(new UiRect(82, 223, 88, 224), plan.led(keys.get(0)), "6px LED in tight");
    }

    @Test
    void roomyBezelUsesTheWiderPadding() {
        assertEquals(58, TacticalBezelPlan.pairWidth(Density.ROOMY, 18, 18));
        TacticalBezelPlan plan = TacticalBezelPlan.plan(ROOMY_BEZEL, Density.ROOMY, 58, 46);

        assertEquals(new UiRect(55, 508, 113, 527), plan.esc());
        assertEquals(new UiRect(859, 508, 905, 527), plan.refresh());
        assertEquals(18, plan.pad());
        assertEquals(38, plan.minKeyWidth());
        assertEquals(54, plan.keyWidth(36));
        assertEquals(4, plan.gap());
        assertEquals(12, plan.separation());
        assertEquals(5, plan.ledHalfWidth());
    }

    @Test
    void keysAreCentredUnderTheScreenAndPushedOutOfEscAndRefresh() {
        // A wide Esc key: the centred row would start inside the Esc separation.
        TacticalBezelPlan wideEsc = TacticalBezelPlan.plan(STANDARD_BEZEL, Density.STANDARD, 200,
                40);
        List<UiRect> keys = wideEsc.keys(6, 48);
        assertEquals(wideEsc.pages().left(), keys.get(0).left(), "pushed right of Esc");
        assertEquals(246, keys.get(0).left());

        // A wide R key: the row ends at the R separation.
        TacticalBezelPlan wideRefresh = TacticalBezelPlan.plan(STANDARD_BEZEL, Density.STANDARD,
                52, 150);
        List<UiRect> right = wideRefresh.keys(6, 48);
        assertEquals(wideRefresh.pages().right(), right.get(5).right(), "pushed left of R");
        assertEquals(136, right.get(0).left());

        // Without Esc and R the row is centred on the bezel.
        TacticalBezelPlan none = TacticalBezelPlan.plan(STANDARD_BEZEL, Density.STANDARD, 0, 0);
        assertTrue(none.esc().isEmpty());
        assertTrue(none.refresh().isEmpty());
        List<UiRect> centred = none.keys(3, 40);
        int middle = (STANDARD_BEZEL.left() + STANDARD_BEZEL.right()) / 2;
        assertEquals(middle, (centred.get(0).left() + centred.get(2).right()) / 2);
        assertTrue(none.keys(0, 40).isEmpty());
    }

    @Test
    void fullNamesThenShortNamesThenThePager() {
        TacticalBezelPlan plan = TacticalBezelPlan.plan(TIGHT_BEZEL, Density.COMPACT, 48, 36);
        int available = plan.pages().width();
        int full = plan.rowWidth(6, plan.keyWidth(36));
        int shortNames = plan.rowWidth(6, plan.keyWidth(18));
        assertEquals(206, available);
        assertEquals(274, full);
        assertEquals(178, shortNames);
        assertEquals(TacticalTabStrip.Mode.SHORT,
                TacticalTabStrip.chooseMode(available, full, shortNames),
                "320×240 shows the short page names (preview: \"sh\")");
        assertEquals(TacticalTabStrip.Mode.FULL, TacticalTabStrip.chooseMode(available,
                plan.rowWidth(4, plan.keyWidth(18)), shortNames));
        assertEquals(TacticalTabStrip.Mode.PAGER,
                TacticalTabStrip.chooseMode(170, full, shortNames), "the widget keeps its pager");
    }

    @Test
    void minimumKeyWidthPerSizeClass() {
        TacticalBezelPlan tight = TacticalBezelPlan.plan(TIGHT_BEZEL, Density.COMPACT, 0, 0);
        TacticalBezelPlan standard = TacticalBezelPlan.plan(STANDARD_BEZEL, Density.STANDARD, 0,
                0);
        assertEquals(28, tight.keyWidth(5));
        assertEquals(38, standard.keyWidth(5));
        assertEquals(36 + 8, tight.keyWidth(36));
        assertEquals(36 + 12, standard.keyWidth(36));
    }

    @Test
    void aLowBezelDropsTheLedsAndKeepsAReadableKey() {
        // The full-screen frame's 14px footer (before the device geometry).
        UiRect footer = new UiRect(6, 340, 634, 354);
        TacticalBezelPlan plan = TacticalBezelPlan.plan(footer, Density.STANDARD, 52, 40);

        assertFalse(plan.hasLeds());
        assertEquals(341, plan.keyTop());
        assertEquals(351, plan.keyBottom());
        assertEquals(plan.keyTop(), plan.pages().top(), "no glow row above the keys");
        assertTrue(plan.keyBottom() - plan.keyTop() >= TacticalBezelPlan.MIN_KEY_HEIGHT);
        assertTrue(plan.led(plan.keys(1, 38).get(0)).isEmpty());
        assertTrue(plan.ledGlow(plan.keys(1, 38).get(0)).isEmpty());
    }

    @Test
    void everyRegionStaysInsideTheBezel() {
        for (Density density : Density.values()) {
            UiRect bezel = density == Density.COMPACT ? TIGHT_BEZEL
                    : density == Density.ROOMY ? ROOMY_BEZEL : STANDARD_BEZEL;
            TacticalBezelPlan plan = TacticalBezelPlan.plan(bezel, density,
                    TacticalBezelPlan.pairWidth(density, 18, 18),
                    TacticalBezelPlan.pairWidth(density, 6, 18));
            assertTrue(bezel.contains(plan.esc()), density + " esc");
            assertTrue(bezel.contains(plan.refresh()), density + " refresh");
            assertTrue(bezel.contains(plan.pages()), density + " pages");
            assertFalse(plan.esc().intersects(plan.pages()), density + " esc / pages");
            assertFalse(plan.refresh().intersects(plan.pages()), density + " refresh / pages");
            for (UiRect key : plan.keys(6, plan.keyWidth(18))) {
                assertTrue(plan.pages().contains(key), density + " key " + key);
                assertTrue(plan.pages().contains(plan.ledGlow(key)), density + " glow " + key);
            }
        }
    }

    @Test
    void inBoundsFillsTheBoundsWithoutLeds() {
        UiRect bounds = new UiRect(100, 10, 300, 26);
        TacticalBezelPlan plan = TacticalBezelPlan.inBounds(bounds, Density.STANDARD);

        assertEquals(bounds, plan.pages());
        assertFalse(plan.hasLeds());
        assertEquals(10, plan.keyTop());
        assertEquals(26, plan.keyBottom());
        List<UiRect> keys = plan.keys(2, 40);
        assertEquals(new UiRect(158, 10, 198, 26), keys.get(0), "centred on the bounds");
        assertEquals(new UiRect(100, 10, 300, 26), plan.keyRow());
    }

    @Test
    void theEscHintGoesLeftAndAnyOtherHintRight() {
        assertTrue(TacticalBezelPlan.isEscape(KeyHint.close()));
        assertTrue(TacticalBezelPlan.isEscape(KeyHint.back()));
        assertTrue(TacticalBezelPlan.isEscape(KeyHint.literal("esc", Component.literal("返回"))));
        assertFalse(TacticalBezelPlan.isEscape(KeyHint.literal("R", Component.literal("刷新"))));
        assertFalse(TacticalBezelPlan.isEscape(null));
    }

    @Test
    void tabOrderPutsTheBezelAfterThePageFromLeftToRight() {
        assertTrue(TacticalBezelPlan.TAB_ORDER_ESC > 0, "after the page's controls (group 0)");
        assertTrue(TacticalBezelPlan.TAB_ORDER_ESC < TacticalBezelPlan.TAB_ORDER_PAGES);
        assertTrue(TacticalBezelPlan.TAB_ORDER_PAGES < TacticalBezelPlan.TAB_ORDER_REFRESH);
    }
}
