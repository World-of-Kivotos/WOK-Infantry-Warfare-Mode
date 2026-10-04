package com.wok.bodyhealth.client;

/**
 * Pure HUD geometry shared by the overlay, the companion-HUD API and tests.
 * The whole HUD stays left of the hotbar's left edge, so it never covers the
 * hotbar, experience bar or the vanilla/modded status rows stacked above it.
 */
public final class BodyHealthHudLayout {
    public static final int FIGURE_WIDTH = 39;
    public static final int FIGURE_HEIGHT = 48;
    /** Centres the figure texture at x = 69, where the previous 48-pixel texture sat. */
    static final int PREFERRED_FIGURE_X = 50;
    static final int SCREEN_EDGE = 2;
    static final int HOTBAR_CLEARANCE = 4;
    /** Strip under the figure reserved for companion HUDs such as WOK步战核心's stamina. */
    public static final int BOTTOM_RESERVED = 25;
    public static final int COMPANION_WIDTH = 52;
    public static final int COMPANION_HEIGHT = 11;
    /** Chip padding beyond the font advance: two pixels on the left, one after the trailing space. */
    public static final int CHIP_PADDING = 3;

    public enum Tier {
        /** Every part shows "current/maximum". */
        FULL(8),
        /** Every part shows its current value only. */
        SHORT(5),
        /** Figure colours and the total only. */
        COMPACT(0);

        private final int labelGap;

        Tier(int labelGap) {
            this.labelGap = labelGap;
        }

        public int labelGap() {
            return labelGap;
        }
    }

    public record Layout(Tier tier, int figureX, int figureY, int rightLimit) {
        public int figureCenterX() {
            return figureX + FIGURE_WIDTH / 2;
        }

        public int companionLeft() {
            return figureCenterX() - COMPANION_WIDTH / 2;
        }

        public int companionTop() {
            return figureY + FIGURE_HEIGHT + BOTTOM_RESERVED - COMPANION_HEIGHT - 2;
        }

        /** Left edge of a chip centred under the figure, or -1 when it cannot fit left of the hotbar. */
        public int centredChipLeft(int chipWidth) {
            int maximumLeft = rightLimit - chipWidth;
            if (maximumLeft < SCREEN_EDGE) {
                return -1;
            }
            return Math.max(SCREEN_EDGE, Math.min(figureCenterX() - chipWidth / 2, maximumLeft));
        }
    }

    public static int hotbarLeft(int screenWidth) {
        return screenWidth / 2 - 91;
    }

    /**
     * Picks the richest tier whose labels fit. Label widths are the font widths
     * of the widest "maximum/maximum" and "maximum" texts, so the tier only
     * changes with the screen size or the configured maxima, never mid-fight.
     */
    public static Layout compute(int screenWidth, int screenHeight,
                                 int fullLabelWidth, int shortLabelWidth) {
        int rightLimit = hotbarLeft(screenWidth) - HOTBAR_CLEARANCE;
        int figureY = screenHeight - FIGURE_HEIGHT - BOTTOM_RESERVED;
        Layout full = labelled(Tier.FULL, fullLabelWidth, rightLimit, figureY);
        if (full != null) {
            return full;
        }
        Layout brief = labelled(Tier.SHORT, shortLabelWidth, rightLimit, figureY);
        if (brief != null) {
            return brief;
        }
        // Without side labels the companion strip is the widest element of the column.
        int overhang = (COMPANION_WIDTH - FIGURE_WIDTH) / 2;
        int figureX = Math.max(SCREEN_EDGE + overhang,
                Math.min(PREFERRED_FIGURE_X, rightLimit - FIGURE_WIDTH - overhang));
        return new Layout(Tier.COMPACT, figureX, figureY, rightLimit);
    }

    private static Layout labelled(Tier tier, int labelWidth, int rightLimit, int figureY) {
        int extent = tier.labelGap() + labelWidth + CHIP_PADDING;
        int figureX = Math.min(PREFERRED_FIGURE_X, rightLimit - FIGURE_WIDTH - extent);
        return figureX - extent >= SCREEN_EDGE
                ? new Layout(tier, figureX, figureY, rightLimit) : null;
    }

    private BodyHealthHudLayout() {
    }
}
