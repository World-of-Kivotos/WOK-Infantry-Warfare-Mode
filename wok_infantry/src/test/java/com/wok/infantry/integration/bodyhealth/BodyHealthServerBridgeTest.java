package com.wok.infantry.integration.bodyhealth;

import com.wok.infantry.battle.MemberView;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyHealthServerBridgeTest {
    private static final float UNKNOWN = MemberView.UNKNOWN_HEALTH_RATIO;

    @Test
    void withoutBodyHealthTheVanillaRatioIsUsed() {
        AtomicInteger resolves = new AtomicInteger();
        BodyHealthServerBridge.Source<Float> source = new BodyHealthServerBridge.Source<>(
                () -> false,
                () -> {
                    resolves.incrementAndGet();
                    return subject -> 0.1F;
                },
                subject -> subject,
                (message, error) -> {
                    throw new AssertionError("vanilla health needs no log");
                });

        assertEquals(0.4F, source.ratio(0.4F));
        assertEquals(1.0F, source.ratio(1.7F), "over-heal clamps to a full bar");
        assertEquals(UNKNOWN, source.ratio(Float.NaN));
        assertEquals(0, resolves.get());
    }

    @Test
    void bodyHealthWithoutRatioApiReportsUnknownAndLogsOnce() {
        List<String> logs = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        BodyHealthServerBridge.Source<Float> source = new BodyHealthServerBridge.Source<>(
                () -> true,
                () -> null,
                subject -> {
                    throw new AssertionError("vanilla health is meaningless under body health");
                },
                (message, error) -> {
                    logs.add(message);
                    errors.add(error);
                });

        assertEquals(UNKNOWN, source.ratio(0.9F));
        assertEquals(UNKNOWN, source.ratio(0.2F));
        assertEquals(1, logs.size());
        assertNull(errors.get(0));
    }

    @Test
    void bodyHealthRatioApiValuesAreNormalized() {
        Function<String, Object> api = subject -> switch (subject) {
            case "float" -> 0.4F;
            case "double" -> 0.25D;
            case "over" -> 3.0D;
            case "negative" -> -0.5F;
            case "nan" -> Double.NaN;
            default -> "not a number";
        };
        BodyHealthServerBridge.Source<String> source = new BodyHealthServerBridge.Source<>(
                () -> true, () -> api,
                subject -> {
                    throw new AssertionError("API answers take precedence over vanilla");
                },
                (message, error) -> {
                    throw new AssertionError("valid API calls need no log");
                });

        assertEquals(0.4F, source.ratio("float"));
        assertEquals(0.25F, source.ratio("double"));
        assertEquals(1.0F, source.ratio("over"));
        assertEquals(UNKNOWN, source.ratio("negative"));
        assertEquals(UNKNOWN, source.ratio("nan"));
        assertEquals(UNKNOWN, source.ratio("text"));
    }

    @Test
    void failingRatioApiIsDroppedAfterOneLoggedFailure() {
        AtomicInteger calls = new AtomicInteger();
        List<Throwable> errors = new ArrayList<>();
        BodyHealthServerBridge.Source<String> source = new BodyHealthServerBridge.Source<>(
                () -> true,
                () -> subject -> {
                    calls.incrementAndGet();
                    throw new IllegalStateException("capability missing");
                },
                subject -> 1.0F,
                (message, error) -> errors.add(error));

        assertEquals(UNKNOWN, source.ratio("a"));
        assertEquals(UNKNOWN, source.ratio("b"));
        assertEquals(1, calls.get());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof IllegalStateException);
    }

    @Test
    void normalizeAcceptsOnlyFiniteNumbers() {
        assertEquals(0.5F, BodyHealthServerBridge.normalize(0.5F));
        assertEquals(0.5F, BodyHealthServerBridge.normalize(0.5D));
        assertEquals(1.0F, BodyHealthServerBridge.normalize(1));
        assertEquals(0.0F, BodyHealthServerBridge.normalize(0));
        assertEquals(UNKNOWN, BodyHealthServerBridge.normalize(null));
        assertEquals(UNKNOWN, BodyHealthServerBridge.normalize("0.5"));
        assertEquals(UNKNOWN, BodyHealthServerBridge.normalize(Double.POSITIVE_INFINITY));
        assertEquals(UNKNOWN, BodyHealthServerBridge.normalize(-0.01D));
    }

    @Test
    void publicEntryPointToleratesNullEntity() {
        assertEquals(UNKNOWN, BodyHealthServerBridge.ratio(null));
    }
}
