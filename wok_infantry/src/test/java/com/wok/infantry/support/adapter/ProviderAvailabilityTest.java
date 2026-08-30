package com.wok.infantry.support.adapter;

import com.wok.infantry.support.SupportOptionView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderAvailabilityTest {
    @Test
    void providerReasonIsBoundedBeforeItCanReachSnapshotCodec() {
        ProviderAvailability unavailable = ProviderAvailability.unavailable("r".repeat(500));
        assertFalse(unavailable.available());
        assertEquals(SupportOptionView.MAX_AVAILABILITY_REASON_LENGTH,
                unavailable.reason().length());

        ProviderAvailability available = new ProviderAvailability(true, "ignored");
        assertTrue(available.available());
        assertEquals("", available.reason());
    }

    @Test
    void blankFailureReasonUsesReadableFailClosedFallback() {
        ProviderAvailability unavailable = ProviderAvailability.unavailable(null);

        assertFalse(unavailable.available());
        assertFalse(unavailable.reason().isBlank());
        assertTrue(unavailable.reason().length()
                <= SupportOptionView.MAX_AVAILABILITY_REASON_LENGTH);
    }
}
