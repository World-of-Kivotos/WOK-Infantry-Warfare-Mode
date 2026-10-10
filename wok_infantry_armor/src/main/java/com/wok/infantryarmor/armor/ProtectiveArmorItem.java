package com.wok.infantryarmor.armor;

import com.wok.infantryarmor.armor.settings.ArmorItemProfile;
import com.wok.infantryarmor.armor.settings.ArmorItemSettings;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Shared combat contract for localized chest and head protection.
 *
 * <p>等级、类型、材质、防护数值、耐久与覆盖部位一律经过逐件配置解析器 {@link #settings()}，
 * 穿甲判定（{@link #protectionTier()}）因此自动使用覆盖后的等级。</p>
 */
public interface ProtectiveArmorItem {

    /** 解析后的逐件设置；每次调用都取当前快照，不要长期保存返回值。 */
    ArmorItemProfile settings();

    /** 已通过注册表检查的额外属性修正（以及被忽略的条目）。 */
    ArmorItemSettings.ItemAttributes extraAttributes();

    default PlateArmorTier protectionTier() {
        return settings().tier();
    }

    default PlateArmorWeight protectionWeight() {
        return settings().weight();
    }

    default PlateArmorConstructionMaterial constructionMaterial() {
        return settings().material();
    }

    EquipmentSlot protectionSlot();

    default PlateArmorCoverage.Coverage coverage() {
        return settings().coverage();
    }

    boolean isFunctional(ItemStack stack);

    /** Multiplier applied to protection rates and pressure capacity for this stack. */
    default double protectionEfficiency(ItemStack stack) {
        return 1.0D;
    }

    /** TaCZ 一弹一次的磨损结算。实现类必须自己按逐件磨损倍率只乘一次，不能再转调 applyCombatWear。 */
    void applyBallisticWear(ItemStack stack, double normalDamage, double armorPiercingDamage, Player wearer);

    void applyCombatWear(ItemStack stack, double incomingDamage, Player wearer);

    void breakExhausted(ItemStack stack, Player wearer);
}
