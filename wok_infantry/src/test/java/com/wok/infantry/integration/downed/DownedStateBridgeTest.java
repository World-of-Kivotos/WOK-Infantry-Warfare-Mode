package com.wok.infantry.integration.downed;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownedStateBridgeTest {
    @Test
    void missingModuleIsNeverDownedAndNeverProbed() {
        AtomicInteger resolves = new AtomicInteger();
        AtomicInteger fallbacks = new AtomicInteger();
        List<String> logs = new ArrayList<>();
        DownedStateBridge.Probe<String> probe = new DownedStateBridge.Probe<>(
                () -> false,
                () -> {
                    resolves.incrementAndGet();
                    return subject -> true;
                },
                subject -> {
                    fallbacks.incrementAndGet();
                    return true;
                },
                (message, error) -> logs.add(message));

        assertFalse(probe.test("casualty"));
        assertFalse(probe.test("casualty"));
        assertEquals(0, resolves.get());
        assertEquals(0, fallbacks.get());
        assertEquals(List.of(), logs);
    }

    @Test
    void installedModuleApiIsResolvedOnceAndAnswersDirectly() {
        AtomicInteger resolves = new AtomicInteger();
        List<String> logs = new ArrayList<>();
        DownedStateBridge.Probe<String> probe = new DownedStateBridge.Probe<>(
                () -> true,
                () -> {
                    resolves.incrementAndGet();
                    return "downed"::equals;
                },
                subject -> {
                    throw new AssertionError("fallback must not run while the API works");
                },
                (message, error) -> logs.add(message));

        assertTrue(probe.test("downed"));
        assertFalse(probe.test("standing"));
        assertTrue(probe.test("downed"));
        assertEquals(1, resolves.get());
        assertEquals(List.of(), logs);
    }

    @Test
    void missingApiFallsBackToEffectProbeAndLogsOnce() {
        List<String> logs = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        DownedStateBridge.Probe<String> probe = new DownedStateBridge.Probe<>(
                () -> true,
                () -> null,
                "downed"::equals,
                (message, error) -> {
                    logs.add(message);
                    errors.add(error);
                });

        assertTrue(probe.test("downed"));
        assertFalse(probe.test("standing"));
        assertEquals(1, logs.size());
        assertNull(errors.get(0), "a missing optional API is informational");
    }

    @Test
    void failingApiSwitchesToFallbackPermanentlyAndLogsOnce() {
        AtomicInteger apiCalls = new AtomicInteger();
        List<Throwable> errors = new ArrayList<>();
        Predicate<String> brokenApi = subject -> {
            apiCalls.incrementAndGet();
            throw new IllegalStateException("module changed");
        };
        DownedStateBridge.Probe<String> probe = new DownedStateBridge.Probe<>(
                () -> true, () -> brokenApi, "downed"::equals,
                (message, error) -> errors.add(error));

        assertTrue(probe.test("downed"));
        assertFalse(probe.test("standing"));
        assertTrue(probe.test("downed"));
        assertEquals(1, apiCalls.get(), "a failed API is not retried every heartbeat");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof IllegalStateException);
    }

    @Test
    void failingFallbackDisablesTheProbeWithoutASecondLog() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        List<String> logs = new ArrayList<>();
        DownedStateBridge.Probe<String> probe = new DownedStateBridge.Probe<>(
                () -> true,
                () -> null,
                subject -> {
                    fallbackCalls.incrementAndGet();
                    throw new IllegalStateException("registry unavailable");
                },
                (message, error) -> logs.add(message));

        assertFalse(probe.test("downed"));
        assertFalse(probe.test("downed"));
        assertEquals(1, fallbackCalls.get());
        assertEquals(1, logs.size(), "every bridge failure is logged at most once");
    }

    @Test
    void throwingInstallationCheckCountsAsNotInstalled() {
        DownedStateBridge.Probe<String> probe = new DownedStateBridge.Probe<>(
                () -> {
                    throw new IllegalStateException("mod list not ready");
                },
                () -> subject -> true,
                subject -> true,
                (message, error) -> {
                    throw new AssertionError("not installed is not a failure");
                });

        assertFalse(probe.test("downed"));
    }

    @Test
    void publicEntryPointToleratesNullPlayer() {
        assertFalse(DownedStateBridge.isDowned(null));
    }
}
