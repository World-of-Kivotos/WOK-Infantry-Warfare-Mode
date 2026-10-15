package com.wok.commandersupport.artillery;

import com.wok.infantry.support.adapter.SupportSpawnException;

import java.util.Objects;

/**
 * Mission outcomes of the howitzer barrages.
 *
 * <p>Step 0 only checks the explosion API, builds the fire plan and starts the first whistle: no
 * shell has landed, so any failure there returns the cooldown (a defective integration still
 * trips the circuit). From step 1 on rounds may already have landed: a single round outside the
 * world border is skipped without ending the mission, while an explosion API fault trips the
 * circuit without a refund.</p>
 */
final class ArtilleryFailures {
    static final String EXPLOSION_BROKEN_MESSAGE = "卓越前线爆炸接口不可用";
    static final String MARKER_BROKEN_MESSAGE = "炮击落点定位实体无法创建";
    static final String EXECUTION_FAULT_MESSAGE = "榴弹炮击执行异常";

    private ArtilleryFailures() {
    }

    /** The target fell outside the world border before the first round; nothing was fired. */
    static SupportSpawnException targetOutsideBorder() {
        return SupportSpawnException.notDelivered("目标点已超出世界边界");
    }

    /** Step 0 precheck: Superb Warfare's explosion API or particle type cannot be resolved. */
    static SupportSpawnException explosionUnavailable(Throwable cause) {
        return SupportSpawnException.providerBroken(EXPLOSION_BROKEN_MESSAGE, cause, true);
    }

    /** Step 0 precheck: the vanilla marker that locates each explosion cannot be created. */
    static SupportSpawnException markerUnavailable() {
        return SupportSpawnException.providerBroken(MARKER_BROKEN_MESSAGE, null, true);
    }

    /** The explosion API failed on a landing round; earlier rounds may already have landed. */
    static SupportSpawnException explosionFailed(Throwable cause) {
        return SupportSpawnException.providerBroken(EXPLOSION_BROKEN_MESSAGE, cause, false);
    }

    /**
     * Pure classification of one step failure. On step 0 every outcome returns the cooldown and a
     * provider-broken outcome keeps tripping the circuit; later steps keep a
     * {@link SupportSpawnException} unchanged and turn any other fault into a non-refunding
     * provider-broken outcome.
     */
    static SupportSpawnException classify(int stepIndex, Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        if (stepIndex < 0) {
            throw new IllegalArgumentException("Artillery step cannot be negative: " + stepIndex);
        }
        boolean beforeFirstRound = stepIndex == 0;
        if (failure instanceof SupportSpawnException spawnFailure) {
            if (!beforeFirstRound || spawnFailure.refundCooldown()) {
                return spawnFailure;
            }
            if (spawnFailure.providerBroken()) {
                return SupportSpawnException.providerBroken(spawnFailure.getMessage(),
                        spawnFailure, true);
            }
            return SupportSpawnException.notDelivered(spawnFailure.getMessage(), spawnFailure);
        }
        return SupportSpawnException.providerBroken(EXECUTION_FAULT_MESSAGE, failure,
                beforeFirstRound);
    }
}
