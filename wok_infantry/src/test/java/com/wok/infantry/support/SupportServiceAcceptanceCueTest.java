package com.wok.infantry.support;

import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportProvider;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportServiceAcceptanceCueTest {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("test", "acceptance_cue");
    private static final UUID CALL_ID = new UUID(0x1234L, 0x5678L);

    @Test
    void theProviderHearsAboutAnAcceptedMissionExactlyOnce() {
        AtomicInteger cues = new AtomicInteger();
        AtomicInteger contexts = new AtomicInteger();

        assertTrue(SupportService.announceAccepted(new CueProvider(cues, null), ID, CALL_ID,
                () -> {
                    contexts.incrementAndGet();
                    return null;
                }));

        assertEquals(1, cues.get());
        assertEquals(1, contexts.get(), "the context is built once, for the cue only");
    }

    @Test
    void directProviderFailuresNeverEscapeTheAcceptance() {
        for (Throwable failure : new Throwable[]{new IllegalStateException("cue"),
                new NoClassDefFoundError("optional sound"),
                new NoSuchFieldException("SOUND")}) {
            AtomicInteger cues = new AtomicInteger();
            assertFalse(SupportService.announceAccepted(new CueProvider(cues, failure), ID,
                    CALL_ID, () -> null), failure.toString());
            assertEquals(1, cues.get());
        }
    }

    @Test
    void aContextThatCannotBeBuiltSkipsTheCueWithoutFailing() {
        AtomicInteger cues = new AtomicInteger();
        assertFalse(SupportService.announceAccepted(new CueProvider(cues, null), ID, CALL_ID,
                () -> {
                    throw new IllegalArgumentException(
                            "Support execution level does not match target");
                }));
        assertEquals(0, cues.get());

        assertFalse(SupportService.announceAccepted(null, ID, CALL_ID, () -> null));
        assertFalse(SupportService.announceAccepted(new CueProvider(cues, null), ID, CALL_ID,
                null));
        assertEquals(0, cues.get());
    }

    @Test
    void registeredProvidersSwallowCueFailuresInsideTheirGuardAndStayAvailable() {
        AtomicInteger cues = new AtomicInteger();
        SupportProvider registered = SupportRegistry.builder()
                .register(new SupportDefinition(ID, "support.test.acceptance_cue", "Cue", "Cue",
                        SupportTargetMode.POINT, 0L, 0L, 1, 1, 0.0D),
                        new CueProvider(cues, new IllegalStateException("cue")))
                .freeze().provider(ID).orElseThrow();

        assertTrue(SupportService.announceAccepted(registered, ID, CALL_ID, () -> null));
        assertEquals(1, cues.get());
        assertTrue(registered.availability().available(),
                "a failing cue must not trip the circuit");
    }

    @Test
    void virtualMachineErrorsAreNeverSwallowed() {
        assertThrows(SyntheticVmError.class, () -> SupportService.announceAccepted(
                new CueProvider(new AtomicInteger(), new SyntheticVmError("synthetic")),
                ID, CALL_ID, () -> null));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable failure) throws T {
        throw (T) failure;
    }

    /** Direct, unguarded provider that counts cues and optionally fails them. */
    private record CueProvider(AtomicInteger cues, Throwable cueFailure)
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
        public void accepted(SupportSpawnContext context) {
            cues.incrementAndGet();
            if (cueFailure != null) {
                SupportServiceAcceptanceCueTest.<RuntimeException>sneakyThrow(cueFailure);
            }
        }

        @Override
        public void executeStep(SupportSpawnContext context) throws SupportSpawnException {
        }
    }

    private static final class SyntheticVmError extends VirtualMachineError {
        private SyntheticVmError(String message) {
            super(message);
        }
    }
}
