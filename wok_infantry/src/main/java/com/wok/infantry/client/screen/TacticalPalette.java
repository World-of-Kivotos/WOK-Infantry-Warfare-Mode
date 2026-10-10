package com.wok.infantry.client.screen;

import java.util.Objects;

/**
 * One complete set of values for the swappable {@link PaletteToken}s of
 * {@link TacticalBoardTheme}.
 *
 * <p>A {@link TacticalScreen} pushes the palette of its {@link TacticalLivery.Livery} for the
 * duration of one frame ({@link #push}), so every call site keeps reading the plain
 * {@code TacticalBoardTheme} fields while the HUD, drawn outside that scope, always sees
 * {@link #A}.
 *
 * <p>Only the A scheme exists so far: the tokens are still compile-time constants, so
 * {@link #apply()} and {@link #push} cannot change anything yet and every scope resolves to
 * {@code A}.
 */
public final class TacticalPalette {
    private static final PaletteToken[] TOKENS = PaletteToken.values();

    /** The A · 战术平板 scheme: the {@link TacticalBoardTheme} values as declared. */
    public static final TacticalPalette A = new TacticalPalette("A", readTheme());

    private final String name;
    private final int[] values;

    private TacticalPalette(String name, int[] values) {
        this.name = Objects.requireNonNull(name, "name");
        if (values.length != TOKENS.length) {
            throw new IllegalArgumentException("expected " + TOKENS.length + " values, got "
                    + values.length);
        }
        this.values = values.clone();
    }

    /** Short name for logs and probe notes ({@code A}, later the livery names). */
    public String name() {
        return name;
    }

    /** Value of {@code token} in this palette. */
    public int get(PaletteToken token) {
        return values[Objects.requireNonNull(token, "token").ordinal()];
    }

    /** This palette with the overrides of {@code scope} on top (none for the A scheme). */
    public TacticalPalette withScope(TacticalLivery.Scope scope) {
        return this;
    }

    /**
     * Writes this palette into the {@link TacticalBoardTheme} fields. Render thread only; prefer
     * {@link #push}, which restores the previous palette.
     */
    public void apply() {
        // The tokens are still constants with the A values: nothing to write.
    }

    /** The palette whose values the theme fields hold right now. */
    public static TacticalPalette active() {
        return A;
    }

    /**
     * Applies {@code palette} until the returned handle is closed, then restores the palette that
     * was active before; scopes may nest. Use with try-with-resources so an exception during
     * rendering cannot leave a faction palette behind for the HUD.
     */
    public static Applied push(TacticalPalette palette) {
        Objects.requireNonNull(palette, "palette");
        return () -> {
        };
    }

    /** Handle of one {@link #push}; closing it restores the previous palette. */
    @FunctionalInterface
    public interface Applied extends AutoCloseable {
        @Override
        void close();
    }

    @Override
    public String toString() {
        return "TacticalPalette[" + name + "]";
    }

    /** Snapshot of the theme fields, indexed by {@link PaletteToken#ordinal()}. */
    private static int[] readTheme() {
        int[] read = new int[TOKENS.length];
        for (PaletteToken token : TOKENS) {
            read[token.ordinal()] = themeValue(token);
        }
        return read;
    }

