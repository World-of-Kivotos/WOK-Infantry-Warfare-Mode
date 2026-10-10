package com.wok.infantryarmor.armor.settings;

import java.util.Locale;

/** 属性修正的三种运算；与原版 AttributeModifier.Operation 一一对应，但不依赖 Minecraft 类，方便纯逻辑测试。 */
public enum ModifierOperation {
    ADDITION,
    MULTIPLY_BASE,
    MULTIPLY_TOTAL;

    /** 写进 UUID 种子和提示框语言键的稳定小写名。 */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean multiplicative() {
        return this != ADDITION;
    }
}
