package com.wok.infantry.testmode;

import com.wok.infantry.battle.Faction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Pure rules of the server-wide test mode and of {@code /battle admin test start}, apart from
 * the server singletons so they are unit-tested: the on/off switch, which faction and formation
 * a test start uses, where the missing main bases are placed and which squad callsign is taken.
 */
public final class TestModeRules {
    /** The red main base is placed this many blocks east (+X) of the overworld spawn. */
    public static final int RED_BASE_OFFSET_X = 64;
    /** Formation id preferred when neither an argument nor a lock decides. */
    public static final String DEFAULT_FORMATION_ID = "default";

    private TestModeRules() {
    }

    // ---- switch -----------------------------------------------------------------------------

    /** Result of {@code /battle admin test mode on|off}. */
    public enum Transition {
        ENABLED(true, true),
        ALREADY_ENABLED(false, true),
        DISABLED(true, false),
        ALREADY_DISABLED(false, false);

        private final boolean changed;
        private final boolean enabledAfter;

        Transition(boolean changed, boolean enabledAfter) {
            this.changed = changed;
            this.enabledAfter = enabledAfter;
        }

        /** Whether the switch really moved (only then is it saved and announced). */
        public boolean changed() {
            return changed;
        }

        /** Whether the test mode is on afterwards. */
        public boolean enabledAfter() {
            return enabledAfter;
        }
    }

    /** The switch: turning it to the state it already has changes nothing. */
    public static Transition transition(boolean enabled, boolean requested) {
        if (requested) {
            return enabled ? Transition.ALREADY_ENABLED : Transition.ENABLED;
        }
        return enabled ? Transition.DISABLED : Transition.ALREADY_DISABLED;
    }

    // ---- faction --------------------------------------------------------------------------------

    /** Where the faction of a test start came from. */
    public enum FactionSource {
        /** The command argument (the page's browsed faction). */
        ARGUMENT,
        /** The faction the player is already in. */
        CURRENT,
        /** No argument and no faction: the first enabled public faction of the catalog. */
        FIRST_PUBLIC
    }

    /** Chosen public faction id, or an error text. */
    public record FactionChoice(String factionId, FactionSource source, String error) {
        public FactionChoice {
            factionId = Objects.requireNonNullElse(factionId, "");
            error = Objects.requireNonNullElse(error, "");
        }

        public boolean ok() {
            return error.isEmpty();
        }

        static FactionChoice failure(String error) {
            return new FactionChoice("", null, error);
        }
    }

    /**
     * Faction of a test start: the argument first, else the faction the player is already in,
     * else the first enabled public faction. An argument that names no enabled faction is an
     * error (never silently replaced).
     *
     * @param requested       command argument, blank when omitted
     * @param current         public faction the player is in, blank when none
     * @param enabledFactions enabled public faction ids in catalog order
     */
    public static FactionChoice chooseFaction(String requested, String current,
                                              List<String> enabledFactions) {
        List<String> factions = enabledFactions == null ? List.of() : enabledFactions;
        String argument = normalize(requested);
        if (!argument.isEmpty()) {
            String match = find(factions, argument);
            return match == null
                    ? FactionChoice.failure("阵营“" + requested.trim() + "”不存在或已停用")
                    : new FactionChoice(match, FactionSource.ARGUMENT, "");
        }
        String own = find(factions, normalize(current));
        if (own != null) {
            return new FactionChoice(own, FactionSource.CURRENT, "");
        }
        if (factions.isEmpty()) {
            return FactionChoice.failure("阵营编制目录里没有启用的公开阵营");
        }
        return new FactionChoice(factions.get(0), FactionSource.FIRST_PUBLIC, "");
    }

    // ---- formation ------------------------------------------------------------------------------

    /** Where the formation of a test start came from. */
    public enum FormationSource {
        /** The command argument (the page's highlighted formation). */
        ARGUMENT,
        /** The faction's locked formation. */
        LOCKED,
        /** The faction's {@code default} formation. */
        DEFAULT,
        /** The faction's first usable formation. */
        FIRST_CANDIDATE
    }