    /** Current value of the {@link TacticalBoardTheme} field named by {@code token}. */
    static int themeValue(PaletteToken token) {
        return switch (token) {
            case FRAME -> TacticalBoardTheme.FRAME;
            case FRAME_MID -> TacticalBoardTheme.FRAME_MID;
            case RIVET -> TacticalBoardTheme.RIVET;
            case BOARD -> TacticalBoardTheme.BOARD;
            case BOARD_ALT -> TacticalBoardTheme.BOARD_ALT;
            case CARD -> TacticalBoardTheme.CARD;
            case CARD_HOVER -> TacticalBoardTheme.CARD_HOVER;
            case CARD_PRESSED -> TacticalBoardTheme.CARD_PRESSED;
            case CARD_DISABLED -> TacticalBoardTheme.CARD_DISABLED;
            case CARD_LIP -> TacticalBoardTheme.CARD_LIP;
            case CARD_LIP_HOVER -> TacticalBoardTheme.CARD_LIP_HOVER;
            case BEVEL -> TacticalBoardTheme.BEVEL;
            case BADGE_BG -> TacticalBoardTheme.BADGE_BG;
            case WELL -> TacticalBoardTheme.WELL;
            case WELL_ROW -> TacticalBoardTheme.WELL_ROW;
            case WELL_ROW_ALT -> TacticalBoardTheme.WELL_ROW_ALT;
            case WELL_EDGE -> TacticalBoardTheme.WELL_EDGE;
            case WELL_LIGHT_EDGE -> TacticalBoardTheme.WELL_LIGHT_EDGE;
            case ROW_HOVER -> TacticalBoardTheme.ROW_HOVER;
            case CELL -> TacticalBoardTheme.CELL;
            case CELL_EDGE -> TacticalBoardTheme.CELL_EDGE;
            case INPUT_EDGE -> TacticalBoardTheme.INPUT_EDGE;
            case BORDER -> TacticalBoardTheme.BORDER;
            case BORDER_DARK -> TacticalBoardTheme.BORDER_DARK;
            case EDGE -> TacticalBoardTheme.EDGE;
            case TEXT -> TacticalBoardTheme.TEXT;
            case MUTED -> TacticalBoardTheme.MUTED;
            case FAINT -> TacticalBoardTheme.FAINT;
            case LIGHT -> TacticalBoardTheme.LIGHT;
            case LIGHT_MUTED -> TacticalBoardTheme.LIGHT_MUTED;
            case OFFLINE -> TacticalBoardTheme.OFFLINE;
            case DISABLED_TEXT -> TacticalBoardTheme.DISABLED_TEXT;
            case ON_FILL -> TacticalBoardTheme.ON_FILL;
            case SELECT -> TacticalBoardTheme.SELECT;
            case SELECT_HOVER -> TacticalBoardTheme.SELECT_HOVER;
            case SELECT_EDGE -> TacticalBoardTheme.SELECT_EDGE;
            case SELECT_BAR -> TacticalBoardTheme.SELECT_BAR;
            case SELECT_SUB -> TacticalBoardTheme.SELECT_SUB;
            case SELECT_WELL -> TacticalBoardTheme.SELECT_WELL;
            case ON_SELECT -> TacticalBoardTheme.ON_SELECT;
            case ACCENT -> TacticalBoardTheme.ACCENT;
            case ACCENT_SOFT -> TacticalBoardTheme.ACCENT_SOFT;
            case ACCENT_TEXT -> TacticalBoardTheme.ACCENT_TEXT;
            case SECTION -> TacticalBoardTheme.SECTION;
            case ADJUST -> TacticalBoardTheme.ADJUST;
            case ADJUST_SOFT -> TacticalBoardTheme.ADJUST_SOFT;
            case DANGER -> TacticalBoardTheme.DANGER;
            case DANGER_HOVER -> TacticalBoardTheme.DANGER_HOVER;
            case DANGER_SOFT -> TacticalBoardTheme.DANGER_SOFT;
            case DANGER_DEEP -> TacticalBoardTheme.DANGER_DEEP;
            case SUCCESS -> TacticalBoardTheme.SUCCESS;
            case SUCCESS_HOVER -> TacticalBoardTheme.SUCCESS_HOVER;
            case SUCCESS_SOFT -> TacticalBoardTheme.SUCCESS_SOFT;
            case SUCCESS_EDGE -> TacticalBoardTheme.SUCCESS_EDGE;
            case DISABLED_EDGE -> TacticalBoardTheme.DISABLED_EDGE;
            case SELECT_B -> TacticalBoardTheme.SELECT_B;
            case ACCENT_B -> TacticalBoardTheme.ACCENT_B;
            case DANGER_B -> TacticalBoardTheme.DANGER_B;
            case SUCCESS_B -> TacticalBoardTheme.SUCCESS_B;
            case NEUTRAL_B -> TacticalBoardTheme.NEUTRAL_B;
            case SECTION_B -> TacticalBoardTheme.SECTION_B;
            case ADJUST_B -> TacticalBoardTheme.ADJUST_B;
            case FOCUS -> TacticalBoardTheme.FOCUS;
            case SILHOUETTE -> TacticalBoardTheme.SILHOUETTE;
            case TAB_HOVER -> TacticalBoardTheme.TAB_HOVER;
            case KEYCAP_EDGE -> TacticalBoardTheme.KEYCAP_EDGE;
            case FEEDBACK_BG -> TacticalBoardTheme.FEEDBACK_BG;
            case SCROLL_TRACK -> TacticalBoardTheme.SCROLL_TRACK;
            case TOOLTIP_BG -> TacticalBoardTheme.TOOLTIP_BG;
            case TOOLTIP_EDGE -> TacticalBoardTheme.TOOLTIP_EDGE;
            case HUD_TRACK -> TacticalBoardTheme.HUD_TRACK;
        };
    }
}
