package com.wok.capturepoints.client;

/**
 * Pure geometry of this add-on's thin capture strip (0.1.0-alpha.4), drawn only when WOK步战核心
 * does not show the point in its own battle strip: without the core, with a core older than
 * 0.5.0-beta.2, or with the core's {@code hud.showBattleStrip} turned off.
 *
 * <p>The strip is centred at the top, at most {@value #MAX_WIDTH} wide, one
 * {@value #HEIGHT}px row "name │ blue ▬▬|▬▬ red" (the bar runs out from its centre line towards
 * the side the control leans to, blue left and red right like the core's battle strip). On wide
 * screens (layout at least {@value #WIDE_MIN_WIDTH}×{@value #WIDE_MIN_HEIGHT}, the core HUD's
 * non-tight tier) a second row carries the status text, {@value #HEIGHT_WIDE}px in all.
 *
 * <p>Layout pixels are GUI pixels divided by {@link #factor}: at GUI scale 1 on a large window
 * the strip is drawn at 2× (as the core HUD) so CJK text stays readable. With the core installed
 * the strip goes into the core's {@code top_center_next} slot (under its plates, in the strip
 * column); without a slot it sits where alpha.3 kept its panel clear of the core's old banner and
 * roster (GUI y {@value #FALLBACK_TOP_WITH_CORE} centred, or from x {@value #FALLBACK_COMPACT_LEFT}
 * at y {@value #FALLBACK_COMPACT_TOP} on screens up to {@value #FALLBACK_COMPACT_MAX_WIDTH} wide),
 * or at the top edge without the core. Either way it moves below vanilla boss bars that really
 * cover it; bars a core has already moved below it are left alone (moving under those would make
 * the core move them again, and the two would chase each other off the screen). A core
 * 0.5.0-beta.2+ moves the bars below a strip that starts in the first boss row, so there the
 * strip keeps its place even while bars cover it for a frame
 * ({@link #CORE_CLEARS_BOSS_ROW_ABOVE}).
 */
public final class CaptureStripLayout {
    public static final int MAX_WIDTH = 200;
    public static final int HEIGHT = 14;
    public static final int HEIGHT_WIDE = 23;
    public static final int WIDE_MIN_WIDTH = 400;
    public static final int WIDE_MIN_HEIGHT = 280;
    /** Screen edge margin without a core slot. */
    public static final int EDGE = 4;
    /** GUI y under the core's top plates when the core gives no slot (alpha.3's place). */
    public static final int FALLBACK_TOP_WITH_CORE = 28;
    /**
     * Without a slot on screens up to this GUI width alpha.3 drew its compact panel right of the
     * core's roster, from {@link #FALLBACK_COMPACT_LEFT} to the right edge less
     * {@link #FALLBACK_COMPACT_RIGHT}, at {@link #FALLBACK_COMPACT_TOP}.
     */
    public static final int FALLBACK_COMPACT_MAX_WIDTH = 360;
    public static final int FALLBACK_COMPACT_LEFT = 140;
    public static final int FALLBACK_COMPACT_RIGHT = 8;
    public static final int FALLBACK_COMPACT_TOP = 26;
    /** Text inset from the plate's left and right edges. */
    public static final int PAD = 4;
    /** First and second text rows below the plate's top. */
    public static final int TEXT_TOP = 3;
    public static final int STATUS_TOP = 12;
    /** Gap on each side of the 1px divider after the name. */
    public static final int GAP = 4;
    /** Gap between a player count and the bar. */
    public static final int COUNT_GAP = 3;
    /** The bar never gets narrower than this; a long name is shortened instead. */
    public static final int BAR_MIN = 20;
    /** GUI pixels kept between the strip and the vanilla boss bars. */
    public static final int BOSS_CLEARANCE = 4;
    /** A vanilla boss bar at y: its name row starts this far above y, the bar is this tall. */
    public static final int BOSS_TITLE_ABOVE = 9;
    public static final int BOSS_BAR_HEIGHT = 5;
    /** Vanilla boss bars span the screen centre ± this (BossHealthOverlay). */
    public static final int BOSS_HALF_WIDTH = 91;
    /**
     * WOK步战核心 0.5.0-beta.2+ moves the boss bars below a reported strip in their column whose
     * top (GUI y) is above this: first bar 12 + 5 tall + 4 clearance. Such a strip stays where it
     * is and leaves the bars to the core; going under them instead would make the core treat the
     * strip as an obstacle beside the bars and stop moving them clear of the squad roster.
     */
    public static final int CORE_CLEARS_BOSS_ROW_ABOVE = 21;
    /** Smallest layout the 2× rule may produce (WOK步战核心 {@code UiScale}). */
    static final int MIN_LAYOUT_WIDTH = 320;
    static final int MIN_LAYOUT_HEIGHT = 240;

