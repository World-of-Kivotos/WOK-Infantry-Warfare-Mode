package com.wok.infantry.network.battle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerRecipientDeliveryTest {
    @Test
    void oneFailingRecipientDoesNotStopTheRestOfTheBroadcast() {
        List<String> delivered = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        int failures = PerRecipientDelivery.deliverEach(List.of("alpha", "bravo", "charlie"),
                recipient -> {
                    if (recipient.equals("bravo")) {
                        throw new IllegalArgumentException(
                                "Support missions must be unique and reference an option");
                    }
                    delivered.add(recipient);
                },
                (recipient, failure) -> failed.add(recipient));

        assertEquals(1, failures);
        assertEquals(List.of("alpha", "charlie"), delivered);
        assertEquals(List.of("bravo"), failed);
    }

    @Test
    void aFailingReporterStillLetsLaterRecipientsReceiveTheirPacket() {
        List<String> delivered = new ArrayList<>();

        int failures = PerRecipientDelivery.deliverEach(List.of("alpha", "bravo"),
                recipient -> {
                    if (recipient.equals("alpha")) {
                        throw new IllegalStateException("encoder rejected the snapshot");
                    }
                    delivered.add(recipient);
                },
                (recipient, failure) -> {
                    throw new IllegalStateException("logger unavailable");
                });

        assertEquals(1, failures);
        assertEquals(List.of("bravo"), delivered);
    }

    @Test
    void eachFailureSignatureIsReportedOnlyOnce() {
        PerRecipientDelivery delivery = new PerRecipientDelivery();
        IllegalArgumentException crash = new IllegalArgumentException("mission without option");

        assertTrue(delivery.firstReport(crash));
        assertFalse(delivery.firstReport(new IllegalArgumentException("mission without option")),
                "the one-second heartbeat must not repeat the same stack trace");
        assertTrue(delivery.firstReport(new IllegalStateException("mission without option")));
        assertFalse(delivery.firstReport(null));
    }

    @Test
    void distinctSignaturesStopBeingReportedAfterTheCap() {
        PerRecipientDelivery delivery = new PerRecipientDelivery();
        for (int index = 0; index < PerRecipientDelivery.MAX_REPORTED_SIGNATURES; index++) {
            assertTrue(delivery.firstReport(new IllegalStateException("failure " + index)));
        }
        assertFalse(delivery.firstReport(new IllegalStateException("one more")));
    }

    @Test
    void signatureIsBounded() {
        String signature = PerRecipientDelivery.signature(
                new IllegalStateException("x".repeat(10_000)));
        assertTrue(signature.length() <= 240);
        assertTrue(signature.startsWith(IllegalStateException.class.getName()));
    }
}
