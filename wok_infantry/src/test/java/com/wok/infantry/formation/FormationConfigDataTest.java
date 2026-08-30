package com.wok.infantry.formation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.Faction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationConfigDataTest {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    @Test
    void defaultCatalogDefinesBothPublicFactionsAndConventionalFormations() {
        FormationConfigData data = FormationConfigData.defaultConfig();

        assertEquals(2, data.version());
        assertTrue(data.diagnostics().isEmpty());
        assertEquals(2, data.factions().size());

        FactionDefinition academy = data.findFaction(" ACADEMY ").orElseThrow();
        FactionDefinition caesar = data.findFaction(Faction.RED).orElseThrow();
        assertEquals(Faction.BLUE, academy.battleSide());
        assertEquals("学院军", academy.displayName());
        assertEquals("caesar", caesar.id());

        FormationDefinition academyDefault = data.findFormation("academy", "default")
                .orElseThrow();
        FormationDefinition caesarDefault = data.findFormation(Faction.RED, "default")
                .orElseThrow();
        assertTrue(academyDefault.enabled());
        assertTrue(caesarDefault.enabled());
        assertEquals(40, academyDefault.capacity());
        assertEquals(FormationCategory.INFANTRY, academyDefault.category());
        assertFalse(academyDefault.capabilities().outpost().enabled());
        assertFalse(academyDefault.capabilities().rally().enabled());
        assertEquals(FormationRespawnPolicy.INHERIT_GLOBAL_DELAY,
                academyDefault.capabilities().respawn().delaySeconds());
        assertEquals(FormationSupportPolicy.Mode.NONE,
                academyDefault.capabilities().support().mode());
        assertEquals(List.of("assault", "support", "engineer", "recon"),
                academyDefault.classes().stream().map(FormationClassRule::classId).toList());
        assertEquals(List.of("alpha", "bravo", "charlie", "delta", "echo"),
                academyDefault.squads().stream()
                        .map(FormationSquadDefinition::callsign).toList());
        assertTrue(academyDefault.squads().stream()
                .allMatch(squad -> squad.capacity() == 8));
        assertTrue(academyDefault.vehicles().isEmpty());

        FormationDefinition millennium = data.findFormation("academy",
                "millennium_seminar_mobile").orElseThrow();
        assertEquals("千禧年研讨会机动部队", millennium.displayName());
        assertEquals("wok_infantry:textures/gui/formations/"
                + "millennium_seminar_mobile.png", millennium.icon());
        assertEquals(FormationCategory.MECHANIZED, millennium.category());
        assertEquals(8, millennium.vehicles().size());
        assertEquals(3, millennium.vehicles().stream()
                .filter(vehicle -> vehicle.entityId().equals("fcp:stryker_dragoon"))
                .filter(vehicle -> vehicle.replenishmentCooldownSeconds() == -1).count());
        assertEquals(2, millennium.vehicles().stream()
                .filter(vehicle -> vehicle.entityId().equals("fcp:stryker_mgs"))
                .filter(vehicle -> vehicle.replenishmentCooldownSeconds() == -1).count());
        assertEquals(2, millennium.vehicles().stream()
                .filter(vehicle -> vehicle.entityId().equals("fcp:hmmwv_armored_m2"))
                .filter(vehicle -> vehicle.replenishmentCooldownSeconds() == 300).count());
        assertEquals(1, millennium.vehicles().stream()
                .filter(vehicle -> vehicle.entityId().equals("fcp:littlebird_armed"))
                .filter(vehicle -> vehicle.replenishmentCooldownSeconds() == 600).count());

        assertEquals(2, data.formationsById("default").size());
        assertTrue(data.findFormation("default").isEmpty(),
                "a formation-only lookup must fail closed when the id is ambiguous");
    }

    @Test
    void gsonRoundTripUsesLowerCaseBattleSideAndNeverSerializesDiagnostics() {
        FormationConfigData source = FormationConfigData.defaultConfig();
        String json = GSON.toJson(source);

        assertTrue(json.contains("\"battleSide\":\"blue\""));
        assertTrue(json.contains("\"battleSide\":\"red\""));
        assertFalse(json.contains("diagnostics"));

        FormationConfigData decoded = GSON.fromJson(json, FormationConfigData.class);
        decoded.normalize();
        assertEquals(2, decoded.factions().size());
        assertEquals(Faction.BLUE, decoded.findFaction("academy").orElseThrow().battleSide());
        assertEquals(5, decoded.findFormation("caesar", "default")
                .orElseThrow().squads().size());
        assertTrue(decoded.diagnostics().isEmpty());
    }

    @Test
    void normalizationBoundsContentDeduplicatesAndFailsClosed() {
        Map<String, List<String>> allowedEntries = new LinkedHashMap<>();
        allowedEntries.put(" Primary ", List.of("pack/rifle", "PACK/RIFLE", "bad entry"));
        FormationClassRule support = new FormationClassRule(" Support ", "", 99,
                allowedEntries);
        FormationSquadDefinition alpha = new FormationSquadDefinition(
                " Alpha ", "", 3, Map.of(" SUPPORT ", 99, "unknown", 2));

        List<FormationVehicleDefinition> vehicles = new ArrayList<>();
        vehicles.add(new FormationVehicleDefinition(" Tank ", "", "MOD:TANK",
                1.0D, 2.0D, 3.0D, 450.0F));
        vehicles.add(new FormationVehicleDefinition("tank", "Duplicate", "mod:tank_two",
                0.0D, 0.0D, 0.0D, 0.0F));
        vehicles.add(new FormationVehicleDefinition("broken", "Broken", "not-an-entity-id",
                0.0D, 0.0D, 0.0D, 0.0F));
        for (int index = 0; index < FormationDefinition.MAX_VEHICLES + 5; index++) {
            vehicles.add(new FormationVehicleDefinition("extra_" + index, "Extra " + index,
                    "mod:extra_" + index, 0.0D, 0.0D, 0.0D, 0.0F));
        }

        FormationDefinition valid = new FormationDefinition(" Default ", "", "x".repeat(700),
                true, 999, List.of(support), List.of(alpha), vehicles);
        FormationDefinition duplicate = valid.copy();
        FormationDefinition incomplete = new FormationDefinition("empty", "Empty", "",
                true, 10, List.of(), List.of(), List.of());
        FactionDefinition academy = new FactionDefinition(" Academy ", "", "y".repeat(700),
                " BLUE ", true, 999, List.of(valid, duplicate, incomplete));
        FactionDefinition duplicateSide = new FactionDefinition("other", "Other", "",
                Faction.BLUE, true, 40, List.of(valid));
        FactionDefinition invalid = new FactionDefinition("bad id", "Bad", "",
                "green", true, 40, List.of(valid));
        FormationConfigData data = new FormationConfigData(99,
                List.of(academy, duplicateSide, invalid));

        data.normalize();

        assertEquals(2, data.version());
        assertEquals(1, data.factions().size());
        FactionDefinition normalizedFaction = data.findFaction("academy").orElseThrow();
        assertEquals(BattleRules.FACTION_CAPACITY, normalizedFaction.maxPlayers());
        assertEquals("academy", normalizedFaction.displayName());
        assertEquals(FormationValidation.MAX_DESCRIPTION_LENGTH,
                normalizedFaction.description().length());
        assertEquals(2, normalizedFaction.formations().size());
        assertFalse(normalizedFaction.findFormation("empty").orElseThrow().enabled());

        FormationDefinition normalized = normalizedFaction.findFormation("default").orElseThrow();
        assertEquals(BattleRules.FACTION_CAPACITY, normalized.capacity());
        assertEquals(FormationValidation.MAX_DESCRIPTION_LENGTH,
                normalized.description().length());
        FormationClassRule normalizedSupport = normalized.findClass("support").orElseThrow();
        assertEquals(BattleRules.SQUAD_CAPACITY, normalizedSupport.squadLimit());
        assertEquals(List.of("pack/rifle"), normalizedSupport.allowedEntriesFor("primary"));
        assertTrue(normalizedSupport.allowsEntry("secondary", "anything"));
        assertFalse(normalizedSupport.allowsEntry("primary", "other/rifle"));
        FormationSquadDefinition normalizedAlpha = normalized.findSquad("alpha").orElseThrow();
        assertEquals("alpha", normalizedAlpha.displayName());
        assertEquals(Map.of("support", 3), normalizedAlpha.classLimits());
        assertEquals(FormationDefinition.MAX_VEHICLES, normalized.vehicles().size());
        FormationVehicleDefinition tank = normalized.findVehicle("tank").orElseThrow();
        assertEquals("mod:tank", tank.entityId());
        assertEquals(90.0F, tank.yaw());

        assertTrue(data.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.kind() == FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE));
        assertTrue(data.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.kind() == FormationConfigDiagnostic.Kind.REMOVED_INVALID));
        assertTrue(data.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.kind() == FormationConfigDiagnostic.Kind.DISABLED));
        assertTrue(data.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.kind() == FormationConfigDiagnostic.Kind.NORMALIZED));
    }

    @Test
    void categoryAndCapabilitiesRoundTripAndFailClosedAtTheirBoundaries() {
        FormationDeployablePolicy outpost = new FormationDeployablePolicy(true, 3,
                true, true, 400, 90, 60, 120);
        FormationDeployablePolicy rally = new FormationDeployablePolicy(true, 1,
                true, false, 100, 30, 20, 45);
        FormationRespawnPolicy respawn = new FormationRespawnPolicy(25,
                List.of("ifv", "IFV", "missing"));
        FormationSupportPolicy support = new FormationSupportPolicy(
                FormationSupportPolicy.Mode.ALLOW_LIST,
                List.of("wok_infantry:artillery", "WOK_INFANTRY:ARTILLERY", "bad id"));
        FormationCapabilityProfile capabilities = new FormationCapabilityProfile(
                outpost, rally, respawn, support);
        FormationVehicleDefinition ifv = new FormationVehicleDefinition("ifv", "IFV",
                "superbwarfare:ifv", 0, 0, 0, 0, 600);
        FormationDefinition formation = new FormationDefinition("mechanized_test",
                "机械化测试编制", "", FormationCategory.MECHANIZED, true, 8,
                capabilities, List.of(new FormationClassRule("assault", 8, Map.of())),
                List.of(new FormationSquadDefinition("alpha", "Alpha", 8,
                        Map.of("assault", 8))), List.of(ifv));
        FormationConfigData data = new FormationConfigData(1, List.of(new FactionDefinition(
                "academy", "学院军", "", Faction.BLUE, true, 8,
                List.of(formation))));

        data.normalize();

        FormationDefinition normalized = data.findFormation("academy", "mechanized_test")
                .orElseThrow();
        assertEquals(2, data.version());
        assertEquals(FormationCategory.MECHANIZED, normalized.category());
        assertTrue(normalized.capabilities().outpost().squadLeaderCanPlace());
        assertTrue(normalized.capabilities().outpost().commanderCanPlace());
        assertEquals(1, normalized.capabilities().rally().maxActive());
        assertEquals(List.of("ifv"),
                normalized.capabilities().respawn().mobileSpawnVehicleIds());
        assertEquals(List.of("wok_infantry:artillery"),
                normalized.capabilities().support().allowList());
        assertTrue(normalized.capabilities().support().allows("WOK_INFANTRY:ARTILLERY"));
        assertEquals(600, normalized.findVehicle("ifv").orElseThrow()
                .replenishmentCooldownSeconds());
        assertTrue(data.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.path().contains("mobileSpawnVehicleIds")));
        assertTrue(data.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.path().contains("support.allowList")));

        FormationConfigData decoded = GSON.fromJson(GSON.toJson(data),
                FormationConfigData.class);
        decoded.normalize();
        assertEquals(FormationCategory.MECHANIZED,
                decoded.findFormation("academy", "mechanized_test").orElseThrow().category());
    }

    @Test
    void normalizationMatchesBattleCapacityAndCallsignContractWithDiagnostics() {
        FormationClassRule assault = new FormationClassRule("assault", Integer.MAX_VALUE,
                Map.of());
        FormationSquadDefinition alpha = new FormationSquadDefinition(" ALPHA ", "Alpha",
                Integer.MAX_VALUE, Map.of("assault", Integer.MAX_VALUE));
        FormationSquadDefinition unsupported = new FormationSquadDefinition("foxtrot", "Foxtrot",
                Integer.MAX_VALUE, Map.of("assault", Integer.MAX_VALUE));
        FormationSquadDefinition echo = new FormationSquadDefinition("ECHO", "Echo",
                Integer.MAX_VALUE, Map.of("assault", Integer.MAX_VALUE));
        FormationDefinition formation = new FormationDefinition("default", "Default", "",
                true, Integer.MAX_VALUE, List.of(assault),
                List.of(alpha, unsupported, echo), List.of());
        FormationConfigData data = new FormationConfigData(List.of(new FactionDefinition(
                "academy", "Academy", "", Faction.BLUE, true, Integer.MAX_VALUE,
                List.of(formation))));

        data.normalize();

        FactionDefinition normalizedFaction = data.findFaction("academy").orElseThrow();
        FormationDefinition normalizedFormation = normalizedFaction.findFormation("default")
                .orElseThrow();
        assertEquals(BattleRules.FACTION_CAPACITY, FormationValidation.MAX_PLAYERS);
        assertEquals(BattleRules.SQUAD_CAPACITY, FormationValidation.MAX_SQUAD_CAPACITY);
        assertEquals(BattleRules.FACTION_CAPACITY, normalizedFaction.maxPlayers());
        assertEquals(BattleRules.FACTION_CAPACITY, normalizedFormation.capacity());
        assertEquals(BattleRules.SQUAD_CAPACITY,
                normalizedFormation.findClass("assault").orElseThrow().squadLimit());
        assertEquals(List.of("alpha", "echo"), normalizedFormation.squads().stream()
                .map(FormationSquadDefinition::callsign).toList());
        assertEquals(BattleRules.SQUAD_CAPACITY,
                normalizedFormation.findSquad("alpha").orElseThrow().capacity());
        assertEquals(BattleRules.SQUAD_CAPACITY,
                normalizedFormation.findSquad("echo").orElseThrow().classLimit("assault", 0));
        assertTrue(normalizedFormation.findSquad("foxtrot").isEmpty());

        assertDiagnostic(data, FormationConfigDiagnostic.Kind.NORMALIZED,
                "factions[0].maxPlayers", "value clamped to " + BattleRules.FACTION_CAPACITY);
        assertDiagnostic(data, FormationConfigDiagnostic.Kind.NORMALIZED,
                "factions[0].formations[0].capacity",
                "value clamped to " + BattleRules.FACTION_CAPACITY);
        assertDiagnostic(data, FormationConfigDiagnostic.Kind.NORMALIZED,
                "factions[0].formations[0].classes[0].squadLimit",
                "value clamped to " + BattleRules.SQUAD_CAPACITY);
        assertDiagnostic(data, FormationConfigDiagnostic.Kind.NORMALIZED,
                "factions[0].formations[0].squads[0].callsign",
                "callsign normalized to alpha");
        assertDiagnostic(data, FormationConfigDiagnostic.Kind.NORMALIZED,
                "factions[0].formations[0].squads[0].capacity",
                "value clamped to " + BattleRules.SQUAD_CAPACITY);
        assertDiagnostic(data, FormationConfigDiagnostic.Kind.NORMALIZED,
                "factions[0].formations[0].squads[0].classLimits.assault",
                "value clamped to " + BattleRules.SQUAD_CAPACITY);
        assertDiagnostic(data, FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                "factions[0].formations[0].squads[1]",
                "unsupported callsign");
    }

    @Test
    void publicViewsAreImmutableAndCopiesDoNotShareNestedCollections() {
        FormationConfigData original = FormationConfigData.defaultConfig();
        FormationConfigData copy = original.copy();
        FormationDefinition formation = original.findFormation("academy", "default")
                .orElseThrow();
        FormationClassRule assault = formation.findClass("assault").orElseThrow();
        FormationSquadDefinition alpha = formation.findSquad("alpha").orElseThrow();

        assertNotSame(original, copy);
        assertNotSame(original.factions().get(0), copy.factions().get(0));
        assertNotSame(formation,
                copy.findFormation("academy", "default").orElseThrow());
        assertThrows(UnsupportedOperationException.class,
                () -> original.factions().add(original.factions().get(0)));
        assertThrows(UnsupportedOperationException.class,
                () -> formation.classes().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> assault.allowedEntries().put("primary", List.of("entry")));
        assertThrows(UnsupportedOperationException.class,
                () -> alpha.classLimits().put("assault", 1));
        assertThrows(UnsupportedOperationException.class,
                () -> original.diagnostics().clear());
    }

    private static void assertDiagnostic(FormationConfigData data,
                                         FormationConfigDiagnostic.Kind kind,
                                         String path, String messageFragment) {
        assertTrue(data.diagnostics().stream().anyMatch(diagnostic ->
                        diagnostic.kind() == kind
                                && diagnostic.path().equals(path)
                                && diagnostic.message().contains(messageFragment)),
                () -> "missing diagnostic " + kind + " at " + path + " containing '"
                        + messageFragment + "': " + data.diagnostics());
    }
}
