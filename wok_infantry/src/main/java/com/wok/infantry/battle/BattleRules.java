package com.wok.infantry.battle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BattleRules {
    public static final int FACTION_CAPACITY = 40;
    public static final int SQUAD_CAPACITY = 8;
    public static final int ADMIN_PERMISSION_LEVEL = 2;
    /** A kick must remain authoritative long enough for the leader to manage the roster. */
    public static final long SQUAD_KICK_REJOIN_COOLDOWN_MILLIS = 60_000L;
    /** Reconnect grace period before an offline player releases faction/squad capacity. */
    public static final long RECONNECT_RESERVATION_MILLIS = 300_000L;
    /** Persisted online heartbeat; deliberately much shorter than the reconnect grace period. */
    public static final long PLAYER_HEARTBEAT_MILLIS = 30_000L;
    public static final long HISTORY_PRUNE_INTERVAL_MILLIS = 60_000L;
    /** Unassigned identity history is useful for reconnects, but must not grow without bound. */
    public static final long UNASSIGNED_HISTORY_TTL_MILLIS = 30L * 24L * 60L * 60L * 1_000L;
    public static final int MAX_PLAYER_RECORDS = 4_096;
    public static final int MAX_PLAYER_LOAD_SCAN = MAX_PLAYER_RECORDS * 4;
    public static final int MAX_PLAYER_NAME_LENGTH = 64;
    public static final int MAX_CLASS_ID_LENGTH = 64;
    public static final int MAX_FORMATION_ID_LENGTH = 64;
    public static final int MAX_CLASS_DISPLAY_NAME_LENGTH = 40;
    public static final int MAX_DIMENSION_ID_LENGTH = 128;

    public static final int MAX_MARKERS_PER_FACTION = 64;
    public static final int MAX_MARKERS_PER_CREATOR = 12;
    public static final int MAX_PERSISTED_MARKERS = MAX_MARKERS_PER_FACTION * 2;
    public static final int MAX_MARKER_LOAD_SCAN = MAX_PERSISTED_MARKERS * 4;
    /** At most one leader per active squad, and each faction holds at most 40 players. */
    public static final int MAX_PERSISTED_LEADERS = FACTION_CAPACITY * 2;
    public static final int MAX_LEADER_LOAD_SCAN = MAX_PERSISTED_LEADERS * 4;
    public static final int MAX_COMMANDER_LOAD_SCAN = 8;
    public static final int MAX_MARKERS_PER_RATE_WINDOW = 4;
    public static final long MARKER_RATE_WINDOW_MILLIS = 10_000L;
    public static final long MIN_MARKER_TTL_MILLIS = 5_000L;
    public static final long DEFAULT_MARKER_TTL_MILLIS = 180_000L;
    public static final long MAX_MARKER_TTL_MILLIS = 600_000L;
    public static final double MAX_COORDINATE = 29_999_984.0D;
    /** Two-click attack arrows shorter than this are treated as accidental clicks. */
    public static final double MIN_ATTACK_DIRECTION_LENGTH_BLOCKS = 4.0D;
    /** Prevents a single marker from spanning an unreasonable part of the world/map. */
    public static final double MAX_ATTACK_DIRECTION_LENGTH_BLOCKS = 4_096.0D;
    /** Length used when migrating version-1 markers that persisted only an angle. */
    public static final double LEGACY_ATTACK_DIRECTION_LENGTH_BLOCKS = 256.0D;

    public static final String DEFAULT_CLASS_ID = "assault";
    public static final Map<String, Integer> DEFAULT_CLASS_LIMITS;

    static {
        LinkedHashMap<String, Integer> limits = new LinkedHashMap<>();
        limits.put("assault", 8);
        limits.put("support", 2);
        limits.put("engineer", 2);
        limits.put("recon", 1);
        DEFAULT_CLASS_LIMITS = Collections.unmodifiableMap(limits);
    }

    private BattleRules() {
    }

    public static int defaultClassLimit(String classId) {
        if (classId == null) {
            return 0;
        }
        return DEFAULT_CLASS_LIMITS.getOrDefault(classId, SQUAD_CAPACITY);
    }
}
