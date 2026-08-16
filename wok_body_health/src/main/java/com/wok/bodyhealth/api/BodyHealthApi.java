package com.wok.bodyhealth.api;

import com.wok.bodyhealth.config.BodyHealthConfig;
import com.wok.bodyhealth.event.BodyHealthEvents;
import com.wok.bodyhealth.health.BodyHealthData;
import com.wok.bodyhealth.health.BodyHealthService;
import com.wok.bodyhealth.health.DamageOutcome;
import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class BodyHealthApi {
    /**
     * Reflection-friendly bridge for WOK Trauma's silent bleeding damage.
     */
    public static boolean applyBleedingDamage(LivingEntity entity, DamageSource source,
                                              float vanillaAmount) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        DamageOutcome outcome = BodyHealthService.applySystemic(player, vanillaAmount);
        if (outcome.fatal()) {
            BodyHealthEvents.hurtBypassingBodyHealth(player, source);
        }
        return true;
    }

    /**
     * Reflection-friendly bridge for medical items.
     */
    public static boolean heal(LivingEntity entity, float vanillaAmount) {
        return entity instanceof ServerPlayer player
                && BodyHealthService.heal(player, vanillaAmount);
    }

    /**
     * Reflection-friendly bridge for regeneration injectors. Each pulse
     * restores the supplied amount to every damaged body part.
     */
    public static boolean healAllParts(LivingEntity entity, float vanillaAmount) {
        return entity instanceof ServerPlayer player
                && BodyHealthService.healAllParts(player, vanillaAmount);
    }

    public static boolean needsHealing(LivingEntity entity) {
        return entity instanceof ServerPlayer player
                && BodyHealthService.needsHealing(player);
    }

    /**
     * Selects one treatment target for a continuous medical-kit use. The
     * caller keeps this name until the player releases and uses the kit again.
     */
    public static String selectTreatmentPart(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return "";
        }
        BodyPart part = BodyHealthService.defaultTreatmentPart(player);
        return part == null ? "" : part.name();
    }

    public static boolean isPartDamaged(LivingEntity entity, String partName) {
        if (!(entity instanceof ServerPlayer player) || partName == null) {
            return false;
        }
        try {
            return BodyHealthService.isPartDamaged(
                    player, BodyPart.valueOf(partName));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Heals only the named part. It deliberately never redirects excess or a
     * completed treatment tick into a different body part.
     */
    public static boolean healPart(LivingEntity entity, String partName,
                                   float vanillaAmount) {
        if (!(entity instanceof ServerPlayer player) || partName == null) {
            return false;
        }
        try {
            return BodyHealthService.healPart(
                    player, BodyPart.valueOf(partName), vanillaAmount);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static String lastDamagedPart(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return BodyHealthData.load(player).lastDamagedPart().name();
        }
        return "CHEST";
    }

    /**
     * Optional armor bridge. A blank result means the armor should retain its original all-body behavior.
     * A body-part name means a coverage-aware armor may decide whether this hit is protected.
     */
    public static String armorResistancePart(LivingEntity entity, DamageSource source) {
        if (!BodyHealthConfig.armorBodyPartResistanceEnabled()
                || !(entity instanceof ServerPlayer player)
                || source == null
                || !BodyHealthEvents.isLocalizedDamage(source)) {
            return "";
        }
        return BodyHealthEvents.resolveOrRecordArmorPart(player, source).name();
    }

    private BodyHealthApi() {
    }
}
