package com.wok.infantry.client.screen;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The page keys of the bottom bezel: {@link TacticalTabStrip.Skin#BEZEL} and its placement. */
class TacticalTabStripBezelTest {
    @Test
    void theBattleTerminalUsesBezelPageKeysWithTheSameIds() {
        TacticalTabStrip strip = BattleTab.strip(BattleTab.CLASSES, tab -> null, tab -> { });

        assertEquals(TacticalTabStrip.Skin.BEZEL, strip.skin());
        assertEquals(BattleTab.CLASSES.ordinal(), strip.current());
        assertEquals(List.of("squads", "classes", "deployment", "loadout", "map", "formation"),
                strip.tabs().stream().map(TacticalTabStrip.Tab::id).toList(),
                "uiTest clicks terminal.tabs/<id>");
    }

    @Test
    void bezelKeysComeAfterThePageInTabNavigation() {
        TacticalTabStrip bezel = BattleTab.strip(BattleTab.SQUADS, tab -> null, tab -> { });
        TacticalTabStrip header = new TacticalTabStrip(TacticalTabStrip.Skin.HEADER,
                BattleTab.tabs(null), 0, index -> { });

        assertEquals(TacticalBezelPlan.TAB_ORDER_PAGES, bezel.getTabOrderGroup(),
                "Tab reaches the bezel after every control of the page");
        assertEquals(0, header.getTabOrderGroup());
    }

    @Test
    void bezelLabelsCentreTheGlyphsLikeBoardKeys() {
        UiRect cell = new UiRect(0, 10, 40, 22);

        assertEquals(12, TacticalTabStrip.labelTextY(TacticalTabStrip.Skin.BEZEL, cell),
                "preview keyCap: t + floor((h - 8) / 2)");
        assertEquals(12, TacticalTabStrip.labelTextY(TacticalTabStrip.Skin.BOARD, cell));
        assertEquals(11, TacticalTabStrip.labelTextY(TacticalTabStrip.Skin.HEADER, cell),
                "header tabs sit on the title baseline");
        assertEquals(10, TacticalTabStrip.labelTextY(TacticalTabStrip.Skin.BEZEL,
                new UiRect(0, 10, 40, 16)), "never above the cell");
    }

    @Test
    void placeOnBezelTakesThePagesSlotAndKeepsThePlanWhileTheBoundsHold() {
        TacticalTabStrip strip = BattleTab.strip(BattleTab.SQUADS, tab -> null, tab -> { });
        TacticalBezelPlan plan = TacticalBezelPlan.plan(new UiRect(22, 332, 618, 355),
                TacticalShellLayout.Density.STANDARD, 52, 40);

        strip.placeOnBezel(plan);

        assertEquals(plan.pages(), UiRect.ofSize(strip.getX(), strip.getY(), strip.getWidth(),
                strip.getHeight()));
        assertTrue(strip.visible);
        assertSame(plan, strip.bezelPlan(true));

        // Moved somewhere else (the old header slot): the keys fill the new bounds, no LEDs, and
        // keep the size class of the bezel they came from.
        strip.setBounds(100, 8, 200, 14);
        TacticalBezelPlan moved = strip.bezelPlan(true);
        assertNotSame(plan, moved);
        assertEquals(new UiRect(100, 8, 300, 22), moved.pages());
        assertFalse(moved.hasLeds());
        assertEquals(TacticalShellLayout.Density.STANDARD, moved.density());
    }

    @Test
    void aStripNeverPlacedOnABezelFillsItsBoundsInTheGivenSizeClass() {
        TacticalTabStrip strip = BattleTab.strip(BattleTab.SQUADS, tab -> null, tab -> { });
        strip.setBounds(40, 4, 300, 12);

        TacticalBezelPlan compact = strip.bezelPlan(true);
        assertEquals(new UiRect(40, 4, 340, 16), compact.pages());
        assertEquals(TacticalShellLayout.Density.COMPACT, compact.density());
        assertEquals(28, compact.minKeyWidth());
        assertEquals(TacticalShellLayout.Density.STANDARD, strip.bezelPlan(false).density());
    }

    @Test
    void placeBezelPositionsTheStripFromTheShellLayout() {
        for (int[] size : new int[][]{{320, 240}, {480, 360}, {640, 336}, {960, 540}}) {
            TacticalShellLayout layout = TacticalShellLayout.compute(size[0], size[1]);
            TacticalTabStrip strip = BattleTab.strip(BattleTab.SQUADS, tab -> null, tab -> { });

            TacticalBezelPlan plan = TacticalBoardChrome.placeBezel(null, layout, strip, List.of());

            assertEquals(TacticalBezelPlan.plan(layout.bezel(), layout.density(), 0, 0), plan);
            assertEquals(plan.pages(), UiRect.ofSize(strip.getX(), strip.getY(),
                    strip.getWidth(), strip.getHeight()), size[0] + "x" + size[1]);
            assertTrue(layout.bezel().contains(plan.pages()), size[0] + "x" + size[1]);
            assertSame(plan, strip.bezelPlan(layout.tight()));
        }
        TacticalShellLayout layout = TacticalShellLayout.compute(640, 360);
        assertEquals(TacticalBezelPlan.plan(layout.bezel(), layout.density(), 0, 0),
                TacticalBoardChrome.placeBezel(null, layout, null, null), "no strip: plan only");
    }

    @Test
    void escAndRefreshShrinkToTheirKeyNamesBeforeThePageKeysPage() {
        // en_us at 320×240: "Esc Back" 54px and "R Refresh" 59px leave 177px, six "Role"-wide
        // keys need 184px (real-client run, OPEN_COMPACT_CLASSES).
        UiRect bezel = TacticalShellLayout.compute(320, 240).bezel();
        TacticalShellLayout.Density compact = TacticalShellLayout.Density.COMPACT;

        TacticalBezelPlan full = TacticalBezelPlan.planFitting(bezel, compact, 54, 26, 59, 14,
                plan -> plan.pages().width() >= 100);
        assertEquals(54, full.esc().width(), "the full caps stay while the page keys fit");
        assertEquals(59, full.refresh().width());
        assertEquals(177, full.pages().width());

        TacticalBezelPlan keyed = TacticalBezelPlan.planFitting(bezel, compact, 54, 26, 59, 14,
                plan -> plan.pages().width() >= 184);
        assertEquals(28, keyed.esc().width(), "key name only, as wide as a page key");
        assertEquals(28, keyed.refresh().width());
        assertTrue(keyed.pages().width() >= 184);
        assertEquals(keyed.bezel().right() - TacticalBezelPlan.inset(compact),
                keyed.refresh().right(), "R stays at the right end");

        TacticalBezelPlan bare = TacticalBezelPlan.planFitting(bezel, compact, 54, 26, 59, 14,
                plan -> plan.pages().width() >= 250);
        assertEquals(26, bare.esc().width(), "then just the key name and its padding");
        assertEquals(14, bare.refresh().width());

        TacticalBezelPlan hopeless = TacticalBezelPlan.planFitting(bezel, compact, 54, 26, 59,
                14, plan -> false);
        assertEquals(full, hopeless, "the pager keeps the full caps");
    }

    @Test
    void keyOnlyCapsAreNeverWiderThanTheFullCap() {
        assertArrayEquals(new int[]{28, 26}, TacticalBezelPlan.keyOnlyWidths(
                TacticalShellLayout.Density.COMPACT, 54, 26));
        assertArrayEquals(new int[]{30, 18}, TacticalBezelPlan.keyOnlyWidths(
                TacticalShellLayout.Density.STANDARD, 30, 18));
        assertArrayEquals(new int[]{0, 0}, TacticalBezelPlan.keyOnlyWidths(
                TacticalShellLayout.Density.COMPACT, 0, 14), "a missing key stays missing");
    }

    @Test
    void withoutAFontOrABezelStripTheKeysKeepTheirFullCaps() {
        TacticalShellLayout layout = TacticalShellLayout.compute(320, 240);
        TacticalTabStrip strip = BattleTab.strip(BattleTab.SQUADS, tab -> null, tab -> { });

        assertEquals(TacticalBezelPlan.plan(null, layout, TacticalBoardChrome.KeyHint.back(),
                null), TacticalBezelPlan.plan(null, layout, TacticalBoardChrome.KeyHint.back(),
                null, strip));
    }

    @Test
    void aBezelWithoutRoomHidesTheStrip() {
        TacticalTabStrip strip = BattleTab.strip(BattleTab.SQUADS, tab -> null, tab -> { });
        TacticalBezelPlan crowded = TacticalBezelPlan.plan(new UiRect(0, 200, 120, 218),
                TacticalShellLayout.Density.COMPACT, 60, 50);

        strip.placeOnBezel(crowded);

        assertTrue(crowded.pages().isEmpty());
        assertFalse(strip.visible);
    }

    @Test
    void keyboardBehaviourIsUnchangedOnTheBezel() {
        List<Integer> selected = new ArrayList<>();
        TacticalTabStrip strip = BattleTab.strip(BattleTab.SQUADS, tab -> tab == BattleTab.CLASSES
                ? Component.literal("编制锁定后开放") : null, tab -> selected.add(tab.ordinal()));

        assertTrue(strip.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        assertTrue(strip.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0), "nothing left of the first tab");
        assertTrue(strip.cycle(-1));
        assertEquals(List.of(BattleTab.DEPLOYMENT.ordinal(), BattleTab.FORMATION.ordinal()),
                selected, "←/→ and Ctrl+Tab skip the disabled tab");
        assertFalse(strip.select(BattleTab.SQUADS.ordinal()), "the pressed key is not clickable");
    }

    @Test
    void pagerSplitsTheKeyRow() {
        List<UiRect> parts = TacticalTabStrip.pagerParts(new UiRect(100, 10, 300, 22));

        assertEquals(new UiRect(100, 10, 116, 22), parts.get(0));
        assertEquals(new UiRect(118, 10, 282, 22), parts.get(1));
        assertEquals(new UiRect(284, 10, 300, 22), parts.get(2));
        List<UiRect> narrow = TacticalTabStrip.pagerParts(new UiRect(0, 0, 40, 12));
        assertEquals(8, narrow.get(0).width(), "a fifth of a narrow strip");
    }

    @Test
    void badgeColourIsResolvedWhenDrawn() {
        TacticalTabStrip.Tab tab = TacticalTabStrip.Tab.of("squads", Component.literal("小队"));

        assertEquals(TacticalTabStrip.BADGE_MUTED, tab.badgeColor(),
                "no colour is taken at construction");
        assertEquals(TacticalBoardTheme.MUTED, tab.resolvedBadgeColor());
        assertEquals(TacticalTabStrip.BADGE_MUTED,
                tab.withBadge(Component.literal("3/4"), TacticalTabStrip.BADGE_MUTED).badgeColor());
        assertEquals(0xFF123456,
                tab.withBadge(Component.literal("3/4"), 0xFF123456).resolvedBadgeColor());
    }
}
