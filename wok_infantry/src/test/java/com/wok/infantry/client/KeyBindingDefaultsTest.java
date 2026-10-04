package com.wok.infantry.client;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.PermissionView;
import com.wok.infantry.client.KeyBindingDefaults.Binding;
import com.wok.infantry.client.KeyBindingDefaults.LabelSource;
import com.wok.infantry.client.KeyBindingDefaults.MapRoute;
import com.wok.infantry.client.KeyBindingDefaults.TerminalRoute;
import com.wok.infantry.config.InfantryClientConfig;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyBindingDefaultsTest {
    /**
     * Keys the test modpack (D:\WOK步战测试\1.20.1-Forge_47.4.22, inventory text-state §1) and
     * vanilla already use: B C G H I J N R V X Y Z from the pack, K (SuperbWarfare thermal
     * imaging, TaCZ Expands), L (advancements, Quest Log), M (Xaero's World Map), O (TaCZ
     * interact, JourneyMap options), U (JEI uses, Xaero waypoints), P (social interactions) and
     * the vanilla movement / hotbar / function keys.
     */
    private static final Set<Integer> TAKEN_BY_TEST_PACK = Set.of(
            GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H,
            GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_R,
            GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_Z,
            GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_O,
            GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_P,
            GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D,
            GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_T,
            GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_LEFT_SHIFT,
            GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2,
            GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F11,
            GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4,
            GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8,
            GLFW.GLFW_KEY_9);

    @Test
    void terminalUsesTheGraveAccentAndNothingDefaultsToP() {
        assertEquals(GLFW.GLFW_KEY_GRAVE_ACCENT, Binding.TERMINAL.defaultKey());
        for (boolean xaero : new boolean[]{false, true}) {
            for (Binding binding : Binding.values()) {
                assertTrue(KeyBindingDefaults.defaultKey(binding, xaero) != GLFW.GLFW_KEY_P,
                        () -> binding + " must not default to P (vanilla social interactions)");
            }
        }
    }

    @Test
    void loadoutFormerSquadAndAdministratorKeysStartUnbound() {
        assertEquals(-1, KeyBindingDefaults.UNBOUND, "InputConstants.UNKNOWN is key code -1");
        for (Binding binding : List.of(Binding.LOADOUT, Binding.SQUAD, Binding.ADMIN_LOADOUT,
                Binding.WEAPON_TUNING)) {
            assertEquals(KeyBindingDefaults.UNBOUND, KeyBindingDefaults.defaultKey(binding, false),
                    binding::name);
            assertEquals(KeyBindingDefaults.UNBOUND, KeyBindingDefaults.defaultKey(binding, true),
                    binding::name);
        }
    }

    @Test
    void tacticalMapIsMUnlessXaerosWorldMapAlreadyRoutesMToIt() {
        assertEquals(GLFW.GLFW_KEY_M, KeyBindingDefaults.defaultKey(Binding.TACTICAL_MAP, false));
        assertEquals(KeyBindingDefaults.UNBOUND,
                KeyBindingDefaults.defaultKey(Binding.TACTICAL_MAP, true),
                "Xaero's own M is redirected; a second WOK mapping on M would be marked red");
        assertEquals(GLFW.GLFW_KEY_GRAVE_ACCENT,
                KeyBindingDefaults.defaultKey(Binding.TERMINAL, true));
    }

    @Test
    void defaultsAvoidEveryKeyTheTestModpackUsesAndEachOther() {
        // The test modpack ships Xaero's World Map, so its defaults are the "xaero" column.
        Set<Integer> seen = new HashSet<>();
        for (Binding binding : Binding.values()) {
            int key = KeyBindingDefaults.defaultKey(binding, true);
            if (key == KeyBindingDefaults.UNBOUND) {
                continue;
            }
            assertFalse(TAKEN_BY_TEST_PACK.contains(key),
                    () -> binding + " default collides with the test modpack");
            assertTrue(seen.add(key), () -> binding + " shares its default key");
        }
        assertEquals(Set.of(GLFW.GLFW_KEY_GRAVE_ACCENT), seen);
        Set<Integer> withoutXaero = Arrays.stream(Binding.values())
                .map(binding -> KeyBindingDefaults.defaultKey(binding, false))
                .filter(key -> key != KeyBindingDefaults.UNBOUND)
                .collect(Collectors.toSet());
        assertEquals(Set.of(GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_M), withoutXaero);
    }

    @Test
    void legacyDefaultsAreTheKeysUpToBetaSeven() {
        assertEquals(1, KeyBindingDefaults.DEFAULTS_REVISION);
        Map<Binding, Integer> legacy = new EnumMap<>(Binding.class);
        legacy.put(Binding.TERMINAL, KeyBindingDefaults.UNBOUND);
        legacy.put(Binding.SQUAD, GLFW.GLFW_KEY_K);
        legacy.put(Binding.TACTICAL_MAP, GLFW.GLFW_KEY_M);
        legacy.put(Binding.LOADOUT, GLFW.GLFW_KEY_L);
        legacy.put(Binding.ADMIN_LOADOUT, GLFW.GLFW_KEY_U);
        legacy.put(Binding.WEAPON_TUNING, GLFW.GLFW_KEY_O);
        for (Binding binding : Binding.values()) {
            assertEquals(legacy.get(binding), binding.legacyDefaultKey(), binding::name);
        }
    }

    @Test
    void untouchedOldDefaultsMoveOnceToTheNewDefaults() {
        for (Binding binding : List.of(Binding.SQUAD, Binding.LOADOUT, Binding.ADMIN_LOADOUT,
                Binding.WEAPON_TUNING)) {
            for (boolean xaero : new boolean[]{false, true}) {
                assertEquals(OptionalInt.of(KeyBindingDefaults.UNBOUND),
                        KeyBindingDefaults.migratedKey(binding, true,
                                binding.legacyDefaultKey(), false, xaero),
                        () -> binding + " still on its old default is unbound");
            }
        }
        assertEquals(OptionalInt.of(KeyBindingDefaults.UNBOUND),
                KeyBindingDefaults.migratedKey(Binding.TACTICAL_MAP, true, GLFW.GLFW_KEY_M,
                        false, true), "M is left to Xaero's redirected open-map key");
        assertEquals(OptionalInt.empty(),
                KeyBindingDefaults.migratedKey(Binding.TACTICAL_MAP, true, GLFW.GLFW_KEY_M,
                        false, false), "without Xaero M stays the tactical map key");
    }

    @Test
    void theOldMapKeyOnlyGivesWayToXaerosKeyOnTheSameKey() {
        // The test client: Xaero's open-map key and the WOK map key both on M.
        assertTrue(KeyBindingDefaults.xaeroTakesOverMapKey(true, true, GLFW.GLFW_KEY_M, false));
        assertEquals(OptionalInt.of(KeyBindingDefaults.UNBOUND),
                KeyBindingDefaults.migratedKey(Binding.TACTICAL_MAP, true, GLFW.GLFW_KEY_M, false,
                        KeyBindingDefaults.xaeroTakesOverMapKey(true, true, GLFW.GLFW_KEY_M,
                                false)));
        // A player who moved Xaero's key off M to keep the WOK map there keeps WOK's M.
        assertFalse(KeyBindingDefaults.xaeroTakesOverMapKey(true, true, GLFW.GLFW_KEY_COMMA,
                false));
        assertEquals(OptionalInt.empty(),
                KeyBindingDefaults.migratedKey(Binding.TACTICAL_MAP, true, GLFW.GLFW_KEY_M, false,
                        KeyBindingDefaults.xaeroTakesOverMapKey(true, true, GLFW.GLFW_KEY_COMMA,
                                false)));
        assertFalse(KeyBindingDefaults.xaeroTakesOverMapKey(true, true, GLFW.GLFW_KEY_M, true),
                "Ctrl+M on Xaero does not collide with a plain M");
        assertFalse(KeyBindingDefaults.xaeroTakesOverMapKey(true, false, GLFW.GLFW_KEY_M, false),
                "Xaero's key missing or on a mouse button");
        assertFalse(KeyBindingDefaults.xaeroTakesOverMapKey(false, true, GLFW.GLFW_KEY_M, false),
                "no working redirect (unknown Xaero build): WOK keeps M");
    }

    @Test
    void keysThePlayerChoseAreNeverMigrated() {
        // The test client's own options.txt: loadout moved to ';', everything else untouched.
        assertEquals(OptionalInt.empty(), KeyBindingDefaults.migratedKey(Binding.LOADOUT, true,
                GLFW.GLFW_KEY_SEMICOLON, false, true));
        assertEquals(OptionalInt.empty(), KeyBindingDefaults.migratedKey(Binding.SQUAD, true,
                GLFW.GLFW_KEY_K, true, true), "Ctrl+K is a choice, not the old default");
        assertEquals(OptionalInt.empty(), KeyBindingDefaults.migratedKey(Binding.SQUAD, false,
                GLFW.GLFW_KEY_K, false, true), "a mouse button with the same number");
        assertEquals(OptionalInt.empty(), KeyBindingDefaults.migratedKey(Binding.SQUAD, true,
                KeyBindingDefaults.UNBOUND, false, true), "already unbound");
        assertEquals(OptionalInt.empty(), KeyBindingDefaults.migratedKey(Binding.WEAPON_TUNING,
                true, GLFW.GLFW_KEY_K, false, true), "the old default of another mapping");
        for (int key : new int[]{KeyBindingDefaults.UNBOUND, GLFW.GLFW_KEY_GRAVE_ACCENT,
                GLFW.GLFW_KEY_K}) {
            assertEquals(OptionalInt.empty(), KeyBindingDefaults.migratedKey(Binding.TERMINAL,
                    true, key, false, true), "the terminal key is new and never migrated");
        }
    }

    @Test
    void migrationWaitsForTheClientConfig() {
        assertEquals(-1, InfantryClientConfig.keyDefaultsRevision(),
                "unloaded client config: nothing is migrated or recorded yet");
    }

    @Test
    void onlyASnapshotWithAFactionCountsAsInABattle() {
        assertFalse(KeyBindingDefaults.inBattle(null));
        assertFalse(KeyBindingDefaults.inBattle(snapshot(null)),
                "players without a faction can still receive a snapshot");
        assertTrue(KeyBindingDefaults.inBattle(snapshot(Faction.BLUE)));
        assertTrue(KeyBindingDefaults.inBattle(snapshot(Faction.RED)));
    }

    @Test
    void everyMappingIsInGameOnly() {
        for (Binding binding : Binding.values()) {
            assertEquals(KeyConflictContext.IN_GAME, binding.conflictContext(), binding::name);
        }
    }

    @Test
    void mappingNamesStayStableSoPlayersKeepTheirKeys() {
        Map<Binding, String> expected = new EnumMap<>(Binding.class);
        expected.put(Binding.TERMINAL, "key.wok_infantry.open_terminal");
        expected.put(Binding.SQUAD, "key.wok_infantry.open_squad");
        expected.put(Binding.TACTICAL_MAP, "key.wok_infantry.open_tactical_map");
        expected.put(Binding.LOADOUT, "key.wok_infantry.open_loadout");
        expected.put(Binding.ADMIN_LOADOUT, "key.wok_infantry.open_admin_loadout");
        expected.put(Binding.WEAPON_TUNING, "key.wok_infantry.open_weapon_tuning");
        for (Binding binding : Binding.values()) {
            assertEquals(expected.get(binding), binding.mappingName());
        }
        assertEquals("key.categories.wok_infantry", KeyBindingDefaults.CATEGORY);
        // UI acceptance harness convention: the only "*terminal*" mapping is the terminal key.
        List<Binding> terminals = Arrays.stream(Binding.values())
                .filter(binding -> binding.mappingName().contains("terminal")).toList();
        assertEquals(List.of(Binding.TERMINAL), terminals);
    }

    @Test
    void administratorKeysAreOnlySentWithPermissionLevelTwo() {
        assertEquals(BattleRules.ADMIN_PERMISSION_LEVEL, KeyBindingDefaults.ADMIN_PERMISSION_LEVEL);
        for (Binding binding : Binding.values()) {
            boolean admin = binding == Binding.ADMIN_LOADOUT || binding == Binding.WEAPON_TUNING;
            assertEquals(admin, binding.administratorOnly(), binding::name);
            assertEquals(!admin, KeyBindingDefaults.allowsPress(binding, level -> level <= 0),
                    () -> binding + " for an ordinary player");
            assertEquals(!admin, KeyBindingDefaults.allowsPress(binding, level -> level <= 1),
                    () -> binding + " for a permission-1 player");
            assertTrue(KeyBindingDefaults.allowsPress(binding, level -> level <= 2),
                    () -> binding + " for an administrator");
            assertEquals(!admin, KeyBindingDefaults.allowsPress(binding, null),
                    () -> binding + " without a player");
        }
    }

    @Test
    void terminalOpensTheFormationPageUntilTheFormationIsSet() {
        // No faction: the server keeps no battle state, the formation snapshot says "required".
        assertEquals(TerminalRoute.FORMATION, KeyBindingDefaults.terminalRoute(false, true));
        // Joined a faction, vote not locked yet.
        assertEquals(TerminalRoute.FORMATION, KeyBindingDefaults.terminalRoute(true, true));
        // Nothing cached at all: most likely no faction.
        assertEquals(TerminalRoute.FORMATION, KeyBindingDefaults.terminalRoute(false, null));
        // Formation set.
        assertEquals(TerminalRoute.SQUAD, KeyBindingDefaults.terminalRoute(true, false));
        assertEquals(TerminalRoute.SQUAD, KeyBindingDefaults.terminalRoute(true, null));
        // Formation set, battle snapshot not received yet: the server opens the squad page.
        assertEquals(TerminalRoute.SQUAD, KeyBindingDefaults.terminalRoute(false, false));
    }

    @Test
    void mapKeyLeavesSharedPressesToXaeroAndFallsBackToTheFormationPage() {
        for (boolean battle : new boolean[]{false, true}) {
            for (Boolean required : new Boolean[]{null, false, true}) {
                assertEquals(MapRoute.LEAVE_TO_XAERO,
                        KeyBindingDefaults.mapRoute(true, battle, required));
            }
        }
        assertEquals(MapRoute.MAP, KeyBindingDefaults.mapRoute(false, true, null));
        assertEquals(MapRoute.MAP, KeyBindingDefaults.mapRoute(false, true, false));
        // Review fix UI-03: while the faction still votes the terminal's map tab is locked, so
        // the map key goes to the formation page like the terminal key.
        assertEquals(MapRoute.FORMATION, KeyBindingDefaults.mapRoute(false, true, true),
                "a faction without a locked formation has not entered the battle");
        assertEquals(MapRoute.MAP, KeyBindingDefaults.mapRoute(false, false, false));
        assertEquals(MapRoute.FORMATION, KeyBindingDefaults.mapRoute(false, false, true));
        assertEquals(MapRoute.FORMATION, KeyBindingDefaults.mapRoute(false, false, null));
    }

    @Test
    void theThinGraveAccentIsNamedAsPrintedOnTheKey() {
        // Review fix UI-04: the default terminal key's own name is a 1-2px glyph in key caps.
        assertEquals("key.wok_infantry.cap.grave", KeyBindingDefaults.readableKeyNameKey("`"));
        for (String keyName : new String[]{"M", "K", "Tab", "Ctrl + `", "~", "", "F1"}) {
            assertNull(KeyBindingDefaults.readableKeyNameKey(keyName), keyName);
        }
        assertNull(KeyBindingDefaults.readableKeyNameKey(null));
    }

    @Test
    void terminalMapAndXaeroRedirectShareOneBattleEnteredRule() {
        for (boolean faction : new boolean[]{false, true}) {
            for (Boolean required : new Boolean[]{null, false, true}) {
                boolean entered = KeyBindingDefaults.battleEntered(faction, required);
                assertEquals(entered ? TerminalRoute.SQUAD : TerminalRoute.FORMATION,
                        KeyBindingDefaults.terminalRoute(faction, required));
                assertEquals(entered ? MapRoute.MAP : MapRoute.FORMATION,
                        KeyBindingDefaults.mapRoute(false, faction, required));
            }
        }
        // The formation snapshot decides; the battle snapshot only before it arrived.
        assertTrue(KeyBindingDefaults.battleEntered(false, false));
        assertFalse(KeyBindingDefaults.battleEntered(true, true), "voting faction member");
        assertTrue(KeyBindingDefaults.battleEntered(true, null));
        assertFalse(KeyBindingDefaults.battleEntered(false, null));
    }

    @Test
    void keyLabelsFallBackToTheMappingThatOpensTheSameScreen() {
        assertEquals(List.of(LabelSource.TERMINAL, LabelSource.SQUAD),
                KeyBindingDefaults.labelSources(Binding.TERMINAL, true));
        assertEquals(List.of(LabelSource.SQUAD, LabelSource.TERMINAL),
                KeyBindingDefaults.labelSources(Binding.SQUAD, false));
        assertEquals(List.of(LabelSource.TACTICAL_MAP),
                KeyBindingDefaults.labelSources(Binding.TACTICAL_MAP, false));
        assertEquals(List.of(LabelSource.TACTICAL_MAP, LabelSource.XAERO_OPEN_MAP),
                KeyBindingDefaults.labelSources(Binding.TACTICAL_MAP, true));
        for (Binding binding : List.of(Binding.LOADOUT, Binding.ADMIN_LOADOUT,
                Binding.WEAPON_TUNING)) {
            List<LabelSource> sources = KeyBindingDefaults.labelSources(binding, true);
            assertEquals(1, sources.size());
            assertEquals(binding, sources.get(0).binding());
        }
        assertNull(LabelSource.XAERO_OPEN_MAP.binding());
    }

    @Test
    void effectiveMappingIsTheFirstBoundSource() {
        Map<LabelSource, FakeKey> keys = new EnumMap<>(LabelSource.class);
        keys.put(LabelSource.TERMINAL, new FakeKey("`", true));
        keys.put(LabelSource.SQUAD, new FakeKey("K", true));
        keys.put(LabelSource.TACTICAL_MAP, new FakeKey("", false));
        keys.put(LabelSource.XAERO_OPEN_MAP, new FakeKey("M", true));
        keys.put(LabelSource.LOADOUT, new FakeKey("", false));

        assertEquals("`", label(Binding.TERMINAL, true, keys));
        assertEquals("K", label(Binding.SQUAD, true, keys));
        assertEquals("M", label(Binding.TACTICAL_MAP, true, keys),
                "unbound WOK map key shows Xaero's redirected key");
        assertNull(label(Binding.TACTICAL_MAP, false, keys), "redirect off: nothing to show");
        assertNull(label(Binding.LOADOUT, true, keys), "unbound loadout key: hint left out");
        assertNull(label(Binding.WEAPON_TUNING, true, keys), "missing mapping: hint left out");

        keys.put(LabelSource.TERMINAL, new FakeKey("", false));
        assertEquals("K", label(Binding.TERMINAL, true, keys),
                "a player who unbound the terminal but kept K still sees K");
        keys.put(LabelSource.TACTICAL_MAP, new FakeKey("N", true));
        assertEquals("N", label(Binding.TACTICAL_MAP, true, keys), "own binding wins");
    }

    /** Stand-in for a KeyMapping: its display text and whether it has a key. */
    private record FakeKey(String label, boolean bound) {
    }

    private static BattleSnapshot snapshot(Faction faction) {
        return new BattleSnapshot(new UUID(0L, 1L), faction, null, false, false, 0, 0,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY, List.of(), List.of(),
                List.of(), new PermissionView(false, false, false, false, false, false, false),
                List.of(), 1_000L, 1L);
    }

    private static String label(Binding binding, boolean xaeroRedirect,
                                Map<LabelSource, FakeKey> keys) {
        FakeKey mapping = KeyBindingDefaults.effectiveMapping(binding, xaeroRedirect, keys::get,
                FakeKey::bound);
        return mapping == null ? null : mapping.label();
    }
}
