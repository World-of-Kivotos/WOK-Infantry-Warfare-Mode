package com.wok.infantryarmor.armor.settings;

import java.util.List;
import java.util.Locale;

/**
 * 逐件配置值无效的原因。{@link #translationKey()} 供 /wokarmor 命令按客户端语言显示；
 * {@link #describe(List)} 是同一句话的中文版，用于服务端日志和没有语言文件时的回退文字。
 */
public enum ProblemReason {
    WRONG_TYPE("类型不对：应为带引号的文字或数字"),
    NOT_A_NUMBER("不是数字"),
    NOT_FINITE("不是有限数（NaN 或 inf）"),
    OUT_OF_RANGE("超出范围 %s ~ %s"),
    CLAMPED("超出范围 %s ~ %s，已按 %s 处理"),
    RATE_LOOKS_LIKE_PERCENT("比率不能写成大于 1 的数字；85% 请写 \"0.85\" 或 \"85%\""),
    PERCENT_NOT_ALLOWED("这个键不接受百分数"),
    NOT_INTEGER("必须是整数"),
    UNKNOWN_TIER("未知等级，可用 I II III IV V VI 或 1~6"),
    UNKNOWN_WEIGHT("未知类型，可用 light medium heavy（轻型/中型/重型）"),
    UNKNOWN_MATERIAL("未知材质，可用 uhmwpe aramid armor_steel combined aluminum titanium ceramic 或其中文名"),
    COVERAGE_EMPTY("部位列表为空"),
    COVERAGE_UNKNOWN_PART("未知部位：%s"),
    COVERAGE_HEAD("插板不能保护头部（头部只由头盔保护）"),
    ATTRIBUTE_SYNTAX("格式应为 \"<属性> <操作> <数值>\""),
    ATTRIBUTE_BAD_ID("属性 ID 格式不对：%s"),
    ATTRIBUTE_UNKNOWN_OPERATION("未知操作：%s（可用 add / multiply_base / multiply_total）"),
    ATTRIBUTE_PERCENT_ADDITION("百分数只能用于乘法操作（multiply_base / multiply_total）"),
    ATTRIBUTE_MOVEMENT_FORBIDDEN("移速不能写在这里，请改用 movementModifier"),
    ATTRIBUTE_ARMOR_FORBIDDEN("%s 不能写在这里：穿着插板时原版护甲值和韧性会被清零，写了也没有效果"),
    ATTRIBUTE_AMOUNT_RANGE("数值超出范围（乘法不低于 -0.95；加法绝对值不超过 1024）"),
    ATTRIBUTE_MERGED_RANGE("同一属性、同一操作的多条合并后超出范围：%s"),
    ATTRIBUTE_UNKNOWN("没有这个属性：%s"),
    ATTRIBUTE_NOT_ON_PLAYER("玩家没有这个属性：%s");

    private final String chineseTemplate;

    ProblemReason(String chineseTemplate) {
        this.chineseTemplate = chineseTemplate;
    }

    public String translationKey() {
        return "problem.wok_infantry_armor." + name().toLowerCase(Locale.ROOT);
    }

    public String chineseTemplate() {
        return chineseTemplate;
    }

    public String describe(List<String> args) {
        return args.isEmpty() ? chineseTemplate : String.format(Locale.ROOT, chineseTemplate, args.toArray());
    }
}
