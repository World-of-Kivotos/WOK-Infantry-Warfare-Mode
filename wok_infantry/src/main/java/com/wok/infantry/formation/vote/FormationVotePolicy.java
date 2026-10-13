package com.wok.infantry.formation.vote;

import java.util.Objects;

/**
 * Pure server rules of the shared formation vote (formation protocol 5), kept apart from
 * {@code FormationService} so they can be unit-tested without a running server.
 *
 * <ul>
 *   <li>A ballot locked for a match stays locked until a battle reset (vote-04).</li>
 *   <li>Only current faction members are counted (vote-10); a full faction does not stop its own
 *   members from voting (vote-08).</li>
 *   <li>A formation smaller than the faction cannot become the shared formation, so votes for it
 *   are rejected before the lock (vote-11).</li>
 *   <li>The administrator locks any candidate without voting first (vote-09).</li>
 *   <li>After the lock, joining and administrator assignment hand out the locked formation
 *   (vote-01/02/03); before it only the faction is assigned.</li>
 * </ul>
 */
public final class FormationVotePolicy {
    /** Why a vote is refused, checked in this order. */
    public enum VoteBlock {
        NONE,
        NOT_MEMBER,
        NOT_OPEN,
        NOT_CANDIDATE,
        CAPACITY_SHORTFALL,
        CHANGE_NOT_ALLOWED
    }

    /** What an administrator assignment of a player to a faction does. */
    public enum AssignPlan {
        /** The faction's ballot is locked: assign the locked formation. */
        ASSIGN_LOCKED,
        /** No locked result yet: assign only the faction; the formation follows the lock. */
        FACTION_ONLY,
        /** The ballot is locked and a different formation was requested. */
        REJECT_NOT_LOCKED_FORMATION
    }

    private FormationVotePolicy() {
    }

    /**
     * Whether a formation of {@code formationCapacity} cannot hold a faction of
     * {@code factionMembers} players and therefore cannot become its shared formation.
     */
    public static boolean capacityShortfall(int formationCapacity, int factionMembers) {
        return formationCapacity < factionMembers;
    }

    /**
     * How many players the faction may have when a new one joins: its configured maximum, and
     * after the lock also the locked formation's capacity.
     */
    public static int joinCapacity(int factionMaxPlayers, FormationVotePhase phase,
                                   int lockedFormationCapacity) {
        int maximum = Math.max(0, factionMaxPlayers);
        return phase == FormationVotePhase.LOCKED
                ? Math.min(maximum, Math.max(0, lockedFormationCapacity)) : maximum;
    }

    /** Whether one more player fits into a faction of {@code members} with {@code capacity}. */
    public static boolean canJoin(int members, int capacity) {
        return members < capacity;
    }

    /**
     * Vote admission. A faction that is full for new players still lets its members vote; a
     * formation that cannot hold the faction is refused before it could win.
     *
     * @param ownVote       the voter's current vote ("" = none)
     * @param changeAllowed whether the ballot allows changing a vote
     */
    public static VoteBlock voteBlock(boolean member, FormationVotePhase phase, boolean candidate,
                                      int formationCapacity, int factionMembers,
                                      String ownVote, String targetFormationId,
                                      boolean changeAllowed) {
        if (!member) {
            return VoteBlock.NOT_MEMBER;
        }
        if (phase != FormationVotePhase.OPEN) {
            return VoteBlock.NOT_OPEN;
        }
        if (!candidate) {
            return VoteBlock.NOT_CANDIDATE;
        }
        if (capacityShortfall(formationCapacity, factionMembers)) {
            return VoteBlock.CAPACITY_SHORTFALL;
        }
        String own = Objects.requireNonNullElse(ownVote, "");
        if (!own.isEmpty() && !own.equals(targetFormationId) && !changeAllowed) {
            return VoteBlock.CHANGE_NOT_ALLOWED;
        }
        return VoteBlock.NONE;
    }

    /**
     * Administrator assignment rule: after the lock only the locked formation may be assigned
     * ({@code requestedFormationId} null or blank means "whatever is locked"); before the lock only
     * the faction is assigned, so nobody is placed into a formation the faction did not vote for.
     */
    public static AssignPlan adminAssign(FormationVotePhase phase, String lockedFormationId,
                                         String requestedFormationId) {
        if (phase != FormationVotePhase.LOCKED || lockedFormationId == null
                || lockedFormationId.isBlank()) {
            return AssignPlan.FACTION_ONLY;
        }
        if (requestedFormationId == null || requestedFormationId.isBlank()
                || lockedFormationId.equals(requestedFormationId)) {
            return AssignPlan.ASSIGN_LOCKED;
        }
        return AssignPlan.REJECT_NOT_LOCKED_FORMATION;
    }

    /**
     * Whether a player who is in a faction without a formation receives the faction's locked
     * formation (late join, login after the lock, roster reconciliation).
     */
    public static boolean inheritsLockedFormation(boolean hasFaction, boolean hasFormation,
                                                  FormationVotePhase phase,
                                                  boolean lockedFormationAvailable) {
        return hasFaction && !hasFormation && phase == FormationVotePhase.LOCKED
                && lockedFormationAvailable;
    }
}
