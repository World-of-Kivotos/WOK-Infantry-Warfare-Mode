package com.wok.infantry.server;

import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.FormationClassRule;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationSquadDefinition;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationServiceRosterReconciliationTest {
    @Test
    void rosterRuleUsesCatalogClassOrderAndFailsClosedForMissingSquad() {
        List<FormationClassRule> classes = List.of(
                new FormationClassRule("support", 2, Map.of()),
                new FormationClassRule("engineer", 6, Map.of()),
                new FormationClassRule("assault", 6, Map.of()));
        LinkedHashMap<String, Integer> overrides = new LinkedHashMap<>();
        overrides.put("support", 3);
        overrides.put("engineer", 1);
        FormationSquadDefinition alpha = new FormationSquadDefinition(
                "alpha", "Alpha", 3, overrides);
        FormationDefinition formation = new FormationDefinition(
                "line", "Line", "", true, 12, classes, List.of(alpha), List.of());
        FormationConfigData active = new FormationConfigData(List.of(
                new FactionDefinition("academy", "Academy", "", Faction.BLUE,
                        true, 16, List.of(formation))));
        active.normalize();

        BattleService.FormationRosterRule rule = FormationService.rosterRuleFor(
                active, Faction.BLUE, "line", SquadCallsign.ALPHA).orElseThrow();

        assertEquals(3, rule.capacity());
        assertEquals(List.of("support", "engineer", "assault"),
                List.copyOf(rule.classLimits().keySet()));
        assertEquals(Map.of("support", 3, "engineer", 1, "assault", 3),
                rule.classLimits());
        assertThrows(UnsupportedOperationException.class,
                () -> rule.classLimits().put("medic", 1));
        assertTrue(FormationService.rosterRuleFor(active, Faction.BLUE, "line",
                SquadCallsign.BRAVO).isEmpty());
        assertTrue(FormationService.rosterRuleFor(active, Faction.BLUE, "missing",
                SquadCallsign.ALPHA).isEmpty());
        assertTrue(FormationService.rosterRuleFor(active, Faction.RED, "line",
                SquadCallsign.ALPHA).isEmpty());
    }

    @Test
    void affectedPlayersAreDeduplicatedBeforeDeploymentNotification() {
        UUID selectionOnly = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID shared = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID rosterOnly = UUID.fromString("00000000-0000-0000-0000-000000000003");

        List<UUID> affected = FormationService.mergeAffectedPlayers(
                List.of(selectionOnly, shared), List.of(shared, rosterOnly, selectionOnly));

        assertEquals(List.of(selectionOnly, shared, rosterOnly), affected);
        assertThrows(UnsupportedOperationException.class, () -> affected.add(UUID.randomUUID()));
    }
}
