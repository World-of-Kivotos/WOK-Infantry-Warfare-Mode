package com.wok.infantryarmor;

import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.PlateArmorConstructionMaterial;
import com.wok.infantryarmor.armor.PlateArmorCoverage;
import com.wok.infantryarmor.armor.PlateArmorDefaults;
import com.wok.infantryarmor.armor.PlateArmorStats;
import com.wok.infantryarmor.armor.PlateArmorTier;
import com.wok.infantryarmor.armor.PlateArmorVariant;
import com.wok.infantryarmor.armor.PlateArmorWeight;
import com.wok.infantryarmor.armor.ProtectedBodyPart;
import com.wok.infantryarmor.armor.settings.ArmorItemParser;
import com.wok.infantryarmor.armor.settings.ArmorLangNames;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * 第二个 SERVER 配置 wok-infantry-armor-items.toml：54 件插板和 17 件头盔的逐件覆盖。
 *
 * <p>可覆盖的键一律用宽松校验（任意文字或数字，coverage 另接受列表），保证 Forge 不会把管理员写的内容改回
 * "default" 或删掉；真正的解析、范围检查和回退在 {@link com.wok.infantryarmor.armor.settings.ArmorItemParser}
 * 里做。每节注释只用编译期常量和模组自带的 zh_cn.json 生成、数字按 {@link Locale#ROOT} 格式化，
 * 保证每台机器生成的注释逐字相同（Forge 每次加载都比对注释，不同就会重写文件）。</p>
 *
 * <p>这里只构建 spec，绝不读取任何 ConfigValue；读取只在进入世界后由解析器完成。</p>
 */
public final class ArmorItemConfig {

    public static final String FILE_NAME = "wok-infantry-armor-items.toml";
    public static final String DEFAULT = "default";

    public static final String TIER = "tier";
    public static final String WEIGHT = "weight";
    public static final String MATERIAL = "material";
    public static final String BALLISTIC_PROTECTION = "ballisticProtectionR";
    public static final String ARMOR_PIERCING_BUFFER = "armorPiercingBufferQ";
    public static final String GENERAL_PROTECTION = "generalProtectionG";
    public static final String PRESSURE_CAPACITY = "pressureCapacityT";
    public static final String COVERAGE = "coverage";
    public static final String DURABILITY = "durability";
    public static final String WEAR_MULTIPLIER = "wearMultiplier";
    public static final String MOVEMENT_MODIFIER = "movementModifier";
    public static final String ATTRIBUTE_MODIFIERS = "attributeModifiers";

    private static final String[] PLATES_HEADER = {
            "WOK步战附属-独立护甲：逐件配置（54 件插板 + 17 件头盔）。主配置 wok-infantry-armor.toml 按等级/类型/材质整类调，本文件按单件覆盖。",
            "用法：只改英文双引号里面的内容，引号不要删；数字、百分比也可以写在引号里，例如 \"0.9\"、\"85%\"、\"-10%\"。默认没有引号的键（wearMultiplier、头盔的 movementModifier）直接改数字，写成带引号的数字也可以；头盔的 movementModifier 还可以写百分比（如 \"-5%\"），wearMultiplier 不接受百分比。",
            "\"default\" = 沿用主配置 wok-infantry-armor.toml 按等级/类型/材质算出的值。数值覆盖是最终有效值，优先于分类键，不再乘材料系数。",
            "键名 ↔ 提示框：ballisticProtectionR=缓冲（普通弹道段）；armorPiercingBufferQ=抗穿（穿甲段，仅子弹穿甲等级 ≤ tier 时生效）；generalProtectionG=防护率（非 TaCZ 物理伤害）；pressureCapacityT=抗压；durability=耐久上限；movementModifier=机动修正；tier/weight/material=等级/类型/材质。",
            "取值：R/Q/G 为 0~0.99（或 \"0%\"~\"99%\"）；T 为 0~10000；durability 为 1~100000 的整数；movementModifier 为 -0.95~1.0（-0.08 或 \"-8%\" 即机动修正 -8%）；wearMultiplier 为 0~100 的数字，不接受百分比（1 = 不变，0 = 不磨损，0.5 = 平均磨损减半，小数部分按概率进位）。",
            "写错的文字或数字不会被 Forge 改掉，会留在文件里并在日志 WARN、列入 /wokarmor problems（例外：true/false 之类的其他类型会被 Forge 改回默认；attributeModifiers 列表里不带引号的条目会被 Forge 删掉、其余条目保留，不写方括号时整项被改成 []）：wearMultiplier 与头盔 movementModifier 越界时夹到边界，其余越界或写错的值一律回退 \"default\"。",
            "tier = I~VI（或 1~6）；weight = light/medium/heavy（或 轻型/中型/重型）；material = uhmwpe/aramid/armor_steel/combined/aluminum/titanium/ceramic（或中文材质名）。",
            "分类键作用：tier → 选矩阵行、子弹穿甲判定（插板与头盔相同）；weight → 选矩阵列，插板另决定基础移速，头盔另决定默认耐久（60/80/180）；material → 泄漏系数与抗压系数，插板另决定默认耐久和移速惩罚（头盔不受影响）。",
            "coverage（仅插板）：\"default\"、[\"chest\", \"abdomen\"] 或 \"chest,abdomen\"；可用 chest abdomen left_arm right_arm left_leg right_leg 或中文部位名，不能写 head。只有安装部位血量、其开关开启且是局部命中时才按部位判定，否则胸甲（没有胸甲时头盔）保护全身。",
            "attributeModifiers：每条 \"<属性 id> <操作> <数值>\"，例如 [\"generic.knockback_resistance add 0.1\", \"generic.attack_damage multiply_total -5%\"]；只有一条也必须写方括号（[\"generic.luck add 1\"]），不像 coverage 那样接受单个字符串；操作为 add / multiply_base / multiply_total；移速、原版护甲值和韧性不能写在这里。",
            "每节第一行注释是按“代码默认主配置”算出的默认值；如果本世界主配置改过，以主配置为准（游戏内提示框或 /wokarmor inspect 显示实际值）。头盔数值为满耐久标称值。",
            "不要在本文件里写自己的注释，Forge 加载/重载时会删掉；改值时在已有的节里原地改那一行，不要再写一个同名的 [plates.xxx] 节或同一个键的第二行，也不要重新输入键名或节名（拼错的键或节会被 Forge 删掉并恢复默认值，不会列入 /wokarmor problems）。TOML 语法错误（去掉了引号、节名或键重复，日志里是 \"declared twice\"/\"defined twice\"）：服务器没运行时会导致世界无法加载，改坏了可以删掉本文件让它重新生成；服务器运行中改坏时热重载失败（日志 \"Failed loading config file\"），暂时沿用旧值，但必须在关服或退出世界前改好，否则 Forge 关服时会把只剩坏行以上内容的配置写回本文件，坏行以下的覆盖全部丢失；文件坏着期间新玩家也进不了服务器。在运行中的服务器上修改前请先备份本文件。",
            "等离子护盾不在这里，见主配置 plasmaShield.balanceV4.<型号>。完整说明见模组 JAR 根目录的 CONFIG.md（用压缩软件打开 JAR 即可），或 GitHub 仓库 World-of-Kivotos/WOK-Infantry-Warfare-Mode 的 wok_infantry_armor/CONFIG.md。"
    };

    private static final String[] HELMETS_HEADER = {
            "头盔逐件覆盖（17 件）。头盔没有 coverage 键：只有安装部位血量、其开关开启且是局部命中时才按部位判定、只护头；否则（未安装部位血量、开关关闭，或爆炸、摔落等非局部伤害）没有胸甲时头盔保护全身，也会因此磨损。",
            "头盔的 movementModifier 默认 0，不沿用构型或材质的移速；与胸甲移速相乘低于 0.05 倍时会自动收紧头盔这一项。"
    };

    /** 可覆盖的键：任意文字或数字都先接受，留给解析器判断，Forge 不会改写管理员的内容。 */
    private static final Predicate<Object> LOOSE = value -> value instanceof String || value instanceof Number;
    private static final Predicate<Object> LOOSE_OR_LIST = value -> LOOSE.test(value) || value instanceof List;

    public static final ForgeConfigSpec SPEC;
    public static final ArmorItemConfig ITEMS;

    static {
        Built built = create();
        ITEMS = built.items();
        SPEC = built.spec();
    }

    private final Entry[] plates = new Entry[PlateArmorVariant.values().length];
    private final Entry[] helmets = new Entry[HelmetVariant.values().length];

    private ArmorItemConfig(ForgeConfigSpec.Builder builder) {
        builder.comment(PLATES_HEADER).push("plates");
        for (PlateArmorVariant variant : PlateArmorVariant.values()) {
            builder.comment(plateComment(variant)).push(variant.itemId());
            plates[variant.ordinal()] = new Entry(builder, "plates." + variant.itemId(), true);
            builder.pop();
        }
        builder.pop();

        builder.comment(HELMETS_HEADER).push("helmets");
        for (HelmetVariant variant : HelmetVariant.values()) {
            builder.comment(helmetComment(variant)).push(variant.itemId());
            helmets[variant.ordinal()] = new Entry(builder, "helmets." + variant.itemId(), false);
            builder.pop();
        }
        builder.pop();
    }

    /** 新建一份独立的 spec；正式运行只用静态的 {@link #SPEC}，测试可多次调用以核对生成结果是否稳定。 */
    public static Built create() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ArmorItemConfig items = new ArmorItemConfig(builder);
        return new Built(items, builder.build());
    }

    public Entry plate(PlateArmorVariant variant) {
        return plates[variant.ordinal()];
    }

    public Entry helmet(HelmetVariant variant) {
        return helmets[variant.ordinal()];
    }

    public record Built(ArmorItemConfig items, ForgeConfigSpec spec) {
    }

    /** 一件护甲的全部键。插板的 coverage 非空，头盔为 null；全部可覆盖键都是宽松校验。 */
    public static final class Entry {
        private final String path;
        private final boolean plate;
        public final ForgeConfigSpec.ConfigValue<Object> tier;
        public final ForgeConfigSpec.ConfigValue<Object> weight;
        public final ForgeConfigSpec.ConfigValue<Object> material;
        public final ForgeConfigSpec.ConfigValue<Object> ballisticProtection;
        public final ForgeConfigSpec.ConfigValue<Object> armorPiercingBuffer;
        public final ForgeConfigSpec.ConfigValue<Object> generalProtection;
        public final ForgeConfigSpec.ConfigValue<Object> pressureCapacity;
        public final ForgeConfigSpec.ConfigValue<Object> coverage;
        public final ForgeConfigSpec.ConfigValue<Object> durability;
        /** 宽松值，默认写成数字 1.0；越界由解析器夹到 [0, 100] 并记录问题。 */
        public final ForgeConfigSpec.ConfigValue<Object> wearMultiplier;
        /**
         * 宽松值。插板默认 "default"（沿用构型 − 材料惩罚），越界回退默认；
         * 头盔默认写成数字 0.0，越界由解析器夹到 [-0.95, 1.0]。两者都接受数字、数字文字和百分数。
         */
        public final ForgeConfigSpec.ConfigValue<Object> movementModifier;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> attributeModifiers;

        private Entry(ForgeConfigSpec.Builder builder, String path, boolean plate) {
            this.path = path;
            this.plate = plate;
            tier = loose(builder, TIER);
            weight = loose(builder, WEIGHT);
            material = loose(builder, MATERIAL);
            ballisticProtection = loose(builder, BALLISTIC_PROTECTION);
            armorPiercingBuffer = loose(builder, ARMOR_PIERCING_BUFFER);
            generalProtection = loose(builder, GENERAL_PROTECTION);
            pressureCapacity = loose(builder, PRESSURE_CAPACITY);
            coverage = plate ? builder.<Object>define(COVERAGE, () -> DEFAULT, LOOSE_OR_LIST) : null;
            durability = loose(builder, DURABILITY);
            // 不用 defineInRange：Forge 的 Range 修不了 NaN（每次加载都判“不正确”并重写、热重载时反复备份），
            // 还会把带引号的数字静默改回默认。改成宽松校验，夹取与 WARN 统一由解析器负责。
            wearMultiplier = builder.<Object>define(WEAR_MULTIPLIER,
                    () -> ArmorItemParser.WEAR_DEFAULT, LOOSE);
            movementModifier = plate
                    ? loose(builder, MOVEMENT_MODIFIER)
                    : builder.<Object>define(MOVEMENT_MODIFIER, () -> ArmorItemParser.HELMET_MOVEMENT_DEFAULT, LOOSE);
            attributeModifiers = builder.defineListAllowEmpty(List.of(ATTRIBUTE_MODIFIERS),
                    () -> List.<String>of(), value -> value instanceof String);
        }

        /** 完整键路径前缀，例如 {@code plates.plate_armor_slick}。 */
        public String path() {
            return path;
        }

        public boolean isPlate() {
            return plate;
        }

        private static ForgeConfigSpec.ConfigValue<Object> loose(ForgeConfigSpec.Builder builder, String key) {
            return builder.<Object>define(key, () -> DEFAULT, LOOSE);
        }
    }

    private static String plateComment(PlateArmorVariant variant) {
        PlateArmorStats stats = PlateArmorDefaults.codeDefaultPlateStats(
                variant.tier(), variant.weight(), variant.material());
        return header(variant.itemId(), variant.tier(), variant.weight(), variant.material())
                + " | " + statsText(stats, variant.material().defaultDurability())
                + " | 部位=" + partsText(PlateArmorCoverage.forVariant(variant));
    }

    private static String helmetComment(HelmetVariant variant) {
        PlateArmorStats stats = PlateArmorDefaults.codeDefaultHelmetStats(
                variant.tier(), variant.weight(), variant.material());
        return header(variant.itemId(), variant.tier(), variant.weight(), variant.material())
                + " | " + statsText(stats, PlateArmorDefaults.helmetIntegrity(variant.weight()))
                + " | 部位=头（按部位判定时）；否则没有胸甲时护全身";
    }

    private static String header(String itemId, PlateArmorTier tier, PlateArmorWeight weight,
                                 PlateArmorConstructionMaterial material) {
        return ArmorLangNames.itemName(itemId) + "（" + WokInfantryArmorMod.MODID + ":" + itemId + "）"
                + " | " + tier.name() + "级 "
                + ArmorLangNames.get(weight.translationKey(), weight.id()) + " "
                + ArmorLangNames.get(material.translationKey(), material.id())
                + " | 主配置矩阵第" + (tier.configIndex(weight) + 1) + "项";
    }

    private static String statsText(PlateArmorStats stats, int durability) {
        return "缓冲R=" + number(stats.ballisticProtection())
                + " 抗穿Q=" + number(stats.armorPiercingBuffer())
                + " 防护率G=" + number(stats.generalProtection())
                + " 抗压T=" + number(stats.pressureCapacity())
                + " 耐久=" + durability
                + " 机动=" + signedPercent(stats.movementModifier());
    }

    private static String partsText(PlateArmorCoverage.Coverage coverage) {
        if (!coverage.configured() || coverage.parts().isEmpty()) {
            return "未配置";
        }
        StringBuilder text = new StringBuilder();
        for (ProtectedBodyPart part : coverage.parts()) {
            if (!text.isEmpty()) {
                text.append('、');
            }
            text.append(ArmorLangNames.get(part.translationKey(), part.id()));
        }
        return text.toString();
    }

    /** 最多 4 位小数，去掉末尾的 0，固定 Locale.ROOT。 */
    static String number(double value) {
        return trim(String.format(Locale.ROOT, "%.4f", value));
    }

    static String signedPercent(double value) {
        String text = trim(String.format(Locale.ROOT, "%.2f", value * 100.0D));
        if (text.equals("-0") || text.equals("0")) {
            return "0%";
        }
        return (value > 0.0D ? "+" : "") + text + "%";
    }

    private static String trim(String formatted) {
        int end = formatted.length();
        if (formatted.indexOf('.') >= 0) {
            while (end > 0 && formatted.charAt(end - 1) == '0') {
                end--;
            }
            if (end > 0 && formatted.charAt(end - 1) == '.') {
                end--;
            }
        }
        String result = formatted.substring(0, end);
        return result.equals("-0") ? "0" : result;
    }
}
