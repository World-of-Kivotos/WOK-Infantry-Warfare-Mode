package com.wok.infantryarmor.armor.settings;

import com.wok.infantryarmor.ArmorItemConfig;
import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.PlateArmorConstructionMaterial;
import com.wok.infantryarmor.armor.PlateArmorCoverage;
import com.wok.infantryarmor.armor.PlateArmorTier;
import com.wok.infantryarmor.armor.PlateArmorVariant;
import com.wok.infantryarmor.armor.PlateArmorWeight;
import com.wok.infantryarmor.armor.ProtectedBodyPart;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 逐件配置文件的纯解析层：把 ConfigValue 里的原始对象（String / Number / List）解析成覆盖值，
 * 非法值回退默认并记录 {@link ArmorConfigProblem}。不依赖 Minecraft 类和注册表，可以脱离游戏单独测试；
 * 属性是否存在、玩家是否拥有由 {@link ArmorItemSettings} 在有注册表时再检查。
 */
public final class ArmorItemParser {

    private static final Pattern NUMBER = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?");
    private static final Pattern LIST_SEPARATOR = Pattern.compile("[,;、\\s]+");
    private static final Pattern ATTRIBUTE_SEPARATOR = Pattern.compile("[,\\s]+");
    private static final Pattern RESOURCE_NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern RESOURCE_PATH = Pattern.compile("[a-z0-9_./-]+");

    public static final double RATE_MIN = 0.0D;
    public static final double RATE_MAX = 0.99D;
    public static final double CAPACITY_MIN = 0.0D;
    public static final double CAPACITY_MAX = 10000.0D;
    public static final int DURABILITY_MIN = 1;
    public static final int DURABILITY_MAX = 100000;
    public static final double MOVEMENT_MIN = -0.95D;
    public static final double MOVEMENT_MAX = 1.0D;
    public static final double HELMET_MOVEMENT_DEFAULT = 0.0D;
    public static final double WEAR_DEFAULT = 1.0D;
    public static final double WEAR_MIN = 0.0D;
    public static final double WEAR_MAX = 100.0D;
    public static final double MULTIPLY_MIN = -0.95D;
    public static final double ADDITION_LIMIT = 1024.0D;

    public static final String MOVEMENT_SPEED_ID = "minecraft:generic.movement_speed";
    public static final String ARMOR_ID = "minecraft:generic.armor";
    public static final String ARMOR_TOUGHNESS_ID = "minecraft:generic.armor_toughness";

    private ArmorItemParser() {
    }

    /** 解析整个逐件配置文件的结果：每件的设置（按变体 ordinal）和全部问题。 */
    public record ParsedItems(ArmorItemProfile[] plates, ArmorItemProfile[] helmets,
                              List<ArmorConfigProblem> problems) {

        public ArmorItemProfile plate(PlateArmorVariant variant) {
            return plates[variant.ordinal()];
        }

        public ArmorItemProfile helmet(HelmetVariant variant) {
            return helmets[variant.ordinal()];
        }
    }

    /** 读取逐件配置的全部 ConfigValue 并解析；只能在配置已加载后、在服务端线程或渲染线程上调用。 */
    public static ParsedItems parse(ArmorItemConfig config) {
        List<ArmorConfigProblem> problems = new ArrayList<>();
        ArmorItemProfile[] plates = new ArmorItemProfile[PlateArmorVariant.values().length];
        for (PlateArmorVariant variant : PlateArmorVariant.values()) {
            plates[variant.ordinal()] = parseEntry(ArmorItemProfile.plate(variant), config.plate(variant), problems);
        }
        ArmorItemProfile[] helmets = new ArmorItemProfile[HelmetVariant.values().length];
        for (HelmetVariant variant : HelmetVariant.values()) {
            helmets[variant.ordinal()] = parseEntry(ArmorItemProfile.helmet(variant), config.helmet(variant),
                    problems);
        }
        return new ParsedItems(plates, helmets, List.copyOf(problems));
    }

    /** 全部沿用默认、没有任何问题的结果；用于还没收到逐件配置（或客户端已断线）的时候，不读取任何 ConfigValue。 */
    public static ParsedItems defaults() {
        ArmorItemProfile[] plates = new ArmorItemProfile[PlateArmorVariant.values().length];
        for (PlateArmorVariant variant : PlateArmorVariant.values()) {
            plates[variant.ordinal()] = ArmorItemProfile.defaults(variant);
        }
        ArmorItemProfile[] helmets = new ArmorItemProfile[HelmetVariant.values().length];
        for (HelmetVariant variant : HelmetVariant.values()) {
            helmets[variant.ordinal()] = ArmorItemProfile.defaults(variant);
        }
        return new ParsedItems(plates, helmets, List.of());
    }

