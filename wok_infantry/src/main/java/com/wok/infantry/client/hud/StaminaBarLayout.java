package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.TacticalIcon;
import com.wok.infantry.client.screen.UiRect;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry of the stamina bar (preview {@code surfaces/16-stamina.js}, variant
 * {@code stamina-a4} "战术平板凹槽", position A): a thin translucent plate in the vanilla
 * experience-bar row right above the hotbar, two 3px grooves side by side (arms 手 filled from the
 * left end, legs 腿 from the right end) and an "ear" at each outer end with the hand / boot
 * silhouette and, from 640 layout pixels wide, the percentage. No Minecraft class is touched, so
 * every tier is unit-tested.
 *
 * <p>Everything is in GUI pixels, the space the vanilla hotbar is drawn in, because the grooves
 * must sit exactly in the experience row: [h − 30, h − 23), under the vanilla status rows (they end
 * at h − 30) and above the hotbar's selection frame (h − 23). The ears are drawn at
 * {@link Layout#earScale()}: 1, or 2 at GUI scale 1 with the 2× core HUD
 * ({@code UiScale.hudFactor()}), so their silhouettes stay readable while the grooves keep the
 * vanilla row at 1×. Rows of one plate, top to bottom: h − 30 outline, h − 29 tick row, h − 28 …
 * h − 26 groove (top row an inner shadow), h − 25 tick row, h − 24 outline; ears [h − 36, h − 23)
 * (2×: [h − 49, h − 23)).
 *
 * <p>Narrow screens: the right end of the row is cut 2px before TaCZ's ammo readout keep-out
 * ([w − 117, w − 5) × [h − 48, h − 22)), and once the right ear would reach into it (below about
 * 445 GUI pixels) the boot moves into a 12×10 tab in the 20px gap the vanilla status rows always
 * leave at the centre, the left ear shrinks to 10×11 (between the body-health figure and the
 * hotbar column) and the legs' accent becomes the groove plate's right 2px. This depends on the
 * screen width only, so the bar never jumps when the player switches weapons.
 *
 * <p>GUI scale 1 with the 2× HUD: the 2× ears reach above the vanilla chat's last line (h − 40);
 * where the left one would enter the chat's columns (GUI 1 windows narrower than about 906 pixels
 * at the default chat width) the ears stay 1×.
 */
public final class StaminaBarLayout {
    /** Top of the groove row, pixels above the bottom (the vanilla experience row). */
    public static final int BAND_TOP = 30;
    /** Bottom of the groove row: the hotbar's selection frame starts here. */
    public static final int BAND_BOTTOM = 23;
    /** Ear height in ear units. */
    public static final int EAR_HEIGHT = 13;
    /** Status accent on an ear's outer side, ear units. */
    public static final int ACCENT = 2;
    /** Silhouette inset from an ear's outer side (accent + 2) and from its top, ear units. */
    public static final int ICON_INSET = 4;
    public static final int ICON_TOP = 2;
    /** Percentage row, ear units from the ear top, and its gap to the icon and the inner side. */
    public static final int NUMBER_TOP = 3;
    public static final int NUMBER_GAP = 3;
    public static final int NUMBER_PAD = 2;
    /** Text row height of the percentage, ear units. */
    public static final int NUMBER_HEIGHT = 8;
    /** Ear without the percentage: accent 2, 2, silhouette 9, 2. */
    public static final int EAR_WIDTH = ICON_INSET + TacticalIcon.SIZE + 2;
    /** The percentages are shown from this layout width (GUI width / core HUD factor) on. */
    public static final int NUMBERS_MIN_WIDTH = 640;
    /** Widest percentage; its font width sizes the ears so they never change width. */
    public static final String NUMBER_SAMPLE = "100%";
    /** Width of {@link #NUMBER_SAMPLE} in the vanilla font. */
    public static final int DEFAULT_NUMBER_WIDTH = 24;
    /** Narrow form: left ear [L − 10, L) × [h − 36, h − 25). */
    public static final int NARROW_EAR_WIDTH = 10;
    public static final int NARROW_EAR_TOP = 36;
    public static final int NARROW_EAR_BOTTOM = 25;
    /** Narrow form: the boot's tab [cx − 6, cx + 6) × [h − 40, h − 30). */
    public static final int TAB_HALF_WIDTH = 6;
    public static final int TAB_TOP = 40;
    /** TaCZ's ammo readout keep-out starts this far left of the screen's right edge. */
    public static final int TACZ_READOUT = 117;
    /** Pixels the row keeps clear of that keep-out. */
    public static final int TACZ_CLEARANCE = 2;
    /** Background right edge of the vanilla chat at its default width (320) and scale (1). */
    public static final int DEFAULT_CHAT_RIGHT = 332;
    /** The tick on the arms groove: sway starts below 50 % (StaminaRules.SWAY_START_STAMINA). */
    public static final float ARMS_TICK = 0.5F;
    /** The tick on the legs groove: sprint unlocks at the default 15 % (and a mantle costs 15). */
    public static final float LEGS_TICK = 0.15F;

