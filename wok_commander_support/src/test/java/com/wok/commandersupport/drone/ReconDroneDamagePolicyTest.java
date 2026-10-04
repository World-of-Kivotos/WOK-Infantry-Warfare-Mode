package com.wok.commandersupport.drone;

import com.wok.infantry.battle.Faction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconDroneDamagePolicyTest {
    private static final ResourceLocation SBW_CUSTOM_EXPLOSION = id("superbwarfare",
            "custom_explosion");
    private static final ResourceLocation SBW_PROJECTILE_EXPLOSION = id("superbwarfare",
            "projectile_explosion");
    private static final ResourceLocation VANILLA_EXPLOSION = id("minecraft", "explosion");

    @Test
    void blastsTaggedAsExplosionsDamageTheDrone() {
        assertTrue(ReconDroneDamagePolicy.accepts(true, SBW_CUSTOM_EXPLOSION, 250.0F, false,
                Faction.BLUE, Faction.RED));
        assertTrue(ReconDroneDamagePolicy.accepts(true, SBW_PROJECTILE_EXPLOSION, 40.0F, false,
                Faction.BLUE, null));
        assertTrue(ReconDroneDamagePolicy.accepts(true, VANILLA_EXPLOSION, 12.0F, false,
                Faction.RED, null));
        // The tag decides, whatever the id of the explosion type is.
        assertTrue(ReconDroneDamagePolicy.accepts(true, id("other_mod", "flak"), 5.0F, false,
                Faction.RED, null));
    }

    @Test
    void superbWarfareProjectileHitsDamageTheDrone() {
        assertEquals(id("superbwarfare", "projectile_hit"),
                ReconDroneDamagePolicy.SBW_PROJECTILE_HIT);
        assertTrue(ReconDroneDamagePolicy.accepts(false,
                ReconDroneDamagePolicy.SBW_PROJECTILE_HIT, 80.0F, false, Faction.BLUE,
                Faction.RED));
    }

    @Test
    void bulletsMeleeAndOtherDamageAreIgnored() {
        for (ResourceLocation type : new ResourceLocation[]{
                id("tacz", "bullet"),
                id("tacz", "bullet_ignore_armor"),
                id("superbwarfare", "gunfire"),
                id("superbwarfare", "gunfire_headshot"),
                id("minecraft", "player_attack"),
                id("minecraft", "arrow"),
                id("minecraft", "fall"),
                id("minecraft", "generic"),
                null}) {
            assertFalse(ReconDroneDamagePolicy.accepts(false, type, 1_000.0F, false,
                    Faction.BLUE, Faction.RED), String.valueOf(type));
        }
    }

    @Test
    void playersOfTheDronesOwnFactionCannotShootItDown() {
        assertFalse(ReconDroneDamagePolicy.accepts(true, SBW_CUSTOM_EXPLOSION, 500.0F, false,
                Faction.BLUE, Faction.BLUE));
        assertFalse(ReconDroneDamagePolicy.accepts(false,
                ReconDroneDamagePolicy.SBW_PROJECTILE_HIT, 500.0F, false, Faction.RED,
                Faction.RED));
        assertTrue(ReconDroneDamagePolicy.accepts(true, SBW_CUSTOM_EXPLOSION, 500.0F, false,
                Faction.RED, Faction.BLUE));
        // Non-players and unassigned players are not friendly.
        assertTrue(ReconDroneDamagePolicy.accepts(true, SBW_CUSTOM_EXPLOSION, 500.0F, false,
                Faction.RED, null));
        // A drone of unknown faction has no friends.
        assertTrue(ReconDroneDamagePolicy.accepts(true, SBW_CUSTOM_EXPLOSION, 500.0F, false,
                null, Faction.RED));
    }

    @Test
    void aFallingWreckTakesNoFurtherDamage() {
        assertFalse(ReconDroneDamagePolicy.accepts(true, SBW_CUSTOM_EXPLOSION, 500.0F, true,
                Faction.BLUE, Faction.RED));
        assertFalse(ReconDroneDamagePolicy.accepts(false,
                ReconDroneDamagePolicy.SBW_PROJECTILE_HIT, 500.0F, true, Faction.BLUE, null));
    }

    @Test
    void invalidAmountsAreIgnored() {
        for (float amount : new float[]{0.0F, -5.0F, Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY}) {
            assertFalse(ReconDroneDamagePolicy.accepts(true, SBW_CUSTOM_EXPLOSION, amount,
                    false, Faction.BLUE, Faction.RED), String.valueOf(amount));
        }
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
