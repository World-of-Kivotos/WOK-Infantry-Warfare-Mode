package com.wok.bodyhealth.prone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProneScopeTest {
    private final Object bulletA = new Object();
    private final Object bulletB = new Object();

    @BeforeEach
    @AfterEach
    void clearSlot() {
        ProneScope.reset();
    }

    @Test
    void consumeMatchesOnlyTheScopedProjectileOnce() {
        ProneScope.enter(bulletA);
        assertFalse(ProneScope.consume(bulletB));
        assertTrue(ProneScope.consume(bulletA));
        assertFalse(ProneScope.consume(bulletA));
    }

    @Test
    void exitClosesItsOwnScope() {
        ProneScope.enter(bulletA);
        ProneScope.exit(bulletA);
        assertFalse(ProneScope.consume(bulletA));
    }

    @Test
    void exitForAnotherProjectileKeepsTheScope() {
        ProneScope.enter(bulletA);
        ProneScope.exit(bulletB);
        assertTrue(ProneScope.consume(bulletA));
    }

    @Test
    void enterReplacesALeakedScope() {
        ProneScope.enter(bulletA);
        ProneScope.enter(bulletB);
        assertFalse(ProneScope.consume(bulletA));
        assertTrue(ProneScope.consume(bulletB));
    }

    @Test
    void resetEmptiesTheSlot() {
        ProneScope.enter(bulletA);
        ProneScope.reset();
        assertFalse(ProneScope.consume(bulletA));
        assertFalse(ProneScope.consume(null));
    }

    @Test
    void scopesArePerThread() throws Exception {
        ProneScope.enter(bulletA);
        assertFalse(CompletableFuture.supplyAsync(() -> ProneScope.consume(bulletA)).get());
        assertTrue(ProneScope.consume(bulletA));
    }
}
