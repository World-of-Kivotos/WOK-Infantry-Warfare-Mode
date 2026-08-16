package com.wok.trauma.effect;

import com.wok.trauma.compat.bodyhealth.BodyHealthCompat;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class TimedRegenerationEffect extends MobEffect {
    private final int intervalTicks;
    private final float healAmount;

    public TimedRegenerationEffect(int color, int intervalTicks, float healAmount) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.intervalTicks = intervalTicks;
        this.healAmount = healAmount;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }

        if (BodyHealthCompat.isAvailable()) {
            if (BodyHealthCompat.needsHealing(entity)) {
                BodyHealthCompat.healAllParts(entity, healAmount);
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
