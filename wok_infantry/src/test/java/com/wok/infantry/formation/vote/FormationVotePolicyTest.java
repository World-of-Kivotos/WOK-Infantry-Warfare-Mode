package com.wok.infantry.formation.vote;

import com.wok.infantry.formation.vote.FormationVotePolicy.AssignPlan;
import com.wok.infantry.formation.vote.FormationVotePolicy.VoteBlock;
import org.junit.jupiter.api.Test;

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
