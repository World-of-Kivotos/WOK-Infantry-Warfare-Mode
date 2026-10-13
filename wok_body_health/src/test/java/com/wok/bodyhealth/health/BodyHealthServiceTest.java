package com.wok.bodyhealth.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BodyHealthServiceTest {
    @Test
    void convertsOrdinaryDamageWithTheConfiguredScale() {
        assertEquals(20.0F, BodyHealthService.toDamagePoints(2.5F, 8.0F));
    }

    @Test
    void keepsInstantKillDamageFiniteInsteadOfDiscardingIt() {
        assertEquals(Float.MAX_VALUE, BodyHealthService.toDamagePoints(Float.MAX_VALUE, 8.0F));
        assertEquals(Float.MAX_VALUE,
                BodyHealthService.toDamagePoints(Float.POSITIVE_INFINITY, 8.0F));
    }

    @Test
    void ignoresInvalidOrNonPositiveDamage() {
        assertEquals(0.0F, BodyHealthService.toDamagePoints(Float.NaN, 8.0F));
        assertEquals(0.0F, BodyHealthService.toDamagePoints(0.0F, 8.0F));
        assertEquals(0.0F, BodyHealthService.toDamagePoints(-3.0F, 8.0F));
    }

    @Test
    void scalesJumpsByDestroyedLegCount() {
        assertEquals(1.0D, BodyHealthService.jumpVelocityMultiplier(0));
        assertEquals(0.65D, BodyHealthService.jumpVelocityMultiplier(1));
        assertEquals(0.35D, BodyHealthService.jumpVelocityMultiplier(2));
    }
}
