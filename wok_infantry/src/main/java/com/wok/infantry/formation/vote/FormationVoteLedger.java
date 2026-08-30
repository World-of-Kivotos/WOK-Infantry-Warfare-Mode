package com.wok.infantry.formation.vote;

import com.wok.infantry.battle.Faction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Policy-neutral in-memory authority for one independent ballot per battle faction.
 * The caller explicitly decides whether votes may be changed and which candidate is locked;
 * this class intentionally does not invent deadlines, winner rules, or tie breaking.
 */
public final class FormationVoteLedger {
    private static final int MAX_ID_LENGTH = 64;
    private static final int MAX_CANDIDATES = 32;

    private final EnumMap<Faction, Ballot> ballots = new EnumMap<>(Faction.class);
    private long revision;

    public synchronized FormationVoteResult open(Faction faction,
                                                 List<String> candidateFormationIds,
                                                 boolean allowVoteChange) {
        if (faction == null) {
            return FormationVoteResult.failure(FormationVoteResult.Code.INVALID_FACTION,
                    "阵营不存在");
        }
        List<String> candidates = normalizedCandidates(candidateFormationIds);
        if (candidates.isEmpty()) {
            return FormationVoteResult.failure(FormationVoteResult.Code.INVALID_CANDIDATES,
                    "投票必须至少包含一个有效具体编制");
        }
        ballots.put(faction, new Ballot(candidates, allowVoteChange));
        revision++;
        return FormationVoteResult.ok("编制投票已开启");
    }

    public synchronized FormationVoteResult cast(Faction faction, UUID voterId,
                                                 String formationId) {
        if (faction == null || voterId == null) {
            return FormationVoteResult.failure(FormationVoteResult.Code.INVALID_FACTION,
                    "阵营或投票玩家不存在");
        }
        Ballot ballot = ballots.get(faction);
        if (ballot == null || ballot.phase != FormationVotePhase.OPEN) {
            return FormationVoteResult.failure(FormationVoteResult.Code.BALLOT_NOT_OPEN,
                    "该阵营的编制投票尚未开启");
        }
        String normalized = normalizeId(formationId);
        if (normalized == null || !ballot.candidates.contains(normalized)) {
            return FormationVoteResult.failure(FormationVoteResult.Code.CANDIDATE_NOT_FOUND,
                    "具体编制不在当前候选列表中");
        }
        String previous = ballot.votes.get(voterId);
        if (previous != null && !previous.equals(normalized) && !ballot.allowVoteChange) {
            return FormationVoteResult.failure(FormationVoteResult.Code.VOTE_ALREADY_CAST,
                    "当前投票规则不允许改票");
        }
        if (normalized.equals(previous)) {
            return FormationVoteResult.ok("投票未改变");
        }
        ballot.votes.put(voterId, normalized);
        revision++;
        return FormationVoteResult.ok(previous == null ? "投票已记录" : "投票已修改");
    }

    /** Locks an explicitly selected result; winner and tie policy belong to the caller. */
    public synchronized FormationVoteResult lock(Faction faction, String formationId) {
        if (faction == null) {
            return FormationVoteResult.failure(FormationVoteResult.Code.INVALID_FACTION,
                    "阵营不存在");
        }
        Ballot ballot = ballots.get(faction);
        if (ballot == null || ballot.phase == FormationVotePhase.NOT_STARTED) {
            return FormationVoteResult.failure(FormationVoteResult.Code.BALLOT_NOT_OPEN,
                    "该阵营的编制投票尚未开启");
        }
        if (ballot.phase == FormationVotePhase.LOCKED) {
            return FormationVoteResult.failure(FormationVoteResult.Code.RESULT_ALREADY_LOCKED,
                    "该阵营的编制结果已经锁定");
        }
        String normalized = normalizeId(formationId);
        if (normalized == null || !ballot.candidates.contains(normalized)) {
            return FormationVoteResult.failure(FormationVoteResult.Code.CANDIDATE_NOT_FOUND,
                    "锁定结果不在当前候选列表中");
        }
        ballot.lockedFormationId = normalized;
        ballot.phase = FormationVotePhase.LOCKED;
        revision++;
        return FormationVoteResult.ok("阵营共享编制已锁定");
    }

    public synchronized void clear(Faction faction) {
        if (faction != null && ballots.remove(faction) != null) {
            revision++;
        }
    }

