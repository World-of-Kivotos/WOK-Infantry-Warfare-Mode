package com.wok.infantry.client.hud;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.tickets.TicketNetwork;
import com.wok.infantry.client.hud.BattleStripModel.Outcome;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.UiRect;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleStripModelTest {
    private static final Map<String, String> ZH = HudTestSupport.bundle(HudTestSupport.ZH_CN);
    private static final Map<String, String> EN = HudTestSupport.bundle(HudTestSupport.EN_US);

    @Test
    void outcomeFollowsTheViewersSideAndHandlesADraw() {
        assertEquals(Outcome.NONE, BattleStripModel.outcome(412, 377, Faction.BLUE));
        assertEquals(Outcome.OWN_VICTORY, BattleStripModel.outcome(12, 0, Faction.BLUE));
        assertEquals(Outcome.OWN_DEFEAT, BattleStripModel.outcome(0, 12, Faction.BLUE));
        assertEquals(Outcome.OWN_VICTORY, BattleStripModel.outcome(0, 12, Faction.RED));
        assertEquals(Outcome.OWN_DEFEAT, BattleStripModel.outcome(12, 0, Faction.RED));
        assertEquals(Outcome.DRAW, BattleStripModel.outcome(0, 0, Faction.RED));
        assertEquals(Outcome.DRAW, BattleStripModel.outcome(0, 0, null));
        assertEquals(Outcome.BLUE_VICTORY, BattleStripModel.outcome(5, 0, null));
        assertEquals(Outcome.RED_VICTORY, BattleStripModel.outcome(0, 5, null));
    }

    @Test
    void outcomeWordingComesFromBothLanguages() {
        assertNull(BattleStripModel.outcomeText(Outcome.NONE));
        assertEquals("本局结束 · 我方获胜", text(Outcome.OWN_VICTORY, ZH));
        assertEquals("本局结束 · 我方落败", text(Outcome.OWN_DEFEAT, ZH));
        assertEquals("本局结束 · 平局", text(Outcome.DRAW, ZH));
        assertEquals("本局结束 · 蓝方获胜", text(Outcome.BLUE_VICTORY, ZH));
        assertEquals("本局结束 · 红方获胜", text(Outcome.RED_VICTORY, ZH));
        assertEquals("Round over · Draw", text(Outcome.DRAW, EN));
        assertEquals("Round over · Red Force wins", text(Outcome.RED_VICTORY, EN));
        for (Outcome outcome : Outcome.values()) {
            Component component = BattleStripModel.outcomeText(outcome);
            if (component != null) {
                assertTrue(!text(outcome, ZH).contains("!") && !text(outcome, EN).contains("!"),
                        "every key exists for " + outcome);
            }
        }
    }

    @Test
    void toneIsGreenForVictoryRedForDefeatOtherwiseNeutral() {
        assertEquals(TacticalHud.Tone.SUCCESS, BattleStripModel.outcomeTone(Outcome.OWN_VICTORY));
        assertEquals(TacticalHud.Tone.DANGER, BattleStripModel.outcomeTone(Outcome.OWN_DEFEAT));
        assertEquals(TacticalHud.Tone.NEUTRAL, BattleStripModel.outcomeTone(Outcome.DRAW));
        assertEquals(TacticalHud.Tone.NEUTRAL, BattleStripModel.outcomeTone(Outcome.BLUE_VICTORY));
    }

    @Test
    void ownSideIsFriendlyBlueAndBarsUseTheRoundMaximum() {
        TicketNetwork.Snapshot tickets = new TicketNetwork.Snapshot(250, 500, 500, true);
        BattleStripModel.Sides blue = BattleStripModel.sides(tickets, Faction.BLUE);
        assertEquals(TacticalBoardTheme.HUD_FRIENDLY, blue.blueColor());
        assertEquals(TacticalBoardTheme.HUD_HOSTILE, blue.redColor());
        assertEquals(0.5F, blue.blueRatio());
        assertEquals(1.0F, blue.redRatio());
        assertEquals("250", blue.blueText());

        BattleStripModel.Sides red = BattleStripModel.sides(tickets, Faction.RED);
        assertEquals(TacticalBoardTheme.HUD_HOSTILE, red.blueColor());
        assertEquals(TacticalBoardTheme.HUD_FRIENDLY, red.redColor());
    }

    @Test
    void numbersSitAtTheEdgesAndBarsMeetAtTheCentre() {
        BattleStripModel.Geometry geometry = BattleStripModel.geometry(
                UiRect.of(123, 2, 318, 19), 18, 18);
        assertEquals(127, geometry.blueTextX());
        assertEquals(318 - 4 - 18, geometry.redTextX());
        assertEquals(7, geometry.textY(), "first text row 5px under the top edge");
        assertEquals(UiRect.of(147, 9, 218, 12), geometry.blueTrack());
        assertEquals(UiRect.of(222, 9, 294, 12), geometry.redTrack());
        BattleStripModel.Geometry cramped = BattleStripModel.geometry(UiRect.of(0, 0, 30, 17),
                30, 30);
        assertTrue(cramped.blueTrack().isEmpty() && cramped.redTrack().isEmpty(),
                "no negative tracks on a cramped strip");
    }

    // ---- capture objective tile (0.5.0-beta.2) ------------------------------------------------

    @Test
    void objectiveTileSitsOnTheCentreLineAndTheBarsEndShortOfIt() {
        UiRect strip = UiRect.of(123, 2, 318, 19);
        int max = BattleStripModel.tileMaxWidth(strip, 18, 18, true);
        assertEquals(127, max, "both numbers and a 6px track keep their room on each side");
        BattleStripModel.Tile tile = BattleStripModel.tile(strip, 6, 18, false, max);
        assertEquals(UiRect.of(202, 5, 238, 16), tile.plate(),
                "preview: 3px under the top, 11 tall");
        assertEquals(207, tile.nameX(), "after the 2px edge and 3px");
        assertEquals(6, tile.nameRoom());
        assertEquals(216, tile.valueX(), "the number right-aligned, 4px from the edge");
        assertEquals(7, tile.textY(), "on the strip's first text row");
        BattleStripModel.Geometry geometry = BattleStripModel.geometry(strip, 18, 18,
                tile.plate());
        assertEquals(UiRect.of(147, 9, 198, 12), geometry.blueTrack(), "4px short of the tile");
        assertEquals(UiRect.of(242, 9, 294, 12), geometry.redTrack());

        BattleStripModel.Tile locked = BattleStripModel.tile(strip, 6, 18, true, max);
        assertEquals(UiRect.of(197, 5, 243, 16), locked.plate());
        assertEquals(201, locked.iconX(), "the lock icon after the edge");
        assertEquals(6, locked.iconY());
        assertEquals(212, locked.nameX(), "the name after the 9px icon");

        BattleStripModel.Tile longName = BattleStripModel.tile(strip, 300, 18, false, max);
        assertEquals(127, longName.plate().width(), "capped");
        assertEquals(97, longName.nameRoom(), "the name is shortened, never the number");
        assertEquals(longName.plate().right() - 4 - 18, longName.valueX());
        BattleStripModel.Tile cramped = BattleStripModel.tile(strip, 6, 18, true, 10);
        assertEquals(0, cramped.nameRoom(), "no room: the name is left out");
        assertEquals(40, cramped.plate().width(), "the number and lock always fit");

        assertEquals(187, BattleStripModel.tileMaxWidth(strip, 18, 18, false),
                "without manpower the tile may use the whole strip");
    }

    @Test
    void objectiveColoursFollowTheTakingSideFromTheViewersPointOfView() {
        CaptureObjective b = CaptureObjective.fromMap(CaptureObjectiveTest.pointB());
        BattleStripModel.Objective blue = BattleStripModel.objective(b, Faction.BLUE);
        assertEquals(TacticalBoardTheme.HUD_FRIENDLY, blue.edge(), "our side is taking it");
        assertEquals(TacticalBoardTheme.HUD_FRIENDLY, blue.valueColor());
        assertEquals("B", blue.name().getString());
        assertEquals("62%", blue.value().getString());
        assertTrue(blue.solid());
        assertTrue(!blue.lock());
        BattleStripModel.Objective red = BattleStripModel.objective(b, Faction.RED);
        assertEquals(TacticalBoardTheme.HUD_HOSTILE, red.edge(), "the enemy is taking it");
        assertTrue(red.lock(), "red may not take B yet");
        assertEquals(TacticalBoardTheme.HUD_FRIENDLY,
                BattleStripModel.objective(b, null).edge(), "unknown side: blue is friendly");

        Map<String, Object> contested = CaptureObjectiveTest.pointB();
        contested.put("capturing", "neutral");
        BattleStripModel.Objective frozen = BattleStripModel.objective(
                CaptureObjective.fromMap(contested), Faction.BLUE);
        assertEquals(TacticalBoardTheme.ACCENT_B, frozen.edge());
        assertEquals(TacticalBoardTheme.ACCENT_B, frozen.valueColor());

        Map<String, Object> secured = CaptureObjectiveTest.pointB();
        secured.put("capturing", "neutral");
        secured.put("redPlayers", 0);
        secured.put("owner", "red");
        secured.put("leading", "red");
        secured.put("control", -1.0D);
        secured.put("percent", 100);
        BattleStripModel.Objective owned = BattleStripModel.objective(
                CaptureObjective.fromMap(secured), Faction.BLUE);
        assertEquals(TacticalBoardTheme.HUD_HOSTILE, owned.edge(), "held by the enemy");
        assertEquals("100%", owned.value().getString());

        Map<String, Object> off = CaptureObjectiveTest.pointB();
        off.put("enabled", false);
        BattleStripModel.Objective disabled = BattleStripModel.objective(
                CaptureObjective.fromMap(off), Faction.RED);
        assertEquals(TacticalBoardTheme.OFFLINE, disabled.edge());
        assertEquals(TacticalBoardTheme.LIGHT_MUTED, disabled.nameColor());
        assertTrue(!disabled.solid() && !disabled.lock());
        assertEquals("停用", HudTestSupport.render(disabled.value(), ZH));
        assertEquals("Off", HudTestSupport.render(disabled.value(), EN));
    }

    @Test
    void secondRowNamesThePointItsStatusAndTheTimeLeft() {
        CaptureObjective b = CaptureObjective.fromMap(CaptureObjectiveTest.pointB());
        BattleStripModel.Objective objective = BattleStripModel.objective(b, Faction.BLUE);
        assertEquals("B点 · 指挥所 · 蓝方正在占领 · 速度 ×2 · 剩余 0:09",
                HudTestSupport.render(objective.line(), ZH));
        assertEquals("B点 · 指挥所 · 蓝方正在占领 · 速度 ×2 · 0:09 left",
                HudTestSupport.render(objective.line(), EN));
        Map<String, Object> frozen = CaptureObjectiveTest.pointB();
        frozen.put("remainingSeconds", -1);
        frozen.put("status", Component.literal("争夺中 · 进度冻结"));
        assertEquals("B点 · 指挥所 · 争夺中 · 进度冻结", HudTestSupport.render(
                BattleStripModel.objective(CaptureObjective.fromMap(frozen), Faction.BLUE)
                        .line(), ZH), "no time while nobody takes it");
    }

    private static String text(Outcome outcome, Map<String, String> bundle) {
        return HudTestSupport.render(BattleStripModel.outcomeText(outcome), bundle);
    }
}
