package com.wok.infantryarmor.armor;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Selects exactly one equipped localized armor item for a hit.
 *
 * <p>只有安装部位血量、其开关开启、且是局部命中时才按部位判定（头部只看头盔，其余部位只看胸甲的覆盖部位，
 * 覆盖部位可被逐件配置 coverage 覆盖）；否则胸甲（没有胸甲时头盔）保护全身。</p>
 */
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
