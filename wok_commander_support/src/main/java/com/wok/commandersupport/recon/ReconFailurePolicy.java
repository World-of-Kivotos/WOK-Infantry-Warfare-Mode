package com.wok.commandersupport.recon;

import com.wok.infantry.support.adapter.SupportSpawnException;

import java.util.Objects;

/**
 * Maps a failed satellite scan onto the core mission outcome.
 *
 * <p>The first scan publishes contacts only as its very last action, so a failure during step 0
 * means nothing reached the map and the cooldown is returned. A defective integration still
 * trips the circuit breaker. From step 1 on contacts are already visible: ordinary failures keep
 * their own outcome and unchecked faults trip the circuit without a refund.</p>
 */
final class ReconFailurePolicy {
    static final String EXECUTION_FAULT_MESSAGE = "侦察卫星执行异常";

    private ReconFailurePolicy() {
    }

    /**
     * Pure classification of one step failure. Callers pass only exceptions and linkage errors;
     * any non-{@link SupportSpawnException} is treated as an integration fault.
     */
    static SupportSpawnException classify(int stepIndex, Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        if (stepIndex < 0) {
            throw new IllegalArgumentException("Recon step cannot be negative: " + stepIndex);
        }
        boolean firstScan = stepIndex == 0;
        if (failure instanceof SupportSpawnException spawnFailure) {
            if (!firstScan || spawnFailure.refundCooldown()) {
                return spawnFailure;
            }
            if (spawnFailure.providerBroken()) {
                return SupportSpawnException.providerBroken(spawnFailure.getMessage(),
                        spawnFailure, true);
            }
            return SupportSpawnException.notDelivered(spawnFailure.getMessage(), spawnFailure);
        }
        return SupportSpawnException.providerBroken(EXECUTION_FAULT_MESSAGE, failure, firstScan);
    }
}
