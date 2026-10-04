package com.wok.infantry.client.screen;

import com.wok.infantry.formation.FormationCategory;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.formation.vote.FormationVotePolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure state of the formation vote page (no rendering): which step the viewer is on, how every
 * faction key and formation row looks, what the join, vote and administrator keys do and why they
 * are disabled. Everything the screen shows is derived here so the rules can be unit-tested.
 *
 * <p>The flow is: join a faction (confirmed) → the administrator opens the vote → vote → the
 * administrator locks one formation (confirmed) → deploy. Faction keys and formation rows only
 * change what is being looked at; they never send anything. Blue means "being looked at"; before
 * joining the browsed faction is drawn as an outline (never solid blue, user report 1), and only
 * the joined faction is solid blue with a check.
 */
public final class FormationVoteModel {
    /** Where the viewer is in the flow. */
    public enum Stage {
        /** No catalog yet (opened from the terminal before the server answered). */
        WAITING,
        /** The server has no enabled faction. */
        EMPTY,
        /** Has not joined a faction. */
        UNJOINED,
        /** Joined; the faction's vote has not been opened. */
        NOT_STARTED,
        /** Joined; voting is open. */
        OPEN,
        /** Joined; the faction's formation is locked. */
        LOCKED
    }

    /** Step-by-step instruction shown in the footer and the faction strip (user report 3). */
    public enum Step {
        SYNC,
        EMPTY,
        /** First step: join the browsed faction. */
        JOIN,
        /** First step: join; the faction's formation is already locked and is used directly. */
        JOIN_LOCKED,
        /** The browsed faction cannot be joined, the other one can. */
        BROWSE_OTHER,
        /** No faction can be joined at the moment. */
        NO_JOINABLE,
        /** Second step: wait for the administrator to open the vote. */
        WAIT_OPEN,
        /** Second step (administrator): open the vote. */
        ADMIN_OPEN,
        /** Third step: pick a formation and vote. */
        VOTE,
        /**
         * Third step (administrator who has not voted): vote, or lock a result straight away
         * (the administrator never has to vote first, vote-09).
         */
        ADMIN_VOTE,
        /** Fourth step: voted, waiting for the administrator's lock. */
        WAIT_LOCK,
        /** Fourth step (administrator): pick the result and lock it. */
        ADMIN_LOCK,
        /** Done: the formation is locked, go to deployment. */
        DEPLOY,
        /** Locked, but this player holds no formation (faction over capacity). */
        LOCKED_NO_FORMATION
    }

    /** Look of a faction key in the strip. */
    public enum FactionLook {
        /** Not joined, not browsed: a plain key. */
        NORMAL,
        /** Not joined, being browsed: blue outline, left bar and light fill (not solid blue). */
        BROWSING,
        /** The joined faction: solid blue with a check (current, not clickable). */
        JOINED,
        /** The other faction after joining: disabled with a lock (cannot change faction). */
        OTHER
    }

    /** Why the join key is disabled. */
    public enum JoinBlock {
        NONE,
        /** The faction reached its maximum. */
        FACTION_FULL,
        /** The faction reached the capacity of its locked formation. */
        LOCKED_FULL,
        /** The faction has no candidate formation. */
        NO_CANDIDATES,
        /** Disabled for another server reason (locked formation unusable, faction disabled). */
        UNAVAILABLE
    }

    /** Join key of the browsed faction. */
    public record JoinAction(boolean enabled, JoinBlock block, int population, int capacity,
                             String lockedFormationId) {
        public boolean locked() {
            return !lockedFormationId.isEmpty();
        }
    }

    /** Label of the vote/action key. */
    public enum VoteLabel {
        VOTE,
        CHANGE,
        /** "你的票" badge instead of a key. */
        MINE,
        DEPLOY,
        CANNOT_JOIN,
        USE_AFTER_JOIN,
        NOT_CHOSEN
    }

