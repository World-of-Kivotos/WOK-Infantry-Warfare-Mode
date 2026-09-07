package com.wok.infantry.formation.selection;

import java.util.List;
import java.util.Objects;

/** Public faction identity. The internal BLUE/RED battle side is intentionally not serialized. */
public record FactionSelectionView(String id,
                                   String displayName,
                                   String description,
                                   int population,
                                   int capacity,
                                   boolean available,
                                   List<FormationSelectionView> formations) {
    public FactionSelectionView {
        id = Objects.requireNonNullElse(id, "");
        displayName = Objects.requireNonNullElse(displayName, id);
        description = Objects.requireNonNullElse(description, "");
        population = Math.max(0, population);
        capacity = Math.max(0, capacity);
        formations = List.copyOf(formations == null ? List.of() : formations);
    }
}
