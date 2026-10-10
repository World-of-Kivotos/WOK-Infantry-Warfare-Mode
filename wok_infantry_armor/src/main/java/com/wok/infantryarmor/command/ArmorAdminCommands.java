package com.wok.infantryarmor.command;

import com.mojang.brigadier.context.CommandContext;
import com.wok.infantryarmor.armor.PlateArmorCoverage;
import com.wok.infantryarmor.armor.PlateArmorStats;
import com.wok.infantryarmor.armor.ProtectedBodyPart;
import com.wok.infantryarmor.armor.ProtectiveArmorItem;
import com.wok.infantryarmor.armor.item.ArmorTooltips;
import com.wok.infantryarmor.armor.settings.ArmorConfigProblem;
import com.wok.infantryarmor.armor.settings.ArmorItemProfile;
import com.wok.infantryarmor.armor.settings.ArmorItemSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * 平衡用的管理命令（权限 2，只在服务端执行、读服务端配置）：
 * <ul>
 *     <li>{@code /wokarmor inspect}：执行者胸、头两槽护甲的解析后设置；</li>
 *     <li>{@code /wokarmor inspect <物品>}：指定护甲的解析后设置；</li>
 *     <li>{@code /wokarmor problems}：逐件配置文件里所有无效值。</li>
 * </ul>
 * 文字都走语言键；服务端控制台没有模组语言文件时显示中文回退文字。
 */
public final class ArmorAdminCommands {

    private static final String PREFIX = "command.wok_infantry_armor.";

