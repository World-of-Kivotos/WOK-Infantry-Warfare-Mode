package com.wok.infantryarmor.armor.settings;

import com.wok.infantryarmor.ArmorItemConfig;
import com.wok.infantryarmor.ArmorerConfig;
import com.wok.infantryarmor.armor.ArmorConfigGeneration;
import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.PlateArmorVariant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 逐件配置的统一入口：给每件插板/头盔返回“解析后的逐件设置”和已通过注册表检查的额外属性修正。
 *
 * <h2>缓存与线程</h2>
 * <p>只缓存来自 wok-infantry-armor-items.toml 的解析结果（每件的覆盖值、覆盖部位、属性修正、问题列表），
 * 做成不可变快照并通过 volatile 引用发布，附带构建时的配置代数。两个护甲配置文件的
 * Loading/Reloading/Unloading 事件只推进代数并清空引用，绝不在事件里重建（事件可能在 FileWatcher 线程或网络线程上，
 * 另一个文件可能尚未同步）。读取方（服务端线程或渲染线程）先记下代数、再读 ConfigValue 构建快照；
 * 发现快照代数与当前不符就重建。没有覆盖的默认值由 {@link ArmorItemProfile} 每次按主配置实时计算，不进快照。</p>
 *
 * <p>远程客户端只会收到 Reloading（网络线程）；单人/局域网主机与内置服务端共用同一份 spec，靠服务端的 Loading。</p>
 */
