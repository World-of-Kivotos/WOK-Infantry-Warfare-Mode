package com.wok.infantry.battle;

import java.util.Locale;
import java.util.Optional;

/** Fixed squad slots. Five eight-player squads exactly fill one forty-player faction. */
public enum SquadCallsign {
    ALPHA("alpha"),
    BRAVO("bravo"),
    CHARLIE("charlie"),
    DELTA("delta"),
    ECHO("echo");

    private final String id;

    SquadCallsign(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<SquadCallsign> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (SquadCallsign callsign : values()) {
            if (callsign.id.equals(normalized) || callsign.name().equalsIgnoreCase(normalized)) {
                return Optional.of(callsign);
            }
        }
        return Optional.empty();
    }
}
