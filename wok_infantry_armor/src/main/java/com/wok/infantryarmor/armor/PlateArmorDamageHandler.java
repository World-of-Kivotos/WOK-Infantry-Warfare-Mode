package com.wok.infantryarmor.armor;

import com.wok.infantryarmor.armor.integration.PlateArmorTaczWearLedger;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 插板受击结算点。LOW 保证先接收冠军 HIGH 重写与易伤 NORMAL 放大，再把结果交给 LOWEST 职业减伤。
 */
public final class PlateArmorDamageHandler {

    /** 先把同 tick 换装状态同步到属性，避免插板公式后又误叠一次原版护甲。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            PlateArmorEquipmentHandler.synchronize(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0.0F || !(event.getEntity() instanceof Player player)) {
            return;
        }
        ArmorProtectionResolver.EquippedProtection protection =
                ArmorProtectionResolver.resolve(player, event.getSource());
        if (protection == null) {
            return;
        }

        ProtectiveArmorItem armor = protection.item();
        PlateArmorStats stats = PlateArmorStats.resolve(armor)
                .withProtectionEfficiency(armor.protectionEfficiency(protection.stack()));
        double input = event.getAmount();
        PlateArmorDamageClassifier.Kind kind = PlateArmorDamageClassifier.classify(event.getSource());
        double penetrationRatio = Double.NaN;
        if (kind == PlateArmorDamageClassifier.Kind.BALLISTIC_NORMAL
                || kind == PlateArmorDamageClassifier.Kind.BALLISTIC_ARMOR_PIERCING) {
            penetrationRatio = PlateArmorTaczWearLedger.recordSegment(
                    player, event.getSource(), kind, input);
        }
        boolean penetrationOvermatch = kind == PlateArmorDamageClassifier.Kind.BALLISTIC_ARMOR_PIERCING
                && Double.isFinite(penetrationRatio)
                && BallisticPenetrationMath.overmatches(penetrationRatio, armor.protectionTier());
        double output = switch (kind) {
            case BALLISTIC_NORMAL -> PlateArmorMath.reduceSegment(input, stats.ballisticProtection());
            case BALLISTIC_ARMOR_PIERCING -> penetrationOvermatch
                    ? input
                    : PlateArmorMath.reduceSegment(input, stats.armorPiercingBuffer());
            case GENERAL_PHYSICAL -> PlateArmorMath.reduceWithPressureCapacity(
                    input, stats.generalProtection(), stats.pressureCapacity());
            case EXCLUDED -> input;
        };
        event.setAmount((float) output);

        // TaCZ 一颗弹丸会产生两个 LivingHurtEvent，耐久统一留给 Post/Kill 集成只扣一次。
        if (kind == PlateArmorDamageClassifier.Kind.GENERAL_PHYSICAL) {
            armor.applyCombatWear(protection.stack(), input, player);
        }
    }
}

