package com.wok.infantryarmor.armor;

import com.wok.infantryarmor.ArmorerConfig;
import com.wok.infantryarmor.armor.item.HelmetItem;
import com.wok.infantryarmor.armor.item.PlateArmorItem;
import com.wok.infantryarmor.shield.item.PlasmaShieldItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * 穿戴状态同步：插板接管原版护甲/韧性并施加一次机动修正；胸槽插板与头槽头盔的逐件额外修正
 * （attributeModifiers、头盔移速）由 {@link ArmorExtraModifiers} 按槽位处理。头盔不触发原版护甲清零。
 */
public final class PlateArmorEquipmentHandler {

    public static final UUID ARMOR_REPLACEMENT_ID = UUID.fromString("5f2234c1-4479-4fb8-a4ba-ef3199bf42a1");
    public static final UUID TOUGHNESS_REPLACEMENT_ID = UUID.fromString("77d8c2e6-9a9c-4da0-b57f-ef8ef1dbdd07");
    public static final UUID MOVEMENT_ID = UUID.fromString("a8df1880-5c7f-4c81-a424-398d22d8372f");

    private static final double FULL_REPLACEMENT = -1.0D;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            synchronize(event.player);
        }
    }

    @SubscribeEvent
    public void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            synchronize(player);
        }
    }

    /** 当前一击的原版护甲阶段结束后清掉击碎护甲留下的瞬时属性；tick 仍作为异常流程兜底。 */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            synchronize(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ArmorExtraModifiers.forget(event.getEntity());
    }

    public static void synchronize(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof PlateArmorItem plate && !plate.isFunctional(chest)) {
            plate.breakExhausted(chest, player);
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof HelmetItem helmet && !helmet.isFunctional(head)) {
            helmet.breakExhausted(head, player);
        }
        PlateArmorItem armor = PlateArmorItem.equippedBy(player);
        double movement = armor == null ? 0.0D : armor.settings().movementModifier();

        // 额外修正必须在“没穿插板就 return”之前处理，只戴头盔的玩家也要拿到/移除头盔的修正。
        HelmetItem helmet = HelmetItem.equippedBy(player);
        double chestMovement = helmet == null ? movement : chestMovement(player, armor, movement);
        ArmorExtraModifiers.synchronize(player, armor, chestMovement, helmet);

        if (armor == null) {
            ArmorExtraModifiers.remove(player, Attributes.ARMOR, ARMOR_REPLACEMENT_ID);
            ArmorExtraModifiers.remove(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_REPLACEMENT_ID);
            ArmorExtraModifiers.remove(player, Attributes.MOVEMENT_SPEED, MOVEMENT_ID);
            return;
        }

        ArmorExtraModifiers.ensure(player, Attributes.ARMOR, ARMOR_REPLACEMENT_ID,
                "plate armor replaces vanilla armor", FULL_REPLACEMENT,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        ArmorExtraModifiers.ensure(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_REPLACEMENT_ID,
                "plate armor replaces vanilla toughness", FULL_REPLACEMENT,
                AttributeModifier.Operation.MULTIPLY_TOTAL);

        if (movement == 0.0D) {
            ArmorExtraModifiers.remove(player, Attributes.MOVEMENT_SPEED, MOVEMENT_ID);
        } else {
            ArmorExtraModifiers.ensure(player, Attributes.MOVEMENT_SPEED, MOVEMENT_ID,
                    "plate armor mobility", movement, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }

    /** 胸甲槽的移速修正：插板取解析后的值；胸甲槽是电浆护盾时取护盾配置的移速（护盾由自己的处理器施加）。 */
    private static double chestMovement(Player player, PlateArmorItem plate, double plateMovement) {
        if (plate != null) {
            return plateMovement;
        }
        PlasmaShieldItem shield = PlasmaShieldItem.equippedBy(player);
        return shield == null
                ? 0.0D
                : ArmorerConfig.PLASMA_SHIELD.stats(shield.shieldVariant()).movementModifier();
    }
}
