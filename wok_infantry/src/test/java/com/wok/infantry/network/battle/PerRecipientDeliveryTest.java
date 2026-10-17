package com.wok.infantry.network.battle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerRecipientDeliveryTest {
    private static final long WINDOW = PerRecipientDelivery.REPORT_WINDOW_MILLIS;

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
    void eachFailureSignatureIsReportedOnlyOnceInsideTheWindow() {
        PerRecipientDelivery delivery = new PerRecipientDelivery();
        IllegalArgumentException crash = new IllegalArgumentException("mission without option");

        assertTrue(delivery.firstReport(crash));
        assertFalse(delivery.firstReport(new IllegalArgumentException("mission without option")),
                "the one-second heartbeat must not repeat the same stack trace");
        assertTrue(delivery.firstReport(new IllegalStateException("mission without option")));
        assertFalse(delivery.firstReport(null));
    }

    @Test
    void numbersAndUuidsInTheMessageDoNotMakeANewSignature() {
        PerRecipientDelivery delivery = new PerRecipientDelivery();

        assertTrue(delivery.shouldReport(new IllegalArgumentException(
                "Invalid member health range: 13.5/20.0"), 0L));
        for (int second = 1; second <= 120; second++) {
            assertFalse(delivery.shouldReport(new IllegalArgumentException(
                    "Invalid member health range: " + (second % 20) + ".25/20.0"),
                    second * 1000L), "NET-4: a changing health value is still the same fault");
        }
        assertEquals(PerRecipientDelivery.signature(new IllegalStateException(
                        "Unknown player " + UUID.randomUUID() + " in squad 3")),
                PerRecipientDelivery.signature(new IllegalStateException(
                        "Unknown player " + UUID.randomUUID() + " in squad 12")));
        assertNotEquals(PerRecipientDelivery.signature(new IllegalStateException("member state")),
                PerRecipientDelivery.signature(new IllegalStateException("support option")));
    }

    @Test
    void aRepeatingFaultIsReportedAgainAfterTheWindow() {
        PerRecipientDelivery delivery = new PerRecipientDelivery();
        IllegalStateException crash = new IllegalStateException("snapshot invariant");

        assertTrue(delivery.shouldReport(crash, 1_000L));
        assertFalse(delivery.shouldReport(crash, 1_000L + WINDOW - 1L));
        assertTrue(delivery.shouldReport(crash, 1_000L + WINDOW),
                "a fault that keeps happening shows up again in the default log");
        assertFalse(delivery.shouldReport(crash, 1_000L + WINDOW + 1L));
    }

    @Test
    void aNewFaultIsNotSilencedForeverOnceManySignaturesWereSeen() {
        PerRecipientDelivery delivery = new PerRecipientDelivery();
        for (int index = 0; index < PerRecipientDelivery.MAX_REPORTED_SIGNATURES; index++) {
            assertTrue(delivery.shouldReport(new IllegalStateException("failure " + "x".repeat(index)),
                    0L));
        }

        assertTrue(delivery.shouldReport(new IllegalStateException("support invariant"), 10L),
                "a burst of distinct faults still logs one more line through the overflow slot");
        assertFalse(delivery.shouldReport(new IllegalStateException("another new fault"), 20L),
                "the overflow slot is limited to one line per window");
        assertTrue(delivery.shouldReport(new IllegalStateException("support invariant"),
                WINDOW + 30L), "after the window the tracked faults expire and the new one logs");
    }

    @Test
    void signatureIsBounded() {
        String signature = PerRecipientDelivery.signature(
                new IllegalStateException("x".repeat(10_000)));
        assertTrue(signature.length() <= 240);
        assertTrue(signature.startsWith(IllegalStateException.class.getName()));
    }
}
