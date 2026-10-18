package com.wok.infantry.support.adapter;

import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportRegistry;

import java.util.Objects;

/**
 * Process-wide bootstrap used by optional integrations during common setup.
 *
 * <p>The first server-side {@link #createDefault()} freezes and caches the registry. Late
 * registration fails immediately instead of creating different catalogs for different servers.</p>
 */
public final class SupportProviders {
    private static final Object LOCK = new Object();
    private static final SupportRegistry.Builder BOOTSTRAP = SupportRegistry.builder();
    private static volatile SupportRegistry frozenRegistry;

    private SupportProviders() {
    }

    public static void registerDefinition(SupportDefinition definition) {
        synchronized (LOCK) {
            ensureRegistrationOpen();
            BOOTSTRAP.registerDefinition(Objects.requireNonNull(definition, "definition"));
        }
    }

    public static void registerProvider(SupportProvider provider) {
        synchronized (LOCK) {
            ensureRegistrationOpen();
            BOOTSTRAP.registerProvider(Objects.requireNonNull(provider, "provider"));
        }
    }

    public static void register(SupportDefinition definition, SupportProvider provider) {
        synchronized (LOCK) {
            ensureRegistrationOpen();
            BOOTSTRAP.register(Objects.requireNonNull(definition, "definition"),
                    Objects.requireNonNull(provider, "provider"));
        }
    }

    /** Returns the same validated and frozen registry for every server in this process. */
    public static SupportRegistry createDefault() {
        SupportRegistry current = frozenRegistry;
        if (current != null) {
            return current;
        }
        synchronized (LOCK) {
            current = frozenRegistry;
            if (current == null) {
                current = BOOTSTRAP.freeze();
                frozenRegistry = current;
            }
            return current;
        }
    }

    public static boolean frozen() {
        return frozenRegistry != null;
    }

    private static void ensureRegistrationOpen() {
        if (frozenRegistry != null || BOOTSTRAP.frozen()) {
            throw new IllegalStateException("Support provider registration is already frozen");
        }
    }
}
