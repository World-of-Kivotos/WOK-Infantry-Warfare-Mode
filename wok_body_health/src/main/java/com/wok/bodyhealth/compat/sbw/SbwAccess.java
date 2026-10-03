package com.wok.bodyhealth.compat.sbw;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Superb Warfare members the prone hook needs, reached by reflection only: SBW is never on this
 * module's classpath, and a missing member must switch the feature off rather than break the
 * mixin. Resolved once on first use; any failure disables SBW prone handling for good.
 */
public final class SbwAccess {
    private static final String PROJECTILE_CLASS = "com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity";
    private static final String RESULT_CLASS = "com.atsuishio.superbwarfare.world.phys.EntityResult";
    private static final Logger LOGGER = LogUtils.getLogger();

    private record Handles(Class<?> projectile, MethodHandle beast, MethodHandle explosionDamage,
                           MethodHandle explosionBullet, MethodHandle shooter, MethodHandle newResult) {
    }

    private static volatile Handles handles;
    private static volatile boolean resolved;

    /** True when every SBW member resolved; resolves on the first call. */
    public static boolean ready(Entity projectile) {
        return resolve(projectile) != null;
    }

    public static boolean isProjectile(Entity e) {
        Handles h = resolve(e);
        return h != null && h.projectile().isInstance(e);
    }

    /** Beast rounds inflate the box by 3 blocks; they are left to SBW. */
    public static boolean isBeast(Entity p) {
        try {
            return (boolean) require().beast().invoke(p);
        } catch (Throwable throwable) {
            throw rethrow(throwable);
        }
    }

    public static float explosionDamage(Entity p) {
        try {
            return (float) require().explosionDamage().invoke(p);
        } catch (Throwable throwable) {
            throw rethrow(throwable);
        }
    }

    /** SBW's {@code shooter} field via {@code getShooter()}; this is what SBW rewinds with. */
    public static Entity shooter(Entity p) {
        try {
            return (Entity) require().shooter().invoke(p);
        } catch (Throwable throwable) {
            throw rethrow(throwable);
        }
    }

    public static Object newEntityResult(Entity target, Vec3 hit, boolean head, boolean leg) {
        try {
            return require().newResult().invoke(target, hit, head, leg);
        } catch (Throwable throwable) {
            throw rethrow(throwable);
        }
    }

    /** {@code projectile.explosionBullet(projectile, hit)}: SBW passes the projectile itself as the entity. */
    public static void explosionBullet(Entity p, Vec3 hit) {
        try {
            require().explosionBullet().invoke(p, p, hit);
        } catch (Throwable throwable) {
            throw rethrow(throwable);
        }
    }

    private static Handles require() {
        Handles h = handles;
        if (h == null) {
            throw new IllegalStateException("SBW members are not resolved");
        }
        return h;
    }

    private static Handles resolve(Entity hint) {
        if (!resolved) {
            synchronized (SbwAccess.class) {
                if (!resolved) {
                    handles = lookup(hint);
                    resolved = true;
                }
            }
        }
        return handles;
    }

    private static Handles lookup(Entity hint) {
        try {
            ClassLoader loader = hint != null ? hint.getClass().getClassLoader() : SbwAccess.class.getClassLoader();
            Class<?> projectile = load(PROJECTILE_CLASS, loader);
            Class<?> result = load(RESULT_CLASS, projectile.getClassLoader());
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            Field beast = projectile.getDeclaredField("beast");
            beast.setAccessible(true);
            Field explosionDamage = projectile.getDeclaredField("explosionDamage");
            explosionDamage.setAccessible(true);
            Method explosionBullet = projectile.getDeclaredMethod("explosionBullet", Entity.class, Vec3.class);
            explosionBullet.setAccessible(true);
            Method shooter = projectile.getMethod("getShooter");
            Constructor<?> newResult = result.getConstructor(Entity.class, Vec3.class, boolean.class, boolean.class);

            return new Handles(projectile,
                    lookup.unreflectGetter(beast).asType(MethodType.methodType(boolean.class, Entity.class)),
                    lookup.unreflectGetter(explosionDamage).asType(MethodType.methodType(float.class, Entity.class)),
                    lookup.unreflect(explosionBullet)
                            .asType(MethodType.methodType(void.class, Entity.class, Entity.class, Vec3.class)),
                    lookup.unreflect(shooter).asType(MethodType.methodType(Entity.class, Entity.class)),
                    lookup.unreflectConstructor(newResult).asType(MethodType.methodType(
                            Object.class, Entity.class, Vec3.class, boolean.class, boolean.class)));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            LOGGER.warn("Superb Warfare is installed, but its projectile members cannot be reached; "
                    + "SBW bullets keep their original prone hitbox.", exception);
            return null;
        }
    }

    private static Class<?> load(String name, ClassLoader loader) throws ClassNotFoundException {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException exception) {
            return Class.forName(name, false, SbwAccess.class.getClassLoader());
        }
    }

    private static RuntimeException rethrow(Throwable throwable) {
        if (throwable instanceof RuntimeException runtime) {
            return runtime;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(throwable);
    }

    private SbwAccess() {
    }
}
