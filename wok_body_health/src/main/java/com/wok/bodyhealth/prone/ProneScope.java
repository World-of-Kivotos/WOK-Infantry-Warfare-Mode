package com.wok.bodyhealth.prone;

/**
 * Marks that the current thread is inside a gun mod's {@code find*OnPath} for one projectile, so
 * the Level hook only widens that broad-phase query and never an explosion or other lookup that
 * also passes the bullet as {@code except}. One slot per thread; the first matching query
 * consumes it.
 */
public final class ProneScope {
    private static final ThreadLocal<Object> SLOT = new ThreadLocal<>();

    /** Opens the scope for {@code projectile}, replacing any leaked one. */
    public static void enter(Object projectile) {
        if (projectile == null) {
            SLOT.remove();
        } else {
            SLOT.set(projectile);
        }
    }

    /** Closes the scope if it still belongs to {@code projectile}. */
    public static void exit(Object projectile) {
        if (projectile != null && SLOT.get() == projectile) {
            SLOT.remove();
        }
    }

    /** True once if the open scope belongs to {@code except}; the scope is cleared then. */
    public static boolean consume(Object except) {
        if (except == null || SLOT.get() != except) {
            return false;
        }
        SLOT.remove();
        return true;
    }

    public static void reset() {
        SLOT.remove();
    }

    private ProneScope() {
    }
}
