package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalButtonStyle.Look;
import com.wok.infantry.client.screen.TacticalButtonStyle.Palette;
import com.wok.infantry.client.screen.TacticalButtonStyle.State;
import com.wok.infantry.client.screen.TacticalButtonStyle.Variant;
import net.minecraft.network.chat.Component;
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
        assertEquals(TacticalBoardTheme.ON_FILL, palette.text(),
                "text on the solid red fill stays light when a faction's LIGHT turns dark");
        assertEquals(TacticalBoardTheme.DANGER_DEEP, palette.stripes(),
                "the hazard tab turns into dark stripes on the solid red key");
        assertFalse(palette.raised(), "an armed key is flat like the other solid fills");
    }

    @Test
    void dangerKeyIsALightKeyWithRedOutlineTextAndHazardStripes() {
        Palette danger = TacticalButtonStyle.palette(resolveActive(Variant.DANGER, false));

        assertEquals(TacticalBoardTheme.CARD, danger.fill());
        assertEquals(TacticalBoardTheme.DANGER, danger.edge());
        assertEquals(TacticalBoardTheme.DANGER, danger.text());
        assertEquals(TacticalBoardTheme.DANGER, danger.stripes());
        assertTrue(danger.hazard());
        assertEquals(TacticalButtonStyle.NONE, danger.bar(),
                "the old 2px red bar is replaced by the hazard tab");
        assertTrue(danger.raised(), "a resting danger key is still a raised light key");
        assertEquals(TacticalBoardTheme.CARD_LIP, danger.lip());
    }

    @Test
    void hoveringADangerKeyArmsIt() {
        Look hovered = resolveActive(Variant.DANGER, true);

        assertEquals(State.DANGER_ARMED, hovered.state());
        assertEquals(TacticalButtonStyle.palette(new Look(State.DANGER_ARMED, false)).fill(),
                TacticalButtonStyle.palette(hovered).fill());
    }

    @Test
    void successWritesOnFillInsideTheSuccessEdge() {
        Palette success = TacticalButtonStyle.palette(resolveActive(Variant.SUCCESS, false));
        Palette hovered = TacticalButtonStyle.palette(resolveActive(Variant.SUCCESS, true));

        assertEquals(TacticalBoardTheme.SUCCESS, success.fill());
        assertEquals(TacticalBoardTheme.SUCCESS_HOVER, hovered.fill());
        assertEquals(TacticalBoardTheme.SUCCESS_EDGE, success.edge());
        assertEquals(TacticalBoardTheme.ON_FILL, success.text());
        assertEquals(TacticalButtonStyle.NONE, success.bar(), "success has no left bar");
        assertFalse(success.hazard());
        assertFalse(success.raised());
    }

    @Test
    void selectedKeyIsSolidWithTheLightBarAndBrightensOnHover() {
        Palette selected = TacticalButtonStyle.palette(new Look(State.SELECTED, false));
        Palette hovered = TacticalButtonStyle.palette(new Look(State.SELECTED, true));

        assertEquals(TacticalBoardTheme.SELECT, selected.fill());
        assertEquals(TacticalBoardTheme.SELECT_HOVER, hovered.fill());
        assertEquals(TacticalBoardTheme.SELECT_EDGE, selected.edge());
        assertEquals(TacticalBoardTheme.SELECT_BAR, selected.bar());
        assertFalse(selected.raised(), "no raised bevel on the selection");
        assertFalse(selected.hazard());
    }

    @Test
    void sevenStatesAndCurrentAreVisuallyDistinct() {
        Set<String> looks = new HashSet<>();
        for (State state : EnumSet.of(State.NORMAL, State.HOVER, State.SELECTED,
                State.DISABLED, State.DANGER, State.DANGER_ARMED, State.SUCCESS, State.CONTROL)) {
            Palette palette = TacticalButtonStyle.palette(new Look(state, false));
            assertTrue(looks.add(palette.fill() + "/" + palette.edge() + "/" + palette.text()
                    + "/" + palette.bar() + "/" + palette.underline() + "/" + palette.hatch()
                    + "/" + palette.stripes() + "/" + palette.lip()),
                    state + " must not look like another state");
        }
    }

    @Test
    void everyStateCarriesAShapeCueBesidesColour() {
        // UI rule 6: selected = bar, danger = hazard tab, disabled = hatch, control = underline,
        // success = check icon (see successGetsACheckWhenItFits), normal/hover = raised bevel.
        assertTrue(TacticalButtonStyle.palette(new Look(State.SELECTED, false)).bar()
                != TacticalButtonStyle.NONE);
        assertTrue(TacticalButtonStyle.palette(new Look(State.DANGER, false)).hazard());
        assertTrue(TacticalButtonStyle.palette(new Look(State.DANGER_ARMED, false)).hazard());
        assertTrue(TacticalButtonStyle.palette(new Look(State.DISABLED, false)).hatch());
        assertTrue(TacticalButtonStyle.palette(new Look(State.CONTROL, false)).underline()
                != TacticalButtonStyle.NONE);
        assertTrue(TacticalButtonStyle.palette(new Look(State.NORMAL, false)).raised());
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
    void barAndHazardWidthsFollowTheKeyHeight() {
        assertEquals(2, TacticalButtonStyle.barWidth(14));
        assertEquals(2, TacticalButtonStyle.barWidth(15));
        assertEquals(3, TacticalButtonStyle.barWidth(16));
        assertEquals(3, TacticalButtonStyle.barWidth(20));
        assertEquals(3, TacticalButtonStyle.hazardWidth(15));
        assertEquals(4, TacticalButtonStyle.hazardWidth(16));
    }

    @Test
    void labelPaddingClearsTheSelectionBarAndTheHazardTab() {
        Palette selected = TacticalButtonStyle.palette(new Look(State.SELECTED, false));
        Palette current = TacticalButtonStyle.palette(new Look(State.CURRENT, false));
        Palette danger = TacticalButtonStyle.palette(new Look(State.DANGER, false));
        Palette armed = TacticalButtonStyle.palette(new Look(State.DANGER_ARMED, false));

        assertEquals(6, TacticalButtonStyle.labelPadLeft(selected, 18), "barW 3 + 3");
        assertEquals(5, TacticalButtonStyle.labelPadLeft(selected, 14), "barW 2 + 3");
        assertEquals(6, TacticalButtonStyle.labelPadLeft(current, 16));
        assertEquals(7, TacticalButtonStyle.labelPadLeft(danger, 18), "hz 4 + 3");
        assertEquals(6, TacticalButtonStyle.labelPadLeft(danger, 14), "hz 3 + 3");
        assertEquals(7, TacticalButtonStyle.labelPadLeft(armed, 20));
        for (State state : EnumSet.of(State.NORMAL, State.HOVER, State.DISABLED, State.SUCCESS,
                State.CONTROL)) {
            assertEquals(3, TacticalButtonStyle.labelPadLeft(
                    TacticalButtonStyle.palette(new Look(state, false)), 18), state.name());
        }
    }

    @Test
    void hazardTabFollowsThePreviewStripePattern() {
        int left = 10;
        int top = 20;
        int bottom = 40;
        int width = TacticalButtonStyle.hazardWidth(bottom - top);
        Set<String> drawn = new HashSet<>();
        int runs = TacticalButtonStyle.hazardRuns(left, top, bottom, width, (l, r, y) -> {
            assertTrue(r > l, "runs are never empty");
            for (int x = l; x < r; x++) {
                assertTrue(drawn.add(x + "," + y), "runs never overlap");
            }
        });
        Set<String> expected = new HashSet<>();
        for (int y = top + 1; y < bottom - 1; y++) {
            for (int x = left + 1; x < left + 1 + width; x++) {
                if (((x - left) + (y - top)) % 4 < 2) {
                    expected.add(x + "," + y);
                }
            }
        }

        assertEquals(expected, drawn, "preview UIX hazardTab: ((x − l) + (y − t)) % 4 < 2");
        assertTrue(runs <= TacticalButtonStyle.MAX_HAZARD_FILLS, runs + " fills");
    }

    @Test
    void hazardTabStaysWithinItsFillBudgetAtEveryHeight() {
        for (int height = 0; height <= 80; height++) {
            int width = TacticalButtonStyle.hazardWidth(height);
            int[] lowest = {Integer.MAX_VALUE};
            int[] highest = {Integer.MIN_VALUE};
            int runs = TacticalButtonStyle.hazardRuns(0, 0, height, width, (l, r, y) -> {
                assertTrue(l >= 1 && r <= 1 + width, "inside the left edge, " + width + "px wide");
                lowest[0] = Math.min(lowest[0], y);
                highest[0] = Math.max(highest[0], y);
            });
            assertTrue(runs <= TacticalButtonStyle.MAX_HAZARD_FILLS,
                    height + "px key: " + runs + " fills");
            if (runs > 0) {
                assertTrue(lowest[0] >= 1 && highest[0] <= height - 2, "inside the outline");
            }
        }
        assertEquals(0, TacticalButtonStyle.hazardRuns(0, 0, 2, 4, (l, r, y) -> { }));
        assertEquals(0, TacticalButtonStyle.hazardRuns(0, 0, 20, 0, (l, r, y) -> { }));
    }

    @Test
    void everyKeyInUseGetsAFullHeightHazardTab() {
        // Keys up to MAX_HAZARD_ROWS + 2 tall (every btnH and the roomy 20px keys) are striped on
        // every row inside the outline; only taller keys get a centred band.
        for (int height = 3; height <= TacticalButtonStyle.MAX_HAZARD_ROWS + 2; height++) {
            Set<Integer> rows = new HashSet<>();
            TacticalButtonStyle.hazardRuns(0, 0, height, TacticalButtonStyle.hazardWidth(height),
                    (l, r, y) -> rows.add(y));
            assertEquals(height - 2, rows.size(), height + "px key");
        }
        Set<Integer> tall = new HashSet<>();
        TacticalButtonStyle.hazardRuns(0, 0, 40, 4, (l, r, y) -> tall.add(y));
        assertEquals(TacticalButtonStyle.MAX_HAZARD_ROWS, tall.size());
        assertEquals(7, tall.stream().mapToInt(Integer::intValue).min().orElseThrow(),
                "38 rows inside the outline, 25 kept: 6 skipped above");
    }

    @Test
    void tightHazardTabIsOneRunPerRow() {
        int[] perRow = new int[14];
        int runs = TacticalButtonStyle.hazardRuns(0, 0, 14, TacticalButtonStyle.hazardWidth(14),
                (l, r, y) -> perRow[y]++);

        assertEquals(12, runs);
        for (int y = 1; y < 13; y++) {
            assertEquals(1, perRow[y], "row " + y);
        }
    }

    @Test
    void countBadgeInsetAndInkFollowTheKeyState() {
        assertEquals(TacticalButtonStyle.BADGE_INSET_SELECTED,
                TacticalButtonStyle.badgeInset(State.SELECTED));
        assertEquals(TacticalButtonStyle.BADGE_INSET_SELECTED,
                TacticalButtonStyle.badgeInset(State.CURRENT));
        assertEquals(0x40000000, TacticalButtonStyle.BADGE_INSET_SELECTED,
                "a darker inset on the selection");
        assertEquals(0x33FFFFFF, TacticalButtonStyle.badgeInset(State.DISABLED),
                "a lighter inset on the gray disabled key");
        assertEquals(TacticalBoardTheme.BADGE_ON_CARD, TacticalButtonStyle.badgeInset(State.NORMAL));
        assertEquals(0x22000000, TacticalButtonStyle.badgeInset(State.SUCCESS));
        assertEquals(0x22000000, TacticalButtonStyle.badgeInset(null));

        assertEquals(TacticalBoardTheme.ON_SELECT,
                TacticalButtonStyle.badgeText(State.SELECTED, TacticalBoardTheme.SUCCESS_B));
        assertEquals(TacticalBoardTheme.ON_SELECT,
                TacticalButtonStyle.badgeText(State.CURRENT, TacticalButtonStyle.NONE));
        assertEquals(TacticalBoardTheme.ON_FILL,
                TacticalButtonStyle.badgeText(State.SUCCESS, TacticalBoardTheme.MUTED));
        assertEquals(TacticalBoardTheme.ON_FILL,
                TacticalButtonStyle.badgeText(State.DANGER_ARMED, TacticalBoardTheme.MUTED));
        assertEquals(TacticalBoardTheme.DISABLED_TEXT,
                TacticalButtonStyle.badgeText(State.DISABLED, TacticalBoardTheme.SUCCESS_B),
                "a disabled key carries no semantic colour");
        assertEquals(TacticalBoardTheme.MUTED,
                TacticalButtonStyle.badgeText(State.NORMAL, TacticalButtonStyle.NONE));
        assertEquals(TacticalBoardTheme.SUCCESS,
                TacticalButtonStyle.badgeText(State.HOVER, TacticalBoardTheme.SUCCESS));
    }

    @Test
    void badgeColourDefaultsToTheRenderTimeSentinel() {
        assertEquals(TacticalButtonStyle.NONE, TacticalButtonStyle.Options.DEFAULT.badgeColor(),
                "MUTED is read when the key is drawn, inside the faction palette");
        BattleUiButton plain = (BattleUiButton) BattleUiButton.builder(Component.literal("投票"),
                ignored -> { }).bounds(0, 0, 40, 18).build();
        BattleUiButton counted = (BattleUiButton) BattleUiButton.builder(Component.literal("投票"),
                        ignored -> { })
                .badge(Component.literal("3"), TacticalBoardTheme.SUCCESS).bounds(0, 0, 40, 18)
                .build();

        assertEquals(TacticalButtonStyle.NONE, plain.badgeColor());
        assertEquals(TacticalBoardTheme.SUCCESS, counted.badgeColor());
    }

    @Test
    void successGetsACheckWhenItFits() {
        // Preview UIX.button: label width + 11 <= key width - 6.
        assertTrue(TacticalButtonStyle.autoCheck(State.SUCCESS, false, 30, 47, 0));
        assertFalse(TacticalButtonStyle.autoCheck(State.SUCCESS, false, 31, 47, 0));
        assertFalse(TacticalButtonStyle.autoCheck(State.SUCCESS, true, 10, 100, 0),
                "a key with its own icon keeps it");
        assertFalse(TacticalButtonStyle.autoCheck(State.SUCCESS, false, 0, 100, 0),
                "no label, no check");
        assertFalse(TacticalButtonStyle.autoCheck(State.SUCCESS, false, 30, 60, 12),
                "the badge takes room too: the label must never be shortened for the check");
        for (State state : State.values()) {
            if (state != State.SUCCESS) {
                assertFalse(TacticalButtonStyle.autoCheck(state, false, 10, 100, 0),
                        state.name());
            }
        }
    }

    @Test
    void iconOnlyDangerKeyMovesItsIconOffTheHazardTab() {
        TacticalButtonStyle.Content plain = TacticalButtonStyle.content(0, 0, 20, 20, 3, true, 0, 0,
                TextFit.Align.CENTER);
        TacticalButtonStyle.Content danger = TacticalButtonStyle.content(0, 0, 20, 20, 7, true, 0,
                0, TextFit.Align.CENTER, TacticalButtonStyle.HAZARD_ICON_SHIFT);

        assertEquals(TacticalIcon.centeredStart(0, 20), plain.iconX());
        assertEquals(plain.iconX() + 2, danger.iconX());
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
        assertEquals(TacticalBoardTheme.BORDER_DARK,
                TacticalButtonStyle.cardEdge(TacticalButtonStyle.CardState.NORMAL, true));
        assertEquals(TacticalBoardTheme.SELECT_EDGE,
                TacticalButtonStyle.cardEdge(TacticalButtonStyle.CardState.SELECTED, true));
        assertEquals(TacticalBoardTheme.BORDER,
                TacticalButtonStyle.cardEdge(TacticalButtonStyle.CardState.DISABLED, true));
    }

    @Test
    void hoveringASelectedCardLightsItOneStep() {
        assertEquals(TacticalBoardTheme.SELECT,
                TacticalButtonStyle.cardFill(TacticalButtonStyle.CardState.SELECTED, false));
        assertEquals(TacticalBoardTheme.SELECT_HOVER,
                TacticalButtonStyle.cardFill(TacticalButtonStyle.CardState.SELECTED, true));
        assertEquals(TacticalBoardTheme.CARD_HOVER,
                TacticalButtonStyle.cardFill(TacticalButtonStyle.CardState.NORMAL, true));
        assertEquals(TacticalBoardTheme.CARD_HOVER,
                TacticalButtonStyle.cardFill(TacticalButtonStyle.CardState.HOVER, false));
        assertEquals(TacticalBoardTheme.CARD,
                TacticalButtonStyle.cardFill(TacticalButtonStyle.CardState.NORMAL, false));
        assertEquals(TacticalBoardTheme.CARD_DISABLED,
                TacticalButtonStyle.cardFill(TacticalButtonStyle.CardState.DISABLED, true),
                "disabled cards never react to hover");
        assertEquals(TacticalBoardTheme.CARD, TacticalButtonStyle.cardFill(null, false));
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
