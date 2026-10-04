package com.wok.infantry.formation.selection;

import com.wok.infantry.formation.vote.FormationVotePhase;

import java.util.List;
import java.util.Objects;

/**
 * Public faction identity. The internal BLUE/RED battle side is intentionally not serialized.
 *
 * <p>{@code available} means "a new player may join now" (enabled, below capacity, has a
 * candidate formation; after the lock also below the locked formation's capacity).
 * {@code votePhase} and {@code lockedFormationId} describe this faction's shared ballot and are
 * sent to every viewer, joined or not (formation protocol 5); the tally stays on the snapshot's
 * own-faction fields and is only sent to members.
 */
public record FactionSelectionView(String id,
                                   String displayName,
                                   String description,
                                   int population,
                                   int capacity,
                                   boolean available,
                                   List<FormationSelectionView> formations,
                                   FormationVotePhase votePhase,
                                   String lockedFormationId) {
    /** Protocol-4 shape: ballot not started. */
    public FactionSelectionView(String id, String displayName, String description,
                                int population, int capacity, boolean available,
                                List<FormationSelectionView> formations) {
        this(id, displayName, description, population, capacity, available, formations,
                FormationVotePhase.NOT_STARTED, "");
    }

    public FactionSelectionView {
        id = Objects.requireNonNullElse(id, "");
        displayName = Objects.requireNonNullElse(displayName, id);
        description = Objects.requireNonNullElse(description, "");
        population = Math.max(0, population);
        capacity = Math.max(0, capacity);
        formations = List.copyOf(formations == null ? List.of() : formations);
        votePhase = Objects.requireNonNullElse(votePhase, FormationVotePhase.NOT_STARTED);
        lockedFormationId = Objects.requireNonNullElse(lockedFormationId, "");
    }

    /** The formation with {@code formationId}, or {@code null}. */
    public FormationSelectionView formation(String formationId) {
        if (formationId == null || formationId.isEmpty()) {
            return null;
        }
        for (FormationSelectionView formation : formations) {
            if (formation.id().equals(formationId)) {
                return formation;
            }
        }
        return null;
    }
}
