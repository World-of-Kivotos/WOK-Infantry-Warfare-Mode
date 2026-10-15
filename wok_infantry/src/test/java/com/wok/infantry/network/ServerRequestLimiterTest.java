package com.wok.infantry.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRequestLimiterTest {
    private static final long SECOND = 1_000_000_000L;

    @Test
    void loadoutAndFormationCatalogRequestsHaveTheirOwnBudgets() {
        UUID player = UUID.randomUUID();
        long now = 50L * SECOND;
        try {
            assertTrue(ServerRequestLimiter.allow(player, ServerRequestLimiter.Kind.OPEN_UI, now));
            // squad-07: the terminal key and an immediate "配装" / "编制" tab press no longer
            // share one window, so neither request is dropped silently.
            assertTrue(ServerRequestLimiter.allow(player,
                    ServerRequestLimiter.Kind.LOADOUT_OPEN, now + 1L));
            assertTrue(ServerRequestLimiter.allow(player,
                    ServerRequestLimiter.Kind.FORMATION_CATALOG, now + 2L));

            assertFalse(ServerRequestLimiter.allow(player,
                    ServerRequestLimiter.Kind.LOADOUT_OPEN, now + SECOND / 2L),
                    "a second loadout request inside its own window is still limited");
            assertTrue(ServerRequestLimiter.allow(player,
                    ServerRequestLimiter.Kind.LOADOUT_OPEN, now + 1L + SECOND));
            assertEquals(1_000L, ServerRequestLimiter.Kind.LOADOUT_OPEN.cooldownMillis());
            assertEquals(1_000L, ServerRequestLimiter.Kind.OPEN_UI.cooldownMillis());
            assertFalse(ServerRequestLimiter.allow(null, ServerRequestLimiter.Kind.OPEN_UI,
                    now));
        } finally {
            ServerRequestLimiter.forget(player);
        }
    }
}
