package com.wok.infantry.formation;

import com.wok.infantry.battle.Faction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Public faction identity mapped onto one existing blue/red battle side. */
public final class FactionDefinition {
    public static final int MAX_FORMATIONS = 32;

    private String id = "faction";
    private String displayName = "Faction";
    private String description = "";
    /** Gson-facing lower-case id; use {@link #battleSide()} from Java. */
    private String battleSide = "blue";
    private boolean enabled = true;
    private int maxPlayers = 40;
    private List<FormationDefinition> formations = new ArrayList<>();

    public FactionDefinition() {
    }

    public FactionDefinition(String id, String displayName, String description,
                             Faction battleSide, boolean enabled, int maxPlayers,
                             List<FormationDefinition> formations) {
        this(id, displayName, description,
                battleSide == null ? null : battleSide.id(), enabled, maxPlayers, formations);
    }

    public FactionDefinition(String id, String displayName, String description,
                             String battleSide, boolean enabled, int maxPlayers,
                             List<FormationDefinition> formations) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.battleSide = battleSide;
        this.enabled = enabled;
        this.maxPlayers = maxPlayers;
        this.formations = copyFormations(formations);
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

    public String battleSideId() {
        return Objects.requireNonNullElse(battleSide, "");
    }

    public Faction battleSide() {
        return Faction.byId(battleSideId()).orElse(null);
    }

    public boolean enabled() {
        return enabled;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public List<FormationDefinition> formations() {
        return List.copyOf(formations == null ? List.of() : formations);
    }

    public Optional<FormationDefinition> findFormation(String formationId) {
        String normalized = FormationValidation.id(formationId,
                FormationValidation.MAX_ID_LENGTH);
        return normalized == null ? Optional.empty() : formations().stream()
                .filter(formation -> normalized.equals(formation.id())).findFirst();
    }

    public FactionDefinition copy() {
        return new FactionDefinition(id(), displayName(), description(), battleSideId(), enabled,
                maxPlayers, formations);
    }

    boolean normalize(String path, List<FormationConfigDiagnostic> diagnostics) {
        String normalizedId = FormationValidation.id(id, FormationValidation.MAX_ID_LENGTH);
        Faction normalizedSide = Faction.byId(battleSide).orElse(null);
        if (normalizedId == null || normalizedSide == null) {
            FormationValidation.add(diagnostics,
                    FormationConfigDiagnostic.Kind.REMOVED_INVALID, path,
                    "faction has an invalid id or battleSide (expected blue/red)");
            return false;
        }
        if (!normalizedId.equals(id)) {
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".id", "faction id normalized to " + normalizedId);
        }
        if (!normalizedSide.id().equals(battleSide)) {
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".battleSide", "battleSide normalized to " + normalizedSide.id());
        }
        id = normalizedId;
        battleSide = normalizedSide.id();
        displayName = FormationValidation.displayName(displayName, id,
                path + ".displayName", diagnostics);
        description = FormationValidation.description(description,
                path + ".description", diagnostics);
        maxPlayers = FormationValidation.clamp(maxPlayers, 1,
                FormationValidation.MAX_PLAYERS, path + ".maxPlayers", diagnostics);

        List<FormationDefinition> normalizedFormations = new ArrayList<>();
        Set<String> formationIds = new LinkedHashSet<>();
        if (formations != null) {
            for (int index = 0; index < formations.size(); index++) {
                if (normalizedFormations.size() >= MAX_FORMATIONS) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            path + ".formations", "extra formation definitions removed");
                    break;
                }
                FormationDefinition formation = formations.get(index);
                String childPath = path + ".formations[" + index + "]";
                if (formation == null
                        || !formation.normalize(childPath, maxPlayers, diagnostics)) {
                    continue;
                }
                if (!formationIds.add(formation.id())) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE, childPath,
                            "duplicate formation removed: " + formation.id());
                    continue;
                }
                normalizedFormations.add(formation);
            }
        }
        formations = normalizedFormations;
        if (enabled && formations.stream().noneMatch(FormationDefinition::enabled)) {
            enabled = false;
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.DISABLED, path,
                    "faction disabled because it has no enabled formations");
        }
        return true;
    }

    private static List<FormationDefinition> copyFormations(List<FormationDefinition> source) {
        List<FormationDefinition> copy = new ArrayList<>();
        if (source != null) {
            source.stream().filter(Objects::nonNull).map(FormationDefinition::copy)
                    .forEach(copy::add);
        }
        return copy;
    }
}