    /** Text next to the vote key: why it is disabled, or what it does. */
    public enum Reason {
        CHANGE_ALLOWED_AFTER,
        NO_CHANGE_AFTER,
        CURRENT_VOTE,
        MINE_CAN_CHANGE,
        MINE_FIXED,
        LOCKED_USES_THIS,
        LOCKED_NOT_CHOSEN,
        JOIN_FIRST,
        FACTION_FULL,
        LATE_USE_THIS,
        LATE_NOT_CHOSEN,
        WAIT_OPEN,
        UNAVAILABLE,
        SHORTFALL,
        NOT_CANDIDATE,
        NO_CHANGE,
        NO_FORMATION
    }

    /** Vote key of the highlighted formation. */
    public record VoteAction(VoteLabel label, boolean enabled, Reason reason, TacticalIcon icon,
                             TacticalIcon buttonIcon) {
        public boolean mine() {
            return label == VoteLabel.MINE;
        }

        /** Whether Enter casts this vote (a live vote or change key). */
        public boolean votes() {
            return enabled && (label == VoteLabel.VOTE || label == VoteLabel.CHANGE);
        }
    }

    /** Status mark at the right of a formation row. */
    public enum RowMark {
        NONE,
        LOCKED,
        NOT_CHOSEN,
        DISABLED,
        SHORTFALL
    }

    /** What a formation row shows besides its name. */
    public record RowStatus(RowMark mark, boolean mine, boolean showVotes, int votes,
                            int percent) {
        public boolean dimmed() {
            return mark == RowMark.DISABLED || mark == RowMark.SHORTFALL
                    || mark == RowMark.NOT_CHOSEN;
        }
    }

    /** How the administrator's lock target relates to the current leaders. */
    public enum AdminRelation {
        SOLE_LEADER,
        NOT_LEADER,
        TIE_INCLUDED,
        TIE_EXCLUDED,
        NO_VOTES
    }

    /** Why the administrator cannot lock the target. */
    public enum LockBlock {
        NONE,
        NO_TARGET,
        UNAVAILABLE,
        SHORTFALL,
        NOT_CANDIDATE
    }

    /** Administrator area (only the viewer's own faction, only before the lock). */
    public record AdminState(boolean visible, boolean opening, boolean openEnabled,
                             boolean lockEnabled, LockBlock lockBlock, AdminRelation relation,
                             int candidates) {
        static final AdminState HIDDEN = new AdminState(false, false, false, false,
                LockBlock.NO_TARGET, AdminRelation.NO_VOTES, 0);
    }

    /** Leading formations of the ballot; more than one id is a tie. */
    public record Leaders(List<String> ids, int votes) {
        public Leaders {
            ids = List.copyOf(ids);
        }

        public boolean tie() {
            return ids.size() > 1;
        }

        public boolean none() {
            return ids.isEmpty();
        }
    }

    /** What Esc does when no modal is open. */
    public enum EscAction {
        /** Narrow detail page: back to the list page. */
        BACK_TO_LIST,
        /** Close the page (not joined: with a hint how to reopen it, user report 4). */
        CLOSE
    }

    private final FormationSelectionSnapshot snapshot;
    private final boolean administrator;
    private final FactionSelectionView browsing;
    private final FormationSelectionView highlighted;

    private FormationVoteModel(FormationSelectionSnapshot snapshot, String browseFactionId,
                               String highlightedFormationId, boolean administrator) {
        this.snapshot = snapshot;
        this.administrator = administrator;
        this.browsing = resolveBrowsing(snapshot, browseFactionId);
        this.highlighted = browsing == null ? null : resolveHighlight(browsing,
                highlightedFormationId);
    }

    /**
     * @param snapshot               the catalog, or {@code null} while waiting for it
     * @param browseFactionId        faction the viewer is looking at (ignored after joining)
     * @param highlightedFormationId formation the viewer is looking at (falls back to
     *                               {@link #initialHighlight})
     * @param administrator          whether the viewer has the administrator permission level
     */
    public static FormationVoteModel of(FormationSelectionSnapshot snapshot,
                                        String browseFactionId, String highlightedFormationId,
                                        boolean administrator) {
        return new FormationVoteModel(snapshot, browseFactionId, highlightedFormationId,
                administrator);
    }

    // ---- basic state ------------------------------------------------------------------------

    public FormationSelectionSnapshot snapshot() {
        return snapshot;
    }

    public boolean administrator() {
        return administrator;
    }

    public boolean waiting() {
        return snapshot == null;
    }

