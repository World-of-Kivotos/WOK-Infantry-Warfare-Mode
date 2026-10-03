package com.wok.bodyhealth.prone;

import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ProneHitLedgerTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID P = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID Q = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final long T = 4000L;
    private static final Vec3 POINT = new Vec3(1.0D, 64.2D, 3.0D);

    @Test
    void anEntryCanBeReadRepeatedlyInItsTick() {
        ProneHitLedger ledger = new ProneHitLedger();
        ledger.put(A, P, T, BodyPart.LEFT_LEG, POINT);

        assertEquals(BodyPart.LEFT_LEG, ledger.find(A, P, T));
        assertEquals(BodyPart.LEFT_LEG, ledger.find(A, P, T));
        assertEquals(POINT, ledger.findPoint(A, P, T));
    }

    @Test
    void everyKeyPartMustMatch() {
        ProneHitLedger ledger = new ProneHitLedger();
        ledger.put(A, P, T, BodyPart.HEAD, POINT);

        assertNull(ledger.find(A, Q, T));
        assertNull(ledger.find(B, P, T));
        assertNull(ledger.find(A, P, T + 1));
        assertNull(ledger.find(A, null, T));
    }

    @Test
    void clearRemovesTheEntry() {
        ProneHitLedger ledger = new ProneHitLedger();
        ledger.put(A, P, T, BodyPart.CHEST, POINT);
        ledger.put(B, P, T, BodyPart.ABDOMEN, POINT);
        ledger.clear(A, P, T);

        assertNull(ledger.find(A, P, T));
        assertEquals(BodyPart.ABDOMEN, ledger.find(B, P, T));
    }

    @Test
    void aNewTickDropsOlderEntries() {
        ProneHitLedger ledger = new ProneHitLedger();
        ledger.put(A, P, T, BodyPart.CHEST, POINT);
        ledger.put(B, Q, T + 1, BodyPart.RIGHT_ARM, POINT);

        assertNull(ledger.find(A, P, T));
        assertEquals(BodyPart.RIGHT_ARM, ledger.find(B, Q, T + 1));

        // Clearing at a later tick also drops everything recorded before it.
        ledger.clear(A, P, T + 2);
        assertNull(ledger.find(B, Q, T + 1));

        ledger.put(A, P, T + 3, BodyPart.HEAD, POINT);
        ledger.clearAll();
        assertNull(ledger.find(A, P, T + 3));
    }
}
