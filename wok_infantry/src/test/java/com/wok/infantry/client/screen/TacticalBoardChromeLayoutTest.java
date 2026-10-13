package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalBoardChrome.FooterPlan;
import com.wok.infantry.client.screen.TacticalBoardChrome.HeaderPlan;
import com.wok.infantry.client.screen.TacticalBoardChrome.HintSlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalBoardChromeLayoutTest {
    private static HeaderPlan plan(int width, int height, int title, int tabsFull, int tabsShort,
                                   int identity) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        return TacticalBoardChrome.planHeader(layout.header(), layout.metrics(), title, tabsFull,
                tabsShort, identity);
    }

    @Test
    void everythingFitsOnAWideHeader() {
        HeaderPlan plan = plan(960, 540, 90, 300, 180, 120);

        assertTrue(plan.identityShown());
        assertFalse(plan.shortTabs());
        assertEquals(90, plan.title().width());
        assertEquals(300, plan.tabs().width());
        assertFalse(plan.tabs().intersects(plan.identity()));
        assertFalse(plan.title().intersects(plan.tabs()));
    }

    @Test
    void identityCollapsesBeforeTheTabsShorten() {
        // 640 wide: available = 628 - 14 = 614; title 90 + full tabs 360 + 10 fits without identity,
        // but not together with a 160px identity.
        HeaderPlan plan = plan(640, 360, 90, 360, 200, 160);

        assertFalse(plan.identityShown());
        assertFalse(plan.shortTabs(), "full tabs still fit once the identity is gone");
        assertEquals(360, plan.tabs().width());
        assertEquals(UiRect.EMPTY, plan.identity());
    }

    @Test
    void tabsShortenBeforeTheTitleIsCut() {
        HeaderPlan plan = plan(320, 240, 70, 300, 150, 80);

        assertFalse(plan.identityShown());
        assertTrue(plan.shortTabs());
        assertEquals(70, plan.title().width(), "title stays whole while short tabs fit");
        assertEquals(150, plan.tabs().width());
    }

    @Test
    void titleIsCutLastAndTheStripGetsTheRemainderForItsPager() {
        HeaderPlan plan = plan(320, 240, 120, 400, 260, 0);
        UiRect header = TacticalShellLayout.compute(320, 240).header();

        assertTrue(plan.shortTabs());
        assertTrue(plan.title().width() < 120 && plan.title().width() >= 24);
        assertTrue(plan.tabs().right() <= header.right() - 6, "strip stays inside the header");
        assertTrue(plan.tabs().width() < 260, "strip must page inside the slot it got");
    }

    @Test
    void tabSlotDoesNotDependOnTheIdentityText() {
        HeaderPlan without = plan(640, 360, 90, 300, 180, 0);
        for (int identity : new int[]{40, 120, 190, 400}) {
            assertEquals(without.tabs(), plan(640, 360, 90, 300, 180, identity).tabs(),
                    "identity width " + identity);
        }
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "480, 270", "640, 360", "640, 336", "960, 540", "480, 360"})
    void headerPartsStayInsideTheHeaderStrip(int width, int height) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        HeaderPlan plan = plan(width, height, 96, 260, 150, 140);
        UiRect header = layout.header();

        assertTrue(header.contains(plan.led()));
        assertTrue(header.contains(plan.title()));
        assertTrue(header.contains(plan.tabs()));
        if (plan.identityShown()) {
            assertTrue(header.contains(plan.identity()));
            assertFalse(plan.identity().intersects(plan.tabs()));
        }
        // Tabs end above the 2px orange/dark bottom line of the header.
        assertTrue(plan.tabs().bottom() <= header.bottom() - 2);
        assertTrue(plan.textY() + 8 <= header.bottom() - 2);
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "640, 336", "480, 360", "960, 540"})
    void headerTabLabelsShareTheTitleBaseline(int width, int height) {
        HeaderPlan plan = plan(width, height, 96, 200, 150, 0);

        // The preview draws tab labels on the title's baseline, above the 2px bottom rule.
        assertEquals(plan.textY(),
                TacticalTabStrip.labelTextY(TacticalTabStrip.Skin.HEADER, plan.tabs()));
        UiRect key = UiRect.ofSize(0, 0, 60, 18);
        assertEquals(5, TacticalTabStrip.labelTextY(TacticalTabStrip.Skin.BOARD, key),
                "board keys centre the glyphs like TacticalButtonStyle");
    }

    @Test
    void footerDropsTrailingHintsAndCapsTheReceipt() {
        UiRect footer = TacticalShellLayout.compute(320, 240).footer();

        FooterPlan plan = TacticalBoardChrome.planFooter(footer, true,
                new int[]{16, 20, 6, 6}, new int[]{20, 20, 60, 60}, 400);

        // Receipt capped at 62% of 314 = 194px.
        assertEquals(194 + 1, plan.feedback().width());
        assertEquals(footer.right() - 1, plan.feedback().right());
        int hintRight = footer.right() - 194 - 6;
        assertEquals(2, plan.hints().size(), "the third hint would cross into the receipt");
        for (HintSlot slot : plan.hints()) {
            assertTrue(footer.contains(slot.keycap()));
            assertTrue(slot.keycap().right() <= hintRight);
            assertFalse(slot.keycap().intersects(plan.feedback()));
        }
        assertEquals(footer.left() + 4, plan.hints().get(0).keycap().left());
        assertEquals(plan.hints().get(0).keycap().right() + 3, plan.hints().get(0).labelX());
    }

    @Test
    void footerWithoutReceiptUsesTheWholeWidthAndStandardCapIsHalf() {
        UiRect footer = TacticalShellLayout.compute(640, 360).footer();

        FooterPlan none = TacticalBoardChrome.planFooter(footer, false,
                new int[]{16, 40}, new int[]{20, 20}, -1);
        FooterPlan receipt = TacticalBoardChrome.planFooter(footer, false,
                new int[0], new int[0], 1000);

        assertEquals(UiRect.EMPTY, none.feedback());
        assertEquals(2, none.hints().size());
        assertEquals(footer.width() / 2 + 1, receipt.feedback().width());
        assertTrue(footer.contains(receipt.feedback()));
    }

    @Test
    void shortReceiptHugsItsText() {
        UiRect footer = TacticalShellLayout.compute(960, 540).footer();

        FooterPlan plan = TacticalBoardChrome.planFooter(footer, false, new int[0], new int[0], 40);

        assertEquals(40 + 16 + 1, plan.feedback().width());
    }
}
