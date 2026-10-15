package com.wok.infantryarmor.armor.settings;

import com.wok.infantryarmor.ArmorerConfig;
import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.PlateArmorConfig;
import com.wok.infantryarmor.armor.PlateArmorConstructionMaterial;
import com.wok.infantryarmor.armor.PlateArmorCoverage;
import com.wok.infantryarmor.armor.PlateArmorDefaults;
import com.wok.infantryarmor.armor.PlateArmorStats;
import com.wok.infantryarmor.armor.PlateArmorTier;
import com.wok.infantryarmor.armor.PlateArmorVariant;
import com.wok.infantryarmor.armor.PlateArmorWeight;
import com.wok.infantryarmor.armor.ProtectedBodyPart;

import java.util.List;

/**
 * 一件插板或头盔“解析后的逐件设置”，是等级/类型/材质、防护数值、耐久、磨损、移速和覆盖部位的唯一解析入口。
 *
 * <p>对象本身只保存来自逐件配置文件 wok-infantry-armor-items.toml 的覆盖值（不可变）。没有覆盖的字段在每次调用时
 * 通过 {@link ArmorerConfig#PLATE_ARMOR} 的 getter 按主配置实时计算、不缓存，所以主配置热重载在下一次读取时就生效。
 * 覆盖值是最终有效值：R/Q/G/T 覆盖后不再乘材料泄漏或抗压系数（头盔仍会乘耐久状态效率）。</p>
 *
 * <p>分类键的作用：</p>
 * <ul>
 *     <li>tier：选主配置矩阵的行；子弹穿甲判定。</li>
 *     <li>weight：选矩阵的列；插板的基础移速；头盔的默认耐久（60/80/180）。</li>
 *     <li>material：泄漏系数与抗压系数；插板另外决定默认耐久和移速惩罚（头盔不受影响）。</li>
 * </ul>
 */
public final class ArmorItemProfile {

    private static final PlateArmorCoverage.Coverage HEAD_COVERAGE =
            PlateArmorCoverage.Coverage.of(ProtectedBodyPart.HEAD);

    public enum Kind {
        PLATE("plates", "chest"),
        HELMET("helmets", "head");

        private final String section;
        private final String slot;

        Kind(String section, String slot) {
            this.section = section;
            this.slot = slot;
        }

        /** 逐件配置文件里的分组表名。 */
        public String section() {
            return section;
        }

        /** 写进额外属性 UUID 种子的槽位名。 */
        public String slot() {
            return slot;
        }
    }

    private final Kind kind;
    private final String itemId;
    private final PlateArmorVariant plateVariant;
    private final PlateArmorTier baseTier;
    private final PlateArmorWeight baseWeight;
    private final PlateArmorConstructionMaterial baseMaterial;

    private final PlateArmorTier tierOverride;
    private final PlateArmorWeight weightOverride;
    private final PlateArmorConstructionMaterial materialOverride;
    private final Double ballisticOverride;
    private final Double armorPiercingOverride;
    private final Double generalOverride;
    private final Double pressureCapacityOverride;
    private final PlateArmorCoverage.Coverage coverageOverride;
    private final Integer durabilityOverride;
    private final double wearMultiplier;
    /** 插板：覆盖值，null 表示沿用“构型 − 材料惩罚”；头盔：始终是配置里的数字（默认 0）。 */
    private final Double movementOverride;
    private final List<AttributeSpec> attributes;
    private final List<ArmorConfigProblem> attributeProblems;

    private ArmorItemProfile(Builder builder) {
        this.kind = builder.kind;
        this.itemId = builder.itemId;
        this.plateVariant = builder.plateVariant;
        this.baseTier = builder.baseTier;
        this.baseWeight = builder.baseWeight;
        this.baseMaterial = builder.baseMaterial;
        this.tierOverride = builder.tier;
        this.weightOverride = builder.weight;
        this.materialOverride = builder.material;
        this.ballisticOverride = builder.ballistic;
        this.armorPiercingOverride = builder.armorPiercing;
        this.generalOverride = builder.general;
        this.pressureCapacityOverride = builder.pressureCapacity;
        this.coverageOverride = builder.coverage;
        this.durabilityOverride = builder.durability;
        this.wearMultiplier = builder.wearMultiplier;
        this.movementOverride = kind == Kind.HELMET
                ? Double.valueOf(builder.movement == null ? 0.0D : builder.movement)
                : builder.movement;
        this.attributes = List.copyOf(builder.attributes);
        this.attributeProblems = List.copyOf(builder.attributeProblems);
    }

