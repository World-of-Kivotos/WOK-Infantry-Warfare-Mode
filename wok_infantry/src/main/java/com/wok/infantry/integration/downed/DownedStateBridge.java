package com.wok.infantry.integration.downed;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.CombatantStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Optional, soft bridge to WOK步战附属-倒地救援 ({@code wok_downed}). The core never links
 * against the module: when it is installed the bridge asks its own
 * {@code DownedService.isDowned(Player)} through reflection, and if that API is missing or fails it
 * falls back to the registry/NBT probe in {@link CombatantStatus}. Every failure is logged once.
 * Called on the server thread while building battle snapshots.
 */
public final class DownedStateBridge {
    static final String MOD_ID = "wok_downed";
    static final String SERVICE_CLASS = "com.wok.downed.state.DownedService";
    static final String METHOD_NAME = "isDowned";

    private static final Probe<Player> PROBE = new Probe<>(
            DownedStateBridge::modLoaded,
            DownedStateBridge::resolveService,
            CombatantStatus::isDowned,
            (message, error) -> {
                if (error == null) {
                    WokInfantryMod.LOGGER.info(message);
                } else {
                    WokInfantryMod.LOGGER.warn(message, error);
                }
            });

    private DownedStateBridge() {
    }

    /** True only when the downed module is installed and reports the player as downed. */
    public static boolean isDowned(Player player) {
        return player != null && PROBE.test(player);
    }

    private static boolean modLoaded() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(MOD_ID);
    }

    private static Predicate<Player> resolveService() {
        Method method;
        try {
            method = Class.forName(SERVICE_CLASS).getMethod(METHOD_NAME, Player.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
        if (method.getReturnType() != boolean.class) {
            return null;
        }
        return player -> {
            try {
                return (boolean) method.invoke(null, player);
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
     * generic so the policy is unit-testable without Forge or a player entity.
     */
    static final class Probe<T> {
        private final BooleanSupplier installed;
        private final Supplier<Predicate<T>> resolver;
        private final Predicate<T> fallback;
        private final BiConsumer<String, Throwable> log;
        private final AtomicBoolean failureLogged = new AtomicBoolean();
        private volatile boolean resolved;
        private volatile boolean active;
        private volatile Predicate<T> primary;

        Probe(BooleanSupplier installed, Supplier<Predicate<T>> resolver,
              Predicate<T> fallback, BiConsumer<String, Throwable> log) {
            this.installed = Objects.requireNonNull(installed, "installed");
            this.resolver = Objects.requireNonNull(resolver, "resolver");
            this.fallback = Objects.requireNonNull(fallback, "fallback");
            this.log = Objects.requireNonNull(log, "log");
        }

        boolean test(T subject) {
            if (!resolved) {
                resolve();
            }
            if (!active) {
                return false;
            }
            Predicate<T> api = primary;
            if (api != null) {
                try {
                    return api.test(subject);
                } catch (RuntimeException | LinkageError exception) {
                    primary = null;
                    logOnce("WOK downed state API failed; using the downed effect probe instead.",
                            exception);
                }
            }
            try {
                return fallback.test(subject);
            } catch (RuntimeException | LinkageError exception) {
                active = false;
                logOnce("WOK downed state probe failed; squad rosters will not show downed members.",
                        exception);
                return false;
            }
        }

        private synchronized void resolve() {
            if (resolved) {
                return;
            }
            boolean present;
            try {
                present = installed.getAsBoolean();
            } catch (RuntimeException | LinkageError exception) {
                present = false;
            }
            if (present) {
                Predicate<T> api = null;
                try {
                    api = resolver.get();
                } catch (RuntimeException | LinkageError exception) {
                    api = null;
                }
                primary = api;
                if (api == null) {
                    logOnce("Installed WOK downed module has no isDowned API; "
                            + "using the downed effect probe instead.", null);
                }
            }
            active = present;
            resolved = true;
        }

        private void logOnce(String message, Throwable error) {
            if (failureLogged.compareAndSet(false, true)) {
                log.accept(message, error);
            }
        }
    }
}
