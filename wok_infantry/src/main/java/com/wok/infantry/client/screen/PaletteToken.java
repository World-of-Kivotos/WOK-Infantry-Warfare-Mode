package com.wok.infantry.client.screen;

/**
 * The swappable colour tokens of {@link TacticalBoardTheme}: the ones a faction livery repaints
 * inside a {@link TacticalScreen} (preview {@code 18-device-livery.js} {@code INNER}, P3).
 *
 * <p>Every constant names the {@link TacticalBoardTheme} field of the same name. Tokens that are
 * not listed here never change with the faction: the world shade, modal dim, hatch, badge insets,
 * {@code BODY_DEAD}, {@code DEVICE_EDGE}, panel shadow and highlight, the HUD plates and friend /
 * foe inks, every map and grid colour and every deprecated alias.
 */
public enum PaletteToken {
    // device-coloured chrome: header plates, glass behind the board, rivets
    FRAME,
    FRAME_MID,
    RIVET,

    // light board surfaces
    BOARD,
    BOARD_ALT,
    CARD,
    CARD_HOVER,
    CARD_PRESSED,
    CARD_DISABLED,
    CARD_LIP,
    CARD_LIP_HOVER,
    BEVEL,
    BADGE_BG,

    // dark wells, rows, cells and inputs
    WELL,
    WELL_ROW,
    WELL_ROW_ALT,
    WELL_EDGE,
    WELL_LIGHT_EDGE,
    ROW_HOVER,
    CELL,
    CELL_EDGE,
    INPUT_EDGE,

    // lines
    BORDER,
    BORDER_DARK,
    EDGE,

    // text
    TEXT,
    MUTED,
    FAINT,
    LIGHT,
    LIGHT_MUTED,
    OFFLINE,
    DISABLED_TEXT,
    ON_FILL,

    // selection
    SELECT,
    SELECT_HOVER,
    SELECT_EDGE,
    SELECT_BAR,
    SELECT_SUB,
    SELECT_WELL,
    ON_SELECT,

    // semantics, board variants
    ACCENT,
    ACCENT_SOFT,
    ACCENT_TEXT,
    SECTION,
    ADJUST,
    ADJUST_SOFT,
    DANGER,
    DANGER_HOVER,
    DANGER_SOFT,
    DANGER_DEEP,
    SUCCESS,
    SUCCESS_HOVER,
    SUCCESS_SOFT,
    SUCCESS_EDGE,
    DISABLED_EDGE,

    // semantics, bright variants
    SELECT_B,
    ACCENT_B,
    DANGER_B,
    SUCCESS_B,
    NEUTRAL_B,
    SECTION_B,
    ADJUST_B,

    // small parts
    FOCUS,
    SILHOUETTE,
    TAB_HOVER,
    KEYCAP_EDGE,
    FEEDBACK_BG,
    SCROLL_TRACK,
    TOOLTIP_BG,
    TOOLTIP_EDGE,
    HUD_TRACK
}
