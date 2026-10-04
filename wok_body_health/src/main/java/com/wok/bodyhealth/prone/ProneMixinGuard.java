package com.wok.bodyhealth.prone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Last line of defence inside mixin handlers. The first failure of a key prints its stack trace
 * and switches that key off, so a broken hook falls back to the gun mod's own code instead of
 * failing every shot. Depends on the JDK and log4j only, so it always links.
 */
public final class ProneMixinGuard {
    private static final Logger LOG = LogManager.getLogger("wok_body_health/mixin");
    private static final Set<String> OFF = ConcurrentHashMap.newKeySet();
    /** Fast path for the Level hook, which runs on every entity query. */
    private static volatile boolean anyOff;

    public static void report(String key, Throwable t) {
        if (OFF.add(key)) {
            anyOff = true;
            LOG.error("WOK Body Health prone hook '{}' failed and is now disabled; "
                    + "the original hit detection stays in place.", key, t);
        }
    }

    public static boolean off(String key) {
        return anyOff && OFF.contains(key);
    }

    /** Switches every key back on; tests only, the game keeps a failed hook off until restart. */
    static void resetForTests() {
        OFF.clear();
        anyOff = false;
    }

    private ProneMixinGuard() {
    }
}
