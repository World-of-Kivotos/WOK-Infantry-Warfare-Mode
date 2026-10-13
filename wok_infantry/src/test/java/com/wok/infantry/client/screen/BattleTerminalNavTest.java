package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BattleTerminalNavTest {
    /**
     * Stand-in for a screen: terminal screens remember the root they return to; legacy pages
     * (the squad page before 档 3) only remember the screen they were opened over.
     */
    private record FakeScreen(String name, FakeScreen root, boolean terminal, boolean legacy) {
        static FakeScreen other(String name) {
            return new FakeScreen(name, null, false, false);
        }

        static FakeScreen terminal(String name, FakeScreen root) {
            return new FakeScreen(name, root, true, false);
        }

        static FakeScreen legacy(String name, FakeScreen parent) {
            return new FakeScreen(name, parent, false, true);
        }
    }

    private FakeScreen rootSkippingLegacy(FakeScreen start) {
        return model.rootSkipping(start, FakeScreen::legacy, FakeScreen::root,
                FakeScreen::terminal, FakeScreen::root);
    }

    private final TerminalNavModel<FakeScreen> model = new TerminalNavModel<>();

    private FakeScreen rootFor(FakeScreen current) {
        return model.returnScreenFor(current, FakeScreen::terminal, FakeScreen::root);
    }

    /** What a tab switch does: the target is built for the session root, never for {@code from}. */
    private FakeScreen switchTo(FakeScreen from, String name) {
        return FakeScreen.terminal(name, rootFor(from));
    }

    @Test
    void tabSwitchesReplaceInsteadOfStacking() {
        FakeScreen squads = FakeScreen.terminal("squads", null);
        FakeScreen map = switchTo(squads, "map");
        FakeScreen loadout = switchTo(map, "loadout");
        FakeScreen formation = switchTo(loadout, "formation");

        assertNull(map.root(), "map returns to the game, not to the squad screen");
        assertNull(loadout.root());
        assertNull(formation.root());
        assertNull(rootFor(formation), "one Esc closes the whole terminal");
    }

    @Test
    void rootIsTheScreenOpenBeforeTheTerminal() {
        FakeScreen inventory = FakeScreen.other("inventory");
        FakeScreen squads = FakeScreen.terminal("squads", rootFor(inventory));
        FakeScreen map = switchTo(squads, "map");

        assertSame(inventory, squads.root());
        assertSame(inventory, map.root());
        assertSame(inventory, rootFor(map));
    }

    /** B11a: the squad page's "编制" key must not make the squad page the vote page's parent. */
    @Test
    void legacySquadPagesAreSkippedWhenEnteringTheVotePage() {
        FakeScreen inventory = FakeScreen.other("inventory");
        FakeScreen squads = FakeScreen.legacy("squads", inventory);
        FakeScreen stacked = FakeScreen.legacy("squads-again", squads);

        assertSame(inventory, rootSkippingLegacy(squads.root()));
        assertSame(inventory, rootSkippingLegacy(stacked.root()),
                "squad pages stacked on each other are all skipped");
        FakeScreen formation = FakeScreen.terminal("formation", rootSkippingLegacy(squads.root()));
        FakeScreen backToSquads = switchTo(formation, "squads");
        assertSame(inventory, backToSquads.root(), "switching tabs keeps the session root");
        assertNull(rootSkippingLegacy(FakeScreen.legacy("squads", null).root()),
                "a squad page opened in game leads back to the game");
        FakeScreen oldFormation = FakeScreen.terminal("formation-old", squads);
        assertSame(inventory, rootSkippingLegacy(oldFormation),
                "a terminal screen whose root is a squad page is walked up as well");
    }

    @Test
    void mapInstanceIsReusedWithinTheSessionOnly() {
        AtomicInteger created = new AtomicInteger();
        FakeScreen first = model.reuse(BattleTab.MAP, null,
                () -> FakeScreen.terminal("map-" + created.incrementAndGet(), null));
        FakeScreen again = model.reuse(BattleTab.MAP, null,
                () -> FakeScreen.terminal("map-" + created.incrementAndGet(), null));

        assertSame(first, again, "coming back to the map keeps its view");
        assertEquals(1, created.get());

        FakeScreen inventory = FakeScreen.other("inventory");
        FakeScreen otherSession = model.reuse(BattleTab.MAP, inventory,
                () -> FakeScreen.terminal("map-" + created.incrementAndGet(), inventory));
        assertNotSame(first, otherSession, "a map built for another root would Esc to the wrong place");
        assertSame(inventory, otherSession.root());
        assertSame(otherSession, model.cached(BattleTab.MAP));
    }

    @Test
    void clearDropsEveryReusableInstance() {
        FakeScreen first = model.reuse(BattleTab.MAP, null, () -> FakeScreen.terminal("a", null));
        model.clear();
        FakeScreen second = model.reuse(BattleTab.MAP, null, () -> FakeScreen.terminal("b", null));

        assertNotSame(first, second);
        assertNull(model.cached(BattleTab.SQUADS));
    }
}
