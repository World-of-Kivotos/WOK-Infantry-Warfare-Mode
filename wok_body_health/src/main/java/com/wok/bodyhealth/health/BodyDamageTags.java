package com.wok.bodyhealth.health;

import com.wok.bodyhealth.WokBodyHealthMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Data-driven damage routing. Datapacks and modpacks can move a damage type
 * between these tags without code changes; types in none of them are treated
 * as a single localized hit.
 */
public final class BodyDamageTags {
    /** Weighted across all seven parts, like vanilla explosions. */
    public static final TagKey<DamageType> EXPLOSION_SPREAD = create("explosion_spread");
    /** Split evenly between both legs, like fall damage. */
    public static final TagKey<DamageType> LEG_SPREAD = create("leg_spread");
    /** Split evenly between every intact part: fire, drowning, poison and similar. */
    public static final TagKey<DamageType> SYSTEMIC_SPREAD = create("systemic_spread");
    /** Localized hits that always land on the head, including third-party headshot types. */
    public static final TagKey<DamageType> HEAD_HIT = create("head_hit");

    private static TagKey<DamageType> create(String path) {
        return TagKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(WokBodyHealthMod.MOD_ID, path));
    }

    private BodyDamageTags() {
    }
}
