package com.wok.infantry.client.hud;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contracts other packs and MODs rely on: overlay ids (hidden by id in configs), the HUD words
 * in both languages, and the hud-stack-01 regression (the manpower strip and supply notice ship
 * with the core, and stamina keeps the experience row above the hotbar), the vanilla overlays the
 * stamina bar replaces, and the hook that tells WOK步战附属-部位血量 its companion strip is free.
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
    void effectIconOffsetIsTakenFromWhicheverMeasurementSawTheTranslation() {
        // registered before the translating MOD: pre sees nothing, post sees the shift
        assertEquals(List.of(-75, 0), List.of(box(HudFrame.effectOffset(0, 0, -75, 0))));
        // registered after it: pre sees the shift, post sees the popped pose
        assertEquals(List.of(-75, 3), List.of(box(HudFrame.effectOffset(-75, 3, 0, 0))));
        assertEquals(List.of(0, 0), List.of(box(HudFrame.effectOffset(0, 0, 0, 0))));
    }

    private static Integer[] box(int[] values) {
        return new Integer[]{values[0], values[1]};
    }

    @Test
    void manpowerStripAndSupplyNoticeStayInTheCoreHud() {
        // hud-stack-01: the D: copy's manpower banner and supply panel must never regress.
        WokHudLayout.Layout layout = WokHudLayout.compute(WokHudLayout.Input.screen(320, 240, 1)
                .withRoster(8, false).withStrip(true).withToasts(List.of(150)));
        assertTrue(layout.strip() != null && layout.toasts().size() == 1);
        StaminaBarLayout.Layout stamina = layout.staminaBar();
        assertTrue(stamina.band().left() >= 320 / 2 - 91 && stamina.band().right() <= 320 / 2 + 91,
                "stamina sits in the experience row of the hotbar column");
        assertEquals(240 - 23, stamina.band().bottom(), "and never on the hotbar itself");
    }

    @Test
    void staminaBarReplacesOnlyTheExperienceAndJumpBars() {
        assertTrue(StaminaHudOverlay.replacesVanillaOverlay(
                new ResourceLocation("minecraft", "experience_bar")));
        assertTrue(StaminaHudOverlay.replacesVanillaOverlay(
                new ResourceLocation("minecraft", "jump_bar")));
        for (String kept : List.of("hotbar", "player_health", "armor_level", "food_level",
                "air_level", "mount_health", "item_name", "chat_panel")) {
            assertFalse(StaminaHudOverlay.replacesVanillaOverlay(
                    new ResourceLocation("minecraft", kept)), kept);
        }
        assertFalse(StaminaHudOverlay.replacesVanillaOverlay(
                new ResourceLocation("wok_infantry", "experience_bar")));
    }

    @Test
    void bodyHealthCompanionStripIsNoLongerUsed() {
        assertFalse(InfantryHudApi.usesBodyHealthCompanionSlot());
    }
}