    private static ArmorItemProfile parseEntry(ArmorItemProfile.Builder builder, ArmorItemConfig.Entry entry,
                                               List<ArmorConfigProblem> problems) {
        String path = entry.path() + ".";
        builder.tier(parseTier(entry.tier.get(), path + ArmorItemConfig.TIER, problems));
        builder.weight(parseWeight(entry.weight.get(), path + ArmorItemConfig.WEIGHT, problems));
        builder.material(parseMaterial(entry.material.get(), path + ArmorItemConfig.MATERIAL, problems));
        builder.ballisticProtection(parseRate(entry.ballisticProtection.get(),
                path + ArmorItemConfig.BALLISTIC_PROTECTION, problems));
        builder.armorPiercingBuffer(parseRate(entry.armorPiercingBuffer.get(),
                path + ArmorItemConfig.ARMOR_PIERCING_BUFFER, problems));
        builder.generalProtection(parseRate(entry.generalProtection.get(),
                path + ArmorItemConfig.GENERAL_PROTECTION, problems));
        builder.pressureCapacity(parseCapacity(entry.pressureCapacity.get(),
                path + ArmorItemConfig.PRESSURE_CAPACITY, problems));
        if (entry.coverage != null) {
            builder.coverage(parseCoverage(entry.coverage.get(), path + ArmorItemConfig.COVERAGE, problems));
        }
        builder.durability(parseDurability(entry.durability.get(), path + ArmorItemConfig.DURABILITY, problems));
        builder.wearMultiplier(parseClamped(entry.wearMultiplier.get(), path + ArmorItemConfig.WEAR_MULTIPLIER,
                WEAR_DEFAULT, WEAR_MIN, WEAR_MAX, false, problems));
        if (entry.isPlate()) {
            builder.movementModifier(parseMovement(entry.movementModifier.get(),
                    path + ArmorItemConfig.MOVEMENT_MODIFIER, problems));
        } else {
            builder.movementModifier(parseClamped(entry.movementModifier.get(),
                    path + ArmorItemConfig.MOVEMENT_MODIFIER, HELMET_MOVEMENT_DEFAULT, MOVEMENT_MIN, MOVEMENT_MAX,
                    true, problems));
        }
        List<ArmorConfigProblem> attributeProblems = new ArrayList<>();
        // 先读成 Object 再判断类型，不能直接把 get() 交给 List 参数：那样 javac 会插入隐式 checkcast。
        // Forge 的 FileWatcher 热重载是先把新文件原地 load 进 spec、再 correct()，这中间 ConfigValue 缓存已清空，
        // get() 会原样返回文件里的值（例如忘了方括号的单个字符串），隐式 checkcast 就会在玩家 tick 里抛异常。
        // 不是列表时按空列表处理：Forge 随后的 correct() 会把它改成 [] 并打自己的 "Incorrect key" 警告，
        // 之后的 Reloading 会让快照过期重建。
        Object rawAttributes = entry.attributeModifiers.get();
        List<?> attributeList = rawAttributes instanceof List<?> list ? list : List.of();
        List<AttributeSpec> attributes = parseAttributes(attributeList,
                path + ArmorItemConfig.ATTRIBUTE_MODIFIERS, attributeProblems);
        problems.addAll(attributeProblems);
        builder.attributes(attributes, attributeProblems);
        return builder.build();
    }

    // ---- 单个字段 ----

    /**
     * wearMultiplier 与头盔 movementModifier：越界时夹到边界（不是回退默认），并记一条 {@link ProblemReason#CLAMPED}。
     * "default"、文字或数字都接受；不是数字、NaN/inf 或不允许的百分数回退 {@code fallback} 并记录问题。
     * 这两个键在 spec 里同样是宽松校验，Forge 不会改写它们，所以 NaN 也会走到这里而不是被 Forge 反复“纠正”。
     */
    public static double parseClamped(Object raw, String path, double fallback, double min, double max,
                                      boolean allowPercent, List<ArmorConfigProblem> problems) {
        try {
            ParsedNumber number = number(raw, allowPercent);
            if (number == null) {
                return fallback;
            }
            double value = number.value();
            if (min <= value && value <= max) {
                return value;
            }
            double clamped = value < min ? min : max;
            problems.add(new ArmorConfigProblem(path, describeRaw(raw), ProblemReason.CLAMPED,
                    List.of(plain(min), plain(max), plain(clamped))));
            return clamped;
        } catch (Invalid invalid) {
            problems.add(invalid.toProblem(path, raw));
            return fallback;
        }
    }

