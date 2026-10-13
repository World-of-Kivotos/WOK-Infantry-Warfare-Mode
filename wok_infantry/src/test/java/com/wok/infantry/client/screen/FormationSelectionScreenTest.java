package com.wok.infantry.client.screen;

import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.vote.FormationVotePhase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.wok.infantry.client.screen.FormationVoteFixtures.ACADEMY;
import static com.wok.infantry.client.screen.FormationVoteFixtures.CAVALRY;
import static com.wok.infantry.client.screen.FormationVoteFixtures.DEFAULT;
import static com.wok.infantry.client.screen.FormationVoteFixtures.MOBILE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FormationSelectionScreenTest {
    private static String state(FormationSelectionSnapshot snapshot, String browse,
                                String highlight, boolean administrator) {
        return state(snapshot, browse, highlight, administrator, false, null);
    }

    private static String state(FormationSelectionSnapshot snapshot, String browse,
                                String highlight, boolean administrator, boolean detailPage,
                                Boolean dangerDialog) {
        return FormationSelectionScreen.uiStateId(FormationVoteModel.of(snapshot, browse,
                highlight, administrator), detailPage, dangerDialog);
    }

    @Test
    void reportsThePreviewStatesBeforeJoining() {
        FormationSelectionSnapshot unjoined = FormationVoteFixtures.unjoined();
        assertEquals("waiting", state(null, "", "", false));
        assertEquals("join", state(unjoined, ACADEMY, "", false));
        assertEquals("confirm", state(unjoined, ACADEMY, "", false, false, false));
        FormationSelectionSnapshot full = FormationVoteFixtures.withFactions(unjoined,
                FormationVoteFixtures.academy(40, 40, false, FormationVotePhase.NOT_STARTED, "",
                        40), FormationVoteFixtures.caesar(21, true));
        assertEquals("facfull", state(full, ACADEMY, "", false));
        FormationSelectionSnapshot late = FormationVoteFixtures.withFactions(unjoined,
                FormationVoteFixtures.academy(18, 40, true, FormationVotePhase.LOCKED, MOBILE, 40),
                FormationVoteFixtures.caesar(21, true));
        assertEquals("latejoin", state(late, ACADEMY, "", false));
        assertEquals("lateconfirm", state(late, ACADEMY, "", false, false, false));
    }

    @Test
    void reportsThePreviewStatesOfTheBallot() {
        assertEquals("pending", state(FormationVoteFixtures.joined(
                FormationVotePhase.NOT_STARTED, true, "", Map.of()), "", "", true));
        Map<String, Integer> tie = FormationVoteFixtures.tally(4, 4, 0);
        assertEquals("vote", state(FormationVoteFixtures.joined(FormationVotePhase.OPEN, true, "",
                tie), "", "", false));
        assertEquals("admintie", state(FormationVoteFixtures.joined(FormationVotePhase.OPEN, true,
                "", tie), "", DEFAULT, true));
        Map<String, Integer> lead = FormationVoteFixtures.tally(3, 5, 1);
        assertEquals("detail", state(FormationVoteFixtures.joined(FormationVotePhase.OPEN, true,
                MOBILE, lead), "", "", false));
        assertEquals("detail", state(FormationVoteFixtures.joined(FormationVotePhase.OPEN, true,
                "", lead), "", "", false, true, null));
        assertEquals("admin", state(FormationVoteFixtures.joined(FormationVotePhase.OPEN, true,
                "", lead), "", DEFAULT, true, false, true));
        FormationSelectionSnapshot small = FormationVoteFixtures.withFactions(
                FormationVoteFixtures.joined(FormationVotePhase.OPEN, true, MOBILE, lead),
                FormationVoteFixtures.academy(18, 40, true, FormationVotePhase.OPEN, "", 12),
                FormationVoteFixtures.caesar(21, true));
        assertEquals("full", state(small, "", CAVALRY, true));
        FormationSelectionSnapshot locked = new FormationSelectionSnapshot(3L, false, ACADEMY,
                MOBILE, FormationVotePhase.LOCKED, false, MOBILE, MOBILE, Map.of(), List.of(
                FormationVoteFixtures.academy(18, 40, true, FormationVotePhase.LOCKED, MOBILE, 40),
                FormationVoteFixtures.caesar(21, true)));
        assertEquals("locked", state(locked, "", "", false));
    }

    @Test
    void administratorOpenCommandStartsChangeableFactionVote() {
        assertEquals("battle admin formation vote open academy true",
                FormationSelectionScreen.administratorOpenVoteCommand("academy"));
    }

    @Test
    void administratorLockCommandUsesPublicFactionAndConcreteFormationIds() {
        assertEquals("battle admin formation vote lock academy millennium_seminar_mobile",
                FormationSelectionScreen.administratorLockCommand(
                        "academy", "millennium_seminar_mobile"));
    }
}