    public boolean joined() {
        return snapshot != null && snapshot.faction(snapshot.selectedFactionId()) != null;
    }

    /** The joined faction, or {@code null}. */
    public FactionSelectionView joinedFaction() {
        return joined() ? snapshot.faction(snapshot.selectedFactionId()) : null;
    }

    /** The faction being looked at: the joined one, else the browsed one, else the first. */
    public FactionSelectionView browsing() {
        return browsing;
    }

    /** The formation being looked at, or {@code null} when the faction has none. */
    public FormationSelectionView highlighted() {
        return highlighted;
    }

    public boolean hasFormation() {
        return joined() && !snapshot.selectedFormationId().isBlank();
    }

    public Stage stage() {
        if (snapshot == null) {
            return Stage.WAITING;
        }
        if (snapshot.factions().isEmpty() || browsing == null) {
            return Stage.EMPTY;
        }
        if (!joined()) {
            return Stage.UNJOINED;
        }
        return switch (snapshot.votePhase()) {
            case NOT_STARTED -> Stage.NOT_STARTED;
            case OPEN -> Stage.OPEN;
            case LOCKED -> Stage.LOCKED;
        };
    }

    /** Ballot phase of {@code faction} (the viewer's own faction uses its personal fields). */
    public FormationVotePhase phase(FactionSelectionView faction) {
        if (faction == null) {
            return FormationVotePhase.NOT_STARTED;
        }
        if (isOwn(faction)) {
            return snapshot.votePhase();
        }
        return faction.votePhase();
    }

    /** Locked formation id of {@code faction}, or "". */
    public String lockedFormationId(FactionSelectionView faction) {
        if (faction == null || phase(faction) != FormationVotePhase.LOCKED) {
            return "";
        }
        String locked = isOwn(faction) ? snapshot.lockedFormationId()
                : faction.lockedFormationId();
        return locked.isEmpty() ? faction.lockedFormationId() : locked;
    }

    /** Locked formation of {@code faction}, or {@code null}. */
    public FormationSelectionView lockedFormation(FactionSelectionView faction) {
        return faction == null ? null : faction.formation(lockedFormationId(faction));
    }

    private boolean isOwn(FactionSelectionView faction) {
        return snapshot != null && faction != null
                && faction.id().equals(snapshot.selectedFactionId());
    }

    /**
     * How many players the faction may hold for a new member: its maximum, and after the lock
     * also the locked formation's capacity.
     */
    public int effectiveCapacity(FactionSelectionView faction) {
        FormationSelectionView locked = lockedFormation(faction);
        return FormationVotePolicy.joinCapacity(faction.capacity(), phase(faction),
                locked == null ? faction.capacity() : locked.capacity());
    }

    /**
     * Whether {@code formation} is too small for its faction before the lock (it could not
     * become the shared formation; votes for it are refused).
     */
    public boolean shortfall(FactionSelectionView faction, FormationSelectionView formation) {
        return faction != null && formation != null
                && phase(faction) != FormationVotePhase.LOCKED
                && FormationVotePolicy.capacityShortfall(formation.capacity(),
                faction.population());
    }

    /** Whether the viewer's ballot lists {@code formation} as a candidate. */
    public boolean candidate(FactionSelectionView faction, FormationSelectionView formation) {
        if (formation == null || !formation.available()) {
            return false;
        }
        if (isOwn(faction) && snapshot.votePhase() == FormationVotePhase.OPEN
                && !snapshot.voteTally().isEmpty()) {
            return snapshot.voteTally().containsKey(formation.id());
        }
        return true;
    }

    // ---- tally --------------------------------------------------------------------------------

    /** Whether vote counts are shown (own faction, vote opened). */
    public boolean showVotes(FactionSelectionView faction) {
        return isOwn(faction) && snapshot.votePhase() != FormationVotePhase.NOT_STARTED;
    }

    public int votes(String formationId) {
        if (snapshot == null) {
            return 0;
        }
        return Math.max(0, snapshot.voteTally().getOrDefault(formationId, 0));
    }

    public int totalVotes() {
        return snapshot == null ? 0
                : snapshot.voteTally().values().stream().mapToInt(value -> Math.max(0, value))
                .sum();
    }

