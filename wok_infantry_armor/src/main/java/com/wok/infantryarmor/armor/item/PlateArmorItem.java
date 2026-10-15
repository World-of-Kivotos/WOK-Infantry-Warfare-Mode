package com.wok.infantryarmor.armor.item;

import com.wok.infantryarmor.armor.PlateArmorCoverage;
import com.wok.infantryarmor.armor.PlateArmorEquipmentMaterial;
import com.wok.infantryarmor.armor.PlateArmorStats;
import com.wok.infantryarmor.armor.PlateArmorVariant;
import com.wok.infantryarmor.armor.ProtectiveArmorItem;
import com.wok.infantryarmor.armor.ProtectedBodyPart;
import com.wok.infantryarmor.armor.client.PlateArmorClient;
import com.wok.infantryarmor.armor.settings.ArmorItemProfile;
import com.wok.infantryarmor.armor.settings.ArmorItemSettings;
import com.wok.infantryarmor.armor.settings.ArmorWearMath;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * 54 个外观共用的可穿戴插板胸甲；物品身份由注册时绑定的 variant 决定，玩家 NBT 不可伪造。
 * 等级、类型、材质和全部数值都经过逐件配置解析器 {@link #settings()}；注册时按 variant 绑定的原版
 * ArmorMaterial（装备音效、图层名）不随配置变化。
 */
public final class PlateArmorItem extends ArmorItem implements ProtectiveArmorItem {

    private static final String MODEL_TEXTURE_PREFIX =
            "wok_infantry_armor:textures/models/armor/plate_armor_";

    private final PlateArmorVariant variant;

    public PlateArmorItem(PlateArmorVariant variant) {
        super(PlateArmorEquipmentMaterial.forWeight(variant.weight()), Type.CHESTPLATE,
                new Item.Properties().stacksTo(1).durability(1));
        this.variant = variant;
    }

    public PlateArmorVariant variant() {
        return variant;
    }

    @Override
    public ArmorItemProfile settings() {
        return ArmorItemSettings.plate(variant);
    }

    @Override
    public ArmorItemSettings.ItemAttributes extraAttributes() {
        return ArmorItemSettings.attributes(variant);
    }

    @Override
    public EquipmentSlot protectionSlot() {
        return EquipmentSlot.CHEST;
    }

    /**
     * 解析后的覆盖部位（逐件配置 coverage 可覆盖默认表）。只有安装部位血量、其开关开启且是局部命中时才按部位判定；
     * 否则胸甲（没有胸甲时头盔）保护全身。
     */
    @Override
    public PlateArmorCoverage.Coverage coverage() {
        return settings().coverage();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(PlateArmorClient.forItem(this));
    }

    /**
     * 胸甲槽穿戴贴图：每个外观都有一张与物品 ID 同名、由 tools/plate-armor/export.mjs 和烘焙网格一起导出的图集，
     * 网格 uv 就是按这张图归一化的。不再按外观逐个列举，新增外观不会因为漏写一行而落到材质的默认贴图上。
     */
    @Override
    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (slot != EquipmentSlot.CHEST) {
            return null;
        }
        return MODEL_TEXTURE_PREFIX + variant.id() + "_layer_1.png";
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return settings().maxDurability();
    }

    /** 禁止原版按两个 TaCZ 伤害段分别磨损；统一磨损由插板处理器和 TaCZ Post/Kill 集成执行。 */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity,
                                                    Consumer<T> onBroken) {
        return 0;
    }

    /** 先完成本击防护，再按进入插板公式前的来伤统一磨损一次：max(1, floor(X/4)) × 逐件磨损倍率。 */
    @Override
    public void applyCombatWear(ItemStack stack, double incomingDamage, Player wearer) {
        applyPlateWear(stack, incomingDamage, wearer);
    }

    /** TaCZ 一弹一次：普通段与穿甲段相加后按同一公式磨损，倍率只乘一次（不转调 applyCombatWear）。 */
    @Override
    public void applyBallisticWear(ItemStack stack, double normalDamage, double armorPiercingDamage,
                                   Player wearer) {
        if (!Double.isFinite(normalDamage) || !Double.isFinite(armorPiercingDamage)) {
            return;
        }
        applyPlateWear(stack, normalDamage + armorPiercingDamage, wearer);
    }

    private void applyPlateWear(ItemStack stack, double incomingDamage, Player wearer) {
        if (stack.isEmpty() || incomingDamage <= 0.0D || !Double.isFinite(incomingDamage)) {
            return;
        }
        if (!isFunctional(stack)) {
            breakExhausted(stack, wearer);
            return;
        }
        int wear = ArmorWearMath.scale(ArmorWearMath.plateLegacyWear(incomingDamage),
                settings().wearMultiplier(), () -> wearer.getRandom().nextDouble());
        if (wear <= 0) {
            return;
        }
        long next = (long) stack.getDamageValue() + wear;
        if (next >= stack.getMaxDamage()) {
            breakExhausted(stack, wearer);
        } else {
            stack.setDamageValue((int) next);
        }
    }

    public static PlateArmorItem equippedBy(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        Item item = stack.getItem();
        return item instanceof PlateArmorItem plate && plate.isFunctional(stack) ? plate : null;
    }

    /** 配置热重载降低最大耐久后，旧物品可能立即越过新上限；耗尽态不得继续提供防护。 */
    @Override
    public boolean isFunctional(ItemStack stack) {
        return !stack.isEmpty() && stack.getDamageValue() < stack.getMaxDamage();
    }

    @Override
    public void breakExhausted(ItemStack stack, Player wearer) {
        if (!stack.isEmpty()) {
            stack.shrink(1);
            wearer.broadcastBreakEvent(EquipmentSlot.CHEST);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        ArmorItemProfile settings = settings();
        PlateArmorStats stats = settings.stats();

        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.level", settings.tier().name())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.category",
                        Component.translatable("category.wok_infantry_armor.plate_armor"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.type",
                        Component.translatable(settings.weight().translationKey()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.material",
                        Component.translatable(settings.material().translationKey()))
                .withStyle(ChatFormatting.GRAY));
        appendCoverageTooltip(tooltip, settings.coverage());

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.durability",
                        Math.max(0, stack.getMaxDamage() - stack.getDamageValue()), stack.getMaxDamage())
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.ballistic_r",
                        ArmorTooltips.percent(stats.ballisticProtection()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.ballistic_q",
                        ArmorTooltips.percent(stats.armorPiercingBuffer()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.general_g",
                        ArmorTooltips.percent(stats.generalProtection()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.capacity_t",
                        ArmorTooltips.decimal(stats.pressureCapacity()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.movement",
                        ArmorTooltips.signedPercent(stats.movementModifier()))
                .withStyle(ChatFormatting.DARK_GRAY));
        ArmorTooltips.appendExtraAttributes(tooltip, extraAttributes());
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.plate_armor.replaces_vanilla")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void appendCoverageTooltip(List<Component> tooltip, PlateArmorCoverage.Coverage coverage) {
        if (!coverage.configured()) {
            tooltip.add(Component.translatable(
                            "tooltip.wok_infantry_armor.plate_armor.protected_parts_unconfigured")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        MutableComponent partNames = Component.empty();
        boolean first = true;
        for (ProtectedBodyPart part : coverage.parts()) {
            if (!first) {
                partNames.append(Component.translatable(
                                "tooltip.wok_infantry_armor.plate_armor.part_separator")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            partNames.append(Component.translatable(part.translationKey()));
            first = false;
        }
        tooltip.add(Component.translatable(
                        "tooltip.wok_infantry_armor.plate_armor.protected_parts", partNames)
                .withStyle(ChatFormatting.GOLD));
    }
}
