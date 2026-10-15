package com.wok.infantry.formation.vote;

import com.wok.infantry.battle.Faction;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable faction-shared ballot state; ownVote is viewer-specific. */
public record FormationVoteSnapshot(Faction faction,
                                    long revision,
                                    FormationVotePhase phase,
                                    boolean voteChangeAllowed,
                                    String lockedFormationId,
                                    String ownVote,
                                    List<String> candidates,
                                    Map<String, Integer> tally) {
    public FormationVoteSnapshot {
        Objects.requireNonNull(faction, "faction");
        revision = Math.max(0L, revision);
        Objects.requireNonNull(phase, "phase");
        lockedFormationId = Objects.requireNonNullElse(lockedFormationId, "");
        ownVote = Objects.requireNonNullElse(ownVote, "");
        candidates = List.copyOf(candidates == null ? List.of() : candidates);
        tally = Map.copyOf(tally == null ? Map.of() : tally);
    }
}
