package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalPaletteTest {
    @Test
    void everyTokenNamesAPublicThemeFieldAndAReadsItsValue() throws ReflectiveOperationException {
        for (PaletteToken token : PaletteToken.values()) {
            Field field = TacticalBoardTheme.class.getField(token.name());
            int modifiers = field.getModifiers();
            assertEquals(int.class, field.getType(), token.name());
            assertTrue(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers), token.name());
            assertEquals(field.getInt(null), TacticalPalette.A.get(token), token.name());
        }
    }

    @Test
    void newTokensCarryTheirASchemeValues() {
        // DEVICE_PORT_PLAN 2.2: ON_FILL keeps A unchanged, the other two match the preview SHARED.
        assertEquals(TacticalBoardTheme.LIGHT, TacticalBoardTheme.ON_FILL);
        assertEquals(0xFFEEF3F0, TacticalPalette.A.get(PaletteToken.ON_FILL));
        assertEquals(0xFF3A0F0C, TacticalPalette.A.get(PaletteToken.DANGER_DEEP));
        assertEquals(0xFF844600, TacticalPalette.A.get(PaletteToken.ACCENT_TEXT));
    }

    @Test
    void fixedColoursNeverBecomeSwappable() {
        // World shade, modal dim, hatch, badge insets, HUD plates and friend / foe inks, map and
        // grid colours and the deprecated aliases stay A for every faction (plan 2.2).
        EnumSet<PaletteToken> tokens = EnumSet.allOf(PaletteToken.class);
        for (String name : new String[]{"WORLD_SHADE", "MODAL_DIM", "HATCH", "BADGE_ON_SELECT",
                "BADGE_ON_CARD", "BODY_DEAD", "DEVICE_EDGE", "HUD_PLATE", "HUD_PLATE_SOLID",
                "HUD_EDGE", "HUD_FRIENDLY", "HUD_HOSTILE", "HOSTILE", "MAP_FRIENDLY",
                "MAP_HOSTILE", "MAP_ICON_HOSTILE", "MAP_WASH", "GRID_MINOR", "GRID_MAJOR",
                "DIVIDER", "DEVICE_FRAME", "SELECTED", "LIGHT_TEXT", "FRIENDLY", "INSET"}) {
            assertTrue(tokens.stream().noneMatch(token -> token.name().equals(name)), name);
        }
    }

    @Test
    void closingAPushLeavesTheASchemeForTheHud() {
        try (TacticalPalette.Applied ignored = TacticalPalette.push(
                TacticalLivery.Livery.CAESAR.palette(TacticalLivery.Scope.MAP))) {
            assertTrue(TacticalPalette.active() != null);
        }
        assertSame(TacticalPalette.A, TacticalPalette.active());
        assertEquals(0xFFBBC5C1, TacticalBoardTheme.BOARD);
        assertEquals(0xFFEEF3F0, TacticalBoardTheme.LIGHT);
    }
}
