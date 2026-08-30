package com.wok.infantry.formation.selection;

import java.util.List;
import java.util.Objects;

/** Bounded, display-only formation summary; all authoritative composition remains on the server. */
public record FormationSelectionView(String id,
                                     String displayName,
                                     String description,
                                     String iconId,
                                     String categoryId,
                                     String categoryDisplayName,
                                     int population,
                                     int capacity,
                                     boolean available,
                                     String unavailableReason,
                                     List<String> classes,
                                     List<String> squads,
                                     List<String> vehicles,
                                     List<String> capabilities) {
    public FormationSelectionView(String id, String displayName, String description,
                                  int population, int capacity, boolean available,
                                  String unavailableReason, List<String> classes,
                                  List<String> squads, List<String> vehicles) {
        this(id, displayName, description, "", "infantry", "步兵营", population,
                capacity, available, unavailableReason, classes, squads, vehicles, List.of());
    }

    public FormationSelectionView(String id, String displayName, String description,
                                  String categoryId, String categoryDisplayName,
                                  int population, int capacity, boolean available,
                                  String unavailableReason, List<String> classes,
                                  List<String> squads, List<String> vehicles,
                                  List<String> capabilities) {
        this(id, displayName, description, "", categoryId, categoryDisplayName, population,
                capacity, available, unavailableReason, classes, squads, vehicles,
                capabilities);
    }

    public FormationSelectionView {
        id = Objects.requireNonNullElse(id, "");
        displayName = Objects.requireNonNullElse(displayName, id);
        description = Objects.requireNonNullElse(description, "");
        iconId = Objects.requireNonNullElse(iconId, "");
        categoryId = Objects.requireNonNullElse(categoryId, "");
        categoryDisplayName = Objects.requireNonNullElse(categoryDisplayName, categoryId);
        population = Math.max(0, population);
        capacity = Math.max(0, capacity);
        unavailableReason = Objects.requireNonNullElse(unavailableReason, "");
        classes = List.copyOf(classes == null ? List.of() : classes);
        squads = List.copyOf(squads == null ? List.of() : squads);
        vehicles = List.copyOf(vehicles == null ? List.of() : vehicles);
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
    }
}
