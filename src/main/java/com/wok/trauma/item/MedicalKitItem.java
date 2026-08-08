package com.wok.trauma.item;

import com.wok.trauma.compat.bodyhealth.BodyHealthCompat;
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

public final class MedicalKitItem extends Item {
    private static final int MAX_USE_DURATION = 72_000;
    private static final String BODY_PART_TARGET_TAG =
            "wok_trauma.body_health_treatment_target";

    private final int healIntervalTicks;
    private final float healAmount;
    private final int smallBleedingTreatmentTicks;
    private final int smallBleedingDurabilityCost;
    private final int majorBleedingTreatmentTicks;
    private final int majorBleedingDurabilityCost;

    public MedicalKitItem(Properties properties, int healIntervalTicks, float healAmount,
                          int smallBleedingTreatmentTicks, int smallBleedingDurabilityCost,
                          int majorBleedingTreatmentTicks, int majorBleedingDurabilityCost) {
        super(properties);
        this.healIntervalTicks = healIntervalTicks;
        this.healAmount = healAmount;
        this.smallBleedingTreatmentTicks = smallBleedingTreatmentTicks;
        this.smallBleedingDurabilityCost = smallBleedingDurabilityCost;
        this.majorBleedingTreatmentTicks = majorBleedingTreatmentTicks;
        this.majorBleedingDurabilityCost = majorBleedingDurabilityCost;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canProvideTreatment(player)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide()) {
            if (BodyHealthCompat.isAvailable()) {
                TreatmentSelection selection = TreatmentSelectionStore.get(player);
                String target = selection.isAutomatic()
                        ? BodyHealthCompat.selectTreatmentPart(player)
                        : selection.bodyPartName();
                if (target.isEmpty()) {
                    stack.removeTagKey(BODY_PART_TARGET_TAG);
                } else {
                    stack.getOrCreateTag().putString(BODY_PART_TARGET_TAG, target);
                }
            } else {
                stack.removeTagKey(BODY_PART_TARGET_TAG);
            }
            level.playSound(null, player.blockPosition(),
                    ModSounds.CANVAS_BAG_OPEN.get(), SoundSource.PLAYERS,
                    0.72F, 0.96F + player.getRandom().nextFloat() * 0.08F);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || stack.isEmpty()) {
            return;
        }

        int elapsedTicks = getUseDuration(stack) - remainingUseDuration;
        if (elapsedTicks >= 12 && (elapsedTicks - 12) % 24 == 0) {
            level.playSound(null, entity.blockPosition(),
                    ModSounds.BANDAGE_WRAP.get(), SoundSource.PLAYERS,
                    0.48F, 0.94F + entity.getRandom().nextFloat() * 0.12F);
        }
        if (smallBleedingTreatmentTicks > 0
                && elapsedTicks == smallBleedingTreatmentTicks) {
            treatEffect(entity, stack, ModEffects.BLEEDING.get(),
                    smallBleedingDurabilityCost);
        }
        if (!stack.isEmpty()
                && majorBleedingTreatmentTicks > 0
                && elapsedTicks == majorBleedingTreatmentTicks) {
            treatEffect(entity, stack, ModEffects.MAJOR_BLEEDING.get(),
                    majorBleedingDurabilityCost);
        }

        if (!stack.isEmpty()
                && elapsedTicks > 0
                && elapsedTicks % healIntervalTicks == 0
                && needsHealthTreatment(entity)) {
            boolean healed;
            if (BodyHealthCompat.isAvailable()) {
                String target = stack.hasTag()
                        ? stack.getTag().getString(BODY_PART_TARGET_TAG) : "";
                healed = !target.isEmpty()
                        && BodyHealthCompat.healPart(entity, target, healAmount);
            } else {
                entity.heal(healAmount);
                healed = true;
            }
            if (healed) {
                consumeDurability(entity, stack, 1);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.wok_trauma.healing_capacity", stack.getMaxDamage())
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(
                "tooltip.wok_trauma.heal_rate",
                displayNumber(healIntervalTicks / 20.0F), displayNumber(healAmount))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_trauma.select_body_part")
                .withStyle(ChatFormatting.DARK_GRAY));
        if (smallBleedingTreatmentTicks > 0) {
            tooltip.add(Component.translatable(
                            "tooltip.wok_trauma.treats_bleeding",
                            displayNumber(smallBleedingTreatmentTicks / 20.0F),
                            smallBleedingDurabilityCost)
                    .withStyle(ChatFormatting.DARK_RED));
        }
        if (majorBleedingTreatmentTicks > 0) {
            tooltip.add(Component.translatable(
                            "tooltip.wok_trauma.treats_major_bleeding",
                            displayNumber(majorBleedingTreatmentTicks / 20.0F),
                            majorBleedingDurabilityCost)
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private boolean canProvideTreatment(LivingEntity entity) {
        // Body-part health is authoritative on the server. Permit the client
        // to predict starting the use animation when the integration exists;
        // the server still rejects the use if no treatment is actually needed.
        if (entity.level().isClientSide() && BodyHealthCompat.isAvailable()) {
            return true;
        }
        return needsHealthTreatment(entity)
                || smallBleedingTreatmentTicks > 0
                && entity.hasEffect(ModEffects.BLEEDING.get())
                || majorBleedingTreatmentTicks > 0
                && entity.hasEffect(ModEffects.MAJOR_BLEEDING.get());
    }

    private static boolean needsHealthTreatment(LivingEntity entity) {
        if (BodyHealthCompat.isAvailable()) {
            if (entity instanceof Player player) {
                TreatmentSelection selection = TreatmentSelectionStore.get(player);
                if (!selection.isAutomatic()) {
                    return BodyHealthCompat.isPartDamaged(
                            entity, selection.bodyPartName());
                }
            }
            return BodyHealthCompat.needsHealing(entity);
        }
        return entity.getHealth() < entity.getMaxHealth();
    }

    private static void treatEffect(LivingEntity entity, ItemStack stack,
                                    MobEffect effect, int durabilityCost) {
        if (!entity.hasEffect(effect) || remainingDurability(stack) < durabilityCost) {
            return;
        }

        entity.removeEffect(effect);
        consumeDurability(entity, stack, durabilityCost);
    }

    private static int remainingDurability(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    private static Number displayNumber(float value) {
        int integerValue = (int) value;
        if (value == integerValue) {
            return integerValue;
        }
        return value;
    }

    private static void consumeDurability(LivingEntity entity, ItemStack stack, int amount) {
        stack.hurtAndBreak(amount, entity,
                brokenEntity -> brokenEntity.broadcastBreakEvent(entity.getUsedItemHand()));
    }
}
