package com.wok.infantry.network.stamina;

import com.wok.infantry.stamina.StaminaSnapshot;

import java.util.Objects;

/** Dist-safe handoff from the common stamina packet to client-only HUD/controller state. */
public final class StaminaClientPacketBridge {
    private static final Handler NOOP = snapshot -> {
    };

    private static volatile Handler handler = NOOP;
    private static StaminaSnapshot pending;

    private StaminaClientPacketBridge() {
    }

    public static synchronized void install(Handler replacement) {
        handler = Objects.requireNonNull(replacement, "replacement");
        if (pending != null) {
            StaminaSnapshot snapshot = pending;
            pending = null;
            replacement.apply(snapshot);
        }
    }

    public static synchronized void apply(StaminaSnapshot snapshot) {
        Handler current = handler;
        if (current == NOOP) {
            pending = Objects.requireNonNull(snapshot, "snapshot");
        } else {
            current.apply(Objects.requireNonNull(snapshot, "snapshot"));
        }
    }

    public static synchronized void reset() {
        handler = NOOP;
        pending = null;
    }

    @FunctionalInterface
    public interface Handler {
        void apply(StaminaSnapshot snapshot);
    }
}
