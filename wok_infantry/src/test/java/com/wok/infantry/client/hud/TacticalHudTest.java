package com.wok.infantry.client.hud;

import com.wok.infantry.client.hud.TacticalHud.Box;
import com.wok.infantry.client.hud.TacticalHud.Edge;
import com.wok.infantry.client.hud.TacticalHud.PlacedSegment;
import com.wok.infantry.client.hud.TacticalHud.Segment;
import com.wok.infantry.client.hud.TacticalHud.Tone;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.stamina.StaminaRules;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalHudTest {
    /** Monospaced stand-in for the font: 6px per character, key caps 4px wider. */
    private static final ToIntFunction<Segment> WIDTH = segment ->
            segment.text().getString().length() * 6 + (segment.key() ? 4 : 0);

    @Test
    void accentBarFollowsTheAnchoredSide() {
        assertEquals(new Box(10, 20, 12, 40), TacticalHud.accentBar(10, 20, 110, 40, Edge.LEFT));
        assertEquals(new Box(108, 20, 110, 40), TacticalHud.accentBar(10, 20, 110, 40, Edge.RIGHT));
        assertEquals(new Box(10, 20, 110, 21), TacticalHud.accentBar(10, 20, 110, 40, Edge.TOP));
        assertNull(TacticalHud.accentBar(10, 20, 110, 40, Edge.NONE));
        assertNull(TacticalHud.accentBar(10, 20, 10, 40, Edge.LEFT), "empty plate");
        assertEquals(new Box(10, 20, 11, 40), TacticalHud.accentBar(10, 20, 11, 40, Edge.LEFT),
                "a 1px plate keeps its bar inside");
    }

    @Test
    void meterFillRoundsLikeThePreviewAndClamps() {
        assertEquals(21, TacticalHud.fillWidth(41, 0.5F));
        assertEquals(0, TacticalHud.fillWidth(41, 0.0F));
        assertEquals(41, TacticalHud.fillWidth(41, 1.7F));
        assertEquals(0, TacticalHud.fillWidth(41, -0.2F));
        assertEquals(0, TacticalHud.fillWidth(41, Float.NaN));
        assertEquals(0, TacticalHud.fillWidth(0, 1.0F));
        assertEquals(41, TacticalHud.fillWidth(41, Float.POSITIVE_INFINITY));
    }

    @Test
    void meterTicksSplitTheTrackEvenly() {
        assertEquals(25, TacticalHud.tickX(0, 100, 1, 4));
        assertEquals(50, TacticalHud.tickX(0, 100, 2, 4));
        assertEquals(10 + 33, TacticalHud.tickX(10, 100, 1, 3));
    }

    @Test
    void healthColourThresholds() {
        assertEquals(TacticalBoardTheme.SUCCESS_B, TacticalHud.healthColor(0.56F));
        assertEquals(TacticalBoardTheme.ACCENT_B, TacticalHud.healthColor(0.55F));
        assertEquals(TacticalBoardTheme.ACCENT_B, TacticalHud.healthColor(0.26F));
        assertEquals(TacticalBoardTheme.DANGER_B, TacticalHud.healthColor(0.25F));
        assertEquals(TacticalBoardTheme.DANGER_B, TacticalHud.healthColor(Float.NaN));
        assertEquals(TacticalBoardTheme.SUCCESS_B, TacticalHud.healthColor(1.0F));
    }

    @Test
    void staminaKeepsTheCoreThresholdsAndANeutralNormalColour() {
        assertEquals(50.0F, StaminaRules.SWAY_START_STAMINA,
                "the orange threshold follows the sway start (plan 6.2: keep 50, not 35)");
        assertEquals(TacticalBoardTheme.DANGER_B, TacticalHud.staminaColor(15.0F));
        assertEquals(TacticalBoardTheme.DANGER_B, TacticalHud.staminaColor(0.0F));
        assertEquals(TacticalBoardTheme.ACCENT_B, TacticalHud.staminaColor(15.1F));
        assertEquals(TacticalBoardTheme.ACCENT_B, TacticalHud.staminaColor(49.9F));
        assertEquals(TacticalBoardTheme.NEUTRAL_B, TacticalHud.staminaColor(50.0F));
        assertEquals(TacticalBoardTheme.NEUTRAL_B, TacticalHud.staminaColor(100.0F));
        assertEquals(TacticalBoardTheme.DANGER_B, TacticalHud.staminaColor(Float.NaN));
        assertFalse(TacticalHud.staminaColor(80.0F) == TacticalBoardTheme.SELECT_B,
                "stamina never looks like the current selection");
    }

    @Test
    void totalsDropTheMaximumInsteadOfBeingEllipsized() {
        ToIntFunction<String> width = text -> text.length() * 6;
        assertEquals("290/405", TacticalHud.totalTextPlain(290, 405, 42, width));
        assertEquals("290", TacticalHud.totalTextPlain(290, 405, 41, width));
        assertEquals("86", TacticalHud.totalTextPlain(86, 405, 10, width),
                "the value stays whole even when it does not fit");
    }

    @Test
    void segmentsPlaceKeyCapsWholeAndCutTextOnlyAtTheEnd() {
        List<Segment> line = List.of(
                Segment.text(Component.literal("Vote "), TacticalBoardTheme.LIGHT_MUTED),
                Segment.key(Component.literal("P")),
                Segment.text(Component.literal(" > tab"), TacticalBoardTheme.LIGHT_MUTED));

        List<PlacedSegment> wide = TacticalHud.layoutSegments(line, 10, 200, WIDTH);
        assertEquals(List.of(new PlacedSegment(0, 10, 30, false),
                new PlacedSegment(1, 40, 10, false),
                new PlacedSegment(2, 50, 36, false)), wide);

        List<PlacedSegment> cut = TacticalHud.layoutSegments(line, 10, 70, WIDTH);
        assertEquals(new PlacedSegment(2, 50, 20, true), cut.get(2),
                "the last text gets the remaining room and is ellipsized");

        List<PlacedSegment> noKey = TacticalHud.layoutSegments(line, 10, 45, WIDTH);
        assertEquals(1, noKey.size(), "a key cap that does not fit whole is left out");
    }

    @Test
    void segmentsStopAfterACutText() {
        List<Segment> line = List.of(
                Segment.text(Component.literal("long status text"), TacticalBoardTheme.LIGHT),
                Segment.key(Component.literal("P")));

        List<PlacedSegment> placed = TacticalHud.layoutSegments(line, 0, 40, WIDTH);
        assertEquals(List.of(new PlacedSegment(0, 0, 40, true)), placed);
        assertTrue(TacticalHud.layoutSegments(line, 40, 40, WIDTH).isEmpty());
        assertTrue(TacticalHud.layoutSegments(Arrays.asList((Segment) null), 0, 40, WIDTH)
                .isEmpty());
    }

    @Test
    void labelledBarSitsOnTheTextMidLine() {
        assertEquals(new Box(20, 102, 60, 105), TacticalHud.labelledBarTrack(20, 100, 60));
        assertNull(TacticalHud.labelledBarTrack(60, 100, 60));
    }

    @Test
    void tonesAndTintsComeFromTheTokens() {
        assertEquals(TacticalBoardTheme.SUCCESS_B, TacticalHud.toneColor(Tone.SUCCESS));
        assertEquals(TacticalBoardTheme.ACCENT_B, TacticalHud.toneColor(Tone.INFO));
        assertEquals(TacticalBoardTheme.DANGER_B, TacticalHud.toneColor(Tone.DANGER));
        assertEquals(TacticalBoardTheme.NEUTRAL_B, TacticalHud.toneColor(Tone.NEUTRAL));
        assertEquals(TacticalBoardTheme.NEUTRAL_B, TacticalHud.toneColor(null));
        assertEquals(0x307BB8EA, TacticalHud.SELF_ROW_TINT);
        assertEquals(0xFF123456, TacticalHud.withAlpha(0x00123456, 300));
        assertEquals(0x00123456, TacticalHud.withAlpha(0xFF123456, -5));
        assertEquals(9, TacticalHud.keyCapWidth(5));
    }
}
