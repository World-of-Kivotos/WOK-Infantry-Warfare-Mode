package com.wok.infantry.support.adapter;

import java.util.Objects;
import java.util.UUID;

/** One short-lived entity contact published by a server-side support provider. */
public record SupportIntelContact(UUID entityId, double x, double y, double z) {
    public SupportIntelContact {
        Objects.requireNonNull(entityId, "entityId");
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Support intel " + field + " must be finite");
        }
    }
}
