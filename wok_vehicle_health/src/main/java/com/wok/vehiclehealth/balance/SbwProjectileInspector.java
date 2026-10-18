package com.wok.vehiclehealth.balance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;

/** Optional, reflection-only inspection of Superb Warfare projectile data. */
public final class SbwProjectileInspector {
    private static final String CANNON_SHELL_ID = "superbwarfare:cannon_shell";
    private static final String CANNON_SHELL_CLASS =
            "com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity";
    private static volatile Field cannonShellTypeField;
    private static volatile boolean typeLookupAttempted;

    public static boolean isCannonShell(Entity entity) {
        return entity != null && CANNON_SHELL_ID.equals(
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    public static boolean isArmorPiercingCannonShell(Entity entity) {
        if (!isCannonShell(entity) || !CANNON_SHELL_CLASS.equals(entity.getClass().getName())) {
            return false;
        }
        Field field = resolveTypeField(entity.getClass());
        if (field == null) {
            return false;
        }
        try {
            Object value = field.get(entity);
            return value instanceof Enum<?> type && "AP".equals(type.name());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static Field resolveTypeField(Class<?> projectileClass) {
        if (typeLookupAttempted) {
            return cannonShellTypeField;
        }
        synchronized (SbwProjectileInspector.class) {
            if (!typeLookupAttempted) {
                try {
                    Field field = projectileClass.getDeclaredField("type");
                    field.setAccessible(true);
                    cannonShellTypeField = field;
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    cannonShellTypeField = null;
                }
                typeLookupAttempted = true;
            }
        }
        return cannonShellTypeField;
    }

    private SbwProjectileInspector() {
    }
}
