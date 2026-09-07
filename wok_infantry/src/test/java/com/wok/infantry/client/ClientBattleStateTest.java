package com.wok.infantry.client;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.PermissionView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.support.SupportOptionView;
import com.wok.infantry.support.SupportTargetMode;
import com.wok.infantry.support.SupportView;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientBattleStateTest {
    private static final PermissionView READ_ONLY = new PermissionView(
            false, false, false, false, false, false, false);

    @AfterEach
    void clearClientState() {
        ClientBattleState.clear();
    }

    @Test
    void sameSquadRecognitionNeverIncludesAnotherAlliedSquad() {
        UUID viewerId = new UUID(0L, 1L);
        UUID squadMateId = new UUID(0L, 2L);
        UUID otherSquadId = new UUID(0L, 3L);
        MemberView viewer = member(viewerId, "Viewer", SquadCallsign.ALPHA, true);
        MemberView squadMate = member(squadMateId, "Mate", SquadCallsign.ALPHA, false);
        MemberView otherSquadMember = member(otherSquadId, "Other", SquadCallsign.BRAVO, true);
        BattleSnapshot snapshot = new BattleSnapshot(viewerId, Faction.BLUE,
                SquadCallsign.ALPHA, true, false, 3, 0,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY,
                List.of(
                        new SquadView(SquadCallsign.ALPHA, viewerId,
                                List.of(viewer, squadMate), BattleRules.SQUAD_CAPACITY),
                        new SquadView(SquadCallsign.BRAVO, otherSquadId,
                                List.of(otherSquadMember), BattleRules.SQUAD_CAPACITY)),
                List.of(), List.of(), READ_ONLY, List.of(), 1_000L, 1L);

        ClientBattleState.update(snapshot);

        assertEquals(2, ClientBattleState.ownSquad().members().size());
        assertTrue(ClientBattleState.isSameSquad(viewerId));
        assertTrue(ClientBattleState.isSameSquad(squadMateId));
        assertFalse(ClientBattleState.isSameSquad(otherSquadId));
        assertFalse(ClientBattleState.isSameSquad(new UUID(0L, 99L)));
        assertFalse(ClientBattleState.isSameSquad(null));
    }

    @Test
    void clearingSnapshotRemovesAllSquadRecognition() {
        UUID viewerId = new UUID(0L, 10L);
        MemberView viewer = member(viewerId, "Viewer", SquadCallsign.CHARLIE, true);
        BattleSnapshot snapshot = new BattleSnapshot(viewerId, Faction.BLUE,
                SquadCallsign.CHARLIE, true, false, 1, 0,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY,
                List.of(new SquadView(SquadCallsign.CHARLIE, viewerId,
                        List.of(viewer), BattleRules.SQUAD_CAPACITY)),
                List.of(), List.of(), READ_ONLY, List.of(), 1_000L, 1L);

        ClientBattleState.update(snapshot);
        assertTrue(ClientBattleState.isSameSquad(viewerId));

        ClientBattleState.clear();

        assertFalse(ClientBattleState.isSameSquad(viewerId));
        assertEquals(List.of(), ClientBattleState.squads());
    }

    @Test
    void successiveSquadChangesNeverRetainOldTeammates() {
        UUID viewerId = new UUID(0L, 20L);
        UUID alphaMateId = new UUID(0L, 21L);
        UUID bravoMateId = new UUID(0L, 22L);

        ClientBattleState.update(snapshot(viewerId, SquadCallsign.ALPHA,
                alphaMateId, 10L));
        assertTrue(ClientBattleState.isSameSquad(alphaMateId));
        assertFalse(ClientBattleState.isSameSquad(bravoMateId));

        ClientBattleState.update(snapshot(viewerId, SquadCallsign.BRAVO,
                bravoMateId, 11L));
        assertFalse(ClientBattleState.isSameSquad(alphaMateId));
        assertTrue(ClientBattleState.isSameSquad(bravoMateId));

        ClientBattleState.update(new BattleSnapshot(viewerId, Faction.BLUE,
                null, false, false, 1, 0,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY,
                List.of(), List.of(), List.of(), READ_ONLY, List.of(), 1_200L, 12L));
        assertFalse(ClientBattleState.isSameSquad(alphaMateId));
        assertFalse(ClientBattleState.isSameSquad(bravoMateId));
        assertEquals(null, ClientBattleState.ownSquad());
    }

    @Test
    void supportCountdownTicksDoNotRebuildUntilStructuralRevisionChanges() {
        UUID viewerId = new UUID(0L, 30L);
        BattleSnapshot initial = snapshot(viewerId, SquadCallsign.DELTA,
                new UUID(0L, 31L), 20L).withSupport(supportView(100L, 4L, 500L));
        ClientBattleState.update(initial);
        long initialGeneration = ClientBattleState.generation();

        ClientBattleState.update(snapshot(viewerId, SquadCallsign.DELTA,
                new UUID(0L, 31L), 20L).withSupport(supportView(120L, 4L, 500L)));
        assertEquals(initialGeneration, ClientBattleState.generation(),
                "authoritative countdown updates are not structural UI changes");
        assertTrue(ClientBattleState.estimatedSupportGameTick() >= 120L);

        ClientBattleState.update(snapshot(viewerId, SquadCallsign.DELTA,
                new UUID(0L, 31L), 20L).withSupport(supportView(121L, 5L, 500L)));
        assertEquals(initialGeneration + 1L, ClientBattleState.generation(),
                "support structural revision invalidates cached widgets exactly once");
    }

    @Test
    void supportClockAdvancesAtTwentyTicksPerSecondAndSaturates() {
        assertEquals(200L, ClientBattleState.estimateSupportGameTick(
                200L, 1_000_000_000L, 1_049_999_999L));
        assertEquals(201L, ClientBattleState.estimateSupportGameTick(
                200L, 1_000_000_000L, 1_050_000_000L));
        assertEquals(200L, ClientBattleState.estimateSupportGameTick(
                200L, 1_000_000_000L, 900_000_000L));
        assertEquals(Long.MAX_VALUE, ClientBattleState.estimateSupportGameTick(
                Long.MAX_VALUE - 1L, 0L, 100_000_000L));
    }

    private static BattleSnapshot snapshot(UUID viewerId, SquadCallsign squad,
                                           UUID teammateId, long revision) {
        MemberView viewer = member(viewerId, "Viewer", squad, true);
        MemberView teammate = member(teammateId, "Mate", squad, false);
        return new BattleSnapshot(viewerId, Faction.BLUE, squad, true, false, 2, 0,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY,
                List.of(new SquadView(squad, viewerId, List.of(viewer, teammate),
                        BattleRules.SQUAD_CAPACITY)),
                List.of(), List.of(), READ_ONLY, List.of(), 1_000L, revision);
    }

    private static MemberView member(UUID playerId, String name,
                                     SquadCallsign squad, boolean leader) {
        return new MemberView(playerId, name, true, true, 20.0F, 20.0F,
                leader, false, squad, "assault");
    }

    private static SupportView supportView(long serverTick, long structuralRevision,
                                           long readyAtTick) {
        return new SupportView(List.of(new SupportOptionView(
                ResourceLocation.fromNamespaceAndPath("wok_infantry", "test_point"),
                "support.wok_infantry.test_point", "测试点支援", "点支援",
                SupportTargetMode.POINT, 16.0D, true, "", readyAtTick, false)),
                List.of(), serverTick, structuralRevision, true, "");
    }
}
