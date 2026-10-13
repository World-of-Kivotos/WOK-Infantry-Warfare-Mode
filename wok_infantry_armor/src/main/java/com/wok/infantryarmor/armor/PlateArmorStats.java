package com.wok.infantryarmor.armor;

import com.wok.infantryarmor.ArmorerConfig;

/** 单件插板在当前服务端配置下的即时属性快照。调用时读取，不跨配置重载缓存。 */
public record PlateArmorStats(double ballisticProtection,
                              double armorPiercingBuffer,
                              double generalProtection,
                              double pressureCapacity,
                              double movementModifier) {

    public PlateArmorStats withProtectionEfficiency(double efficiency) {
        if (!Double.isFinite(efficiency) || efficiency < 0.0D || efficiency > 1.0D) {
            throw new IllegalArgumentException("protection efficiency must be finite and in [0,1]: "
                    + efficiency);
        }
        return new PlateArmorStats(
                ballisticProtection * efficiency,
                armorPiercingBuffer * efficiency,
                generalProtection * efficiency,
                pressureCapacity * efficiency,
                movementModifier);
    }

    public static PlateArmorStats resolve(PlateArmorVariant variant) {
        return resolve(variant.tier(), variant.weight(), variant.material());
    }

    public static PlateArmorStats resolve(ProtectiveArmorItem armor) {
        return resolve(armor.protectionTier(), armor.protectionWeight(), armor.constructionMaterial());
    }

    private static PlateArmorStats resolve(PlateArmorTier tier, PlateArmorWeight weight,
                                           PlateArmorConstructionMaterial material) {
        PlateArmorConfig config = ArmorerConfig.PLATE_ARMOR;
        return new PlateArmorStats(
                adjustProtection(config.ballisticProtection(tier, weight),
                        config.ballisticLeakMultiplier(material)),
                adjustProtection(config.armorPiercingBuffer(tier, weight),
                        config.armorPiercingLeakMultiplier(material)),
                adjustProtection(config.generalProtection(tier, weight),
                        config.generalLeakMultiplier(material)),
                config.pressureCapacity(tier, weight) * config.pressureCapacityMultiplier(material),
                config.movementModifier(weight) - config.movementPenalty(material));
    }

    private static double adjustProtection(double baseProtection, double leakMultiplier) {
        if (baseProtection == 0.0D) {
            return 0.0D;
        }
        double adjusted = 1.0D - (1.0D - baseProtection) * leakMultiplier;
        // 差材料最多让该项失去全部防护，不会把命中放大成额外伤害。
        return adjusted < 0.0D ? 0.0D : adjusted;
    }
}

