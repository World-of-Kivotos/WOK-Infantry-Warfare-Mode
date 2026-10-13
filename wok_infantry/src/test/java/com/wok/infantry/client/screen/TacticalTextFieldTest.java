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
    void textFieldIsStillAVanillaEditBox() {
        TacticalTextField field = field();
        field.setMaxLength(8);
        field.setValue("assault_rifle");

        assertEquals("assault_", field.getValue(), "vanilla max length still applies");
        assertEquals(112, field.getInnerWidth(), "bordered geometry: 4px padding on each side");
    }
}
