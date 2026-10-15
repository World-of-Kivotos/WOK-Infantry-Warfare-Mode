package com.wok.trauma.event;

import com.wok.trauma.config.TraumaConfig;
import com.wok.trauma.damage.ModDamageTypeTags;
import com.wok.trauma.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.Map;
import java.util.WeakHashMap;

public final class TraumaEvents {
    private static final String PAIN_TICKS_KEY = "wok_trauma.pain_ticks";
    private static final String PROPITAL_AFTEREFFECTS_KEY =
            "wok_trauma.propital_aftereffects";
    private static final String AFTEREFFECT_REMAINING_TICKS_KEY = "remaining_ticks";
    private static final String AFTEREFFECT_DURATION_TICKS_KEY = "duration_ticks";
    private static final int TREMOR_THRESHOLD = 20 * 20;
    private static final Map<Player, Long> LAST_CONCUSSION_ROLL = new WeakHashMap<>();

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        Player player = event.player;
        CompoundTag data = player.getPersistentData();
        applyPropitalAftereffect(player, data);
        if (player.hasEffect(ModEffects.ANALGESIA.get())) {
            player.removeEffect(ModEffects.PAIN.get());
            data.remove(PAIN_TICKS_KEY);
            return;
        }

        if (!player.hasEffect(ModEffects.PAIN.get())) {
            data.remove(PAIN_TICKS_KEY);
            return;
        }

        int painTicks = Math.min(data.getInt(PAIN_TICKS_KEY) + 1, TREMOR_THRESHOLD);
        data.putInt(PAIN_TICKS_KEY, painTicks);
        if (painTicks >= TREMOR_THRESHOLD && player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.TREMOR.get(), 40, 0, false, true, true));
        }
    }

    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof Player player)
                || event.getAmount() <= 0.0F
                || !event.getSource().is(ModDamageTypeTags.CAUSES_CONCUSSION)) {
            return;
        }

        long gameTime = player.level().getGameTime();
        Long previousRollTime = LAST_CONCUSSION_ROLL.put(player, gameTime);
        if (previousRollTime != null && previousRollTime == gameTime) {
            return;
        }

        if (player.getRandom().nextDouble() < TraumaConfig.CONCUSSION_CHANCE.get()) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.CONCUSSION.get(),
                    TraumaConfig.CONCUSSION_DURATION_TICKS.get(),
                    0, false, true, true));
        }
    }

    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModEffects.PAIN.get()
                && event.getEntity().hasEffect(ModEffects.ANALGESIA.get())) {
            event.setResult(Event.Result.DENY);
        }
    }

    public static void schedulePropitalAftereffect(LivingEntity entity, int delayTicks,
                                                   int durationTicks) {
        if (!(entity instanceof Player player) || delayTicks <= 0 || durationTicks <= 0) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        ListTag scheduledAftereffects = data.getList(
                PROPITAL_AFTEREFFECTS_KEY, Tag.TAG_COMPOUND);
        CompoundTag scheduledAftereffect = new CompoundTag();
        // The injector finishes before PlayerTickEvent.END in the same tick,
        // so include that immediate countdown step to preserve the advertised delay.
        scheduledAftereffect.putInt(AFTEREFFECT_REMAINING_TICKS_KEY, delayTicks + 1);
        scheduledAftereffect.putInt(AFTEREFFECT_DURATION_TICKS_KEY, durationTicks);
        scheduledAftereffects.add(scheduledAftereffect);
        data.put(PROPITAL_AFTEREFFECTS_KEY, scheduledAftereffects);
    }

    private static void applyPropitalAftereffect(Player player, CompoundTag data) {
        if (!data.contains(PROPITAL_AFTEREFFECTS_KEY, Tag.TAG_LIST)) {
            return;
        }

        ListTag scheduledAftereffects = data.getList(
                PROPITAL_AFTEREFFECTS_KEY, Tag.TAG_COMPOUND);
        int triggeredDurationTicks = 0;
        for (int index = scheduledAftereffects.size() - 1; index >= 0; index--) {
            CompoundTag scheduledAftereffect = scheduledAftereffects.getCompound(index);
            int remainingTicks = scheduledAftereffect.getInt(
                    AFTEREFFECT_REMAINING_TICKS_KEY) - 1;
            if (remainingTicks > 0) {
                scheduledAftereffect.putInt(AFTEREFFECT_REMAINING_TICKS_KEY, remainingTicks);
                continue;
            }

            triggeredDurationTicks = Math.max(
                    triggeredDurationTicks,
                    scheduledAftereffect.getInt(AFTEREFFECT_DURATION_TICKS_KEY));
            scheduledAftereffects.remove(index);
        }

        if (scheduledAftereffects.isEmpty()) {
            data.remove(PROPITAL_AFTEREFFECTS_KEY);
        } else {
            data.put(PROPITAL_AFTEREFFECTS_KEY, scheduledAftereffects);
        }
        if (triggeredDurationTicks > 0) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.TREMOR.get(), triggeredDurationTicks,
                    0, false, true, true));
            player.addEffect(new MobEffectInstance(
                    MobEffects.DARKNESS, triggeredDurationTicks,
                    0, false, false, true));
        }
    }

    private TraumaEvents() {
    }
}
