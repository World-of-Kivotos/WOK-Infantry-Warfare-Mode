package com.wok.infantry.client.screen;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Pure layout of the device's bottom bezel (the preview's {@code pageKeys} in
 * {@code 17-device.js}, D2): the hardware Esc key at the left end, the R key at the right end and
 * one equal-width hardware key per page between them, centred under the screen where the slot
 * allows. A small LED sits above every page key; the current page's LED is lit.
 *
 * <p>The bezel is {@link TacticalShellLayout#bezel()}: as wide as the glass opening, from the row
 * below the glass to the case's bottom edge (preview {@code (S.l, S.b + 1, S.r, D.b)}). Constants
 * per size class (tight / standard / roomy):
 * <ul>
 *   <li>keys end {@code 1 / 3 / 3} px above the case edge, inset {@code 3 / 12 / 12} px from the
 *       glass edges;</li>
 *   <li>LED row {@code 2 / 3 / 3} px below the bezel top, {@code 1 / 2 / 2} px tall, keys
 *       {@code 1 / 2 / 2} px below it; LED half-width {@code 3 / 5 / 5};</li>
 *   <li>key label padding {@code 8 / 12 / 18}, minimum page key width {@code 28 / 38 / 38}, gap
 *       between page keys {@code 2 / 4 / 4}, gap to Esc and R {@code 5 / 12 / 12}.</li>
 * </ul>
 * A bezel too low for LEDs and a readable key (less than {@link #MIN_KEY_HEIGHT}) drops the LED
 * row and lets the keys use its height. When the page keys would only fit as a pager between
 * "Esc 返回" and "R 刷新" (long translations on the compact class), Esc and R shrink to their key
 * names first ({@link #plan(Font, TacticalShellLayout, TacticalBoardChrome.KeyHint,
 * TacticalBoardChrome.KeyHint, TacticalTabStrip)}).
 *
 * @param density   size class of the screen
 * @param bezel     the whole bezel strip; page keys centre on its middle
 * @param esc       the Esc key cap, or {@link UiRect#EMPTY} without an Esc key
 * @param refresh   the R key cap, or {@link UiRect#EMPTY} without an R key
 * @param pages     bounds of the page key strip: the slot between Esc and R, from the LED glow
 *                  (one row above the LEDs) to the bottom of the keys
 * @param ledTop    first row of the page LEDs
 * @param ledHeight LED height, 0 without LEDs
 * @param keyTop    top of every key cap
 * @param keyBottom bottom (exclusive) of every key cap
 */
public record TacticalBezelPlan(TacticalShellLayout.Density density, UiRect bezel, UiRect esc,
                                UiRect refresh, UiRect pages, int ledTop, int ledHeight,
                                int keyTop, int keyBottom) {
    /** Lowest key cap that still centres the 8px glyphs inside its lips. */
    public static final int MIN_KEY_HEIGHT = 10;
    /**
     * Tab order group of the bezel keys: after every control of the page (group 0), in the
     * left-to-right order Esc, page keys, R. Vanilla Tab navigation sorts by this group.
     */
    public static final int TAB_ORDER_ESC = 1000;
    public static final int TAB_ORDER_PAGES = 1001;
    public static final int TAB_ORDER_REFRESH = 1002;
    /** Key text of the Esc hint ({@link TacticalBoardChrome.KeyHint#close()} and back()). */
    static final String ESC_KEY = "Esc";

    /**
     * Plans the bezel for an Esc key {@code escWidth} wide and an R key {@code refreshWidth} wide
     * (0 leaves a key out; see {@link #pairWidth}).
     */
    public static TacticalBezelPlan plan(UiRect bezel, TacticalShellLayout.Density density,
                                         int escWidth, int refreshWidth) {
        TacticalShellLayout.Density size = density == null
                ? TacticalShellLayout.Density.STANDARD : density;
        boolean tight = size == TacticalShellLayout.Density.COMPACT;
        int escW = Math.max(0, escWidth);
        int refreshW = Math.max(0, refreshWidth);
        int top = bezel.top();
        int bottom = Math.max(top, bezel.bottom() - (tight ? 1 : 3));
        int ledTop = top + (tight ? 2 : 3);
        int ledHeight = tight ? 1 : 2;
        int keyTop = ledTop + ledHeight + (tight ? 1 : 2);
        if (bottom - keyTop < MIN_KEY_HEIGHT) {
            // Too low for LEDs: the keys take the whole row.
            ledTop = top + 1;
            ledHeight = 0;
            keyTop = Math.min(bottom, top + 1);
        }
        int left = bezel.left() + inset(size);
        int right = bezel.right() - inset(size);
        int separation = separation(size);
        UiRect esc = escW > 0 ? new UiRect(left, keyTop, left + escW, bottom) : UiRect.EMPTY;
        UiRect refresh = refreshW > 0 ? new UiRect(right - refreshW, keyTop, right, bottom)
                : UiRect.EMPTY;
        int pagesLeft = left + escW + separation;
        int pagesRight = Math.max(pagesLeft, right - refreshW - separation);
        int pagesTop = ledHeight > 0 ? ledTop - 1 : keyTop;
        UiRect pages = new UiRect(pagesLeft, pagesTop, pagesRight, bottom);
        return new TacticalBezelPlan(size, bezel, esc, refresh, pages, ledTop, ledHeight, keyTop,
                bottom);
    }

    /**
     * Plans the bezel of {@code layout} for real text widths. {@code esc} and {@code refresh} may
     * be {@code null}.
     */
    public static TacticalBezelPlan plan(Font font, TacticalShellLayout layout,
                                         TacticalBoardChrome.KeyHint esc,
                                         TacticalBoardChrome.KeyHint refresh) {
        TacticalShellLayout.Density density = layout.density();
        return plan(layout.bezel(), density, pairWidth(font, density, esc),
                pairWidth(font, density, refresh));
    }

    /**
     * Plans the bezel for a screen's {@link TacticalScreen#bezelHints()}: the hint whose key is
     * {@code Esc} goes to the left end, the first other hint to the right end.
     */
    public static TacticalBezelPlan plan(Font font, TacticalShellLayout layout,
                                         List<TacticalBoardChrome.KeyHint> hints) {
        return plan(font, layout, hints, null);
    }

    /**
     * {@link #plan(Font, TacticalShellLayout, List)} for the page keys of {@code pages} (a
     * {@link TacticalTabStrip.Skin#BEZEL} strip, may be {@code null}): see
     * {@link #plan(Font, TacticalShellLayout, TacticalBoardChrome.KeyHint,
     * TacticalBoardChrome.KeyHint, TacticalTabStrip)}.
     */
    public static TacticalBezelPlan plan(Font font, TacticalShellLayout layout,
                                         List<TacticalBoardChrome.KeyHint> hints,
                                         TacticalTabStrip pages) {
        TacticalBoardChrome.KeyHint esc = null;
        TacticalBoardChrome.KeyHint refresh = null;
        for (TacticalBoardChrome.KeyHint hint : hints == null
                ? List.<TacticalBoardChrome.KeyHint>of() : hints) {
            if (hint == null) {
                continue;
            }
            if (esc == null && isEscape(hint)) {
                esc = hint;
            } else if (refresh == null && !isEscape(hint)) {
                refresh = hint;
            }
        }
        return plan(font, layout, esc, refresh, pages);
    }

    /**
     * Plans the bezel of {@code layout} for the Esc and R keys and the page keys of {@code pages}
     * (a {@link TacticalTabStrip.Skin#BEZEL} strip; {@code null} or another skin plans the keys
     * alone). The Esc and R caps carry their action ("Esc 返回") unless the page keys would then
     * fall back to the {@code ‹ name n/m ›} pager: before that, the caps shrink to the key name
     * ({@link #keyOnlyWidths}); the action stays in the key's tooltip and narration
     * ({@link BezelKey} draws the name alone when the action does not fit). When even that does
     * not let the page keys show, the full caps stay and the strip pages.
     */
    public static TacticalBezelPlan plan(Font font, TacticalShellLayout layout,
                                         TacticalBoardChrome.KeyHint esc,
                                         TacticalBoardChrome.KeyHint refresh,
                                         TacticalTabStrip pages) {
        TacticalBezelPlan full = plan(font, layout, esc, refresh);
        if (font == null || pages == null || pages.skin() != TacticalTabStrip.Skin.BEZEL
                || pages.tabs().isEmpty()) {
            return full;
        }
        TacticalShellLayout.Density density = layout.density();
        return planFitting(layout.bezel(), density, pairWidth(font, density, esc),
                keyWidth(font, density, esc), pairWidth(font, density, refresh),
                keyWidth(font, density, refresh),
                candidate -> pages.modeOn(font, candidate) != TacticalTabStrip.Mode.PAGER);
    }

    /**
     * Pure rule of {@link #plan(Font, TacticalShellLayout, TacticalBoardChrome.KeyHint,
     * TacticalBoardChrome.KeyHint, TacticalTabStrip)}: the plan with the full Esc / R caps
     * ({@code escWidth}, {@code refreshWidth}, 0 = no key) when {@code pagesFit} accepts it,
     * else the first accepted plan of {@link #keyOnlyWidths} (key-name caps as wide as a page key,
     * then as narrow as the name, {@code escKeyWidth} / {@code refreshKeyWidth}), else the full
     * caps again.
     */
    static TacticalBezelPlan planFitting(UiRect bezel, TacticalShellLayout.Density density,
                                         int escWidth, int escKeyWidth, int refreshWidth,
                                         int refreshKeyWidth,
                                         Predicate<TacticalBezelPlan> pagesFit) {
        TacticalBezelPlan full = plan(bezel, density, escWidth, refreshWidth);
        if (pagesFit.test(full)) {
            return full;
        }
        int[] esc = keyOnlyWidths(density, escWidth, escKeyWidth);
        int[] refresh = keyOnlyWidths(density, refreshWidth, refreshKeyWidth);
        for (int index = 0; index < esc.length; index++) {
            TacticalBezelPlan shrunk = plan(bezel, density, esc[index], refresh[index]);
            if (pagesFit.test(shrunk)) {
                return shrunk;
            }
        }
        return full;
    }

    /**
     * The key-name cap widths tried before the pager, widest first: as wide as a page key
     * ({@link #minKeyWidth}, so the hardware keys still look like keys), then the bare name and
     * its padding ({@code keyWidth}). Never wider than the full cap ({@code fullWidth}); a missing
     * key ({@code fullWidth} 0) stays 0.
     */
    static int[] keyOnlyWidths(TacticalShellLayout.Density density, int fullWidth, int keyWidth) {
        if (fullWidth <= 0) {
            return new int[]{0, 0};
        }
        int bare = Math.min(fullWidth, Math.max(0, keyWidth));
        return new int[]{Math.min(fullWidth, Math.max(bare, minKeyWidth(density))), bare};
    }

    /** Width of an Esc or R cap that shows only the key name ({@code null}: 0). */
    static int keyWidth(Font font, TacticalShellLayout.Density density,
                        TacticalBoardChrome.KeyHint hint) {
        if (hint == null || font == null) {
            return 0;
        }
        return font.width(hint.key()) + pad(density);
    }

    /**
     * A page key row that fills {@code bounds} without Esc, R or LEDs, for a bezel strip that sits
     * somewhere else (an older header slot); its keys centre on {@code bounds}.
     */
    public static TacticalBezelPlan inBounds(UiRect bounds, TacticalShellLayout.Density density) {
        TacticalShellLayout.Density size = density == null
                ? TacticalShellLayout.Density.STANDARD : density;
        return new TacticalBezelPlan(size, bounds, UiRect.EMPTY, UiRect.EMPTY, bounds,
                bounds.top(), 0, bounds.top(), Math.max(bounds.top(), bounds.bottom()));
    }

    /** Whether {@code hint} is the Esc key's hint. */
    static boolean isEscape(TacticalBoardChrome.KeyHint hint) {
        return hint != null && ESC_KEY.equalsIgnoreCase(hint.key().getString().strip());
    }

    /** Width of an Esc or R key for {@code hint} ({@code null}: 0, the key is left out). */
    static int pairWidth(Font font, TacticalShellLayout.Density density,
                         TacticalBoardChrome.KeyHint hint) {
        if (hint == null || font == null) {
            return 0;
        }
        return pairWidth(density, font.width(hint.key()), font.width(hint.action()));
    }

    /**
     * Width of an Esc or R key: key name, 4px, action label and the size class's label padding
     * (preview {@code pairW}).
     */
    public static int pairWidth(TacticalShellLayout.Density density, int keyTextWidth,
                                int actionWidth) {
        return Math.max(0, keyTextWidth) + BezelKey.LABEL_GAP + Math.max(0, actionWidth)
                + pad(density);
    }

    // ---- size-class constants -------------------------------------------------------------------

    public boolean tight() {
        return density == TacticalShellLayout.Density.COMPACT;
    }

    /** Label padding of a key (both sides together). */
    public int pad() {
        return pad(density);
    }

    static int pad(TacticalShellLayout.Density density) {
        return switch (density == null ? TacticalShellLayout.Density.STANDARD : density) {
            case COMPACT -> 8;
            case STANDARD -> 12;
            case ROOMY -> 18;
        };
    }

    /** How far the Esc and R keys sit in from the ends of the bezel. */
    public static int inset(TacticalShellLayout.Density density) {
        return density == TacticalShellLayout.Density.COMPACT ? 3 : 12;
    }

    /** Gap between two page keys. */
    public int gap() {
        return tight() ? 2 : 4;
    }

    /** Narrowest page key. */
    public int minKeyWidth() {
        return minKeyWidth(density);
    }

    static int minKeyWidth(TacticalShellLayout.Density density) {
        return density == TacticalShellLayout.Density.COMPACT ? 28 : 38;
    }

    /** Gap between the page keys and the Esc / R keys. */
    public int separation() {
        return separation(density);
    }

    static int separation(TacticalShellLayout.Density density) {
        return density == TacticalShellLayout.Density.COMPACT ? 5 : 12;
    }

    /** Half the width of a page LED. */
    public int ledHalfWidth() {
        return tight() ? 3 : 5;
    }

    public boolean hasLeds() {
        return ledHeight > 0;
    }

    // ---- page keys ------------------------------------------------------------------------------

    /** Width of every page key when the widest label (with its badge) is {@code widestLabel}. */
    public int keyWidth(int widestLabel) {
        return Math.max(minKeyWidth(), Math.max(0, widestLabel) + pad());
    }

    /** Width of {@code count} page keys of {@code keyWidth} with their gaps. */
    public int rowWidth(int count, int keyWidth) {
        return count <= 0 ? 0 : count * keyWidth + (count - 1) * gap();
    }

    /**
     * Cells of {@code count} page keys of {@code keyWidth}: centred under the screen (the middle of
     * the bezel), pushed right of Esc and left of R when the centre does not leave room.
     */
    public List<UiRect> keys(int count, int keyWidth) {
        if (count <= 0) {
            return List.of();
        }
        int total = rowWidth(count, keyWidth);
        int centred = (int) Math.round((bezel.left() + bezel.right() - total) / 2.0D);
        int x = Math.max(pages.left(), Math.min(centred, pages.right() - total));
        List<UiRect> keys = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            keys.add(new UiRect(x, keyTop, x + keyWidth, keyBottom));
            x += keyWidth + gap();
        }
        return keys;
    }

    /** The row of the page keys across the whole page slot (pager mode lays out inside it). */
    public UiRect keyRow() {
        return new UiRect(pages.left(), keyTop, pages.right(), keyBottom);
    }

    /** The LED above {@code key}, or {@link UiRect#EMPTY} without LEDs. */
    public UiRect led(UiRect key) {
        if (!hasLeds() || key.isEmpty()) {
            return UiRect.EMPTY;
        }
        int middle = key.left() + key.width() / 2;
        return new UiRect(middle - ledHalfWidth(), ledTop, middle + ledHalfWidth(),
                ledTop + ledHeight);
    }

    /** The glow drawn under a lit LED: the LED grown by one pixel. */
    public UiRect ledGlow(UiRect key) {
        UiRect led = led(key);
        return led.isEmpty() ? UiRect.EMPTY : led.inset(-1);
    }
}
