package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.FormationVoteModel.Reason;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import static com.wok.infantry.client.screen.FormationVoteFixtures.ACADEMY;
import static com.wok.infantry.client.screen.FormationVoteFixtures.DEFAULT;
import static com.wok.infantry.client.screen.FormationVoteFixtures.joined;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Wording choices of {@link FormationText} that depend on who is looking. */
class FormationTextTest {
    private static String keyOf(Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents, component::toString);
        return ((TranslatableContents) component.getContents()).getKey();
    }

    @Test
    void narrowPagesUpTo427WideUseTheShortTitleSoTheIdentityFits() {
        // 320×240 and 427×240 (854×480 at GUI 1) both get "编制投票"; the full
        // "WOK步战 // 编制投票" would make the shell hide "学院军 · 阿尔法小队 · 指挥官" at 427.
        for (int width : new int[] {320, 400, 427, FormationText.SHORT_TITLE_BELOW - 1}) {
            assertEquals(FormationText.PREFIX + "title_short", keyOf(FormationText.title(width)),
                    "width " + width);
        }
        // 480×270, 480×360 (960×720 at the minimum 2×), 640×336 and wider keep the full title.
        for (int width : new int[] {FormationText.SHORT_TITLE_BELOW, 480, 640, 960}) {
            assertEquals(FormationText.PREFIX + "title", keyOf(FormationText.title(width)),
                    "width " + width);
        }
    }

    // ---- step guide in the status bar (D2, 0.5.0-beta.3) --------------------------------------------

    private static final ToIntFunction<String> WIDTH = text -> text.length() * 6;
    private static final String TITLE = "TITLE > PAGE";
    private static final String IDENTITY = "Academy · Alpha · Leader";
    private static final List<String> GUIDE = List.of("Step 3: pick and vote", "Step 3: vote");

    /** The status bar of a {@code width}-wide display, planned like the shell does. */
    private static TacticalBoardChrome.StatusPlan status(int width, String guide) {
        return TacticalBoardChrome.planStatus(new UiRect(0, 0, width, 10), TITLE, guide, IDENTITY,
                "21:30", WIDTH);
    }

    @Test
    void theGuideKeepsTheWholeIdentityWhenTheStatusBarHasRoom() {
        int room = status(480, null).identityRoom();
        int index = FormationText.guideVariant(GUIDE, room, IDENTITY, WIDTH);
        assertEquals(0, index, "the long guide fits beside the whole identity");

        TacticalBoardChrome.StatusPlan plan = status(480, GUIDE.get(index));
        assertFalse(plan.pill().isEmpty());
        assertFalse(plan.feedback().truncated());
        assertEquals(IDENTITY, plan.identity().text());
    }

    @Test
    void onANarrowBarTheIdentityKeepsItsFactionNextToTheShortGuide() {
        int room = status(304, null).identityRoom();
        int index = FormationText.guideVariant(GUIDE, room, IDENTITY, WIDTH);
        assertEquals(1, index);

        TacticalBoardChrome.StatusPlan plan = status(304, GUIDE.get(index));
        assertFalse(plan.feedback().truncated(), "the guide is never cut");
        assertTrue(plan.identity().shown(), "the identity is never pushed out by the guide");
        assertTrue(IDENTITY.startsWith(plan.identity().text()));
    }

    @Test
    void theShortestGuideIsTheLastResortAndNoIdentityFreesTheRoom() {
        assertEquals(1, FormationText.guideVariant(GUIDE, 40, IDENTITY, WIDTH),
                "nothing fits: the shortest variant, which the shell ellipsizes");
        assertEquals(0, FormationText.guideVariant(GUIDE, 140, null, WIDTH));
        assertEquals(0, FormationText.guideVariant(GUIDE, 140, " ", WIDTH));
        assertEquals(-1, FormationText.guideVariant(List.of(), 140, IDENTITY, WIDTH));
    }

    @Test
    void theBezelKeysOfThePageUseTheirOwnTexts() {
        assertEquals(FormationText.PREFIX + "hint.retry", keyOf(FormationText.hintRetry()));
        assertEquals(FormationText.PREFIX + "hint.retry_unavailable",
                keyOf(FormationText.retryUnavailable()));
        assertEquals(FormationText.PREFIX + "hint.back_list",
                keyOf(FormationText.hintBackToList()));
    }

    @Test
    void anAdministratorIsNeverToldToWaitForAnAdministrator() {
        // Review fix UI-10: the administrator's own "open the vote" key is right on the page.
        FormationSelectionSnapshot pending = joined(FormationVotePhase.NOT_STARTED, true, "",
                Map.of());
        FormationVoteModel admin = FormationVoteModel.of(pending, ACADEMY, DEFAULT, true);
        FormationVoteModel player = FormationVoteModel.of(pending, ACADEMY, DEFAULT, false);
        assertEquals(Reason.WAIT_OPEN, admin.voteAction().reason());
        assertEquals(Reason.WAIT_OPEN, player.voteAction().reason());

        List<Component> adminReason = FormationText.reason(admin, admin.voteAction());
        assertEquals(FormationText.PREFIX + "reason.admin_open", keyOf(adminReason.get(0)));
        assertEquals(FormationText.PREFIX + "reason.admin_open_short",
                keyOf(adminReason.get(adminReason.size() - 1)));
        assertEquals(FormationText.PREFIX + "reason.wait_open",
                keyOf(FormationText.reason(player, player.voteAction()).get(0)));
    }
}
