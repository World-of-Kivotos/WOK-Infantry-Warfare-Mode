package com.wok.infantry.deployment;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Filtered deployment DTO for the local player. It deliberately excludes life and issue tokens.
 * The battle network may embed this in its own snapshot without exposing enemy bases.
 */
public record DeploymentView(
        DeploymentPhase phase,
        long revision,
        long serverGameTick,
        long eligibleGameTick,
        long nextResupplyGameTick,
        UUID selectedPointId,
        boolean canChangeClass,
        boolean canChangeSquad,
        boolean canDeploy,
        boolean canResupply,
        List<DeploymentPoint> points
) {
    public DeploymentView {
        Objects.requireNonNull(phase, "phase");
        if (revision < 0L || serverGameTick < 0L || eligibleGameTick < 0L
                || nextResupplyGameTick < 0L) {
            throw new IllegalArgumentException("Deployment ticks and revision cannot be negative");
        }
        points = List.copyOf(points == null ? List.of() : points);
    }

    public long waitingTicks() {
        return Math.max(0L, eligibleGameTick - serverGameTick);
    }

    public long resupplyTicks() {
        return Math.max(0L, nextResupplyGameTick - serverGameTick);
    }
}
