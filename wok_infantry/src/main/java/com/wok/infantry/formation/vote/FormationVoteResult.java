package com.wok.infantry.formation.vote;

import java.util.Objects;

/** Policy-neutral result returned by the vote ledger. */
public record FormationVoteResult(boolean success, Code code, String message) {
    public enum Code {
        OK,
        INVALID_FACTION,
        INVALID_CANDIDATES,
        BALLOT_NOT_OPEN,
        CANDIDATE_NOT_FOUND,
        VOTE_ALREADY_CAST,
        RESULT_ALREADY_LOCKED
    }

    public FormationVoteResult {
        Objects.requireNonNull(code, "code");
        message = Objects.requireNonNullElse(message, "");
    }

    public static FormationVoteResult ok(String message) {
        return new FormationVoteResult(true, Code.OK, message);
    }

    public static FormationVoteResult failure(Code code, String message) {
        return new FormationVoteResult(false, code, message);
    }
}
