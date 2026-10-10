package com.wok.infantryarmor.armor;

import java.util.List;

/**
 * 插板与头盔的代码默认值和唯一一份泄漏公式。
 *
 * <p>主配置 {@link PlateArmorConfig} 的 spec 默认值、逐件配置文件的注释、以及运行时按主配置实时计算的数值
 * 都从这里取常量或调用同一个公式，避免多份公式漂移。这里只放编译期常量和纯函数，不读取任何 ConfigValue。</p>
 */
public final class PlateArmorDefaults {

    /** 四张矩阵的长度：6 个等级 × 3 个构型。 */
    public static final int MATRIX_SIZE = PlateArmorTier.values().length * PlateArmorWeight.values().length;

    public static final List<Double> BALLISTIC_PROTECTION_R = List.of(
            0.45D, 0.50D, 0.55D,
            0.60D, 0.65D, 0.70D,
            0.75D, 0.80D, 0.85D,
            0.85D, 0.88D, 0.90D,
            0.90D, 0.92D, 0.94D,
            0.94D, 0.96D, 0.98D);

    /* I 级只靠 R 区分构型；II-VI 逐步获得穿甲段缓冲，且始终显著低于同格 R。 */
    public static final List<Double> ARMOR_PIERCING_BUFFER_Q = List.of(
            0.00D, 0.00D, 0.00D,
            0.02D, 0.05D, 0.08D,
            0.08D, 0.10D, 0.15D,
            0.15D, 0.20D, 0.25D,
            0.25D, 0.35D, 0.45D,
            0.45D, 0.50D, 0.55D);

    public static final List<Double> GENERAL_PROTECTION_G = List.of(
            0.35D, 0.40D, 0.45D,
            0.45D, 0.50D, 0.55D,
            0.60D, 0.68D, 0.70D,
            0.70D, 0.76D, 0.78D,
            0.78D, 0.84D, 0.86D,
            0.86D, 0.88D, 0.90D);

    public static final List<Double> PRESSURE_CAPACITY_T = List.of(
            16.0D, 20.0D, 24.0D,
            24.0D, 32.0D, 38.0D,
            38.0D, 48.0D, 58.0D,
            58.0D, 72.0D, 84.0D,
            84.0D, 96.0D, 112.0D,
            112.0D, 128.0D, 154.0D);

    public static final double LIGHT_MOVEMENT = 0.00D;
    public static final double MEDIUM_MOVEMENT = -0.05D;
    public static final double HEAVY_MOVEMENT = -0.12D;

    public static final int LIGHT_HELMET_INTEGRITY = 60;
    public static final int MEDIUM_HELMET_INTEGRITY = 80;
    public static final int HEAVY_HELMET_INTEGRITY = 180;

    public static final double HELMET_BALLISTIC_WEAR_SCALE = 1.0D;
    public static final double HELMET_ARMOR_PIERCING_WEAR_MULTIPLIER = 2.0D;

    private PlateArmorDefaults() {
    }

    public static double movement(PlateArmorWeight weight) {
        return switch (weight) {
            case LIGHT -> LIGHT_MOVEMENT;
            case MEDIUM -> MEDIUM_MOVEMENT;
            case HEAVY -> HEAVY_MOVEMENT;
        };
    }

    public static int helmetIntegrity(PlateArmorWeight weight) {
        return switch (weight) {
            case LIGHT -> LIGHT_HELMET_INTEGRITY;
            case MEDIUM -> MEDIUM_HELMET_INTEGRITY;
            case HEAVY -> HEAVY_HELMET_INTEGRITY;
        };
    }

    /**
     * 材料泄漏公式：eff = max(0, 1 - (1 - base) × leak)，基础值为 0 时保持 0。
     * 差材料最多让该项失去全部防护，不会把命中放大成额外伤害。
     */
    public static double adjustProtection(double baseProtection, double leakMultiplier) {
        if (baseProtection == 0.0D) {
            return 0.0D;
        }
        double adjusted = 1.0D - (1.0D - baseProtection) * leakMultiplier;
        return adjusted < 0.0D ? 0.0D : adjusted;
    }

    /** 按代码默认主配置算出的属性；只用于生成逐件配置文件的注释，运行时一律按实际主配置实时计算。 */
    public static PlateArmorStats codeDefaultPlateStats(PlateArmorTier tier, PlateArmorWeight weight,
                                                        PlateArmorConstructionMaterial material) {
        int index = tier.configIndex(weight);
        return new PlateArmorStats(
                adjustProtection(BALLISTIC_PROTECTION_R.get(index), material.defaultBallisticLeakMultiplier()),
                adjustProtection(ARMOR_PIERCING_BUFFER_Q.get(index), material.defaultArmorPiercingLeakMultiplier()),
                adjustProtection(GENERAL_PROTECTION_G.get(index), material.defaultGeneralLeakMultiplier()),
                PRESSURE_CAPACITY_T.get(index) * material.defaultPressureCapacityMultiplier(),
                movement(weight) - material.defaultMovementPenalty());
    }

    /** 头盔的代码默认属性：防护同插板公式，机动默认 0（头盔不沿用构型/材料的移速）。 */
    public static PlateArmorStats codeDefaultHelmetStats(PlateArmorTier tier, PlateArmorWeight weight,
                                                         PlateArmorConstructionMaterial material) {
        PlateArmorStats plate = codeDefaultPlateStats(tier, weight, material);
        return new PlateArmorStats(plate.ballisticProtection(), plate.armorPiercingBuffer(),
                plate.generalProtection(), plate.pressureCapacity(), 0.0D);
    }
}
