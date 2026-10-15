package com.wok.infantryarmor.armor.integration;

import com.wok.infantryarmor.armor.PlateArmorEquipmentHandler;
import com.wok.infantryarmor.armor.ArmorProtectionResolver;
import com.wok.infantryarmor.armor.PlateArmorDamageClassifier;
import com.wok.infantryarmor.armor.ProtectiveArmorItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * TaCZ 一弹一次磨损的短期账本。Pre 保存命中开始时的实际 ItemStack 引用，Post/Kill 再结算；
 * 即使致死流程已把装备移入掉落物，同一引用仍能收到耐久变化。
 */
public final class PlateArmorTaczWearLedger {

    private static final long EXPIRE_TICKS = 100L;
    private static final int MAX_PENDING = 4096;

    private static final Map<ShotKey, PendingArmor> PENDING = new HashMap<>();

    public static void capture(UUID bulletId, Player target, DamageSource source,
                               double referenceDamage) {
        long now = target.level().getGameTime();
        prune(now);
        ShotKey key = new ShotKey(bulletId, target.getUUID());
        ArmorProtectionResolver.EquippedProtection protection =
                ArmorProtectionResolver.resolve(target, source);
        if (protection != null) {
            PENDING.put(key, new PendingArmor(
                    protection.item(), protection.stack(), now + EXPIRE_TICKS,
                    Math.max(0.0D, referenceDamage)));
        } else {
            PENDING.remove(key);
        }
    }

    /** Records the segment and returns its TaCZ armor-ignore ratio when this is the AP segment. */
    public static double recordSegment(Player target, DamageSource source,
                                       PlateArmorDamageClassifier.Kind kind, double incomingDamage) {
        if (incomingDamage <= 0.0D || !Double.isFinite(incomingDamage)) {
            return Double.NaN;
        }
        Entity bullet = source.getDirectEntity();
        if (bullet == null) {
            return Double.NaN;
        }
        PendingArmor entry = PENDING.get(new ShotKey(bullet.getUUID(), target.getUUID()));
        if (entry == null) {
            return Double.NaN;
        }
        switch (kind) {
            case BALLISTIC_NORMAL -> entry.normalDamage += incomingDamage;
            case BALLISTIC_ARMOR_PIERCING -> {
                entry.armorPiercingDamage += incomingDamage;
                if (entry.referenceDamage > 0.0D) {
                    return Math.max(0.0D, Math.min(1.0D,
                            entry.armorPiercingDamage / entry.referenceDamage));
                }
            }
            default -> {
            }
        }
        return Double.NaN;
    }

    public static boolean settle(UUID bulletId, Player target, double baseDamage) {
        PendingArmor entry = PENDING.remove(new ShotKey(bulletId, target.getUUID()));
        if (entry == null) {
            return false;
        }
        double normalPower = Math.max(0.0D, baseDamage);
        double armorPiercingPower = 0.0D;
        double capturedTotal = entry.normalDamage + entry.armorPiercingDamage;
        if (capturedTotal > 0.0D && baseDamage > 0.0D) {
            normalPower = baseDamage * entry.normalDamage / capturedTotal;
            armorPiercingPower = baseDamage * entry.armorPiercingDamage / capturedTotal;
        }
        entry.armor.applyBallisticWear(entry.stack, normalPower, armorPiercingPower, target);
        PlateArmorEquipmentHandler.synchronize(target);
        return true;
    }

    public static int pendingCount() {
        return PENDING.size();
    }

    private static void prune(long now) {
        Iterator<PendingArmor> iterator = PENDING.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expiresAt() < now) {
                iterator.remove();
            }
        }
        if (PENDING.size() >= MAX_PENDING) {
            PENDING.clear();
        }
    }

    private record ShotKey(UUID bulletId, UUID targetId) {
    }

    private static final class PendingArmor {
        private final ProtectiveArmorItem armor;
        private final ItemStack stack;
        private final long expiresAt;
        private final double referenceDamage;
        private double normalDamage;
        private double armorPiercingDamage;

        private PendingArmor(ProtectiveArmorItem armor, ItemStack stack, long expiresAt,
                             double referenceDamage) {
            this.armor = armor;
            this.stack = stack;
            this.expiresAt = expiresAt;
            this.referenceDamage = referenceDamage;
        }

        private long expiresAt() {
            return expiresAt;
        }
    }
}

