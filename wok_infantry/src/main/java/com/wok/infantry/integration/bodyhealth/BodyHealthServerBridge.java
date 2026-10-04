package com.wok.infantry.integration.bodyhealth;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.MemberView;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Server-side health ratio for squad rosters, softly aware of WOK步战附属-部位血量
 * ({@code wok_body_health}).
 *
 * <ul>
 *     <li>Body health not installed: the vanilla {@code health / maxHealth} ratio.</li>
 *     <li>Installed with a {@code BodyHealthApi.healthRatio(LivingEntity)} API: its value.</li>
 *     <li>Installed without that API (every release up to 0.1.0-beta.10), or the API failed:
 *     {@link MemberView#UNKNOWN_HEALTH_RATIO}. Vanilla health is not meaningful while body parts
 *     carry the damage, so clients draw no health bar.</li>
 * </ul>
 * The core never links against the module; failures are logged once.
 */
public final class BodyHealthServerBridge {
    static final String MOD_ID = "wok_body_health";
    static final String API_CLASS = "com.wok.bodyhealth.api.BodyHealthApi";
    static final String METHOD_NAME = "healthRatio";

    private static final Source<LivingEntity> SOURCE = new Source<>(
            BodyHealthServerBridge::modLoaded,
            BodyHealthServerBridge::resolveApi,
            entity -> MemberView.vanillaHealthRatio(entity.getHealth(), entity.getMaxHealth()),
            (message, error) -> {
                if (error == null) {
                    WokInfantryMod.LOGGER.info(message);
                } else {
                    WokInfantryMod.LOGGER.warn(message, error);
                }
            });

    private BodyHealthServerBridge() {
    }

    /** A ratio in [0, 1], or {@link MemberView#UNKNOWN_HEALTH_RATIO}. */
    public static float ratio(LivingEntity entity) {
        return entity == null ? MemberView.UNKNOWN_HEALTH_RATIO : SOURCE.ratio(entity);
    }

    /** Converts an API result to a wire ratio; anything but a finite number is unknown. */
    static float normalize(Object raw) {
        if (!(raw instanceof Number number)) {
            return MemberView.UNKNOWN_HEALTH_RATIO;
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value)) {
            return MemberView.UNKNOWN_HEALTH_RATIO;
        }
        return MemberView.normalizeHealthRatio((float) value);
    }

    private static boolean modLoaded() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(MOD_ID);
    }

    private static Function<LivingEntity, Object> resolveApi() {
        Method method;
        try {
            method = Class.forName(API_CLASS).getMethod(METHOD_NAME, LivingEntity.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
        Class<?> type = method.getReturnType();
        if (type != float.class && type != double.class
                && !Number.class.isAssignableFrom(type)) {
            return null;
        }
        return entity -> {
            try {
                return method.invoke(null, entity);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException(cause);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        };
    }

    /**
     * Installation check, one-time API resolution and fail-once fallback. Package-visible and
     * generic so the policy is unit-testable without Forge or an entity.
     */
    static final class Source<T> {
        private final BooleanSupplier installed;
        private final Supplier<Function<T, Object>> resolver;
        private final Function<T, Float> vanilla;
        private final BiConsumer<String, Throwable> log;
        private final AtomicBoolean failureLogged = new AtomicBoolean();
        private volatile boolean resolved;
        private volatile boolean present;
        private volatile Function<T, Object> api;

        Source(BooleanSupplier installed, Supplier<Function<T, Object>> resolver,
               Function<T, Float> vanilla, BiConsumer<String, Throwable> log) {
            this.installed = Objects.requireNonNull(installed, "installed");
            this.resolver = Objects.requireNonNull(resolver, "resolver");
            this.vanilla = Objects.requireNonNull(vanilla, "vanilla");
            this.log = Objects.requireNonNull(log, "log");
        }

        float ratio(T subject) {
            if (!resolved) {
                resolve();
            }
            if (!present) {
                try {
                    Float value = vanilla.apply(subject);
                    return value == null ? MemberView.UNKNOWN_HEALTH_RATIO
                            : MemberView.normalizeHealthRatio(value);
                } catch (RuntimeException exception) {
                    logOnce("Could not read vanilla health for the squad roster.", exception);
                    return MemberView.UNKNOWN_HEALTH_RATIO;
                }
            }
            Function<T, Object> current = api;
            if (current == null) {
                return MemberView.UNKNOWN_HEALTH_RATIO;
            }
            try {
                return normalize(current.apply(subject));
            } catch (RuntimeException | LinkageError exception) {
                api = null;
                logOnce("WOK Body Health ratio API failed; squad rosters stop showing health bars.",
                        exception);
                return MemberView.UNKNOWN_HEALTH_RATIO;
            }
        }

        private synchronized void resolve() {
            if (resolved) {
                return;
            }
            boolean loaded;
            try {
                loaded = installed.getAsBoolean();
            } catch (RuntimeException | LinkageError exception) {
                loaded = false;
            }
            if (loaded) {
                Function<T, Object> resolvedApi;
                try {
                    resolvedApi = resolver.get();
                } catch (RuntimeException | LinkageError exception) {
                    resolvedApi = null;
                }
                api = resolvedApi;
                if (resolvedApi == null) {
                    logOnce("Installed WOK Body Health has no healthRatio API; "
                            + "squad rosters show no health bars.", null);
                }
            }
            present = loaded;
            resolved = true;
        }

        private void logOnce(String message, Throwable error) {
            if (failureLogged.compareAndSet(false, true)) {
                log.accept(message, error);
            }
        }
    }
}
