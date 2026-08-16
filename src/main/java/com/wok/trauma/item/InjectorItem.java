package com.wok.trauma.item;

import com.wok.trauma.registry.ModEffects;
import com.wok.trauma.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public final class InjectorItem extends Item {
    private static final int USE_DURATION_TICKS = 10;

    private final int analgesiaDurationTicks;
    private final int regenerationDurationTicks;
    private final int regenerationIntervalTicks;
    @Nullable
    private final Supplier<MobEffect> regenerationEffect;

    public InjectorItem(Properties properties, int analgesiaDurationTicks,
                        int regenerationDurationTicks, int regenerationIntervalTicks,
                        @Nullable Supplier<MobEffect> regenerationEffect) {
        super(properties);
        this.analgesiaDurationTicks = analgesiaDurationTicks;
        this.regenerationDurationTicks = regenerationDurationTicks;
        this.regenerationIntervalTicks = regenerationIntervalTicks;
        this.regenerationEffect = regenerationEffect;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            level.playSound(null, entity.blockPosition(),
                    ModSounds.INJECTOR_PUNCTURE.get(), SoundSource.PLAYERS,
                    0.65F, 0.96F + entity.getRandom().nextFloat() * 0.08F);
            entity.removeEffect(ModEffects.CONCUSSION.get());

            if (analgesiaDurationTicks > 0) {
                entity.addEffect(new MobEffectInstance(
                        ModEffects.ANALGESIA.get(), analgesiaDurationTicks,
                        0, false, true, true));
            }
            if (regenerationEffect != null && regenerationDurationTicks > 0) {
                boolean showRegeneration = regenerationEffect == ModEffects.REGENERATION;
                entity.addEffect(new MobEffectInstance(
                        regenerationEffect.get(), regenerationDurationTicks,
                        0, false, showRegeneration, showRegeneration));
            }

            if (!(entity instanceof Player player) || !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                        "tooltip.wok_trauma.injector_use_time",
                        displaySeconds(USE_DURATION_TICKS))
                .withStyle(ChatFormatting.GRAY));
        if (analgesiaDurationTicks > 0) {
            tooltip.add(Component.translatable(
                            "tooltip.wok_trauma.grants_analgesia",
                            displaySeconds(analgesiaDurationTicks))
                    .withStyle(ChatFormatting.AQUA));
        }
        if (regenerationEffect != null && regenerationDurationTicks > 0) {
            String key = regenerationEffect == ModEffects.REGENERATION
                    ? "tooltip.wok_trauma.grants_regeneration"
                    : "tooltip.wok_trauma.grants_slow_regeneration";
            tooltip.add(Component.translatable(
                            key,
                            displaySeconds(regenerationDurationTicks),
                            displaySeconds(regenerationIntervalTicks))
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable(
                            "tooltip.wok_trauma.regenerates_all_body_parts")
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        tooltip.add(Component.translatable("tooltip.wok_trauma.removes_concussion")
                .withStyle(ChatFormatting.GOLD));
    }

    private static Number displaySeconds(int ticks) {
        float seconds = ticks / 20.0F;
        int integerSeconds = (int) seconds;
        if (seconds == integerSeconds) {
            return integerSeconds;
        }
        return seconds;
    }
}
