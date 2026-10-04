package com.wok.infantry.client.screen;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalTabStripTest {
    @Test
    void ctrlTabCyclesAndPlainTabStaysWithVanillaFocusNavigation() {
        assertEquals(1, TacticalTabStrip.navigationDelta(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_CONTROL));
        assertEquals(-1, TacticalTabStrip.navigationDelta(GLFW.GLFW_KEY_TAB,
                GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT));
        assertEquals(0, TacticalTabStrip.navigationDelta(GLFW.GLFW_KEY_TAB, 0), "Tab = focus");
        assertEquals(0, TacticalTabStrip.navigationDelta(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_SHIFT),
                "Shift+Tab = focus back");
        assertEquals(0, TacticalTabStrip.navigationDelta(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_SUPER),
                "Cmd+Tab belongs to macOS");
        assertEquals(0, TacticalTabStrip.navigationDelta(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_ALT));
        assertEquals(0, TacticalTabStrip.navigationDelta(GLFW.GLFW_KEY_Q, GLFW.GLFW_MOD_CONTROL));
        assertEquals(1, TacticalTabStrip.navigationDelta(GLFW.GLFW_KEY_TAB,
                GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_NUM_LOCK), "lock keys do not matter");
    }

    @Test
    void nextEnabledSkipsDisabledTabsAndWraps() {
        boolean[] enabled = {true, false, true, true, false};

        assertEquals(2, TacticalTabStrip.nextEnabled(enabled, 0, 1, true));
        assertEquals(0, TacticalTabStrip.nextEnabled(enabled, 3, 1, true), "wraps past the end");
        assertEquals(3, TacticalTabStrip.nextEnabled(enabled, 0, -1, true), "wraps before 0");
        assertEquals(-1, TacticalTabStrip.nextEnabled(enabled, 3, 1, false), "pager stops at end");
        assertEquals(-1, TacticalTabStrip.nextEnabled(new boolean[]{true, false}, 0, 1, true),
                "no other enabled tab");
        assertEquals(-1, TacticalTabStrip.nextEnabled(new boolean[0], 0, 1, true));
    }

    @Test
    void modeFallsBackFromFullToShortToPager() {
        assertEquals(TacticalTabStrip.Mode.FULL, TacticalTabStrip.chooseMode(200, 200, 120));
        assertEquals(TacticalTabStrip.Mode.SHORT, TacticalTabStrip.chooseMode(199, 200, 120));
        assertEquals(TacticalTabStrip.Mode.PAGER, TacticalTabStrip.chooseMode(119, 200, 120));
    }

    @Test
    void cycleSelectsTheNextEnabledTabAndNeverTheCurrentOne() {
        List<Integer> selected = new ArrayList<>();
        List<TacticalTabStrip.Tab> tabs = BattleTab.tabs(tab -> tab == BattleTab.CLASSES
                ? Component.literal("编制锁定后开放") : null);
        TacticalTabStrip strip = new TacticalTabStrip(TacticalTabStrip.Skin.HEADER, tabs,
                BattleTab.SQUADS.ordinal(), selected::add);

        assertTrue(strip.cycle(1));
        assertEquals(List.of(BattleTab.DEPLOYMENT.ordinal()), selected, "CLASSES is disabled");
        assertTrue(strip.cycle(-1));
        assertEquals(BattleTab.FORMATION.ordinal(), selected.get(1), "wraps to the last tab");
        assertFalse(strip.select(BattleTab.SQUADS.ordinal()), "current tab is not clickable");
        assertFalse(strip.select(BattleTab.CLASSES.ordinal()), "disabled tab is not clickable");
        assertEquals(2, selected.size());
        assertEquals(BattleTab.SQUADS.ordinal(), strip.current(),
                "selecting only reports; the screen switches pages");
    }

    @Test
    void battleTabsKeepThePreviewOrderAndIds() {
        assertEquals(List.of("squads", "classes", "deployment", "loadout", "map", "formation"),
                java.util.Arrays.stream(BattleTab.values()).map(BattleTab::id).toList());
        assertEquals(BattleTab.MAP, BattleTab.byId("map"));
        assertNull(BattleTab.byId("unknown"));
        TacticalTabStrip strip = BattleTab.strip(BattleTab.MAP, tab -> null, tab -> { });
        assertEquals(BattleTab.MAP.ordinal(), strip.current());
        assertEquals(4, strip.indexOf("map"));
        assertTrue(strip.tabs().stream().allMatch(TacticalTabStrip.Tab::enabled));
    }
}
