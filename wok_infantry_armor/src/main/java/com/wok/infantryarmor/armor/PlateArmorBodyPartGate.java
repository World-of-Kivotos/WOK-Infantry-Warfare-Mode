package com.wok.infantryarmor.armor;

import com.wok.infantryarmor.armor.item.PlateArmorItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/** Decides whether the equipped plate armor covers this localized hit. */
public final class PlateArmorBodyPartGate {

    public static boolean protects(
            Player player, DamageSource source, PlateArmorItem armor) {
        String externalPart = BodyHealthArmorCompat.resolvePart(player, source);
        if (externalPart == null) {
            return true;
        }

        PlateArmorCoverage.Coverage coverage = PlateArmorCoverage.forVariant(armor.variant());
        if (!coverage.configured()) {
            return true;
        }
        return ProtectedBodyPart.fromExternalName(externalPart)
                .map(coverage::protects)
                .orElse(true);
    }

    private PlateArmorBodyPartGate() {
    }
}