    private StaminaBarLayout() {
    }

    /**
     * Geometry of one screen, GUI pixels ({@code null} = part not drawn on this screen).
     *
     * @param narrow      the right ear gives way to TaCZ: boot in the centre tab
     * @param numbers     the ears hold the percentages
     * @param earScale    GUI pixels per ear unit (silhouettes, percentages, accents)
     * @param earWidth    ear width in ear units
     * @param band        groove plate [L, R) × [h − 30, h − 23)
     * @param earLeft     arms ear (left of the hotbar column)
     * @param earRight    legs ear (right of it), or null on narrow screens
     * @param tab         narrow screens: the boot's tab over the legs groove, else null
     * @param arms        arms groove interior, filled from its left end
     * @param legs        legs groove interior, filled from its right end
     * @param separator   2px divider between the grooves
     * @param armsAccent  status accent of the arms
     * @param legsAccent  status accent of the legs
     * @param armsIcon    hand (or lock / check) square, 9 ear units
     * @param legsIcon    boot (or lock / check / jump arrow) square
     * @param armsNumber  arms percentage slot (text right-aligned to its right edge), or null
     * @param legsNumber  legs percentage slot (text left-aligned), or null
     * @param outline     1px outline segments around the union of the pieces
     */
    public record Layout(int guiWidth, int guiHeight, boolean narrow, boolean numbers,
                         int earScale, int earWidth,
                         UiRect band, UiRect earLeft, UiRect earRight, UiRect tab,
                         UiRect arms, UiRect legs, UiRect separator,
                         UiRect armsAccent, UiRect legsAccent,
                         UiRect armsIcon, UiRect legsIcon,
                         UiRect armsNumber, UiRect legsNumber,
                         List<UiRect> outline) {
        public Layout {
            outline = List.copyOf(outline);
        }

        /** Plate pieces (they only touch): band, left ear, then the right ear or the tab. */
        public List<UiRect> pieces() {
            List<UiRect> pieces = new ArrayList<>(3);
            pieces.add(band);
            pieces.add(earLeft);
            if (earRight != null) {
                pieces.add(earRight);
            }
            if (tab != null) {
                pieces.add(tab);
            }
            return pieces;
        }

        /** Smallest rectangle around every piece. */
        public UiRect bounds() {
            int left = Integer.MAX_VALUE;
            int top = Integer.MAX_VALUE;
            int right = Integer.MIN_VALUE;
            int bottom = Integer.MIN_VALUE;
            for (UiRect piece : pieces()) {
                left = Math.min(left, piece.left());
                top = Math.min(top, piece.top());
                right = Math.max(right, piece.right());
                bottom = Math.max(bottom, piece.bottom());
            }
            return UiRect.of(left, top, right, bottom);
        }
    }

    /** Ear width (ear units) holding a percentage {@code numberWidth} pixels wide. */
    public static int earWidth(int numberWidth) {
        return ICON_INSET + TacticalIcon.SIZE + NUMBER_GAP + Math.max(0, numberWidth) + NUMBER_PAD;
    }

    /**
     * Background right edge of the vanilla chat (ChatComponent: the lines are drawn at
     * {@code scale} after a 4px indent, the background runs to the line width + 8), GUI pixels.
     */
    public static int chatRight(int chatWidth, double chatScale) {
        double scale = chatScale > 0.0D && Double.isFinite(chatScale) ? chatScale : 1.0D;
        int lineWidth = (int) Math.ceil(Math.max(0, chatWidth) / scale);
        return (int) Math.ceil((lineWidth + 12) * scale);
    }

