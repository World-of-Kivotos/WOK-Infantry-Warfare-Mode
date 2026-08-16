package com.wok.bodyhealth.health;

import com.wok.bodyhealth.WokBodyHealthMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public final class BodyPartKillDamage {
    public static DamageSource create(ServerPlayer victim, ServerPlayer killer,
                                      DamageSource original, BodyPart fatalPart) {
        ResourceKey<DamageType> key = ResourceKey.create(
                Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(
                        WokBodyHealthMod.MOD_ID, "pvp_kill_" + fatalPart.key()));
        Holder<DamageType> type = victim.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        Entity directEntity = original.getDirectEntity();
        if (directEntity == null) {
            directEntity = killer;
        }
        return new DamageSource(type, directEntity, killer);
    }

    private BodyPartKillDamage() {
    }
}
