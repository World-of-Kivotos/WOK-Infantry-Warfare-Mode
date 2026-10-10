package com.wok.infantry.client.screen;

import com.google.gson.JsonObject;
import com.wok.infantry.client.screen.TacticalLivery.Livery;
import com.wok.infantry.client.screen.TacticalLivery.Scope;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The faction palettes: every value against the preview export
 * ({@code ui_palette/p3_palette.json}, written by {@code ui-preview/tools/export-palette.mjs}),
 * the derived tokens, and the push scope that keeps the A values for everything drawn outside a
 * tablet screen.
 */
class TacticalPaletteTest {
    /** Tokens no livery of the preview draws (old header tabs, footer key caps): they keep A. */
    private static final Set<PaletteToken> A_ONLY = EnumSet.of(PaletteToken.TAB_HOVER,
            PaletteToken.KEYCAP_EDGE);

    @AfterEach
    void backToA() {
        TacticalPalette.A.apply();
    }

    // ---- token ↔ field ------------------------------------------------------------------------

    @Test
    void everyTokenNamesASwappableThemeFieldAndAReadsItsValue() throws ReflectiveOperationException {
        for (PaletteToken token : PaletteToken.values()) {
            Field field = TacticalBoardTheme.class.getField(token.name());
            int modifiers = field.getModifiers();
            assertEquals(int.class, field.getType(), token.name());
            assertTrue(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers), token.name());
            assertFalse(Modifier.isFinal(modifiers), token.name() + " must be swappable");
            assertEquals(field.getInt(null), TacticalPalette.A.get(token), token.name());
        }
    }

    @Test
    void applyWritesEveryFieldAndAReadsBack() throws ReflectiveOperationException {
        TacticalPalette.NEUTRAL.apply();
        assertSame(TacticalPalette.NEUTRAL, TacticalPalette.active());
        for (PaletteToken token : PaletteToken.values()) {
            assertEquals(TacticalPalette.NEUTRAL.get(token),
                    TacticalBoardTheme.class.getField(token.name()).getInt(null), token.name());
            assertEquals(TacticalPalette.NEUTRAL.get(token), TacticalPalette.themeValue(token),
                    token.name());
        }
        TacticalPalette.A.apply();
        for (PaletteToken token : PaletteToken.values()) {
            assertEquals(TacticalPalette.A.get(token), TacticalPalette.themeValue(token),
                    token.name());
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
                "BADGE_ON_CARD", "BODY_DEAD", "DEVICE_EDGE", "PANEL_SHADOW", "PANEL_HIGHLIGHT",
                "HUD_PLATE", "HUD_PLATE_SOLID", "HUD_EDGE", "HUD_FRIENDLY", "HUD_HOSTILE",
                "HOSTILE", "MAP_FRIENDLY", "MAP_HOSTILE", "MAP_ICON_HOSTILE", "MAP_WASH",
                "GRID_MINOR", "GRID_MAJOR", "DIVIDER", "DEVICE_FRAME", "SELECTED", "LIGHT_TEXT",
                "FRIENDLY", "INSET"}) {
            assertTrue(tokens.stream().noneMatch(token -> token.name().equals(name)), name);
        }
    }

    // ---- values against the preview export ------------------------------------------------------

    @Test
    void aMatchesThePreviewIncludingTheFixedTokens() throws ReflectiveOperationException {
        JsonObject a = PaletteExport.a();
        for (String name : a.keySet()) {
            int expected = PaletteExport.color(a, name);
            PaletteToken token = PaletteExport.token(name);
            int actual = token != null ? TacticalPalette.A.get(token)
                    : TacticalBoardTheme.class.getField(name).getInt(null);
            assertEquals(hex(expected), hex(actual), "A " + name);
        }
        for (PaletteToken token : PaletteToken.values()) {
            assertTrue(a.has(token.name()) || A_ONLY.contains(token),
                    token + " is missing from the preview export");
        }
    }

    @Test
    void everyLiveryAndScopeMatchesThePreview() {
        for (Livery livery : Livery.values()) {
            for (Scope scope : Scope.values()) {
                JsonObject table = PaletteExport.table(livery, scope);
                TacticalPalette palette = livery.palette(scope);
                for (PaletteToken token : PaletteToken.values()) {
                    String where = livery + "/" + scope + " " + token;
                    if (table.has(token.name())) {
                        assertEquals(hex(PaletteExport.color(table, token.name())),
                                hex(palette.get(token)), where);
                    } else {
                        assertTrue(A_ONLY.contains(token), where + " is missing from the export");
                        assertEquals(hex(TacticalPalette.A.get(token)), hex(palette.get(token)),
                                where + " keeps A");
                    }
                }
            }
        }
    }

    @Test
    void thePreviewNeverRecoloursAFixedToken() {
        // A livery colour the preview changes must be a PaletteToken; everything else stays A.
        JsonObject a = PaletteExport.a();
        for (Livery livery : Livery.values()) {
            for (Scope scope : Scope.values()) {
                JsonObject table = PaletteExport.table(livery, scope);
                assertEquals(a.keySet(), table.keySet(), livery + "/" + scope);
                for (String name : table.keySet()) {
                    if (PaletteExport.token(name) == null) {
                        assertEquals(a.get(name).getAsString(), table.get(name).getAsString(),
                                livery + "/" + scope + " recolours the fixed " + name);
                    }
                }
            }
        }
    }

    // ---- derived tokens and scopes ----------------------------------------------------------------

    @Test
    void liveryTokensTheComponentsDeriveFollowTheirSources() {
        for (TacticalPalette palette : new TacticalPalette[]{TacticalPalette.ACADEMY,
                TacticalPalette.CAESAR, TacticalPalette.NEUTRAL,
                TacticalPalette.CAESAR.withScope(Scope.MAP)}) {
            int frame = palette.get(PaletteToken.FRAME);
            assertEquals(0xF2000000 | frame & 0xFFFFFF, palette.get(PaletteToken.TOOLTIP_BG));
            assertEquals(palette.get(PaletteToken.INPUT_EDGE),
                    palette.get(PaletteToken.TOOLTIP_EDGE));
            assertEquals(frame, palette.get(PaletteToken.SCROLL_TRACK));
            assertEquals(palette.get(PaletteToken.WELL), palette.get(PaletteToken.FEEDBACK_BG));
            assertEquals(palette.get(PaletteToken.CARD_LIP),
                    palette.get(PaletteToken.CARD_LIP_HOVER));
        }
    }

    @Test
    void anOverrideWinsOverTheDerivedValue() {
        Map<PaletteToken, Integer> overrides = new EnumMap<>(PaletteToken.class);
        overrides.put(PaletteToken.FRAME, 0xFF102030);
        overrides.put(PaletteToken.SCROLL_TRACK, 0xFF445566);
        int[] values = TacticalPalette.derive(overrides);

        assertEquals(0xF2102030, values[PaletteToken.TOOLTIP_BG.ordinal()]);
        assertEquals(0xFF445566, values[PaletteToken.SCROLL_TRACK.ordinal()]);
        assertEquals(TacticalPalette.A.get(PaletteToken.TEXT), values[PaletteToken.TEXT.ordinal()]);
    }

    @Test
    void onlyTheCaesarMapHasItsOwnScope() {
        assertSame(TacticalPalette.ACADEMY, Livery.ACADEMY.palette(Scope.MAP));
        assertSame(TacticalPalette.NEUTRAL, Livery.NEUTRAL.palette(Scope.MAP));
        assertSame(TacticalPalette.A, TacticalPalette.A.withScope(Scope.MAP));

        TacticalPalette map = Livery.CAESAR.palette(Scope.MAP);
        assertNotEquals(TacticalPalette.CAESAR, map);
        assertEquals("CAESAR/MAP", map.name());
        assertSame(map, map.withScope(Scope.MAP));
        assertSame(TacticalPalette.CAESAR, map.withScope(Scope.BOARD));
        // Graphite selection with the rose bar; everything off the selection stays Caesar.
        assertEquals(0xFF3B4247, map.get(PaletteToken.SELECT));
        assertEquals(0xFFFF9EB4, map.get(PaletteToken.SELECT_BAR));
        assertEquals(TacticalPalette.CAESAR.get(PaletteToken.DANGER), map.get(PaletteToken.DANGER));
        assertEquals(TacticalPalette.CAESAR.get(PaletteToken.FRAME), map.get(PaletteToken.FRAME));
    }

    // ---- the push scope ---------------------------------------------------------------------------

    @Test
    void closingAPushLeavesTheASchemeForTheHud() {
        try (TacticalPalette.Applied ignored = TacticalPalette.push(
                TacticalLivery.Livery.CAESAR.palette(TacticalLivery.Scope.MAP))) {
            assertEquals("CAESAR/MAP", TacticalPalette.active().name());
            assertEquals(0xFFC6BBB9, TacticalBoardTheme.BOARD);
            assertEquals(0xFF3B4247, TacticalBoardTheme.SELECT);
        }
        assertSame(TacticalPalette.A, TacticalPalette.active());
        assertEquals(0xFFBBC5C1, TacticalBoardTheme.BOARD);
        assertEquals(0xFFEEF3F0, TacticalBoardTheme.LIGHT);
    }

    @Test
    void scopesNestAndRestoreInOrder() {
        try (TacticalPalette.Applied outer = TacticalPalette.push(TacticalPalette.ACADEMY)) {
            assertEquals(TacticalPalette.ACADEMY.get(PaletteToken.SELECT), TacticalBoardTheme.SELECT);
            try (TacticalPalette.Applied inner = TacticalPalette.push(TacticalPalette.NEUTRAL)) {
                assertSame(TacticalPalette.NEUTRAL, TacticalPalette.active());
                assertEquals(0xFF1C2427, TacticalBoardTheme.LIGHT, "Neutral LIGHT is dark ink");
            }
            assertSame(TacticalPalette.ACADEMY, TacticalPalette.active());
            assertEquals(TacticalPalette.ACADEMY.get(PaletteToken.LIGHT), TacticalBoardTheme.LIGHT);
            try (TacticalPalette.Applied same = TacticalPalette.push(TacticalPalette.ACADEMY)) {
                assertSame(TacticalPalette.ACADEMY, TacticalPalette.active());
            }
            assertSame(TacticalPalette.ACADEMY, TacticalPalette.active());
        }
        assertSame(TacticalPalette.A, TacticalPalette.active());
    }

    @Test
    void anExceptionWhileDrawingStillRestoresA() {
        assertThrows(IllegalStateException.class, () -> {
            try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.NEUTRAL)) {
                throw new IllegalStateException("render failed");
            }
        });
        assertSame(TacticalPalette.A, TacticalPalette.active());
        assertEquals(TacticalPalette.A.get(PaletteToken.FRAME), TacticalBoardTheme.FRAME);
    }

    @Test
    void closingTwiceRestoresOnlyOnce() {
        TacticalPalette.Applied outer = TacticalPalette.push(TacticalPalette.CAESAR);
        TacticalPalette.Applied inner = TacticalPalette.push(TacticalPalette.NEUTRAL);
        inner.close();
        inner.close();
        assertSame(TacticalPalette.CAESAR, TacticalPalette.active(), "the outer scope stays open");
        outer.close();
        assertSame(TacticalPalette.A, TacticalPalette.active());
    }

    @Test
    void anotherThreadCannotPushWhileAScopeIsOpen() throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.CAESAR)) {
            Thread worker = new Thread(() -> {
                try {
                    TacticalPalette.push(TacticalPalette.NEUTRAL).close();
                } catch (Throwable thrown) {
                    failure.set(thrown);
                }
            }, "palette-test-worker");
            worker.start();
            worker.join();
            assertSame(TacticalPalette.CAESAR, TacticalPalette.active());
        }
        assertInstanceOf(IllegalStateException.class, failure.get());
        assertSame(TacticalPalette.A, TacticalPalette.active());
    }

    @Test
    void aScreenPushesItsLiveryOnItsPageScope() {
        TacticalScreen screen = new TacticalScreen(Component.literal("palette")) {
            @Override
            protected void initTactical() {
            }

            @Override
            protected Livery livery() {
                return Livery.CAESAR;
            }

            @Override
            protected Scope paletteScope() {
                return Scope.MAP;
            }
        };
        assertSame(TacticalPalette.CAESAR.withScope(Scope.MAP), screen.framePalette());
    }

    // ---- defaults fixed outside the palette scope ----------------------------------------------

    @Test
    void constructionTimeBadgeColoursAreResolvedWhileDrawing() {
        assertEquals(TacticalBoardTheme.RENDER_MUTED,
                TacticalButtonStyle.Options.DEFAULT.badgeColor());
        assertEquals(TacticalBoardTheme.RENDER_MUTED,
                TacticalTabStrip.Tab.of("squads", Component.literal("小队")).badgeColor());
        assertEquals(TacticalBoardTheme.RENDER_MUTED, TacticalTabStrip.Tab.of("map",
                Component.literal("战术地图"), Component.literal("地图")).badgeColor());
        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.NEUTRAL)) {
            assertEquals(TacticalPalette.NEUTRAL.get(PaletteToken.MUTED),
                    TacticalBoardTheme.orMuted(TacticalBoardTheme.RENDER_MUTED));
            assertEquals(0xFF123456, TacticalBoardTheme.orMuted(0xFF123456));
        }
        assertEquals(TacticalPalette.A.get(PaletteToken.MUTED),
                TacticalBoardTheme.orMuted(TacticalBoardTheme.RENDER_MUTED));
    }

    @Test
    void textOnSolidFillsStaysLightInEveryLivery() {
        // Neutral's LIGHT is dark ink: success and armed danger keys write ON_FILL, selected
        // keys and dark badges ON_SELECT (plan 4.1 item 7).
        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.NEUTRAL)) {
            TacticalButtonStyle.Palette success = TacticalButtonStyle.palette(
                    new TacticalButtonStyle.Look(TacticalButtonStyle.State.SUCCESS, false));
            TacticalButtonStyle.Palette armed = TacticalButtonStyle.palette(
                    new TacticalButtonStyle.Look(TacticalButtonStyle.State.DANGER_ARMED, true));
            TacticalButtonStyle.Palette selected = TacticalButtonStyle.palette(
                    new TacticalButtonStyle.Look(TacticalButtonStyle.State.SELECTED, false));
            assertEquals(TacticalBoardTheme.ON_FILL, success.text());
            assertEquals(TacticalBoardTheme.ON_FILL, armed.text());
            assertEquals(TacticalBoardTheme.ON_SELECT, selected.text());
            assertNotEquals(TacticalBoardTheme.LIGHT, success.text());
        }
    }

    private static String hex(int color) {
        return String.format("0x%08X", color);
    }
}
