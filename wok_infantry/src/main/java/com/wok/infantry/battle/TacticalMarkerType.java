package com.wok.infantry.battle;

import java.util.Locale;
import java.util.Optional;

public enum TacticalMarkerType {
    RECON_CONTACT,
    INFANTRY,
    TANK,
    IFV,
    ATTACK_DIRECTION,
    DEFEND,
    RALLY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<TacticalMarkerType> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(id.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
