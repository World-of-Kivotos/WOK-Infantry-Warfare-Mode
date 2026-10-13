package com.wok.infantryarmor.api;

import com.wok.infantryarmor.armor.PlateArmorCoverage;
import com.wok.infantryarmor.armor.ProtectedBodyPart;
import com.wok.infantryarmor.armor.item.HelmetItem;
import com.wok.infantryarmor.armor.item.PlateArmorItem;
import com.wok.infantryarmor.shield.item.PlasmaShieldItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Reflection-friendly optional API; it does not depend on WOK Body Health. */
public final class ArmorCoverageApi {

    public static final int API_VERSION = 2;

    private ArmorCoverageApi() {
    }

    public static boolean isCoverageConfigured(ItemStack stack) {
        return coverage(stack).configured();
    }

    public static boolean protects(ItemStack stack, String bodyPart) {
        return ProtectedBodyPart.fromExternalName(bodyPart)
                .map(part -> coverage(stack).protects(part))
                .orElse(false);
    }

    public static List<String> protectedParts(ItemStack stack) {
        return coverage(stack).parts().stream().map(ProtectedBodyPart::id).toList();
    }

    /** Current shield handler absorbs every non-bypassing damage event, independently of hit location. */
    public static boolean isFullBodyEnergyShield(ItemStack stack) {
        return stack.getItem() instanceof PlasmaShieldItem;
    }

    private static PlateArmorCoverage.Coverage coverage(ItemStack stack) {
        if (stack.getItem() instanceof PlateArmorItem plate) {
            return PlateArmorCoverage.forVariant(plate.variant());
        }
        if (stack.getItem() instanceof HelmetItem helmet) {
            return helmet.coverage();
        }
        return PlateArmorCoverage.Coverage.unconfigured();
    }
}