public final class ArmorItemSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger("wok_infantry_armor");

    private static volatile Snapshot snapshot;

    /**
     * 当前连接（或本机服务端）是否已经拿到逐件配置：收到该 spec 的 Loading/Reloading 置 true，
     * Unloading 或客户端断线置 false。为 false 时一律按默认值构建，绝不解析 spec 里残留的旧数据。
     */
    private static volatile boolean itemsReceived;

    private ArmorItemSettings() {
    }

    /** 在 mod 总线上监听两个护甲配置文件的加载、重载与卸载。 */
    public static void registerConfigListeners(IEventBus modBus) {
        modBus.addListener((ModConfigEvent.Loading event) -> onConfigChanged(event));
        modBus.addListener((ModConfigEvent.Reloading event) -> onConfigChanged(event));
        modBus.addListener((ModConfigEvent.Unloading event) -> onConfigChanged(event));
    }

    /** 只推进代数并清空快照引用；永不抛异常，也不读取任何 ConfigValue。 */
    static void onConfigChanged(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (config == null) {
            return;
        }
        if (config.getSpec() == ArmorItemConfig.SPEC) {
            // 先改标志再推进代数：读取方先记代数、后读标志，中途插进来的事件必然让这次快照过期。
            itemsReceived = !(event instanceof ModConfigEvent.Unloading);
        }
        if (config.getSpec() == ArmorItemConfig.SPEC || config.getSpec() == ArmorerConfig.SPEC) {
            ArmorConfigGeneration.advance();
            snapshot = null;
        }
    }

    /**
     * 客户端断开连接时调用（任何服务器，包括单人世界）。Forge 不会在远程断线时重置 SERVER 配置，
     * 逐件配置 spec 会一直留着上一个服务器同步来的数据；下一个服务器若没有这个文件（例如 1.2.0 服务器），
     * 就不会再收到它的 Loading/Reloading。这里把标志清掉，直到再次收到逐件配置前一律按默认值。
     */
    public static void onClientDisconnected() {
        itemsReceived = false;
        ArmorConfigGeneration.advance();
        snapshot = null;
    }

    public static ArmorItemProfile plate(PlateArmorVariant variant) {
        return current().parsed().plate(variant);
    }

    public static ArmorItemProfile helmet(HelmetVariant variant) {
        return current().parsed().helmet(variant);
    }

    public static ItemAttributes attributes(PlateArmorVariant variant) {
        return current().plateAttributes()[variant.ordinal()];
    }

    public static ItemAttributes attributes(HelmetVariant variant) {
        return current().helmetAttributes()[variant.ordinal()];
    }

    /** 当前逐件配置里的全部无效值（含注册表检查出的属性问题）。 */
    public static List<ArmorConfigProblem> problems() {
        return current().problems();
    }

    private static Snapshot current() {
        Snapshot current = snapshot;
        if (current != null && current.generation() == ArmorConfigGeneration.current()) {
            return current;
        }
        return rebuild();
    }

    private static synchronized Snapshot rebuild() {
        int generation = ArmorConfigGeneration.current();
        Snapshot current = snapshot;
        if (current != null && current.generation() == generation) {
            return current;
        }
        ArmorItemParser.ParsedItems parsed = itemsReceived
                ? ArmorItemParser.parse(ArmorItemConfig.ITEMS)
                : ArmorItemParser.defaults();
        List<ArmorConfigProblem> problems = new ArrayList<>(parsed.problems());
        AttributeSupplier playerAttributes = DefaultAttributes.getSupplier(EntityType.PLAYER);

        ItemAttributes[] plateAttributes = new ItemAttributes[PlateArmorVariant.values().length];
        for (PlateArmorVariant variant : PlateArmorVariant.values()) {
            plateAttributes[variant.ordinal()] = resolveAttributes(parsed.plate(variant), playerAttributes, problems);
        }
        ItemAttributes[] helmetAttributes = new ItemAttributes[HelmetVariant.values().length];
        for (HelmetVariant variant : HelmetVariant.values()) {
            helmetAttributes[variant.ordinal()] = resolveAttributes(parsed.helmet(variant), playerAttributes,
                    problems);
        }

        Snapshot built = new Snapshot(generation, parsed, plateAttributes, helmetAttributes, List.copyOf(problems));
        snapshot = built;
        for (ArmorConfigProblem problem : built.problems()) {
            LOGGER.warn("{} 中 {} = {} 无效（{}），已改用默认值、夹到范围内或忽略该条。",
                    ArmorItemConfig.FILE_NAME, problem.path(), problem.rawValue(), problem.describeReason());
        }
        return built;
    }

    private static ItemAttributes resolveAttributes(ArmorItemProfile profile, AttributeSupplier playerAttributes,
                                                    List<ArmorConfigProblem> problems) {
        List<ArmorConfigProblem> ignored = new ArrayList<>(profile.attributeProblems());
        if (profile.attributes().isEmpty()) {
            return new ItemAttributes(List.of(), List.copyOf(ignored));
        }
        String path = profile.configPath() + "." + ArmorItemConfig.ATTRIBUTE_MODIFIERS;
        List<ArmorAttributeModifier> modifiers = new ArrayList<>();
        for (AttributeSpec spec : profile.attributes()) {
            ResourceLocation id = ResourceLocation.tryParse(spec.attributeId());
            Attribute attribute = id == null || !ForgeRegistries.ATTRIBUTES.containsKey(id)
                    ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
            ArmorConfigProblem problem = null;
            if (attribute == null) {
                problem = new ArmorConfigProblem(path, "\"" + spec.source() + "\"",
                        ProblemReason.ATTRIBUTE_UNKNOWN, List.of(spec.attributeId()));
            } else if (!playerAttributes.hasAttribute(attribute)) {
                problem = new ArmorConfigProblem(path, "\"" + spec.source() + "\"",
                        ProblemReason.ATTRIBUTE_NOT_ON_PLAYER, List.of(spec.attributeId()));
            }
            if (problem != null) {
                problems.add(problem);
                ignored.add(problem);
                continue;
            }
            String seed = "wok_infantry_armor:" + profile.kind().slot() + ":" + spec.attributeId() + ":"
                    + spec.operation().id();
            modifiers.add(new ArmorAttributeModifier(
                    attribute,
                    spec.attributeId(),
                    spec.operation(),
                    toVanilla(spec.operation()),
                    spec.amount(),
                    UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)),
                    "wok_infantry_armor " + profile.kind().slot() + " " + spec.attributeId()));
        }
        return new ItemAttributes(List.copyOf(modifiers), List.copyOf(ignored));
    }

    private static AttributeModifier.Operation toVanilla(ModifierOperation operation) {
        return switch (operation) {
            case ADDITION -> AttributeModifier.Operation.ADDITION;
            case MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
            case MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
        };
    }

    /** 一条已通过注册表检查、可以施加给玩家的额外属性修正。 */
    public record ArmorAttributeModifier(Attribute attribute,
                                         String attributeId,
                                         ModifierOperation operation,
                                         AttributeModifier.Operation vanillaOperation,
                                         double amount,
                                         UUID id,
                                         String name) {
    }

    /** 一件护甲的额外属性：实际施加的修正，以及被忽略的条目（含原因）。 */
    public record ItemAttributes(List<ArmorAttributeModifier> modifiers, List<ArmorConfigProblem> ignored) {
    }

    private record Snapshot(int generation,
                            ArmorItemParser.ParsedItems parsed,
                            ItemAttributes[] plateAttributes,
                            ItemAttributes[] helmetAttributes,
                            List<ArmorConfigProblem> problems) {
    }
}
