package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormationSelectionScreenTest {
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
