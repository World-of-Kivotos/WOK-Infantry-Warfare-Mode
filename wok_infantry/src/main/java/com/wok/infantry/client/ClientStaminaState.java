package com.wok.infantry.client;

import com.wok.infantry.network.stamina.StaminaClientPacketBridge;
import com.wok.infantry.stamina.StaminaSnapshot;

/** Latest server-authoritative stamina values for local HUD and TaCZ sway. */
public final class ClientStaminaState {
    private static final StaminaSnapshot EMPTY = new StaminaSnapshot(100.0F, 100.0F, false);
    private static volatile StaminaSnapshot snapshot = EMPTY;

    private ClientStaminaState() {
    }

    public static void install() {
        StaminaClientPacketBridge.install(ClientStaminaState::update);
    }

    public static StaminaSnapshot snapshot() {
        return snapshot;
    }

    public static void update(StaminaSnapshot replacement) {
        snapshot = replacement;
    }

    public static void clear() {
        snapshot = EMPTY;
    }
}
