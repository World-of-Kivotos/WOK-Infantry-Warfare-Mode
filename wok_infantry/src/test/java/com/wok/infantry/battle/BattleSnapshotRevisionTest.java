package com.wok.infantry.battle;

import com.wok.infantry.client.ClientBattleState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleSnapshotRevisionTest {
    private static final UUID LEADER = new UUID(0L, 1L);
    private static final UUID RIFLEMAN = new UUID(0L, 2L);
    private static final PermissionView PERMISSIONS = new PermissionView(
            false, false, true, true, true, false, true);
    private static final List<ClassQuotaView> QUOTAS = List.of(
            new ClassQuotaView("assault", "突击兵", 8, 1));

    @Test
    void healthAndHealthRatioNeverChangeTheRevision() {
        long baseline = revision(squad(
                member(LEADER, true, MemberState.DEPLOYED, 20.0F, 1.0F),
                member(RIFLEMAN, false, MemberState.DEPLOYED, 20.0F, 1.0F)));
        long wounded = revision(squad(
                member(LEADER, true, MemberState.DEPLOYED, 3.5F, 0.175F),
                member(RIFLEMAN, false, MemberState.DEPLOYED, 11.0F, 0.55F)));
        long unknownRatio = revision(squad(
                member(LEADER, true, MemberState.DEPLOYED, 20.0F,
                        MemberView.UNKNOWN_HEALTH_RATIO),
                member(RIFLEMAN, false, MemberState.DEPLOYED, 20.0F,
                        MemberView.UNKNOWN_HEALTH_RATIO)));

        assertEquals(baseline, wounded, "taking damage must not rebuild client screens");
        assertEquals(baseline, unknownRatio);
        assertTrue(baseline >= 0L, "revision travels as a non-negative long");
    }

    @Test
    void stateAndRosterStructureStillChangeTheRevision() {
        long baseline = revision(squad(
                member(LEADER, true, MemberState.DEPLOYED, 20.0F, 1.0F),
                member(RIFLEMAN, false, MemberState.DEPLOYED, 20.0F, 1.0F)));

        assertNotEquals(baseline, revision(squad(
                member(LEADER, true, MemberState.DEPLOYED, 20.0F, 1.0F),
                member(RIFLEMAN, false, MemberState.DOWNED, 1.0F, 0.05F))),
                "going down is a roster change");
        assertNotEquals(baseline, revision(squad(
                member(LEADER, true, MemberState.DEPLOYED, 20.0F, 1.0F),
                new MemberView(RIFLEMAN, "Rifleman", false, false, 0.0F, 20.0F, false,
                        false, SquadCallsign.ALPHA, "assault", MemberState.OFFLINE,
                        MemberView.UNKNOWN_HEALTH_RATIO))),
                "disconnecting is a roster change");
        assertNotEquals(baseline, revision(squad(
                member(LEADER, true, MemberState.DEPLOYED, 20.0F, 1.0F),
                new MemberView(RIFLEMAN, "Rifleman", true, true, 20.0F, 20.0F, false,
                        false, SquadCallsign.ALPHA, "medic", MemberState.DEPLOYED, 1.0F))),
                "a class change is a roster change");
    }

    @Test
    void rosterProjectionDropsOnlyHealthFields() {
        MemberView wounded = member(RIFLEMAN, false, MemberState.DOWNED, 1.0F, 0.05F);
        List<BattleSnapshotRevision.SquadShape> shapes =
                BattleSnapshotRevision.rosterStructure(List.of(squad(wounded)));

        assertEquals(1, shapes.size());
        BattleSnapshotRevision.MemberShape shape = shapes.get(0).members().get(0);
        assertEquals(new BattleSnapshotRevision.MemberShape(RIFLEMAN, "Rifleman", true, true,
                MemberState.DOWNED, false, false, SquadCallsign.ALPHA, "assault"), shape);
        assertEquals(LEADER, shapes.get(0).leaderId());
        assertEquals(BattleRules.SQUAD_CAPACITY, shapes.get(0).capacity());
        assertEquals(List.of(), BattleSnapshotRevision.rosterStructure(List.of()));
        assertEquals(List.of(), BattleSnapshotRevision.rosterStructure(null));
    }

    @Test
    void healthHeartbeatRefreshesClientRatioWithoutRebuildingScreens() {
        SquadView healthy = squad(
                member(LEADER, true, MemberState.DEPLOYED, 20.0F, 1.0F),
                member(RIFLEMAN, false, MemberState.DEPLOYED, 20.0F, 1.0F));
        SquadView wounded = squad(
                member(LEADER, true, MemberState.DEPLOYED, 20.0F, 1.0F),
                member(RIFLEMAN, false, MemberState.DEPLOYED, 6.0F, 0.3F));
        SquadView downed = squad(
                member(LEADER, true, MemberState.DEPLOYED, 20.0F, 1.0F),
                member(RIFLEMAN, false, MemberState.DOWNED, 1.0F, 0.05F));
        try {
            ClientBattleState.update(clientSnapshot(healthy, 1_000L));
            long generation = ClientBattleState.generation();

            ClientBattleState.update(clientSnapshot(wounded, 2_000L));
            assertEquals(generation, ClientBattleState.generation(),
                    "a health-only heartbeat must not rebuild squad or map screens");
            assertEquals(0.3F, ClientBattleState.member(RIFLEMAN).healthRatio(),
                    "the newest ratio is still readable at render time");

            ClientBattleState.update(clientSnapshot(downed, 3_000L));
            assertEquals(generation + 1L, ClientBattleState.generation(),
                    "a state change rebuilds exactly once");
            assertEquals(MemberState.DOWNED, ClientBattleState.member(RIFLEMAN).state());
        } finally {
            ClientBattleState.clear();
        }
    }

    private static BattleSnapshot clientSnapshot(SquadView squad, long serverTimeMillis) {
        return new BattleSnapshot(LEADER, Faction.BLUE, SquadCallsign.ALPHA, true, false, 2, 3,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY, List.of(squad),
                List.of(), List.of(), PERMISSIONS, QUOTAS, serverTimeMillis, revision(squad));
    }

    private static long revision(SquadView squad) {
        return BattleSnapshotRevision.visible(Faction.BLUE, "default", SquadCallsign.ALPHA,
                true, false, 2, 3, List.of(squad), List.of(), PERMISSIONS, QUOTAS, 7L);
    }

    private static SquadView squad(MemberView... members) {
        return new SquadView(SquadCallsign.ALPHA, LEADER, List.of(members),
                BattleRules.SQUAD_CAPACITY);
    }

    private static MemberView member(UUID id, boolean leader, MemberState state,
                                     float health, float ratio) {
        boolean online = state != MemberState.OFFLINE;
        boolean alive = state.hasVitals();
        return new MemberView(id, leader ? "Leader" : "Rifleman", online, alive, health, 20.0F,
                leader, false, SquadCallsign.ALPHA, "assault", state, ratio);
    }
}
