package com.wok.trauma.compat.bodyhealth;

import com.wok.trauma.WokTraumaMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class BodyHealthCompat {
    private static final String API_CLASS = "com.wok.bodyhealth.api.BodyHealthApi";

    private static boolean resolved;
    private static Method applyBleedingDamage;
    private static Method heal;
    private static Method needsHealing;
    private static Method selectTreatmentPart;
    private static Method isPartDamaged;
    private static Method healPart;
    private static boolean available;

    public static boolean applyBleedingDamage(LivingEntity entity, DamageSource source,
                                              float amount) {
        resolve();
        return invokeBoolean(applyBleedingDamage, entity, source, amount);
    }

    public static boolean heal(LivingEntity entity, float amount) {
        resolve();
        return invokeBoolean(heal, entity, amount);
    }

    public static boolean needsHealing(LivingEntity entity) {
        resolve();
        return invokeBoolean(needsHealing, entity);
    }

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    public static String selectTreatmentPart(LivingEntity entity) {
        resolve();
        return invokeString(selectTreatmentPart, entity);
    }

    public static boolean healPart(LivingEntity entity, String partName, float amount) {
        resolve();
        return invokeBoolean(healPart, entity, partName, amount);
    }

    public static boolean isPartDamaged(LivingEntity entity, String partName) {
        resolve();
        return invokeBoolean(isPartDamaged, entity, partName);
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!ModList.get().isLoaded("wok_body_health")) {
            return;
        }

        try {
            Class<?> api = Class.forName(API_CLASS);
            applyBleedingDamage = api.getMethod(
                    "applyBleedingDamage", LivingEntity.class, DamageSource.class, float.class);
            heal = api.getMethod("heal", LivingEntity.class, float.class);
            needsHealing = api.getMethod("needsHealing", LivingEntity.class);
            selectTreatmentPart = api.getMethod(
                    "selectTreatmentPart", LivingEntity.class);
            isPartDamaged = api.getMethod(
                    "isPartDamaged", LivingEntity.class, String.class);
            healPart = api.getMethod(
                    "healPart", LivingEntity.class, String.class, float.class);
            available = true;
            WokTraumaMod.LOGGER.info("WOK body-health treatment integration enabled.");
        } catch (ReflectiveOperationException exception) {
            WokTraumaMod.LOGGER.error(
                    "WOK Body Health is installed, but its public API is unavailable", exception);
        }
    }

    private static boolean invokeBoolean(Method method, Object... arguments) {
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null, arguments));
        } catch (IllegalAccessException | InvocationTargetException exception) {
            WokTraumaMod.LOGGER.error("WOK body-health integration call failed", exception);
            return false;
        }
    }

    private static String invokeString(Method method, Object... arguments) {
        if (method == null) {
            return "";
        }
        try {
            Object result = method.invoke(null, arguments);
            return result instanceof String value ? value : "";
        } catch (IllegalAccessException | InvocationTargetException exception) {
            WokTraumaMod.LOGGER.error("WOK body-health integration call failed", exception);
            return "";
        }
    }

    private BodyHealthCompat() {
    }
}
