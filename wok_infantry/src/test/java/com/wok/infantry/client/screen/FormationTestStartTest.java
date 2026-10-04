package com.wok.infantry.client.screen;

import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.vote.FormationVotePhase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.wok.infantry.client.screen.FormationVoteFixtures.ACADEMY;
import static com.wok.infantry.client.screen.FormationVoteFixtures.CAESAR;
import static com.wok.infantry.client.screen.FormationVoteFixtures.CAESAR_234;
import static com.wok.infantry.client.screen.FormationVoteFixtures.CAVALRY;
import static com.wok.infantry.client.screen.FormationVoteFixtures.DEFAULT;
import static com.wok.infantry.client.screen.FormationVoteFixtures.MOBILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The administrator test-start key of the formation page (0.4.0-beta.2). */
class FormationTestStartTest {
    private static final int LIST_NEED = 2 * 13 + 4 * 18 + 2;
    private static final int TEST_KEY_WIDTH = 66;

    // ---- model --------------------------------------------------------------------------------

    @Test
    void onlyAnAdministratorWithACatalogSeesTheKey() {
        FormationSelectionSnapshot unjoined = FormationVoteFixtures.unjoined();
        assertFalse(FormationVoteModel.of(unjoined, ACADEMY, DEFAULT, false).testStart()
                .visible(), "players never see the test start");
        assertFalse(FormationVoteModel.of(null, "", "", true).testStart().visible(),
                "nothing to start while the catalog is on its way");
        FormationVoteModel.TestStart admin = FormationVoteModel.of(unjoined, ACADEMY, MOBILE,
                true).testStart();
        assertTrue(admin.visible() && admin.enabled(),
                "an administrator in the black waiting space can start a test before joining");
        assertEquals(ACADEMY, admin.factionId());
        assertEquals(MOBILE, admin.formationId(), "the highlighted formation is used");
        assertFalse(admin.locked());
    }

    @Test
    void theBrowsedFactionIsUsedBeforeJoiningAndTheOwnOneAfterwards() {
        FormationVoteModel.TestStart other = FormationVoteModel.of(
                FormationVoteFixtures.unjoined(), CAESAR, CAESAR_234, true).testStart();
        assertEquals(CAESAR, other.factionId());
        assertEquals(CAESAR_234, other.formationId());

        FormationVoteModel.TestStart joined = FormationVoteModel.of(
                FormationVoteFixtures.joined(FormationVotePhase.OPEN, true, "", Map.of()),
                CAESAR, CAVALRY, true).testStart();
        assertEquals(ACADEMY, joined.factionId(), "after joining the own faction is shown");
        assertEquals(CAVALRY, joined.formationId());
        assertTrue(joined.enabled());
    }

    @Test
    void aLockedFactionKeepsItsLockedFormation() {
        FormationSelectionSnapshot late = FormationVoteFixtures.withFactions(
                FormationVoteFixtures.unjoined(),
                FormationVoteFixtures.academy(18, 40, true, FormationVotePhase.LOCKED, MOBILE, 40),
                FormationVoteFixtures.caesar(21, true));
        FormationVoteModel.TestStart lockedOther = FormationVoteModel.of(late, ACADEMY, DEFAULT,
                true).testStart();
        assertTrue(lockedOther.enabled() && lockedOther.locked());
        assertEquals(MOBILE, lockedOther.formationId(),
                "the command names the locked formation, never another one");
        assertTrue(lockedOther.lockKept(), "another formation was highlighted");

        FormationSelectionSnapshot locked = new FormationSelectionSnapshot(3L, false, ACADEMY,
                MOBILE, FormationVotePhase.LOCKED, false, MOBILE, MOBILE, Map.of(), List.of(
                FormationVoteFixtures.academy(18, 40, true, FormationVotePhase.LOCKED, MOBILE, 40),
                FormationVoteFixtures.caesar(21, true)));
        FormationVoteModel.TestStart own = FormationVoteModel.of(locked, "", MOBILE, true)
                .testStart();
        assertTrue(own.visible() && own.enabled(),
                "after the lock the key still deploys a player waiting in the void");
        assertFalse(own.lockKept());
    }

    @Test
    void unusableTargetsDisableTheKeyWithAReason() {
        FormationSelectionSnapshot unavailable = FormationVoteFixtures.withFactions(
                FormationVoteFixtures.unjoined(),
                new com.wok.infantry.formation.selection.FactionSelectionView(ACADEMY, "学院军",
                        "", 18, 40, true, List.of(
                        FormationVoteFixtures.formation(DEFAULT, "常规编制", "infantry", 40, true,
                                ""),
                        FormationVoteFixtures.formation(MOBILE, "机动部队", "mechanized", 40,
                                false, "缺少卓越前线 MOD")),
                        FormationVotePhase.NOT_STARTED, ""),
                FormationVoteFixtures.caesar(21, true));
        FormationVoteModel.TestStart missingMod = FormationVoteModel.of(unavailable, ACADEMY,
                MOBILE, true).testStart();
        assertTrue(missingMod.visible());
        assertEquals(FormationVoteModel.TestBlock.UNAVAILABLE, missingMod.block());
        assertFalse(missingMod.enabled());

        FormationSelectionSnapshot small = FormationVoteFixtures.withFactions(
                FormationVoteFixtures.joined(FormationVotePhase.OPEN, true, "", Map.of()),
                FormationVoteFixtures.academy(18, 40, true, FormationVotePhase.OPEN, "", 12),
                FormationVoteFixtures.caesar(21, true));
        FormationVoteModel.TestStart shortfall = FormationVoteModel.of(small, "", CAVALRY, true)
                .testStart();
        assertEquals(FormationVoteModel.TestBlock.SHORTFALL, shortfall.block(),
                "a formation smaller than the faction could not be locked");
    }

