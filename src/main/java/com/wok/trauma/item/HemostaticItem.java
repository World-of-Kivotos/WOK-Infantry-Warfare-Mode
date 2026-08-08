package com.wok.trauma.item;

import com.wok.trauma.registry.ModEffects;
import com.wok.trauma.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
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

public final class HemostaticItem extends Item {
    private final int treatmentTicks;
    private final int uses;
    private final Supplier<MobEffect> treatedEffect;
    private final String treatmentTooltipKey;

    public HemostaticItem(Properties properties, int treatmentTicks, int uses,
                          Supplier<MobEffect> treatedEffect, String treatmentTooltipKey) {
        super(properties);
        this.treatmentTicks = treatmentTicks;
        this.uses = uses;
        this.treatedEffect = treatedEffect;
        this.treatmentTooltipKey = treatmentTooltipKey;
    }

    public static HemostaticItem forBleeding(Properties properties, int treatmentTicks, int uses) {
        return new HemostaticItem(properties, treatmentTicks, uses,
                ModEffects.BLEEDING, "tooltip.wok_trauma.stops_bleeding");
    }

    public static HemostaticItem forMajorBleeding(Properties properties, int treatmentTicks, int uses) {
        return new HemostaticItem(properties, treatmentTicks, uses,
                ModEffects.MAJOR_BLEEDING, "tooltip.wok_trauma.stops_major_bleeding");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.hasEffect(treatedEffect.get())) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity.removeEffect(treatedEffect.get())) {
            consumeUse(stack, entity);
        }
        return stack;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity,
                          ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide()) {
            return;
        }

        int elapsedTicks = getUseDuration(stack) - remainingUseDuration;
        if (elapsedTicks >= 5 && (elapsedTicks - 5) % 16 == 0) {
            level.playSound(null, entity.blockPosition(),
                    ModSounds.BANDAGE_WRAP.get(), SoundSource.PLAYERS,
                    0.55F, 0.94F + entity.getRandom().nextFloat() * 0.12F);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return treatmentTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                        treatmentTooltipKey, displaySeconds(treatmentTicks))
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable(
                        "tooltip.wok_trauma.available_uses", remainingUses(stack))
                .withStyle(ChatFormatting.GRAY));
    }

    private void consumeUse(ItemStack stack, LivingEntity entity) {
        if (uses <= 1) {
            stack.shrink(1);
            return;
        }

        stack.hurtAndBreak(1, entity,
                brokenEntity -> brokenEntity.broadcastBreakEvent(entity.getUsedItemHand()));
    }

    private static Number displaySeconds(int ticks) {
        float seconds = ticks / 20.0F;
        int integerSeconds = (int) seconds;
        if (seconds == integerSeconds) {
            return integerSeconds;
        }
        return seconds;
    }

    private int remainingUses(ItemStack stack) {
        if (uses <= 1) {
            return 1;
        }
        return stack.getMaxDamage() - stack.getDamageValue();
    }
}
