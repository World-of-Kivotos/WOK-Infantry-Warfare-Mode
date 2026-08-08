package com.wok.infantryarmor.armor;

import java.util.Locale;
import java.util.Optional;

/** Stable seven-part vocabulary shared with optional body-health integrations. */
public enum ProtectedBodyPart {
    HEAD("head"),
    CHEST("chest"),
    ABDOMEN("abdomen"),
    LEFT_ARM("left_arm"),
    RIGHT_ARM("right_arm"),
    LEFT_LEG("left_leg"),
    RIGHT_LEG("right_leg");

    private final String id;

    ProtectedBodyPart(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "body_part.wok_infantry_armor." + id;
    }

    /** Accepts both the stable lower-case id and enum-style names used by external mods. */
    public static Optional<ProtectedBodyPart> fromExternalName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        for (ProtectedBodyPart part : values()) {
            if (part.id.equals(normalized)) {
                return Optional.of(part);
            }
        }
        return Optional.empty();
    }
}
