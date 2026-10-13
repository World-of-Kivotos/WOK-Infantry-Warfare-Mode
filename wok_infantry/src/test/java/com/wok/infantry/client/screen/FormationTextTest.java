package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.FormationVoteModel.Reason;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.wok.infantry.client.screen.FormationVoteFixtures.ACADEMY;
import static com.wok.infantry.client.screen.FormationVoteFixtures.DEFAULT;
import static com.wok.infantry.client.screen.FormationVoteFixtures.joined;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
