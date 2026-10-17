package com.wok.infantryarmor.armor;

/**
 * 单件护甲在当前服务端配置下的即时属性快照。调用时读取，不跨配置重载缓存。
 *
 * <p>唯一入口是 {@link #resolve(ProtectiveArmorItem)}，它经过逐件配置解析器
 * {@link com.wok.infantryarmor.armor.settings.ArmorItemProfile}；不再提供按变体直接计算的入口，
 * 任何地方都不能绕过逐件覆盖。</p>
 */
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

    public static PlateArmorStats resolve(ProtectiveArmorItem armor) {
        return armor.settings().stats();
    }
}
