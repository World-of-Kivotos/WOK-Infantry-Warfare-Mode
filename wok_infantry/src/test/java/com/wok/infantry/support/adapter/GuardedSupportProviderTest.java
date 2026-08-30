package com.wok.infantry.support.adapter;

import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportRegistry;
import com.wok.infantry.support.SupportTargetMode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuardedSupportProviderTest {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("test", "guarded");

    @Test
    void nullRuntimeAndLinkageAvailabilityFailuresTripIndependentCircuits() {
        assertAvailabilityTrips(() -> null);
        assertAvailabilityTrips(() -> {
            throw new IllegalStateException("runtime");
        });
        assertAvailabilityTrips(() -> {
            throw new NoClassDefFoundError("optional dependency");
        });
    }

    @Test
    void runtimeAndLinkageSpawnFailuresBecomeCheckedFailuresAndStayTripped() {
        assertSpawnTrips(new IllegalStateException("runtime"), IllegalStateException.class);
        assertSpawnTrips(new NoClassDefFoundError("optional dependency"),
                NoClassDefFoundError.class);
    }

    @Test
    void threadDeathAndVirtualMachineErrorsAreNeverSwallowed() {
        SupportProvider availabilityFailure = guarded(new DirectProvider(
                () -> {
                    throw new ThreadDeath();
                }, () -> {
                }));
        assertThrows(ThreadDeath.class, availabilityFailure::availability);

        SupportProvider spawnFailure = guarded(new DirectProvider(
                ProviderAvailability::present,
                () -> {
                    throw new SyntheticVmError("synthetic");
                }));
        assertThrows(SyntheticVmError.class, () -> spawnFailure.executeStep(null));
    }

    private static void assertAvailabilityTrips(AvailabilityAction action) {
        AtomicInteger probes = new AtomicInteger();
        SupportProvider provider = guarded(new DirectProvider(() -> {
            probes.incrementAndGet();
            return action.run();
        }, () -> {
        }));

        assertFalse(provider.availability().available());
        assertFalse(provider.availability().available());
        assertEquals(1, probes.get());
    }

    private static void assertSpawnTrips(Throwable failure,
                                         Class<? extends Throwable> expectedCause) {
        AtomicInteger executions = new AtomicInteger();
        SupportProvider provider = guarded(new DirectProvider(
                ProviderAvailability::present, () -> {
                    executions.incrementAndGet();
                    if (failure instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    if (failure instanceof LinkageError linkage) {
                        throw linkage;
                    }
                    throw new AssertionError("unsupported fixture", failure);
                }));

        SupportSpawnException safeFailure = assertThrows(SupportSpawnException.class,
                () -> provider.executeStep(null));
        assertInstanceOf(expectedCause, safeFailure.getCause());
        assertFalse(provider.availability().available());
        assertThrows(SupportSpawnException.class, () -> provider.executeStep(null));
        assertEquals(1, executions.get());
    }

    private static SupportProvider guarded(SupportProvider provider) {
        SupportDefinition definition = new SupportDefinition(ID,
                "support.test.guarded", "Guarded", "Guarded", SupportTargetMode.POINT,
                0L, 0L, 1, 1, 0.0D);
        return SupportRegistry.builder().register(definition, provider).freeze()
                .provider(ID).orElseThrow();
    }

    @FunctionalInterface
    private interface AvailabilityAction {
        ProviderAvailability run();
    }

    @FunctionalInterface
    private interface SpawnAction {
        void run();
    }

    private record DirectProvider(AvailabilityAction availabilityAction,
                                  SpawnAction spawnAction) implements SupportProvider {
        @Override
        public ResourceLocation supportId() {
            return ID;
        }

        @Override
        public ProviderAvailability availability() {
            return availabilityAction.run();
        }

        @Override
        public void executeStep(SupportSpawnContext context) {
            spawnAction.run();
        }
    }

    private static final class SyntheticVmError extends VirtualMachineError {
        private SyntheticVmError(String message) {
            super(message);
        }
    }
}
