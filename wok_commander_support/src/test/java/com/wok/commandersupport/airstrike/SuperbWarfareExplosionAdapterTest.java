package com.wok.commandersupport.airstrike;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperbWarfareExplosionAdapterTest {
    @Test
    void jdamUsesTheSuperbWarfareGiantExplosionContract() {
        assertEquals("superbwarfare", SuperbWarfareExplosionAdapter.MOD_ID);
        assertEquals("com.atsuishio.superbwarfare.tools.CustomExplosion$Builder",
                SuperbWarfareExplosionAdapter.BUILDER_CLASS);
        assertEquals("GIANT", SuperbWarfareExplosionAdapter.PARTICLE_TYPE);
        assertEquals(1_000.0F,
                SuperbWarfareExplosionAdapter.EXPLOSION_DAMAGE);
        assertEquals(32.0F,
                SuperbWarfareExplosionAdapter.EXPLOSION_RADIUS);
        assertTrue(SuperbWarfareExplosionAdapter.PARTICLE_TYPE_CLASS
                .endsWith("ParticleTool$ParticleType"));
    }
}
