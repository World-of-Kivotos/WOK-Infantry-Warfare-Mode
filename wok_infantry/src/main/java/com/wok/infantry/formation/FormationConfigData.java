package com.wok.infantry.formation;

import com.wok.infantry.battle.Faction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Versioned, Gson-serializable catalog for public factions and their formations. */
public final class FormationConfigData {
    public static final int CURRENT_VERSION = 2;
    public static final int MAX_FACTIONS = 16;

    private int version = CURRENT_VERSION;
    private List<FactionDefinition> factions = new ArrayList<>();
    private transient List<FormationConfigDiagnostic> diagnostics = List.of();

    public FormationConfigData() {
    }

    public FormationConfigData(List<FactionDefinition> factions) {
        this(CURRENT_VERSION, factions);
    }

    public FormationConfigData(int version, List<FactionDefinition> factions) {
        this.version = version;
        this.factions = copyFactions(factions);
    }

    public int version() {
        return version;
    }

    public List<FactionDefinition> factions() {
        return List.copyOf(factions == null ? List.of() : factions);
    }

    public List<FormationConfigDiagnostic> diagnostics() {
        return List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    /** Canonicalizes all input and records every removal, repair, or fail-closed disablement. */
    public void normalize() {
        List<FormationConfigDiagnostic> repairs = new ArrayList<>();
        if (version != CURRENT_VERSION) {
            FormationValidation.add(repairs, FormationConfigDiagnostic.Kind.NORMALIZED,
                    "version", "configuration version normalized to " + CURRENT_VERSION);
        }
        version = CURRENT_VERSION;

        List<FactionDefinition> normalizedFactions = new ArrayList<>();
        Set<String> factionIds = new LinkedHashSet<>();
        Set<Faction> battleSides = new LinkedHashSet<>();
        if (factions != null) {
            for (int index = 0; index < factions.size(); index++) {
                if (normalizedFactions.size() >= MAX_FACTIONS) {
                    FormationValidation.add(repairs,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            "factions", "extra faction definitions removed");
                    break;
                }
                FactionDefinition faction = factions.get(index);
                String path = "factions[" + index + "]";
                if (faction == null || !faction.normalize(path, repairs)) {
                    continue;
                }
                if (!factionIds.add(faction.id())) {
                    FormationValidation.add(repairs,
                            FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE, path,
                            "duplicate public faction id removed: " + faction.id());
                    continue;
                }
                if (!battleSides.add(faction.battleSide())) {
                    FormationValidation.add(repairs,
                            FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE, path,
                            "duplicate battle side removed: " + faction.battleSideId());
                    continue;
                }
                normalizedFactions.add(faction);
            }
        }
        factions = normalizedFactions;
        diagnostics = Collections.unmodifiableList(repairs);
    }

    public FormationConfigData normalizedCopy() {
        FormationConfigData copy = copy();
        copy.normalize();
        return copy;
    }

    public Optional<FactionDefinition> findFaction(String publicFactionId) {
        String normalized = FormationValidation.id(publicFactionId,
                FormationValidation.MAX_ID_LENGTH);
        return normalized == null ? Optional.empty() : factions().stream()
                .filter(faction -> normalized.equals(faction.id())).findFirst();
    }

    public Optional<FactionDefinition> findFaction(Faction battleSide) {
        return battleSide == null ? Optional.empty() : factions().stream()
                .filter(faction -> faction.battleSide() == battleSide).findFirst();
    }

    public Optional<FormationDefinition> findFormation(String publicFactionId,
                                                        String formationId) {
        return findFaction(publicFactionId).flatMap(faction ->
                faction.findFormation(formationId));
    }

    public Optional<FormationDefinition> findFormation(Faction battleSide,
                                                        String formationId) {
        return findFaction(battleSide).flatMap(faction -> faction.findFormation(formationId));
    }

    /** Returns a result only when a formation id is globally unambiguous. */
    public Optional<FormationDefinition> findFormation(String formationId) {
        List<FormationDefinition> matches = formationsById(formationId);
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    public List<FormationDefinition> formationsById(String formationId) {
        String normalized = FormationValidation.id(formationId,
                FormationValidation.MAX_ID_LENGTH);
        if (normalized == null) {
            return List.of();
        }
        List<FormationDefinition> matches = new ArrayList<>();
        factions().forEach(faction -> faction.formations().stream()
                .filter(formation -> normalized.equals(formation.id()))
                .forEach(matches::add));
        return List.copyOf(matches);
    }

    public FormationConfigData copy() {
        FormationConfigData copy = new FormationConfigData(version, factions);
        copy.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
        return copy;
    }

    public static FormationConfigData defaultConfig() {
        FormationDefinition standard = defaultFormation();
        FormationDefinition millenniumMobile = millenniumMobileFormation();
        FormationConfigData data = new FormationConfigData(List.of(
                new FactionDefinition("academy", "学院军", "学院军战场阵营",
                        Faction.BLUE, true, 40, List.of(standard, millenniumMobile)),
                new FactionDefinition("caesar", "凯撒", "凯撒战场阵营",
                        Faction.RED, true, 40, List.of(standard.copy()))));
        data.normalize();
        return data;
    }

    private static FormationDefinition defaultFormation() {
        return new FormationDefinition("default", "常规编制", "默认步兵常规编制",
                true, 40, defaultClasses(), defaultSquads(), List.of());
    }

    private static FormationDefinition millenniumMobileFormation() {
        List<FormationVehicleDefinition> vehicles = List.of(
                new FormationVehicleDefinition("m1296_dragoon_1", "M1296 龙骑兵",
                        "fcp:stryker_dragoon", -10.0D, 0.0D, 6.0D, 0.0F, -1),
                new FormationVehicleDefinition("m1296_dragoon_2", "M1296 龙骑兵",
                        "fcp:stryker_dragoon", -5.0D, 0.0D, 6.0D, 0.0F, -1),
                new FormationVehicleDefinition("m1296_dragoon_3", "M1296 龙骑兵",
                        "fcp:stryker_dragoon", 0.0D, 0.0D, 6.0D, 0.0F, -1),
                new FormationVehicleDefinition("m1128_mgs_1", "M1128 MGS",
                        "fcp:stryker_mgs", 5.0D, 0.0D, 6.0D, 0.0F, -1),
                new FormationVehicleDefinition("m1128_mgs_2", "M1128 MGS",
                        "fcp:stryker_mgs", 10.0D, 0.0D, 6.0D, 0.0F, -1),
                new FormationVehicleDefinition("hmmwv_m2_1", "悍马 M2",
                        "fcp:hmmwv_armored_m2", -4.0D, 0.0D, 13.0D, 0.0F, 300),
                new FormationVehicleDefinition("hmmwv_m2_2", "悍马 M2",
                        "fcp:hmmwv_armored_m2", 4.0D, 0.0D, 13.0D, 0.0F, 300),
                new FormationVehicleDefinition("littlebird_armed", "小鸟 机枪版",
                        "fcp:littlebird_armed", 0.0D, 0.0D, 23.0D, 0.0F, 600));
        return new FormationDefinition("millennium_seminar_mobile",
                "千禧年研讨会机动部队",
                "学院军所属的千禧年研讨会快速机动编制。",
                "wok_infantry:textures/gui/formations/millennium_seminar_mobile.png",
                FormationCategory.MECHANIZED, true, 40,
                new FormationCapabilityProfile(), defaultClasses(), defaultSquads(), vehicles);
    }

    private static List<FormationClassRule> defaultClasses() {
        return List.of(
                new FormationClassRule("assault", "突击兵", 8, Map.of()),
                new FormationClassRule("support", "支援兵", 2, Map.of()),
                new FormationClassRule("engineer", "工程兵", 2, Map.of()),
                new FormationClassRule("recon", "侦察兵", 1, Map.of()));
    }

    private static List<FormationSquadDefinition> defaultSquads() {
        LinkedHashMap<String, Integer> limits = new LinkedHashMap<>();
        limits.put("assault", 8);
        limits.put("support", 2);
        limits.put("engineer", 2);
        limits.put("recon", 1);
        List<FormationSquadDefinition> squads = new ArrayList<>();
        for (String callsign : List.of("alpha", "bravo", "charlie", "delta", "echo")) {
            String display = Character.toUpperCase(callsign.charAt(0)) + callsign.substring(1);
            squads.add(new FormationSquadDefinition(callsign, display, 8, limits));
        }
        return List.copyOf(squads);
    }

    private static List<FactionDefinition> copyFactions(List<FactionDefinition> source) {
        List<FactionDefinition> copy = new ArrayList<>();
        if (source != null) {
            source.stream().filter(Objects::nonNull).map(FactionDefinition::copy)
                    .forEach(copy::add);
        }
        return copy;
    }
}
