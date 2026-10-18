package com.wok.infantry.support;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/** Faction-filtered summary of a support call that is inbound or executing. */
public record SupportMissionView(
        UUID callId,
        ResourceLocation supportId,
        ResourceLocation dimension,
        double startX,
        double startZ,
        double endX,
        double endZ,
        long executeAtGameTick,
        int remainingSteps
) {
    public SupportMissionView {
        Objects.requireNonNull(callId, "callId");
        SupportDefinition.requireValidId(supportId);
        Objects.requireNonNull(dimension, "dimension");
        if (!Double.isFinite(startX) || !Double.isFinite(startZ)
                || !Double.isFinite(endX) || !Double.isFinite(endZ)) {
            throw new IllegalArgumentException("Support mission coordinates must be finite");
        }
        if (executeAtGameTick < 0L || remainingSteps < 0
                || remainingSteps > SupportDefinition.MAX_STEPS) {
            throw new IllegalArgumentException("Invalid support mission timing or step count");
        }
    }
}