    /**
     * @param guiWidth    GUI-scaled screen width
     * @param guiHeight   GUI-scaled screen height
     * @param factor      core HUD factor (2 at GUI scale 1 with the minimum 2×, otherwise 1)
     * @param numberWidth font width of {@link #NUMBER_SAMPLE}
     * @param chatRight   background right edge of the vanilla chat ({@link #chatRight})
     */
    public static Layout compute(int guiWidth, int guiHeight, int factor, int numberWidth,
                                 int chatRight) {
        int width = Math.max(1, guiWidth);
        int height = Math.max(1, guiHeight);
        int f = Math.max(1, factor);
        int cx = width / 2;
        int left = cx - WokHudLayout.HOTBAR_HALF_WIDTH;
        int tacz = width - TACZ_READOUT;
        int right = Math.min(cx + WokHudLayout.HOTBAR_HALF_WIDTH, tacz - TACZ_CLEARANCE);
        int mid = right < cx + WokHudLayout.HOTBAR_HALF_WIDTH ? Math.floorDiv(left + right, 2) : cx;

        int scale = f > 1 ? 2 : 1;
        boolean numbers = width / f >= NUMBERS_MIN_WIDTH;
        int earWidth = numbers ? earWidth(numberWidth) : EAR_WIDTH;
        if (scale > 1) {
            // 2× ears reach above the chat's last line (h − 40): keep out of its columns.
            if (numbers && left - earWidth * scale < chatRight) {
                numbers = false;
                earWidth = EAR_WIDTH;
            }
            if (left - earWidth * scale < chatRight) {
                scale = 1;
            }
        }
        if (numbers && right + earWidth * scale > tacz) {
            numbers = false;
            earWidth = EAR_WIDTH;
        }
        boolean narrow = right + earWidth * scale > tacz;
        if (narrow) {
            scale = 1;
            numbers = false;
            earWidth = EAR_WIDTH;
        }

        int bandTop = height - BAND_TOP;
        int bandBottom = height - BAND_BOTTOM;
        int grooveTop = bandTop + 2;
        int grooveBottom = bandBottom - 2;
        UiRect band = UiRect.of(left, bandTop, right, bandBottom);
        UiRect separator = UiRect.of(mid - 1, bandTop + 1, mid + 1, bandBottom - 1);
        UiRect arms = UiRect.of(left + 1, grooveTop, mid - 2, grooveBottom);
        int icon = TacticalIcon.SIZE * scale;
        UiRect earLeft;
        UiRect earRight = null;
        UiRect tab = null;
        UiRect legs;
        UiRect armsAccent;
        UiRect legsAccent;
        UiRect armsIcon;
        UiRect legsIcon;
        UiRect armsNumber = null;
        UiRect legsNumber = null;
        if (narrow) {
            earLeft = UiRect.of(left - NARROW_EAR_WIDTH, height - NARROW_EAR_TOP, left,
                    height - NARROW_EAR_BOTTOM);
            tab = UiRect.of(cx - TAB_HALF_WIDTH, height - TAB_TOP, cx + TAB_HALF_WIDTH, bandTop);
            // the legs' accent takes the plate's last 2px, 1px of plate before it
            legs = UiRect.of(mid + 2, grooveTop, right - 3, grooveBottom);
            armsAccent = UiRect.of(earLeft.left(), earLeft.top(), earLeft.left() + 1,
                    earLeft.bottom());
            legsAccent = UiRect.of(right - ACCENT, bandTop, right, bandBottom);
            armsIcon = UiRect.ofSize(earLeft.left() + 1, earLeft.top() + 1, icon, icon);
            legsIcon = UiRect.ofSize(tab.left() + 2, tab.top() + 1, icon, icon);
        } else {
            int earTop = bandBottom - EAR_HEIGHT * scale;
            earLeft = UiRect.of(left - earWidth * scale, earTop, left, bandBottom);
            earRight = UiRect.of(right, earTop, right + earWidth * scale, bandBottom);
            legs = UiRect.of(mid + 2, grooveTop, right - 1, grooveBottom);
            armsAccent = UiRect.of(earLeft.left(), earTop, earLeft.left() + ACCENT * scale,
                    bandBottom);
            legsAccent = UiRect.of(earRight.right() - ACCENT * scale, earTop, earRight.right(),
                    bandBottom);
            int iconTop = earTop + ICON_TOP * scale;
            armsIcon = UiRect.ofSize(earLeft.left() + ICON_INSET * scale, iconTop, icon, icon);
            legsIcon = UiRect.ofSize(earRight.right() - ICON_INSET * scale - icon, iconTop, icon,
                    icon);
            if (numbers) {
                int numberTop = earTop + NUMBER_TOP * scale;
                int numberBottom = numberTop + NUMBER_HEIGHT * scale;
                int iconSide = (ICON_INSET + TacticalIcon.SIZE + NUMBER_GAP) * scale;
                armsNumber = UiRect.of(earLeft.left() + iconSide, numberTop,
                        earLeft.right() - NUMBER_PAD * scale, numberBottom);
                legsNumber = UiRect.of(earRight.left() + NUMBER_PAD * scale, numberTop,
                        earRight.right() - iconSide, numberBottom);
            }
        }
        List<UiRect> pieces = new ArrayList<>(3);
        pieces.add(band);
        pieces.add(earLeft);
        if (earRight != null) {
            pieces.add(earRight);
        }
        if (tab != null) {
            pieces.add(tab);
        }
        return new Layout(width, height, narrow, numbers, scale, earWidth, band, earLeft,
                earRight, tab, arms, legs, separator, armsAccent, legsAccent, armsIcon, legsIcon,
                armsNumber, legsNumber, outline(pieces));
    }

