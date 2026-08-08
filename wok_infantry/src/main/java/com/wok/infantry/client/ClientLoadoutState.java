package com.wok.infantry.client;

import com.wok.infantry.loadout.LoadoutSnapshot;

public final class ClientLoadoutState {
    private static LoadoutSnapshot snapshot;

    private ClientLoadoutState() {
    }

    public static LoadoutSnapshot snapshot() {
        return snapshot;
    }

    public static void update(LoadoutSnapshot value) {
        snapshot = value;
        if (snapshot != null && snapshot.config() != null) {
            snapshot.config().normalize();
        }
    }
}
