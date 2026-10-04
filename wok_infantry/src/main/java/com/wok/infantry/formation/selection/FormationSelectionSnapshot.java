package com.wok.infantry.formation.selection;

import com.wok.infantry.formation.vote.FormationVotePhase;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Server-authored public catalog used by the faction/formation vote screen.
 *
 * <p>The vote fields ({@code votePhase} … {@code voteTally}) describe the viewer's own faction
 * and are empty while the viewer has not joined; every faction's phase and locked formation are
 * on {@link FactionSelectionView}. {@code supportLabels} names every support ID that a
 * formation detail mentions (formation protocol 5).
 */
public record FormationSelectionSnapshot(long generation,
                                         boolean selectionRequired,
                                         String selectedFactionId,
                                         String selectedFormationId,
                                         FormationVotePhase votePhase,
                                         boolean voteChangeAllowed,
                                         String ownVoteFormationId,
                                         String lockedFormationId,
                                         Map<String, Integer> voteTally,
                                         List<FactionSelectionView> factions,
                                         List<FormationSupportLabel> supportLabels) {
    public FormationSelectionSnapshot(long generation, boolean selectionRequired,
                                      String selectedFactionId, String selectedFormationId,
                                      List<FactionSelectionView> factions) {
        this(generation, selectionRequired, selectedFactionId, selectedFormationId,
                FormationVotePhase.NOT_STARTED, false, "", "", Map.of(), factions);
    }

    /** Protocol-4 shape without support labels. */
    public FormationSelectionSnapshot(long generation, boolean selectionRequired,
                                      String selectedFactionId, String selectedFormationId,
                                      FormationVotePhase votePhase, boolean voteChangeAllowed,
                                      String ownVoteFormationId, String lockedFormationId,
                                      Map<String, Integer> voteTally,
                                      List<FactionSelectionView> factions) {
        this(generation, selectionRequired, selectedFactionId, selectedFormationId, votePhase,
                voteChangeAllowed, ownVoteFormationId, lockedFormationId, voteTally, factions,
                List.of());
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
        supportLabels = List.copyOf(supportLabels == null ? List.of() : supportLabels);
    }

    /** The faction with {@code factionId}, or {@code null}. */
    public FactionSelectionView faction(String factionId) {
        if (factionId == null || factionId.isEmpty()) {
            return null;
        }
        for (FactionSelectionView faction : factions) {
            if (faction.id().equals(factionId)) {
                return faction;
            }
        }
        return null;
    }

    /** The label of support {@code supportId}, or {@code null}. */
    public FormationSupportLabel supportLabel(String supportId) {
        for (FormationSupportLabel label : supportLabels) {
            if (label.id().equals(supportId)) {
                return label;
            }
        }
        return null;
    }
}
