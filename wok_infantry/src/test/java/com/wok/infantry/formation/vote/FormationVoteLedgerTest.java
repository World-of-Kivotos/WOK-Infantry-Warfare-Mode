package com.wok.infantry.formation.vote;

import com.wok.infantry.battle.Faction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationVoteLedgerTest {
    @Test
    void factionsHaveIndependentSharedBallotsAndExplicitlyLockedResults() {
        FormationVoteLedger ledger = new FormationVoteLedger();
        UUID blueOne = UUID.randomUUID();
        UUID blueTwo = UUID.randomUUID();
        UUID redOne = UUID.randomUUID();

        assertTrue(ledger.open(Faction.BLUE,
                List.of("light_infantry", "armored"), false).success());
        assertTrue(ledger.open(Faction.RED,
                List.of("motorized", "mechanized"), false).success());
        assertTrue(ledger.cast(Faction.BLUE, blueOne, "light_infantry").success());
        assertTrue(ledger.cast(Faction.BLUE, blueTwo, "armored").success());
        assertTrue(ledger.cast(Faction.RED, redOne, "mechanized").success());

        FormationVoteSnapshot blue = ledger.snapshot(Faction.BLUE, blueOne);
        FormationVoteSnapshot red = ledger.snapshot(Faction.RED, redOne);
        assertEquals(1, blue.tally().get("light_infantry"));
        assertEquals(1, blue.tally().get("armored"));
        assertEquals("light_infantry", blue.ownVote());
        assertEquals(1, red.tally().get("mechanized"));
        assertEquals("mechanized", red.ownVote());

        assertTrue(ledger.lock(Faction.BLUE, "armored").success());
        assertEquals(FormationVotePhase.LOCKED,
                ledger.snapshot(Faction.BLUE, null).phase());
        assertEquals("armored", ledger.snapshot(Faction.BLUE, null).lockedFormationId());
        assertEquals(FormationVotePhase.OPEN, ledger.snapshot(Faction.RED, null).phase());
    }

    @Test
    void callerControlsWhetherVotesCanChangeAndNoWinnerIsInvented() {
        FormationVoteLedger ledger = new FormationVoteLedger();
        UUID voter = UUID.randomUUID();
        ledger.open(Faction.BLUE, List.of("infantry", "armored"), false);

        assertTrue(ledger.cast(Faction.BLUE, voter, "infantry").success());
        FormationVoteResult rejected = ledger.cast(Faction.BLUE, voter, "armored");
        assertFalse(rejected.success());
        assertEquals(FormationVoteResult.Code.VOTE_ALREADY_CAST, rejected.code());
        ledger.open(Faction.BLUE, List.of("infantry", "armored"), true);
        assertTrue(ledger.cast(Faction.BLUE, voter, "infantry").success());
        assertTrue(ledger.cast(Faction.BLUE, voter, "armored").success());
        assertEquals("armored", ledger.snapshot(Faction.BLUE, voter).ownVote());
        assertTrue(ledger.snapshot(Faction.BLUE, voter).voteChangeAllowed());
        assertEquals("", ledger.snapshot(Faction.BLUE, voter).lockedFormationId());
    }

    @Test
    void malformedCandidatesAndVotesFailClosed() {
        FormationVoteLedger ledger = new FormationVoteLedger();
        assertFalse(ledger.open(Faction.BLUE, List.of("BAD ID", ""), false).success());
        assertTrue(ledger.open(Faction.BLUE,
                List.of(" Infantry ", "infantry", "armored"), false).success());
        assertEquals(List.of("infantry", "armored"),
                ledger.snapshot(Faction.BLUE, null).candidates());
        assertFalse(ledger.cast(Faction.BLUE, UUID.randomUUID(), "unknown").success());
        assertFalse(ledger.lock(Faction.BLUE, "unknown").success());
    }

    @Test
    void catalogReconciliationPreservesValidVotesAndClearsInvalidLockedResult() {
        FormationVoteLedger ledger = new FormationVoteLedger();
        UUID retained = UUID.randomUUID();
        UUID removed = UUID.randomUUID();
        ledger.open(Faction.BLUE, List.of("infantry", "armored", "special"), true);
        ledger.cast(Faction.BLUE, retained, "infantry");
        ledger.cast(Faction.BLUE, removed, "special");

        assertTrue(ledger.reconcileCandidates(Faction.BLUE,
                List.of("infantry", "armored")));
        FormationVoteSnapshot open = ledger.snapshot(Faction.BLUE, retained);
        assertEquals(List.of("infantry", "armored"), open.candidates());
        assertEquals("infantry", open.ownVote());
        assertEquals(1, open.tally().get("infantry"));

        ledger.lock(Faction.BLUE, "armored");
        assertTrue(ledger.reconcileCandidates(Faction.BLUE, List.of("infantry")));
        assertEquals(FormationVotePhase.NOT_STARTED,
                ledger.snapshot(Faction.BLUE, retained).phase());
    }
}
