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

    private static String text(Outcome outcome, Map<String, String> bundle) {
        return HudTestSupport.render(BattleStripModel.outcomeText(outcome), bundle);
    }
}