    /** Whole-number share of the votes (0 without votes). */
    public int percent(String formationId) {
        int total = totalVotes();
        return total <= 0 ? 0 : Math.round(votes(formationId) * 100.0F / total);
    }

    /** Leading formation(s) of the viewer's ballot. */
    public Leaders leaders() {
        if (snapshot == null) {
            return new Leaders(List.of(), 0);
        }
        int best = 0;
        for (int votes : snapshot.voteTally().values()) {
            best = Math.max(best, votes);
        }
        if (best <= 0) {
            return new Leaders(List.of(), 0);
        }
        List<String> ids = new ArrayList<>();
        FactionSelectionView own = joinedFaction();
        List<FormationSelectionView> order = own == null ? List.of() : orderedFormations(own);
        for (FormationSelectionView formation : order) {
            if (votes(formation.id()) == best) {
                ids.add(formation.id());
            }
        }
        for (Map.Entry<String, Integer> entry : snapshot.voteTally().entrySet()) {
            if (entry.getValue() == best && !ids.contains(entry.getKey())) {
                ids.add(entry.getKey());
            }
        }
        return new Leaders(ids, best);
    }

    // ---- flow ---------------------------------------------------------------------------------

    /** Step-by-step instruction for the footer and the strip. */
    public Step step() {
        return switch (stage()) {
            case WAITING -> Step.SYNC;
            case EMPTY -> Step.EMPTY;
            case UNJOINED -> {
                if (snapshot.factions().stream().noneMatch(FactionSelectionView::available)) {
                    yield Step.NO_JOINABLE;
                }
                if (!browsing.available()) {
                    yield Step.BROWSE_OTHER;
                }
                yield phase(browsing) == FormationVotePhase.LOCKED ? Step.JOIN_LOCKED : Step.JOIN;
            }
            case NOT_STARTED -> administrator ? Step.ADMIN_OPEN : Step.WAIT_OPEN;
            case OPEN -> snapshot.ownVoteFormationId().isBlank()
                    ? administrator ? Step.ADMIN_VOTE : Step.VOTE
                    : administrator ? Step.ADMIN_LOCK : Step.WAIT_LOCK;
            case LOCKED -> hasFormation() ? Step.DEPLOY : Step.LOCKED_NO_FORMATION;
        };
    }

    /** Look of a faction key. */
    public FactionLook factionLook(FactionSelectionView faction) {
        if (joined()) {
            return isOwn(faction) ? FactionLook.JOINED : FactionLook.OTHER;
        }
        return browsing != null && browsing.id().equals(faction.id())
                ? FactionLook.BROWSING : FactionLook.NORMAL;
    }

    /** Whether clicking a faction key changes what is browsed (never after joining). */
    public boolean factionClickable(FactionSelectionView faction) {
        return !joined() && faction != null;
    }

    /** Join key for the browsed faction (only shown while not joined). */
    public JoinAction joinAction() {
        if (browsing == null || joined()) {
            return new JoinAction(false, JoinBlock.UNAVAILABLE, 0, 0, "");
        }
        int capacity = effectiveCapacity(browsing);
        String locked = lockedFormationId(browsing);
        if (browsing.available()) {
            return new JoinAction(true, JoinBlock.NONE, browsing.population(), capacity, locked);
        }
        JoinBlock block;
        if (!FormationVotePolicy.canJoin(browsing.population(), capacity)) {
            block = capacity < browsing.capacity() ? JoinBlock.LOCKED_FULL
                    : JoinBlock.FACTION_FULL;
        } else if (browsing.formations().stream().noneMatch(FormationSelectionView::available)) {
            block = JoinBlock.NO_CANDIDATES;
        } else {
            block = JoinBlock.UNAVAILABLE;
        }
        return new JoinAction(false, block, browsing.population(), capacity, locked);
    }

    /** Vote key for the highlighted formation. */
    public VoteAction voteAction() {
        return voteAction(highlighted);
    }

