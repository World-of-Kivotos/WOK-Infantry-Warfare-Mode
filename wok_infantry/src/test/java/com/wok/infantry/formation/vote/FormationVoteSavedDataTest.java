package com.wok.infantry.formation.vote;

import com.wok.infantry.battle.Faction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationVoteSavedDataTest {
    @Test
    void roundTripPreservesIndependentVotesTalliesAndLockedResult() {
        FormationVoteSavedData source = new FormationVoteSavedData();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        source.open(Faction.BLUE, List.of("light_infantry", "armored"), false);
        source.cast(Faction.BLUE, first, "light_infantry");
        source.cast(Faction.BLUE, second, "armored");
        source.lock(Faction.BLUE, "armored");
        source.open(Faction.RED, List.of("motorized", "mechanized"), true);
        source.cast(Faction.RED, first, "mechanized");

        FormationVoteSavedData decoded = FormationVoteSavedData.load(
                source.save(new CompoundTag()));

        FormationVoteSnapshot blue = decoded.snapshot(Faction.BLUE, first);
        assertEquals(FormationVotePhase.LOCKED, blue.phase());
        assertEquals("armored", blue.lockedFormationId());
        assertEquals("light_infantry", blue.ownVote());
        assertEquals(1, blue.tally().get("light_infantry"));
        assertEquals(1, blue.tally().get("armored"));
        FormationVoteSnapshot red = decoded.snapshot(Faction.RED, first);
        assertEquals(FormationVotePhase.OPEN, red.phase());
        assertEquals("mechanized", red.ownVote());
        assertTrue(red.voteChangeAllowed());
    }

    @Test
    void malformedOrOversizedPayloadFailsClosed() {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 1);
        ListTag ballots = new ListTag();
        CompoundTag ballot = new CompoundTag();
        ballot.putString("Faction", "blue");
        ballot.putString("Phase", "LOCKED");
        ballot.putString("LockedFormation", "not_a_candidate");
        ListTag candidates = new ListTag();
        candidates.add(StringTag.valueOf("infantry"));
        for (int index = 0; index < 100; index++) {
            candidates.add(StringTag.valueOf("extra_" + index));
        }
        ballot.put("Candidates", candidates);
        ListTag votes = new ListTag();
        CompoundTag invalidVote = new CompoundTag();
        invalidVote.putUUID("Voter", UUID.randomUUID());
        invalidVote.putString("Formation", "unknown");
        votes.add(invalidVote);
        ballot.put("Votes", votes);
        ballots.add(ballot);
        root.put("Ballots", ballots);

        FormationVoteSavedData decoded = FormationVoteSavedData.load(root);
        FormationVoteSnapshot snapshot = decoded.snapshot(Faction.BLUE, null);
        assertEquals(FormationVotePhase.OPEN, snapshot.phase());
        assertEquals(32, snapshot.candidates().size());
        assertEquals("", snapshot.lockedFormationId());
        assertTrue(snapshot.tally().values().stream().allMatch(count -> count == 0));

        CompoundTag wrongVersion = root.copy();
        wrongVersion.putInt("Version", 99);
        assertEquals(FormationVotePhase.NOT_STARTED,
                FormationVoteSavedData.load(wrongVersion)
                        .snapshot(Faction.BLUE, null).phase());
    }
}
