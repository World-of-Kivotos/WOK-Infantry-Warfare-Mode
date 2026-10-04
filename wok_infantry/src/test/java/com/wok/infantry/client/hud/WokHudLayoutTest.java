package com.wok.infantry.client.hud;

import com.wok.infantry.client.hud.WokHudLayout.Input;
import com.wok.infantry.client.hud.WokHudLayout.Layout;
import com.wok.infantry.client.hud.WokHudLayout.RosterPresence;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.config.InfantryClientConfig.HudRosterMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WokHudLayoutTest {
    /** The seven layout tiers of the acceptance plan, plus 960×720 at GUI scale 1 with the 2× HUD. */
    private static final List<int[]> TIERS = List.of(
            new int[]{320, 240, 1}, new int[]{427, 240, 1}, new int[]{480, 270, 1},
            new int[]{640, 336, 1}, new int[]{640, 360, 1}, new int[]{960, 540, 1},
            new int[]{960, 720, 1}, new int[]{960, 720, 2});

    @Test
    void narrowScreenMatchesTheAcceptedGeometry() {
        Layout layout = WokHudLayout.compute(Input.screen(320, 240, 1).withRoster(8, false)
                .withStrip(true).withStamina(true));
        assertTrue(layout.tight());
        assertTrue(layout.narrow());
        assertEquals(UiRect.of(2, 2, 118, 95), layout.roster(), "8 rows end at y95 (chat starts at 110)");
        assertEquals(UiRect.of(123, 2, 318, 19), layout.strip(), "strip docks right of the roster");
        assertEquals(UiRect.of(2, 211, 65, 238), layout.staminaPlate());
        assertEquals(UiRect.of(2, 203, 65, 238), layout.vitals());

        Layout collapsed = WokHudLayout.compute(Input.screen(320, 240, 1).withRoster(8, true));
        assertEquals(UiRect.of(2, 2, 118, 13), collapsed.roster(), "title row only");
    }

    @Test
    void largeScreenMatchesTheAcceptedGeometry() {
        Layout layout = WokHudLayout.compute(Input.screen(960, 720, 1).withRoster(8, false)
                .withStrip(true).withStamina(true));
        assertFalse(layout.tight());
        assertEquals(UiRect.of(4, 4, 176, 115), layout.roster());
        assertEquals(UiRect.of(330, 4, 630, 21), layout.strip(), "300px strip centred");
        assertEquals(UiRect.of(4, 689, 114, 716), layout.staminaPlate());
    }

    @Test
    void guiScaleOneLaysOutAtHalfSize() {
        Layout layout = WokHudLayout.compute(Input.screen(960, 720, 2).withRoster(8, false)
                .withStrip(true).withStamina(true));
        assertEquals(480, layout.width());
        assertEquals(360, layout.height());
        assertEquals(UiRect.of(4, 4, 176, 115), layout.roster());
        assertEquals(UiRect.of(184, 4, 476, 21), layout.strip(), "too narrow to centre: docked");
        assertTrue(layout.staminaRow(), "only one row fits under the 1x chat");
        assertEquals(UiRect.of(4, 341, 114, 356), layout.staminaPlate());
        assertEquals(UiRect.of(8, 682, 228, 712), layout.toGui(layout.staminaPlate()),
                "2 GUI pixels under the chat's last line at 680");
    }

    @Test
    void vitalsNeverReachIntoTheVanillaChat() {
        for (int[] tier : TIERS) {
            Layout layout = WokHudLayout.compute(Input.screen(tier[0], tier[1], tier[2])
                    .withStamina(true));
            String where = tier[0] + "x" + tier[1] + "@" + tier[2];
            assertTrue(layout.toGui(layout.vitals()).top() >= tier[1] - 40 + 2, where);
            assertEquals(tier[2] == 2, layout.staminaRow(), where + ": stacked plate at 1x");
            assertEquals(layout.staminaRow() ? 15 : 27, layout.staminaPlate().height(), where);
        }
        Layout scaled = WokHudLayout.compute(Input.screen(1920, 1080, 2).withStamina(true));
        assertTrue(scaled.staminaRow());
        assertEquals(UiRect.of(8, 1042, 228, 1072), scaled.toGui(scaled.staminaPlate()));
    }

    @Test
    void stripMovesBelowEffectIconsThatAnotherModShiftedIntoIt() {
        // JourneyMap moves the effect icons 75px left, clear of its minimap
        Input shifted = Input.screen(320, 240, 1).withRoster(8, false).withStrip(true)
                .withToasts(List.of(100)).withEffects(3, 2).withEffectOffset(-75, 0);
        Layout layout = WokHudLayout.compute(shifted);
        List<UiRect> rows = WokHudLayout.effectRects(shifted);
        assertEquals(UiRect.of(170, 1, 244, 25), rows.get(0));
        assertTrue(layout.strip().top() >= 51 + layout.gap(), "below both icon rows: "
                + layout.strip());
        assertEquals(195, layout.strip().width(), "keeps its full width");
        assertSound(shifted, layout, "shifted icons");

        Input wide = Input.screen(960, 720, 2).withStrip(true).withEffects(3, 2)
                .withEffectOffset(-75, 0);
        Layout scaled = WokHudLayout.compute(wide);
        assertSound(wide, scaled, "shifted icons at 2x");
        Input hidden = Input.screen(320, 240, 1).withStrip(true).withEffects(0, 0)
                .withEffectOffset(-75, 0);
        assertEquals(2, WokHudLayout.compute(hidden).strip().top(), "no icons, nothing moves");
    }

    @Test
    void everyTierKeepsCorePlatesApartOnScreenAndClearOfVanillaParts() {
        for (int[] tier : TIERS) {
            for (boolean capture : new boolean[]{false, true}) {
                for (boolean offhand : new boolean[]{false, true}) {
                    for (boolean collapsed : new boolean[]{false, true}) {
                        for (int effects : new int[]{0, 3}) {
                            Input input = Input.screen(tier[0], tier[1], tier[2])
                                    .withRoster(8, collapsed).withStrip(true)
                                    .withToasts(List.of(150, 60)).withStamina(true)
                                    .withHotbarNeighbours(offhand, !offhand)
                                    .withEffects(effects, effects == 0 ? 0 : 2)
                                    .withCapturePanel(capture
                                            ? CaptureHudBridge.mirroredPanel(tier[0]) : null);
                            assertSound(input, WokHudLayout.compute(input),
                                    tier[0] + "x" + tier[1] + "@" + tier[2] + " capture=" + capture
                                            + " offhand=" + offhand + " collapsed=" + collapsed
                                            + " effects=" + effects);
                        }
                    }
                }
            }
        }
    }

    @Test
    void everyTierKeepsTheBallotApartFromARosterAndOnScreen() {
        for (int[] tier : TIERS) {
            for (boolean meter : new boolean[]{false, true}) {
                Input input = Input.screen(tier[0], tier[1], tier[2]).withRoster(3, false)
                        .withStrip(true).withToasts(List.of(80)).withVote(400, meter)
                        .withEffects(2, 1);
                Layout layout = WokHudLayout.compute(input);
                String where = tier[0] + "x" + tier[1] + "@" + tier[2];
                assertNull(layout.strip(), where + ": the ballot replaces the strip");
                assertTrue(layout.toasts().isEmpty(), where + ": and its notices");
                assertEquals(meter ? 32 : 27, layout.vote().height(), where);
                assertTrue(layout.vote().width() <= (layout.tight() ? 220 : 300), where);
                assertSound(input, layout, where);
            }
        }
    }

    @Test
    void ballotIsCentredAndCappedPerTier() {
        Layout narrow = WokHudLayout.compute(Input.screen(320, 240, 1).withVote(400, false));
        assertEquals(UiRect.of(50, 2, 270, 29), narrow.vote());
        Layout wide = WokHudLayout.compute(Input.screen(960, 720, 1).withVote(250, true));
        assertEquals(UiRect.of(355, 4, 605, 36), wide.vote());
        Layout docked = WokHudLayout.compute(Input.screen(320, 240, 1).withRoster(2, false)
                .withVote(400, false));
        assertEquals(UiRect.of(123, 2, 318, 29), docked.vote(), "never under a roster");
    }

    @Test
    void capturePanelKeepsItsAlphaThreePremiseOnNarrowScreens() {
        for (int width = 320; width <= 360; width++) {
            UiRect panel = CaptureHudBridge.mirroredPanel(width);
            Layout layout = WokHudLayout.compute(Input.screen(width, 240, 1).withRoster(8, false)
                    .withStrip(true).withToasts(List.of(120)).withCapturePanel(panel));
            assertEquals(2, layout.roster().top(), "roster stays at the top at width " + width);
            assertTrue(layout.roster().right() <= 136, "roster right edge at width " + width);
            assertTrue(layout.strip().bottom() <= 24, "strip above the panel at width " + width);
            assertTrue(layout.toasts().get(0).top() >= 64, "notice under the panel at width " + width);
        }
    }

    @Test
    void rosterMovesUnderTheWideCapturePanel() {
        assertEquals(83, rosterTopWithCapture(427, 240));
        assertEquals(83, rosterTopWithCapture(480, 270));
        assertEquals(84, rosterTopWithCapture(640, 360));
        assertEquals(84, rosterTopWithCapture(640, 336));
        assertEquals(4, rosterTopWithCapture(960, 720), "the wide panel is clear of the roster");
        for (int[] size : new int[][]{{427, 240}, {480, 270}, {640, 360}}) {
            Layout layout = WokHudLayout.compute(Input.screen(size[0], size[1], 1)
                    .withRoster(8, false).withStrip(true)
                    .withCapturePanel(CaptureHudBridge.mirroredPanel(size[0])));
            assertEquals(WokHudLayout.STRIP_HEIGHT, layout.strip().height(), "narrow strip");
            assertTrue(layout.strip().bottom() <= 24, "strip above the panel");
        }
    }

    @Test
    void stripAndNoticesGiveWayToStatusEffectIcons() {
        Layout layout = WokHudLayout.compute(Input.screen(320, 240, 1).withRoster(8, false)
                .withStrip(true).withToasts(List.of(190)).withEffects(3, 0));
        // three beneficial icons start at x = 320 − 75 = 245
        assertTrue(layout.strip().right() <= 245 - layout.gap(), layout.strip().toString());
        assertTrue(layout.strip().left() >= 118 + 5, "still clear of the roster");
        Layout wide = WokHudLayout.compute(Input.screen(960, 720, 2).withStrip(true)
                .withEffects(3, 0));
        assertEquals(UiRect.of(184, 4, 438, 21), wide.strip());
    }

    @Test
    void bossBarsMoveBelowTheTopCentrePlates() {
        Layout narrow = WokHudLayout.compute(Input.screen(320, 240, 1).withStrip(true));
        assertEquals(19, narrow.topCenterBottom());
        assertEquals(19 + 4 - 3, narrow.bossShift(), "first boss title row lands 4px under the strip");
        Layout scaled = WokHudLayout.compute(Input.screen(960, 720, 2).withStrip(true)
                .withToasts(List.of(50)));
        assertEquals(21 + 5 + 12, scaled.topCenterBottom());
        assertEquals(38 * 2 + 1, scaled.bossShift(), "GUI pixels at the 2x HUD");
        Layout none = WokHudLayout.compute(Input.screen(960, 720, 1));
        assertEquals(0, none.topCenterBottom());
        assertEquals(0, none.bossShift());
    }

    @Test
    void vitalsStayLeftOfHotbarOffhandAndAttackIndicator() {
        for (int width = 320; width <= 1920; width++) {
            for (int factor = 1; factor <= 2; factor++) {
                for (int neighbours = 0; neighbours < 4; neighbours++) {
                    Input input = Input.screen(width, 480, factor).withStamina(true)
                            .withHotbarNeighbours((neighbours & 1) != 0, (neighbours & 2) != 0);
                    Layout layout = WokHudLayout.compute(input);
                    int limit = Math.floorDiv(WokHudLayout.hotbarObstacleLeft(input), factor) - 4;
                    String where = width + "@" + factor + " neighbours=" + neighbours;
                    assertTrue(layout.staminaPlate().right() <= limit, where);
                    assertTrue(layout.vitals().right() <= limit, where);
                    assertTrue(layout.toGui(layout.staminaPlate()).right()
                            <= WokHudLayout.hotbarObstacleLeft(input) - 4, where);
                }
            }
        }
        Input offhand = Input.screen(320, 240, 1).withStamina(true).withHotbarNeighbours(true, false);
        assertEquals(160 - 124, WokHudLayout.compute(offhand).staminaPlate().right(),
                "off-hand slot starts at w/2 − 120");
        Input indicator = Input.screen(320, 240, 1).withStamina(true).withHotbarNeighbours(false, true);
        assertEquals(160 - 117, WokHudLayout.compute(indicator).staminaPlate().right(),
                "attack indicator starts at w/2 − 113");
    }

    @Test
    void rosterCollapseRules() {
        assertEquals(RosterPresence.COLLAPSED, WokHudLayout.rosterPresence(HudRosterMode.AUTO,
                true, true, false, false, false), "tight screen with chat open");
        assertEquals(RosterPresence.FULL, WokHudLayout.rosterPresence(HudRosterMode.AUTO,
                false, true, false, false, false), "roomy screens keep the roster with chat open");
        assertEquals(RosterPresence.COLLAPSED, WokHudLayout.rosterPresence(HudRosterMode.AUTO,
                false, false, true, false, false), "F3");
        assertEquals(RosterPresence.COLLAPSED, WokHudLayout.rosterPresence(HudRosterMode.AUTO,
                false, false, false, true, false), "player list held");
        assertEquals(RosterPresence.FULL, WokHudLayout.rosterPresence(HudRosterMode.AUTO,
                true, false, false, false, false));
        assertEquals(RosterPresence.HIDDEN, WokHudLayout.rosterPresence(HudRosterMode.AUTO,
                false, false, false, false, true), "spectators see no roster");
        assertEquals(RosterPresence.FULL, WokHudLayout.rosterPresence(HudRosterMode.FULL,
                true, true, true, true, false));
        assertEquals(RosterPresence.COLLAPSED, WokHudLayout.rosterPresence(
                HudRosterMode.COLLAPSED, false, false, false, false, false));
        assertEquals(RosterPresence.HIDDEN, WokHudLayout.rosterPresence(HudRosterMode.HIDDEN,
                false, false, false, false, false));
        assertEquals(RosterPresence.HIDDEN, WokHudLayout.rosterPresence(HudRosterMode.FULL,
                false, false, false, false, true));
        assertEquals(RosterPresence.FULL, WokHudLayout.rosterPresence(null,
                false, false, false, false, false), "unknown mode behaves like AUTO");
    }

    @Test
    void rowAndHeaderHeightsFollowTheTier() {
        assertEquals(11 + 8 * 10 + 2, WokHudLayout.rosterHeight(8, true, false));
        assertEquals(13 + 8 * 12 + 2, WokHudLayout.rosterHeight(8, false, false));
        assertEquals(13, WokHudLayout.rosterHeight(8, false, true));
        assertEquals(11 + 8 * 10 + 2, WokHudLayout.rosterHeight(20, true, false), "at most 8 rows");
        assertEquals(116, WokHudLayout.rosterWidth(true));
        assertEquals(172, WokHudLayout.rosterWidth(false));
        assertTrue(WokHudLayout.isTight(480, 270), "480×270 is tight by height");
        assertFalse(WokHudLayout.isTight(640, 336));
    }

    @Test
    void centreLowSlotSitsUnderTheCrosshair() {
        Layout narrow = WokHudLayout.compute(Input.screen(320, 240, 1));
        assertEquals(UiRect.of(60, 136, 260, 184), narrow.centerLow());
        Layout tiny = WokHudLayout.compute(Input.screen(200, 240, 1));
        assertEquals(200 - 2 * 2 - 20, tiny.centerLow().width(), "never wider than the screen allows");
    }

    private static int rosterTopWithCapture(int width, int height) {
        return WokHudLayout.compute(Input.screen(width, height, 1).withRoster(8, false)
                .withCapturePanel(CaptureHudBridge.mirroredPanel(width))).roster().top();
    }

    private static void assertSound(Input input, Layout layout, String where) {
        List<UiRect> plates = layout.plates();
        for (UiRect plate : plates) {
            assertTrue(plate.left() >= 0 && plate.top() >= 0 && plate.right() <= layout.width()
                    && plate.bottom() <= layout.height(), where + ": on screen " + plate);
            if (layout.capturePanel() != null) {
                assertTrue(separated(plate, layout.capturePanel(), layout.gap()),
                        where + ": clear of the capture panel " + plate);
            }
            for (UiRect row : WokHudLayout.effectRects(input)) {
                assertTrue(separated(plate, row, layout.gap() - 1),
                        where + ": clear of the effect icons " + plate);
            }
        }
        for (int i = 0; i < plates.size(); i++) {
            for (int j = i + 1; j < plates.size(); j++) {
                assertTrue(separated(plates.get(i), plates.get(j), layout.gap()),
                        where + ": " + plates.get(i) + " vs " + plates.get(j));
            }
        }
        if (layout.staminaPlate() != null) {
            int limit = Math.floorDiv(WokHudLayout.hotbarObstacleLeft(input), input.factor()) - 4;
            assertTrue(layout.staminaPlate().right() <= limit, where + ": stamina vs hotbar");
        }
        List<UiRect> top = new ArrayList<>(layout.toasts());
        if (layout.strip() != null) {
            top.add(layout.strip());
        }
        if (layout.vote() != null) {
            top.add(layout.vote());
        }
        for (UiRect plate : top) {
            assertTrue(plate.bottom() <= layout.topCenterBottom(), where + ": top-centre bottom");
        }
    }

    /** At least {@code gap} pixels between the two rectangles on one axis. */
    private static boolean separated(UiRect a, UiRect b, int gap) {
        return a.right() + gap <= b.left() || b.right() + gap <= a.left()
                || a.bottom() + gap <= b.top() || b.bottom() + gap <= a.top();
    }
}
