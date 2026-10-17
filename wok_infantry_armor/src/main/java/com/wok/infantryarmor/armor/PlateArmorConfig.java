package com.wok.infantryarmor.armor;

import net.minecraftforge.common.ForgeConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 插板护甲的服务端平衡配置。四张矩阵均按 I轻、I中、I重、II轻……VI重排列。 */
public final class PlateArmorConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("wok_infantry_armor");

    private final ForgeConfigSpec.ConfigValue<List<? extends Double>> ballisticProtection;
    private final ForgeConfigSpec.ConfigValue<List<? extends Double>> armorPiercingBuffer;
    private final ForgeConfigSpec.ConfigValue<List<? extends Double>> generalProtection;
    private final ForgeConfigSpec.ConfigValue<List<? extends Double>> pressureCapacity;
    private final ForgeConfigSpec.DoubleValue lightMovement;
    private final ForgeConfigSpec.DoubleValue mediumMovement;
    private final ForgeConfigSpec.DoubleValue heavyMovement;
    private final ForgeConfigSpec.IntValue lightHelmetIntegrity;
    private final ForgeConfigSpec.IntValue mediumHelmetIntegrity;
    private final ForgeConfigSpec.IntValue heavyHelmetIntegrity;
    private final ForgeConfigSpec.DoubleValue helmetBallisticWearScale;
    private final ForgeConfigSpec.DoubleValue helmetArmorPiercingWearMultiplier;
    private final Map<PlateArmorConstructionMaterial, MaterialProfile> materialProfiles =
            new EnumMap<>(PlateArmorConstructionMaterial.class);
    /** 每张矩阵上次报告错误时的配置代数；配置加载/重载会推进代数，从而重新允许报告一次。 */
    private final Map<String, Integer> reportedMatrixProblems = new ConcurrentHashMap<>();

    private PlateArmorConfig(ForgeConfigSpec.Builder builder) {
        builder.push("plateArmor");
        builder.comment("All 18-value matrices use order I-light, I-medium, I-heavy, then II-light ... VI-heavy.");
        ballisticProtection = builder.comment("R: protection applied to tacz:bullet and tacz:bullet_void normal ballistic segments.")
                .defineList("ballisticProtectionR", PlateArmorDefaults.BALLISTIC_PROTECTION_R, PlateArmorConfig::isRate);
        armorPiercingBuffer = builder.comment("Q: buffer applied only to tacz:bullet_ignore_armor and tacz:bullet_void_ignore_armor.")
                .defineList("armorPiercingBufferQ", PlateArmorDefaults.ARMOR_PIERCING_BUFFER_Q, PlateArmorConfig::isRate);
        generalProtection = builder.comment("G: protection of the covered portion of eligible non-TaCZ combat damage.")
                .defineList("generalProtectionG", PlateArmorDefaults.GENERAL_PROTECTION_G, PlateArmorConfig::isRate);
        pressureCapacity = builder.comment("T: per-hit covered damage capacity for eligible non-TaCZ combat damage.")
                .defineList("pressureCapacityT", PlateArmorDefaults.PRESSURE_CAPACITY_T, PlateArmorConfig::isNonNegative);

        builder.push("movement");
        lightMovement = builder.comment("Light plate movement modifier; 0.00 keeps the wearer's base speed.")
                .defineInRange("light", PlateArmorDefaults.LIGHT_MOVEMENT, -0.95D, 10.0D);
        mediumMovement = builder.comment("Medium plate movement modifier.")
                .defineInRange("medium", PlateArmorDefaults.MEDIUM_MOVEMENT, -0.95D, 10.0D);
        heavyMovement = builder.comment("Heavy plate movement modifier; -0.12 = -12%, MULTIPLY_TOTAL.")
                .defineInRange("heavy", PlateArmorDefaults.HEAVY_MOVEMENT, -0.95D, 10.0D);
        builder.pop();

        builder.push("helmetIntegrity");
        builder.comment("Helmet integrity is consumed from the bullet's actual normal/AP damage segments.",
                "TaCZ duplicate hurt segments are consolidated, so one bullet settles wear once.");
        lightHelmetIntegrity = builder.comment("Structural integrity of light helmets.")
                .defineInRange("light", PlateArmorDefaults.LIGHT_HELMET_INTEGRITY, 1, 10000);
        mediumHelmetIntegrity = builder.comment("Structural integrity of medium helmets.")
                .defineInRange("medium", PlateArmorDefaults.MEDIUM_HELMET_INTEGRITY, 1, 10000);
        heavyHelmetIntegrity = builder.comment("Structural integrity of heavy helmets.")
                .defineInRange("heavy", PlateArmorDefaults.HEAVY_HELMET_INTEGRITY, 1, 10000);
        helmetBallisticWearScale = builder.comment("Global multiplier applied to bullet structural wear.")
                .defineInRange("ballisticWearScale", PlateArmorDefaults.HELMET_BALLISTIC_WEAR_SCALE, 0.0D, 100.0D);
        helmetArmorPiercingWearMultiplier = builder.comment(
                        "Additional structural wear multiplier for the armor-piercing damage segment.")
                .defineInRange("armorPiercingWearMultiplier",
                                PlateArmorDefaults.HELMET_ARMOR_PIERCING_WEAR_MULTIPLIER, 0.0D, 100.0D);
        builder.pop();

        builder.push("materialProfiles");
        builder.comment("Version 2 material profiles. Legacy materialDurability values are intentionally not reused.",
                "Leak multipliers below 1 improve protection; values above 1 increase unblocked damage.");
        for (PlateArmorConstructionMaterial material : PlateArmorConstructionMaterial.values()) {
            builder.push(material.id());
            materialProfiles.put(material, new MaterialProfile(
                    builder.comment("Maximum durability for this construction material.")
                            .defineInRange("durability", material.defaultDurability(), 1, 100000),
                    builder.comment("Multiplier for the damage not blocked by R.")
                            .defineInRange("ballisticLeak", material.defaultBallisticLeakMultiplier(), 0.75D, 1.25D),
                    builder.comment("Multiplier for the damage not blocked by Q; a base Q of zero stays zero.")
                            .defineInRange("armorPiercingLeak", material.defaultArmorPiercingLeakMultiplier(),
                                    0.75D, 1.25D),
                    builder.comment("Multiplier for the damage not blocked by G.")
                            .defineInRange("generalLeak", material.defaultGeneralLeakMultiplier(), 0.75D, 1.25D),
                    builder.comment("Multiplier applied to T.")
                            .defineInRange("pressureCapacity", material.defaultPressureCapacityMultiplier(),
                                    0.75D, 1.25D),
                    builder.comment("Non-negative material movement penalty; 0.03 = -3%, MULTIPLY_TOTAL.")
                            .defineInRange("movementPenalty", material.defaultMovementPenalty(), 0.0D, 0.04D)));
            builder.pop();
        }
        builder.pop();
        builder.pop();
    }

    public static PlateArmorConfig define(ForgeConfigSpec.Builder builder) {
        return new PlateArmorConfig(builder);
    }

    public double ballisticProtection(PlateArmorTier tier, PlateArmorWeight weight) {
        return matrixValue("ballisticProtectionR", ballisticProtection,
                PlateArmorDefaults.BALLISTIC_PROTECTION_R, tier, weight);
    }

    public double armorPiercingBuffer(PlateArmorTier tier, PlateArmorWeight weight) {
        return matrixValue("armorPiercingBufferQ", armorPiercingBuffer,
                PlateArmorDefaults.ARMOR_PIERCING_BUFFER_Q, tier, weight);
    }

    public double generalProtection(PlateArmorTier tier, PlateArmorWeight weight) {
        return matrixValue("generalProtectionG", generalProtection,
                PlateArmorDefaults.GENERAL_PROTECTION_G, tier, weight);
    }

    public double pressureCapacity(PlateArmorTier tier, PlateArmorWeight weight) {
        return matrixValue("pressureCapacityT", pressureCapacity,
                PlateArmorDefaults.PRESSURE_CAPACITY_T, tier, weight);
    }

    public double movementModifier(PlateArmorWeight weight) {
        return switch (weight) {
            case LIGHT -> lightMovement.get();
            case MEDIUM -> mediumMovement.get();
            case HEAVY -> heavyMovement.get();
        };
    }

    public int maxDurability(PlateArmorConstructionMaterial material) {
        return profile(material).durability().get();
    }

    public int helmetMaxDurability(PlateArmorWeight weight) {
        return switch (weight) {
            case LIGHT -> lightHelmetIntegrity.get();
            case MEDIUM -> mediumHelmetIntegrity.get();
            case HEAVY -> heavyHelmetIntegrity.get();
        };
    }

    public double helmetBallisticWearScale() {
        return helmetBallisticWearScale.get();
    }

    public double helmetArmorPiercingWearMultiplier() {
        return helmetArmorPiercingWearMultiplier.get();
    }

    public double ballisticLeakMultiplier(PlateArmorConstructionMaterial material) {
        return profile(material).ballisticLeak().get();
    }

    public double armorPiercingLeakMultiplier(PlateArmorConstructionMaterial material) {
        return profile(material).armorPiercingLeak().get();
    }

    public double generalLeakMultiplier(PlateArmorConstructionMaterial material) {
        return profile(material).generalLeak().get();
    }

    public double pressureCapacityMultiplier(PlateArmorConstructionMaterial material) {
        return profile(material).pressureCapacity().get();
    }

    public double movementPenalty(PlateArmorConstructionMaterial material) {
        return profile(material).movementPenalty().get();
    }

    private MaterialProfile profile(PlateArmorConstructionMaterial material) {
        MaterialProfile values = materialProfiles.get(material);
        if (values == null) {
            throw new IllegalArgumentException("unregistered plate armor material: " + material);
        }
        return values;
    }

    /**
     * 读取矩阵的一格。Forge 只校验元素、不校验长度，而且会把不合格的元素直接删掉（列表因此变短）；
     * 不是列表或长度不是 18 时整张矩阵都按代码默认值读，只有该格不是有限数时才只回退这一格，
     * 并在每次配置加载/重载后只打一次 ERROR。这里在伤害、tick 和提示框路径上被调用，绝不能抛异常。
     */
    private double matrixValue(String name,
                               ForgeConfigSpec.ConfigValue<List<? extends Double>> configured,
                               List<Double> defaults,
                               PlateArmorTier tier,
                               PlateArmorWeight weight) {
        int index = tier.configIndex(weight);
        // 读成 Object 再判断，避免 javac 对 get() 插入隐式 checkcast List：FileWatcher 热重载在 load() 与 correct()
        // 之间，get() 可能原样返回文件里写错类型的值（例如 ballisticProtectionR = 0.9），隐式转换会抛 ClassCastException。
        Object raw = configured.get();
        if (!(raw instanceof List<?> values)) {
            reportMatrixProblem(name, "must be a list of " + PlateArmorDefaults.MATRIX_SIZE + " numbers, got "
                    + (raw == null ? "nothing" : raw.getClass().getSimpleName()));
            return defaults.get(index);
        }
        if (values.size() != PlateArmorDefaults.MATRIX_SIZE) {
            reportMatrixProblem(name, "must contain exactly " + PlateArmorDefaults.MATRIX_SIZE + " values, got "
                    + values.size());
            return defaults.get(index);
        }
        Object value = values.get(index);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            reportMatrixProblem(name, "contains a non-finite number at index " + index);
            return defaults.get(index);
        }
        return number.doubleValue();
    }

    private void reportMatrixProblem(String name, String problem) {
        int generation = ArmorConfigGeneration.current();
        Integer previous = reportedMatrixProblems.put(name, generation);
        if (previous == null || previous != generation) {
            LOGGER.error("plateArmor.{} {}; using the built-in default matrix for the affected entries "
                    + "until wok-infantry-armor.toml is fixed and reloaded.", name, problem);
        }
    }

    private static boolean isRate(Object value) {
        return value instanceof Number number
                && Double.isFinite(number.doubleValue())
                && number.doubleValue() >= 0.0D
                && number.doubleValue() < 1.0D;
    }

    private static boolean isNonNegative(Object value) {
        return value instanceof Number number
                && Double.isFinite(number.doubleValue())
                && number.doubleValue() >= 0.0D;
    }

    private record MaterialProfile(ForgeConfigSpec.IntValue durability,
                                   ForgeConfigSpec.DoubleValue ballisticLeak,
                                   ForgeConfigSpec.DoubleValue armorPiercingLeak,
                                   ForgeConfigSpec.DoubleValue generalLeak,
                                   ForgeConfigSpec.DoubleValue pressureCapacity,
                                   ForgeConfigSpec.DoubleValue movementPenalty) {
    }
}