    public static Builder plate(PlateArmorVariant variant) {
        return new Builder(Kind.PLATE, variant.itemId(), variant, variant.tier(), variant.weight(),
                variant.material());
    }

    public static Builder helmet(HelmetVariant variant) {
        return new Builder(Kind.HELMET, variant.itemId(), null, variant.tier(), variant.weight(),
                variant.material());
    }

    /** 没有任何覆盖的设置，等同 1.2.0-beta.2 的整类默认行为。 */
    public static ArmorItemProfile defaults(PlateArmorVariant variant) {
        return plate(variant).build();
    }

    public static ArmorItemProfile defaults(HelmetVariant variant) {
        return helmet(variant).build();
    }

    public Kind kind() {
        return kind;
    }

    public String itemId() {
        return itemId;
    }

    /** 逐件配置文件里本件的完整键路径前缀，例如 {@code plates.plate_armor_slick}。 */
    public String configPath() {
        return kind.section() + "." + itemId;
    }

    public PlateArmorTier tier() {
        return tierOverride != null ? tierOverride : baseTier;
    }

    public PlateArmorWeight weight() {
        return weightOverride != null ? weightOverride : baseWeight;
    }

    public PlateArmorConstructionMaterial material() {
        return materialOverride != null ? materialOverride : baseMaterial;
    }

    public boolean tierOverridden() {
        return tierOverride != null;
    }

    public boolean weightOverridden() {
        return weightOverride != null;
    }

    public boolean materialOverridden() {
        return materialOverride != null;
    }

    public boolean ballisticOverridden() {
        return ballisticOverride != null;
    }

    public boolean armorPiercingOverridden() {
        return armorPiercingOverride != null;
    }

    public boolean generalOverridden() {
        return generalOverride != null;
    }

    public boolean pressureCapacityOverridden() {
        return pressureCapacityOverride != null;
    }

    public boolean durabilityOverridden() {
        return durabilityOverride != null;
    }

    public boolean coverageOverridden() {
        return coverageOverride != null;
    }

    /** 插板：是否覆盖了移速；头盔：配置值是否不为 0。 */
    public boolean movementOverridden() {
        return kind == Kind.HELMET ? movementOverride != 0.0D : movementOverride != null;
    }

    /** 满耐久的标称防护数值与移速；头盔的实际防护还要乘 {@code protectionEfficiency(stack)}。 */
    public PlateArmorStats stats() {
        PlateArmorConfig config = ArmorerConfig.PLATE_ARMOR;
        PlateArmorTier tier = tier();
        PlateArmorWeight weight = weight();
        PlateArmorConstructionMaterial material = material();
        double ballistic = ballisticOverride != null
                ? ballisticOverride
                : PlateArmorDefaults.adjustProtection(config.ballisticProtection(tier, weight),
                config.ballisticLeakMultiplier(material));
        double armorPiercing = armorPiercingOverride != null
                ? armorPiercingOverride
                : PlateArmorDefaults.adjustProtection(config.armorPiercingBuffer(tier, weight),
                config.armorPiercingLeakMultiplier(material));
        double general = generalOverride != null
                ? generalOverride
                : PlateArmorDefaults.adjustProtection(config.generalProtection(tier, weight),
                config.generalLeakMultiplier(material));
        double pressureCapacity = pressureCapacityOverride != null
                ? pressureCapacityOverride
                : config.pressureCapacity(tier, weight) * config.pressureCapacityMultiplier(material);
        return new PlateArmorStats(ballistic, armorPiercing, general, pressureCapacity, movementModifier());
    }

    /**
     * 本件自己的移速修正（MULTIPLY_TOTAL）。插板默认 = 主配置构型移速 − 材料惩罚，不夹取；
     * 头盔默认 0，不沿用构型或材料。实际施加时头盔还会受“胸 × 头 ≥ 0.05”的收紧。
     */
    public double movementModifier() {
        if (movementOverride != null) {
            return movementOverride;
        }
        PlateArmorConfig config = ArmorerConfig.PLATE_ARMOR;
        return config.movementModifier(weight()) - config.movementPenalty(material());
    }

