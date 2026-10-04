package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BattleTerminalNavTest {
    /** Stand-in for a screen: terminal screens remember the root they return to. */
    private record FakeScreen(String name, FakeScreen root, boolean terminal) {
        static FakeScreen other(String name) {
            return new FakeScreen(name, null, false);
        }

        static FakeScreen terminal(String name, FakeScreen root) {
            return new FakeScreen(name, root, true);
        }
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