    // ---- page state and command ---------------------------------------------------------------

    @Test
    void theOpenTestConfirmationReportsItsOwnState() {
        FormationVoteModel unjoined = FormationVoteModel.of(FormationVoteFixtures.unjoined(),
                ACADEMY, DEFAULT, true);
        assertEquals("testconfirm", FormationSelectionScreen.uiStateId(unjoined, false, false,
                true));
        assertEquals("confirm", FormationSelectionScreen.uiStateId(unjoined, false, false,
                false), "the join confirmation keeps its state");
        assertEquals("join", FormationSelectionScreen.uiStateId(unjoined, false, null, false));
        assertEquals("waiting", FormationSelectionScreen.uiStateId(
                FormationVoteModel.of(null, "", "", true), false, false, true));
    }

    @Test
    void testStartKeySendsTheFactionAndFormationIds() {
        assertEquals("battle admin test start academy millennium_seminar_mobile",
                FormationSelectionScreen.administratorTestStartCommand(ACADEMY, MOBILE));
    }

    // ---- layout -------------------------------------------------------------------------------

    /**
     * Acceptance sizes (320×240, 427×240, 640×336, 960×720 laid out at 2× = 480×360, plus
     * 480×270 and a raw 960×720): beside the vote key the test key takes the right part of the
     * administrator row; without vote keys a compact area holds it alone. Either way it stays in
     * the list panel, below the formation well and above the narrow action bar.
     */
    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "427, 240", "480, 270", "640, 336", "480, 360", "960, 720"})
    void testKeyFitsBesideTheVoteKeyOrInItsCompactArea(int width, int height) {
        for (boolean admin : new boolean[]{false, true}) {
            FormationScreenLayout layout = FormationScreenLayout.compute(width, height, 2,
                    admin, admin, false, false, LIST_NEED, admin ? 0 : 150, TEST_KEY_WIDTH);
            String label = width + "x" + height + " admin=" + admin;
            TacticalShellLayout.Metrics m = layout.shell().metrics();
            UiRect test = layout.testKey();
            assertFalse(test.isEmpty(), label + ": no test key");
            assertEquals(m.buttonHeight(), test.height(), label);
            assertTrue(layout.admin().contains(test), label + ": " + test + " outside "
                    + layout.admin());
            assertTrue(layout.listPanel().contains(layout.admin()), label);
            assertTrue(layout.well().bottom() <= layout.admin().top(), label
                    + ": the well runs into the administrator area");
            if (!layout.actionBar().isEmpty()) {
                assertTrue(layout.admin().bottom() <= layout.actionBar().top(), label
                        + ": the administrator area runs into the action bar");
            }
            if (admin) {
                UiRect vote = layout.adminKey();
                assertFalse(vote.isEmpty(), label + ": the vote key lost its place");
                assertFalse(vote.intersects(test), label + ": " + vote + " overlaps " + test);
                assertEquals(vote.top(), test.top(), label + ": one key row");
                assertTrue(test.width() <= layout.admin().width()
                        * FormationScreenLayout.TEST_KEY_SHARE_PERCENT / 100, label);
                assertTrue(vote.width() >= test.width(), label
                        + ": the vote key keeps the larger part");
                assertEquals(FormationScreenLayout.adminHeight(m), layout.admin().height());
            } else {
                assertTrue(layout.adminKey().isEmpty(), label);
                assertEquals(layout.admin().width(), test.width(), label
                        + ": alone the key takes the whole row");
                assertEquals(FormationScreenLayout.adminCompactHeight(m),
                        layout.admin().height(), label);
            }
        }
    }

    @Test
    void withoutTestKeyTheLayoutIsUnchanged() {
        FormationScreenLayout before = FormationScreenLayout.compute(640, 336, 2, true, true,
                false, false, LIST_NEED, 0);
        FormationScreenLayout zero = FormationScreenLayout.compute(640, 336, 2, true, true,
                false, false, LIST_NEED, 0, 0);
        assertEquals(before, zero, "players see exactly the 0.4.0-beta.1 layout");
        assertTrue(zero.testKey().isEmpty());
        FormationScreenLayout player = FormationScreenLayout.compute(320, 240, 2, false, false,
                false, false, LIST_NEED, 150, 0);
        assertTrue(player.admin().isEmpty() && player.testKey().isEmpty(),
                "no administrator area without the test key");
    }
}
