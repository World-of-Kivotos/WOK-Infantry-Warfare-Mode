package com.wok.infantry.deployment;

import com.wok.infantry.battle.Faction;

import java.util.Objects;

/**
 * Outcome of {@link DeploymentService#provisionMainBase}: the side already had a main base, one
 * was created, or none could be placed ({@code message} says why).
 *
 * @param faction battle side
 * @param created a new main base was saved by this call
 * @param point   the side's main base afterwards, {@code null} when it still has none
 * @param message human-readable outcome (where it was placed, or why it failed)
 */
public record MainBaseProvision(Faction faction, boolean created, DeploymentPoint point,
                                String message) {
    public MainBaseProvision {
        Objects.requireNonNull(faction, "faction");
        message = Objects.requireNonNullElse(message, "");
    }

    /** Whether the side has a main base afterwards. */
    public boolean present() {
        return point != null;
    }
}
