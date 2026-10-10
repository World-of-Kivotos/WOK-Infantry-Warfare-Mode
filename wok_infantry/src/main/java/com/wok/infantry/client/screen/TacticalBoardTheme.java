package com.wok.infantry.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Design tokens of the WOK步战 "A · 战术平板" style (finalised 2026-10-04), shared by every
 * battle screen, the HUD and the map canvas.
 *
 * <p>Values come from the layout preview ({@code ui-preview/kit/ui.js} token table {@code T},
 * the colours its components hard-code, and {@code kit/map-icons.js}). Colour semantics follow
 * the permanent UI rules: blue = current selection, orange = sections and adjustable controls,
 * red = danger / hostile, green = success / confirmation, gray = disabled.
 *
 * <p>Board variants ({@code SELECT}, {@code DANGER}, ...) are readable on the light gray-green
 * board; bright variants ({@code *_B}) are for dark wells, the device frame and the HUD.
 * Token names follow the preview, so {@code T.EDGE} there is {@link #EDGE} here (the
 * translucent separator) and {@code T.DEVICE_EDGE} is {@link #DEVICE_EDGE} (the frame line).
 * The preview's {@code FRIENDLY}/{@code HOSTILE} are HUD colours: {@link #HUD_FRIENDLY} and
 * {@link #HOSTILE}; the Java {@link #FRIENDLY} is the deprecated blue of the old board.
 * Add-on MODs must not hard-depend on this class; copy the values they need instead.
 */
public final class TacticalBoardTheme {
    // ---- device shell ----------------------------------------------------------------------
    public static final int WORLD_SHADE = 0xE60A0E11;
    public static final int FRAME = 0xFF14191B;
    public static final int FRAME_MID = 0xFF262E30;
    /** Light outline of the device frame and the rivet highlight. */
    public static final int DEVICE_EDGE = 0xFF77817F;
    public static final int RIVET = 0xFF080B0C;

    // ---- board surfaces (light gray-green) ----------------------------------------------------
    public static final int BOARD = 0xFFBBC5C1;
    public static final int BOARD_ALT = 0xFFA9B5B1;
    public static final int CARD = 0xFFD6DDD9;
    public static final int CARD_HOVER = 0xFFE6EBE8;
    public static final int CARD_PRESSED = 0xFFC6CFCB;
    public static final int CARD_DISABLED = 0xFFA3ADAB;
    /** Bottom lip of a raised key or card (drawn as intended, not as the commented-out preview). */
    public static final int CARD_LIP = 0xFFBAC4C0;
    public static final int CARD_LIP_HOVER = 0xFFB9C3BF;
    public static final int WELL = 0xFF1D2527;
    public static final int WELL_ROW = 0xFF263032;
    public static final int WELL_ROW_ALT = 0xFF222B2D;
    /** Outline of empty slots and grid lines inside dark wells. */
    public static final int WELL_EDGE = 0xFF3A4547;
    /** Lower/right light edge that makes a dark well look recessed. */
    public static final int WELL_LIGHT_EDGE = 0xFF3C4749;
    public static final int BORDER = 0xFF4E5C5A;
    public static final int BORDER_DARK = 0xFF2E3837;
    /** Top bevel line of raised keys. */
    public static final int BEVEL = 0xFFF1F5F2;
    /**
     * Translucent dark hairline separator on boards and panels (preview {@code T.EDGE}). Drawn as
     * intended; the chosen A-scheme screenshots lacked it because the preview line was commented out.
     */
    public static final int EDGE = 0x40243032;
    /** Same colour as {@link #EDGE} under a descriptive name. */
    public static final int DIVIDER = EDGE;
    public static final int PANEL_SHADOW = 0x50000000;
    public static final int PANEL_HIGHLIGHT = 0x80F1F5F2;

    // ---- text ----------------------------------------------------------------------------------
    public static final int TEXT = 0xFF1B262A;
    public static final int MUTED = 0xFF52626A;
    public static final int FAINT = 0xFF6E7C7F;
    public static final int LIGHT = 0xFFEEF3F0;
    public static final int LIGHT_MUTED = 0xFF9DAAA8;
    public static final int DISABLED_TEXT = 0xFF6B7676;
    /**
     * Text on solid success fills and on an armed (hovered or confirming) danger fill. The same
     * colour as {@link #LIGHT} in the A scheme; a faction palette keeps it light where
     * {@code LIGHT} turns into dark ink (preview {@code 18-device-livery.js} {@code ON_FILL}).
     */
    public static final int ON_FILL = LIGHT;

    // ---- semantics, board variants ---------------------------------------------------------------
    public static final int SELECT = 0xFF2E679C;
    public static final int SELECT_HOVER = 0xFF3D7AB2;
    public static final int SELECT_EDGE = 0xFF1D4B75;
    public static final int SELECT_BAR = 0xFF8CC3EE;
    /** Secondary text on a {@link #SELECT} fill. */
    public static final int SELECT_SUB = 0xFFCFE3F4;
    /** Dark preview well inside a selected card. */
    public static final int SELECT_WELL = 0xFF1D3843;
    /** Primary text on a {@link #SELECT} fill. */
    public static final int ON_SELECT = 0xFFEEF3F0;
    /** Attention (cooldown, unsaved). In the A scheme it is the same orange as sections and controls. */
    public static final int ACCENT = 0xFFBE7A1E;
    public static final int ACCENT_SOFT = 0xFFE9C58E;
    /**
     * Attention-orange text on light boards, cards and keys, where {@link #ACCENT} itself is only
     * 1.7–2.8:1 (preview {@code SHARED.ACCENT_TEXT}); fills, LEDs and meters keep {@code ACCENT}.
     */
    public static final int ACCENT_TEXT = 0xFF844600;
    /** Section markers. */
    public static final int SECTION = 0xFFBE7A1E;
    /** Adjustable controls (sliders, steppers, control-key underline). */
    public static final int ADJUST = 0xFFBE7A1E;
    public static final int ADJUST_SOFT = 0xFFE9C58E;
    public static final int DANGER = 0xFFB0443C;
    public static final int DANGER_HOVER = 0xFFC2544B;
    public static final int DANGER_SOFT = 0xFFEBC3BE;
    /** Dark stripes of the hazard tab on an armed (solid {@link #DANGER}) danger key. */
    public static final int DANGER_DEEP = 0xFF3A0F0C;
    public static final int SUCCESS = 0xFF3B7A57;
    public static final int SUCCESS_HOVER = 0xFF478D66;
    public static final int SUCCESS_SOFT = 0xFFC0DCC9;
    public static final int SUCCESS_EDGE = 0xFF285640;
    public static final int DISABLED_EDGE = 0xFF7F8A89;
    /** Diagonal hatch drawn over disabled keys so "disabled" never relies on colour alone. */
    public static final int HATCH = 0x18000000;

    // ---- semantics, bright variants (dark wells, frame, HUD) ------------------------------------
    public static final int SELECT_B = 0xFF7BB8EA;
    public static final int ACCENT_B = 0xFFF0A63A;
    public static final int DANGER_B = 0xFFE8695D;
    public static final int SUCCESS_B = 0xFF7CCB8F;
    public static final int NEUTRAL_B = 0xFFB6C2C0;
    public static final int SECTION_B = 0xFFF0A63A;
    public static final int ADJUST_B = 0xFFF0A63A;

    // ---- rows, cells, inputs and small parts -----------------------------------------------------
    public static final int ROW_HOVER = 0xFF334043;
    public static final int OFFLINE = 0xFF7D898A;
    public static final int INPUT_EDGE = 0xFF55625F;
    public static final int CELL = 0xFF3A464A;
    public static final int CELL_EDGE = 0xFF5A6765;
    public static final int BADGE_BG = 0xFFD6DDD9;
    public static final int BADGE_ON_SELECT = 0x40FFFFFF;
    public static final int BADGE_ON_CARD = 0x22000000;
    public static final int BODY_DEAD = 0xFF1A1A1A;
    /** Keyboard focus ring. */
    public static final int FOCUS = 0xFFF4F7F5;
    /** Gun silhouettes in loadout previews. */
    public static final int SILHOUETTE = 0xFFD9E1DE;
    public static final int TAB_HOVER = 0xFF2A3437;
    public static final int KEYCAP_EDGE = 0xFF3A4547;
    public static final int FEEDBACK_BG = 0xFF1A2224;
    public static final int SCROLL_TRACK = 0xFF151B1D;
    public static final int TOOLTIP_BG = 0xF2141A1C;
    public static final int TOOLTIP_EDGE = 0xFF55625F;
    public static final int MODAL_DIM = 0xB0060909;

    // ---- HUD -------------------------------------------------------------------------------------
    public static final int HUD_PLATE = 0xB3121A1D;
    public static final int HUD_PLATE_SOLID = 0xE0121A1D;
    public static final int HUD_EDGE = 0xCC56625F;
    public static final int HUD_TRACK = 0xCC424E52;
    /** Own side on the HUD and dark wells (preview {@code T.FRIENDLY}). */
    public static final int HUD_FRIENDLY = 0xFF6FB1E6;
    public static final int HUD_HOSTILE = 0xFFE8695D;
    /** Hostile ink on dark surfaces (preview {@code T.HOSTILE}); same as {@link #HUD_HOSTILE}. */
    public static final int HOSTILE = HUD_HOSTILE;

    // ---- map -------------------------------------------------------------------------------------
    public static final int MAP_FRIENDLY = 0xFF6FB1E6;
    public static final int MAP_HOSTILE = 0xFFE8695D;
    public static final int MAP_ICON_HOSTILE = 0xFFD8433A;
    public static final int MAP_ICON_RECON = 0xFFB0303A;
    public static final int MAP_ICON_ATTACK = 0xFFE36A2C;
    public static final int MAP_ICON_DEFEND = 0xFF2F7FD0;
    public static final int MAP_ICON_RALLY = 0xFF3F9A55;
    public static final int MAP_ICON_FRIENDLY = 0xFF2F6FB0;
    public static final int MAP_ICON_OUTLINE = 0xFF101417;
    public static final int MAP_ICON_RING = 0xFFFFE36E;
    /** Map canvas colours of the current map screen; the map batch retunes them. */
    public static final int MAP_WASH = 0x2495A7A2;
    public static final int GRID_MINOR = 0x30405050;
    public static final int GRID_MAJOR = 0x70404A49;

    // ---- deprecated aliases (old names; every screen now draws the finalised colours) -------------
    /** @deprecated use {@link #FRAME}. */
    @Deprecated
    public static final int DEVICE_FRAME = FRAME;
    /** @deprecated use {@link #FRAME_MID}. */
    @Deprecated
    public static final int DEVICE_MID = FRAME_MID;
    /** @deprecated use {@link #SELECT}. */
    @Deprecated
    public static final int SELECTED = SELECT;
    /** @deprecated use {@link #SELECT_HOVER}. */
    @Deprecated
    public static final int SELECTED_HOVER = SELECT_HOVER;
    /** @deprecated use {@link #LIGHT}. */
    @Deprecated
    public static final int LIGHT_TEXT = LIGHT;
    /** @deprecated use {@link #MUTED}. */
    @Deprecated
    public static final int MUTED_TEXT = MUTED;
    /** @deprecated use {@link #BEVEL}. */
    @Deprecated
    public static final int BORDER_BRIGHT = BEVEL;

    // ---- deprecated legacy colours without a finalised equivalent ---------------------------------
    // Kept at their old values until the owning screen batch replaces them (map, squad, formation).
    /**
     * @deprecated own-side blue of the old board; use {@link #SELECT} for selection,
     * {@link #MAP_FRIENDLY} on the map or {@link #HUD_FRIENDLY} on the HUD.
     */
    @Deprecated
    public static final int FRIENDLY = 0xFF2F75B5;
    /** @deprecated mid-gray inset of the old board; use {@link #WELL} or {@link #FRAME_MID}. */
    @Deprecated
    public static final int INSET = 0xFF7F8D8B;
    /** @deprecated the device frame is opaque, so its drop shadow is never visible. */
    @Deprecated
    public static final int DEVICE_SHADOW = 0xD0000000;

    /** Height of the legacy {@link #sectionHeader} strip. */
    public static final int SECTION_HEADER_HEIGHT = 14;

    private TacticalBoardTheme() {
    }

    /** Raised board panel: soft drop shadow, outline and a light top highlight. */
    public static void raisedPanel(GuiGraphics graphics, int left, int top,
                                   int right, int bottom, int fill) {
        graphics.fill(left + 1, top + 1, right + 1, bottom + 1, PANEL_SHADOW);
        graphics.fill(left, top, right, bottom, fill);
        BattleUiTheme.outline(graphics, left, top, right, bottom, BORDER);
        graphics.fill(left + 1, top + 1, right - 1, top + 2, PANEL_HIGHLIGHT);
    }

    /** Recessed panel keeping the caller's fill: outline plus a light lower/right edge. */
    public static void insetPanel(GuiGraphics graphics, int left, int top,
                                  int right, int bottom, int fill) {
        graphics.fill(left, top, right, bottom, fill);
        BattleUiTheme.outline(graphics, left, top, right, bottom, BORDER);
        graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, BEVEL);
        graphics.fill(right - 2, top + 1, right - 1, bottom - 1, BEVEL);
    }

    public static void sectionHeader(GuiGraphics graphics, Font font, String text,
                                     int left, int top, int right, int accent) {
        sectionHeader(graphics, font, Component.literal(text), left, top, right, accent);
    }

    /** Section strip of the legacy fixed height: dark plate, 3px colour tab, light label. */
    public static void sectionHeader(GuiGraphics graphics, Font font, Component text,
                                     int left, int top, int right, int accent) {
        sectionHeader(graphics, font, text, Component.empty(), LIGHT_MUTED,
                left, top, right, top + SECTION_HEADER_HEIGHT, accent);
    }

    /**
     * Section strip with an optional right-aligned meta text (at most 40% of the width).
     * The title is ellipsized so it never runs under the meta text or past the strip.
     */
    public static void sectionHeader(GuiGraphics graphics, Font font, Component title,
                                     Component meta, int metaColor,
                                     int left, int top, int right, int bottom, int accent) {
        if (right <= left || bottom <= top) {
            return;
        }
        graphics.fill(left, top, right, bottom, FRAME_MID);
        graphics.fill(left, top, Math.min(right, left + 3), bottom, accent);
        int width = right - left;
        int textY = top + Math.max(0, (bottom - top - 8) / 2);
        int metaWidth = 0;
        if (meta != null && !meta.getString().isEmpty()) {
            metaWidth = Math.min(font.width(meta), width * 2 / 5);
            TextFit.draw(graphics, font, meta, right - metaWidth - 4, textY, metaWidth,
                    metaColor, TextFit.Align.RIGHT);
        }
        int titleWidth = width - 12 - (metaWidth > 0 ? metaWidth + 6 : 0);
        TextFit.draw(graphics, font, title, left + 7, textY, titleWidth, LIGHT,
                TextFit.Align.LEFT);
    }

    public static void cornerBrackets(GuiGraphics graphics, int left, int top,
                                      int right, int bottom, int color) {
        int arm = Math.max(4, Math.min(14, Math.min(right - left, bottom - top) / 8));
        graphics.fill(left, top, left + arm, top + 2, color);
        graphics.fill(left, top, left + 2, top + arm, color);
        graphics.fill(right - arm, top, right, top + 2, color);
        graphics.fill(right - 2, top, right, top + arm, color);
        graphics.fill(left, bottom - 2, left + arm, bottom, color);
        graphics.fill(left, bottom - arm, left + 2, bottom, color);
        graphics.fill(right - arm, bottom - 2, right, bottom, color);
        graphics.fill(right - 2, bottom - arm, right, bottom, color);
    }

    public static void rivet(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 2, y + 2, RIVET);
        graphics.fill(x, y, x + 1, y + 1, DEVICE_EDGE);
    }
}
