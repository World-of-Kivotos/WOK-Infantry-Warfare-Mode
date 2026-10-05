package com.wok.infantry.client.hud;

import com.wok.infantry.client.hud.StaminaBarModel.Mount;
import com.wok.infantry.client.hud.StaminaBarModel.Pool;
import com.wok.infantry.client.hud.StaminaBarModel.State;
import com.wok.infantry.client.hud.StaminaBarModel.Tone;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalIcon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** State rules of the A4 stamina bar, one test per preview state (surfaces/16-stamina.js DEMO). */
class StaminaBarModelTest {
    @Test
    void fullStaminaIsNeutralWithSilhouettes() {
        State full = State.of(100.0F, 100.0F);
        Tone arms = StaminaBarModel.arms(full, true);
        Tone legs = StaminaBarModel.legs(full, true);
        assertEquals(TacticalBoardTheme.NEUTRAL_B, arms.fill());
        assertEquals(TacticalBoardTheme.NEUTRAL_B, arms.accent());
        assertEquals(TacticalIcon.HAND, arms.icon());
        assertEquals(TacticalBoardTheme.LIGHT_MUTED, arms.iconColor());
        assertEquals(TacticalBoardTheme.LIGHT_MUTED, arms.numberColor());
        assertEquals(TacticalIcon.BOOT, legs.icon());
        assertEquals("100%", legs.percent());
        assertTrue(arms.tick() && legs.tick(), "ticks always shown: arms 50 %, legs 15 %");
        assertEquals(TacticalBoardTheme.HUD_TRACK, legs.track());
        assertEquals(TacticalBoardTheme.FRAME, legs.shadow(), "inner shadow");
        assertEquals(0, arms.warn());
    }

    @Test
    void sprintLeavesARemnantOnTheLegs() {
        // preview: 疾跑中腿 64%（0.6 秒前 69%，残影 5%）
        State sprint = new State(Pool.of(100.0F), new Pool(64.0F, 69.0F, false), false, false,
                Mount.NONE, 0.0F);
        Tone legs = StaminaBarModel.legs(sprint, true);
        StaminaBarModel.Spans spans = StaminaBarModel.spans(88, legs.pool());
        assertEquals(56, spans.fill(), "round(88 × 0.64)");
        assertEquals(61, spans.ghost(), "round(88 × 0.69): a 5px remnant");
        assertFalse(spans.head());
        assertEquals(TacticalHud.withAlpha(TacticalBoardTheme.NEUTRAL_B, 0x66), legs.ghostColor(),
                "remnant: fill colour at 40 %");
    }

    @Test
    void armsBelowFiftyTurnOrange() {
        // preview: 开镜中手 42%（橙，残影 2%）
        State aim = new State(new Pool(42.0F, 44.0F, false), Pool.of(86.0F), false, false,
                Mount.NONE, 0.0F);
        Tone arms = StaminaBarModel.arms(aim, true);
        assertEquals(TacticalBoardTheme.ACCENT_B, arms.fill());
        assertEquals(TacticalBoardTheme.ACCENT_B, arms.accent());
        assertEquals(TacticalBoardTheme.ACCENT_B, arms.iconColor());
        assertEquals(TacticalBoardTheme.ACCENT_B, arms.numberColor());
        assertEquals("42%", arms.percent());
        assertEquals(1, arms.warn());
        assertEquals(TacticalBoardTheme.ACCENT_B, StaminaBarModel.arms(aim, false).accent(),
                "orange does not blink");
        assertEquals(TacticalBoardTheme.NEUTRAL_B, StaminaBarModel.arms(State.of(50.0F, 50.0F),
                true).fill(), "exactly 50 is still neutral (sway starts below 50)");
    }

    @Test
    void lockedLegsShowARedEmptySlotAndALock() {
        // preview: 腿耗尽 0%（锁定，红闪亮帧）
        State out = new State(Pool.of(70.0F), new Pool(0.0F, 5.0F, false), true, false,
                Mount.NONE, 0.0F);
        Tone legs = StaminaBarModel.legs(out, true);
        assertEquals(TacticalIcon.LOCK, legs.icon());
        assertEquals(TacticalBoardTheme.DANGER_B, legs.iconColor());
        assertEquals(TacticalBoardTheme.DANGER_B, legs.accent());
        assertEquals(TacticalBoardTheme.DANGER_B, legs.numberColor());
        assertEquals("0%", legs.percent());
        assertEquals(StaminaBarModel.LOCK_TRACK, legs.track(), "a red empty slot, not a full bar");
        assertEquals(0xFF622F2C, StaminaBarModel.LOCK_TRACK, "DANGER and FRAME half and half");
        assertEquals(0xFF3B2423, StaminaBarModel.LOCK_SHADOW);
        assertEquals(StaminaBarModel.LOCK_SHADOW, legs.shadow());
        assertEquals(TacticalBoardTheme.LIGHT, legs.tickColor(), "bright tick: unlocks at 15 %");
        assertEquals(2, legs.warn());
        Tone dim = StaminaBarModel.legs(out, false);
        assertNotEquals(TacticalBoardTheme.DANGER_B, dim.accent(), "2 Hz blink, dim phase");
        assertEquals(dim.accent(), dim.iconColor());
        assertEquals(TacticalBoardTheme.DANGER_B, dim.fill(), "the fill itself does not blink");
        assertEquals(TacticalBoardTheme.NEUTRAL_B, StaminaBarModel.arms(out, true).fill());
    }

