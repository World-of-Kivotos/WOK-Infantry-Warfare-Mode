package com.wok.infantry.battle;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public record MemberPosition(
        UUID playerId,
        ResourceLocation dimension,
        double x,
        double y,
        double z,
        float yaw
) {
    public MemberPosition {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(dimension, "dimension");
    }
}
