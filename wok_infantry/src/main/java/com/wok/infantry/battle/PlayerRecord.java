package com.wok.infantry.battle;

import java.util.Objects;
import java.util.UUID;

/** Immutable public view of a persisted battle player record. */
public record PlayerRecord(
        UUID playerId,
        String lastKnownName,
        Faction faction,
        String formationId,
        SquadCallsign squad,
        String assignedClassId,
        long firstJoinedAtMillis,
        long lastSeenAtMillis
) {
    public PlayerRecord {
        Objects.requireNonNull(playerId, "playerId");
        lastKnownName = Objects.requireNonNullElse(lastKnownName, "");
        formationId = normalizeFormationId(formationId);
        assignedClassId = normalizeClassId(assignedClassId);
    }

    private static String normalizeFormationId(String formationId) {
        if (formationId == null || formationId.isBlank()) {
            return "";
        }
        return formationId.trim();
    }

    private static String normalizeClassId(String classId) {
        if (classId == null || classId.isBlank()) {
            return BattleRules.DEFAULT_CLASS_ID;
        }
        return classId.trim();
    }
}
