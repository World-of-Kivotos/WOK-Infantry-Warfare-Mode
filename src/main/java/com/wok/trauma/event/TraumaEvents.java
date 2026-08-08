package com.wok.trauma.event;

import com.wok.trauma.config.TraumaConfig;
import com.wok.trauma.damage.ModDamageTypeTags;
import com.wok.trauma.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.Map;
import java.util.WeakHashMap;

public final class TraumaEvents {
    private static final String PAIN_TICKS_KEY = "wok_trauma.pain_ticks";
    private static final int TREMOR_THRESHOLD = 20 * 20;
    private static final Map<Player, Long> LAST_CONCUSSION_ROLL = new WeakHashMap<>();

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        Player player = event.player;
        CompoundTag data = player.getPersistentData();
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

    private TraumaEvents() {
    }
}
