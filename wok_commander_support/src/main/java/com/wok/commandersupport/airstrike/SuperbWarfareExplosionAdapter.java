package com.wok.commandersupport.airstrike;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Optional reflective bridge to Superb Warfare's event-aware custom explosion.
 *
 * <p>Public so the howitzer barrages ({@code com.wok.commandersupport.artillery}) share the same
 * binding; the signatures only expose vanilla types. Block damage is left to Superb Warfare's own
 * {@code EXPLOSION_DESTROY} server setting for every caller: the builder default is never
 * overridden here.</p>
 */
public final class SuperbWarfareExplosionAdapter {
    static final String MOD_ID = "superbwarfare";
    static final String BUILDER_CLASS =
            "com.atsuishio.superbwarfare.tools.CustomExplosion$Builder";
    static final String PARTICLE_TYPE_CLASS =
            "com.atsuishio.superbwarfare.tools.ParticleTool$ParticleType";
    static final String PARTICLE_TYPE = "GIANT";
    static final float EXPLOSION_DAMAGE = 1_000.0F;
    static final float EXPLOSION_RADIUS = 32.0F;

    private static volatile Binding cachedBinding;

    private SuperbWarfareExplosionAdapter() {
    }

    public static boolean available() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        try {
            binding();
            return true;
        } catch (ReflectiveOperationException | LinkageError failure) {
            return false;
        }
    }

    /**
     * Resolves the binding and the named particle type without exploding anything, so a caller
     * can find a missing or renamed Superb Warfare API before it delivers any ordnance. A missing
     * particle constant surfaces as the {@link java.lang.reflect.InvocationTargetException} that
     * {@link #explode(Entity, Entity, Vec3, float, float, String)} would throw later.
     */
    public static void verify(String particleTypeName) throws ReflectiveOperationException {
        if (particleTypeName == null || particleTypeName.isBlank()) {
            throw new IllegalArgumentException("Invalid Superb Warfare particle type");
        }
        Binding binding = binding();
        binding.particleType().getMethod("valueOf", String.class)
                .invoke(null, particleTypeName);
    }

    static void explode(Entity directSource, Entity attacker, Vec3 position)
            throws ReflectiveOperationException {
        explode(directSource, attacker, position, EXPLOSION_DAMAGE,
                EXPLOSION_RADIUS, PARTICLE_TYPE);
    }

    public static void explode(Entity directSource, Entity attacker, Vec3 position,
                               float damage, float radius, String particleTypeName)
            throws ReflectiveOperationException {
        if (!Float.isFinite(damage) || damage <= 0.0F
                || !Float.isFinite(radius) || radius <= 0.0F
                || particleTypeName == null || particleTypeName.isBlank()) {
            throw new IllegalArgumentException("Invalid Superb Warfare explosion parameters");
        }
        Binding binding = binding();
        Object builder = binding.constructor().newInstance(directSource);
        builder = binding.attacker().invoke(builder, attacker);
        builder = binding.damage().invoke(builder, damage);
        builder = binding.radius().invoke(builder, radius);
        builder = binding.position().invoke(builder, position);
        Object particleType = binding.particleType().getMethod("valueOf", String.class)
                .invoke(null, particleTypeName);
        builder = binding.withParticleType().invoke(builder, particleType);
        binding.explode().invoke(builder);
    }

    private static Binding binding() throws ReflectiveOperationException {
        Binding existing = cachedBinding;
        if (existing != null) {
            return existing;
        }
        synchronized (SuperbWarfareExplosionAdapter.class) {
            existing = cachedBinding;
            if (existing != null) {
                return existing;
            }
            Class<?> builderClass = Class.forName(BUILDER_CLASS);
            Class<?> particleType = Class.forName(PARTICLE_TYPE_CLASS);
            if (!particleType.isEnum()) {
                throw new ReflectiveOperationException(
                        "Superb Warfare particle type is no longer an enum");
            }
            Binding resolved = new Binding(
                    builderClass.getConstructor(Entity.class),
                    builderClass.getMethod("attacker", Entity.class),
                    builderClass.getMethod("damage", float.class),
                    builderClass.getMethod("radius", float.class),
                    builderClass.getMethod("position", Vec3.class),
                    builderClass.getMethod("withParticleType", particleType),
                    builderClass.getMethod("explode"),
                    particleType);
            cachedBinding = resolved;
            return resolved;
        }
    }

    private record Binding(Constructor<?> constructor, Method attacker,
                           Method damage, Method radius, Method position,
                           Method withParticleType, Method explode,
                           Class<?> particleType) {
    }
}
