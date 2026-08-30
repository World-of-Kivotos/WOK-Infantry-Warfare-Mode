package com.wok.infantry.formation;

import java.util.Objects;

/** One deterministic repair made while normalizing a formation configuration. */
public record FormationConfigDiagnostic(Kind kind, String path, String message) {
    public FormationConfigDiagnostic {
        Objects.requireNonNull(kind, "kind");
        path = Objects.requireNonNullElse(path, "");
        message = Objects.requireNonNullElse(message, "");
    }

    public enum Kind {
        NORMALIZED,
        REMOVED_INVALID,
        REMOVED_DUPLICATE,
        DISABLED
    }
}
