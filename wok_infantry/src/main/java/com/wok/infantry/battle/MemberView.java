package com.wok.infantry.battle;

import java.util.Objects;
import java.util.UUID;

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
        String classId
) {
    public MemberView {
        Objects.requireNonNull(playerId, "playerId");
        name = Objects.requireNonNullElse(name, "");
        classId = Objects.requireNonNullElse(classId, BattleRules.DEFAULT_CLASS_ID);
        health = Math.max(0.0F, health);
        maxHealth = Math.max(1.0F, maxHealth);
    }
}