    public synchronized void clearAll() {
        if (!ballots.isEmpty()) {
            ballots.clear();
            revision++;
        }
    }

    /** Reconciles a live ballot after catalog reload without manufacturing a new ballot. */
    public synchronized boolean reconcileCandidates(Faction faction,
                                                    List<String> candidateFormationIds) {
        if (faction == null) {
            return false;
        }
        Ballot current = ballots.get(faction);
        if (current == null) {
            return false;
        }
        List<String> candidates = normalizedCandidates(candidateFormationIds);
        if (candidates.isEmpty()
                || current.phase == FormationVotePhase.LOCKED
                && !candidates.contains(current.lockedFormationId)) {
            ballots.remove(faction);
            revision++;
            return true;
        }
        boolean candidateChanged = !current.candidates.equals(candidates);
        boolean voteChanged = current.votes.values().stream()
                .anyMatch(vote -> !candidates.contains(vote));
        if (!candidateChanged && !voteChanged) {
            return false;
        }
        Ballot replacement = new Ballot(candidates, current.allowVoteChange);
        replacement.phase = current.phase;
        replacement.lockedFormationId = current.lockedFormationId;
        current.votes.forEach((voter, vote) -> {
            if (candidates.contains(vote)) {
                replacement.votes.put(voter, vote);
            }
        });
        ballots.put(faction, replacement);
        revision++;
        return true;
    }

    public synchronized FormationVoteSnapshot snapshot(Faction faction, UUID viewerId) {
        Objects.requireNonNull(faction, "faction");
        Ballot ballot = ballots.get(faction);
        if (ballot == null) {
            return new FormationVoteSnapshot(faction, revision,
                    FormationVotePhase.NOT_STARTED, false, "", "", List.of(), Map.of());
        }
        LinkedHashMap<String, Integer> tally = new LinkedHashMap<>();
        ballot.candidates.forEach(candidate -> tally.put(candidate, 0));
        ballot.votes.values().forEach(candidate ->
                tally.computeIfPresent(candidate, (ignored, count) -> count + 1));
        return new FormationVoteSnapshot(faction, revision, ballot.phase,
                ballot.allowVoteChange,
                ballot.lockedFormationId,
                viewerId == null ? "" : ballot.votes.getOrDefault(viewerId, ""),
                ballot.candidates, tally);
    }

    synchronized Map<Faction, StoredBallot> storedBallots() {
        EnumMap<Faction, StoredBallot> stored = new EnumMap<>(Faction.class);
        ballots.forEach((faction, ballot) -> stored.put(faction,
                new StoredBallot(ballot.phase, ballot.lockedFormationId,
                        ballot.candidates, ballot.votes, ballot.allowVoteChange)));
        return Map.copyOf(stored);
    }

    private static List<String> normalizedCandidates(List<String> source) {
        List<String> result = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        if (source != null) {
            for (String value : source) {
                if (result.size() >= MAX_CANDIDATES) {
                    break;
                }
                String normalized = normalizeId(value);
                if (normalized != null && unique.add(normalized)) {
                    result.add(normalized);
                }
            }
        }
        return List.copyOf(result);
    }

    private static String normalizeId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > MAX_ID_LENGTH) {
            return null;
        }
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (!(current >= 'a' && current <= 'z')
                    && !(current >= '0' && current <= '9')
                    && current != '_' && current != '-' && current != '.') {
                return null;
            }
        }
        return normalized;
    }

    private static final class Ballot {
        private final List<String> candidates;
        private final Map<UUID, String> votes = new LinkedHashMap<>();
        private final boolean allowVoteChange;
        private FormationVotePhase phase = FormationVotePhase.OPEN;
        private String lockedFormationId = "";

        private Ballot(List<String> candidates, boolean allowVoteChange) {
            this.candidates = List.copyOf(candidates);
            this.allowVoteChange = allowVoteChange;
        }
    }

    record StoredBallot(FormationVotePhase phase,
                        String lockedFormationId,
                        List<String> candidates,
                        Map<UUID, String> votes,
                        boolean allowVoteChange) {
        StoredBallot {
            Objects.requireNonNull(phase, "phase");
            lockedFormationId = Objects.requireNonNullElse(lockedFormationId, "");
            candidates = List.copyOf(candidates == null ? List.of() : candidates);
            votes = Map.copyOf(votes == null ? Map.of() : votes);
        }
    }
}
