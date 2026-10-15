package com.wok.infantryarmor.armor.item;

import com.wok.infantryarmor.ArmorerConfig;
import com.wok.infantryarmor.armor.ArmorCondition;
import com.wok.infantryarmor.armor.BallisticWearMath;
import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.PlateArmorCoverage;
import com.wok.infantryarmor.armor.PlateArmorEquipmentMaterial;
import com.wok.infantryarmor.armor.PlateArmorStats;
import com.wok.infantryarmor.armor.ProtectedBodyPart;
import com.wok.infantryarmor.armor.ProtectiveArmorItem;
import com.wok.infantryarmor.armor.client.HelmetArmorClient;
import com.wok.infantryarmor.armor.settings.ArmorItemProfile;
import com.wok.infantryarmor.armor.settings.ArmorItemSettings;
import com.wok.infantryarmor.armor.settings.ArmorWearMath;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * A head-slot armor item whose protection is routed only to localized head hits.
 * 等级、类型、材质、数值、耐久、磨损与移速都经过逐件配置解析器 {@link #settings()}。
 */
public final class HelmetItem extends ArmorItem implements ProtectiveArmorItem, GeoItem {

    private static final String MODEL_TEXTURE_PREFIX =
            "wok_infantry_armor:textures/models/armor/helmet_";

    private final HelmetVariant variant;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public HelmetItem(HelmetVariant variant) {
        super(PlateArmorEquipmentMaterial.forWeight(variant.weight()), Type.HELMET,
                new Item.Properties().stacksTo(1).durability(1));
        this.variant = variant;
    }

    public HelmetVariant variant() {
        return variant;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Headgear is currently static; the GeckoLib controller hook remains available for later attachments.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public ArmorItemProfile settings() {
        return ArmorItemSettings.helmet(variant);
    }

    @Override
    public ArmorItemSettings.ItemAttributes extraAttributes() {
        return ArmorItemSettings.attributes(variant);
    }

    @Override
    public EquipmentSlot protectionSlot() {
        return EquipmentSlot.HEAD;
    }

    /** 头盔固定只护头；没有安装部位血量（或不是局部命中）且没有胸甲时，头盔保护全身。 */
    @Override
    public PlateArmorCoverage.Coverage coverage() {
        return settings().coverage();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(HelmetArmorClient.forItem(this));
    }

    @Override
    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return slot == EquipmentSlot.HEAD
                ? MODEL_TEXTURE_PREFIX + variant.id() + "_layer_1.png"
                : null;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return settings().maxDurability();
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity,
                                                    Consumer<T> onBroken) {
        return 0;
    }

    /** 非 TaCZ 物理伤害：max(1, ceil(X)) × 逐件磨损倍率。 */
    @Override
    public void applyCombatWear(ItemStack stack, double incomingDamage, Player wearer) {
        if (stack.isEmpty() || incomingDamage <= 0.0D || !Double.isFinite(incomingDamage)) {
            return;
        }
        if (!isFunctional(stack)) {
            breakExhausted(stack, wearer);
            return;
        }
        int wear = ArmorWearMath.scale(ArmorWearMath.helmetLegacyWear(incomingDamage),
                settings().wearMultiplier(), () -> wearer.getRandom().nextDouble());
        if (wear > 0) {
            applyWear(stack, wear, wearer);
        }
    }

    /** TaCZ 一弹一次：BallisticWearMath（含全局 ballisticWearScale 与穿甲段倍率）× 逐件磨损倍率，只乘一次。 */
    @Override
    public void applyBallisticWear(ItemStack stack, double normalDamage,
                                   double armorPiercingDamage, Player wearer) {
        if (stack.isEmpty() || !Double.isFinite(normalDamage) || !Double.isFinite(armorPiercingDamage)) {
            return;
        }
        int legacy = BallisticWearMath.wear(
                Math.max(0.0D, normalDamage),
                Math.max(0.0D, armorPiercingDamage),
                ArmorerConfig.PLATE_ARMOR.helmetBallisticWearScale(),
                ArmorerConfig.PLATE_ARMOR.helmetArmorPiercingWearMultiplier());
        int wear = ArmorWearMath.scale(legacy, settings().wearMultiplier(),
                () -> wearer.getRandom().nextDouble());
        if (wear > 0) {
            applyWear(stack, wear, wearer);
        }
    }

    private void applyWear(ItemStack stack, int wear, Player wearer) {
        if (!isFunctional(stack)) {
            breakExhausted(stack, wearer);
            return;
        }
        long next = (long) stack.getDamageValue() + wear;
        if (next >= stack.getMaxDamage()) {
            breakExhausted(stack, wearer);
        } else {
            stack.setDamageValue((int) next);
        }
    }

    @Override
    public boolean isFunctional(ItemStack stack) {
        return !stack.isEmpty() && stack.getDamageValue() < stack.getMaxDamage();
    }

    @Override
    public double protectionEfficiency(ItemStack stack) {
        double remaining = ArmorCondition.remainingRatio(stack.getDamageValue(), stack.getMaxDamage());
        return ArmorCondition.protectionEfficiency(remaining);
    }

    @Override
    public void breakExhausted(ItemStack stack, Player wearer) {
        if (!stack.isEmpty()) {
            stack.shrink(1);
            wearer.broadcastBreakEvent(EquipmentSlot.HEAD);
        }
    }

    public static HelmetItem equippedBy(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        return stack.getItem() instanceof HelmetItem helmet && helmet.isFunctional(stack)
                ? helmet : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        ArmorItemProfile settings = settings();
        double efficiency = protectionEfficiency(stack);
        PlateArmorStats stats = settings.stats().withProtectionEfficiency(efficiency);

        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.level",
                        settings.tier().name()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.category",
                        Component.translatable("category.wok_infantry_armor.helmet"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.type",
                        Component.translatable("type.wok_infantry_armor.helmet."
                                + settings.weight().id()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.material",
                        Component.translatable(settings.material().translationKey()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.protected_parts",
                        Component.translatable(ProtectedBodyPart.HEAD.translationKey()))
                .withStyle(ChatFormatting.GOLD));

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.durability",
                        Math.max(0, stack.getMaxDamage() - stack.getDamageValue()), stack.getMaxDamage())
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.condition_efficiency",
                ArmorTooltips.percent(efficiency)).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.ballistic_r",
                ArmorTooltips.percent(stats.ballisticProtection())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.ballistic_q",
                ArmorTooltips.percent(stats.armorPiercingBuffer())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.general_g",
                ArmorTooltips.percent(stats.generalProtection())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.capacity_t",
                ArmorTooltips.decimal(stats.pressureCapacity())).withStyle(ChatFormatting.DARK_GRAY));
        if (stats.movementModifier() != 0.0D) {
            tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.movement",
                    ArmorTooltips.signedPercent(stats.movementModifier())).withStyle(ChatFormatting.DARK_GRAY));
        }
        ArmorTooltips.appendExtraAttributes(tooltip, extraAttributes());
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.localized_only")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
