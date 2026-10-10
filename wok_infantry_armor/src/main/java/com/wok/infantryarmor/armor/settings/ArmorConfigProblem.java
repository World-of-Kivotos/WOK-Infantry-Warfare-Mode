package com.wok.infantryarmor.armor.settings;

import java.util.List;

/**
 * 逐件配置文件里的一个无效值：完整键路径（例如 {@code plates.plate_armor_slick.ballisticProtectionR}）、
 * 管理员写的原值和原因。无效值一律回退该字段的默认值，文件内容保持不动。
 */
public record ArmorConfigProblem(String path, String rawValue, ProblemReason reason, List<String> args) {

    public ArmorConfigProblem {
        args = List.copyOf(args);
    }

    public String describeReason() {
        return reason.describe(args);
    }

    @Override
    public String toString() {
        return path + " = " + rawValue + "：" + describeReason();
    }
}
