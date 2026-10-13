package com.wok.infantryarmor.armor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared combat contract for localized chest and head protection. */
public interface ProtectiveArmorItem {

    PlateArmorTier protectionTier();

    PlateArmorWeight protectionWeight();

    PlateArmorConstructionMaterial constructionMaterial();

    EquipmentSlot protectionSlot();

    PlateArmorCoverage.Coverage coverage();

    boolean isFunctional(ItemStack stack);

    /** Multiplier applied to protection rates and pressure capacity for this stack. */
    default double protectionEfficiency(ItemStack stack) {
        return 1.0D;
    }

    default void applyBallisticWear(ItemStack stack, double normalDamage,
                                    double armorPiercingDamage, Player wearer) {
        applyCombatWear(stack, normalDamage + armorPiercingDamage, wearer);
    }

    void applyCombatWear(ItemStack stack, double incomingDamage, Player wearer);

    void breakExhausted(ItemStack stack, Player wearer);
}
