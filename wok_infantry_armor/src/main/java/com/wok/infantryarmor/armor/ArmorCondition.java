package com.wok.infantryarmor.armor;

/** Pure durability-to-protection curve used by localized head armor. */
public final class ArmorCondition {

    private ArmorCondition() {
    }

    public static double remainingRatio(int damage, int maxDamage) {
        if (maxDamage <= 0) {
            return 0.0D;
        }
        int clampedDamage = Math.max(0, Math.min(damage, maxDamage));
        return (double) (maxDamage - clampedDamage) / (double) maxDamage;
    }

    /**
     * Preserves near-nominal protection while condition is high, then falls
     * progressively faster as the armor approaches failure.
     */
    public static double protectionEfficiency(double remainingRatio) {
        double condition = Math.max(0.0D, Math.min(remainingRatio, 1.0D));
        return condition * (2.0D - condition);
    }
}
