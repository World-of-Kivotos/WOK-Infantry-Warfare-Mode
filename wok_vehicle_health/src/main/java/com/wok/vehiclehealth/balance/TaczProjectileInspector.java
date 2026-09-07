package com.wok.vehiclehealth.balance;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/** Reflection-only TaCZ bridge so TaCZ remains an optional runtime dependency. */
public final class TaczProjectileInspector {
    private static final String CARL_GUSTAF_ID = "carl_gustaf_m4:carl_gustaf_m4";
    private static final String M72_ID = "m72_law:m72_law";

    public static Optional<InfantryAntiTankWeapon> inspect(Entity projectile) {
        if (projectile == null
                || !projectile.getClass().getName().equals("com.tacz.guns.entity.EntityKineticBullet")) {
            return Optional.empty();
        }
        try {
            Method getGunId = projectile.getClass().getMethod("getGunId");
            Object gunId = getGunId.invoke(projectile);
            if (gunId == null) {
                return Optional.empty();
            }
            String id = gunId.toString();
            if (M72_ID.equals(id)) {
                return Optional.of(InfantryAntiTankWeapon.M72_LIGHT_AT);
            }
            if (!CARL_GUSTAF_ID.equals(id)) {
                return Optional.empty();
            }
            float radius = readFloat(projectile, "explosionRadius").orElse(0.0F);
            return Optional.of(radius > 4.0F
                    ? InfantryAntiTankWeapon.CARL_GUSTAF_THERMOBARIC
                    : InfantryAntiTankWeapon.CARL_GUSTAF_HEAT);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<InfantryAntiTankWeapon> inspectExplosion(Object explosion) {
        if (explosion == null
                || !explosion.getClass().getName().equals("com.tacz.guns.util.block.ProjectileExplosion")) {
            return Optional.empty();
        }
        try {
            Field owner = findField(explosion.getClass(), "owner");
            owner.setAccessible(true);
            Object projectile = owner.get(explosion);
            return projectile instanceof Entity entity ? inspect(entity) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Float> readFloat(Object target, String name) {
        try {
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            return Optional.of(field.getFloat(target));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private TaczProjectileInspector() {
    }
}