    /** R/Q/G：[0, 0.99]，接受百分数；大于 1 的裸数字（如 "85"）视为写错，不自动当百分数。null = 沿用默认。 */
    public static Double parseRate(Object raw, String path, List<ArmorConfigProblem> problems) {
        try {
            ParsedNumber number = number(raw, true);
            if (number == null) {
                return null;
            }
            double value = number.value();
            if (!number.percent() && value > 1.0D && value <= 100.0D) {
                throw new Invalid(ProblemReason.RATE_LOOKS_LIKE_PERCENT);
            }
            return checkRange(value, RATE_MIN, RATE_MAX);
        } catch (Invalid invalid) {
            problems.add(invalid.toProblem(path, raw));
            return null;
        }
    }

    /** T：[0, 10000]，不接受百分数。 */
    public static Double parseCapacity(Object raw, String path, List<ArmorConfigProblem> problems) {
        try {
            ParsedNumber number = number(raw, false);
            return number == null ? null : checkRange(number.value(), CAPACITY_MIN, CAPACITY_MAX);
        } catch (Invalid invalid) {
            problems.add(invalid.toProblem(path, raw));
            return null;
        }
    }

    /** 插板 movementModifier 覆盖：[-0.95, 1.0]，接受百分数（"-10%" = -0.10）。 */
    public static Double parseMovement(Object raw, String path, List<ArmorConfigProblem> problems) {
        try {
            ParsedNumber number = number(raw, true);
            return number == null ? null : checkRange(number.value(), MOVEMENT_MIN, MOVEMENT_MAX);
        } catch (Invalid invalid) {
            problems.add(invalid.toProblem(path, raw));
            return null;
        }
    }

    /** durability：[1, 100000] 的整数；"250.0" 可以，"250.5" 不行。 */
    public static Integer parseDurability(Object raw, String path, List<ArmorConfigProblem> problems) {
        try {
            ParsedNumber number = number(raw, false);
            if (number == null) {
                return null;
            }
            double value = number.value();
            if (value != Math.rint(value)) {
                throw new Invalid(ProblemReason.NOT_INTEGER);
            }
            checkRange(value, DURABILITY_MIN, DURABILITY_MAX);
            return (int) value;
        } catch (Invalid invalid) {
            problems.add(invalid.toProblem(path, raw));
            return null;
        }
    }

    /** tier：I~VI（不区分大小写，可带“级”），也接受 1~6。 */
    public static PlateArmorTier parseTier(Object raw, String path, List<ArmorConfigProblem> problems) {
        try {
            if (raw instanceof Number number) {
                return tierFromNumber(number.doubleValue());
            }
            if (!(raw instanceof String string)) {
                throw new Invalid(ProblemReason.WRONG_TYPE);
            }
            String text = normalize(string);
            if (isDefault(text)) {
                return null;
            }
            if (text.endsWith("级")) {
                text = text.substring(0, text.length() - 1).trim();
            }
            String upper = text.toUpperCase(Locale.ROOT);
            for (PlateArmorTier tier : PlateArmorTier.values()) {
                if (tier.name().equals(upper)) {
                    return tier;
                }
            }
            if (NUMBER.matcher(text).matches()) {
                return tierFromNumber(Double.parseDouble(text));
            }
            throw new Invalid(ProblemReason.UNKNOWN_TIER);
        } catch (Invalid invalid) {
            problems.add(invalid.toProblem(path, raw));
            return null;
        }
    }

    private static PlateArmorTier tierFromNumber(double value) {
        if (Double.isFinite(value) && value == Math.rint(value) && value >= 1.0D
                && value <= PlateArmorTier.values().length) {
            return PlateArmorTier.values()[(int) value - 1];
        }
        throw new Invalid(ProblemReason.UNKNOWN_TIER);
    }