    /**
     * The 1px outline of the union of {@code pieces} (rectangles that may touch but never
     * overlap): every pixel of the union with a 4-neighbour outside it, as horizontal and
     * vertical segments that never cover a pixel twice (the outline colour is translucent).
     */
    public static List<UiRect> outline(List<UiRect> pieces) {
        List<UiRect> segments = new ArrayList<>();
        for (UiRect piece : pieces) {
            if (piece == null || piece.isEmpty()) {
                continue;
            }
            List<int[]> top = uncoveredRow(piece, pieces, piece.top() - 1);
            List<int[]> bottom = uncoveredRow(piece, pieces, piece.bottom());
            for (int[] span : top) {
                segments.add(UiRect.of(span[0], piece.top(), span[1], piece.top() + 1));
            }
            if (piece.height() > 1) {
                for (int[] span : bottom) {
                    segments.add(UiRect.of(span[0], piece.bottom() - 1, span[1],
                            piece.bottom()));
                }
            }
            for (int column : piece.width() > 1
                    ? new int[]{piece.left(), piece.right() - 1} : new int[]{piece.left()}) {
                int neighbour = column == piece.left() ? piece.left() - 1 : piece.right();
                List<int[]> cuts = new ArrayList<>();
                for (UiRect other : pieces) {
                    if (other != piece && other != null && other.left() <= neighbour
                            && neighbour < other.right()) {
                        cuts.add(new int[]{other.top(), other.bottom()});
                    }
                }
                // corner pixels already drawn by this piece's own top or bottom segments
                if (covers(top, column)) {
                    cuts.add(new int[]{piece.top(), piece.top() + 1});
                }
                if (piece.height() > 1 && covers(bottom, column)) {
                    cuts.add(new int[]{piece.bottom() - 1, piece.bottom()});
                }
                for (int[] span : subtract(piece.top(), piece.bottom(), cuts)) {
                    segments.add(UiRect.of(column, span[0], column + 1, span[1]));
                }
            }
        }
        return segments;
    }

    /** Spans of {@code piece}'s columns whose pixel in row {@code row} is outside every other piece. */
    private static List<int[]> uncoveredRow(UiRect piece, List<UiRect> pieces, int row) {
        List<int[]> cuts = new ArrayList<>();
        for (UiRect other : pieces) {
            if (other != piece && other != null && other.top() <= row && row < other.bottom()) {
                cuts.add(new int[]{other.left(), other.right()});
            }
        }
        return subtract(piece.left(), piece.right(), cuts);
    }

    private static boolean covers(List<int[]> spans, int value) {
        for (int[] span : spans) {
            if (span[0] <= value && value < span[1]) {
                return true;
            }
        }
        return false;
    }

    /** [from, to) minus the union of {@code cuts}, as ordered disjoint spans. */
    static List<int[]> subtract(int from, int to, List<int[]> cuts) {
        List<int[]> spans = new ArrayList<>();
        int cursor = from;
        while (cursor < to) {
            int next = to;
            boolean cut = false;
            for (int[] c : cuts) {
                if (c[0] <= cursor && cursor < c[1]) {
                    cursor = c[1];
                    cut = true;
                    break;
                }
                if (c[0] > cursor) {
                    next = Math.min(next, c[0]);
                }
            }
            if (cut) {
                continue;
            }
            spans.add(new int[]{cursor, next});
            cursor = next;
        }
        return spans;
    }
}
