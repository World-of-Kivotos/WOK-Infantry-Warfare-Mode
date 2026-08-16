package com.wok.bodyhealth.health;

import com.wok.bodyhealth.config.BodyHealthConfig;
import com.wok.bodyhealth.network.BodyHealthNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BodyHealthService {
    private static final UUID LEG_SPEED_MODIFIER_ID =
            UUID.fromString("cb450589-8c55-4e21-9160-b22442a6e195");
    private static final String LEG_SPEED_MODIFIER_NAME = "WOK destroyed leg penalty";
    private static final float[] EXPLOSION_WEIGHTS = {
            0.08F, 0.25F, 0.22F, 0.10F, 0.10F, 0.125F, 0.125F
    };
    private static final BodyPart[] DEFAULT_TREATMENT_LIMBS = {
            BodyPart.LEFT_ARM,
            BodyPart.RIGHT_ARM,
            BodyPart.LEFT_LEG,
            BodyPart.RIGHT_LEG
    };

    public static DamageOutcome applyLocalized(ServerPlayer player, BodyPart part,
                                                float vanillaDamage) {
        float points = toDamagePoints(vanillaDamage);
        BodyHealthData data = BodyHealthData.load(player);
        boolean fatal = applyPartDamage(data, part, points, true);
        data.setLastDamagedPart(part);
        saveAndSync(player, data);
        return new DamageOutcome(fatal, part, points);
    }

    public static DamageOutcome applyExplosion(ServerPlayer player, float vanillaDamage) {
        float points = toDamagePoints(vanillaDamage);
        BodyHealthData data = BodyHealthData.load(player);
        BodyPart[] parts = BodyPart.values();
        boolean fatal = false;
        for (int index = 0; index < parts.length; index++) {
            fatal |= applyPartDamage(data, parts[index], points * EXPLOSION_WEIGHTS[index], true);
        }
        data.setLastDamagedPart(BodyPart.CHEST);
        saveAndSync(player, data);
        return new DamageOutcome(fatal, BodyPart.CHEST, points);
    }

    public static DamageOutcome applyFall(ServerPlayer player, float vanillaDamage) {
        float points = toDamagePoints(vanillaDamage);
        BodyHealthData data = BodyHealthData.load(player);
        boolean fatal = applyPartDamage(data, BodyPart.LEFT_LEG, points * 0.5F, true);
        fatal |= applyPartDamage(data, BodyPart.RIGHT_LEG, points * 0.5F, true);
        data.setLastDamagedPart(BodyPart.LEFT_LEG);
        saveAndSync(player, data);
        return new DamageOutcome(fatal, BodyPart.LEFT_LEG, points);
    }

    public static DamageOutcome applySystemic(ServerPlayer player, float vanillaDamage) {
        float points = toDamagePoints(vanillaDamage);
        BodyHealthData data = BodyHealthData.load(player);
        boolean fatal = distributeDamage(data, points, null);
        saveAndSync(player, data);
        return new DamageOutcome(fatal, data.lastDamagedPart(), points);
    }

    public static boolean heal(ServerPlayer player, float vanillaAmount) {
        if (!(vanillaAmount > 0.0F) || !Float.isFinite(vanillaAmount)) {
            return false;
        }

        BodyHealthData data = BodyHealthData.load(player);
        float remaining = vanillaAmount * BodyHealthConfig.HEAL_SCALE.get().floatValue();
        boolean changed = false;
        while (remaining > 0.0001F) {
            BodyPart target = mostInjured(data);
            if (target == null) {
                break;
            }
            float missing = BodyHealthConfig.maxHealth(target) - data.get(target);
            float restored = Math.min(missing, remaining);
            data.set(target, data.get(target) + restored);
            remaining -= restored;
            changed = true;
        }

        if (changed) {
            saveAndSync(player, data);
        }
        return changed;
    }

    /**
     * Restores every damaged body part by the supplied amount. Injector
     * regeneration uses this path so one regeneration pulse affects all
     * seven parts simultaneously instead of selecting only one treatment
     * target or distributing a shared healing pool.
     */
    public static boolean healAllParts(ServerPlayer player, float vanillaAmount) {
        if (!(vanillaAmount > 0.0F) || !Float.isFinite(vanillaAmount)) {
            return false;
        }

        BodyHealthData data = BodyHealthData.load(player);
        float restoredPerPart = vanillaAmount
                * BodyHealthConfig.HEAL_SCALE.get().floatValue();
        boolean changed = false;
        for (BodyPart part : BodyPart.values()) {
            float maximum = BodyHealthConfig.maxHealth(part);
            float current = data.get(part);
            if (current >= maximum - 0.0001F) {
                continue;
            }
            data.set(part, Math.min(maximum, current + restoredPerPart));
            changed = true;
        }

        if (changed) {
            saveAndSync(player, data);
        }
        return changed;
    }

    /**
     * Heals one explicitly selected body part without spilling into another
     * part. Medical items use this to keep one continuous use bound to the
     * body part that was selected when treatment started.
     */
    public static boolean healPart(ServerPlayer player, BodyPart part, float vanillaAmount) {
        if (!(vanillaAmount > 0.0F) || !Float.isFinite(vanillaAmount)) {
            return false;
        }

        BodyHealthData data = BodyHealthData.load(player);
        float maximum = BodyHealthConfig.maxHealth(part);
        float missing = maximum - data.get(part);
        if (missing <= 0.0001F) {
            return false;
        }

        float restored = Math.min(
                missing, vanillaAmount * BodyHealthConfig.HEAL_SCALE.get().floatValue());
        if (restored <= 0.0001F) {
            return false;
        }
        data.set(part, data.get(part) + restored);
        saveAndSync(player, data);
        return true;
    }

    public static BodyPart mostInjuredPart(ServerPlayer player) {
        return mostInjured(BodyHealthData.load(player));
    }

    public static BodyPart defaultTreatmentPart(ServerPlayer player) {
        BodyHealthData data = BodyHealthData.load(player);
        for (BodyPart priority : new BodyPart[] {
                BodyPart.HEAD, BodyPart.CHEST, BodyPart.ABDOMEN
        }) {
            if (isDamaged(data, priority)) {
                return priority;
            }
        }

        BodyPart result = null;
        float lowestRatio = Float.POSITIVE_INFINITY;
        for (BodyPart limb : DEFAULT_TREATMENT_LIMBS) {
            if (!isDamaged(data, limb)) {
                continue;
            }
            float ratio = data.get(limb) / BodyHealthConfig.maxHealth(limb);
            if (ratio < lowestRatio) {
                result = limb;
                lowestRatio = ratio;
            }
        }
        return result;
    }

    public static boolean isPartDamaged(ServerPlayer player, BodyPart part) {
        return isDamaged(BodyHealthData.load(player), part);
    }

    public static boolean needsHealing(ServerPlayer player) {
        return BodyHealthData.load(player).needsHealing();
    }

    public static void restoreAfterTotem(ServerPlayer player) {
        BodyHealthData data = BodyHealthData.load(player);
        for (BodyPart part : BodyPart.values()) {
            float restoreRatio = part.isCritical() ? 0.15F : 0.10F;
            data.set(part, Math.max(data.get(part), BodyHealthConfig.maxHealth(part) * restoreRatio));
        }
        saveAndSync(player, data);
    }

    public static void reset(ServerPlayer player) {
        BodyHealthData.reset(player);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        BodyHealthData data = BodyHealthData.load(player);
        BodyHealthNetwork.sync(player, BodyHealthSnapshot.from(data));
    }

    public static void updatePenalties(ServerPlayer player) {
        BodyHealthData data = BodyHealthData.load(player);
        if (player.isAlive() && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        int destroyedLegs = destroyedCount(data, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        applyLegSpeedPenalty(player, destroyedLegs);

        if (player.tickCount % 20 == 0) {
            int destroyedArms = destroyedCount(data, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM);
            if (destroyedArms > 0) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.DIG_SLOWDOWN, 35, destroyedArms - 1,
                        false, false, true));
                player.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 35, destroyedArms - 1,
                        false, false, true));
            }
        }

        if (data.isDestroyed(BodyPart.ABDOMEN) && player.tickCount % 40 == 0) {
            player.causeFoodExhaustion(1.0F);
        }
    }

    public static void applyJumpPenalty(ServerPlayer player) {
        BodyHealthData data = BodyHealthData.load(player);
        int destroyedLegs = destroyedCount(data, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        if (destroyedLegs == 0) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        double multiplier = destroyedLegs == 1 ? 0.65D : 0.35D;
        player.setDeltaMovement(movement.x, movement.y * multiplier, movement.z);
    }

    private static boolean applyPartDamage(BodyHealthData data, BodyPart part,
                                           float damage, boolean allowOverflow) {
        if (!(damage > 0.0F) || !Float.isFinite(damage)) {
            return false;
        }

        float before = data.get(part);
        boolean wasAlreadyDestroyed = before <= 0.0001F;
        float absorbed = Math.min(before, damage);
        data.set(part, before - absorbed);
        float overflow = damage - absorbed;
        boolean fatal = part.isCritical() && data.isDestroyed(part);

        if (allowOverflow && overflow > 0.0001F && !part.isCritical()) {
            float transferMultiplier = wasAlreadyDestroyed
                    ? BodyHealthConfig.DESTROYED_PART_DAMAGE_TRANSFER_MULTIPLIER.get().floatValue()
                    : part.overflowMultiplier();
            fatal |= distributeDamage(data, overflow * transferMultiplier, part);
        }
        return fatal;
    }

    private static boolean distributeDamage(BodyHealthData data, float totalDamage,
                                            BodyPart excluded) {
        List<BodyPart> recipients = new ArrayList<>();
        for (BodyPart part : BodyPart.values()) {
            if (part != excluded && !data.isDestroyed(part)) {
                recipients.add(part);
            }
        }
        if (recipients.isEmpty()) {
            return true;
        }

        float share = totalDamage / recipients.size();
        boolean fatal = false;
        for (BodyPart recipient : recipients) {
            fatal |= applyPartDamage(data, recipient, share, false);
        }
        return fatal;
    }

    private static BodyPart mostInjured(BodyHealthData data) {
        BodyPart result = null;
        float lowestRatio = 1.0F;
        for (BodyPart part : BodyPart.values()) {
            float maximum = BodyHealthConfig.maxHealth(part);
            float current = data.get(part);
            if (current >= maximum - 0.0001F) {
                continue;
            }
            float ratio = current / maximum;
            if (result == null || ratio < lowestRatio) {
                result = part;
                lowestRatio = ratio;
            }
        }
        return result;
    }

    private static boolean isDamaged(BodyHealthData data, BodyPart part) {
        return data.get(part) < BodyHealthConfig.maxHealth(part) - 0.0001F;
    }

    private static int destroyedCount(BodyHealthData data, BodyPart first, BodyPart second) {
        int count = data.isDestroyed(first) ? 1 : 0;
        return count + (data.isDestroyed(second) ? 1 : 0);
    }

    private static void applyLegSpeedPenalty(ServerPlayer player, int destroyedLegs) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        movementSpeed.removeModifier(LEG_SPEED_MODIFIER_ID);
        if (destroyedLegs > 0) {
            double amount = destroyedLegs == 1 ? -0.25D : -0.45D;
            movementSpeed.addTransientModifier(new AttributeModifier(
                    LEG_SPEED_MODIFIER_ID, LEG_SPEED_MODIFIER_NAME, amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static float toDamagePoints(float vanillaDamage) {
        if (!(vanillaDamage > 0.0F) || !Float.isFinite(vanillaDamage)) {
            return 0.0F;
        }
        return vanillaDamage * BodyHealthConfig.DAMAGE_SCALE.get().floatValue();
    }

    private static void saveAndSync(ServerPlayer player, BodyHealthData data) {
        data.save(player);
        BodyHealthNetwork.sync(player, BodyHealthSnapshot.from(data));
    }

    private BodyHealthService() {
    }
}
