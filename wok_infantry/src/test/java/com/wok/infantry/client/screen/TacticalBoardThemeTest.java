package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class TacticalBoardThemeTest {
    @Test
    void deprecatedBoardAliasesPointAtTheFinalisedTokens() {
        assertEquals(TacticalBoardTheme.FRAME, TacticalBoardTheme.DEVICE_FRAME);
        assertEquals(TacticalBoardTheme.FRAME_MID, TacticalBoardTheme.DEVICE_MID);
        assertEquals(TacticalBoardTheme.SELECT, TacticalBoardTheme.SELECTED);
        assertEquals(TacticalBoardTheme.SELECT_HOVER, TacticalBoardTheme.SELECTED_HOVER);
        assertEquals(TacticalBoardTheme.LIGHT, TacticalBoardTheme.LIGHT_TEXT);
        assertEquals(TacticalBoardTheme.MUTED, TacticalBoardTheme.MUTED_TEXT);
        assertEquals(TacticalBoardTheme.BEVEL, TacticalBoardTheme.BORDER_BRIGHT);
    }

    @Test
    void deprecatedHudAliasesPointAtTheBrightTokens() {
        assertEquals(TacticalBoardTheme.ACCENT_B, BattleUiTheme.ACCENT);
        assertEquals(TacticalBoardTheme.HUD_FRIENDLY, BattleUiTheme.FRIENDLY);
        assertEquals(TacticalBoardTheme.LIGHT, BattleUiTheme.TEXT);
        assertEquals(TacticalBoardTheme.LIGHT_MUTED, BattleUiTheme.MUTED_TEXT);
        assertEquals(TacticalBoardTheme.DANGER_B, BattleUiTheme.DANGER);
        assertEquals(TacticalBoardTheme.SUCCESS_B, BattleUiTheme.SUCCESS);
    }

    @Test
    void deprecatedNamesAreMarkedDeprecated() throws ReflectiveOperationException {
        for (String name : new String[]{"DEVICE_FRAME", "DEVICE_MID",
                "SELECTED", "SELECTED_HOVER", "LIGHT_TEXT", "MUTED_TEXT", "BORDER_BRIGHT",
                "FRIENDLY", "INSET", "DEVICE_SHADOW"}) {
            assertTrue(TacticalBoardTheme.class.getField(name).isAnnotationPresent(Deprecated.class),
                    name);
        }
        for (String name : new String[]{"ACCENT", "FRIENDLY", "TEXT", "MUTED_TEXT",
                "DANGER", "SUCCESS", "BACKGROUND", "PANEL", "PANEL_ALT", "PANEL_HOVER", "BORDER",
                "FRIENDLY_DARK"}) {
            assertTrue(BattleUiTheme.class.getField(name).isAnnotationPresent(Deprecated.class),
                    name);
        }
    }

    @Test
    void previewTokenNamesKeepThePreviewMeaning() throws ReflectiveOperationException {
        // ui.js: DEVICE_EDGE is the opaque frame line, EDGE the translucent board separator.
        assertEquals(0xFF77817F, TacticalBoardTheme.DEVICE_EDGE);
        assertFalse(TacticalBoardTheme.class.getField("DEVICE_EDGE")
                .isAnnotationPresent(Deprecated.class));
        assertEquals(0x40243032, TacticalBoardTheme.EDGE);
        assertEquals(TacticalBoardTheme.EDGE, TacticalBoardTheme.DIVIDER);
        // Preview FRIENDLY / HOSTILE are the HUD colours.
        assertEquals(0xFF6FB1E6, TacticalBoardTheme.HUD_FRIENDLY);
        assertEquals(0xFFE8695D, TacticalBoardTheme.HOSTILE);
        assertEquals(TacticalBoardTheme.HUD_HOSTILE, TacticalBoardTheme.HOSTILE);
    }

    @Test
    void hudPublicConstantsStayAvailableUntilTheHudMigrates() throws ReflectiveOperationException {
        for (String name : new String[]{"BACKGROUND", "PANEL", "PANEL_ALT", "PANEL_HOVER",
                "BORDER", "FRIENDLY_DARK", "ACCENT", "FRIENDLY", "TEXT", "MUTED_TEXT", "DANGER",
                "SUCCESS"}) {
            int modifiers = BattleUiTheme.class.getField(name).getModifiers();
            assertTrue(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers), name);
        }
    }

    @Test
    void tabletPaletteMatchesTheChosenPreview() {
        // ui-preview/kit/ui.js token table T (A · 战术平板).
        assertEquals(0xFFBBC5C1, TacticalBoardTheme.BOARD);
        assertEquals(0xFFA9B5B1, TacticalBoardTheme.BOARD_ALT);
        assertEquals(0xFFD6DDD9, TacticalBoardTheme.CARD);
        assertEquals(0xFFE6EBE8, TacticalBoardTheme.CARD_HOVER);
        assertEquals(0xFFA3ADAB, TacticalBoardTheme.CARD_DISABLED);
        assertEquals(0xFF4E5C5A, TacticalBoardTheme.BORDER);
        assertEquals(0xFF2E3837, TacticalBoardTheme.BORDER_DARK);
        assertEquals(0xFF1B262A, TacticalBoardTheme.TEXT);
        assertEquals(0xFF2E679C, TacticalBoardTheme.SELECT);
        assertEquals(0xFF8CC3EE, TacticalBoardTheme.SELECT_BAR);
        assertEquals(0xFFBE7A1E, TacticalBoardTheme.ACCENT);
        assertEquals(0xFFB0443C, TacticalBoardTheme.DANGER);
        assertEquals(0xFF3B7A57, TacticalBoardTheme.SUCCESS);
        assertEquals(0xFFF0A63A, TacticalBoardTheme.ACCENT_B);
        assertEquals(0xB3121A1D, TacticalBoardTheme.HUD_PLATE);
    }

    @Test
    void lipDividerAndEmptySlotUseTheIntendedColours() {
        // Decided 2026-10-04: draw these three as intended, not as the commented-out preview line.
        assertEquals(0xFFBAC4C0, TacticalBoardTheme.CARD_LIP);
        assertEquals(0x40243032, TacticalBoardTheme.EDGE);
        assertEquals(0xFF3A4547, TacticalBoardTheme.WELL_EDGE);
    }

    @Test
    void sectionAdjustAndAccentShareOneOrangeInTheTabletScheme() {
        assertEquals(TacticalBoardTheme.ACCENT, TacticalBoardTheme.SECTION);
        assertEquals(TacticalBoardTheme.ACCENT, TacticalBoardTheme.ADJUST);
        assertEquals(TacticalBoardTheme.ACCENT_B, TacticalBoardTheme.SECTION_B);
        assertEquals(TacticalBoardTheme.ACCENT_B, TacticalBoardTheme.ADJUST_B);
        assertEquals(TacticalBoardTheme.ACCENT_SOFT, TacticalBoardTheme.ADJUST_SOFT);
    }

    @Test
    void everyTokenIsAPublicConstant() throws IllegalAccessException {
        assertTrue(Modifier.isPublic(TacticalBoardTheme.class.getModifiers()),
                "HUD and map packages must be able to use the tokens");
        int tokens = 0;
        for (Field field : TacticalBoardTheme.class.getDeclaredFields()) {
            if (field.getType() != int.class || field.isSynthetic()) {
                continue;
            }
            int modifiers = field.getModifiers();
            assertTrue(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
                    && Modifier.isFinal(modifiers), field.getName());
            tokens++;
        }
        assertTrue(tokens >= 90, "expected the full tablet token set, found " + tokens);
    }
}
