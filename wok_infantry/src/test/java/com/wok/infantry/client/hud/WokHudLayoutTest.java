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
    void bossBarsAlsoClearTheCapturePanelDrawnAboveThem() {
        // the capture panel is drawn above all and would hide bars shifted only under the strip
        for (int[] tier : TIERS) {
            UiRect panel = CaptureHudBridge.mirroredPanel(tier[0]);
            Layout layout = WokHudLayout.compute(Input.screen(tier[0], tier[1], tier[2])
                    .withRoster(8, false).withStrip(true).withCapturePanel(panel));
            String where = tier[0] + "x" + tier[1] + "@" + tier[2];
            int titleTop = WokHudLayout.BOSS_TITLE_TOP + layout.bossShift();
            assertTrue(titleTop >= panel.bottom() + 4, where + ": boss title under the panel");
            assertTrue(titleTop >= layout.topCenterBottom() * tier[2] + 4,
                    where + ": and under the strip");
        }
        assertEquals(61 + 4 - 3, WokHudLayout.compute(Input.screen(320, 240, 1).withStrip(true)
                .withCapturePanel(CaptureHudBridge.mirroredPanel(320))).bossShift());
        assertEquals(0, WokHudLayout.compute(Input.screen(960, 720, 1)
                        .withCapturePanel(CaptureHudBridge.mirroredPanel(960))).bossShift(),
                "no core plate: the bars keep their place above the panel");
        assertEquals(19 + 4 - 3, WokHudLayout.bossShift(1200, 19, UiRect.of(4, 26, 300, 61)),
                "a panel beside the boss column does not move the bars");
    }

    /**
     * B11a: below about 530 GUI pixels the roster reaches into the boss bar column (427 and 480
     * also while both sit under the capture panel); the bars move right, clear of it.
     */
    @Test
    void bossBarsMoveRightOfTheRosterOnNarrowScreens() {
        for (int[] tier : TIERS) {
            for (boolean capture : new boolean[]{false, true}) {
                Input input = Input.screen(tier[0], tier[1], tier[2]).withRoster(8, false)
                        .withStrip(true).withCapturePanel(capture
                                ? CaptureHudBridge.mirroredPanel(tier[0]) : null);
                Layout layout = WokHudLayout.compute(input);
                String where = tier[0] + "x" + tier[1] + "@" + tier[2]
                        + (capture ? " in a capture point" : "");
                UiRect bars = WokHudLayout.bossBarRegion(tier[0], tier[1], layout.bossShift())
                        .offset(layout.bossShiftX(), 0);
                UiRect roster = layout.toGui(layout.roster());
                assertFalse(bars.intersects(roster), where + ": bars " + bars + " vs roster "
                        + roster);
                assertTrue(bars.right() <= tier[0] - layout.edge() * tier[2],
                        where + ": bars stay on screen");
            }
        }
        assertEquals(53, WokHudLayout.compute(Input.screen(320, 240, 1).withRoster(8, false)
                .withStrip(true)).bossShiftX(), "118 + 4 − (160 − 91)");
        assertEquals(56, WokHudLayout.compute(Input.screen(427, 240, 1).withRoster(8, false)
                .withStrip(true).withCapturePanel(CaptureHudBridge.mirroredPanel(427)))
                .bossShiftX(), "reviewed case: 427 wide inside a capture point");
        assertEquals(29, WokHudLayout.compute(Input.screen(480, 270, 1).withRoster(8, false)
                .withStrip(true).withCapturePanel(CaptureHudBridge.mirroredPanel(480)))
                .bossShiftX());
        assertEquals(0, WokHudLayout.compute(Input.screen(640, 336, 1).withRoster(8, false)
                .withStrip(true)).bossShiftX(), "wide enough: the bars stay centred");
        assertEquals(0, WokHudLayout.compute(Input.screen(320, 240, 1).withStrip(true))
                .bossShiftX(), "no roster, no move");
    }

    @Test
    void bossBarsMoveOnlyAsFarAsTheirRightNeighboursAllow() {
        UiRect roster = UiRect.of(2, 2, 118, 95);
        // h = 240: rows at 12, 31, 50, 69 (88 ≥ 80 stops) → region y [23, 94) after a 20px shift
        assertEquals(UiRect.of(69, 23, 251, 94), WokHudLayout.bossBarRegion(320, 240, 20));
        assertEquals(53, WokHudLayout.bossShiftX(320, 240, 20, roster, List.of(), 2));
        assertEquals(15, WokHudLayout.bossShiftX(320, 240, 20, roster,
                List.of(UiRect.of(270, 30, 319, 54)), 2), "stops 4px left of the effect icons");
        assertEquals(53, WokHudLayout.bossShiftX(320, 240, 20, roster,
                List.of(UiRect.of(270, 100, 319, 124), UiRect.of(10, 30, 60, 54)), 2),
                "neighbours below the bars or left of them do not limit the move");
        assertEquals(0, WokHudLayout.bossShiftX(320, 240, 20, roster,
                List.of(UiRect.of(200, 30, 319, 54)), 2), "no room: the bars keep their place");
        assertEquals(0, WokHudLayout.bossShiftX(320, 240, 20, UiRect.of(2, 100, 118, 150),
                List.of(), 2), "a roster under the bars does not move them");
        assertEquals(0, WokHudLayout.bossShiftX(320, 240, 20, null, List.of(), 2));
    }

    @Test
    void playerListCollapsesTheRosterOnlyWhenVanillaDrawsIt() {
        assertFalse(WokHudLayout.vanillaPlayerListShown(false, false, 10, true), "key not held");
        assertFalse(WokHudLayout.vanillaPlayerListShown(true, true, 1, false),
                "single player alone: vanilla shows no list, the roster stays full");
        assertTrue(WokHudLayout.vanillaPlayerListShown(true, true, 2, false));
        assertTrue(WokHudLayout.vanillaPlayerListShown(true, true, 1, true),
                "a list objective is shown even alone");
        assertTrue(WokHudLayout.vanillaPlayerListShown(true, false, 1, false),
                "on a server the list is always drawn");
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

    /**
     * 整体审查修正 compat-01: inside a capture point on a 240–255 high screen the roster, pushed
     * under the capture panel, reached into WOK步战附属-部位血量's figure (drawn under the core
     * HUD) and covered its head label. Real add-on geometry: body health 0.1.0-beta.10 puts the
     * figure at h − 73 and the head chip one pixel higher; the companion slot sits 60 under the
     * figure top.
     */
    @Test
    void rosterEndsAboveTheBodyHealthFigureInsideACapturePoint() {
        Layout reviewed = WokHudLayout.compute(inCapturePoint(427, 240)
                .withAddonPanels(List.of(bodyHealthColumn(427, 240))));
        assertEquals(83, reviewed.roster().top(), "still under the capture panel");
        assertEquals(6, reviewed.rosterRows(), "two member rows give way to the figure");
        assertEquals(UiRect.of(2, 83, 174, 83 + 11 + 6 * 10 + 2), reviewed.roster());
        assertTrue(reviewed.roster().bottom() + reviewed.gap() <= 166, "head chip starts at y166");

        assertEquals(6, WokHudLayout.compute(inCapturePoint(400, 240)
                .withAddonPanels(List.of(bodyHealthColumn(400, 240)))).rosterRows());
        assertEquals(7, WokHudLayout.compute(inCapturePoint(427, 250)
                .withAddonPanels(List.of(bodyHealthColumn(427, 250)))).rosterRows());
        for (int[] size : new int[][]{{455, 256}, {480, 270}, {640, 336}, {640, 360}}) {
            Layout layout = WokHudLayout.compute(inCapturePoint(size[0], size[1])
                    .withAddonPanels(List.of(bodyHealthColumn(size[0], size[1]))));
            assertEquals(8, layout.rosterRows(), size[0] + "x" + size[1] + ": everyone fits");
            assertEquals(WokHudLayout.compute(inCapturePoint(size[0], size[1])).roster(),
                    layout.roster(), size[0] + "x" + size[1] + ": nothing moves");
        }
        Layout alone = WokHudLayout.compute(inCapturePoint(427, 240));
        assertEquals(8, alone.rosterRows(), "without body health the roster keeps every row");
        assertEquals(UiRect.of(2, 83, 174, 176), alone.roster());
        Layout outside = WokHudLayout.compute(Input.screen(427, 240, 1).withRoster(8, false)
                .withStrip(true).withAddonPanels(List.of(bodyHealthColumn(427, 240))));
        assertEquals(8, outside.rosterRows(), "outside a capture point the figure is far below");
        Layout collapsed = WokHudLayout.compute(Input.screen(427, 240, 1).withRoster(8, true)
                .withStrip(true).withCapturePanel(CaptureHudBridge.mirroredPanel(427))
                .withAddonPanels(List.of(bodyHealthColumn(427, 240))));
        assertEquals(0, collapsed.rosterRows());
        assertEquals(UiRect.of(2, 2, 174, 13), collapsed.roster(),
                "the title row alone stays above the capture panel");
    }

    /**
     * 整体审查修正 compat-01: WOK步战附属-倒地 0.1.0-alpha.3 draws its panel above all at the
     * bottom centre while the viewer is down; at 427×240 it covered the roster's last row.
     */
    @Test
    void rosterEndsAboveTheDownedPanelWhileTheViewerIsDown() {
        UiRect panel = DownedHudBridge.mirroredPanel(427, 240);
        Layout layout = WokHudLayout.compute(inCapturePoint(427, 240)
                .withAddonPanels(List.of(panel)));
        assertEquals(6, layout.rosterRows());
        assertTrue(layout.roster().bottom() + layout.gap() <= panel.top(), layout.roster().toString());
        // 21:9 (3440×1440 at GUI 6): the centre-low slot is clear of the roster, the panel is not
        Layout ultrawide = WokHudLayout.compute(inCapturePoint(573, 240)
                .withAddonPanels(List.of(DownedHudBridge.mirroredPanel(573, 240))));
        assertEquals(6, ultrawide.rosterRows());
        Layout both = WokHudLayout.compute(inCapturePoint(427, 240)
                .withAddonPanels(List.of(bodyHealthColumn(427, 240), panel)));
        assertEquals(6, both.rosterRows(), "the higher of the two decides");
    }

    /**
     * 整体审查修正 compat-01/05: an add-on that asks for the centre-low slot (the slot the core
     * offers the downed panel) gets it clear of the roster, also under a capture panel.
     */
    @Test
    void rosterEndsAboveTheCentreLowSlotWhileAnAddOnUsesIt() {
        Layout free = WokHudLayout.compute(inCapturePoint(427, 240));
        assertTrue(free.roster().intersects(free.centerLow()),
                "premise: under the capture panel the full roster reaches into the slot");
        Layout used = WokHudLayout.compute(inCapturePoint(427, 240).withCenterLowInUse(true));
        assertEquals(free.centerLow(), used.centerLow(), "the slot itself does not move");
        assertEquals(3, used.rosterRows());
        assertTrue(used.roster().bottom() + used.gap() <= used.centerLow().top());
        Layout top = WokHudLayout.compute(Input.screen(427, 240, 1).withRoster(8, false)
                .withStrip(true).withCenterLowInUse(true));
        assertEquals(8, top.rosterRows(), "a roster at the top is clear of the slot anyway");
        assertTrue(HudFrame.centerLowInUse(5, 5), "asked this frame");
        assertTrue(HudFrame.centerLowInUse(5, 4), "asked last frame (laid out before the ask)");
        assertFalse(HudFrame.centerLowInUse(5, 3), "no longer asked");
        assertFalse(HudFrame.centerLowInUse(0, -1), "never asked");
    }

    @Test
    void rosterGivesUpRowsThenItsMembersThenEverything() {
        assertEquals(8, WokHudLayout.rosterRowsAbove(83, 200, 8, true, false));
        assertEquals(6, WokHudLayout.rosterRowsAbove(83, 163, 8, true, false));
        assertEquals(1, WokHudLayout.rosterRowsAbove(83, 106, 8, true, false));
        assertEquals(0, WokHudLayout.rosterRowsAbove(83, 105, 8, true, false), "title row only");
        assertEquals(0, WokHudLayout.rosterRowsAbove(83, 94, 8, true, false));
        assertEquals(-1, WokHudLayout.rosterRowsAbove(83, 93, 8, true, false), "not even the title");
        assertEquals(0, WokHudLayout.rosterRowsAbove(83, 200, 8, true, true), "collapsed stays so");
        assertEquals(7, WokHudLayout.rosterRowsAbove(84, 84 + 13 + 2 + 7 * 12, 8, false, false));

        Layout hidden = WokHudLayout.compute(inCapturePoint(427, 240)
                .withAddonPanels(List.of(UiRect.of(0, 90, 200, 240))));
        assertNull(hidden.roster(), "a roster that cannot keep its title row clear is not drawn");
        assertEquals(0, hidden.rosterRows());
        assertEquals(Integer.MAX_VALUE, WokHudLayout.floorAbove(UiRect.of(2, 83, 174, 176),
                List.of(UiRect.of(180, 100, 300, 240), UiRect.of(0, 10, 100, 60)), 3),
                "panels beside or above the roster do not limit it");
    }

    /** Every tier, in and out of a capture point, with every add-on panel of the real add-ons. */
    @Test
    void everyTierKeepsTheRosterClearOfAddOnPanels() {
        for (int[] tier : TIERS) {
            for (boolean capture : new boolean[]{false, true}) {
                for (boolean downed : new boolean[]{false, true}) {
                    for (boolean centreLow : new boolean[]{false, true}) {
                        List<UiRect> panels = new ArrayList<>();
                        panels.add(bodyHealthColumn(tier[0], tier[1]));
                        if (downed) {
                            panels.add(DownedHudBridge.mirroredPanel(tier[0], tier[1]));
                        }
                        Input input = Input.screen(tier[0], tier[1], tier[2]).withRoster(8, false)
                                .withStrip(true).withToasts(List.of(150, 60)).withEffects(3, 2)
                                .withCapturePanel(capture
                                        ? CaptureHudBridge.mirroredPanel(tier[0]) : null)
                                .withAddonPanels(panels).withCenterLowInUse(centreLow);
                        Layout layout = WokHudLayout.compute(input);
                        String where = tier[0] + "x" + tier[1] + "@" + tier[2] + " capture="
                                + capture + " downed=" + downed + " centreLow=" + centreLow;
                        assertSound(input, layout, where);
                        UiRect roster = layout.toGui(layout.roster());
                        for (UiRect panel : panels) {
                            assertFalse(roster.intersects(panel), where + ": " + roster + " vs "
                                    + panel);
                        }
                        if (centreLow) {
                            assertFalse(layout.roster().intersects(layout.centerLow()), where);
                        }
                        assertTrue(layout.rosterRows() >= 3, where + ": keeps most of the squad");
                    }
                }
            }
        }
    }

    private static Input inCapturePoint(int width, int height) {
        return Input.screen(width, height, 1).withRoster(8, false).withStrip(true)
                .withCapturePanel(CaptureHudBridge.mirroredPanel(width));
    }

    /** Body health 0.1.0-beta.10's column through the bridge, as its companion slot gives it. */
    private static UiRect bodyHealthColumn(int width, int height) {
        int figureX = 50;
        int figureY = height - 48 - 25;
        int[] slot = {figureX + 39 / 2 - 52 / 2, figureY + 48 + 25 - 11 - 2, 52, 11};
        return BodyHealthHudBridge.columnAbove(slot, width, height);
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
