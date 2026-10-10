package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalLivery.Livery;
import com.wok.infantry.client.screen.TacticalLivery.Scope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.wok.infantry.client.screen.PaletteToken.ACCENT;
import static com.wok.infantry.client.screen.PaletteToken.ACCENT_B;
import static com.wok.infantry.client.screen.PaletteToken.ACCENT_TEXT;
import static com.wok.infantry.client.screen.PaletteToken.ADJUST;
import static com.wok.infantry.client.screen.PaletteToken.BOARD;
import static com.wok.infantry.client.screen.PaletteToken.BOARD_ALT;
import static com.wok.infantry.client.screen.PaletteToken.CARD;
import static com.wok.infantry.client.screen.PaletteToken.CARD_DISABLED;
import static com.wok.infantry.client.screen.PaletteToken.DANGER;
import static com.wok.infantry.client.screen.PaletteToken.DANGER_B;
import static com.wok.infantry.client.screen.PaletteToken.DANGER_HOVER;
import static com.wok.infantry.client.screen.PaletteToken.DISABLED_TEXT;
import static com.wok.infantry.client.screen.PaletteToken.FAINT;
import static com.wok.infantry.client.screen.PaletteToken.FRAME;
import static com.wok.infantry.client.screen.PaletteToken.FRAME_MID;
import static com.wok.infantry.client.screen.PaletteToken.LIGHT;
import static com.wok.infantry.client.screen.PaletteToken.LIGHT_MUTED;
import static com.wok.infantry.client.screen.PaletteToken.MUTED;
import static com.wok.infantry.client.screen.PaletteToken.OFFLINE;
import static com.wok.infantry.client.screen.PaletteToken.ON_FILL;
import static com.wok.infantry.client.screen.PaletteToken.ON_SELECT;
import static com.wok.infantry.client.screen.PaletteToken.ROW_HOVER;
import static com.wok.infantry.client.screen.PaletteToken.SECTION;
import static com.wok.infantry.client.screen.PaletteToken.SELECT;
import static com.wok.infantry.client.screen.PaletteToken.SELECT_B;
import static com.wok.infantry.client.screen.PaletteToken.SELECT_BAR;
import static com.wok.infantry.client.screen.PaletteToken.SELECT_EDGE;
import static com.wok.infantry.client.screen.PaletteToken.SELECT_HOVER;
import static com.wok.infantry.client.screen.PaletteToken.SELECT_SUB;
import static com.wok.infantry.client.screen.PaletteToken.SUCCESS;
import static com.wok.infantry.client.screen.PaletteToken.SUCCESS_B;
import static com.wok.infantry.client.screen.PaletteToken.SUCCESS_HOVER;
import static com.wok.infantry.client.screen.PaletteToken.TEXT;
import static com.wok.infantry.client.screen.PaletteToken.TOOLTIP_BG;
import static com.wok.infantry.client.screen.PaletteToken.WELL;
import static com.wok.infantry.client.screen.PaletteToken.WELL_ROW;
import static com.wok.infantry.client.screen.PaletteToken.WELL_ROW_ALT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrast pairs the P3 review checked (preview {@code 18-device-livery.js} notes), for the three
 * liveries on both page scopes: body text at least 4.5:1; secondary and status text, markers and
 * lines at least 3:1 (WCAG relative luminance, translucent colours composited onto their ground).
 *
 * <p>Not asserted, a call-site rule instead (DEVICE_PORT_PLAN 4.7): {@code FAINT} is never written
 * straight on {@code BOARD}; on Academy and Caesar that pair is only about 2.5:1.
 */
class TacticalPaletteContrastTest {
    private static final double BODY = 4.5;
    private static final double SECONDARY = 3.0;

    private record Pair(PaletteToken ink, PaletteToken ground, double minimum, String use) {
    }

