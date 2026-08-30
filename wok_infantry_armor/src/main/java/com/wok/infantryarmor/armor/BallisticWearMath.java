package com.wok.infantryarmor.armor;

/** Pure conversion from TaCZ normal/AP damage segments to structural wear. */
public final class BallisticWearMath {

    private BallisticWearMath() {
    }

    public static int wear(double normalDamage, double armorPiercingDamage,
                           double damageScale, double armorPiercingMultiplier) {
        requireNonNegativeFinite("normal damage", normalDamage);
        requireNonNegativeFinite("armor-piercing damage", armorPiercingDamage);
        requireNonNegativeFinite("damage scale", damageScale);
        requireNonNegativeFinite("armor-piercing multiplier", armorPiercingMultiplier);
        double power = (normalDamage + armorPiercingDamage * armorPiercingMultiplier) * damageScale;
        if (power <= 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(Math.min(power, Integer.MAX_VALUE)));
    }

    private static void requireNonNegativeFinite(String label, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(label + " must be finite and non-negative: " + value);
        }
    }
}