    private ArmorAdminCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("wokarmor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("inspect")
                        .executes(context -> inspectSelf(context.getSource()))
                        .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                .executes(ArmorAdminCommands::inspectArgument)))
                .then(Commands.literal("problems")
                        .executes(context -> problems(context.getSource()))));
    }

    private static int inspectArgument(CommandContext<CommandSourceStack> context) {
        Item item = ItemArgument.getItem(context, "item").getItem();
        if (!(item instanceof ProtectiveArmorItem armor)) {
            context.getSource().sendFailure(text("inspect.not_armor", "%s 不是可逐件配置的插板护甲或头盔",
                    item.getDescription()));
            return 0;
        }
        describe(context.getSource(), item, armor);
        return 1;
    }

    private static int inspectSelf(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(text("inspect.not_player", "不带物品参数时只能由玩家执行；控制台请写 /wokarmor inspect <物品>"));
            return 0;
        }
        int shown = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof ProtectiveArmorItem armor) {
                describe(source, stack.getItem(), armor);
                shown++;
            }
        }
        if (shown == 0) {
            source.sendFailure(text("inspect.empty", "胸甲槽和头盔槽里都没有可逐件配置的护甲"));
        }
        return shown;
    }

    private static void describe(CommandSourceStack source, Item item, ProtectiveArmorItem armor) {
        ArmorItemProfile settings = armor.settings();
        PlateArmorStats stats = settings.stats();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        boolean helmet = settings.kind() == ArmorItemProfile.Kind.HELMET;

        send(source, text("inspect.header", "%s（%s）", item.getDescription(),
                String.valueOf(id)).withStyle(ChatFormatting.GOLD));
        send(source, text("inspect.classification", "等级 %s｜类型 %s｜材质 %s",
                value(Component.literal(settings.tier().name()), settings.tierOverridden()),
                value(Component.translatable(helmet
                        ? "type.wok_infantry_armor.helmet." + settings.weight().id()
                        : settings.weight().translationKey()), settings.weightOverridden()),
                value(Component.translatable(settings.material().translationKey()), settings.materialOverridden())));
        send(source, text("inspect.protection", "缓冲R %s｜抗穿Q %s｜防护率G %s｜抗压T %s",
                value(ArmorTooltips.percent(stats.ballisticProtection()), settings.ballisticOverridden()),
                value(ArmorTooltips.percent(stats.armorPiercingBuffer()), settings.armorPiercingOverridden()),
                value(ArmorTooltips.percent(stats.generalProtection()), settings.generalOverridden()),
                value(ArmorTooltips.decimal(stats.pressureCapacity()), settings.pressureCapacityOverridden())));
        send(source, text("inspect.durability", "耐久上限 %s｜磨损倍率 %s｜机动修正 %s",
                value(Integer.toString(settings.maxDurability()), settings.durabilityOverridden()),
                value(ArmorTooltips.decimal(settings.wearMultiplier(), 3), settings.wearMultiplier() != 1.0D),
                value(ArmorTooltips.signedPercent(stats.movementModifier()), settings.movementOverridden())));
        send(source, text("inspect.coverage", "部位 %s",
                value(parts(settings.coverage()), settings.coverageOverridden())));

        ArmorItemSettings.ItemAttributes attributes = armor.extraAttributes();
        if (attributes.modifiers().isEmpty() && attributes.ignored().isEmpty()) {
            send(source, text("inspect.attribute_none", "额外属性：无"));
        }
        for (ArmorItemSettings.ArmorAttributeModifier modifier : attributes.modifiers()) {
            send(source, text("inspect.attribute", "额外属性 %s %s（%s，%s）",
                    ArmorTooltips.amountText(modifier),
                    Component.translatable(modifier.attribute().getDescriptionId()),
                    modifier.attributeId(),
                    modifier.operation().id()).withStyle(ChatFormatting.BLUE));
        }
        for (ArmorConfigProblem ignored : attributes.ignored()) {
            send(source, text("inspect.ignored", "已忽略 %s：%s",
                    ignored.rawValue(), reason(ignored)).withStyle(ChatFormatting.RED));
        }
        if (helmet) {
            send(source, text("inspect.helmet_note", "头盔防护数值为满耐久标称值，实际按剩余耐久折算")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static int problems(CommandSourceStack source) {
        List<ArmorConfigProblem> problems = ArmorItemSettings.problems();
        if (problems.isEmpty()) {
            send(source, text("problems.none", "护甲逐件配置没有问题").withStyle(ChatFormatting.GREEN));
            return 0;
        }
        send(source, text("problems.header", "护甲逐件配置有 %s 个无效值（均已回退默认或忽略）：",
                problems.size()).withStyle(ChatFormatting.GOLD));
        for (ArmorConfigProblem problem : problems) {
            send(source, text("problems.entry", "%s = %s：%s",
                    problem.path(), problem.rawValue(), reason(problem)).withStyle(ChatFormatting.YELLOW));
        }
        return problems.size();
    }

    private static Component reason(ArmorConfigProblem problem) {
        return Component.translatableWithFallback(problem.reason().translationKey(),
                problem.describeReason(), problem.args().toArray());
    }

    private static Component parts(PlateArmorCoverage.Coverage coverage) {
        if (!coverage.configured() || coverage.parts().isEmpty()) {
            return Component.translatable("tooltip.wok_infantry_armor.plate_armor.protected_parts_unconfigured");
        }
        MutableComponent names = Component.empty();
        boolean first = true;
        for (ProtectedBodyPart part : coverage.parts()) {
            if (!first) {
                names.append(Component.translatable("tooltip.wok_infantry_armor.plate_armor.part_separator"));
            }
            names.append(Component.translatable(part.translationKey()));
            first = false;
        }
        return names;
    }

    private static MutableComponent value(String text, boolean overridden) {
        return value(Component.literal(text), overridden);
    }

    private static MutableComponent value(Component content, boolean overridden) {
        MutableComponent result = Component.empty().append(content);
        if (overridden) {
            result.append(text("inspect.overridden", "（覆盖）").withStyle(ChatFormatting.YELLOW));
        }
        return result;
    }

    private static MutableComponent text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(PREFIX + key, fallback, args);
    }

    private static void send(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }
}
