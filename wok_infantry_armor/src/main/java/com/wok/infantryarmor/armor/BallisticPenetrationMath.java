package com.wok.infantryarmor.armor;

/** Maps TaCZ armor-ignore ratios to the six WOK armor penetration grades. */
public final class BallisticPenetrationMath {

    private BallisticPenetrationMath() {
    }

    public static PlateArmorTier penetrationTier(double armorIgnoreRatio) {
        if (!Double.isFinite(armorIgnoreRatio)) {
            throw new IllegalArgumentException("armor-ignore ratio must be finite: " + armorIgnoreRatio);
        }
        double ratio = Math.max(0.0D, Math.min(1.0D, armorIgnoreRatio));
        if (ratio >= 0.60D) {
            return PlateArmorTier.VI;
        }
        if (ratio >= 0.50D) {
            return PlateArmorTier.V;
        }
        if (ratio >= 0.35D) {
            return PlateArmorTier.IV;
        }
        if (ratio >= 0.20D) {
            return PlateArmorTier.III;
        }
        if (ratio >= 0.10D) {
            return PlateArmorTier.II;
        }
        return PlateArmorTier.I;
    }

    public static boolean overmatches(double armorIgnoreRatio, PlateArmorTier armorTier) {
        return penetrationTier(armorIgnoreRatio).ordinal() > armorTier.ordinal();
    }
}
