package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.FormationContextView;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Faction livery of the WOK步战 tablet: the device paint ({@link DeviceSkin}) and the screen
 * palette ({@link TacticalPalette}) of the viewer's side (P3, preview {@code 18-device-livery.js}):
 * the blue side is painted as the Academy (navy), the red side as Caesar (true red), and a viewer
 * without a faction sees the pale Neutral livery.
 *
 * <p>{@link #current()} decides once per frame, from volatile client snapshots only:
 * <ol>
 * <li>a battle snapshot: its side ({@link Faction#BLUE} → Academy, {@link Faction#RED} → Caesar);</li>
 * <li>no battle snapshot yet (up to a second after login) but a faction chosen in the formation
 *     catalog: that faction's side, from the faction ids the battle snapshots of this session
 *     reported (own and enemy), else the default catalog's {@code academy} = blue and
 *     {@code caesar} = red; an unknown id is Neutral;</li>
 * <li>otherwise Neutral (no faction yet, or a spectating administrator).</li>
 * </ol>
 * The catalog view deliberately carries no battle side (no protocol change), hence the learned
 * id table; it is cleared on logout ({@link #reset()}).
 */
public final class TacticalLivery {
    /** The three paint schemes of the device and its screens. */
    public enum Livery {
        /** Blue side: navy case, blue selection. */
        ACADEMY,
        /** Red side: true-red case, deep crimson selection. */
        CAESAR,
        /** No faction yet (or a spectating administrator): pale steel case, graphite selection. */
        NEUTRAL;

        /** Screen palette of this livery in {@code scope}. */
        public TacticalPalette palette(Scope scope) {
            TacticalPalette board = switch (this) {
                case ACADEMY -> TacticalPalette.ACADEMY;
                case CAESAR -> TacticalPalette.CAESAR;
                case NEUTRAL -> TacticalPalette.NEUTRAL;
            };
            return board.withScope(scope);
        }

        /** Device paint of this livery. */
        public DeviceSkin skin() {
            return DeviceSkin.forLivery(this);
        }
    }

    /**
     * Page scope of a palette. A scope may override a few tokens on top of the livery, e.g. the
     * Caesar map selects in graphite so a red selection never reads as an enemy marker.
     */
    public enum Scope {
        /** Every board page (squad terminal, formation page, deployment mini-map). */
        BOARD,
        /** The tactical map page. */
        MAP
    }

    /** Battle sides of the default catalog's factions ({@code FormationConfigData.defaultConfig}). */
    static final Map<String, Faction> DEFAULT_SIDES = Map.of("academy", Faction.BLUE,
            "caesar", Faction.RED);

    private static volatile Livery pinned;
    /** Faction id → side, learned from this session's battle snapshots; replaced, never mutated. */
    private static volatile Map<String, Faction> learnedSides = Map.of();
    /** The snapshot {@link #learnedSides} was last learned from (identity check, once per frame). */
    private static volatile BattleSnapshot learnedFrom;

    private TacticalLivery() {
    }

    /** Livery of the local viewer, read once per frame by {@link TacticalScreen}. */
    public static Livery current() {
        Livery pin = pinned;
        if (pin != null) {
            return pin;
        }
        BattleSnapshot battle = ClientBattleState.snapshot();
        if (battle != null) {
            learnFrom(battle);
            return forSide(battle.faction());
        }
        FormationSelectionSnapshot catalog = ClientFormationState.snapshot();
        return resolve(null, catalog == null ? "" : catalog.selectedFactionId(), learnedSides);
    }

    /**
     * UI acceptance only: every screen uses {@code livery} regardless of the battle state;
     * {@code null} returns to the viewer's own livery.
     */
    public static void pinForAcceptance(Livery livery) {
        pinned = livery;
    }

    /** Forgets the learned faction sides (logout: the next server may name its factions anew). */
    public static void reset() {
        learnedSides = Map.of();
        learnedFrom = null;
    }

    /**
     * The livery rule (pure): the battle side wins; without one, the chosen catalog faction's side
     * from {@code learned}, then {@link #DEFAULT_SIDES}; otherwise Neutral.
     *
     * @param battleSide        the viewer's side in the battle snapshot, or {@code null} without one
     * @param selectedFactionId the faction chosen in the formation catalog, {@code ""} or {@code null}
     *                          for none
     * @param learned           faction id → side as reported by earlier battle snapshots
     */
    static Livery resolve(Faction battleSide, String selectedFactionId,
                          Map<String, Faction> learned) {
        if (battleSide != null) {
            return forSide(battleSide);
        }
        if (selectedFactionId == null || selectedFactionId.isEmpty()) {
            return Livery.NEUTRAL;
        }
        Faction side = learned == null ? null : learned.get(selectedFactionId);
        if (side == null) {
            side = DEFAULT_SIDES.get(selectedFactionId);
        }
        return forSide(side);
    }

    /** Academy for the blue side, Caesar for the red side, Neutral for none. */
    public static Livery forSide(Faction side) {
        if (side == null) {
            return Livery.NEUTRAL;
        }
        return switch (side) {
            case BLUE -> Livery.ACADEMY;
            case RED -> Livery.CAESAR;
        };
    }

    /**
     * {@code table} plus the two faction ids of a battle context (pure): the own faction is on
     * {@code side}, the enemy faction on the opposite side. Empty ids are skipped (and an enemy id
     * equal to the own one); the same table instance comes back when nothing changes.
     */
    static Map<String, Faction> learn(Map<String, Faction> table, Faction side,
                                      FormationContextView context) {
        Objects.requireNonNull(table, "table");
        if (side == null || context == null) {
            return table;
        }
        Map<String, Faction> next = new HashMap<>(table);
        String own = context.factionId();
        String enemy = context.enemyFactionId();
        if (!own.isEmpty()) {
            next.put(own, side);
        }
        if (!enemy.isEmpty() && !enemy.equals(own)) {
            next.put(enemy, side.opposite());
        }
        return next.equals(table) ? table : Map.copyOf(next);
    }

    private static void learnFrom(BattleSnapshot battle) {
        if (battle == learnedFrom) {
            return;
        }
        learnedSides = learn(learnedSides, battle.faction(), battle.formationContext());
        learnedFrom = battle;
    }

    /**
     * The faction sides learned so far. Read by {@link FormationSelectionScreen#pageLivery} for a
     * joined page whose battle snapshot has not caught up with the join; also a unit-test seam.
     */
    static Map<String, Faction> learnedSides() {
        return learnedSides;
    }
}