    /**
     * Chosen formation of a test start.
     *
     * @param chosenId       formation the administrator asked for (or the fallback)
     * @param source         where {@code chosenId} came from
     * @param effectiveId    formation the player really joins: the faction's lock wins
     * @param lockKept       the faction is locked to another formation than the argument; the
     *                       lock is kept and the player joins the locked one
     * @param needsLock      the faction is not locked yet: the test start locks {@code chosenId}
     * @param error          why no formation could be chosen (empty when chosen)
     */
    public record FormationChoice(String chosenId, FormationSource source, String effectiveId,
                                  boolean lockKept, boolean needsLock, String error) {
        public FormationChoice {
            chosenId = Objects.requireNonNullElse(chosenId, "");
            effectiveId = Objects.requireNonNullElse(effectiveId, "");
            error = Objects.requireNonNullElse(error, "");
        }

        public boolean ok() {
            return error.isEmpty();
        }

        static FormationChoice failure(String error) {
            return new FormationChoice("", null, "", false, false, error);
        }
    }

    /**
     * Formation of a test start: the argument first, else the faction's locked formation, else
     * {@code default}, else the first usable formation. A locked faction keeps its lock: the
     * player joins the locked formation even when the argument names another one (reported as
     * {@code lockKept}); an unlocked faction is locked to the chosen formation.
     *
     * @param requested     command argument, blank when omitted
     * @param lockedId      the faction's locked formation, blank while it is not locked
     * @param usableIds     the faction's usable formation ids in catalog order (the ballot
     *                      candidates)
     */
    public static FormationChoice chooseFormation(String requested, String lockedId,
                                                  List<String> usableIds) {
        List<String> usable = usableIds == null ? List.of() : usableIds;
        String argument = normalize(requested);
        String locked = normalize(lockedId);
        if (!argument.isEmpty()) {
            String match = find(usable, argument);
            if (!locked.isEmpty()) {
                // The lock decides; an argument naming the locked formation itself is fine even
                // when that formation is no longer usable (joining then reports why).
                boolean same = argument.equals(locked);
                if (match == null && !same) {
                    return FormationChoice.failure("编制“" + requested.trim()
                            + "”不存在或当前不可用");
                }
                return new FormationChoice(same ? locked : match, FormationSource.ARGUMENT,
                        locked, !same, false, "");
            }
            return match == null
                    ? FormationChoice.failure("编制“" + requested.trim() + "”不存在或当前不可用")
                    : new FormationChoice(match, FormationSource.ARGUMENT, match, false, true, "");
        }
        if (!locked.isEmpty()) {
            return new FormationChoice(locked, FormationSource.LOCKED, locked, false, false, "");
        }
        String fallback = find(usable, DEFAULT_FORMATION_ID);
        if (fallback != null) {
            return new FormationChoice(fallback, FormationSource.DEFAULT, fallback, false, true,
                    "");
        }
        if (usable.isEmpty()) {
            return FormationChoice.failure("该阵营没有可用的编制");
        }
        return new FormationChoice(usable.get(0), FormationSource.FIRST_CANDIDATE,
                usable.get(0), false, true, "");
    }

    // ---- room -----------------------------------------------------------------------------------

    /**
     * Whether a test start can seat the player in the faction and its (locked or about to be
     * locked) formation, checked before anything changes so a full faction never leaves a lock
     * behind without the player (审查修正). The faction then holds at most the smaller of its
     * maximum and the formation's capacity, the same bound the administrator assignment and the
     * lock apply.
     *
     * @param factionName       public faction name for the reason
     * @param othersInFaction   members of the faction other than the player
     * @param factionMax        the faction's maximum
     * @param formationName     public formation name for the reason
     * @param formationCapacity the formation's capacity
     * @return blank when there is room, otherwise why not
     */
    public static String seatRefusal(String factionName, int othersInFaction, int factionMax,
                                     String formationName, int formationCapacity) {
        int limit = Math.min(factionMax, formationCapacity);
        if (limit < 1) {
            return factionName + "的编制“" + formationName + "”容量配置无效，无法加入";
        }
        if (othersInFaction < limit) {
            return "";
        }
        return factionName + "已满：已有 " + othersInFaction + " 人，最多 " + limit + " 人"
                + (formationCapacity < factionMax ? "（编制“" + formationName + "”容量 "
                + formationCapacity + "）" : "") + "，未做任何改动";
    }

    // ---- squad ----------------------------------------------------------------------------------

    /**
     * First callsign of the formation that nobody uses yet (the test start creates it and the
     * player leads it), or blank when every configured squad already exists.
     *
     * @param configured callsign ids of the formation in catalog order
     * @param existing   callsign ids that already have members
     */
    public static String firstFreeCallsign(List<String> configured, Collection<String> existing) {
        if (configured == null) {
            return "";
        }
        for (String callsign : configured) {
            String id = normalize(callsign);
            if (!id.isEmpty() && (existing == null || existing.stream()
                    .noneMatch(taken -> id.equals(normalize(taken))))) {
                return id;
            }
        }
        return "";
    }

