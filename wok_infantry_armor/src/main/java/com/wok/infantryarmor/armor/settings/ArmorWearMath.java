package com.wok.infantryarmor.armor.settings;

import java.util.function.DoubleSupplier;

/**
 * 逐件磨损倍率的纯计算。先由调用方算出现行整数磨损 legacy（含“每次至少 1”），再乘倍率 m：
 * m == 1 时原样返回（与 1.2.0-beta.2 逐位一致，也不消耗随机数）；m == 0 时不磨损；
 * 其余情况 s = legacy × m，磨损 = floor(s)，再以 frac(s) 的概率 +1，期望值正好是 legacy × m。
 * 全程用 long/double 饱和运算，结果夹到 int。
 */
public final class ArmorWearMath {

    private ArmorWearMath() {
    }

    /** 插板普通伤害（以及子弹结算时的 N+AP）现行磨损：max(1, floor(X / 4))；输入非有限或不为正时为 0。 */
    public static long plateLegacyWear(double incomingDamage) {
        if (!Double.isFinite(incomingDamage) || incomingDamage <= 0.0D) {
            return 0L;
        }
        return Math.max(1L, (long) Math.floor(incomingDamage / 4.0D));
    }

    /** 头盔普通伤害现行磨损：max(1, ceil(X))；输入非有限或不为正时为 0。 */
    public static long helmetLegacyWear(double incomingDamage) {
        if (!Double.isFinite(incomingDamage) || incomingDamage <= 0.0D) {
            return 0L;
        }
        return Math.max(1L, (long) Math.ceil(incomingDamage));
    }

    /**
     * @param random 只在需要按概率进位时调用，返回 [0, 1) 的随机数（服务端用穿戴者的 getRandom()）
     */
    public static int scale(long legacyWear, double multiplier, DoubleSupplier random) {
        if (legacyWear <= 0L || !Double.isFinite(multiplier) || multiplier <= 0.0D) {
            return 0;
        }
        if (multiplier == 1.0D) {
            return (int) Math.min(legacyWear, Integer.MAX_VALUE);
        }
        double scaled = (double) legacyWear * multiplier;
        if (!(scaled < Integer.MAX_VALUE)) {
            return Integer.MAX_VALUE;
        }
        double whole = Math.floor(scaled);
        double fraction = scaled - whole;
        long wear = (long) whole;
        if (fraction > 0.0D && random.getAsDouble() < fraction) {
            wear++;
        }
        return (int) Math.min(wear, Integer.MAX_VALUE);
    }
}
