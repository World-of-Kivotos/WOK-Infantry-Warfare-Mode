package com.wok.infantry.formation.vote;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.formation.vote.FormationVotePolicy.AssignPlan;
import com.wok.infantry.formation.vote.FormationVotePolicy.VoteBlock;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationVotePolicyTest {
    @Test
    void membersOfAFullFactionMayStillVote() {
        // 40/40: full for new players, but its own members still vote (vote-08).
        assertEquals(VoteBlock.NONE, FormationVotePolicy.voteBlock(true,
                FormationVotePhase.OPEN, true, 40, 40, "", "infantry", true));
        assertFalse(FormationVotePolicy.canJoin(40, 40));
    }

    @Test
    void formationSmallerThanTheFactionIsRefusedBeforeTheLock() {
        assertTrue(FormationVotePolicy.capacityShortfall(12, 18));
        assertFalse(FormationVotePolicy.capacityShortfall(18, 18));
        assertEquals(VoteBlock.CAPACITY_SHORTFALL, FormationVotePolicy.voteBlock(true,
                FormationVotePhase.OPEN, true, 12, 18, "", "cavalry", true));
    }

    @Test
    void voteBlocksAreCheckedInOrder() {
        assertEquals(VoteBlock.NOT_MEMBER, FormationVotePolicy.voteBlock(false,
                FormationVotePhase.NOT_STARTED, false, 1, 99, "x", "y", false));
        assertEquals(VoteBlock.NOT_OPEN, FormationVotePolicy.voteBlock(true,
                FormationVotePhase.LOCKED, true, 40, 1, "", "infantry", true));
        assertEquals(VoteBlock.NOT_CANDIDATE, FormationVotePolicy.voteBlock(true,
                FormationVotePhase.OPEN, false, 40, 1, "", "infantry", true));
        assertEquals(VoteBlock.CHANGE_NOT_ALLOWED, FormationVotePolicy.voteBlock(true,
                FormationVotePhase.OPEN, true, 40, 1, "armored", "infantry", false));
        assertEquals(VoteBlock.NONE, FormationVotePolicy.voteBlock(true,
                FormationVotePhase.OPEN, true, 40, 1, "infantry", "infantry", false),
                "repeating the own vote is not a change");
    }

    /**
     * B11a: the server checks votes with {@link FormationVotePolicy#voteBlock} before the ledger
     * records them, so the two must agree on every case the ledger also decides (capacity is
     * the policy's own check).
     */
    @Test
    void voteBlockAgreesWithTheBallotLedger() {
        UUID voter = UUID.nameUUIDFromBytes("voter".getBytes(StandardCharsets.UTF_8));
        FormationVoteLedger ledger = new FormationVoteLedger();
        assertAgrees(ledger, voter, "infantry", false, "not opened yet");

        ledger.open(Faction.BLUE, List.of("infantry", "armored"), false);
        assertAgrees(ledger, voter, "special", false, "not a candidate");
        assertAgrees(ledger, voter, "infantry", true, "first vote");
        assertAgrees(ledger, voter, "infantry", true, "repeating the own vote");
        assertAgrees(ledger, voter, "armored", false, "change not allowed");

        ledger.lock(Faction.BLUE, "infantry");
        assertAgrees(ledger, voter, "infantry", false, "locked");

        FormationVoteLedger changeable = new FormationVoteLedger();
        changeable.open(Faction.BLUE, List.of("infantry", "armored"), true);
        assertAgrees(changeable, voter, "infantry", true, "first vote");
        assertAgrees(changeable, voter, "armored", true, "change allowed");
    }

    private static void assertAgrees(FormationVoteLedger ledger, UUID voter, String target,
                                     boolean expectedAccepted, String what) {
        FormationVoteSnapshot vote = ledger.snapshot(Faction.BLUE, voter);
        VoteBlock block = FormationVotePolicy.voteBlock(true, vote.phase(),
                vote.candidates().contains(target), 40, 1, vote.ownVote(), target,
                vote.voteChangeAllowed());
        boolean accepted = ledger.cast(Faction.BLUE, voter, target).success();
        assertEquals(expectedAccepted, accepted, what + ": ledger");
        assertEquals(expectedAccepted, block == VoteBlock.NONE, what + ": policy " + block);
    }

    @Test
    void joinCapacityIsBoundedByTheLockedFormation() {
        assertEquals(40, FormationVotePolicy.joinCapacity(40, FormationVotePhase.OPEN, 12));
        assertEquals(12, FormationVotePolicy.joinCapacity(40, FormationVotePhase.LOCKED, 12));
        assertEquals(40, FormationVotePolicy.joinCapacity(40, FormationVotePhase.LOCKED, 64));
    }

    @Test
    void administratorAssignmentFollowsTheLock() {
        assertEquals(AssignPlan.FACTION_ONLY, FormationVotePolicy.adminAssign(
                FormationVotePhase.NOT_STARTED, "", "armored"), "vote-02: only the faction");
        assertEquals(AssignPlan.FACTION_ONLY, FormationVotePolicy.adminAssign(
                FormationVotePhase.OPEN, "", null), "vote-03: no hard-coded default");
        assertEquals(AssignPlan.ASSIGN_LOCKED, FormationVotePolicy.adminAssign(
                FormationVotePhase.LOCKED, "armored", null));
        assertEquals(AssignPlan.ASSIGN_LOCKED, FormationVotePolicy.adminAssign(
                FormationVotePhase.LOCKED, "armored", "armored"));
        assertEquals(AssignPlan.REJECT_NOT_LOCKED_FORMATION, FormationVotePolicy.adminAssign(
                FormationVotePhase.LOCKED, "armored", "infantry"));
    }

    @Test
    void onlyFactionMembersWithoutFormationInheritAnAvailableLock() {
        assertTrue(FormationVotePolicy.inheritsLockedFormation(true, false,
                FormationVotePhase.LOCKED, true), "vote-01: login after the lock");
        assertFalse(FormationVotePolicy.inheritsLockedFormation(true, true,
                FormationVotePhase.LOCKED, true));
        assertFalse(FormationVotePolicy.inheritsLockedFormation(false, false,
                FormationVotePhase.LOCKED, true));
        assertFalse(FormationVotePolicy.inheritsLockedFormation(true, false,
                FormationVotePhase.OPEN, true));
        assertFalse(FormationVotePolicy.inheritsLockedFormation(true, false,
                FormationVotePhase.LOCKED, false));
    }
}
