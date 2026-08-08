package com.wok.trauma.damage;

import com.wok.trauma.WokTraumaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypeTags {
    public static final TagKey<DamageType> CAUSES_CONCUSSION = TagKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(
                    WokTraumaMod.MOD_ID, "causes_concussion"));

    private ModDamageTypeTags() {
    }
}
