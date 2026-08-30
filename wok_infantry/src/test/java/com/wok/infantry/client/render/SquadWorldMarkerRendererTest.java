package com.wok.infantry.client.render;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.PermissionView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SquadWorldMarkerRendererTest {
    private static final UUID VIEWER = new UUID(0L, 1L);
    private static final UUID TARGET = new UUID(0L, 2L);

    @Test
    void eligibleLivingSquadMateRendersThroughTheNinetySixBlockBoundary() {
        MemberView member = member(TARGET, SquadCallsign.ALPHA, true, true);
        assertTrue(visible(snapshot(VIEWER, SquadCallsign.ALPHA, member), TARGET,
                96.0D * 96.0D, true, false));
    }

    @Test
    void selfAndOtherAlliedSquadsNeverReceiveTheBadge() {
        BattleSnapshot selfSnapshot = snapshot(VIEWER, SquadCallsign.ALPHA,
                member(TARGET, SquadCallsign.ALPHA, true, true));
        assertFalse(visible(selfSnapshot, VIEWER, 1.0D, true, false));
        MemberView otherSquad = member(TARGET, SquadCallsign.BRAVO, true, true);
        assertFalse(visible(snapshot(VIEWER, SquadCallsign.ALPHA, otherSquad), TARGET,
                1.0D, true, false));
        assertFalse(visible(snapshot(VIEWER, null, null), TARGET,
                1.0D, true, false));
    }

    @Test
    void staleOfflineDeadOrMismatchedRosterRecordsNeverRender() {
        assertFalse(visible(snapshot(VIEWER, SquadCallsign.ALPHA, null), TARGET,
                1.0D, true, false));
        assertFalse(visible(snapshot(VIEWER, SquadCallsign.ALPHA,
                member(new UUID(0L, 99L), SquadCallsign.ALPHA, true, true)), TARGET,
                1.0D, true, false));
        assertFalse(visible(snapshot(VIEWER, SquadCallsign.ALPHA,
                member(TARGET, SquadCallsign.ALPHA, false, true)), TARGET,
                1.0D, true, false));
        assertFalse(visible(snapshot(VIEWER, SquadCallsign.ALPHA,
                member(TARGET, SquadCallsign.ALPHA, true, false)), TARGET,
                1.0D, true, false));
        assertFalse(visible(snapshot(VIEWER, SquadCallsign.ALPHA,
                member(TARGET, SquadCallsign.ALPHA, true, true)), TARGET,
                1.0D, false, false));
    }

    @Test
    void rangeAndVanillaInvisibilityRemainHardVisibilityGates() {
        MemberView member = member(TARGET, SquadCallsign.ALPHA, true, true);
        BattleSnapshot snapshot = snapshot(VIEWER, SquadCallsign.ALPHA, member);
        assertFalse(visible(snapshot, TARGET,
                Math.nextUp(96.0D * 96.0D), true, false));
        assertFalse(visible(snapshot, TARGET, -1.0D, true, false));
        assertFalse(visible(snapshot, TARGET, Double.NaN, true, false));
        assertFalse(visible(snapshot, TARGET, Double.POSITIVE_INFINITY, true, false));
        assertFalse(visible(snapshot, TARGET, 1.0D, true, true));
    }

    @Test
    void snapshotForAnotherViewerCannotLeakAStaleBadge() {
        UUID previousViewer = new UUID(0L, 77L);
        BattleSnapshot stale = snapshot(previousViewer, SquadCallsign.ALPHA,
                member(TARGET, SquadCallsign.ALPHA, true, true));
        assertFalse(visible(stale, TARGET, 1.0D, true, false));
    }

    private static boolean visible(BattleSnapshot snapshot, UUID targetId,
                                   double distanceSquared, boolean entityAlive,
                                   boolean invisible) {
        return SquadWorldMarkerRenderer.eligibleMember(snapshot, VIEWER, targetId,
                distanceSquared, entityAlive, invisible) != null;
    }

    private static MemberView member(UUID id, SquadCallsign squad,
                                     boolean online, boolean alive) {
        return new MemberView(id, "member", online, alive, 20.0F, 20.0F,
                false, false, squad, "assault");
    }

    private static BattleSnapshot snapshot(UUID viewerId, SquadCallsign ownSquad,
                                           MemberView candidate) {
        List<SquadView> squads = new ArrayList<>();
        if (ownSquad != null) {
            MemberView viewer = new MemberView(viewerId, "viewer", true, true,
                    20.0F, 20.0F, true, false, ownSquad, "assault");
            List<MemberView> ownMembers = new ArrayList<>();
            ownMembers.add(viewer);
            if (candidate != null && candidate.squad() == ownSquad) {
                ownMembers.add(candidate);
            }
            squads.add(new SquadView(ownSquad, viewerId, ownMembers,
                    BattleRules.SQUAD_CAPACITY));
            if (candidate != null && candidate.squad() != null
                    && candidate.squad() != ownSquad) {
                squads.add(new SquadView(candidate.squad(), candidate.playerId(),
                        List.of(candidate), BattleRules.SQUAD_CAPACITY));
            }
        }
        PermissionView permissions = new PermissionView(false, false, false,
                false, false, false, false);
        return new BattleSnapshot(viewerId, Faction.BLUE, ownSquad,
                ownSquad != null, false, squads.stream()
                .mapToInt(squad -> squad.members().size()).sum(), 0,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY,
                squads, List.of(), List.of(), permissions, List.of(), 1_000L, 1L);
    }
}
