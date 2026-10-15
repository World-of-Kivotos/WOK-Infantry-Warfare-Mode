package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.PermissionView;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.screen.TacticalLivery.Livery;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.wok.infantry.client.screen.FormationVoteFixtures.ACADEMY;
import static com.wok.infantry.client.screen.FormationVoteFixtures.CAESAR;
import static com.wok.infantry.client.screen.FormationVoteFixtures.CAVALRY;
import static com.wok.infantry.client.screen.FormationVoteFixtures.DEFAULT;
import static com.wok.infantry.client.screen.FormationVoteFixtures.MOBILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // ---- D2 tablet: livery and bezel keys (0.5.0-beta.3) --------------------------------------------

    @AfterEach
    void clearClientState() {
        ClientBattleState.clear();
        ClientFormationState.clear();
        TacticalLivery.reset();
        TacticalLivery.pinForAcceptance(null);
    }

    @Test
    void theResolverPaintsAViewerWithoutAFactionNeutral() {
        ClientFormationState.update(FormationVoteFixtures.unjoined());
        assertEquals(Livery.NEUTRAL, TacticalLivery.current(), "catalog without a faction");
        ClientBattleState.update(battle(null));
        assertEquals(Livery.NEUTRAL, TacticalLivery.current(), "battle snapshot without a side");

        ClientFormationState.update(FormationVoteFixtures.joined(FormationVotePhase.OPEN, true, "",
                Map.of()));
        ClientBattleState.update(battle(Faction.BLUE));
        assertEquals(Livery.ACADEMY, TacticalLivery.current(), "joined: the faction's side");
    }

    @Test
    void anUnjoinedPageIsNeutralAndAJoinedOneTakesItsFactionsSide() {
        Map<String, Faction> none = Map.of();
        assertEquals(Livery.NEUTRAL, FormationSelectionScreen.pageLivery(false, "",
                Livery.ACADEMY, none), "the join flow belongs to no side, whatever is cached");
        assertEquals(Livery.NEUTRAL, FormationSelectionScreen.pageLivery(false, null,
                Livery.CAESAR, none));
        assertEquals(Livery.CAESAR, FormationSelectionScreen.pageLivery(false, CAESAR,
                Livery.NEUTRAL, none), "a battle snapshot that has not caught up with the join");
        assertEquals(Livery.ACADEMY, FormationSelectionScreen.pageLivery(false, ACADEMY,
                null, none));
        assertEquals(Livery.CAESAR, FormationSelectionScreen.pageLivery(false, ACADEMY,
                Livery.CAESAR, none), "the battle side (or an acceptance pin) wins");
        assertEquals(Livery.CAESAR, FormationSelectionScreen.pageLivery(false, "gehenna",
                Livery.NEUTRAL, Map.of("gehenna", Faction.RED)), "renamed factions are learned");
        assertEquals(Livery.NEUTRAL, FormationSelectionScreen.pageLivery(false, "gehenna",
                Livery.NEUTRAL, none), "an unknown faction stays neutral");
    }

    @Test
    void whileWaitingForTheCatalogTheViewersLiveryDecides() {
        assertEquals(Livery.ACADEMY, FormationSelectionScreen.pageLivery(true, "",
                Livery.ACADEMY, Map.of()), "a participant reopening the page from the terminal");
        assertEquals(Livery.NEUTRAL, FormationSelectionScreen.pageLivery(true, "", null,
                Map.of()));
    }

    @Test
    void theEscKeySaysWhatEscDoesOnThisPage() {
        Component terminal = Component.literal("～");
        TacticalBoardChrome.KeyHint joined =
                FormationSelectionScreen.escHint(false, true, terminal);
        assertEquals("Esc", joined.key().getString());
        assertTrue(TacticalBezelPlan.isEscape(joined), "the bezel puts it at the left end");
        assertEquals(FormationText.PREFIX + "hint.close", keyOf(joined.action()));
        assertEquals(FormationText.PREFIX + "hint.close_reopen",
                keyOf(FormationSelectionScreen.escHint(false, false, terminal).action()),
                "before joining Esc closes and says which key reopens the page (user report 4)");
        assertEquals(FormationText.PREFIX + "hint.close_unbound",
                keyOf(FormationSelectionScreen.escHint(false, false, null).action()));
        for (boolean member : new boolean[]{false, true}) {
            assertEquals(FormationText.PREFIX + "hint.back_list",
                    keyOf(FormationSelectionScreen.escHint(true, member, terminal).action()),
                    "the narrow detail page goes back to the list");
        }
    }

    @Test
    void theRefreshKeyOnlyRetriesWhileWaitingForTheCatalog() {
        assertNull(FormationSelectionScreen.retryDisabledReason(true));
        assertEquals(FormationText.PREFIX + "hint.retry_unavailable",
                keyOf(FormationSelectionScreen.retryDisabledReason(false)));
    }

    private static String keyOf(Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents, component::toString);
        return ((TranslatableContents) component.getContents()).getKey();
    }

    private static BattleSnapshot battle(Faction side) {
        return new BattleSnapshot(new UUID(0L, 1L), side, null, false, false, 1, 0,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY, List.of(), List.of(),
                List.of(), new PermissionView(false, false, false, false, false, false, false),
                List.of(), 1_000L, 1L);
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
