package com.wok.infantry.formation;

import java.util.List;

/** Data-only policy for a destructible placed spawn object such as an outpost or rally. */
public final class FormationDeployablePolicy {
    public static final int MAX_ACTIVE = 64;
    public static final int MAX_HEALTH = 1_000_000;
    public static final int MAX_COOLDOWN_SECONDS = 86_400;

    private boolean enabled;
    private int maxActive;
    private boolean squadLeaderCanPlace;
    private boolean commanderCanPlace;
    private int maxHealth;
    private int placementCooldownSeconds;
    private int replacementCooldownSeconds;
    private int destructionCooldownSeconds;

    public FormationDeployablePolicy() {
    }

    public FormationDeployablePolicy(boolean enabled, int maxActive,
                                     boolean squadLeaderCanPlace,
                                     boolean commanderCanPlace, int maxHealth,
                                     int placementCooldownSeconds,
                                     int replacementCooldownSeconds,
                                     int destructionCooldownSeconds) {
        this.enabled = enabled;
        this.maxActive = maxActive;
        this.squadLeaderCanPlace = squadLeaderCanPlace;
        this.commanderCanPlace = commanderCanPlace;
        this.maxHealth = maxHealth;
        this.placementCooldownSeconds = placementCooldownSeconds;
        this.replacementCooldownSeconds = replacementCooldownSeconds;
        this.destructionCooldownSeconds = destructionCooldownSeconds;
    }

    public boolean enabled() {
        return enabled;
    }

    public int maxActive() {
        return maxActive;
    }

    public boolean squadLeaderCanPlace() {
        return squadLeaderCanPlace;
    }

    public boolean commanderCanPlace() {
        return commanderCanPlace;
    }

    public int maxHealth() {
        return maxHealth;
    }

    public int placementCooldownSeconds() {
        return placementCooldownSeconds;
    }

    public int replacementCooldownSeconds() {
        return replacementCooldownSeconds;
    }

    public int destructionCooldownSeconds() {
        return destructionCooldownSeconds;
    }

    public FormationDeployablePolicy copy() {
        return new FormationDeployablePolicy(enabled, maxActive, squadLeaderCanPlace,
                commanderCanPlace, maxHealth, placementCooldownSeconds,
                replacementCooldownSeconds, destructionCooldownSeconds);
    }

    void normalize(String path, List<FormationConfigDiagnostic> diagnostics) {
        maxActive = FormationValidation.clamp(maxActive, 0, MAX_ACTIVE,
                path + ".maxActive", diagnostics);
        maxHealth = FormationValidation.clamp(maxHealth, 0, MAX_HEALTH,
                path + ".maxHealth", diagnostics);
        placementCooldownSeconds = FormationValidation.clamp(placementCooldownSeconds, 0,
                MAX_COOLDOWN_SECONDS, path + ".placementCooldownSeconds", diagnostics);
        replacementCooldownSeconds = FormationValidation.clamp(replacementCooldownSeconds, 0,
                MAX_COOLDOWN_SECONDS, path + ".replacementCooldownSeconds", diagnostics);
        destructionCooldownSeconds = FormationValidation.clamp(destructionCooldownSeconds, 0,
                MAX_COOLDOWN_SECONDS, path + ".destructionCooldownSeconds", diagnostics);
        if (enabled && (maxActive == 0 || maxHealth == 0
                || !squadLeaderCanPlace && !commanderCanPlace)) {
            enabled = false;
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.DISABLED, path,
                    "deployable disabled because count, health, or placement authority is absent");
        }
    }
}