    /** Vote key for {@code formation} of the browsed faction (see the class comment). */
    public VoteAction voteAction(FormationSelectionView formation) {
        if (formation == null || browsing == null) {
            return new VoteAction(VoteLabel.VOTE, false, Reason.UNAVAILABLE, TacticalIcon.LOCK,
                    null);
        }
        FormationVotePhase phase = phase(browsing);
        if (!joined()) {
            if (!FormationVotePolicy.canJoin(browsing.population(), effectiveCapacity(browsing))) {
                return new VoteAction(phase == FormationVotePhase.LOCKED
                        ? VoteLabel.CANNOT_JOIN : VoteLabel.VOTE, false, Reason.FACTION_FULL,
                        TacticalIcon.LOCK, null);
            }
            if (phase == FormationVotePhase.LOCKED) {
                return formation.id().equals(lockedFormationId(browsing))
                        ? new VoteAction(VoteLabel.USE_AFTER_JOIN, false, Reason.LATE_USE_THIS,
                        TacticalIcon.LOCK, null)
                        : new VoteAction(VoteLabel.NOT_CHOSEN, false, Reason.LATE_NOT_CHOSEN,
                        TacticalIcon.LOCK, null);
            }
            return new VoteAction(VoteLabel.VOTE, false, Reason.JOIN_FIRST, TacticalIcon.LOCK,
                    null);
        }
        if (phase == FormationVotePhase.LOCKED) {
            Reason reason = formation.id().equals(lockedFormationId(browsing))
                    ? Reason.LOCKED_USES_THIS : Reason.LOCKED_NOT_CHOSEN;
            return hasFormation()
                    ? new VoteAction(VoteLabel.DEPLOY, true, reason, TacticalIcon.LOCK,
                    TacticalIcon.DEPLOY)
                    : new VoteAction(VoteLabel.DEPLOY, false, Reason.NO_FORMATION,
                    TacticalIcon.LOCK, TacticalIcon.DEPLOY);
        }
        if (phase == FormationVotePhase.NOT_STARTED) {
            return new VoteAction(VoteLabel.VOTE, false, Reason.WAIT_OPEN, TacticalIcon.CLOCK,
                    null);
        }
        if (!formation.available()) {
            return new VoteAction(VoteLabel.VOTE, false, Reason.UNAVAILABLE, TacticalIcon.LOCK,
                    null);
        }
        if (shortfall(browsing, formation)) {
            return new VoteAction(VoteLabel.VOTE, false, Reason.SHORTFALL, TacticalIcon.LOCK,
                    null);
        }
        if (!candidate(browsing, formation)) {
            return new VoteAction(VoteLabel.VOTE, false, Reason.NOT_CANDIDATE, TacticalIcon.LOCK,
                    null);
        }
        String own = snapshot.ownVoteFormationId();
        boolean change = snapshot.voteChangeAllowed();
        if (own.equals(formation.id())) {
            return new VoteAction(VoteLabel.MINE, false,
                    change ? Reason.MINE_CAN_CHANGE : Reason.MINE_FIXED, TacticalIcon.INFO,
                    TacticalIcon.CHECK);
        }
        if (!own.isBlank() && !change) {
            return new VoteAction(VoteLabel.CHANGE, false, Reason.NO_CHANGE, TacticalIcon.LOCK,
                    null);
        }
        if (!own.isBlank()) {
            return new VoteAction(VoteLabel.CHANGE, true, Reason.CURRENT_VOTE, TacticalIcon.INFO,
                    TacticalIcon.CHECK);
        }
        return new VoteAction(VoteLabel.VOTE, true,
                change ? Reason.CHANGE_ALLOWED_AFTER : Reason.NO_CHANGE_AFTER, TacticalIcon.INFO,
                TacticalIcon.CHECK);
    }

    /** Status of a formation row of the browsed faction. */
    public RowStatus rowStatus(FormationSelectionView formation) {
        FactionSelectionView faction = browsing;
        boolean showVotes = showVotes(faction);
        int votes = showVotes ? votes(formation.id()) : 0;
        int percent = showVotes ? percent(formation.id()) : 0;
        boolean mine = joined() && isOwn(faction)
                && formation.id().equals(snapshot.ownVoteFormationId());
        RowMark mark;
        boolean locked = phase(faction) == FormationVotePhase.LOCKED;
        if (!formation.available()) {
            mark = RowMark.DISABLED;
        } else if (shortfall(faction, formation)) {
            mark = RowMark.SHORTFALL;
        } else if (locked && formation.id().equals(lockedFormationId(faction))) {
            mark = RowMark.LOCKED;
        } else if (locked) {
            mark = RowMark.NOT_CHOSEN;
        } else {
            mark = RowMark.NONE;
        }
        return new RowStatus(mark, mine, showVotes, votes, percent);
    }

