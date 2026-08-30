package com.wok.infantry.formation.selection;

import com.wok.infantry.formation.vote.FormationVotePhase;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Server-authored public catalog used by the mandatory faction/formation selection screen. */
public record FormationSelectionSnapshot(long generation,
                                         boolean selectionRequired,
                                         String selectedFactionId,
                                         String selectedFormationId,
                                         FormationVotePhase votePhase,
                                         boolean voteChangeAllowed,
                                         String ownVoteFormationId,
                                         String lockedFormationId,
                                         Map<String, Integer> voteTally,
                                         List<FactionSelectionView> factions) {
    public FormationSelectionSnapshot(long generation, boolean selectionRequired,
                                      String selectedFactionId, String selectedFormationId,
                                      List<FactionSelectionView> factions) {
        this(generation, selectionRequired, selectedFactionId, selectedFormationId,
                FormationVotePhase.NOT_STARTED, false, "", "", Map.of(), factions);
    }

    public FormationSelectionSnapshot {
        generation = Math.max(0L, generation);
        selectedFactionId = Objects.requireNonNullElse(selectedFactionId, "");
        selectedFormationId = Objects.requireNonNullElse(selectedFormationId, "");
        votePhase = Objects.requireNonNullElse(votePhase, FormationVotePhase.NOT_STARTED);
        ownVoteFormationId = Objects.requireNonNullElse(ownVoteFormationId, "");
        lockedFormationId = Objects.requireNonNullElse(lockedFormationId, "");
        voteTally = Map.copyOf(voteTally == null ? Map.of() : voteTally);
        factions = List.copyOf(factions == null ? List.of() : factions);
    }
}
