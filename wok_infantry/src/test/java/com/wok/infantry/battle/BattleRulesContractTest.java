package com.wok.infantry.battle;

import com.wok.infantry.network.battle.BattleNetworkLimits;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleRulesContractTest {
    @Test
    void fiveEightPlayerSquadsExactlyFillOneFortyPlayerFaction() {
        assertEquals(5, SquadCallsign.values().length);
        assertEquals(40, BattleRules.FACTION_CAPACITY);
        assertEquals(8, BattleRules.SQUAD_CAPACITY);
        assertEquals(BattleRules.FACTION_CAPACITY,
                SquadCallsign.values().length * BattleRules.SQUAD_CAPACITY);

        assertEquals(SquadCallsign.values().length, BattleNetworkLimits.MAX_SQUADS);
        assertEquals(BattleRules.SQUAD_CAPACITY,
                BattleNetworkLimits.MAX_MEMBERS_PER_SQUAD);
        assertEquals(BattleRules.FACTION_CAPACITY,
                BattleNetworkLimits.MAX_ALLIED_POSITIONS);
    }

    @Test
    void defaultClassQuotasAreBoundedPerSquadAndCannotBeMutated() {
        assertEquals(Map.of(
                "assault", 8,
                "support", 2,
                "engineer", 2,
                "recon", 1), BattleRules.DEFAULT_CLASS_LIMITS);
        assertTrue(BattleRules.DEFAULT_CLASS_LIMITS.values().stream()
                .allMatch(limit -> limit >= 1 && limit <= BattleRules.SQUAD_CAPACITY));
        assertThrows(UnsupportedOperationException.class,
                () -> BattleRules.DEFAULT_CLASS_LIMITS.put("medic", 1));

        assertEquals(BattleRules.SQUAD_CAPACITY,
                BattleRules.defaultClassLimit("unconfigured_class"));
        assertEquals(0, BattleRules.defaultClassLimit(null));
    }

    @Test
    void factionAndRequiredTacticalMarkerIdsRoundTripWithoutCrossFactionAmbiguity() {
        for (Faction faction : Faction.values()) {
            assertSame(faction, Faction.byId("  " + faction.id().toUpperCase() + "  ")
                    .orElseThrow());
            assertSame(faction, faction.opposite().opposite());
            assertFalse(faction == faction.opposite());
        }

        Set<TacticalMarkerType> requiredTypes = Set.of(
                TacticalMarkerType.INFANTRY,
                TacticalMarkerType.TANK,
                TacticalMarkerType.IFV,
                TacticalMarkerType.ATTACK_DIRECTION);
        assertTrue(Set.of(TacticalMarkerType.values()).containsAll(requiredTypes));
        for (TacticalMarkerType type : requiredTypes) {
            assertSame(type, TacticalMarkerType.byId(type.id()).orElseThrow());
        }
    }
}
