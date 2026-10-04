package com.wok.infantry.client.hud;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contracts other packs and MODs rely on: overlay ids (hidden by id in configs), the HUD words
 * in both languages, and the hud-stack-01 regression (the manpower strip and supply notice ship
 * with the core, and stamina keeps clear of the hotbar).
 */
class HudOverlayContractTest {
    @Test
    void overlayIdsAreKept() {
        assertEquals("squad_roster", SquadHudOverlay.ID);
        assertEquals("stamina", StaminaHudOverlay.ID);
        assertEquals("tickets", BattleStripOverlay.ID);
        assertEquals("formation_vote", FormationVoteHudOverlay.ID);
    }

    @Test
    void hudWordsExistInBothLanguages() {
        Map<String, String> zh = HudTestSupport.bundle(HudTestSupport.ZH_CN);
        Map<String, String> en = HudTestSupport.bundle(HudTestSupport.EN_US);
        for (String key : List.of(StaminaHudOverlay.ARMS_SHORT_KEY,
                StaminaHudOverlay.LEGS_SHORT_KEY, SquadRosterModel.DOWNED_KEY,
                SquadRosterModel.DEAD_KEY, SquadRosterModel.WAITING_KEY,
                SquadRosterModel.WAITING_SHORT_KEY, SquadRosterModel.OFFLINE_KEY,
                BattleStripModel.VICTORY_KEY, BattleStripModel.DEFEAT_KEY,
                BattleStripModel.DRAW_KEY, BattleStripModel.SIDE_VICTORY_KEY)) {
            assertTrue(zh.containsKey(key) && en.containsKey(key), key);
        }
        assertEquals("手", zh.get(StaminaHudOverlay.ARMS_SHORT_KEY));
        assertEquals("腿", zh.get(StaminaHudOverlay.LEGS_SHORT_KEY));
        assertEquals("待命", zh.get(SquadRosterModel.WAITING_SHORT_KEY),
                "two-character short word, never half of 待部署");
    }

    @Test
    void manpowerStripAndSupplyNoticeStayInTheCoreHud() {
        // hud-stack-01: the D: copy's manpower banner and supply panel must never regress.
        WokHudLayout.Layout layout = WokHudLayout.compute(WokHudLayout.Input.screen(320, 240, 1)
                .withRoster(8, false).withStrip(true).withToasts(List.of(150)).withStamina(true));
        assertTrue(layout.strip() != null && layout.toasts().size() == 1);
        assertTrue(layout.staminaPlate().right() <= 320 / 2 - 91 - 4);
    }
}
