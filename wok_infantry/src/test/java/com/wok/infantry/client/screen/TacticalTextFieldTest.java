package com.wok.infantry.client.screen;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalTextFieldTest {
    private static TacticalTextField field() {
        // The font is only used while rendering; a unit test never renders.
        return new TacticalTextField(null, 0, 0, 120, 18, Component.literal("装备 ID"));
    }

    @Test
    void outlineFollowsFocusErrorAndEditability() {
        TacticalTextField field = field();

        assertEquals(TacticalBoardTheme.INPUT_EDGE, field.edgeColor());
        field.setFocused(true);
        assertEquals(TacticalBoardTheme.SELECT_B, field.edgeColor(), "typing: blue outline");
        field.setError(Component.literal("ID 已存在"));
        assertEquals(TacticalBoardTheme.DANGER_B, field.edgeColor(), "an error wins over focus");
        field.setError(null);
        field.setFocused(false);
        field.setEditable(false);
        assertFalse(field.isEditableField());
        assertEquals(TacticalBoardTheme.WELL_EDGE, field.edgeColor());
    }

    @Test
    void errorReasonBecomesTheTooltipAndTheScreenTooltipComesBack() {
        TacticalTextField field = field();
        Tooltip own = Tooltip.create(Component.literal("只能用 a-z、0-9、_ . -"));
        field.setTooltip(own);

        field.setError(Component.literal("ID 已存在"));
        assertTrue(field.hasError());
        assertEquals("ID 已存在", field.error().getString());
        Tooltip errorTooltip = field.getTooltip();
        assertFalse(errorTooltip == own, "the reason is shown while the error lasts");

        Tooltip newer = Tooltip.create(Component.literal("新提示"));
        field.setTooltip(newer);
        assertSame(errorTooltip, field.getTooltip(), "a new screen tooltip waits for the error");

        field.setError(Component.empty());
        assertFalse(field.hasError(), "an empty reason clears the error");
        assertNull(field.error());
        assertSame(newer, field.getTooltip(), "the screen's tooltip is back");
    }

    @Test
    void placeholderFollowsThePaletteTheFieldIsDrawnIn() {
        // Built in init (A palette); every frame re-reads the colours, so the hint is not the A
        // gray inside the pale Neutral well and is A again outside the scope.
        TacticalTextField field = field().placeholder(Component.literal("搜索装备"));
        assertEquals(TacticalPalette.A.get(PaletteToken.FAINT), field.placeholderColor());
        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.NEUTRAL)) {
            field.syncColors();
            assertEquals(TacticalPalette.NEUTRAL.get(PaletteToken.FAINT), field.placeholderColor());
        }
        field.syncColors();
        assertEquals(TacticalPalette.A.get(PaletteToken.FAINT), field.placeholderColor());
    }

    @Test
    void cursorBarTakesTheTextOnWellColourOfThePaletteItIsDrawnIn() {
        // Vanilla's fixed light-gray bar vanishes in the pale Neutral well; the field's cursor is
        // its own text colour, read while drawing.
        TacticalTextField field = field();
        assertEquals(TacticalPalette.A.get(PaletteToken.LIGHT), field.cursorColor());
        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.NEUTRAL)) {
            int cursor = field.cursorColor();
            assertEquals(TacticalPalette.NEUTRAL.get(PaletteToken.LIGHT), cursor);
            assertFalse(cursor == TacticalTextField.VANILLA_CURSOR);
            int well = TacticalPalette.NEUTRAL.get(PaletteToken.WELL);
            assertTrue(contrast(TacticalTextField.VANILLA_CURSOR, well) < 1.5,
                    "the vanilla gray all but disappears in the pale Neutral well");
            assertTrue(contrast(cursor, well) >= 4.5, "dark ink on the pale Neutral well");
        }
        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.CAESAR)) {
            assertEquals(TacticalPalette.CAESAR.get(PaletteToken.LIGHT), field.cursorColor());
        }
        assertEquals(TacticalPalette.A.get(PaletteToken.LIGHT), field.cursorColor());
    }

    /** WCAG contrast ratio of two opaque colours. */
    private static double contrast(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** Relative luminance (sRGB) of an ARGB colour, 0–1. */
    private static double luminance(int argb) {
        double r = channel((argb >> 16) & 0xFF);
        double g = channel((argb >> 8) & 0xFF);
        double b = channel(argb & 0xFF);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(int value) {
        double c = value / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    @Test
    void textFieldIsStillAVanillaEditBox() {
        TacticalTextField field = field();
        field.setMaxLength(8);
        field.setValue("assault_rifle");

        assertEquals("assault_", field.getValue(), "vanilla max length still applies");
        assertEquals(112, field.getInnerWidth(), "bordered geometry: 4px padding on each side");
    }
}
