package com.wok.trauma.effect;

import com.wok.trauma.compat.bodyhealth.BodyHealthCompat;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class TimedRegenerationEffect extends MobEffect {
    public enum BodyHealthHealingMode {
        SHARED_POOL,
        ALL_DAMAGED_PARTS
    }

    private final int intervalTicks;
    private final float healAmount;
    private final BodyHealthHealingMode bodyHealthHealingMode;

    public TimedRegenerationEffect(int color, int intervalTicks, float healAmount,
                                   BodyHealthHealingMode bodyHealthHealingMode) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.intervalTicks = intervalTicks;
        this.healAmount = healAmount;
        this.bodyHealthHealingMode = bodyHealthHealingMode;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (BodyHealthCompat.isAvailable()) {
            if (BodyHealthCompat.needsHealing(entity)) {
                if (bodyHealthHealingMode == BodyHealthHealingMode.ALL_DAMAGED_PARTS) {
                    BodyHealthCompat.healAllParts(entity, healAmount);
                } else {
                    BodyHealthCompat.heal(entity, healAmount);
                }
            }
        } else if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(healAmount);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % intervalTicks == 1;
    }
}
