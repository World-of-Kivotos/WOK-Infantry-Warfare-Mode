package com.wok.infantry.battle;

import java.util.Locale;
import java.util.Optional;

/** The two server-authoritative sides in a WOK Infantry battle. */
public enum Faction {
    BLUE("blue"),
    RED("red");

    private final String id;

    Faction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Faction opposite() {
        return this == BLUE ? RED : BLUE;
    }

    public static Optional<Faction> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (Faction faction : values()) {
            if (faction.id.equals(normalized) || faction.name().equalsIgnoreCase(normalized)) {
                return Optional.of(faction);
            }
        }
        return Optional.empty();
    }
}
