package com.wok.infantry.support.adapter;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractSoftSupportProviderTest {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("test", "soft");

    @Test
    void ordinaryMissionFailuresPassThroughWithoutTrippingTheCircuit()
            throws SupportSpawnException {
        for (SupportSpawnException declared : new SupportSpawnException[]{
                SupportSpawnException.endMission("目标已离开"),
                SupportSpawnException.notDelivered("没有照射"),
                new SupportSpawnException("旧构造器")}) {
            TestProvider provider = new TestProvider();

            assertSame(declared, assertThrows(SupportSpawnException.class,
                    () -> provider.runStep(() -> {
                        throw declared;
                    })));
            assertTrue(provider.availability().available());
            provider.runStep(() -> {
            });
        }
    }

    @Test
    void providerBrokenFailureTripsAndKeepsItsRefundDecision() {
        for (boolean refund : new boolean[]{false, true}) {
            TestProvider provider = new TestProvider();
            SupportSpawnException declared = SupportSpawnException.providerBroken(
                    "适配接口返回了非法弹体", null, refund);

            SupportSpawnException failure = assertThrows(SupportSpawnException.class,
                    () -> provider.runStep(() -> {
                        throw declared;
                    }));

            assertSame(declared, failure);
            assertEquals(refund, failure.refundCooldown());
            ProviderAvailability tripped = provider.availability();
            assertFalse(tripped.available());
            assertTrue(tripped.reason().contains("适配接口返回了非法弹体"));
        }
    }

    @Test
    void reflectiveRuntimeAndLinkageFaultsBecomeNonRefundingBrokenFailures() {
        assertFaultTrips(() -> {
            throw new NoSuchMethodException("setOrientation");
        }, NoSuchMethodException.class);
        assertFaultTrips(() -> {
            throw new IllegalStateException("shell could not be spawned");
        }, IllegalStateException.class);
        assertFaultTrips(() -> {
            throw new NoClassDefFoundError("optional dependency");
        }, NoClassDefFoundError.class);
    }

    @Test
    void mismatchedContextOnlyEndsTheMission() {
        TestProvider provider = new TestProvider();

        SupportSpawnException failure = assertThrows(SupportSpawnException.class,
                () -> provider.executeStep(null));

        assertFalse(failure.providerBroken());
        assertFalse(failure.refundCooldown());
        assertTrue(provider.availability().available());
        assertEquals(0, provider.executions.get());
    }

    @Test
    void virtualMachineErrorsAreNeverSwallowed() {
        TestProvider provider = new TestProvider();

        assertThrows(SyntheticVmError.class, () -> provider.runStep(() -> {
            throw new SyntheticVmError("synthetic");
        }));
        assertTrue(provider.availability().available());
    }

    private static void assertFaultTrips(AbstractSoftSupportProvider.StepAction action,
                                         Class<? extends Throwable> expectedCause) {
        TestProvider provider = new TestProvider();

        SupportSpawnException failure = assertThrows(SupportSpawnException.class,
                () -> provider.runStep(action));

        assertTrue(failure.providerBroken());
        assertFalse(failure.refundCooldown());
        assertInstanceOf(expectedCause, failure.getCause());
        assertFalse(provider.availability().available());
        assertFalse(provider.availability().reason().isBlank());
    }

    private static final class TestProvider extends AbstractSoftSupportProvider {
        private final AtomicInteger executions = new AtomicInteger();

        private TestProvider() {
            super(ID);
        }

        @Override
        protected ProviderAvailability probeAvailability() {
            return ProviderAvailability.present();
        }

        @Override
        protected void doExecuteStep(SupportSpawnContext context) {
            executions.incrementAndGet();
        }
    }

    private static final class SyntheticVmError extends VirtualMachineError {
        private SyntheticVmError(String message) {
            super(message);
        }
    }
}
