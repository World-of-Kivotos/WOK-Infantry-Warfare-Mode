package com.wok.infantry.loadout;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.Optional;

/** A bounded player-inventory destination that an administrator may assign to a loadout slot. */
public enum LoadoutInventoryTarget {
    HOTBAR_1("快捷栏 1", 0, null),
    HOTBAR_2("快捷栏 2", 1, null),
    HOTBAR_3("快捷栏 3", 2, null),
    HOTBAR_4("快捷栏 4", 3, null),
    HOTBAR_5("快捷栏 5", 4, null),
    HOTBAR_6("快捷栏 6", 5, null),
    HOTBAR_7("快捷栏 7", 6, null),
    HOTBAR_8("快捷栏 8", 7, null),
    HOTBAR_9("快捷栏 9", 8, null),
    OFFHAND("副手", 40, EquipmentSlot.OFFHAND),
    ARMOR_HEAD("头盔", 39, EquipmentSlot.HEAD),
    ARMOR_CHEST("胸甲", 38, EquipmentSlot.CHEST),
    ARMOR_LEGS("护腿", 37, EquipmentSlot.LEGS),
    ARMOR_FEET("靴子", 36, EquipmentSlot.FEET);

    private final String displayName;
    private final int inventoryIndex;
    private final EquipmentSlot equipmentSlot;

    LoadoutInventoryTarget(String displayName, int inventoryIndex,
                           EquipmentSlot equipmentSlot) {
        this.displayName = displayName;
        this.inventoryIndex = inventoryIndex;
        this.equipmentSlot = equipmentSlot;
    }

    public String displayName() {
        return displayName;
    }

    public int inventoryIndex() {
        return inventoryIndex;
    }

    public boolean isArmor() {
        return inventoryIndex >= 36 && inventoryIndex <= 39;
    }

    public boolean accepts(ItemStack stack, LivingEntity wearer) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!isArmor()) {
            return true;
        }
        return stack.canEquip(equipmentSlot, wearer);
    }

    public static Optional<LoadoutInventoryTarget> byInventoryIndex(int inventoryIndex) {
        return Arrays.stream(values())
                .filter(target -> target.inventoryIndex == inventoryIndex).findFirst();
    }

    public static LoadoutInventoryTarget legacyTarget(LoadoutSlot slot) {
        return values()[slot.hotbarIndex()];
    }
}