    /** Administrator area for the viewer's own faction (hidden when not joined or locked). */
    public AdminState admin() {
        FactionSelectionView own = joinedFaction();
        if (!administrator || own == null || snapshot.votePhase() == FormationVotePhase.LOCKED) {
            return AdminState.HIDDEN;
        }
        int candidates = (int) own.formations().stream()
                .filter(formation -> candidate(own, formation)).count();
        boolean opening = snapshot.votePhase() == FormationVotePhase.NOT_STARTED;
        if (opening) {
            return new AdminState(true, true, candidates > 0, false, LockBlock.NO_TARGET,
                    AdminRelation.NO_VOTES, candidates);
        }
        FormationSelectionView target = highlighted;
        LockBlock block;
        if (target == null) {
            block = LockBlock.NO_TARGET;
        } else if (!target.available()) {
            block = LockBlock.UNAVAILABLE;
        } else if (shortfall(own, target)) {
            block = LockBlock.SHORTFALL;
        } else if (!candidate(own, target)) {
            block = LockBlock.NOT_CANDIDATE;
        } else {
            block = LockBlock.NONE;
        }
        return new AdminState(true, false, false, block == LockBlock.NONE, block,
                relation(target), candidates);
    }

    /** Why the administrator's test-start key is disabled. */
    public enum TestBlock {
        NONE,
        /** The browsed faction has no formation to use. */
        NO_TARGET,
        /** The highlighted formation is disabled or misses a required mod. */
        UNAVAILABLE,
        /** The highlighted formation is smaller than the faction (it could not be locked). */
        SHORTFALL
    }

    /**
     * Administrator test-start key (0.4.0-beta.2): visible to an administrator whenever a
     * catalog is shown, before and after joining and also after the lock. It sends
     * {@code battle admin test start <factionId> <formationId>}: the browsed faction and the
     * highlighted formation, or the faction's locked formation, which a test start never
     * changes ({@code lockKept} when another formation is highlighted).
     */
    public record TestStart(boolean visible, TestBlock block, String factionId,
                            String formationId, boolean locked, boolean lockKept) {
        static final TestStart HIDDEN = new TestStart(false, TestBlock.NO_TARGET, "", "", false,
                false);

        public boolean enabled() {
            return visible && block == TestBlock.NONE;
        }
    }

    /** The administrator test-start key for what the page shows. */
    public TestStart testStart() {
        if (!administrator || snapshot == null || browsing == null) {
            return TestStart.HIDDEN;
        }
        String locked = lockedFormationId(browsing);
        if (!locked.isEmpty()) {
            boolean other = highlighted != null && !highlighted.id().equals(locked);
            return new TestStart(true, TestBlock.NONE, browsing.id(), locked, true, other);
        }
        if (highlighted == null) {
            return new TestStart(true, TestBlock.NO_TARGET, browsing.id(), "", false, false);
        }
        TestBlock block = !highlighted.available() ? TestBlock.UNAVAILABLE
                : shortfall(browsing, highlighted) ? TestBlock.SHORTFALL : TestBlock.NONE;
        return new TestStart(true, block, browsing.id(), highlighted.id(), false, false);
    }

    /** How {@code target} relates to the leaders of the viewer's ballot. */
    public AdminRelation relation(FormationSelectionView target) {
        Leaders leaders = leaders();
        if (leaders.none() || target == null) {
            return AdminRelation.NO_VOTES;
        }
        if (leaders.tie()) {
            return leaders.ids().contains(target.id())
                    ? AdminRelation.TIE_INCLUDED : AdminRelation.TIE_EXCLUDED;
        }
        return leaders.ids().get(0).equals(target.id())
                ? AdminRelation.SOLE_LEADER : AdminRelation.NOT_LEADER;
    }