    private CaptureStripLayout() {
    }

    /** The strip's plate in layout pixels; {@link #guiRect} gives GUI pixels. */
    public record Plate(int factor, int left, int top, int right, int bottom, boolean wide) {
        public int width() {
            return right - left;
        }

        public int height() {
            return bottom - top;
        }

        /** {@code {left, top, width, height}} in GUI pixels. */
        public int[] guiRect() {
            return new int[]{left * factor, top * factor, width() * factor, height() * factor};
        }
    }

    /**
     * Where the parts of the row go (layout pixels): the name from {@code nameX} with
     * {@code nameRoom} (0 = no room, the name and divider are left out), the divider, the blue
     * count, the bar [{@code barLeft}, {@code barRight}) with its centre line at {@code centre},
     * the red count, and the status row.
     */
    public record Row(int nameX, int nameRoom, int dividerX, int blueX, int barLeft,
                      int barRight, int centre, int redX, int textY, int statusY,
                      int statusRoom) {
        public int barTop() {
            return textY + 2;
        }

        public int barBottom() {
            return textY + 5;
        }
    }

    /**
     * WOK步战核心's 2× rule: 2 at GUI scale 1 while half the scaled window is still at least
     * 320×240, otherwise 1.
     */
    public static int factor(double guiScale, int guiWidth, int guiHeight) {
        if (!(Math.abs(guiScale - 1.0D) < 1.0E-6D)) {
            return 1;
        }
        return guiWidth / 2 >= MIN_LAYOUT_WIDTH && guiHeight / 2 >= MIN_LAYOUT_HEIGHT ? 2 : 1;
    }

    /** Whether a layout this size shows the status row. */
    public static boolean wide(int layoutWidth, int layoutHeight) {
        return layoutWidth >= WIDE_MIN_WIDTH && layoutHeight >= WIDE_MIN_HEIGHT;
    }

    /**
     * GUI y where the name row of a boss bar drawn at {@code barY} (moved down by {@code shiftY})
     * starts.
     */
    public static int bossTop(int barY, int shiftY) {
        return barY + shiftY - BOSS_TITLE_ABOVE;
    }

    /** GUI y under a boss bar drawn at {@code barY} (moved down by {@code shiftY}). */
    public static int bossBottom(int barY, int shiftY) {
        return barY + shiftY + BOSS_BAR_HEIGHT;
    }