    private static final List<Pair> PAIRS = List.of(
            // body text
            new Pair(TEXT, CARD, BODY, "key and card labels"),
            new Pair(TEXT, BOARD_ALT, BODY, "panel text"),
            new Pair(TEXT, BOARD, BODY, "screen floor text"),
            new Pair(MUTED, BOARD, BODY, "secondary text on the screen floor"),
            new Pair(MUTED, CARD, BODY, "secondary text on keys"),
            new Pair(ON_SELECT, SELECT, BODY, "selected key / row label"),
            new Pair(ON_SELECT, SELECT_HOVER, BODY, "hovered selected label"),
            new Pair(LIGHT, WELL_ROW, BODY, "list row text"),
            new Pair(LIGHT, WELL_ROW_ALT, BODY, "alternate list row text"),
            new Pair(LIGHT, ROW_HOVER, BODY, "hovered list row text"),
            new Pair(LIGHT, FRAME_MID, BODY, "section plate title"),
            new Pair(LIGHT, FRAME, BODY, "chrome text"),
            new Pair(LIGHT, TOOLTIP_BG, BODY, "tooltip text"),
            new Pair(ON_FILL, SUCCESS, BODY, "success key label"),
            new Pair(ON_FILL, SUCCESS_HOVER, BODY, "hovered success key label"),
            new Pair(ON_FILL, DANGER, BODY, "armed danger key label"),
            new Pair(ON_FILL, DANGER_HOVER, BODY, "hovered armed danger key label"),
            new Pair(ACCENT_TEXT, CARD, BODY, "attention value on cards and keys"),
            new Pair(ACCENT_TEXT, BOARD_ALT, BODY, "attention value on panels"),
            // secondary / status text, markers and lines
            new Pair(FAINT, BOARD_ALT, SECONDARY, "faintest hint on panels"),
            new Pair(FAINT, CARD, SECONDARY, "faintest hint on keys"),
            new Pair(FAINT, WELL, SECONDARY, "empty slot in a well"),
            new Pair(FAINT, WELL_ROW, SECONDARY, "disabled list row"),
            new Pair(FAINT, WELL_ROW_ALT, SECONDARY, "disabled alternate list row"),
            new Pair(ACCENT, CARD, SECONDARY, "attention line on keys"),
            new Pair(ACCENT, BOARD_ALT, SECONDARY, "panel LED and meter"),
            new Pair(ADJUST, CARD, SECONDARY, "adjust underline"),
            new Pair(SECTION, FRAME_MID, SECONDARY, "section marker on its plate"),
            new Pair(DANGER, BOARD, SECONDARY, "danger text on the screen floor"),
            new Pair(DANGER, BOARD_ALT, SECONDARY, "danger text on panels"),
            new Pair(DANGER, CARD, SECONDARY, "danger key label and stripes"),
            new Pair(SUCCESS, BOARD_ALT, SECONDARY, "success text on panels"),
            new Pair(SUCCESS, CARD, SECONDARY, "success text on cards"),
            new Pair(SELECT_BAR, SELECT, SECONDARY, "selection bar"),
            new Pair(SELECT_BAR, SELECT_HOVER, SECONDARY, "selection bar while hovered"),
            new Pair(SELECT_SUB, SELECT, SECONDARY, "secondary text on a selection"),
            new Pair(SELECT_SUB, SELECT_HOVER, SECONDARY, "secondary text on a hovered selection"),
            new Pair(LIGHT_MUTED, WELL_ROW, SECONDARY, "row sub text"),
            new Pair(LIGHT_MUTED, FRAME_MID, SECONDARY, "section plate meta"),
            new Pair(OFFLINE, WELL_ROW, SECONDARY, "offline member"),
            new Pair(DISABLED_TEXT, CARD_DISABLED, SECONDARY, "disabled key label"),
            new Pair(SELECT_B, WELL_ROW, SECONDARY, "current / selected ink in wells"),
            new Pair(SELECT_B, FRAME_MID, SECONDARY, "selection marker on a section plate"),
            new Pair(DANGER_B, WELL_ROW, SECONDARY, "danger ink in wells"),
            new Pair(DANGER_B, FRAME_MID, SECONDARY, "danger marker on a section plate"),
            new Pair(SUCCESS_B, WELL_ROW, SECONDARY, "success ink in wells"),
            new Pair(ACCENT_B, WELL_ROW, SECONDARY, "attention ink in wells"));

    @Test
    void reviewedPairsKeepTheirContrastInEveryLivery() {
        List<String> failures = new ArrayList<>();
        for (Livery livery : Livery.values()) {
            for (Scope scope : Scope.values()) {
                TacticalPalette palette = livery.palette(scope);
                for (Pair pair : PAIRS) {
                    double ratio = contrast(palette.get(pair.ink()), palette.get(pair.ground()));
                    if (ratio < pair.minimum()) {
                        failures.add(String.format("%s %s on %s (%s): %.2f < %.1f", palette.name(),
                                pair.ink(), pair.ground(), pair.use(), ratio, pair.minimum()));
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    void countBadgesReadOnTheirInsets() {
        // Selected key: dark inset 0x40000000 under ON_SELECT; disabled key: light inset
        // 0x33FFFFFF under MUTED; normal key: 0x22000000 under MUTED (preview UIX.button).
        for (Livery livery : Livery.values()) {
            TacticalPalette palette = livery.palette(Scope.BOARD);
            int selected = over(0x40000000, palette.get(SELECT));
            int disabled = over(0x33FFFFFF, palette.get(CARD_DISABLED));
            int normal = over(0x22000000, palette.get(CARD));
            assertTrue(contrast(palette.get(ON_SELECT), selected) >= BODY, livery + " selected");
            assertTrue(contrast(palette.get(MUTED), disabled) >= SECONDARY, livery + " disabled");
            assertTrue(contrast(palette.get(MUTED), normal) >= SECONDARY, livery + " normal");
        }
    }

    @Test
    void neutralFramesItsPaleSelectionBarWithADarkLine() {
        // On the pale Neutral well the light bar alone is 1.02–1.05:1; the 1px SELECT_EDGE line
        // outside it carries the shape (13.7:1).
        TacticalPalette neutral = TacticalPalette.NEUTRAL;
        assertTrue(contrast(neutral.get(SELECT_EDGE), neutral.get(WELL_ROW)) >= SECONDARY);
        assertTrue(contrast(neutral.get(SELECT_EDGE), neutral.get(WELL)) >= SECONDARY);
    }

    @Test
    void contrastMathMatchesWcag() {
        assertEquals(21.0, contrast(0xFFFFFFFF, 0xFF000000), 1e-9);
        assertEquals(1.0, contrast(0xFF777777, 0xFF777777), 1e-9);
        assertEquals(0xFF808080, over(0x80FFFFFF, 0xFF000000));
    }

    /** WCAG 2 contrast of {@code ink} drawn over the opaque {@code ground}. */
    static double contrast(int ink, int ground) {
        double a = luminance(over(ink, ground));
        double b = luminance(ground | 0xFF000000);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    /** {@code top} alpha-composited onto the opaque {@code ground}. */
    static int over(int top, int ground) {
        double alpha = (top >>> 24) / 255.0;
        int color = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            double channel = ((top >>> shift) & 0xFF) * alpha + ((ground >>> shift) & 0xFF) * (1 - alpha);
            color |= (int) Math.round(channel) << shift;
        }
        return color;
    }

    private static double luminance(int color) {
        return 0.2126 * linear((color >>> 16) & 0xFF) + 0.7152 * linear((color >>> 8) & 0xFF)
                + 0.0722 * linear(color & 0xFF);
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
