package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.FormationVoteModel.AdminRelation;
import com.wok.infantry.client.screen.FormationVoteModel.Arrival;
import com.wok.infantry.client.screen.FormationVoteModel.FactionLook;
import com.wok.infantry.client.screen.FormationVoteModel.JoinBlock;
import com.wok.infantry.client.screen.FormationVoteModel.LockBlock;
import com.wok.infantry.client.screen.FormationVoteModel.Reason;
import com.wok.infantry.client.screen.FormationVoteModel.RowMark;
import com.wok.infantry.client.screen.FormationVoteModel.Stage;
import com.wok.infantry.client.screen.FormationVoteModel.Step;
import com.wok.infantry.client.screen.FormationVoteModel.VoteLabel;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.wok.infantry.client.screen.FormationVoteFixtures.ACADEMY;
import static com.wok.infantry.client.screen.FormationVoteFixtures.CAESAR;
import static com.wok.infantry.client.screen.FormationVoteFixtures.CAVALRY;
import static com.wok.infantry.client.screen.FormationVoteFixtures.DEFAULT;
import static com.wok.infantry.client.screen.FormationVoteFixtures.MOBILE;
import static com.wok.infantry.client.screen.FormationVoteFixtures.academy;
import static com.wok.infantry.client.screen.FormationVoteFixtures.caesar;
import static com.wok.infantry.client.screen.FormationVoteFixtures.joined;
import static com.wok.infantry.client.screen.FormationVoteFixtures.tally;
import static com.wok.infantry.client.screen.FormationVoteFixtures.unjoined;
import static com.wok.infantry.client.screen.FormationVoteFixtures.withFactions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationVoteModelTest {
    private static FormationVoteModel model(FormationSelectionSnapshot snapshot, String browse,
                                            String highlight, boolean admin) {
        return FormationVoteModel.of(snapshot, browse, highlight, admin);
    }

    // ---- user report 1: browsing is not "joined" -------------------------------------------------

    @Test
    void browsedFactionBeforeJoiningIsOutlinedNeverSolidBlue() {
        FormationVoteModel model = model(unjoined(), ACADEMY, "", false);

        assertEquals(Stage.UNJOINED, model.stage());
        assertEquals(FactionLook.BROWSING, model.factionLook(model.snapshot().faction(ACADEMY)));
        assertEquals(FactionLook.NORMAL, model.factionLook(model.snapshot().faction(CAESAR)));
        assertTrue(model.factionClickable(model.snapshot().faction(CAESAR)),
                "faction keys only switch what is browsed");
        assertEquals(Step.JOIN, model.step());
        assertTrue(model.joinAction().enabled());
        FormationVoteModel.VoteAction vote = model.voteAction();
        assertFalse(vote.enabled());
        assertEquals(Reason.JOIN_FIRST, vote.reason());
    }

    @Test
    void joinedFactionIsCurrentAndTheOtherOneIsLockedAway() {
        FormationVoteModel model = model(joined(FormationVotePhase.NOT_STARTED, true, "",
                Map.of()), CAESAR, "", false);

        assertEquals(ACADEMY, model.browsing().id(), "after joining only the own faction shows");
        assertEquals(FactionLook.JOINED, model.factionLook(model.snapshot().faction(ACADEMY)));
        assertEquals(FactionLook.OTHER, model.factionLook(model.snapshot().faction(CAESAR)));
        assertFalse(model.factionClickable(model.snapshot().faction(CAESAR)));
        assertFalse(model.joinAction().enabled());
    }

    // ---- user report 2: not started = disabled with a reason ----------------------------------------

    @Test
    void notStartedVoteKeyIsDisabledAndSaysWaitForTheAdministrator() {
        FormationVoteModel model = model(joined(FormationVotePhase.NOT_STARTED, true, "",
                Map.of()), "", MOBILE, false);

        assertEquals(Stage.NOT_STARTED, model.stage());
        FormationVoteModel.VoteAction vote = model.voteAction();
        assertEquals(VoteLabel.VOTE, vote.label());
        assertFalse(vote.enabled());
        assertEquals(Reason.WAIT_OPEN, vote.reason());
        assertEquals(TacticalIcon.CLOCK, vote.icon());
        assertEquals(Step.WAIT_OPEN, model.step());
        assertFalse(model.enterVotes());
        assertFalse(model.showVotes(model.browsing()), "no vote counts before the vote opens");
        assertFalse(model.admin().visible(), "players without permission see no admin area");
    }

    @Test
    void administratorSeesTheOpenKeyBeforeTheVote() {
        FormationVoteModel model = model(joined(FormationVotePhase.NOT_STARTED, true, "",
                Map.of()), "", DEFAULT, true);

        FormationVoteModel.AdminState admin = model.admin();
        assertTrue(admin.visible());
        assertTrue(admin.opening());
        assertTrue(admin.openEnabled());
        assertEquals(3, admin.candidates());
        assertEquals(Step.ADMIN_OPEN, model.step());
    }

    // ---- user report 3: steps ---------------------------------------------------------------------

    @Test
    void stepsWalkThroughJoinOpenVoteLockAndDeploy() {
        assertEquals(Step.JOIN, model(unjoined(), ACADEMY, "", false).step());
        assertEquals(Step.WAIT_OPEN, model(joined(FormationVotePhase.NOT_STARTED, true, "",
                Map.of()), "", "", false).step());
        assertEquals(Step.VOTE, model(joined(FormationVotePhase.OPEN, true, "",
                tally(0, 0, 0)), "", "", false).step());
        // B11a: an administrator who has not voted may vote or lock straight away (vote-09).
        assertEquals(Step.ADMIN_VOTE, model(joined(FormationVotePhase.OPEN, true, "",
                tally(0, 0, 0)), "", "", true).step());
        assertEquals(Step.WAIT_LOCK, model(joined(FormationVotePhase.OPEN, true, MOBILE,
                tally(0, 1, 0)), "", "", false).step());
        assertEquals(Step.ADMIN_LOCK, model(joined(FormationVotePhase.OPEN, true, MOBILE,
                tally(0, 1, 0)), "", "", true).step());
        assertEquals(Step.DEPLOY, model(lockedJoined(), "", "", false).step());
    }

    // ---- user report 4: Esc never traps the player ---------------------------------------------------

    @Test
    void escapeClosesEvenBeforeJoiningAndGoesBackFromTheNarrowDetailPage() {
        assertEquals(FormationVoteModel.EscAction.CLOSE, FormationVoteModel.escAction(false));
        assertEquals(FormationVoteModel.EscAction.BACK_TO_LIST,
                FormationVoteModel.escAction(true));
    }

    // ---- voting rules -----------------------------------------------------------------------------

    @Test
    void openBallotOffersTheVoteKeyAndEnter() {
        FormationVoteModel model = model(joined(FormationVotePhase.OPEN, true, "",
                tally(2, 5, 1)), "", MOBILE, false);

        FormationVoteModel.VoteAction vote = model.voteAction();
        assertEquals(VoteLabel.VOTE, vote.label());
        assertTrue(vote.enabled());
        assertEquals(Reason.CHANGE_ALLOWED_AFTER, vote.reason());
        assertEquals(TacticalIcon.CHECK, vote.buttonIcon());
        assertTrue(model.enterVotes());
        assertEquals(8, model.totalVotes());
        assertEquals(63, model.percent(MOBILE));
        FormationVoteModel.RowStatus row = model.rowStatus(model.highlighted());
        assertTrue(row.showVotes());
        assertEquals(5, row.votes());
    }

    @Test
    void fullFactionMembersCanStillVote() {
        FormationSelectionSnapshot full = withFactions(joined(FormationVotePhase.OPEN, true, "",
                tally(0, 0, 0)), academy(40, 40, false, FormationVotePhase.OPEN, "", 40),
                caesar(21, true));
        FormationVoteModel model = model(full, "", DEFAULT, false);

        assertTrue(model.voteAction().enabled(), "vote-08: a full faction is full for joiners only");
    }

    @Test
    void formationSmallerThanTheFactionCannotBeVotedOrLocked() {
        FormationSelectionSnapshot snapshot = withFactions(joined(FormationVotePhase.OPEN, true,
                "", tally(0, 0, 0)), academy(18, 40, true, FormationVotePhase.OPEN, "", 12),
                caesar(21, true));
        FormationVoteModel model = model(snapshot, "", CAVALRY, true);

        assertTrue(model.shortfall(model.browsing(), model.highlighted()));
        assertEquals(RowMark.SHORTFALL, model.rowStatus(model.highlighted()).mark());
        assertEquals(Reason.SHORTFALL, model.voteAction().reason());
        assertFalse(model.voteAction().enabled());
        assertFalse(model.admin().lockEnabled());
        assertEquals(LockBlock.SHORTFALL, model.admin().lockBlock());
    }

    @Test
    void myVoteBecomesABadgeAndOtherRowsFollowTheChangeRule() {
        FormationVoteModel changeable = model(joined(FormationVotePhase.OPEN, true, MOBILE,
                tally(0, 1, 0)), "", MOBILE, false);
        assertTrue(changeable.voteAction().mine());
        assertEquals(Reason.MINE_CAN_CHANGE, changeable.voteAction().reason());
        assertTrue(changeable.rowStatus(changeable.highlighted()).mine());
        FormationVoteModel.VoteAction change = changeable.voteAction(
                changeable.browsing().formation(DEFAULT));
        assertEquals(VoteLabel.CHANGE, change.label());
        assertTrue(change.enabled());

        FormationVoteModel fixed = model(joined(FormationVotePhase.OPEN, false, MOBILE,
                tally(0, 1, 0)), "", DEFAULT, false);
        assertEquals(VoteLabel.CHANGE, fixed.voteAction().label());
        assertFalse(fixed.voteAction().enabled());
        assertEquals(Reason.NO_CHANGE, fixed.voteAction().reason());
        assertEquals(Reason.MINE_FIXED, fixed.voteAction(fixed.browsing().formation(MOBILE))
                .reason());
    }

    @Test
    void formationOutsideTheOpenBallotIsNotACandidate() {
        FormationVoteModel model = model(joined(FormationVotePhase.OPEN, true, "",
                Map.of(DEFAULT, 0, MOBILE, 0)), "", CAVALRY, false);

        assertEquals(Reason.NOT_CANDIDATE, model.voteAction().reason());
        assertFalse(model.enterVotes());
    }

    @Test
    void administratorLocksAnyCandidateWithoutVotingFirst() {
        FormationVoteModel model = model(joined(FormationVotePhase.OPEN, true, "",
                tally(1, 4, 0)), "", DEFAULT, true);

        FormationVoteModel.AdminState admin = model.admin();
        assertTrue(admin.visible());
        assertFalse(admin.opening());
        assertTrue(admin.lockEnabled(), "vote-09: no own vote needed");
        assertEquals(AdminRelation.NOT_LEADER, admin.relation());
        assertEquals(AdminRelation.SOLE_LEADER,
                model.relation(model.browsing().formation(MOBILE)));
    }

    @Test
    void tiesAreReportedInsteadOfPickingAWinner() {
        FormationVoteModel model = model(joined(FormationVotePhase.OPEN, true, "",
                tally(4, 4, 0)), "", DEFAULT, true);

        FormationVoteModel.Leaders leaders = model.leaders();
        assertTrue(leaders.tie());
        assertEquals(4, leaders.votes());
        assertEquals(List.of(DEFAULT, MOBILE), leaders.ids());
        assertEquals(AdminRelation.TIE_INCLUDED, model.admin().relation());
        assertEquals(AdminRelation.TIE_EXCLUDED,
                model.relation(model.browsing().formation(CAVALRY)));
        assertEquals(AdminRelation.NO_VOTES, model(joined(FormationVotePhase.OPEN, true, "",
                tally(0, 0, 0)), "", DEFAULT, true).admin().relation());
    }

    // ---- open confirmations under a newer catalog (review fix UI-05) ----------------------------

    @Test
    void anOpenLockConfirmationFollowsTheNewCatalogOrCloses() {
        FormationVoteModel.DialogFate keep = FormationVoteModel.DialogFate.KEEP;
        // The tally moved (the target became the leader): still lockable, the text is rewritten.
        assertEquals(keep, model(joined(FormationVotePhase.OPEN, true, "", tally(1, 4, 0)), "",
                DEFAULT, true).lockDialogFate(ACADEMY, DEFAULT));
        assertEquals(keep, model(joined(FormationVotePhase.OPEN, true, "", tally(5, 4, 0)), "",
                DEFAULT, true).lockDialogFate(ACADEMY, DEFAULT));
        // The faction grew past the target's capacity: no longer lockable.
        FormationSelectionSnapshot grown = withFactions(joined(FormationVotePhase.OPEN, true, "",
                tally(0, 0, 0)), academy(18, 40, true, FormationVotePhase.OPEN, "", 12),
                caesar(21, true));
        assertEquals(FormationVoteModel.DialogFate.CLOSE_WITH_NOTICE,
                model(grown, "", CAVALRY, true).lockDialogFate(ACADEMY, CAVALRY));
        // Another administrator locked the ballot meanwhile: the page shows the result.
        assertEquals(FormationVoteModel.DialogFate.CLOSE_SILENTLY,
                model(lockedJoined(), "", MOBILE, true).lockDialogFate(ACADEMY, MOBILE));
        // The highlight moved off the target (it left the catalog), or permission was lost.
        assertEquals(FormationVoteModel.DialogFate.CLOSE_WITH_NOTICE,
                model(joined(FormationVotePhase.OPEN, true, "", tally(1, 4, 0)), "", MOBILE,
                        true).lockDialogFate(ACADEMY, DEFAULT));
        assertEquals(FormationVoteModel.DialogFate.CLOSE_WITH_NOTICE,
                model(joined(FormationVotePhase.OPEN, true, "", tally(1, 4, 0)), "", DEFAULT,
                        false).lockDialogFate(ACADEMY, DEFAULT));
    }

    @Test
    void anOpenJoinConfirmationClosesWhenTheFactionFillsUp() {
        assertEquals(FormationVoteModel.DialogFate.KEEP,
                model(unjoined(), ACADEMY, "", false).joinDialogFate(ACADEMY));
        FormationSelectionSnapshot full = withFactions(unjoined(),
                academy(40, 40, false, FormationVotePhase.NOT_STARTED, "", 40), caesar(21, true));
        assertEquals(FormationVoteModel.DialogFate.CLOSE_WITH_NOTICE,
                model(full, ACADEMY, "", false).joinDialogFate(ACADEMY));
        assertEquals(FormationVoteModel.DialogFate.CLOSE_SILENTLY,
                model(joined(FormationVotePhase.NOT_STARTED, true, "", Map.of()), ACADEMY, "",
                        false).joinDialogFate(ACADEMY), "an administrator assigned the viewer");
    }

    // ---- locked ballots and late joiners (vote-01) ---------------------------------------------------

    @Test
    void lateJoinerSeesTheLockedFormationBeforeJoining() {
        FormationSelectionSnapshot snapshot = withFactions(unjoined(),
                academy(18, 40, true, FormationVotePhase.LOCKED, MOBILE, 40), caesar(21, true));
        FormationVoteModel model = model(snapshot, ACADEMY, "", false);

        assertEquals(MOBILE, model.highlighted().id(), "the locked result is shown first");
        assertEquals(Step.JOIN_LOCKED, model.step());
        assertTrue(model.joinAction().locked());
        assertEquals(VoteLabel.USE_AFTER_JOIN, model.voteAction().label());
        assertEquals(Reason.LATE_USE_THIS, model.voteAction().reason());
        assertEquals(RowMark.LOCKED, model.rowStatus(model.highlighted()).mark());
        FormationSelectionView other = model.browsing().formation(DEFAULT);
        assertEquals(RowMark.NOT_CHOSEN, model.rowStatus(other).mark());
        assertEquals(VoteLabel.NOT_CHOSEN, model.voteAction(other).label());
    }

    @Test
    void lockedFormationCapacityBoundsJoining() {
        FactionSelectionView locked = new FactionSelectionView(ACADEMY, "学院军", "", 18, 40,
                false, List.of(FormationVoteFixtures.formation(MOBILE, "机动部队", "mechanized",
                18, true, "")), FormationVotePhase.LOCKED, MOBILE);
        FormationVoteModel model = model(withFactions(unjoined(), locked, caesar(21, true)),
                ACADEMY, "", false);

        assertEquals(18, model.effectiveCapacity(locked));
        assertFalse(model.joinAction().enabled());
        assertEquals(JoinBlock.LOCKED_FULL, model.joinAction().block());
        assertEquals(Step.BROWSE_OTHER, model.step());
        assertEquals(VoteLabel.CANNOT_JOIN, model.voteAction().label());
    }

    @Test
    void lockedMemberDeploysAndTheAdministratorAreaIsGone() {
        FormationVoteModel model = model(lockedJoined(), "", "", true);

        assertEquals(Stage.LOCKED, model.stage());
        assertEquals(MOBILE, model.highlighted().id());
        assertEquals(VoteLabel.DEPLOY, model.voteAction().label());
        assertTrue(model.voteAction().enabled());
        assertEquals(TacticalIcon.DEPLOY, model.voteAction().buttonIcon());
        assertFalse(model.admin().visible());
        assertFalse(model.enterVotes());
    }

    @Test
    void lockedWithoutFormationTellsThePlayerToAskAnAdministrator() {
        FormationSelectionSnapshot snapshot = new FormationSelectionSnapshot(3L, true, ACADEMY,
                "", FormationVotePhase.LOCKED, true, "", MOBILE, tally(0, 3, 0), List.of(
                academy(40, 40, false, FormationVotePhase.LOCKED, MOBILE, 40), caesar(21, true)));
        FormationVoteModel model = model(snapshot, "", "", false);

        assertEquals(Step.LOCKED_NO_FORMATION, model.step());
        assertFalse(model.voteAction().enabled());
        assertEquals(Reason.NO_FORMATION, model.voteAction().reason());
    }

    // ---- empty and waiting --------------------------------------------------------------------------

    @Test
    void waitingAndEmptyCatalogsHaveTheirOwnStage() {
        FormationVoteModel waiting = model(null, "", "", false);
        assertEquals(Stage.WAITING, waiting.stage());
        assertEquals(Step.SYNC, waiting.step());
        assertNull(waiting.highlighted());

        FormationVoteModel empty = model(new FormationSelectionSnapshot(1L, true, "", "",
                List.of()), "", "", false);
        assertEquals(Stage.EMPTY, empty.stage());
        assertEquals(Step.EMPTY, empty.step());
    }

    @Test
    void noJoinableFactionIsSaidOutLoud() {
        FormationVoteModel model = model(withFactions(unjoined(),
                academy(40, 40, false, FormationVotePhase.NOT_STARTED, "", 40),
                new FactionSelectionView(CAESAR, "凯撒", "", 40, 40, false, List.of(
                        FormationVoteFixtures.formation("caesar_234_mechanized", "234",
                                "mechanized", 40, true, "")))), ACADEMY, "", false);

        assertEquals(Step.NO_JOINABLE, model.step());
        assertEquals(JoinBlock.FACTION_FULL, model.joinAction().block());
    }

    // ---- ordering, defaults and routing -----------------------------------------------------------

    @Test
    void highlightPrefersLockedThenOwnVoteThenFirstCandidate() {
        FormationSelectionSnapshot locked = lockedJoined();
        assertEquals(MOBILE, FormationVoteModel.initialHighlight(locked,
                locked.faction(ACADEMY)));
        FormationSelectionSnapshot voted = joined(FormationVotePhase.OPEN, true, CAVALRY,
                tally(0, 0, 1));
        assertEquals(CAVALRY, FormationVoteModel.initialHighlight(voted,
                voted.faction(ACADEMY)));
        FormationSelectionSnapshot fresh = unjoined();
        assertEquals(DEFAULT, FormationVoteModel.initialHighlight(fresh,
                fresh.faction(ACADEMY)));
    }

    @Test
    void formationsAreGroupedByCategoryOrder() {
        List<String> ids = FormationVoteModel.orderedFormations(
                unjoined().faction(ACADEMY)).stream().map(FormationSelectionView::id).toList();

        assertEquals(List.of(DEFAULT, CAVALRY, MOBILE), ids,
                "infantry, armored, mechanized (FormationCategory order)");
    }

    @Test
    void arrivingCatalogsRouteTheScreens() {
        FormationSelectionSnapshot locked = lockedJoined();
        assertEquals(Arrival.DEPLOYMENT, FormationVoteModel.arrival(locked, false, true,
                FormationSelectionScreen.Entry.SERVER, true));
        assertEquals(Arrival.NOTICE_ONLY, FormationVoteModel.arrival(locked, false, true,
                null, false));
        assertEquals(Arrival.SQUADS, FormationVoteModel.arrival(locked, true, false,
                FormationSelectionScreen.Entry.KEY, true));
        assertEquals(Arrival.CLOSE, FormationVoteModel.arrival(locked, false, false,
                FormationSelectionScreen.Entry.SERVER, true));
        assertEquals(Arrival.REPLACE, FormationVoteModel.arrival(locked, true, false,
                FormationSelectionScreen.Entry.TERMINAL, true),
                "the formation tab keeps showing the locked result");
        FormationSelectionSnapshot pending = unjoined();
        assertEquals(Arrival.REPLACE, FormationVoteModel.arrival(pending, true, false,
                FormationSelectionScreen.Entry.KEY, true));
        assertEquals(Arrival.OPEN, FormationVoteModel.arrival(pending, true, false, null,
                true));
        assertEquals(Arrival.IGNORE, FormationVoteModel.arrival(pending, false, false, null,
                true));
        assertEquals(Arrival.IGNORE, FormationVoteModel.arrival(locked, true, false, null,
                true));
    }

    @Test
    void theReplyToThePagesOwnCatalogRequestNeverReopensAClosedPage() {
        // Review fix UI-01: the page opens itself before asking; a late reply only refreshes it.
        assertFalse(com.wok.infantry.network.formation.packet.c2s.RequestFormationCatalogPacket
                .REPLY_OPENS_SCREEN);
        boolean reply = com.wok.infantry.network.formation.packet.c2s
                .RequestFormationCatalogPacket.REPLY_OPENS_SCREEN;
        for (FormationSelectionSnapshot pending : List.of(unjoined(),
                joined(FormationVotePhase.OPEN, true, "", tally(4, 4, 0)))) {
            assertEquals(Arrival.IGNORE, FormationVoteModel.arrival(pending, reply, false, null,
                    true), "closed with Esc before the reply arrived");
            assertEquals(Arrival.REPLACE, FormationVoteModel.arrival(pending, reply, false,
                    FormationSelectionScreen.Entry.KEY, true), "still open: shows the reply");
        }
    }

    private static FormationSelectionSnapshot lockedJoined() {
        return new FormationSelectionSnapshot(3L, false, ACADEMY, MOBILE,
                FormationVotePhase.LOCKED, true, MOBILE, MOBILE, tally(1, 3, 0), List.of(
                academy(18, 40, true, FormationVotePhase.LOCKED, MOBILE, 40), caesar(21, true)));
    }
}
