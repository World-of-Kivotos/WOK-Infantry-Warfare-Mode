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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void sneakyReflectiveFailuresAreTreatedAsBrokenProviders() {
        AtomicInteger executions = new AtomicInteger();
        SupportProvider provider = guarded(new DirectProvider(
                ProviderAvailability::present, () -> {
                    executions.incrementAndGet();
                    GuardedSupportProviderTest.<RuntimeException>sneakyThrow(
                            new NoSuchMethodException("setOrientation"));
                }));

        SupportSpawnException failure = assertThrows(SupportSpawnException.class,
                () -> provider.executeStep(null));
        assertTrue(failure.providerBroken());
        assertFalse(failure.refundCooldown());
        assertInstanceOf(NoSuchMethodException.class, failure.getCause());
        assertFalse(provider.availability().available());
        assertEquals(1, executions.get());
    }

    @Test
    void ordinaryMissionFailuresPassThroughWithoutTrippingTheCircuit() {
        assertPassesThrough(SupportSpawnException.endMission("目标已离开"));
        assertPassesThrough(SupportSpawnException.notDelivered("没有照射"));
        assertPassesThrough(new SupportSpawnException("旧构造器"));
    }

    @Test
    void providerBrokenFailureTripsAndKeepsItsRefundDecision() {
        for (boolean refund : new boolean[]{false, true}) {
            AtomicInteger executions = new AtomicInteger();
            SupportSpawnException declared = SupportSpawnException.providerBroken(
                    "临时情报目标超出支援扫描区域", null, refund);
            SupportProvider provider = guarded(new DirectProvider(
                    ProviderAvailability::present, () -> {
                        executions.incrementAndGet();
                        throw declared;
                    }));

            SupportSpawnException failure = assertThrows(SupportSpawnException.class,
                    () -> provider.executeStep(null));
            assertSame(declared, failure);
            assertTrue(failure.providerBroken());
            assertEquals(refund, failure.refundCooldown());

            ProviderAvailability tripped = provider.availability();
            assertFalse(tripped.available());
            assertTrue(tripped.reason().contains("临时情报目标超出支援扫描区域"));
            SupportSpawnException blocked = assertThrows(SupportSpawnException.class,
                    () -> provider.executeStep(null));
            assertFalse(blocked.providerBroken());
            assertFalse(blocked.refundCooldown());
            assertEquals(1, executions.get());
        }
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

    @Test
    void abandonReachesATrippedProviderAndOnlyLogsItsFailures() {
        AtomicInteger cleanups = new AtomicInteger();
        SupportProvider provider = guarded(new CleanupProvider(cleanups, null));
        assertThrows(SupportSpawnException.class, () -> provider.executeStep(null));
        assertFalse(provider.availability().available(), "the fixture trips the circuit");

        provider.abandon(null);
        assertEquals(1, cleanups.get(), "cleanup must still run after the circuit tripped");

        for (Throwable failure : new Throwable[]{new IllegalStateException("cleanup"),
                new NoClassDefFoundError("optional dependency")}) {
            AtomicInteger attempts = new AtomicInteger();
            SupportProvider failing = guarded(new CleanupProvider(attempts, failure));
            failing.abandon(null);
            assertEquals(1, attempts.get());
        }
    }

    @Test
    void acceptedIsForwardedOnceAndDefaultsToANoOp() {
        AtomicInteger cues = new AtomicInteger();
        SupportProvider provider = guarded(new CueProvider(cues, null,
                ProviderAvailability::present, () -> {
                }));

        provider.accepted(null);
        assertEquals(1, cues.get());

        SupportProvider silent = guarded(new DirectProvider(
                ProviderAvailability::present, () -> {
                }));
        silent.accepted(null);
        assertTrue(silent.availability().available());
    }

    @Test
    void failingAcceptanceCuesAreOnlyLoggedAndNeverTripTheCircuit()
            throws SupportSpawnException {
        for (Throwable failure : new Throwable[]{new IllegalStateException("cue"),
                new NoClassDefFoundError("optional sound"),
                new NoSuchFieldException("SOUND")}) {
            AtomicInteger cues = new AtomicInteger();
            AtomicInteger executions = new AtomicInteger();
            SupportProvider provider = guarded(new CueProvider(cues, failure,
                    ProviderAvailability::present, executions::incrementAndGet));

            provider.accepted(null);
            provider.accepted(null);

            assertEquals(2, cues.get(), "a failed cue must not disable later cues: " + failure);
            assertTrue(provider.availability().available(), failure.toString());
            provider.executeStep(null);
            assertEquals(1, executions.get(), "the mission must still execute: " + failure);
        }
    }

    @Test
    void trippedProvidersReceiveNoAcceptanceCue() {
        AtomicInteger brokenStepCues = new AtomicInteger();
        SupportProvider brokenStep = guarded(new CueProvider(brokenStepCues, null,
                ProviderAvailability::present, () -> {
                    throw SupportSpawnException.providerBroken("集成损坏", null, false);
                }));
        assertThrows(SupportSpawnException.class, () -> brokenStep.executeStep(null));
        assertFalse(brokenStep.availability().available());
        brokenStep.accepted(null);
        assertEquals(0, brokenStepCues.get());

        AtomicInteger brokenProbeCues = new AtomicInteger();
        SupportProvider brokenProbe = guarded(new CueProvider(brokenProbeCues, null, () -> {
            throw new NoClassDefFoundError("optional dependency");
        }, () -> {
        }));
        assertFalse(brokenProbe.availability().available());
        brokenProbe.accepted(null);
        assertEquals(0, brokenProbeCues.get());
    }

    @Test
    void virtualMachineErrorsFromAcceptanceCuesAreNeverSwallowed() {
        SupportProvider provider = guarded(new CueProvider(new AtomicInteger(),
                new SyntheticVmError("synthetic"), ProviderAvailability::present, () -> {
                }));
        assertThrows(SyntheticVmError.class, () -> provider.accepted(null));
    }

    private static void assertPassesThrough(SupportSpawnException declared) {
        AtomicInteger executions = new AtomicInteger();
        SupportProvider provider = guarded(new DirectProvider(
                ProviderAvailability::present, () -> {
                    executions.incrementAndGet();
                    throw declared;
                }));

        assertSame(declared, assertThrows(SupportSpawnException.class,
                () -> provider.executeStep(null)));
        assertTrue(provider.availability().available());
        assertSame(declared, assertThrows(SupportSpawnException.class,
                () -> provider.executeStep(null)));
        assertEquals(2, executions.get());
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
        assertTrue(safeFailure.providerBroken());
        assertFalse(safeFailure.refundCooldown());
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable failure) throws T {
        throw (T) failure;
    }

    @FunctionalInterface
    private interface AvailabilityAction {
        ProviderAvailability run();
    }

    @FunctionalInterface
    private interface SpawnAction {
        void run() throws SupportSpawnException;
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
        public void executeStep(SupportSpawnContext context) throws SupportSpawnException {
            spawnAction.run();
        }
    }

    /** Counts acceptance cues and optionally fails them with any throwable. */
    private record CueProvider(AtomicInteger cues, Throwable cueFailure,
                               AvailabilityAction availabilityAction,
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
        public void accepted(SupportSpawnContext context) {
            cues.incrementAndGet();
            if (cueFailure != null) {
                GuardedSupportProviderTest.<RuntimeException>sneakyThrow(cueFailure);
            }
        }

        @Override
        public void executeStep(SupportSpawnContext context) throws SupportSpawnException {
            spawnAction.run();
        }
    }

    /** Trips on every execution; counts cleanups and optionally fails them. */
    private record CleanupProvider(AtomicInteger cleanups, Throwable cleanupFailure)
            implements SupportProvider {
        @Override
        public ResourceLocation supportId() {
            return ID;
        }

        @Override
        public ProviderAvailability availability() {
            return ProviderAvailability.present();
        }

        @Override
        public void executeStep(SupportSpawnContext context) {
            throw new IllegalStateException("broken integration");
        }

        @Override
        public void abandon(SupportSpawnContext context) {
            cleanups.incrementAndGet();
            if (cleanupFailure instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cleanupFailure instanceof LinkageError linkage) {
                throw linkage;
            }
        }
    }

    private static final class SyntheticVmError extends VirtualMachineError {
        private SyntheticVmError(String message) {
            super(message);
        }
    }
}
