package com.wok.bodyhealth.prone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Level hook may only widen a consumer's broad phase while that consumer's segmented test can
 * run; otherwise the gun mod tests the added target with its own box (spec 13).
 */
final class ProneBroadPhaseGateTest {
    private static final String STATUS_PREFIX = "wok.bodyhealth.mixin.";

    // All test classes share one JVM: start from and leave behind the state of a fresh server.
    @BeforeEach
    @AfterEach
    void resetGlobalState() {
        for (ProneConsumer c : ProneConsumer.values()) {
            System.clearProperty(STATUS_PREFIX + c.headKey());
        }
        ProneHitService.resetFailuresForTests();
        ProneMixinGuard.resetForTests();
    }

    @Test
    void headKeysAreTheOnesTheMixinsUse() {
        assertEquals("tacz.head", ProneConsumer.TACZ.headKey());
        assertEquals("sbw.head", ProneConsumer.SBW.headKey());
    }

    @Test
    void wideningStopsWheneverTheSegmentedTestFallsBack() {
        // Hook not applied.
        assertFalse(ProneHitService.segmentedTestLive(ProneConsumer.TACZ));
        assertFalse(ProneHitService.segmentedTestLive(ProneConsumer.SBW));
        System.setProperty(STATUS_PREFIX + "tacz.head", "true");
        System.setProperty(STATUS_PREFIX + "sbw.head", "true");
        assertTrue(ProneHitService.segmentedTestLive(ProneConsumer.TACZ));
        assertTrue(ProneHitService.segmentedTestLive(ProneConsumer.SBW));

        // Hook switched off by the mixin guard.
        ProneMixinGuard.report("sbw.head", new IllegalStateException("expected by the test"));
        assertFalse(ProneHitService.segmentedTestLive(ProneConsumer.SBW));
        assertTrue(ProneHitService.segmentedTestLive(ProneConsumer.TACZ));

        // Consumer switched off on its twentieth failure.
        for (int i = 1; i < 20; i++) {
            ProneHitService.reportFailure(ProneConsumer.TACZ, new IllegalStateException("expected by the test"));
        }
        assertTrue(ProneHitService.segmentedTestLive(ProneConsumer.TACZ));
        ProneHitService.reportFailure(ProneConsumer.TACZ, new IllegalStateException("expected by the test"));
        assertFalse(ProneHitService.segmentedTestLive(ProneConsumer.TACZ));
    }
}
