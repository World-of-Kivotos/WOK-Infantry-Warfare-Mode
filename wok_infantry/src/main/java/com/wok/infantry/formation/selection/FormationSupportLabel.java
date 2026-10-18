package com.wok.infantry.formation.selection;

import java.util.Objects;

/**
 * Display name source of one commander support in the formation catalog. The client shows the
 * translation of {@code translationKey} when its language has it (the support add-on ships the
 * keys) and otherwise the server-side {@code fallbackName}.
 */
public record FormationSupportLabel(String id, String translationKey, String fallbackName) {
    public FormationSupportLabel {
        id = Objects.requireNonNullElse(id, "");
        translationKey = Objects.requireNonNullElse(translationKey, "");
        fallbackName = Objects.requireNonNullElse(fallbackName, id);
    }
}