    // ---- main bases -----------------------------------------------------------------------------

    /** Half width of the square searched around a base centre when the centre itself fails. */
    public static final int BASE_SEARCH_RADIUS = 24;
    /** Distance between the columns of that search (one per half chunk). */
    public static final int BASE_SEARCH_STEP = 8;

    /**
     * A column (x, z) tried for a missing main base; the height comes from the world surface.
     *
     * @param label short description for the report ("出生点", "出生点 +64 X" …)
     */
    public record BaseColumn(int x, int z, String label) {
    }

    /**
     * Centres tried, in order, for a missing main base of {@code side} near the overworld spawn
     * ({@code spawnX}, {@code spawnZ}): blue at the spawn, red {@value #RED_BASE_OFFSET_X} blocks
     * east of it, and red {@value #RED_BASE_OFFSET_X} blocks west only when nothing east of the
     * spawn can be used (the world border, water everywhere).
     */
    public static List<BaseColumn> baseCenters(Faction side, int spawnX, int spawnZ) {
        if (side == Faction.RED) {
            int d = RED_BASE_OFFSET_X;
            return List.of(new BaseColumn(spawnX + d, spawnZ, "出生点 +" + d + " X"),
                    new BaseColumn(spawnX - d, spawnZ, "出生点 -" + d + " X"));
        }
        return List.of(new BaseColumn(spawnX, spawnZ, "出生点"));
    }

    /**
     * Columns tried for one centre: the centre first, then square rings every
     * {@value #BASE_SEARCH_STEP} blocks out to {@value #BASE_SEARCH_RADIUS} (an ocean spawn still
     * finds the nearest shore). Blue columns stay within 24 blocks of the spawn and red ones at
     * least 40 blocks away from it, so the two bases never share a column.
     */
    public static List<BaseColumn> searchColumns(BaseColumn center) {
        List<BaseColumn> columns = new ArrayList<>();
        columns.add(center);
        for (int r = BASE_SEARCH_STEP; r <= BASE_SEARCH_RADIUS; r += BASE_SEARCH_STEP) {
            for (int dx = -r; dx <= r; dx += BASE_SEARCH_STEP) {
                columns.add(offset(center, dx, -r));
                columns.add(offset(center, dx, r));
            }
            for (int dz = -r + BASE_SEARCH_STEP; dz <= r - BASE_SEARCH_STEP;
                 dz += BASE_SEARCH_STEP) {
                columns.add(offset(center, -r, dz));
                columns.add(offset(center, r, dz));
            }
        }
        return List.copyOf(columns);
    }

    /** Every column tried for {@code side}, in order (all centres with their searches). */
    public static List<BaseColumn> baseColumns(Faction side, int spawnX, int spawnZ) {
        List<BaseColumn> columns = new ArrayList<>();
        for (BaseColumn center : baseCenters(side, spawnX, spawnZ)) {
            columns.addAll(searchColumns(center));
        }
        return List.copyOf(columns);
    }

    private static BaseColumn offset(BaseColumn center, int dx, int dz) {
        return new BaseColumn(center.x() + dx, center.z() + dz,
                center.label() + " 附近（偏移 " + dx + ", " + dz + "）");
    }

    /**
     * Initial facing of a provisioned main base: blue looks east towards the red base, red
     * looks west towards the blue one (Minecraft yaw: 270 = east, 90 = west).
     */
    public static float baseYaw(Faction side) {
        return side == Faction.RED ? 90.0F : 270.0F;
    }

    /**
     * Facing of a base whose centre lies {@code offsetX} blocks east of the spawn: a red base
     * that fell back west of the spawn ({@code offsetX < 0}) looks east towards the spawn instead
     * of away from it (审查修正); otherwise {@link #baseYaw(Faction)}.
     */
    public static float baseYaw(Faction side, int offsetX) {
        if (side == Faction.RED && offsetX < 0) {
            return 270.0F;
        }
        return baseYaw(side);
    }

    /** Whether column ({@code x}, {@code z}) lies inside a border spanning the given bounds. */
    public static boolean insideBorder(double minX, double minZ, double maxX, double maxZ,
                                       int x, int z) {
        return x + 1 > minX && x < maxX && z + 1 > minZ && z < maxZ;
    }

    // ---- helpers --------------------------------------------------------------------------------

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private static String find(List<String> ids, String wanted) {
        if (wanted == null || wanted.isEmpty()) {
            return null;
        }
        for (String id : ids) {
            if (id != null && wanted.equals(normalize(id))) {
                return id;
            }
        }
        return null;
    }
}
