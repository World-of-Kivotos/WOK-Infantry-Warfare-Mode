package com.wok.infantryarmor.armor.item;

import com.wok.infantryarmor.ArmorerConfig;
import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.ArmorCondition;
import com.wok.infantryarmor.armor.BallisticWearMath;
import com.wok.infantryarmor.armor.PlateArmorConstructionMaterial;
import com.wok.infantryarmor.armor.PlateArmorCoverage;
import com.wok.infantryarmor.armor.PlateArmorEquipmentMaterial;
import com.wok.infantryarmor.armor.PlateArmorStats;
import com.wok.infantryarmor.armor.PlateArmorTier;
import com.wok.infantryarmor.armor.PlateArmorWeight;
import com.wok.infantryarmor.armor.ProtectedBodyPart;
import com.wok.infantryarmor.armor.ProtectiveArmorItem;
import com.wok.infantryarmor.armor.client.HelmetArmorClient;
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
import java.util.Locale;
import java.util.function.Consumer;

/** A head-slot armor item whose protection is routed only to localized head hits. */
public final class HelmetItem extends ArmorItem implements ProtectiveArmorItem, GeoItem {

    private static final PlateArmorCoverage.Coverage HEAD_COVERAGE =
            PlateArmorCoverage.Coverage.of(ProtectedBodyPart.HEAD);
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
    public PlateArmorTier protectionTier() {
        return variant.tier();
    }

    @Override
    public PlateArmorWeight protectionWeight() {
        return variant.weight();
    }

    @Override
    public PlateArmorConstructionMaterial constructionMaterial() {
        return variant.material();
    }

    @Override
    public EquipmentSlot protectionSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public PlateArmorCoverage.Coverage coverage() {
        return HEAD_COVERAGE;
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
        return ArmorerConfig.PLATE_ARMOR.helmetMaxDurability(variant.weight());
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity,
                                                    Consumer<T> onBroken) {
        return 0;
    }

    @Override
    public void applyCombatWear(ItemStack stack, double incomingDamage, Player wearer) {
        if (stack.isEmpty() || incomingDamage <= 0.0D || !Double.isFinite(incomingDamage)) {
            return;
        }
        if (!isFunctional(stack)) {
            breakExhausted(stack, wearer);
            return;
        }
        int wear = Math.max(1, (int) Math.ceil(incomingDamage));
        applyWear(stack, wear, wearer);
    }

    @Override
    public void applyBallisticWear(ItemStack stack, double normalDamage,
                                   double armorPiercingDamage, Player wearer) {
        if (stack.isEmpty()) {
            return;
        }
        int wear = BallisticWearMath.wear(
                normalDamage,
                armorPiercingDamage,
                ArmorerConfig.PLATE_ARMOR.helmetBallisticWearScale(),
                ArmorerConfig.PLATE_ARMOR.helmetArmorPiercingWearMultiplier());
        if (wear > 0) {
            applyWear(stack, wear, wearer);
        }
    }

    private void applyWear(ItemStack stack, int wear, Player wearer) {
        if (!isFunctional(stack)) {
            breakExhausted(stack, wearer);
            return;
        }
        int next = stack.getDamageValue() + wear;
        if (next >= stack.getMaxDamage()) {
            breakExhausted(stack, wearer);
        } else {
            stack.setDamageValue(next);
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
        double efficiency = protectionEfficiency(stack);
        PlateArmorStats stats = PlateArmorStats.resolve(this).withProtectionEfficiency(efficiency);

        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.level",
                        variant.tier().name()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.category",
                        Component.translatable("category.wok_infantry_armor.helmet"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.type",
                        Component.translatable("type.wok_infantry_armor.helmet."
                                + variant.weight().id()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.material",
                        Component.translatable(variant.material().translationKey()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.protected_parts",
                        Component.translatable(ProtectedBodyPart.HEAD.translationKey()))
                .withStyle(ChatFormatting.GOLD));

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.durability",
                        Math.max(0, stack.getMaxDamage() - stack.getDamageValue()), stack.getMaxDamage())
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.condition_efficiency",
                        percent(efficiency)).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.ballistic_r",
                        percent(stats.ballisticProtection())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.ballistic_q",
                        percent(stats.armorPiercingBuffer())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.general_g",
                        percent(stats.generalProtection())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.capacity_t",
                        decimal(stats.pressureCapacity())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.wok_infantry_armor.helmet.localized_only")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String percent(double value) {
        return decimal(value * 100.0D) + "%";
    }

    private static String decimal(double value) {
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        int end = formatted.length();
        while (end > 0 && formatted.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && formatted.charAt(end - 1) == '.') {
            end--;
        }
        return formatted.substring(0, end);
    }
}