    /** weight：light / medium / heavy，也接受 轻/中/重、轻型/中型/重型 和语言文件里的头盔类型名。 */
    public static PlateArmorWeight parseWeight(Object raw, String path, List<ArmorConfigProblem> problems) {
        try {
            if (!(raw instanceof String string)) {
                throw new Invalid(raw instanceof Number ? ProblemReason.UNKNOWN_WEIGHT : ProblemReason.WRONG_TYPE);
            }
            String text = normalize(string);
            if (isDefault(text)) {
                return null;
            }
            String lower = text.toLowerCase(Locale.ROOT);
            for (PlateArmorWeight weight : PlateArmorWeight.values()) {
                if (weight.id().equals(lower)
                        || text.equals(ArmorLangNames.get(weight.translationKey(), "\u0000"))
                        || text.equals(ArmorLangNames.get("type.wok_infantry_armor.helmet." + weight.id(), "\u0000"))
                        || text.equals(chineseWeight(weight))
                        || text.equals(chineseWeight(weight) + "型")) {
                    return weight;
                }
            }
            throw new Invalid(ProblemReason.UNKNOWN_WEIGHT);
        } catch (Invalid invalid) {
            problems.add(invalid.toProblem(path, raw));
            return null;
        }
    }

    private static String chineseWeight(PlateArmorWeight weight) {
        return switch (weight) {
            case LIGHT -> "轻";
            case MEDIUM -> "中";
            case HEAVY -> "重";
        };
    }

    /** material：英文 ID（armor-steel / armor steel 也可）或语言文件里的中文材质名。 */
    public static PlateArmorConstructionMaterial parseMaterial(Object raw, String path,
                                                               List<ArmorConfigProblem> problems) {
        try {
            if (!(raw instanceof String string)) {
                throw new Invalid(raw instanceof Number ? ProblemReason.UNKNOWN_MATERIAL : ProblemReason.WRONG_TYPE);
            }
            String text = normalize(string);
            if (isDefault(text)) {
                return null;
            }
            String id = text.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            for (PlateArmorConstructionMaterial material : PlateArmorConstructionMaterial.values()) {
                if (material.id().equals(id)
                        || text.equals(ArmorLangNames.get(material.translationKey(), "\u0000"))) {
                    return material;
                }
            }
            throw new Invalid(ProblemReason.UNKNOWN_MATERIAL);
        } catch (Invalid invalid) {
            problems.add(invalid.toProblem(path, raw));
            return null;
        }
    }

    /**
     * coverage（只插板）："default"、列表 ["chest","abdomen"] 或单个字符串 "chest,abdomen"（也可用、或空格分隔）。
     * 部位用 {@link ProtectedBodyPart#id()} 或中文部位名；空列表、未知部位、含 head 都整项回退默认。
     */
    public static PlateArmorCoverage.Coverage parseCoverage(Object raw, String path,
                                                            List<ArmorConfigProblem> problems) {
        try {
            List<String> names = new ArrayList<>();
            if (raw instanceof String string) {
                String text = normalize(string);
                if (isDefault(text)) {
                    return null;
                }
                splitInto(text, names);
            } else if (raw instanceof List<?> list) {
                for (Object element : list) {
                    if (!(element instanceof String string)) {
                        throw new Invalid(ProblemReason.COVERAGE_UNKNOWN_PART, String.valueOf(element));
                    }
                    splitInto(normalize(string), names);
                }
            } else {
                throw new Invalid(ProblemReason.WRONG_TYPE);
            }
            if (names.isEmpty()) {
                throw new Invalid(ProblemReason.COVERAGE_EMPTY);
            }
            EnumSet<ProtectedBodyPart> parts = EnumSet.noneOf(ProtectedBodyPart.class);
            for (String name : names) {
                ProtectedBodyPart part = bodyPart(name);
                if (part == null) {
                    throw new Invalid(ProblemReason.COVERAGE_UNKNOWN_PART, name);
                }
                if (part == ProtectedBodyPart.HEAD) {
                    throw new Invalid(ProblemReason.COVERAGE_HEAD);
                }
                parts.add(part);
            }
            return PlateArmorCoverage.Coverage.of(parts.toArray(ProtectedBodyPart[]::new));
        } catch (Invalid invalid) {
            problems.add(invalid.toProblem(path, raw));
            return null;
        }
    }

    private static void splitInto(String text, List<String> names) {
        for (String token : LIST_SEPARATOR.split(text)) {
            if (!token.isBlank()) {
                names.add(token.trim());
            }
        }
    }

