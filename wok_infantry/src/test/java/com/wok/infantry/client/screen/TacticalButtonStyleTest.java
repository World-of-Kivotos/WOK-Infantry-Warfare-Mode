package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalButtonStyle.Look;
import com.wok.infantry.client.screen.TacticalButtonStyle.Palette;
import com.wok.infantry.client.screen.TacticalButtonStyle.State;
import com.wok.infantry.client.screen.TacticalButtonStyle.Variant;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalButtonStyleTest {
    @Test
    void disabledBeatsSelectionKindAndHover() {
        for (Variant variant : Variant.values()) {
            for (boolean selected : new boolean[]{false, true}) {
                for (boolean hovered : new boolean[]{false, true}) {
                    Look look = TacticalButtonStyle.resolve(false, selected, false, variant,
                            hovered, true);
                    assertEquals(State.DISABLED, look.state(), variant + " selected=" + selected);
                    assertFalse(look.hovered(), "disabled keys never react to hover");
                }
            }
        }
    }

    @Test
    void currentBeatsDisabledAndIgnoresHover() {
        Look look = TacticalButtonStyle.resolve(false, true, true, Variant.CONTROL, true, false);

        assertEquals(State.CURRENT, look.state());
        assertFalse(look.hovered());
    }

    @Test
    void selectionBeatsKind() {
        for (Variant variant : Variant.values()) {
            assertEquals(State.SELECTED, TacticalButtonStyle.resolve(true, true, false, variant,
                    false, false).state(), variant.name());
        }
        assertTrue(TacticalButtonStyle.resolve(true, true, false, Variant.NORMAL, true, false)
                .hovered(), "a clickable selected key still brightens on hover");
    }

    @Test
    void kindBeatsHover() {
        assertEquals(State.DANGER, resolveActive(Variant.DANGER, false).state());
        assertEquals(State.DANGER_ARMED, resolveActive(Variant.DANGER, true).state());
        assertEquals(State.SUCCESS, resolveActive(Variant.SUCCESS, true).state());
        assertEquals(State.CONTROL, resolveActive(Variant.CONTROL, true).state());
        assertEquals(State.HOVER, resolveActive(Variant.NORMAL, true).state());
        assertEquals(State.NORMAL, resolveActive(Variant.NORMAL, false).state());
    }

    @Test
    void armedDangerKeyIsRedWithoutHover() {
        Look look = TacticalButtonStyle.resolve(true, false, false, Variant.DANGER, false, true);
        Palette palette = TacticalButtonStyle.palette(look);

        assertEquals(State.DANGER_ARMED, look.state());
        assertEquals(TacticalBoardTheme.DANGER, palette.fill());
        assertEquals(TacticalBoardTheme.LIGHT, palette.text());
    }

    @Test
    void sevenStatesAndCurrentAreVisuallyDistinct() {
        Set<String> looks = new HashSet<>();
        for (State state : EnumSet.of(State.NORMAL, State.HOVER, State.SELECTED,
                State.DISABLED, State.DANGER, State.SUCCESS, State.CONTROL)) {
            Palette palette = TacticalButtonStyle.palette(new Look(state, false));
            assertTrue(looks.add(palette.fill() + "/" + palette.edge() + "/" + palette.text()
                    + "/" + palette.bar() + "/" + palette.underline() + "/" + palette.hatch()),
                    state + " must not look like another state");
        }
    }

    @Test
    void currentLooksSelectedButNeverHovers() {
        Palette current = TacticalButtonStyle.palette(new Look(State.CURRENT, true));
        Palette selected = TacticalButtonStyle.palette(new Look(State.SELECTED, false));

        assertEquals(selected, current);
        assertEquals(TacticalBoardTheme.SELECT, current.fill());
        assertEquals(TacticalBoardTheme.SELECT_BAR, current.bar());
        assertEquals(TacticalBoardTheme.ON_SELECT, current.text());
    }

    @Test
    void disabledIsGrayHatchedAndCarriesNoSemanticColour() {
        Palette disabled = TacticalButtonStyle.palette(new Look(State.DISABLED, false));

        assertEquals(TacticalBoardTheme.CARD_DISABLED, disabled.fill());
        assertEquals(TacticalBoardTheme.DISABLED_EDGE, disabled.edge());
        assertEquals(TacticalBoardTheme.DISABLED_TEXT, disabled.text());
        assertEquals(TacticalButtonStyle.NONE, disabled.bar());
        assertEquals(TacticalButtonStyle.NONE, disabled.underline());
        assertTrue(disabled.hatch());
    }

    @Test
    void controlUnderlineOnlyWhileUsable() {
        Palette usable = TacticalButtonStyle.palette(resolveActive(Variant.CONTROL, false));
        Palette unusable = TacticalButtonStyle.palette(TacticalButtonStyle.resolve(false, false,
                false, Variant.CONTROL, false, false));

        assertEquals(TacticalBoardTheme.ADJUST, usable.underline());
        assertEquals(TacticalButtonStyle.NONE, unusable.underline());
    }

    @Test
    void hoverDarkensTheOutlineInsteadOfTurningBlue() {
        Palette hover = TacticalButtonStyle.palette(resolveActive(Variant.NORMAL, true));

        assertEquals(TacticalBoardTheme.CARD_HOVER, hover.fill());
        assertEquals(TacticalBoardTheme.BORDER_DARK, hover.edge());
        assertNotEquals(TacticalBoardTheme.SELECT, hover.edge());
    }

    @Test
    void raisedKeysUseTheIntendedCardLip() {
        assertEquals(TacticalBoardTheme.CARD_LIP,
                TacticalButtonStyle.palette(resolveActive(Variant.NORMAL, false)).lip());
        assertEquals(TacticalBoardTheme.CARD_LIP_HOVER,
                TacticalButtonStyle.palette(resolveActive(Variant.NORMAL, true)).lip());
        assertFalse(TacticalButtonStyle.palette(new Look(State.SELECTED, false)).raised());
    }

    @Test
    void engagedButUnclickableOnlyNavigationDrawsCurrent() {
        assertEquals(State.CURRENT, TacticalBoardButton.look(TacticalBoardButton.Kind.NAVIGATION,
                true, false, true).state());
        for (TacticalBoardButton.Kind kind : TacticalBoardButton.Kind.values()) {
            if (kind == TacticalBoardButton.Kind.NAVIGATION) {
                continue;
            }
            assertEquals(State.DISABLED, TacticalBoardButton.look(kind, true, false, true).state(),
                    kind + ": an engaged key that cannot be clicked must look disabled");
        }
    }

    @Test
    void formationRowsOutsideVotingLookDisabledAndIgnoreHover() {
        // FormationSelectionScreen: highlighted formation row (CONTROL, engaged) before voting opens.
        Look highlighted = TacticalBoardButton.look(TacticalBoardButton.Kind.CONTROL,
                true, false, true);
        Look other = TacticalBoardButton.look(TacticalBoardButton.Kind.CONTROL,
                false, false, true);
        // Category without formations (TOGGLE, engaged when it was the remembered category).
        Look emptyCategory = TacticalBoardButton.look(TacticalBoardButton.Kind.TOGGLE,
                true, false, true);

        assertEquals(new Look(State.DISABLED, false), highlighted);
        assertEquals(new Look(State.DISABLED, false), other);
        assertEquals(new Look(State.DISABLED, false), emptyCategory);
        assertEquals(TacticalButtonStyle.NONE, TacticalButtonStyle.palette(other).underline());
    }

    @Test
    void boardKindsMapOntoTheSharedVariants() {
        assertEquals(State.SELECTED, TacticalBoardButton.look(TacticalBoardButton.Kind.TOGGLE,
                true, true, false).state());
        assertEquals(State.NORMAL, TacticalBoardButton.look(TacticalBoardButton.Kind.TOOL,
                false, true, false).state());
        assertEquals(State.CONTROL, TacticalBoardButton.look(TacticalBoardButton.Kind.CONTROL,
                false, true, false).state());
        assertEquals(State.DANGER, TacticalBoardButton.look(TacticalBoardButton.Kind.DANGER,
                false, true, false).state());
        assertEquals(State.DANGER_ARMED, TacticalBoardButton.look(TacticalBoardButton.Kind.DANGER,
                false, true, true).state());
    }

    @Test
    void battleKeySelectedButInactiveDrawsCurrent() {
        // Current tab / class / deployment point: selected(true) with active = false.
        Look look = BattleUiButton.look(BattleUiButton.Kind.NORMAL, true, false, false, true,
                false);

        assertEquals(new Look(State.CURRENT, false), look);
    }

    @Test
    void battleKeyInactiveWithoutSelectionIsDisabled() {
        assertEquals(State.DISABLED, BattleUiButton.look(BattleUiButton.Kind.SUCCESS, false,
                false, false, true, false).state());
    }

    @Test
    void battleKeyExplicitCurrentIgnoresHoverEvenWhenActive() {
        assertEquals(new Look(State.CURRENT, false), BattleUiButton.look(
                BattleUiButton.Kind.NORMAL, false, true, true, true, false));
    }

    @Test
    void battleKindsMapOntoTheSharedVariants() {
        assertEquals(Variant.NORMAL, BattleUiButton.variant(BattleUiButton.Kind.NORMAL));
        assertEquals(Variant.CONTROL, BattleUiButton.variant(BattleUiButton.Kind.CONTROL));
        assertEquals(Variant.DANGER, BattleUiButton.variant(BattleUiButton.Kind.DANGER));
        assertEquals(Variant.SUCCESS, BattleUiButton.variant(BattleUiButton.Kind.SUCCESS));
        assertEquals(Variant.NORMAL, BattleUiButton.variant(null));
    }

    @Test
    void labelPaddingLeavesRoomForTheSelectionStripe() {
        assertEquals(5, TacticalButtonStyle.labelPadLeft(
                TacticalButtonStyle.palette(new Look(State.SELECTED, false))));
        assertEquals(3, TacticalButtonStyle.labelPadLeft(
                TacticalButtonStyle.palette(new Look(State.NORMAL, false))));
    }

    @Test
    void cardOutlineFollowsThePreviewCard() {
        assertEquals(TacticalBoardTheme.SELECT_EDGE,
                TacticalButtonStyle.cardEdge(TacticalButtonStyle.CardState.SELECTED));
        assertEquals(TacticalBoardTheme.BORDER_DARK,
                TacticalButtonStyle.cardEdge(TacticalButtonStyle.CardState.HOVER));
        assertEquals(TacticalBoardTheme.BORDER,
                TacticalButtonStyle.cardEdge(TacticalButtonStyle.CardState.NORMAL));
        assertEquals(TacticalBoardTheme.BORDER,
                TacticalButtonStyle.cardEdge(TacticalButtonStyle.CardState.DISABLED));
    }

    @Test
    void loadoutCardsFollowTheSamePriority() {
        assertEquals(TacticalButtonStyle.CardState.SELECTED,
                LoadoutPreviewButton.cardState(true, false, true));
        assertEquals(TacticalButtonStyle.CardState.DISABLED,
                LoadoutPreviewButton.cardState(false, false, true));
        assertEquals(TacticalButtonStyle.CardState.HOVER,
                LoadoutPreviewButton.cardState(false, true, true));
        assertEquals(TacticalButtonStyle.CardState.NORMAL,
                LoadoutPreviewButton.cardState(false, true, false));
    }

    @Test
    void labelWithoutIconCentresLikeTextFit() {
        // 60px key, pad 3 + 3: room 54; a 20px label centres at 3 + 17.
        TacticalButtonStyle.Content content = TacticalButtonStyle.content(0, 0, 60, 14, 3,
                false, 20, 0, TextFit.Align.CENTER);

        assertEquals(54, content.textRoom());
        assertEquals(TextFit.alignedX(3, 54, 20, TextFit.Align.CENTER), content.textX());
        assertEquals(20, content.textX());
    }

    @Test
    void iconAndLabelCentreAsOneGroup() {
        // Preview UI.button: room excludes the 11px icon advance; the icon+label group is centred.
        TacticalButtonStyle.Content content = TacticalButtonStyle.content(0, 0, 60, 14, 3,
                true, 20, 0, TextFit.Align.CENTER);

        assertEquals(43, content.textRoom());
        assertEquals(14, content.iconX());
        assertEquals(2, content.iconY());
        assertEquals(25, content.textX());
        int groupLeft = content.iconX();
        int groupRight = content.textX() + 20;
        assertTrue(Math.abs((groupLeft - 3) - (60 - 3 - groupRight)) <= 1,
                "the group sits in the middle of the padded key");
    }

    @Test
    void iconOnlyKeyCentresTheIconOnTheWholeKey() {
        TacticalButtonStyle.Content selected = TacticalButtonStyle.content(10, 4, 26, 20, 5,
                true, 0, 0, TextFit.Align.CENTER);

        assertEquals(TacticalIcon.centeredStart(10, 26), selected.iconX(),
                "the selection stripe padding does not shift an icon-only key");
        assertEquals(TacticalIcon.centeredStart(4, 20), selected.iconY());
    }

    @Test
    void leftAlignedIconStartsAtThePadding() {
        TacticalButtonStyle.Content content = TacticalButtonStyle.content(0, 0, 100, 18, 5,
                true, 30, 0, TextFit.Align.LEFT);

        assertEquals(5, content.iconX());
        assertEquals(5 + TacticalIcon.ADVANCE, content.textX());
    }

    @Test
    void badgeAndIconShareTheRoomWithTheLabel() {
        TacticalButtonStyle.Content content = TacticalButtonStyle.content(0, 0, 80, 14, 3,
                true, 200, 20, TextFit.Align.CENTER);

        assertEquals(80 - 3 - 3 - TacticalIcon.ADVANCE - 22, content.textRoom());
        assertEquals(3, content.iconX(), "a label that fills the room starts at the padding");
    }

    @Test
    void optionsKeepTheIconlessFormAndTrackIconOnly() {
        TacticalButtonStyle.Options plain = new TacticalButtonStyle.Options(TextFit.Align.LEFT,
                null, TacticalBoardTheme.MUTED, true);

        assertEquals(null, plain.icon());
        assertFalse(plain.iconOnly());
        assertTrue(plain.withIconOnly(TacticalIcon.CLOSE).iconOnly());
        assertFalse(plain.withIconOnly(null).iconOnly());
        assertFalse(plain.withIconOnly(TacticalIcon.CLOSE).withIcon(TacticalIcon.CHECK).iconOnly());
        assertEquals(TacticalIcon.CLOSE,
                plain.withIconOnly(TacticalIcon.CLOSE).withFocusRing(false).icon());
        assertTrue(plain.withIconOnly(TacticalIcon.CLOSE).withFocusRing(false).iconOnly());
    }

    private static Look resolveActive(Variant variant, boolean hovered) {
        return TacticalButtonStyle.resolve(true, false, false, variant, hovered, false);
    }
}