    @Test
    void recoveringPoolsEndInALightHead() {
        State recover = new State(new Pool(46.0F, 46.0F, true), new Pool(31.0F, 31.0F, true),
                false, false, Mount.NONE, 0.0F);
        StaminaBarModel.Spans spans = StaminaBarModel.spans(88,
                StaminaBarModel.legs(recover, true).pool());
        assertEquals(27, spans.fill());
        assertTrue(spans.head());
        assertFalse(StaminaBarModel.spans(88, new Pool(100.0F, 100.0F, true)).head(),
                "no head on a full groove");
        assertFalse(StaminaBarModel.spans(88, new Pool(0.5F, 0.5F, true)).head(),
                "nor on an empty one");
    }

    @Test
    void unlockShowsAGreenCheck() {
        State unlock = new State(Pool.of(88.0F), new Pool(16.0F, 16.0F, true), false, true,
                Mount.NONE, 0.0F);
        Tone legs = StaminaBarModel.legs(unlock, true);
        assertEquals(TacticalIcon.CHECK, legs.icon());
        assertEquals(TacticalBoardTheme.SUCCESS_B, legs.iconColor());
        assertEquals(TacticalBoardTheme.SUCCESS_B, legs.accent());
        assertEquals(TacticalBoardTheme.ACCENT_B, legs.fill(), "16 % is below 50: orange");
        assertEquals(TacticalBoardTheme.ACCENT_B, legs.numberColor());
        assertFalse(new State(Pool.of(1.0F), Pool.of(0.0F), true, true, Mount.NONE, 0.0F)
                .unlocked(), "never locked and unlocked at once");
    }

    @Test
    void seatedGraysTheLegsAndJumpableMountsShowTheCharge() {
        State vehicle = new State(Pool.of(64.0F), new Pool(78.0F, 78.0F, true), false, false,
                Mount.SEATED, 0.0F);
        Tone seated = StaminaBarModel.legs(vehicle, true);
        assertEquals(TacticalBoardTheme.OFFLINE, seated.fill());
        assertEquals(TacticalBoardTheme.OFFLINE, seated.accent());
        assertEquals(TacticalBoardTheme.OFFLINE, seated.iconColor());
        assertEquals(TacticalBoardTheme.OFFLINE, seated.numberColor());
        assertEquals(TacticalIcon.BOOT, seated.icon());
        assertFalse(seated.tick());
        assertEquals("78%", seated.percent());
        assertEquals(TacticalBoardTheme.NEUTRAL_B, StaminaBarModel.arms(vehicle, true).fill(),
                "arms keep their colours while seated");

        // preview: 骑马：跳跃蓄力 55%，手 44% 在开镜中下降
        State horse = new State(new Pool(44.0F, 46.0F, false), Pool.of(100.0F), true, false,
                Mount.JUMP, 0.55F);
        Tone jump = StaminaBarModel.legs(horse, true);
        assertEquals(TacticalIcon.JUMP, jump.icon());
        assertEquals(TacticalBoardTheme.LIGHT, jump.fill());
        assertEquals(TacticalBoardTheme.LIGHT, jump.accent());
        assertEquals(TacticalBoardTheme.LIGHT, jump.numberColor());
        assertEquals("55%", jump.percent(), "the percentage is the jump charge");
        assertFalse(jump.tick());
        assertEquals(TacticalBoardTheme.HUD_TRACK, jump.track(), "a lock never shows on a mount");
        assertEquals(TacticalBoardTheme.ACCENT_B, StaminaBarModel.arms(horse, true).fill());

        Tone cooling = StaminaBarModel.legs(new State(Pool.of(90.0F), Pool.of(90.0F), false,
                false, Mount.JUMP_COOLDOWN, 0.0F), true);
        assertEquals(TacticalIcon.JUMP, cooling.icon());
        assertEquals(TacticalBoardTheme.OFFLINE, cooling.iconColor());
    }

    @Test
    void criticalPoolsBlinkAtTwoHertz() {
        Tone bright = StaminaBarModel.arms(State.of(15.0F, 100.0F), true);
        Tone dim = StaminaBarModel.arms(State.of(15.0F, 100.0F), false);
        assertEquals(TacticalBoardTheme.DANGER_B, bright.accent());
        assertEquals(StaminaBarModel.blink(TacticalBoardTheme.DANGER_B, false), dim.accent());
        assertEquals(dim.accent(), dim.numberColor());
        assertTrue(StaminaBarModel.blinkOn(0) && StaminaBarModel.blinkOn(4));
        assertFalse(StaminaBarModel.blinkOn(5) || StaminaBarModel.blinkOn(9));
        assertTrue(StaminaBarModel.blinkOn(10), "5 ticks on, 5 off");
        assertEquals(2, StaminaBarModel.warnLevel(Float.NaN));
        assertEquals(1, StaminaBarModel.warnLevel(15.5F));
    }

    @Test
    void ticksAndPercentagesFollowThePreviewRounding() {
        assertEquals(44, StaminaBarModel.tickOffset(88, StaminaBarLayout.ARMS_TICK));
        assertEquals(13, StaminaBarModel.tickOffset(88, StaminaBarLayout.LEGS_TICK),
                "round(88 × 0.15) = 13.2 → 13");
        assertEquals(9, StaminaBarModel.tickOffset(61, StaminaBarLayout.LEGS_TICK));
        assertEquals("43%", StaminaBarModel.percent(42.5F));
        assertEquals("100%", StaminaBarModel.percent(140.0F));
        assertEquals("0%", StaminaBarModel.percent(Float.NaN));
        assertEquals("0%", StaminaBarModel.percent(-3.0F));
        for (int value = 0; value <= 100; value++) {
            assertEquals(value + "%", StaminaBarModel.percent(value));
        }
        assertSame(StaminaBarModel.percent(64.2F), StaminaBarModel.percent(63.6F),
                "from a table: nothing is formatted per frame");
        assertEquals(new Pool(30.0F, 30.0F, false), new Pool(30.0F, 10.0F, false),
                "a remnant below the value is no remnant");
    }
}
