package com.wok.infantryarmor.armor;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Selects exactly one equipped localized armor item for a hit. */
public final class ArmorProtectionResolver {

    public static EquippedProtection resolve(Player player, DamageSource source) {
        EquippedProtection chest = functionalAt(player, EquipmentSlot.CHEST);
        EquippedProtection head = functionalAt(player, EquipmentSlot.HEAD);
        String externalPart = BodyHealthArmorCompat.resolvePart(player, source);

        // A blank optional-API result preserves the historical all-body chest behavior.
        if (externalPart == null) {
            return chest != null ? chest : head;
        }

        ProtectedBodyPart part = ProtectedBodyPart.fromExternalName(externalPart).orElse(null);
        if (part == null) {
            return chest != null ? chest : head;
        }
        if (part == ProtectedBodyPart.HEAD) {
            return protects(head, part) ? head : null;
        }
        return protects(chest, part) ? chest : null;
    }

    private static boolean protects(EquippedProtection protection, ProtectedBodyPart part) {
        return protection != null && protection.item().coverage().protects(part);
    }

    private static EquippedProtection functionalAt(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.getItem() instanceof ProtectiveArmorItem armor && armor.isFunctional(stack)) {
            return new EquippedProtection(armor, stack);
        }
        return null;
    }

    public record EquippedProtection(ProtectiveArmorItem item, ItemStack stack) {
    }

    private ArmorProtectionResolver() {
    }
}
