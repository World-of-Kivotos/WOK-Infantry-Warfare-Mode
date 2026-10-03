package com.wok.bodyhealth.prone;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProneHitMemoryTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void remembersTargetsPerProjectile() {
        ProneHitMemory memory = new ProneHitMemory();
        Object bullet = new Object();
        Object otherBullet = new Object();

        assertFalse(memory.hasHit(bullet, A));
        memory.markHit(bullet, A);
        assertTrue(memory.hasHit(bullet, A));
        assertFalse(memory.hasHit(otherBullet, A));
        assertFalse(memory.hasHit(bullet, B));

        memory.markHit(bullet, B);
        assertTrue(memory.hasHit(bullet, A));
        assertTrue(memory.hasHit(bullet, B));

        memory.clearAll();
        assertFalse(memory.hasHit(bullet, A));
        assertFalse(memory.hasHit(bullet, B));
    }

    @Test
    void nullsAreIgnored() {
        ProneHitMemory memory = new ProneHitMemory();
        memory.markHit(null, A);
        memory.markHit(new Object(), null);
        assertFalse(memory.hasHit(null, A));
    }
}
