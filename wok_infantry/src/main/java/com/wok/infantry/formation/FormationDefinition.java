package com.wok.infantry.formation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** A selectable military formation and its server-owned composition rules. */
public final class FormationDefinition {
    public static final int MAX_CLASSES = 64;
    public static final int MAX_SQUADS = FormationValidation.MAX_SQUADS;
    public static final int MAX_VEHICLES = 64;

    private String id = "default";
    private String displayName = "常规编制";
    private String description = "";
    private String icon = "";
    private String category = FormationCategory.INFANTRY.id();
    private boolean enabled = true;
    private int capacity = 40;
    private FormationCapabilityProfile capabilities = new FormationCapabilityProfile();
    private List<FormationClassRule> classes = new ArrayList<>();
    private List<FormationSquadDefinition> squads = new ArrayList<>();
    private List<FormationVehicleDefinition> vehicles = new ArrayList<>();

    public FormationDefinition() {
    }

    public FormationDefinition(String id, String displayName, String description,
                               boolean enabled, int capacity,
                               List<FormationClassRule> classes,
                               List<FormationSquadDefinition> squads,
                               List<FormationVehicleDefinition> vehicles) {
        this(id, displayName, description, FormationCategory.INFANTRY, enabled, capacity,
                new FormationCapabilityProfile(), classes, squads, vehicles);
    }

    public FormationDefinition(String id, String displayName, String description,
                               FormationCategory category, boolean enabled, int capacity,
                               FormationCapabilityProfile capabilities,
                               List<FormationClassRule> classes,
                               List<FormationSquadDefinition> squads,
                               List<FormationVehicleDefinition> vehicles) {
        this(id, displayName, description, "", category, enabled, capacity, capabilities,
                classes, squads, vehicles);
    }

