package com.wok.infantryarmor.armor.settings;

/** 胸、头两个 MULTIPLY_TOTAL 移速修正的组合限制；纯函数，便于脱离游戏测试。 */
public final class ArmorMovementMath {

    /** 胸、头两个移速修正相乘后的最低倍率。 */
    public static final double MIN_COMBINED_FACTOR = 0.05D;

    private ArmorMovementMath() {
    }

    /**
     * 若 (1 + 胸) × (1 + 头) 低于 0.05，把头盔这一项收紧到乘积正好 0.05；胸甲本身已不高于 0.05 时头盔减速归零。
     * 头盔的加速（正值）不受影响；非有限值按 0 处理。
     */
    public static double limitHelmetMovement(double chestMovement, double helmetMovement) {
        if (!Double.isFinite(helmetMovement)) {
            return 0.0D;
        }
        if (helmetMovement >= 0.0D) {
            return helmetMovement;
        }
        double chestFactor = 1.0D + (Double.isFinite(chestMovement) ? chestMovement : 0.0D);
        if (chestFactor * (1.0D + helmetMovement) >= MIN_COMBINED_FACTOR) {
            return helmetMovement;
        }
        if (chestFactor <= MIN_COMBINED_FACTOR) {
            return 0.0D;
        }
        return Math.min(0.0D, MIN_COMBINED_FACTOR / chestFactor - 1.0D);
    }
}
