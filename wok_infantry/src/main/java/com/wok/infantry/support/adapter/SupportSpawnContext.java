package com.wok.infantry.support.adapter;

import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTarget;
import com.wok.infantry.battle.Faction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Objects;
import java.util.UUID;

/** One server-authoritative logical mission step passed to a registered provider. */
public record SupportSpawnContext(
        ServerLevel level,
        Entity owner,
        UUID callId,
        SupportDefinition definition,
        SupportTarget target,
        int stepIndex,
        Faction faction
) {
    public SupportSpawnContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(callId, "callId");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(faction, "faction");
        if (stepIndex < 0 || stepIndex >= definition.stepCount()) {
            throw new IllegalArgumentException("Support step is outside the mission");
        }
        if (!level.dimension().location().equals(target.dimension())) {
            throw new IllegalArgumentException("Support execution level does not match target");
        }
    }
}