    private static ProtectedBodyPart bodyPart(String name) {
        ProtectedBodyPart byId = ProtectedBodyPart.fromExternalName(name).orElse(null);
        if (byId != null) {
            return byId;
        }
        for (ProtectedBodyPart part : ProtectedBodyPart.values()) {
            if (name.equals(ArmorLangNames.get(part.translationKey(), "\u0000"))) {
                return part;
            }
        }
        return null;
    }

    /**
     * attributeModifiers：每条 "&lt;属性 id&gt; &lt;操作&gt; &lt;数值&gt;"，分隔符可以是半角/全角逗号或空白的任意组合。
     * 操作 add/addition/+、multiply_base/base、multiply_total/total/mul（不区分大小写）；百分数只用于乘法。
     * 移速、原版护甲值、韧性被拒绝。同一（属性, 操作）多条相加合并，合并后再检查一次范围。
     * 被忽略的条目写进 problems，路径形如 {@code ....attributeModifiers[1]}（从 1 开始数）。
     */
    public static List<AttributeSpec> parseAttributes(List<?> raw, String path, List<ArmorConfigProblem> problems) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Map<String, AttributeSpec> merged = new LinkedHashMap<>();
        for (int index = 0; index < raw.size(); index++) {
            Object element = raw.get(index);
            String entryPath = path + "[" + (index + 1) + "]";
            try {
                if (!(element instanceof String string)) {
                    throw new Invalid(ProblemReason.ATTRIBUTE_SYNTAX);
                }
                AttributeSpec spec = parseAttribute(string);
                String key = spec.attributeId() + "|" + spec.operation().id();
                AttributeSpec previous = merged.get(key);
                merged.put(key, previous == null ? spec : new AttributeSpec(spec.attributeId(), spec.operation(),
                        previous.amount() + spec.amount(), previous.source() + " + " + spec.source()));
            } catch (Invalid invalid) {
                problems.add(invalid.toProblem(entryPath, element));
            }
        }
        List<AttributeSpec> result = new ArrayList<>(merged.size());
        for (AttributeSpec spec : merged.values()) {
            if (!amountInRange(spec.operation(), spec.amount())) {
                problems.add(new ArmorConfigProblem(path, quote(spec.source()),
                        ProblemReason.ATTRIBUTE_MERGED_RANGE, List.of(spec.attributeId())));
                continue;
            }
            result.add(spec);
        }
        return List.copyOf(result);
    }

    private static AttributeSpec parseAttribute(String raw) {
        String text = normalize(raw);
        String[] tokens = ATTRIBUTE_SEPARATOR.split(text);
        List<String> parts = new ArrayList<>(3);
        for (String token : tokens) {
            if (!token.isEmpty()) {
                parts.add(token);
            }
        }
        if (parts.size() != 3) {
            throw new Invalid(ProblemReason.ATTRIBUTE_SYNTAX);
        }
        String id = normalizeAttributeId(parts.get(0));
        if (id.equals(MOVEMENT_SPEED_ID)) {
            throw new Invalid(ProblemReason.ATTRIBUTE_MOVEMENT_FORBIDDEN);
        }
        if (id.equals(ARMOR_ID) || id.equals(ARMOR_TOUGHNESS_ID)) {
            throw new Invalid(ProblemReason.ATTRIBUTE_ARMOR_FORBIDDEN, id);
        }
        ModifierOperation operation = operation(parts.get(1));
        String amountText = parts.get(2);
        boolean percent = amountText.endsWith("%");
        if (percent) {
            if (!operation.multiplicative()) {
                throw new Invalid(ProblemReason.ATTRIBUTE_PERCENT_ADDITION);
            }
            amountText = amountText.substring(0, amountText.length() - 1).trim();
        }
        if (!NUMBER.matcher(amountText).matches()) {
            throw new Invalid(ProblemReason.NOT_A_NUMBER);
        }
        double amount = Double.parseDouble(amountText);
        if (percent) {
            amount /= 100.0D;
        }
        if (!Double.isFinite(amount)) {
            throw new Invalid(ProblemReason.NOT_FINITE);
        }
        if (!amountInRange(operation, amount)) {
            throw new Invalid(ProblemReason.ATTRIBUTE_AMOUNT_RANGE);
        }
        return new AttributeSpec(id, operation, amount, raw.trim());
    }

    private static boolean amountInRange(ModifierOperation operation, double amount) {
        if (!Double.isFinite(amount)) {
            return false;
        }
        return operation.multiplicative()
                ? amount >= MULTIPLY_MIN
                : -ADDITION_LIMIT <= amount && amount <= ADDITION_LIMIT;
    }

    /** "generic.luck" → "minecraft:generic.luck"；格式不合法时抛出。 */
    public static String normalizeAttributeId(String raw) {
        String id = raw.trim().toLowerCase(Locale.ROOT);
        int colon = id.indexOf(':');
        String namespace = colon >= 0 ? id.substring(0, colon) : "minecraft";
        String attributePath = colon >= 0 ? id.substring(colon + 1) : id;
        if (!RESOURCE_NAMESPACE.matcher(namespace).matches() || !RESOURCE_PATH.matcher(attributePath).matches()) {
            throw new Invalid(ProblemReason.ATTRIBUTE_BAD_ID, raw);
        }
        return namespace + ":" + attributePath;
    }

    private static ModifierOperation operation(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "add", "addition", "+" -> ModifierOperation.ADDITION;
            case "multiply_base", "base" -> ModifierOperation.MULTIPLY_BASE;
            case "multiply_total", "total", "mul" -> ModifierOperation.MULTIPLY_TOTAL;
            default -> throw new Invalid(ProblemReason.ATTRIBUTE_UNKNOWN_OPERATION, raw);
        };
    }

    // ---- 通用 ----

    private record ParsedNumber(double value, boolean percent) {
    }

    /** null = "default"；数字或数字字符串（可带 %）解析成 double；其余抛出。 */
    private static ParsedNumber number(Object raw, boolean allowPercent) {
        if (raw instanceof Number number) {
            double value = number.doubleValue();
            if (!Double.isFinite(value)) {
                throw new Invalid(ProblemReason.NOT_FINITE);
            }
            return new ParsedNumber(value, false);
        }
        if (!(raw instanceof String string)) {
            throw new Invalid(ProblemReason.WRONG_TYPE);
        }
        String text = normalize(string);
        if (isDefault(text)) {
            return null;
        }
        boolean percent = text.endsWith("%");
        if (percent) {
            if (!allowPercent) {
                throw new Invalid(ProblemReason.PERCENT_NOT_ALLOWED);
            }
            text = text.substring(0, text.length() - 1).trim();
        }
        if (!NUMBER.matcher(text).matches()) {
            throw new Invalid(ProblemReason.NOT_A_NUMBER);
        }
        double value = Double.parseDouble(text);
        if (percent) {
            value /= 100.0D;
        }
        if (!Double.isFinite(value)) {
            throw new Invalid(ProblemReason.NOT_FINITE);
        }
        return new ParsedNumber(value, percent);
    }

    /** 必须写成 min <= v && v <= max，NaN 才会被拦下。 */
    private static double checkRange(double value, double min, double max) {
        if (Double.isFinite(value) && min <= value && value <= max) {
            return value;
        }
        throw new Invalid(ProblemReason.OUT_OF_RANGE, plain(min), plain(max));
    }

    private static boolean isDefault(String normalized) {
        return normalized.equalsIgnoreCase(ArmorItemConfig.DEFAULT);
    }

    /** 全角 ASCII（含 ，％－．和全角数字）换成半角，全角空格换成半角空格，再去掉首尾空白。 */
    public static String normalize(String raw) {
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '！' && c <= '～') {
                builder.append((char) (c - 0xFEE0));
            } else if (c == '　') {
                builder.append(' ');
            } else {
                builder.append(c);
            }
        }
        return builder.toString().trim();
    }

    /** 日志与命令里显示原值：文字加引号，列表按 TOML 样式。 */
    public static String describeRaw(Object raw) {
        if (raw instanceof String string) {
            return quote(string);
        }
        if (raw instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(describeRaw(list.get(i)));
            }
            return builder.append(']').toString();
        }
        return String.valueOf(raw);
    }

    private static String quote(String text) {
        return "\"" + text + "\"";
    }

    private static String plain(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0E9D) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /** 解析失败的内部信号；只在本类里抛出和捕获。 */
    private static final class Invalid extends RuntimeException {
        private final ProblemReason reason;
        private final List<String> args;

        private Invalid(ProblemReason reason, String... args) {
            super(reason.name(), null, false, false);
            this.reason = reason;
            this.args = List.of(args);
        }

        private ArmorConfigProblem toProblem(String path, Object raw) {
            return new ArmorConfigProblem(path, describeRaw(raw), reason, args);
        }
    }
}
