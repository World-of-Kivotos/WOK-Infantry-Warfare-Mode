package com.wok.trauma.effect;

import com.wok.trauma.compat.bodyhealth.BodyHealthCompat;
import com.wok.trauma.damage.ModDamageTypes;
import com.wok.trauma.registry.ModParticles;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

public final class BleedingEffect extends MobEffect {
    private static final int BLEEDING_DECAL_INTERVAL = 5 * 20;
    private static final int MAJOR_BLEEDING_DECAL_INTERVAL = 3 * 20;

    private final float damage;
    private final boolean major;

    public BleedingEffect(float damage, boolean major, int color) {
        super(MobEffectCategory.HARMFUL, color);
        this.damage = damage;
        this.major = major;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
            return;
        }

        applySilentBleedingDamage(level, entity);
        int decalInterval = major ? MAJOR_BLEEDING_DECAL_INTERVAL : BLEEDING_DECAL_INTERVAL;
        MobEffectInstance activeEffect = entity.getEffect(this);
        int remainingDuration = activeEffect == null ? 0 : activeEffect.getDuration();
        if (remainingDuration % decalInterval == 0) {
            spawnBloodDecal(level, entity,
                    major ? ModParticles.BLOOD_POOL.get() : ModParticles.BLOOD_SPOT.get());
        }
    }

    private void applySilentBleedingDamage(ServerLevel level, LivingEntity entity) {
        float remainingDamage = damage;
        float absorption = entity.getAbsorptionAmount();
        if (absorption > 0.0F) {
            float absorbed = Math.min(absorption, remainingDamage);
            entity.setAbsorptionAmount(absorption - absorbed);
            remainingDamage -= absorbed;
        }

        if (remainingDamage <= 0.0F) {
            return;
        }

        DamageSource bleedingSource = ModDamageTypes.bleeding(level);
        if (BodyHealthCompat.applyBleedingDamage(entity, bleedingSource, remainingDamage)) {
            return;
        }

        if (entity.getHealth() > remainingDamage) {
            // Updating health directly avoids the vanilla hurt event, so a
            // bleeding tick has no red flash, camera shake, hurt sound or
            // knockback.
            entity.setHealth(entity.getHealth() - remainingDamage);
        } else {
            // Do not re-enter LivingEntity#hurt for the final tick: that method
            // broadcasts the vanilla hurt animation before death. Calling die
            // directly keeps drops/death messages while the bleed stays silent.
            if (tryUseTotem(entity, bleedingSource)) {
                return;
            }
            entity.setHealth(0.0F);
            entity.die(bleedingSource);
        }
    }

    private static boolean tryUseTotem(LivingEntity entity, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        ItemStack usedTotem = ItemStack.EMPTY;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack heldItem = entity.getItemInHand(hand);
            if (heldItem.is(Items.TOTEM_OF_UNDYING)
                    && ForgeHooks.onLivingUseTotem(entity, source, heldItem, hand)) {
                usedTotem = heldItem.copy();
                heldItem.shrink(1);
                break;
            }
        }

        if (usedTotem.isEmpty()) {
            return false;
        }

        if (entity instanceof ServerPlayer player) {
            player.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING), 1);
            CriteriaTriggers.USED_TOTEM.trigger(player, usedTotem);
        }

        entity.setHealth(1.0F);
        entity.removeAllEffects();
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        entity.level().broadcastEntityEvent(entity, (byte) 35);
        return true;
    }

    private static void spawnBloodDecal(ServerLevel level, LivingEntity entity, SimpleParticleType particle) {
        if (!entity.onGround()) {
            return;
        }

        double offsetX = (entity.getRandom().nextDouble() - 0.5D) * 0.7D;
        double offsetZ = (entity.getRandom().nextDouble() - 0.5D) * 0.7D;
        Vec3 start = new Vec3(entity.getX() + offsetX, entity.getY() + 0.35D, entity.getZ() + offsetZ);
        Vec3 end = start.add(0.0D, -2.5D, 0.0D);
        BlockHitResult hit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 location = hit.getLocation();
            level.sendParticles(particle, location.x, location.y + 0.004D, location.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
