package com.wok.capturepoints.capture;

import java.util.Locale;
import java.util.Optional;

public enum CaptureTeam {
    NEUTRAL("neutral", 0),
    BLUE("blue", 1),
    RED("red", -1);

    private final String id;
    private final int direction;

    CaptureTeam(String id, int direction) {
        this.id = id;
        this.direction = direction;
    }

    public String id() {
        return id;
    }

    public int direction() {
        return direction;
    }

    public static Optional<CaptureTeam> byId(String id) {
        if (id == null) return Optional.empty();
        String value = id.trim().toLowerCase(Locale.ROOT);
        for (CaptureTeam team : values()) {
            if (team.id.equals(value) || team.name().equalsIgnoreCase(value)) {
                return Optional.of(team);
            }
        }
        return Optional.empty();
    }
}