    /**
     * The plate on a {@code guiWidth}×{@code guiHeight} screen.
     *
     * @param slot          the core's {@code top_center_next} slot {@code {left, top, width,
     *                      height}} in GUI pixels, or null without one
     * @param coreLoaded    WOK步战核心 is installed (used only without a slot)
     * @param coreMovesBossBars the installed core moves the boss bars below this strip when it
     *                      starts above {@link #CORE_CLEARS_BOSS_ROW_ABOVE} (core 0.5.0-beta.2+)
     * @param bossTopGui    GUI y of the first boss bar's name row as drawn ({@link #bossTop})
     * @param bossBottomGui GUI y under the lowest vanilla boss bar drawn ({@link #bossBottom});
     *                      0 or less = no boss bar
     */
    public static Plate place(int guiWidth, int guiHeight, int factor, int[] slot,
                              boolean coreLoaded, boolean coreMovesBossBars, int bossTopGui,
                              int bossBottomGui) {
        int f = Math.max(1, factor);
        int width = Math.max(1, guiWidth / f);
        int height = Math.max(1, guiHeight / f);
        boolean wide = wide(width, height);
        int plateHeight = wide ? HEIGHT_WIDE : HEIGHT;
        int left;
        int plateWidth;
        int top;
        if (validSlot(slot)) {
            int slotLeft = Math.floorDiv(slot[0], f);
            int slotRight = Math.floorDiv(slot[0] + slot[2], f);
            plateWidth = Math.max(0, Math.min(MAX_WIDTH, slotRight - slotLeft));
            left = slotLeft + (slotRight - slotLeft - plateWidth) / 2;
            top = ceilDiv(slot[1], f);
        } else if (coreLoaded && guiWidth <= FALLBACK_COMPACT_MAX_WIDTH) {
            left = ceilDiv(FALLBACK_COMPACT_LEFT, f);
            plateWidth = Math.max(0, Math.min(MAX_WIDTH,
                    Math.floorDiv(guiWidth - FALLBACK_COMPACT_RIGHT, f) - left));
            top = ceilDiv(FALLBACK_COMPACT_TOP, f);
        } else {
            plateWidth = Math.max(0, Math.min(MAX_WIDTH, width - 2 * EDGE));
            left = (width - plateWidth) / 2;
            top = coreLoaded ? ceilDiv(FALLBACK_TOP_WITH_CORE, f) : EDGE;
        }
        boolean coreClears = coreMovesBossBars && top * f < CORE_CLEARS_BOSS_ROW_ABOVE;
        if (bossBottomGui > 0 && !coreClears) {
            int guiLeft = left * f;
            int guiRight = (left + plateWidth) * f;
            boolean sharesColumn = guiLeft < guiWidth / 2 + BOSS_HALF_WIDTH
                    && guiRight > guiWidth / 2 - BOSS_HALF_WIDTH;
            // Only bars that reach into the strip (clearance included) push it down. Bars already
            // under it stay there: a core that moved them below the strip would otherwise move
            // them again below the moved strip, every frame.
            boolean covered = top * f < bossBottomGui + BOSS_CLEARANCE
                    && (top + plateHeight) * f + BOSS_CLEARANCE > bossTopGui;
            if (sharesColumn && covered) {
                top = ceilDiv(bossBottomGui + BOSS_CLEARANCE, f);
            }
        }
        return new Plate(f, left, top, left + plateWidth, top + plateHeight, wide);
    }

    /** The row of {@code plate} for the given text widths (layout pixels). */
    public static Row row(Plate plate, int nameWidth, int blueWidth, int redWidth) {
        int textY = plate.top() + TEXT_TOP;
        int redX = plate.right() - PAD - Math.max(0, redWidth);
        int fixed = PAD + GAP + 1 + GAP + Math.max(0, blueWidth) + COUNT_GAP + BAR_MIN
                + COUNT_GAP + Math.max(0, redWidth) + PAD;
        int nameRoom = Math.max(0, plate.width() - fixed);
        int nameUsed = Math.min(Math.max(0, nameWidth), nameRoom);
        int nameX = plate.left() + PAD;
        int dividerX;
        int blueX;
        if (nameUsed > 0) {
            dividerX = nameX + nameUsed + GAP;
            blueX = dividerX + 1 + GAP;
        } else {
            dividerX = -1;
            blueX = nameX;
            nameRoom = 0;
        }
        int barLeft = blueX + Math.max(0, blueWidth) + COUNT_GAP;
        int barRight = Math.max(barLeft, redX - COUNT_GAP);
        int centre = (barLeft + barRight) / 2;
        return new Row(nameX, nameRoom, dividerX, blueX, barLeft, barRight, centre, redX, textY,
                plate.top() + STATUS_TOP, Math.max(0, plate.width() - 2 * PAD));
    }

    /**
     * Filled part {@code {from, to}} of the bar for {@code control} (−1..1): from the centre
     * line towards the left for blue (control above 0), towards the right for red.
     */
    public static int[] fill(Row row, double control) {
        double value = Double.isFinite(control) ? Math.max(-1.0D, Math.min(1.0D, control)) : 0.0D;
        if (value > 0.0D) {
            int length = (int) Math.round(value * (row.centre() - row.barLeft()));
            return new int[]{row.centre() - length, row.centre()};
        }
        int length = (int) Math.round(-value * (row.barRight() - row.centre()));
        return new int[]{row.centre(), row.centre() + length};
    }

    private static boolean validSlot(int[] slot) {
        return slot != null && slot.length == 4 && slot[2] > 0 && slot[3] > 0;
    }

    private static int ceilDiv(int value, int divisor) {
        return -Math.floorDiv(-value, divisor);
    }
}
