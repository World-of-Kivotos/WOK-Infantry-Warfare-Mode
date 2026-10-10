package com.wok.infantryarmor.armor;

import com.wok.infantryarmor.armor.item.HelmetItem;
import com.wok.infantryarmor.armor.item.PlateArmorItem;
import com.wok.infantryarmor.armor.settings.ArmorItemSettings;
import com.wok.infantryarmor.armor.settings.ArmorMovementMath;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 按槽位施加逐件配置的“额外修正”：胸槽插板与头槽头盔各自的 attributeModifiers，以及头盔的 movementModifier。
 *
 * <p>每次同步都对每个期望修正调用 ensure()，对照实时的 AttributeInstance；一个以玩家 UUID 为键的表
 * 只记录上次施加过的（属性, 修正 UUID），仅用来算出“上次有、这次没有”的要移除项，绝不用来做“没变就跳过”的捷径
 * （复活、末地返回会产生新的玩家实体，瞬时修正不一定带过去，必须照实时属性重新施加）。只在服务端线程访问。</p>
 *
 * <p>不能用以实体为键的 WeakHashMap：1.20.1 的 Entity 按网络 id 实现 equals/hashCode，复活时新实体沿用旧 id，
 * WeakHashMap 会把记录留在旧实体的弱引用上，旧实体被回收后记录被清掉，下一次脱下装备时就漏掉移除，
 * 修正卡在玩家身上。按 UUID 记录时新实体直接继承上一份集合，对新属性表做多余的移除只是空操作。
 * 记录在 {@code PlayerLoggedOutEvent} 时删除。</p>
 */
public final class ArmorExtraModifiers {

    /** 头盔移速的固定 UUID；不走额外属性的派生规则，也不复用插板的 MOVEMENT_ID。 */
    public static final UUID HELMET_MOVEMENT_ID = UUID.fromString("3e9b6f2a-7c41-4d8e-9a53-b1f04c2d6e87");

    private static final Map<UUID, Set<AppliedKey>> APPLIED = new HashMap<>();

    private ArmorExtraModifiers() {
    }

    /**
     * @param plate         胸槽里可用的插板（没有则 null）
     * @param chestMovement 胸甲槽的移速修正（插板解析值或电浆护盾配置值，都没有为 0）
     * @param helmet        头槽里可用的头盔（没有则 null）
     */
    static void synchronize(Player player, PlateArmorItem plate, double chestMovement, HelmetItem helmet) {
        Map<AppliedKey, Desired> desired = new LinkedHashMap<>();
        if (plate != null) {
            addAttributes(desired, plate.extraAttributes());
        }
        if (helmet != null) {
            double helmetMovement = ArmorMovementMath.limitHelmetMovement(chestMovement,
                    helmet.settings().movementModifier());
            if (helmetMovement != 0.0D) {
                desired.put(new AppliedKey(Attributes.MOVEMENT_SPEED, HELMET_MOVEMENT_ID),
                        new Desired("helmet mobility", helmetMovement, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            addAttributes(desired, helmet.extraAttributes());
        }

        for (Map.Entry<AppliedKey, Desired> entry : desired.entrySet()) {
            Desired value = entry.getValue();
            ensure(player, entry.getKey().attribute(), entry.getKey().id(), value.name(), value.amount(),
                    value.operation());
        }

        UUID playerId = player.getUUID();
        Set<AppliedKey> previous = APPLIED.get(playerId);
        if (previous != null) {
            for (AppliedKey key : previous) {
                if (!desired.containsKey(key)) {
                    remove(player, key.attribute(), key.id());
                }
            }
        }
        if (desired.isEmpty()) {
            APPLIED.remove(playerId);
        } else {
            APPLIED.put(playerId, Set.copyOf(desired.keySet()));
        }
    }

    /** 玩家登出时丢掉记录（瞬时修正不会存档）。 */
    static void forget(Player player) {
        APPLIED.remove(player.getUUID());
    }

    private static void addAttributes(Map<AppliedKey, Desired> desired, ArmorItemSettings.ItemAttributes attributes) {
        for (ArmorItemSettings.ArmorAttributeModifier modifier : attributes.modifiers()) {
            desired.put(new AppliedKey(modifier.attribute(), modifier.id()),
                    new Desired(modifier.name(), modifier.amount(), modifier.vanillaOperation()));
        }
    }

    static void ensure(Player player, Attribute attribute, UUID id, String name,
                       double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.getAmount() == amount && current.getOperation() == operation) {
            return;
        }
        if (current != null) {
            instance.removeModifier(id);
        }
        instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
    }

    static void remove(Player player, Attribute attribute, UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
    }

    private record AppliedKey(Attribute attribute, UUID id) {
        private AppliedKey {
            Objects.requireNonNull(attribute);
            Objects.requireNonNull(id);
        }
    }

    private record Desired(String name, double amount, AttributeModifier.Operation operation) {
    }
}
