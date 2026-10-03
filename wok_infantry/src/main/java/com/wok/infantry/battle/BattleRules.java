package com.wok.infantry.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    /**
     * 快照里卫星红点保底的显示名额；手工标记同样保底剩下的
     * {@code MAX_MARKERS_PER_FACTION - RESERVED_SUPPORT_INTEL_SNAPSHOT_SLOTS} 个。
     * 任一方用不完的名额让给另一方，合计仍不超过 {@link #MAX_MARKERS_PER_FACTION}。
     */
    public static final int RESERVED_SUPPORT_INTEL_SNAPSHOT_SLOTS = 32;
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

    /**
     * 按快照名额合并一个阵营的卫星红点与手工标记：先给红点保底
     * {@link #RESERVED_SUPPORT_INTEL_SNAPSHOT_SLOTS} 个，手工标记取剩余名额，
     * 红点再用掉手工标记没用完的部分。红点按传入顺序取前面的；手工标记按创建时间升序传入，
     * 名额不够时保留最新的（列表尾部），刚放下的标记不会因旧标记占满名额而看不见。
     * 结果红点在前、手工标记在后，各自保持传入顺序，总数不超过 {@link #MAX_MARKERS_PER_FACTION}。
     */
    public static <T> List<T> mergeSnapshotMarkers(List<? extends T> supportIntel,
                                                   List<? extends T> manualMarkers) {
        Objects.requireNonNull(supportIntel, "supportIntel");
        Objects.requireNonNull(manualMarkers, "manualMarkers");
        int reservedForIntel = Math.min(supportIntel.size(),
                RESERVED_SUPPORT_INTEL_SNAPSHOT_SLOTS);
        int manualShown = Math.min(manualMarkers.size(),
                MAX_MARKERS_PER_FACTION - reservedForIntel);
        int intelShown = Math.min(supportIntel.size(), MAX_MARKERS_PER_FACTION - manualShown);
        List<T> merged = new ArrayList<>(intelShown + manualShown);
        merged.addAll(supportIntel.subList(0, intelShown));
        merged.addAll(manualMarkers.subList(manualMarkers.size() - manualShown,
                manualMarkers.size()));
        return merged;
    }
}
