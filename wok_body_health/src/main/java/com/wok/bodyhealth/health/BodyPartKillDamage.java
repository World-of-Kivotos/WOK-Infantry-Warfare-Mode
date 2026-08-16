package com.wok.bodyhealth.health;

import com.wok.bodyhealth.WokBodyHealthMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class BodyPartKillDamage {
    public static DamageSource create(ServerPlayer victim, ServerPlayer killer,
                                      DamageSource original, BodyPart fatalPart,
                                      boolean propagatedFatal) {
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
        boolean ranged = original.is(DamageTypeTags.IS_PROJECTILE)
                || directEntity != killer;
        String style = propagatedFatal ? "spread" : ranged ? "ranged" : "generic";
        int variants = propagatedFatal ? 3 : ranged ? 4 : 2;
        int variant = victim.getRandom().nextInt(variants) + 1;
        String messageKey = "death.attack.wok_body_health.pvp_kill_"
                + fatalPart.key() + "." + style + "_" + variant;
        return new VariedKillDamageSource(
                type, directEntity, killer, messageKey);
    }

    private static final class VariedKillDamageSource extends DamageSource {
        private static final String NAMED_WEAPON_KEY =
                "death.attack.wok_body_health.named_weapon";

        private final String messageKey;

        private VariedKillDamageSource(Holder<DamageType> type, Entity directEntity,
                                       Entity causingEntity, String messageKey) {
            super(type, directEntity, causingEntity);
            this.messageKey = messageKey;
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity victim) {
            Entity attacker = getEntity() != null ? getEntity() : getDirectEntity();
            if (attacker == null) {
                return super.getLocalizedDeathMessage(victim);
            }

            Component message = Component.translatable(
                    messageKey, victim.getDisplayName(), attacker.getDisplayName());
            ItemStack weapon = attacker instanceof LivingEntity living
                    ? living.getMainHandItem() : ItemStack.EMPTY;
            if (!weapon.isEmpty() && weapon.hasCustomHoverName()) {
                return Component.translatable(
                        NAMED_WEAPON_KEY, message, weapon.getDisplayName());
            }
            return message;
        }
    }

    private BodyPartKillDamage() {
    }
}