    /** Whether Enter casts a vote for the highlighted formation. */
    public boolean enterVotes() {
        return joined() && voteAction().votes();
    }

    /** What a newer catalog does to an open join or lock confirmation. */
    public enum DialogFate {
        /** Still valid: rewrite its text from the new catalog. */
        KEEP,
        /** Settled by itself (joined meanwhile, the ballot got locked): close, no receipt. */
        CLOSE_SILENTLY,
        /** Its target can no longer be joined or locked: close it and say so. */
        CLOSE_WITH_NOTICE
    }

    /**
     * Fate of the join confirmation for {@code factionId} under this (newer) catalog: the viewer
     * joined meanwhile (an administrator assigned them), or the faction can no longer be joined.
     */
    public DialogFate joinDialogFate(String factionId) {
        if (joined()) {
            return DialogFate.CLOSE_SILENTLY;
        }
        if (browsing == null || !browsing.id().equals(factionId) || !joinAction().enabled()) {
            return DialogFate.CLOSE_WITH_NOTICE;
        }
        return DialogFate.KEEP;
    }

    /**
     * Fate of the administrator's lock confirmation for {@code formationId} of {@code factionId}
     * under this (newer) catalog: the ballot got locked meanwhile, or the target can no longer be
     * locked (gone, disabled, too small, no longer a candidate).
     */
    public DialogFate lockDialogFate(String factionId, String formationId) {
        FactionSelectionView own = joinedFaction();
        if (own == null || !own.id().equals(factionId)) {
            return DialogFate.CLOSE_WITH_NOTICE;
        }
        if (snapshot.votePhase() == FormationVotePhase.LOCKED) {
            return DialogFate.CLOSE_SILENTLY;
        }
        if (highlighted == null || !highlighted.id().equals(formationId)
                || !admin().lockEnabled()) {
            return DialogFate.CLOSE_WITH_NOTICE;
        }
        return DialogFate.KEEP;
    }

    /** What Esc does on the page itself (a modal is closed first by the screen). */
    public static EscAction escAction(boolean narrowDetailPage) {
        return narrowDetailPage ? EscAction.BACK_TO_LIST : EscAction.CLOSE;
    }

    /** What an arriving catalog does to the client's screens. */
    public enum Arrival {
        /** The formation was just applied: open the deployment page in place of the terminal. */
        DEPLOYMENT,
        /** The formation was just applied while another mod's screen is open: HUD notice only. */
        NOTICE_ONLY,
        /** Close the server-pushed vote page: nothing is left to choose. */
        CLOSE,
        /** The terminal key opened the page but a formation exists: show the squad page. */
        SQUADS,
        /** Show the new catalog in the open vote page. */
        REPLACE,
        /** Open the vote page (server request while a choice is pending or a vote is open). */
        OPEN,
        /** Only cache the catalog. */
        IGNORE
    }

    /**
     * Pure routing of an arriving catalog.
     *
     * @param openScreen      the server asks to show the vote page
     * @param lockNotice      the faction's formation was just applied to the viewer
     * @param formationScreen entry of the vote page that is open, or {@code null} when another
     *                        screen (or none) is open
     * @param replaceable     the current screen may be replaced by the deployment page (no
     *                        screen, or a battle-terminal screen); other mods' screens and WOK
     *                        editors with drafts are never replaced (HUD notice instead)
     */
    public static Arrival arrival(FormationSelectionSnapshot snapshot, boolean openScreen,
                                  boolean lockNotice, FormationSelectionScreen.Entry formationScreen,
                                  boolean replaceable) {
        if (lockNotice && !snapshot.selectedFormationId().isBlank()) {
            return replaceable ? Arrival.DEPLOYMENT : Arrival.NOTICE_ONLY;
        }
        boolean settled = !snapshot.selectionRequired();
        if (formationScreen != null) {
            if (formationScreen == FormationSelectionScreen.Entry.KEY && settled) {
                return Arrival.SQUADS;
            }
            if (formationScreen == FormationSelectionScreen.Entry.SERVER && settled
                    && snapshot.votePhase() != FormationVotePhase.OPEN) {
                return Arrival.CLOSE;
            }
            return Arrival.REPLACE;
        }
        if (openScreen && (!settled || snapshot.votePhase() == FormationVotePhase.OPEN)) {
            return Arrival.OPEN;
        }
        return Arrival.IGNORE;
    }