    public FormationDefinition(String id, String displayName, String description, String icon,
                               FormationCategory category, boolean enabled, int capacity,
                               FormationCapabilityProfile capabilities,
                               List<FormationClassRule> classes,
                               List<FormationSquadDefinition> squads,
                               List<FormationVehicleDefinition> vehicles) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.category = category == null ? null : category.id();
        this.enabled = enabled;
        this.capacity = capacity;
        this.capabilities = capabilities == null
                ? new FormationCapabilityProfile() : capabilities.copy();
        this.classes = copyClasses(classes);
        this.squads = copySquads(squads);
        this.vehicles = copyVehicles(vehicles);
    }

    public String id() {
        return Objects.requireNonNullElse(id, "");
    }

    public String displayName() {
        return Objects.requireNonNullElse(displayName, id());
    }

    public String description() {
        return Objects.requireNonNullElse(description, "");
    }

    public String icon() {
        return Objects.requireNonNullElse(icon, "");
    }

    public String categoryId() {
        return Objects.requireNonNullElse(category, "");
    }

    public FormationCategory category() {
        return FormationCategory.byId(categoryId()).orElse(null);
    }

    public boolean enabled() {
        return enabled;
    }

    public int capacity() {
        return capacity;
    }

    public FormationCapabilityProfile capabilities() {
        return capabilities == null ? new FormationCapabilityProfile() : capabilities.copy();
    }

    public List<FormationClassRule> classes() {
        return List.copyOf(classes == null ? List.of() : classes);
    }

    public List<FormationSquadDefinition> squads() {
        return List.copyOf(squads == null ? List.of() : squads);
    }

    public List<FormationVehicleDefinition> vehicles() {
        return List.copyOf(vehicles == null ? List.of() : vehicles);
    }

    public Optional<FormationClassRule> findClass(String classId) {
        String normalized = FormationValidation.id(classId, FormationValidation.MAX_ID_LENGTH);
        return normalized == null ? Optional.empty() : classes().stream()
                .filter(rule -> normalized.equals(rule.classId())).findFirst();
    }

    /**
     * The first profession that can absorb every squad slot is the safe fallback/default.
     * Display order is deliberately independent, so a limited squad-leader role may appear first.
     */
    public Optional<FormationClassRule> defaultClass() {
        List<FormationClassRule> configured = classes();
        List<FormationSquadDefinition> configuredSquads = squads();
        return configured.stream().filter(rule -> configuredSquads.stream().allMatch(squad ->
                        squad.classLimit(rule.classId(), rule.squadLimit()) >= squad.capacity()))
                .findFirst().or(() -> configured.stream().findFirst());
    }

    public Optional<FormationSquadDefinition> findSquad(String callsign) {
        String normalized = FormationValidation.callsign(callsign);
        return normalized == null ? Optional.empty() : squads().stream()
                .filter(squad -> normalized.equals(squad.callsign())).findFirst();
    }

    public Optional<FormationVehicleDefinition> findVehicle(String vehicleId) {
        String normalized = FormationValidation.id(vehicleId, FormationValidation.MAX_ID_LENGTH);
        return normalized == null ? Optional.empty() : vehicles().stream()
                .filter(vehicle -> normalized.equals(vehicle.id())).findFirst();
    }

    public FormationDefinition copy() {
        return new FormationDefinition(id(), displayName(), description(), icon(), category(),
                enabled, capacity, capabilities, classes, squads, vehicles);
    }

    boolean normalize(String path, int factionCapacity,
                      List<FormationConfigDiagnostic> diagnostics) {
        String normalizedId = FormationValidation.id(id, FormationValidation.MAX_ID_LENGTH);
        FormationCategory normalizedCategory = FormationCategory.byId(category).orElse(null);
        if (normalizedId == null || normalizedCategory == null) {
            FormationValidation.add(diagnostics,
                    FormationConfigDiagnostic.Kind.REMOVED_INVALID, path,
                    "formation has an invalid id or category");
            return false;
        }
        if (!normalizedId.equals(id)) {
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".id", "formation id normalized to " + normalizedId);
        }
        id = normalizedId;
        if (!normalizedCategory.id().equals(category)) {
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".category", "category normalized to " + normalizedCategory.id());
        }
        category = normalizedCategory.id();
        displayName = FormationValidation.displayName(displayName, id,
                path + ".displayName", diagnostics);
        description = FormationValidation.description(description,
                path + ".description", diagnostics);
        icon = FormationValidation.optionalResourceId(icon, path + ".icon", diagnostics);
        capacity = FormationValidation.clamp(capacity, 1,
                Math.max(1, Math.min(FormationValidation.MAX_PLAYERS, factionCapacity)),
                path + ".capacity", diagnostics);

        List<FormationClassRule> normalizedClasses = new ArrayList<>();
        Set<String> classIds = new LinkedHashSet<>();
        if (classes != null) {
            for (int index = 0; index < classes.size(); index++) {
                if (normalizedClasses.size() >= MAX_CLASSES) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            path + ".classes", "extra class rules removed");
                    break;
                }
                FormationClassRule rule = classes.get(index);
                String childPath = path + ".classes[" + index + "]";
                if (rule == null || !rule.normalize(childPath, diagnostics)) {
                    continue;
                }
                if (!classIds.add(rule.classId())) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE, childPath,
                            "duplicate class rule removed: " + rule.classId());
                    continue;
                }
                normalizedClasses.add(rule);
            }
        }
        classes = normalizedClasses;

        List<FormationSquadDefinition> normalizedSquads = new ArrayList<>();
        Set<String> squadIds = new LinkedHashSet<>();
        if (squads != null) {
            for (int index = 0; index < squads.size(); index++) {
                if (normalizedSquads.size() >= MAX_SQUADS) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            path + ".squads", "extra squad definitions removed");
                    break;
                }
                FormationSquadDefinition squad = squads.get(index);
                String childPath = path + ".squads[" + index + "]";
                if (squad == null || !squad.normalize(childPath, classIds, diagnostics)) {
                    continue;
                }
                if (!squadIds.add(squad.callsign())) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE, childPath,
                            "duplicate squad removed: " + squad.callsign());
                    continue;
                }
                normalizedSquads.add(squad);
            }
        }
        squads = normalizedSquads;

        List<FormationVehicleDefinition> normalizedVehicles = new ArrayList<>();
        Set<String> vehicleIds = new LinkedHashSet<>();
        if (vehicles != null) {
            for (int index = 0; index < vehicles.size(); index++) {
                if (normalizedVehicles.size() >= MAX_VEHICLES) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            path + ".vehicles", "extra vehicle definitions removed");
                    break;
                }
                FormationVehicleDefinition vehicle = vehicles.get(index);
                String childPath = path + ".vehicles[" + index + "]";
                if (vehicle == null || !vehicle.normalize(childPath, diagnostics)) {
                    continue;
                }
                if (!vehicleIds.add(vehicle.id())) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE, childPath,
                            "duplicate vehicle removed: " + vehicle.id());
                    continue;
                }
                normalizedVehicles.add(vehicle);
            }
        }
        vehicles = normalizedVehicles;

        if (capabilities == null) {
            capabilities = new FormationCapabilityProfile();
        }
        capabilities.normalize(path + ".capabilities", vehicleIds, diagnostics);

        if (enabled && (classes.isEmpty() || squads.isEmpty())) {
            enabled = false;
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.DISABLED, path,
                    "formation disabled because it has no valid classes or squads");
        }
        return true;
    }

    private static List<FormationClassRule> copyClasses(List<FormationClassRule> source) {
        List<FormationClassRule> copy = new ArrayList<>();
        if (source != null) {
            source.stream().filter(Objects::nonNull).map(FormationClassRule::copy)
                    .forEach(copy::add);
        }
        return copy;
    }

    private static List<FormationSquadDefinition> copySquads(
            List<FormationSquadDefinition> source) {
        List<FormationSquadDefinition> copy = new ArrayList<>();
        if (source != null) {
            source.stream().filter(Objects::nonNull).map(FormationSquadDefinition::copy)
                    .forEach(copy::add);
        }
        return copy;
    }

    private static List<FormationVehicleDefinition> copyVehicles(
            List<FormationVehicleDefinition> source) {
        List<FormationVehicleDefinition> copy = new ArrayList<>();
        if (source != null) {
            source.stream().filter(Objects::nonNull).map(FormationVehicleDefinition::copy)
                    .forEach(copy::add);
        }
        return copy;
    }
}
