package com.wok.infantry.battle;

import java.util.Objects;
import java.util.UUID;

/**
 * One squad member as seen by an allied viewer.
 *
 * @param state       server-classified roster state (protocol 19)
 * @param healthRatio trusted health in [0, 1], or {@link #UNKNOWN_HEALTH_RATIO} when the server
 *                    cannot vouch for one (for example WOK步战附属-部位血量 is installed without a
 *                    ratio API, or the member has no body in the battle). Clients must not draw a
 *                    health bar for an unknown ratio.
 */
public record MemberView(
        UUID playerId,
        String name,
        boolean online,
        boolean alive,
        float health,
        float maxHealth,
        boolean leader,
        boolean commander,
        SquadCallsign squad,
        String classId,
        MemberState state,
        float healthRatio
) {
    /** Wire and model value for "no trusted health ratio". */
    public static final float UNKNOWN_HEALTH_RATIO = -1.0F;

    public MemberView {
        Objects.requireNonNull(playerId, "playerId");
        name = Objects.requireNonNullElse(name, "");
        classId = Objects.requireNonNullElse(classId, BattleRules.DEFAULT_CLASS_ID);
        health = Math.max(0.0F, health);
        maxHealth = Math.max(1.0F, maxHealth);
        if (state == null) {
            state = MemberState.fromLegacy(online, alive);
        }
        healthRatio = normalizeHealthRatio(healthRatio);
    }

    /**
     * Compatibility constructor for callers written before protocol 19. The state is derived from
     * {@code online}/{@code alive} (dead and downed cannot be expressed) and the ratio from the
     * vanilla health pair.
     */
    public MemberView(UUID playerId, String name, boolean online, boolean alive,
                      float health, float maxHealth, boolean leader, boolean commander,
                      SquadCallsign squad, String classId) {
        this(playerId, name, online, alive, health, maxHealth, leader, commander, squad,
                classId, MemberState.fromLegacy(online, alive),
                legacyHealthRatio(MemberState.fromLegacy(online, alive), health, maxHealth));
    }

    public boolean hasHealthRatio() {
        return healthRatio >= 0.0F;
    }

    /** Clamps a reported ratio into [0, 1]; non-finite or negative values become unknown. */
    public static float normalizeHealthRatio(float ratio) {
        if (!Float.isFinite(ratio) || ratio < 0.0F) {
            return UNKNOWN_HEALTH_RATIO;
        }
        return Math.min(1.0F, ratio);
    }

    /** Vanilla health ratio, or unknown when the pair is unusable. */
    public static float vanillaHealthRatio(float health, float maxHealth) {
        if (!Float.isFinite(health) || !Float.isFinite(maxHealth) || maxHealth <= 0.0F) {
            return UNKNOWN_HEALTH_RATIO;
        }
        return normalizeHealthRatio(Math.max(0.0F, health) / maxHealth);
    }

    private static float legacyHealthRatio(MemberState state, float health, float maxHealth) {
        return state.hasVitals()
                ? vanillaHealthRatio(health, Math.max(1.0F, maxHealth))
                : UNKNOWN_HEALTH_RATIO;
    }
}
