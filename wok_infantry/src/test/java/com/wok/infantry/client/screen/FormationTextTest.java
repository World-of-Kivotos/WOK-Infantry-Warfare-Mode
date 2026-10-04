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
