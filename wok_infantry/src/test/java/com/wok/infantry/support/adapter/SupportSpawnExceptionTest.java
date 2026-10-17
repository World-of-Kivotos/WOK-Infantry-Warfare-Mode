package com.wok.infantry.support.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportSpawnExceptionTest {
    @Test
    void legacyConstructorsOnlyEndTheMission() {
        IllegalStateException cause = new IllegalStateException("cause");
        SupportSpawnException plain = new SupportSpawnException("任务结束");
        SupportSpawnException withCause = new SupportSpawnException("任务结束", cause);

        assertEquals("任务结束", plain.getMessage());
        assertFalse(plain.providerBroken());
        assertFalse(plain.refundCooldown());
        assertSame(cause, withCause.getCause());
        assertFalse(withCause.providerBroken());
        assertFalse(withCause.refundCooldown());
    }

    @Test
    void factoriesCarryTheirMissionAndCircuitFlags() {
        IllegalStateException cause = new IllegalStateException("cause");

        SupportSpawnException ended = SupportSpawnException.endMission("结束");
        assertFalse(ended.providerBroken());
        assertFalse(ended.refundCooldown());
        assertNull(ended.getCause());
        assertSame(cause, SupportSpawnException.endMission("结束", cause).getCause());

        SupportSpawnException notDelivered = SupportSpawnException.notDelivered("未投送");
        assertFalse(notDelivered.providerBroken());
        assertTrue(notDelivered.refundCooldown());
        SupportSpawnException notDeliveredWithCause =
                SupportSpawnException.notDelivered("未投送", cause);
        assertTrue(notDeliveredWithCause.refundCooldown());
        assertSame(cause, notDeliveredWithCause.getCause());

        SupportSpawnException brokenKeep =
                SupportSpawnException.providerBroken("损坏", null, false);
        assertTrue(brokenKeep.providerBroken());
        assertFalse(brokenKeep.refundCooldown());
        assertNull(brokenKeep.getCause());
        SupportSpawnException brokenRefund =
                SupportSpawnException.providerBroken("损坏", cause, true);
        assertTrue(brokenRefund.providerBroken());
        assertTrue(brokenRefund.refundCooldown());
        assertSame(cause, brokenRefund.getCause());
        assertEquals("损坏", brokenRefund.getMessage());
    }
}
