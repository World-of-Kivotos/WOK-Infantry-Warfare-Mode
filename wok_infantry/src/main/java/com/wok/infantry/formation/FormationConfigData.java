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
        FormationDefinition millenniumCavalry = millenniumCavalryFormation();
        FormationDefinition caesar234Mechanized = caesar234MechanizedFormation();
        FormationConfigData data = new FormationConfigData(List.of(
                new FactionDefinition("academy", "学院军", "学院军战场阵营",
                        Faction.BLUE, true, 40,
                        List.of(standard, millenniumMobile, millenniumCavalry)),
                new FactionDefinition("caesar", "凯撒", "凯撒战场阵营",
                        Faction.RED, true, 40,
                        List.of(standard.copy(), caesar234Mechanized))));
        data.normalize();
        return data;
    }

    private static FormationDefinition defaultFormation() {
        return new FormationDefinition("default", "常规编制", "默认步兵常规编制",
                FormationCategory.INFANTRY, true, 40, defaultCapabilities(),
                defaultClasses(), defaultSquads(), List.of());
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
                "由千禧年研讨会统一组建的先头力量，承担快速部署与快速反应任务。"
                        + "她们以多型斯特赖克（Stryker）轮式战车为核心，在航空兵支援下可于战斗初期"
                        + "投入大量轻型装甲载具；但后劲不足、单兵装备较为平庸，不擅长长时间消耗战。",
                "wok_infantry:textures/gui/formations/millennium_seminar_mobile.png",
                FormationCategory.MECHANIZED, true, 40,
                millenniumCapabilities(), defaultClasses(), defaultSquads(), vehicles);
    }

    private static FormationDefinition caesar234MechanizedFormation() {
        List<FormationVehicleDefinition> vehicles = List.of(
                new FormationVehicleDefinition("leopard_2a4", "豹2A4",
                        "dragonrise_reforge:leopard2a4",
                        0.0D, 0.0D, 6.0D, 0.0F, 900),
                new FormationVehicleDefinition("cv90_1", "CV90",
                        "dragonrise_reforge:cv90",
                        -8.0D, 0.0D, 14.0D, 0.0F, 900),
                new FormationVehicleDefinition("cv90_2", "CV90",
                        "dragonrise_reforge:cv90",
                        8.0D, 0.0D, 14.0D, 0.0F, 900),
                new FormationVehicleDefinition("hmmwv_armored_unarmed_1",
                        "装甲无武装 HMMWV", "fcp:hmmwv_armored_unarmed",
                        -4.0D, 0.0D, 22.0D, 0.0F, 300),
                new FormationVehicleDefinition("hmmwv_armored_unarmed_2",
                        "装甲无武装 HMMWV", "fcp:hmmwv_armored_unarmed",
                        4.0D, 0.0D, 22.0D, 0.0F, 300));
        return new FormationDefinition("caesar_234_mechanized",
                "234机械化作战单元",
                "凯撒重工是凯撒公司旗下的重型军事生产企业，战争前几乎包揽了整个基沃托斯的"
                        + "军火与军用车辆生产，234机械化作战单元则是其核心作战力量之一。"
                        + "该单元以CV90步兵战车伴随推进，并配备略显过时的豹2A4主战坦克，"
                        + "作战职能偏向机动轻步兵。其装备水平尚可，擅长在复杂战线中进行混战缠斗；"
                        + "但重型载具数量有限、战损补充缓慢，一旦脱离步兵协同或分散投入，"
                        + "便容易失去进攻节奏。",
                FormationCategory.MECHANIZED, true, 40, defaultCapabilities(),
                defaultClasses(), defaultSquads(), vehicles);
    }

    private static FormationDefinition millenniumCavalryFormation() {
        List<FormationVehicleDefinition> vehicles = List.of(
                new FormationVehicleDefinition("m1a2_sep_tusk_ii_1",
                        "M1A2 SEP TUSK II", "dragonrise_reforge:m1a2sepv2",
                        -8.0D, 0.0D, 6.0D, 0.0F, 1200),
                new FormationVehicleDefinition("m1a2_sep_tusk_ii_2",
                        "M1A2 SEP TUSK II", "dragonrise_reforge:m1a2sepv2",
                        8.0D, 0.0D, 6.0D, 0.0F, 1200),
                new FormationVehicleDefinition("m3a3_bradley",
                        "M3A3 布莱德利", "dragonrise_reforge:m3a3",
                        -5.0D, 0.0D, 16.0D, 0.0F, 900),
                new FormationVehicleDefinition("m3a3_bradley_recon",
                        "M3A3 布莱德利侦察型", "dragonrise_reforge:m3a3",
                        5.0D, 0.0D, 16.0D, 0.0F, 900));
        return new FormationDefinition("millennium_seminar_cavalry_corps",
                "研讨会骑兵军团",
                "学院军所属的研讨会装甲旅级编制。",
                FormationCategory.ARMORED, true, 40, defaultCapabilities(),
                defaultClasses(), defaultSquads(), vehicles);
    }

    private static FormationCapabilityProfile defaultCapabilities() {
        return new FormationCapabilityProfile(
                new FormationDeployablePolicy(),
                new FormationDeployablePolicy(true, 1, true, true,
                        100, 480, 480, 480),
                new FormationRespawnPolicy(),
                new FormationSupportPolicy(FormationSupportPolicy.Mode.ALLOW_LIST,
                        List.of("wok_commander_support:recon_satellite",
                                "wok_commander_support:f16c_gbu12_paveway_500lb")));
    }

    private static FormationCapabilityProfile millenniumCapabilities() {
        return new FormationCapabilityProfile(
                new FormationDeployablePolicy(),
                new FormationDeployablePolicy(true, 1, true, true,
                        100, 480, 480, 480),
                new FormationRespawnPolicy(),
                new FormationSupportPolicy(FormationSupportPolicy.Mode.ALLOW_LIST,
                        List.of("wok_commander_support:recon_satellite",
                                "wok_commander_support:millennium_f15ex_jdam_1000lb",
                                "wok_commander_support:f16c_gbu12_paveway_500lb")));
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
