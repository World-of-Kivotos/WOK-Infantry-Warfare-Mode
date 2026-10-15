package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.FormationContextView;
import com.wok.infantry.battle.PermissionView;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.screen.TacticalLivery.Livery;
import com.wok.infantry.client.screen.TacticalLivery.Scope;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DEVICE_PORT_PLAN 4.2: battle side first, then the chosen catalog faction, otherwise Neutral. */
class TacticalLiveryResolverTest {
    private static final PermissionView READ_ONLY = new PermissionView(
            false, false, false, false, false, false, false);

    @BeforeEach
    @AfterEach
    void clearClientState() {
        ClientBattleState.clear();
        ClientFormationState.clear();
        TacticalLivery.reset();
        TacticalLivery.pinForAcceptance(null);
    }

    // ---- the pure rule ----------------------------------------------------------------------------

    @Test
    void theBattleSideDecides() {
        assertEquals(Livery.ACADEMY, TacticalLivery.resolve(Faction.BLUE, "", Map.of()));
        assertEquals(Livery.CAESAR, TacticalLivery.resolve(Faction.RED, null, Map.of()));
        // A side always wins over a catalog id that names the other faction.
        assertEquals(Livery.ACADEMY, TacticalLivery.resolve(Faction.BLUE, "caesar", Map.of()));
    }

    @Test
    void withoutABattleTheDefaultCatalogIdsMapToTheirSides() {
        assertEquals(Livery.ACADEMY, TacticalLivery.resolve(null, "academy", Map.of()));
        assertEquals(Livery.CAESAR, TacticalLivery.resolve(null, "caesar", Map.of()));
        assertEquals(Livery.NEUTRAL, TacticalLivery.resolve(null, "gehenna", Map.of()),
                "an unknown faction id is neutral until a battle snapshot names its side");
        assertEquals(Livery.NEUTRAL, TacticalLivery.resolve(null, "", Map.of()));
        assertEquals(Livery.NEUTRAL, TacticalLivery.resolve(null, null, null));
    }

    @Test
    void learnedSidesBeatTheDefaults() {
        Map<String, Faction> learned = Map.of("gehenna", Faction.RED, "academy", Faction.RED);
        assertEquals(Livery.CAESAR, TacticalLivery.resolve(null, "gehenna", learned));
        assertEquals(Livery.CAESAR, TacticalLivery.resolve(null, "academy", learned),
                "an administrator may put the academy on the red side");
        assertEquals(Livery.CAESAR, TacticalLivery.resolve(null, "caesar", learned));
    }

    @Test
    void sidesMapToLiveries() {
        assertEquals(Livery.ACADEMY, TacticalLivery.forSide(Faction.BLUE));
        assertEquals(Livery.CAESAR, TacticalLivery.forSide(Faction.RED));
        assertEquals(Livery.NEUTRAL, TacticalLivery.forSide(null));
    }

    @Test
    void aBattleContextTeachesBothFactionIds() {
        Map<String, Faction> table = TacticalLivery.learn(Map.of(), Faction.RED,
                context("gehenna", "trinity"));
        assertEquals(Map.of("gehenna", Faction.RED, "trinity", Faction.BLUE), table);

        Map<String, Faction> same = TacticalLivery.learn(table, Faction.RED,
                context("gehenna", "trinity"));
        assertSame(table, same, "nothing new: the same table comes back");

        Map<String, Faction> swapped = TacticalLivery.learn(table, Faction.BLUE,
                context("gehenna", ""));
        assertEquals(Map.of("gehenna", Faction.BLUE, "trinity", Faction.BLUE), swapped,
                "empty ids are skipped, a new side replaces the old one");
        assertSame(table, TacticalLivery.learn(table, Faction.BLUE, FormationContextView.EMPTY));
        assertSame(table, TacticalLivery.learn(table, null, context("x", "y")));
    }

    // ---- live client state ------------------------------------------------------------------------

    @Test
    void noStateIsNeutral() {
        assertEquals(Livery.NEUTRAL, TacticalLivery.current());
    }

    @Test
    void theBattleSnapshotPaintsTheViewersSide() {
        ClientBattleState.update(battle(Faction.RED, context("caesar", "academy")));
        assertEquals(Livery.CAESAR, TacticalLivery.current());

        ClientBattleState.update(battle(Faction.BLUE, context("academy", "caesar")));
        assertEquals(Livery.ACADEMY, TacticalLivery.current());
    }

    @Test
    void theCatalogBridgesTheGapBeforeTheFirstBattleSnapshot() {
        ClientFormationState.update(catalog("caesar"));
        assertEquals(Livery.CAESAR, TacticalLivery.current(), "default id, no snapshot yet");

        ClientFormationState.update(catalog(""));
        assertEquals(Livery.NEUTRAL, TacticalLivery.current(), "not joined");
    }

    @Test
    void renamedFactionsAreRememberedUntilLogout() {
        ClientBattleState.update(battle(Faction.BLUE, context("gehenna", "trinity")));
        assertEquals(Livery.ACADEMY, TacticalLivery.current());
        assertEquals(Map.of("gehenna", Faction.BLUE, "trinity", Faction.RED),
                TacticalLivery.learnedSides());

        // The battle snapshot is gone (e.g. between rounds) but the catalog still names the faction.
        ClientBattleState.clear();
        ClientFormationState.update(catalog("trinity"));
        assertEquals(Livery.CAESAR, TacticalLivery.current());

        TacticalLivery.reset();
        assertTrue(TacticalLivery.learnedSides().isEmpty());
        assertEquals(Livery.NEUTRAL, TacticalLivery.current(), "unknown again after logout");
    }

    @Test
    void theAcceptancePinWinsOverEverything() {
        ClientBattleState.update(battle(Faction.RED, context("caesar", "academy")));
        TacticalLivery.pinForAcceptance(Livery.NEUTRAL);
        assertEquals(Livery.NEUTRAL, TacticalLivery.current());
        TacticalLivery.pinForAcceptance(null);
        assertEquals(Livery.CAESAR, TacticalLivery.current());
    }

    @Test
    void everyLiveryNamesItsPaletteAndSkin() {
        assertSame(TacticalPalette.ACADEMY, Livery.ACADEMY.palette(Scope.BOARD));
        assertSame(TacticalPalette.CAESAR, Livery.CAESAR.palette(Scope.BOARD));
        assertSame(TacticalPalette.NEUTRAL, Livery.NEUTRAL.palette(Scope.BOARD));
        assertEquals("CAESAR/MAP", Livery.CAESAR.palette(Scope.MAP).name());
        assertSame(DeviceSkin.CAESAR, Livery.CAESAR.skin());
    }

    private static FormationContextView context(String factionId, String enemyFactionId) {
        return new FormationContextView("", "", "", factionId, "", 40, enemyFactionId, "", 40);
    }

    private static BattleSnapshot battle(Faction side, FormationContextView context) {
        return new BattleSnapshot(new UUID(0L, 1L), side, null, false, false, 1, 0,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY, List.of(), List.of(),
                List.of(), READ_ONLY, List.of(), 1_000L, 1L)
                .withViewerContext(context, "", List.of());
    }

    private static FormationSelectionSnapshot catalog(String selectedFactionId) {
        return new FormationSelectionSnapshot(1L, false, selectedFactionId, "", List.of());
    }
}
