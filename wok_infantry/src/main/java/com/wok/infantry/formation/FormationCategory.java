package com.wok.infantry.formation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Stable second-level category between a public faction and one concrete formation.
 * Categories organize the catalog; gameplay rules remain on each concrete formation.
 */
public enum FormationCategory {
    INFANTRY("infantry", "步兵营"),
    ARMORED("armored", "装甲营"),
    MOTORIZED("motorized", "摩步营"),
    MECHANIZED("mechanized", "机械化步兵营"),
    SPECIAL("special", "特种编制");

    private final String id;
    private final String displayName;

    FormationCategory(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<FormationCategory> byId(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(category -> category.id.equals(normalized))
                .findFirst();
    }
}
