package com.wok.infantry.formation.vehicle;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperbWarfareVehicleGateTest {
    private static final ResourceLocation T90 =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "t_90a");
    private static final ResourceLocation BRADLEY =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "bradley");
    private static final ResourceLocation FCP_DRAGOON =
            ResourceLocation.fromNamespaceAndPath("fcp", "stryker_dragoon");

    @Test
    void unloadedModFailsBeforeTouchingRegistryResolver() {
        AtomicInteger resolutions = new AtomicInteger();

        SuperbWarfareVehicleGate.Resolution<String> result =
                SuperbWarfareVehicleGate.resolve(false, List.of(T90), id -> {
                    resolutions.incrementAndGet();
                    return "unexpected";
                });

        assertFalse(result.result().success());
        assertTrue(result.result().message().contains("未加载"));
        assertEquals(0, resolutions.get());
        assertTrue(result.values().isEmpty());
    }

    @Test
    void nonSuperbWarfareNamespaceIsRejectedWithoutFallback() {
        ResourceLocation pig = ResourceLocation.fromNamespaceAndPath("minecraft", "pig");
        AtomicInteger resolutions = new AtomicInteger();

        SuperbWarfareVehicleGate.Resolution<String> result =
                SuperbWarfareVehicleGate.resolve(true, List.of(T90, pig), id -> {
                    resolutions.incrementAndGet();
                    return "pig";
                });

        assertFalse(result.result().success());
        assertTrue(result.result().message().contains("已支持的卓越前线生态 namespace"));
        assertEquals(0, resolutions.get());
        assertTrue(result.values().isEmpty());
    }

    @Test
    void fcpVehicleNamespaceIsPartOfTheSupportedVehicleEcosystem() {
        SuperbWarfareVehicleGate.Resolution<String> result =
                SuperbWarfareVehicleGate.resolve(true, List.of(FCP_DRAGOON),
                        id -> "type:" + id);

        assertTrue(result.result().success());
        assertEquals("type:" + FCP_DRAGOON, result.values().get(FCP_DRAGOON));
    }

    @Test
    void missingEntityTypeFailsWholeResolutionWithoutSubstitute() {
        SuperbWarfareVehicleGate.Resolution<String> result =
                SuperbWarfareVehicleGate.resolve(true, List.of(T90, BRADLEY), id ->
                        id.equals(T90) ? "resolved-t90" : null);

        assertFalse(result.result().success());
        assertTrue(result.result().message().contains(BRADLEY.toString()));
        assertTrue(result.values().isEmpty());
    }

    @Test
    void resolvesUniqueIdsOnceAndReturnsImmutableCompleteMap() {
        AtomicInteger resolutions = new AtomicInteger();

        SuperbWarfareVehicleGate.Resolution<String> result =
                SuperbWarfareVehicleGate.resolve(true, List.of(T90, BRADLEY, T90), id -> {
                    resolutions.incrementAndGet();
                    return "type:" + id;
                });

        assertTrue(result.result().success());
        assertEquals(2, resolutions.get());
        assertEquals("type:" + T90, result.values().get(T90));
        assertEquals("type:" + BRADLEY, result.values().get(BRADLEY));
    }

    @Test
    void emptyBatchDoesNotRequireOptionalMod() {
        SuperbWarfareVehicleGate.Resolution<String> result =
                SuperbWarfareVehicleGate.resolve(false, List.of(), id -> null);

        assertTrue(result.result().success());
        assertTrue(result.values().isEmpty());
    }
}
