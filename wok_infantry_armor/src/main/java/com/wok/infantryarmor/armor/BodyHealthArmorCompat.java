package com.wok.infantryarmor.armor;

import com.wok.infantryarmor.WokInfantryArmorMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/** Optional reflection bridge; standalone armor never links against WOK Body Health at compile time. */
public final class BodyHealthArmorCompat {
    private static final String BODY_HEALTH_MOD_ID = "wok_body_health";
    private static final String API_CLASS = "com.wok.bodyhealth.api.BodyHealthApi";

    private static boolean attempted;
    private static Method armorResistancePart;

    public static String resolvePart(LivingEntity entity, DamageSource source) {
        if (!ModList.get().isLoaded(BODY_HEALTH_MOD_ID) || !ensureLoaded()) {
            return null;
        }
        try {
            Object result = armorResistancePart.invoke(null, entity, source);
            if (result instanceof String part && !part.isBlank()) {
                return part;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            WokInfantryArmorMod.LOGGER.error(
                    "WOK Body Health armor-coverage bridge failed; retaining all-body armor resistance.",
                    exception);
            armorResistancePart = null;
        }
        return null;
    }

    private static synchronized boolean ensureLoaded() {
        if (armorResistancePart != null) {
            return true;
        }
        if (attempted) {
            return false;
        }
        attempted = true;
        try {
            Class<?> api = Class.forName(API_CLASS);
            armorResistancePart = api.getMethod(
                    "armorResistancePart", LivingEntity.class, DamageSource.class);
            WokInfantryArmorMod.LOGGER.info("WOK Body Health body-part armor resistance enabled.");
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            WokInfantryArmorMod.LOGGER.error(
                    "WOK Body Health is installed but its optional armor API is unavailable; "
                            + "retaining all-body armor resistance.",
                    exception);
            return false;
        }
    }

    private BodyHealthArmorCompat() {
    }
}