    /** 耐久上限。默认：插板按解析后材质的 materialProfiles.durability，头盔按解析后类型的 helmetIntegrity。 */
    public int maxDurability() {
        if (durabilityOverride != null) {
            return durabilityOverride;
        }
        PlateArmorConfig config = ArmorerConfig.PLATE_ARMOR;
        return kind == Kind.PLATE ? config.maxDurability(material()) : config.helmetMaxDurability(weight());
    }

    public double wearMultiplier() {
        return wearMultiplier;
    }

    /**
     * 覆盖部位。插板默认取 {@link PlateArmorCoverage} 的整表；头盔固定只护头。
     * 只有安装部位血量、其开关开启且是局部命中时才按部位判定；否则胸甲（没有胸甲时头盔）保护全身。
     */
    public PlateArmorCoverage.Coverage coverage() {
        if (kind == Kind.HELMET) {
            return HEAD_COVERAGE;
        }
        return coverageOverride != null ? coverageOverride : PlateArmorCoverage.forVariant(plateVariant);
    }

    /** 语法有效（尚未经注册表检查）的额外属性，已按（属性, 操作）合并。 */
    public List<AttributeSpec> attributes() {
        return attributes;
    }

    /** 本件 attributeModifiers 里语法层面被忽略的条目。 */
    public List<ArmorConfigProblem> attributeProblems() {
        return attributeProblems;
    }

    public static final class Builder {
        private final Kind kind;
        private final String itemId;
        private final PlateArmorVariant plateVariant;
        private final PlateArmorTier baseTier;
        private final PlateArmorWeight baseWeight;
        private final PlateArmorConstructionMaterial baseMaterial;
        private PlateArmorTier tier;
        private PlateArmorWeight weight;
        private PlateArmorConstructionMaterial material;
        private Double ballistic;
        private Double armorPiercing;
        private Double general;
        private Double pressureCapacity;
        private PlateArmorCoverage.Coverage coverage;
        private Integer durability;
        private double wearMultiplier = 1.0D;
        private Double movement;
        private List<AttributeSpec> attributes = List.of();
        private List<ArmorConfigProblem> attributeProblems = List.of();

        private Builder(Kind kind, String itemId, PlateArmorVariant plateVariant, PlateArmorTier baseTier,
                        PlateArmorWeight baseWeight, PlateArmorConstructionMaterial baseMaterial) {
            this.kind = kind;
            this.itemId = itemId;
            this.plateVariant = plateVariant;
            this.baseTier = baseTier;
            this.baseWeight = baseWeight;
            this.baseMaterial = baseMaterial;
        }

        public Builder tier(PlateArmorTier value) {
            this.tier = value;
            return this;
        }

        public Builder weight(PlateArmorWeight value) {
            this.weight = value;
            return this;
        }

        public Builder material(PlateArmorConstructionMaterial value) {
            this.material = value;
            return this;
        }

        public Builder ballisticProtection(Double value) {
            this.ballistic = value;
            return this;
        }

        public Builder armorPiercingBuffer(Double value) {
            this.armorPiercing = value;
            return this;
        }

        public Builder generalProtection(Double value) {
            this.general = value;
            return this;
        }

        public Builder pressureCapacity(Double value) {
            this.pressureCapacity = value;
            return this;
        }

        public Builder coverage(PlateArmorCoverage.Coverage value) {
            this.coverage = kind == Kind.PLATE ? value : null;
            return this;
        }

        public Builder durability(Integer value) {
            this.durability = value;
            return this;
        }

        public Builder wearMultiplier(double value) {
            this.wearMultiplier = value;
            return this;
        }

        public Builder movementModifier(Double value) {
            this.movement = value;
            return this;
        }

        public Builder attributes(List<AttributeSpec> value, List<ArmorConfigProblem> problems) {
            this.attributes = value;
            this.attributeProblems = problems;
            return this;
        }

        public ArmorItemProfile build() {
            return new ArmorItemProfile(this);
        }
    }
}