    // ---- ordering and defaults ---------------------------------------------------------------

    /**
     * Formations of {@code faction} grouped by category (fixed category order, unknown
     * categories after them in order of appearance), catalog order within a category.
     */
    public static List<FormationSelectionView> orderedFormations(FactionSelectionView faction) {
        if (faction == null) {
            return List.of();
        }
        List<FormationSelectionView> source = faction.formations();
        List<String> unknownOrder = new ArrayList<>();
        for (FormationSelectionView formation : source) {
            if (FormationCategory.byId(formation.categoryId()).isEmpty()
                    && !unknownOrder.contains(formation.categoryId())) {
                unknownOrder.add(formation.categoryId());
            }
        }
        List<FormationSelectionView> sorted = new ArrayList<>(source);
        sorted.sort(Comparator.comparingInt(formation -> categoryRank(formation.categoryId(),
                unknownOrder)));
        return List.copyOf(sorted);
    }

    private static int categoryRank(String categoryId, List<String> unknownOrder) {
        return FormationCategory.byId(categoryId).map(Enum::ordinal)
                .orElseGet(() -> FormationCategory.values().length
                        + Math.max(0, unknownOrder.indexOf(categoryId)));
    }

    /**
     * Formation shown when the page opens or the faction changes: the locked result, then the
     * viewer's vote, then the first candidate, then the first formation (player-07).
     */
    public static String initialHighlight(FormationSelectionSnapshot snapshot,
                                          FactionSelectionView faction) {
        if (faction == null) {
            return "";
        }
        boolean own = snapshot != null && faction.id().equals(snapshot.selectedFactionId());
        String locked = own && snapshot.votePhase() == FormationVotePhase.LOCKED
                ? snapshot.lockedFormationId() : faction.votePhase() == FormationVotePhase.LOCKED
                ? faction.lockedFormationId() : "";
        if (!locked.isEmpty() && faction.formation(locked) != null) {
            return locked;
        }
        if (own && !snapshot.ownVoteFormationId().isBlank()
                && faction.formation(snapshot.ownVoteFormationId()) != null) {
            return snapshot.ownVoteFormationId();
        }
        List<FormationSelectionView> ordered = orderedFormations(faction);
        return ordered.stream().filter(FormationSelectionView::available).findFirst()
                .or(() -> ordered.stream().findFirst())
                .map(FormationSelectionView::id).orElse("");
    }

    /**
     * Faction shown when the page opens without a joined faction: the requested one if it exists,
     * otherwise the first joinable one, otherwise the first.
     */
    public static String initialFaction(FormationSelectionSnapshot snapshot, String requested) {
        if (snapshot == null) {
            return "";
        }
        if (snapshot.faction(snapshot.selectedFactionId()) != null) {
            return snapshot.selectedFactionId();
        }
        if (snapshot.faction(requested) != null) {
            return requested;
        }
        return snapshot.factions().stream().filter(FactionSelectionView::available)
                .findFirst().or(() -> snapshot.factions().stream().findFirst())
                .map(FactionSelectionView::id).orElse("");
    }

    private static FactionSelectionView resolveBrowsing(FormationSelectionSnapshot snapshot,
                                                        String browseFactionId) {
        if (snapshot == null) {
            return null;
        }
        String id = initialFaction(snapshot, Objects.requireNonNullElse(browseFactionId, ""));
        return snapshot.faction(id);
    }

    private FormationSelectionView resolveHighlight(FactionSelectionView faction,
                                                    String requested) {
        FormationSelectionView formation = faction.formation(requested);
        return formation != null ? formation
                : faction.formation(initialHighlight(snapshot, faction));
    }

    /** Name of formation {@code formationId} in any faction, or the id itself. */
    public String formationName(String formationId) {
        if (snapshot != null) {
            for (FactionSelectionView faction : snapshot.factions()) {
                FormationSelectionView formation = faction.formation(formationId);
                if (formation != null) {
                    return formation.displayName();
                }
            }
        }
        return Objects.requireNonNullElse(formationId, "");
    }
}
