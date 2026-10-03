package com.wok.bodyhealth.prone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProneMixinGuardTest {
    @Test
    void aFailureSwitchesOnlyItsOwnKeyOff() {
        String failing = "test.guard.failing";
        String healthy = "test.guard.healthy";
        assertFalse(ProneMixinGuard.off(failing));

        ProneMixinGuard.report(failing, new IllegalStateException("expected by the test"));
        ProneMixinGuard.report(failing, new IllegalStateException("second report is not logged"));

        assertTrue(ProneMixinGuard.off(failing));
        assertFalse(ProneMixinGuard.off(healthy));
    }
}
