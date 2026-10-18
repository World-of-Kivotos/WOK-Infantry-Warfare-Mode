package com.wok.infantryarmor.armor.settings;

/**
 * 语法已通过的一条（或同属性同操作合并后的）额外属性修正。属性是否存在、玩家是否拥有，
 * 要等到有注册表时由 {@link ArmorItemSettings} 再检查。
 *
 * @param attributeId 规范化后的注册名，例如 {@code minecraft:generic.knockback_resistance}
 * @param source      管理员原文；合并多条时用 “ + ” 连接
 */
public record AttributeSpec(String attributeId, ModifierOperation operation, double amount, String source) {
}
