package com.wok.infantry.client;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * Key table and pure key routing of WOK步战核心 (decided 2026-10-04 for 0.3.0-beta.8).
 *
 * <p>{@link ClientBootstrap} builds every {@code KeyMapping} from this table, so the defaults,
 * the conflict context and the routing rules can be unit tested without a game client.</p>
 *
 * <ul>
 *   <li>Battle terminal (squad page) on the grave accent key {@code `}, which neither vanilla
 *   nor the test modpack uses. {@code P} would collide with vanilla
 *   {@code key.socialInteractions}.</li>
 *   <li>Tactical map on {@code M}. When Xaero's World Map is installed its own open-map key
 *   ({@code gui.xaero_open_map}, also {@code M}) is redirected to the tactical map while the
 *   player is in a battle, so the WOK mapping starts unbound there instead of showing a red
 *   duplicate in the controls screen (see {@link #defaultKey(Binding, boolean)}).</li>
 *   <li>Loadout, the old squad mapping, the administrator loadout terminal and the weapon
 *   editor start unbound. Players who already bound them keep their keys: the mapping names
 *   ({@code key.wok_infantry.*}) are unchanged and options.txt stores the chosen key. Only a
 *   mapping still exactly on its old default is moved once to the new default
 *   ({@link #migratedKey}), because options.txt cannot tell an untouched default from a choice.
 *   Administrator keys are only sent with permission level 2 ({@link #allowsPress}).</li>
 *   <li>Every mapping uses {@link KeyConflictContext#IN_GAME}: it only fires without a screen
 *   open. Note that Forge 47's {@code KeyMapping.same} still reports two mappings on the same
 *   key as a conflict whatever their contexts, so only distinct keys remove the red mark.</li>
 * </ul>
 */
public final class KeyBindingDefaults {
    public static final String CATEGORY = "key.categories.wok_infantry";
    /** GLFW key code of an unbound mapping ({@code InputConstants.UNKNOWN}). */
    public static final int UNBOUND = GLFW.GLFW_KEY_UNKNOWN;
    /** Permission level the server requires for the administrator screens. */
    public static final int ADMIN_PERMISSION_LEVEL = BattleRules.ADMIN_PERMISSION_LEVEL;

    /**
     * Revision of this default key table. {@link #migratedKey} moves mappings still on the
     * defaults of an older revision once; bump it (and extend the legacy defaults) only when a
     * later release moves default keys again.
     */
    public static final int DEFAULTS_REVISION = 1;

    /** One WOK key mapping. The mapping names are a compatibility contract (options.txt). */
    public enum Binding {
        /** Opens the battle terminal (squad page), or the formation page before a formation. */
        TERMINAL("key.wok_infantry.open_terminal", GLFW.GLFW_KEY_GRAVE_ACCENT, UNBOUND, false),
        /** Former squad key (K). Kept for players who bound it; same route as the terminal. */
        SQUAD("key.wok_infantry.open_squad", UNBOUND, GLFW.GLFW_KEY_K, false),
        TACTICAL_MAP("key.wok_infantry.open_tactical_map", GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_M,
                false),
        LOADOUT("key.wok_infantry.open_loadout", UNBOUND, GLFW.GLFW_KEY_L, false),
        ADMIN_LOADOUT("key.wok_infantry.open_admin_loadout", UNBOUND, GLFW.GLFW_KEY_U, true),
        WEAPON_TUNING("key.wok_infantry.open_weapon_tuning", UNBOUND, GLFW.GLFW_KEY_O, true);

        private final String mappingName;
        private final int defaultKey;
        private final int legacyDefaultKey;
        private final boolean administratorOnly;

        Binding(String mappingName, int defaultKey, int legacyDefaultKey,
                boolean administratorOnly) {
            this.mappingName = mappingName;
            this.defaultKey = defaultKey;
            this.legacyDefaultKey = legacyDefaultKey;
            this.administratorOnly = administratorOnly;
        }

        /** Stable {@code KeyMapping} name, also the translation key of its controls entry. */
        public String mappingName() {
            return mappingName;
        }

        /** Default GLFW key without optional map mods; {@link #UNBOUND} for none. */
        public int defaultKey() {
            return defaultKey;
        }

        /**
         * Default key up to 0.3.0-beta.7 (K, M, L, U, O); {@link #UNBOUND} for the terminal key,
         * which did not exist then.
         */
        public int legacyDefaultKey() {
            return legacyDefaultKey;
        }

        /** Whether the key opens a permission-two screen (pressed only by administrators). */
        public boolean administratorOnly() {
            return administratorOnly;
        }

        /** Every WOK mapping only fires in game, never while a screen is open. */
        public IKeyConflictContext conflictContext() {
            return KeyConflictContext.IN_GAME;
        }
    }

    /** Where the terminal key (and the former squad key) goes. */
    public enum TerminalRoute {
        /** Ask the server for the squad page (BattleOpenPacket SQUAD). */
        SQUAD,
        /** Ask the server for the faction/formation catalog, which opens the formation page. */
        FORMATION
    }

    /** Where the tactical-map key goes. */
    public enum MapRoute {
        /** Ask the server for the tactical map (BattleOpenPacket MAP). */
        MAP,
        /** Not in a battle yet: open the formation page instead of an empty map. */
        FORMATION,
        /**
         * Xaero's World Map handles the same key press: in a battle its map screen is redirected
         * to the tactical map, otherwise Xaero's own map opens. WOK sends nothing, so the Xaero
         * map never flashes up before the tactical map.
         */
        LEAVE_TO_XAERO
    }

    /** Label source for a binding, in priority order (see {@link #labelSources}). */
    public enum LabelSource {
        TERMINAL, SQUAD, TACTICAL_MAP, LOADOUT, ADMIN_LOADOUT, WEAPON_TUNING,
        /** Xaero's {@code gui.xaero_open_map} while it is redirected to the tactical map. */
        XAERO_OPEN_MAP;

        static LabelSource of(Binding binding) {
            return valueOf(binding.name());
        }

        /** The WOK binding this source stands for, or {@code null} for Xaero's mapping. */
        public Binding binding() {
            return this == XAERO_OPEN_MAP ? null : Binding.valueOf(name());
        }
    }

    private KeyBindingDefaults() {
    }

    /**
     * Default key of {@code binding}. With Xaero's World Map installed the tactical map has no
     * WOK default: Xaero's own {@code M} reaches the tactical map through the redirect, and a
     * second mapping on {@code M} would be marked red in the controls screen.
     */
    public static int defaultKey(Binding binding, boolean xaeroWorldMapInstalled) {
        Objects.requireNonNull(binding, "binding");
        if (binding == Binding.TACTICAL_MAP && xaeroWorldMapInstalled) {
            return UNBOUND;
        }
        return binding.defaultKey();
    }

    /**
     * One-time move of a mapping that still sits exactly on its pre-0.3.0-beta.8 default key
     * (K, L, U, O, and M while Xaero's World Map is installed) to the new default. options.txt
     * stores every key, so without this an upgraded client would keep the old, conflicting keys
     * and the red marks in the controls screen. Anything the player chose is kept: another key, a
     * mouse button, or the old key with a modifier.
     *
     * @param keyboardKey whether the mapping is bound to a keyboard key ({@code KEYSYM})
     * @param keyCode its GLFW key code
     * @param modified whether it has a Forge key modifier (Ctrl, Shift, Alt)
     * @return the GLFW key to set ({@link #UNBOUND} to unbind), or empty to leave it alone
     */
    public static OptionalInt migratedKey(Binding binding, boolean keyboardKey, int keyCode,
                                          boolean modified, boolean xaeroWorldMapInstalled) {
        Objects.requireNonNull(binding, "binding");
        int legacy = binding.legacyDefaultKey();
        int current = defaultKey(binding, xaeroWorldMapInstalled);
        if (legacy == UNBOUND || legacy == current || !keyboardKey || modified
                || keyCode != legacy) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(current);
    }

    /**
     * Whether the cached battle snapshot puts the player in a battle, i.e. on a faction. The
     * server also answers players without a faction with a snapshot whose faction is
     * {@code null}; those are not in a battle.
     */
    public static boolean inBattle(BattleSnapshot snapshot) {
        return snapshot != null && snapshot.faction() != null;
    }

    /**
     * Client-side permission guess for a key press. Administrator keys are only sent when the
     * local player has permission level 2, so ordinary players pressing them see nothing instead
     * of a red chat error. The server still checks every request.
     */
    public static boolean allowsPress(Binding binding, IntPredicate hasPermissionLevel) {
        Objects.requireNonNull(binding, "binding");
        return !binding.administratorOnly() || hasPermissionLevel != null
                && hasPermissionLevel.test(ADMIN_PERMISSION_LEVEL);
    }

    /**
     * Terminal key routing. Before the player has a formation (cached formation snapshot says a
     * selection is required, or there is no battle state at all because the player has no
     * faction) the key opens the formation page; otherwise the squad page.
     *
     * @param inBattle {@link #inBattle} of the cached battle snapshot (has a faction)
     * @param formationRequired the cached formation snapshot's {@code selectionRequired}, or
     *                          {@code null} when no formation snapshot was received yet
     */
    public static TerminalRoute terminalRoute(boolean inBattle, Boolean formationRequired) {
        if (formationRequired != null) {
            return formationRequired ? TerminalRoute.FORMATION : TerminalRoute.SQUAD;
        }
        return inBattle ? TerminalRoute.SQUAD : TerminalRoute.FORMATION;
    }

    /**
     * Tactical-map key routing.
     *
     * @param xaeroHandlesPress whether Xaero's open-map key fires on the same press and is
     *                          redirected (see {@code XaeroWorldMapPolicy.handlesSamePress})
     * @param inBattle {@link #inBattle} of the cached battle snapshot (has a faction)
     */
    public static MapRoute mapRoute(boolean xaeroHandlesPress, boolean inBattle,
                                    Boolean formationRequired) {
        if (xaeroHandlesPress) {
            return MapRoute.LEAVE_TO_XAERO;
        }
        if (inBattle || Boolean.FALSE.equals(formationRequired)) {
            return MapRoute.MAP;
        }
        return MapRoute.FORMATION;
    }

    /**
     * Mappings whose key may be shown for {@code binding}, best first. The terminal and the former
     * squad key open the same screen, so each falls back to the other; the tactical map falls back
     * to Xaero's open-map key while that key is redirected to it.
     */
    public static List<LabelSource> labelSources(Binding binding, boolean xaeroRedirectActive) {
        Objects.requireNonNull(binding, "binding");
        return switch (binding) {
            case TERMINAL -> List.of(LabelSource.TERMINAL, LabelSource.SQUAD);
            case SQUAD -> List.of(LabelSource.SQUAD, LabelSource.TERMINAL);
            case TACTICAL_MAP -> xaeroRedirectActive
                    ? List.of(LabelSource.TACTICAL_MAP, LabelSource.XAERO_OPEN_MAP)
                    : List.of(LabelSource.TACTICAL_MAP);
            default -> List.of(LabelSource.of(binding));
        };
    }

    /**
     * First candidate of {@link #labelSources} that resolves to a bound mapping, or {@code null}.
     *
     * @param resolve the mapping of a source ({@code null} when it does not exist)
     * @param bound whether a resolved mapping has a key
     */
    public static <K> K effectiveMapping(Binding binding, boolean xaeroRedirectActive,
                                         Function<LabelSource, K> resolve, Predicate<K> bound) {
        for (LabelSource source : labelSources(binding, xaeroRedirectActive)) {
            K mapping = resolve.apply(source);
            if (mapping != null && bound.test(mapping)) {
                return mapping;
            }
        }
        return null;
    }
}
