package com.wok.infantry.client.screen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * One complete set of values for the swappable {@link PaletteToken}s of
 * {@link TacticalBoardTheme}: the A scheme and the three P3 faction liveries (preview
 * {@code ui-preview/surfaces/18-device-livery.js}).
 *
 * <p>A {@link TacticalScreen} pushes the palette of its {@link TacticalLivery.Livery} for the
 * duration of one frame ({@link #push}), so every call site keeps reading the plain
 * {@code TacticalBoardTheme} fields while the HUD, drawn outside that scope, always sees
 * {@link #A}.
 *
 * <p>Every livery is "A, then the livery's overrides ({@link TacticalLiveryTables}), then the
 * derived tokens": the colours the P3 components read from other tokens unless the livery sets
 * them itself (preview {@code UIX}): the tooltip plate is {@code FRAME} at alpha {@code F2} with an
 * {@code INPUT_EDGE} outline, the scrollbar track is {@code FRAME}, a chip on a dark surface sits
 * on {@code WELL} and a hovered key keeps the {@code CARD_LIP} lip. Tokens the preview never
 * draws inside a livery ({@code TAB_HOVER}, {@code KEYCAP_EDGE}: old header tabs and footer key
 * caps) keep the A value. A page scope ({@link #withScope}) adds a few overrides on top; only the
 * Caesar map has one.
 *
 * <p>Only the render thread applies palettes; a second thread pushing while a scope is open is a
 * bug and fails fast.
 */
public final class TacticalPalette {
    private static final PaletteToken[] TOKENS = PaletteToken.values();

    /** The A · 战术平板 scheme: the {@link TacticalBoardTheme} values as declared. */
    public static final TacticalPalette A = new TacticalPalette("A", readTheme());

    /** Academy (blue side): navy chrome, blue-gray boards, academy-blue selection. */
    public static final TacticalPalette ACADEMY = livery("ACADEMY", TacticalLiveryTables.ACADEMY,
            null);
    /**
     * Caesar (red side): true-red chrome, warm-gray boards, deep crimson selection; its map page
     * selects in graphite with a rose bar, so a selection never reads as an enemy marker.
     */
    public static final TacticalPalette CAESAR = livery("CAESAR", TacticalLiveryTables.CAESAR,
            TacticalLiveryTables.CAESAR_MAP);
    /** Neutral (no faction): pale steel chrome with dark ink, graphite selection. */
    public static final TacticalPalette NEUTRAL = livery("NEUTRAL", TacticalLiveryTables.NEUTRAL,
            null);

    // Render thread only: the palette the theme fields hold, and the open push scopes (innermost
    // first) with the thread that opened them.
    private static TacticalPalette active = A;
    private static final Deque<Restore> SCOPES = new ArrayDeque<>();
    private static Thread scopeOwner;

    private final String name;
    private final int[] values;
    /** The same livery on the map page, or null when the map scope changes nothing. */
    private TacticalPalette map;
    /** For a scoped palette: the board palette it was made from (null for a board palette). */
    private TacticalPalette board;

    private TacticalPalette(String name, int[] values) {
        this.name = Objects.requireNonNull(name, "name");
        if (values.length != TOKENS.length) {
            throw new IllegalArgumentException("expected " + TOKENS.length + " values, got "
                    + values.length);
        }
        this.values = values.clone();
    }

    /** Short name for logs and probe notes ({@code A}, {@code ACADEMY}, {@code CAESAR/MAP}, ...). */
    public String name() {
        return name;
    }

    /** Value of {@code token} in this palette. */
    public int get(PaletteToken token) {
        return values[Objects.requireNonNull(token, "token").ordinal()];
    }

    /**
     * This palette on a page of {@code scope}: the scope's overrides on top of the board palette
     * (only the Caesar map has any), otherwise the board palette itself.
     */
    public TacticalPalette withScope(TacticalLivery.Scope scope) {
        TacticalPalette base = board != null ? board : this;
        return switch (Objects.requireNonNull(scope, "scope")) {
            case BOARD -> base;
            case MAP -> base.map != null ? base.map : base;
        };
    }

    /**
     * Writes this palette into the {@link TacticalBoardTheme} fields. Render thread only; prefer
     * {@link #push}, which restores the previous palette.
     */
    public void apply() {
        for (PaletteToken token : TOKENS) {
            writeTheme(token, values[token.ordinal()]);
        }
        active = this;
    }

    /** The palette whose values the theme fields hold right now. */
    public static TacticalPalette active() {
        return active;
    }

    /**
     * Applies {@code palette} until the returned handle is closed, then restores the palette that
     * was active before; scopes may nest. Use with try-with-resources so an exception during
     * rendering cannot leave a faction palette behind for the HUD.
     *
     * @throws IllegalStateException when another thread holds an open scope (render thread only)
     */
    public static Applied push(TacticalPalette palette) {
        Objects.requireNonNull(palette, "palette");
        Thread thread = Thread.currentThread();
        if (!SCOPES.isEmpty() && scopeOwner != thread) {
            throw new IllegalStateException("palette scope is open on " + scopeOwner.getName()
                    + "; palettes are render-thread only");
        }
        Restore scope = new Restore(active);
        SCOPES.push(scope);
        scopeOwner = thread;
        if (palette != active) {
            palette.apply();
        }
        return scope;
    }

    /** Number of open {@link #push} scopes (unit-test seam). */
    static int openScopes() {
        return SCOPES.size();
    }

    /** Handle of one {@link #push}; closing it restores the previous palette. */
    @FunctionalInterface
    public interface Applied extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * One open scope. Closing it also ends the scopes opened inside it and restores the palette
     * that was active when it was pushed; closing it again, or after an enclosing scope already
     * ended it, changes nothing.
     */
    private static final class Restore implements Applied {
        private final TacticalPalette previous;

        private Restore(TacticalPalette previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (!SCOPES.contains(this)) {
                return;
            }
            while (SCOPES.pop() != this) {
                // An inner scope left open: it ends with this one.
            }
            if (active != previous) {
                previous.apply();
            }
            if (SCOPES.isEmpty()) {
                scopeOwner = null;
            }
        }
    }

    @Override
    public String toString() {
        return "TacticalPalette[" + name + "]";
    }

    // ---- construction ---------------------------------------------------------------------------

    /** A livery: A plus {@code overrides} plus the derived tokens, with its optional map scope. */
    private static TacticalPalette livery(String name, Map<PaletteToken, Integer> overrides,
                                          Map<PaletteToken, Integer> mapOverrides) {
        TacticalPalette livery = new TacticalPalette(name, derive(overrides));
        if (mapOverrides != null && !mapOverrides.isEmpty()) {
            Map<PaletteToken, Integer> scoped = new EnumMap<>(PaletteToken.class);
            scoped.putAll(overrides);
            scoped.putAll(mapOverrides);
            TacticalPalette map = new TacticalPalette(name + "/MAP", derive(scoped));
            map.board = livery;
            livery.map = map;
        }
        return livery;
    }

    /**
     * A values with {@code overrides} on top, then every derived token the overrides do not set
     * (the P3 components read them from other tokens, preview {@code UIX}).
     */
    static int[] derive(Map<PaletteToken, Integer> overrides) {
        int[] next = A.values.clone();
        overrides.forEach((token, value) -> next[token.ordinal()] = value);
        int frame = next[PaletteToken.FRAME.ordinal()];
        deriveUnlessSet(next, overrides, PaletteToken.TOOLTIP_BG, 0xF2000000 | frame & 0xFFFFFF);
        deriveUnlessSet(next, overrides, PaletteToken.TOOLTIP_EDGE,
                next[PaletteToken.INPUT_EDGE.ordinal()]);
        deriveUnlessSet(next, overrides, PaletteToken.SCROLL_TRACK, frame);
        deriveUnlessSet(next, overrides, PaletteToken.FEEDBACK_BG, next[PaletteToken.WELL.ordinal()]);
        deriveUnlessSet(next, overrides, PaletteToken.CARD_LIP_HOVER,
                next[PaletteToken.CARD_LIP.ordinal()]);
        return next;
    }

    private static void deriveUnlessSet(int[] values, Map<PaletteToken, Integer> overrides,
                                        PaletteToken token, int value) {
        if (!overrides.containsKey(token)) {
            values[token.ordinal()] = value;
        }
    }

    /** Snapshot of the theme fields, indexed by {@link PaletteToken#ordinal()}. */
    private static int[] readTheme() {
        int[] read = new int[TOKENS.length];
        for (PaletteToken token : TOKENS) {
            read[token.ordinal()] = themeValue(token);
        }
        return read;
    }

    // ---- the theme fields, one explicit case per token -------------------------------------------

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

    /** Writes {@code value} into the {@link TacticalBoardTheme} field named by {@code token}. */
    private static void writeTheme(PaletteToken token, int value) {
        switch (token) {
            case FRAME -> TacticalBoardTheme.FRAME = value;
            case FRAME_MID -> TacticalBoardTheme.FRAME_MID = value;
            case RIVET -> TacticalBoardTheme.RIVET = value;
            case BOARD -> TacticalBoardTheme.BOARD = value;
            case BOARD_ALT -> TacticalBoardTheme.BOARD_ALT = value;
            case CARD -> TacticalBoardTheme.CARD = value;
            case CARD_HOVER -> TacticalBoardTheme.CARD_HOVER = value;
            case CARD_PRESSED -> TacticalBoardTheme.CARD_PRESSED = value;
            case CARD_DISABLED -> TacticalBoardTheme.CARD_DISABLED = value;
            case CARD_LIP -> TacticalBoardTheme.CARD_LIP = value;
            case CARD_LIP_HOVER -> TacticalBoardTheme.CARD_LIP_HOVER = value;
            case BEVEL -> TacticalBoardTheme.BEVEL = value;
            case BADGE_BG -> TacticalBoardTheme.BADGE_BG = value;
            case WELL -> TacticalBoardTheme.WELL = value;
            case WELL_ROW -> TacticalBoardTheme.WELL_ROW = value;
            case WELL_ROW_ALT -> TacticalBoardTheme.WELL_ROW_ALT = value;
            case WELL_EDGE -> TacticalBoardTheme.WELL_EDGE = value;
            case WELL_LIGHT_EDGE -> TacticalBoardTheme.WELL_LIGHT_EDGE = value;
            case ROW_HOVER -> TacticalBoardTheme.ROW_HOVER = value;
            case CELL -> TacticalBoardTheme.CELL = value;
            case CELL_EDGE -> TacticalBoardTheme.CELL_EDGE = value;
            case INPUT_EDGE -> TacticalBoardTheme.INPUT_EDGE = value;
            case BORDER -> TacticalBoardTheme.BORDER = value;
            case BORDER_DARK -> TacticalBoardTheme.BORDER_DARK = value;
            case EDGE -> TacticalBoardTheme.EDGE = value;
            case TEXT -> TacticalBoardTheme.TEXT = value;
            case MUTED -> TacticalBoardTheme.MUTED = value;
            case FAINT -> TacticalBoardTheme.FAINT = value;
            case LIGHT -> TacticalBoardTheme.LIGHT = value;
            case LIGHT_MUTED -> TacticalBoardTheme.LIGHT_MUTED = value;
            case OFFLINE -> TacticalBoardTheme.OFFLINE = value;
            case DISABLED_TEXT -> TacticalBoardTheme.DISABLED_TEXT = value;
            case ON_FILL -> TacticalBoardTheme.ON_FILL = value;
            case SELECT -> TacticalBoardTheme.SELECT = value;
            case SELECT_HOVER -> TacticalBoardTheme.SELECT_HOVER = value;
            case SELECT_EDGE -> TacticalBoardTheme.SELECT_EDGE = value;
            case SELECT_BAR -> TacticalBoardTheme.SELECT_BAR = value;
            case SELECT_SUB -> TacticalBoardTheme.SELECT_SUB = value;
            case SELECT_WELL -> TacticalBoardTheme.SELECT_WELL = value;
            case ON_SELECT -> TacticalBoardTheme.ON_SELECT = value;
            case ACCENT -> TacticalBoardTheme.ACCENT = value;
            case ACCENT_SOFT -> TacticalBoardTheme.ACCENT_SOFT = value;
            case ACCENT_TEXT -> TacticalBoardTheme.ACCENT_TEXT = value;
            case SECTION -> TacticalBoardTheme.SECTION = value;
            case ADJUST -> TacticalBoardTheme.ADJUST = value;
            case ADJUST_SOFT -> TacticalBoardTheme.ADJUST_SOFT = value;
            case DANGER -> TacticalBoardTheme.DANGER = value;
            case DANGER_HOVER -> TacticalBoardTheme.DANGER_HOVER = value;
            case DANGER_SOFT -> TacticalBoardTheme.DANGER_SOFT = value;
            case DANGER_DEEP -> TacticalBoardTheme.DANGER_DEEP = value;
            case SUCCESS -> TacticalBoardTheme.SUCCESS = value;
            case SUCCESS_HOVER -> TacticalBoardTheme.SUCCESS_HOVER = value;
            case SUCCESS_SOFT -> TacticalBoardTheme.SUCCESS_SOFT = value;
            case SUCCESS_EDGE -> TacticalBoardTheme.SUCCESS_EDGE = value;
            case DISABLED_EDGE -> TacticalBoardTheme.DISABLED_EDGE = value;
            case SELECT_B -> TacticalBoardTheme.SELECT_B = value;
            case ACCENT_B -> TacticalBoardTheme.ACCENT_B = value;
            case DANGER_B -> TacticalBoardTheme.DANGER_B = value;
            case SUCCESS_B -> TacticalBoardTheme.SUCCESS_B = value;
            case NEUTRAL_B -> TacticalBoardTheme.NEUTRAL_B = value;
            case SECTION_B -> TacticalBoardTheme.SECTION_B = value;
            case ADJUST_B -> TacticalBoardTheme.ADJUST_B = value;
            case FOCUS -> TacticalBoardTheme.FOCUS = value;
            case SILHOUETTE -> TacticalBoardTheme.SILHOUETTE = value;
            case TAB_HOVER -> TacticalBoardTheme.TAB_HOVER = value;
            case KEYCAP_EDGE -> TacticalBoardTheme.KEYCAP_EDGE = value;
            case FEEDBACK_BG -> TacticalBoardTheme.FEEDBACK_BG = value;
            case SCROLL_TRACK -> TacticalBoardTheme.SCROLL_TRACK = value;
            case TOOLTIP_BG -> TacticalBoardTheme.TOOLTIP_BG = value;
            case TOOLTIP_EDGE -> TacticalBoardTheme.TOOLTIP_EDGE = value;
            case HUD_TRACK -> TacticalBoardTheme.HUD_TRACK = value;
        }
    }
}
