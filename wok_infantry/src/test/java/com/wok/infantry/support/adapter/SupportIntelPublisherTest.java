package com.wok.infantry.support.adapter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportIntelPublisherTest {
    private static final double CENTER_X = 100.0D;
    private static final double CENTER_Z = -50.0D;
    private static final double RADIUS = 150.0D;
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;
    private static final int TTL = 110;

    @Test
    void validBatchIsDeduplicatedInFirstOccurrenceOrder() throws SupportSpawnException {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        SupportIntelContact a = new SupportIntelContact(first, CENTER_X, 64.0D, CENTER_Z);
        SupportIntelContact b = new SupportIntelContact(second,
                CENTER_X + RADIUS, MIN_Y, CENTER_Z);
        SupportIntelContact duplicate = new SupportIntelContact(first,
                CENTER_X + 1.0D, 70.0D, CENTER_Z);

        List<SupportIntelContact> batch = validate(List.of(a, b, duplicate), TTL);

        assertEquals(List.of(a, b), batch);
        assertTrue(validate(List.of(), SupportIntelPublisher.MIN_TTL_TICKS).isEmpty());
        assertEquals(1, validate(List.of(a), SupportIntelPublisher.MAX_TTL_TICKS).size());
    }

    @Test
    void maximumContactCountIsAcceptedAndOneMoreIsAProviderDefect()
            throws SupportSpawnException {
        List<SupportIntelContact> contacts = new ArrayList<>();
        for (int index = 0; index < SupportIntelPublisher.MAX_CONTACTS; index++) {
            contacts.add(contact(CENTER_X, 64.0D, CENTER_Z));
        }
        assertEquals(SupportIntelPublisher.MAX_CONTACTS, validate(contacts, TTL).size());

        contacts.add(contact(CENTER_X, 64.0D, CENTER_Z));
        assertBroken(contacts, TTL);
        assertBroken(null, TTL);
    }

    @Test
    void ttlOutsideTheLeaseWindowIsAProviderDefect() {
        List<SupportIntelContact> contacts = List.of(contact(CENTER_X, 64.0D, CENTER_Z));

        assertBroken(contacts, SupportIntelPublisher.MIN_TTL_TICKS - 1);
        assertBroken(contacts, SupportIntelPublisher.MAX_TTL_TICKS + 1);
    }

    @Test
    void nullAndOutOfScanContactsAreProviderDefects() {
        assertBroken(Arrays.asList(contact(CENTER_X, 64.0D, CENTER_Z), null), TTL);
        assertBroken(List.of(contact(CENTER_X + RADIUS + 0.01D, 64.0D, CENTER_Z)), TTL);
        assertBroken(List.of(contact(CENTER_X, 64.0D, CENTER_Z - RADIUS - 0.01D)), TTL);
        assertBroken(List.of(contact(CENTER_X, MIN_Y - 0.01D, CENTER_Z)), TTL);
        assertBroken(List.of(contact(CENTER_X, MAX_Y, CENTER_Z)), TTL);
    }

    @Test
    void onlyTheRequestersCurrentSessionMayPublish() {
        Object session = new Object();
        Object preRespawn = new Object();

        // A commander waiting on the death screen is still the listed (live) instance.
        assertTrue(SupportIntelPublisher.isCurrentSession(session, session, false));
        // Offline: no listed instance, or the accepted entity already reported a disconnect.
        assertFalse(SupportIntelPublisher.isCurrentSession(session, null, false));
        assertFalse(SupportIntelPublisher.isCurrentSession(session, session, true));
        // The stale entity from before a respawn is no longer the listed instance.
        assertFalse(SupportIntelPublisher.isCurrentSession(preRespawn, session, false));
        assertFalse(SupportIntelPublisher.isCurrentSession(null, null, false));
    }

    private static List<SupportIntelContact> validate(List<SupportIntelContact> contacts,
                                                      int ttlTicks)
            throws SupportSpawnException {
        return SupportIntelPublisher.validatedBatch(contacts, ttlTicks,
                CENTER_X, CENTER_Z, RADIUS, MIN_Y, MAX_Y);
    }

    private static void assertBroken(List<SupportIntelContact> contacts, int ttlTicks) {
        SupportSpawnException failure = assertThrows(SupportSpawnException.class,
                () -> validate(contacts, ttlTicks));
        assertTrue(failure.providerBroken());
        assertFalse(failure.refundCooldown());
        assertFalse(failure.getMessage().isBlank());
    }

    private static SupportIntelContact contact(double x, double y, double z) {
        return new SupportIntelContact(UUID.randomUUID(), x, y, z);
    }
}
