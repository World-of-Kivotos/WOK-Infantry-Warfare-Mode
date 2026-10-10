package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalShellLayout.Density;
import com.wok.infantry.client.screen.TacticalShellLayout.DeviceMetrics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The D2 tablet device drawn procedurally: a pure port of the preview's {@code 17-device.js}
 * ({@code rrect}, {@code inRR}, {@code shape}, {@code screw}, {@code led}, {@code drawCase},
 * {@code drawBumpers}, {@code drawSideKeys}, {@code drawTopBezel}, {@code drawGlass},
 * {@code drawBottomScrews}, {@code glassOverlay}, {@code backdrop}) into uniform rectangles.
 *
 * <p>The static case of one logical size, size class and livery is built once into a
 * {@link Plate} of {@link Runs} (painter-order rectangles, rows of one primitive merged) and
 * cached; every frame only replays it. What changes per frame or per palette is drawn on top:
 * the link LED (its colour follows the link state), the lit display (the live {@code FRAME_MID}),
 * the "WOK-T7" silkscreen (game font) and the matte specks, which the preview draws pixel by
 * pixel and the port tiles from a baked 16×16 texture between the {@link Plate#under} and
 * {@link Plate#over} runs (none on the compact class). The vignette of the backdrop is a baked
 * 64×64 texture, stretched with linear filtering.
 *
 * <p>All coordinates are whole logical pixels: under the 2x pose of {@link UiScale} every logical
 * pixel is exactly 2×2 physical pixels, as the preview draws GUI 1 at 2x. {@link #rasterize}
 * composites the cached runs into an ARGB image; {@code DeviceArtGoldenTest} compares it pixel
 * for pixel with goldens exported from the preview ({@code ui-preview/tools/export-device-golden.mjs}),
 * and a device animation can upload the same image as a texture.
 */
public final class DeviceArt {
    // ---- fixed device parts (preview DT; never part of a palette) --------------------------------

    /** Glass in the opening and the camera well. */
    public static final int GLASS = 0xFF040606;
    /** Reflection along the top edge of the glass (overlay). */
    public static final int GLASS_HI = 0x30FFFFFF;
    public static final int LENS = 0xFF24465E;
    public static final int LENS_HI = 0xFF8DB5CF;
    public static final int SCREW = 0xFF0A0D0D;
    public static final int SCREW_HEAD = 0xFF6A7472;
    public static final int SCREW_SLOT = 0xFF242A2A;
    /** Unlit LED (the page-key LEDs of the bezel that are not the current page). */
    public static final int LED_OFF = 0xFF0E1313;
    /** Drop shadow of the case on the world. */
    public static final int SHADOW = 0x78000000;
    /** World dim behind the device ({@code backdrop}). */
    public static final int WORLD_DIM = 0x880B0F11;
    /** Light and dark matte specks of the case (baked into the speck tile). */
    public static final int SPECK_HI = 0x0DFFFFFF;
    public static final int SPECK_LO = 0x16000000;
    /** Alpha of an LED's 1px halo ({@code led}). */
    public static final int LED_HALO_ALPHA = 0x48;
    /** Model name printed on the top bezel. */
    public static final String SILK_TEXT = "WOK-T7";

    /** Glass overlay: shade of the bezel lip on the display's top row and left column. */
    static final int LIP_SHADOW_TOP = 0x30000000;
    static final int LIP_SHADOW_LEFT = 0x24000000;
    /** Glass overlay: the two faint diagonal sheen bands. */
    static final int SHEEN = 0x0AFFFFFF;
    static final int SHEEN_FAINT = 0x08FFFFFF;

    /** Speck tile edge (texture {@link TacticalTextures#DEVICE_SPECKS}). */
    public static final int SPECK_TILE = 16;
    /** Vignette texture edge (texture {@link TacticalTextures#DEVICE_VIGNETTE}). */
    public static final int VIGNETTE_SIZE = 64;

    private static final int CACHE_SIZE = 6;
    private static final Map<Key, Plate> CACHE = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Plate> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private DeviceArt() {
    }

    // ---- runs ---------------------------------------------------------------------------------

    /**
     * Uniform rectangles [left, right) × [top, bottom) in ARGB, in painter order. Translucent
     * rectangles blend over whatever lies beneath them, exactly like {@code GuiGraphics.fill}.
     */
    public static final class Runs {
        private static final int STRIDE = 5;
        private int[] data = new int[STRIDE * 32];
        private int size;

        public int size() {
            return size;
        }

        public int left(int index) {
            return data[index * STRIDE];
        }

        public int top(int index) {
            return data[index * STRIDE + 1];
        }

        public int right(int index) {
            return data[index * STRIDE + 2];
        }

        public int bottom(int index) {
            return data[index * STRIDE + 3];
        }

        public int color(int index) {
            return data[index * STRIDE + 4];
        }

        /** Appends a rectangle; empty or fully transparent ones are dropped. Returns its index or -1. */
        int add(int left, int top, int right, int bottom, int color) {
            if (right <= left || bottom <= top || (color >>> 24) == 0) {
                return -1;
            }
            if ((size + 1) * STRIDE > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            int at = size * STRIDE;
            data[at] = left;
            data[at + 1] = top;
            data[at + 2] = right;
            data[at + 3] = bottom;
            data[at + 4] = color;
            return size++;
        }

        void setBottom(int index, int bottom) {
            data[index * STRIDE + 3] = bottom;
        }

        /** Composites every rectangle, clipped to the image, over {@code argb} (see {@link #over}). */
        public void composite(int[] argb, int width, int height) {
            for (int index = 0; index < size; index++) {
                int l = Math.max(0, left(index));
                int t = Math.max(0, top(index));
                int r = Math.min(width, right(index));
                int b = Math.min(height, bottom(index));
                int color = color(index);
                for (int y = t; y < b; y++) {
                    int row = y * width;
                    for (int x = l; x < r; x++) {
                        argb[row + x] = over(argb[row + x], color);
                    }
                }
            }
        }

        /** Fills every rectangle with {@code GuiGraphics.fill}, in order. */
        public void draw(GuiGraphics graphics) {
            for (int index = 0; index < size; index++) {
                int at = index * STRIDE;
                graphics.fill(data[at], data[at + 1], data[at + 2], data[at + 3], data[at + 4]);
            }
        }

        /**
         * {@link #draw(GuiGraphics)} with the drop shadow ({@link #SHADOW} rectangles) faded to
         * {@code shadowAlpha}; 1 draws exactly the same fills, 0 leaves the shadow out.
         */
        public void draw(GuiGraphics graphics, float shadowAlpha) {
            if (!(shadowAlpha < 1.0F)) {
                draw(graphics);
                return;
            }
            int shadow = fadedShadow(shadowAlpha);
            for (int index = 0; index < size; index++) {
                int at = index * STRIDE;
                int color = data[at + 4];
                if (color == SHADOW) {
                    if (shadow == 0) {
                        continue;
                    }
                    color = shadow;
                }
                graphics.fill(data[at], data[at + 1], data[at + 2], data[at + 3], color);
            }
        }

        /**
         * These rectangles with {@code hole} cut out (each split into at most four), in the same
         * painter order: drawing the result equals drawing these and clearing the hole back to
         * transparency, as the preview's clear glass does ({@code d2DeviceArt glass: 'clear'}).
         */
        public Runs minus(UiRect hole) {
            Runs out = new Runs();
            for (int index = 0; index < size; index++) {
                int l = left(index);
                int t = top(index);
                int r = right(index);
                int b = bottom(index);
                int c = color(index);
                if (hole == null || hole.isEmpty() || r <= hole.left() || l >= hole.right()
                        || b <= hole.top() || t >= hole.bottom()) {
                    out.add(l, t, r, b, c);
                    continue;
                }
                out.add(l, t, r, Math.max(t, hole.top()), c);
                int midTop = Math.max(t, hole.top());
                int midBottom = Math.min(b, hole.bottom());
                out.add(l, midTop, Math.max(l, hole.left()), midBottom, c);
                out.add(Math.min(r, hole.right()), midTop, r, midBottom, c);
                out.add(l, Math.min(b, hole.bottom()), r, b, c);
            }
            return out;
        }
    }

    /** {@link #SHADOW} with its alpha times {@code alpha} (0 when nothing is left of it). */
    static int fadedShadow(float alpha) {
        float a = Float.isFinite(alpha) ? Math.max(0.0F, Math.min(1.0F, alpha)) : 0.0F;
        return withAlpha(SHADOW, Math.round((SHADOW >>> 24) * a));
    }

    /**
     * Collects the rows of one primitive and merges a row run into the run above it when both
     * have the same columns and colour. Safe only inside one primitive, whose pixels never overlap.
     */
    private static final class Merger {
        private final Runs out;
        private int row = Integer.MIN_VALUE;
        private int[] previous = new int[8];
        private int previousCount;
        private int[] current = new int[8];
        private int currentCount;

        Merger(Runs out) {
            this.out = out;
        }

        void add(int left, int y, int right, int color) {
            if (right <= left || (color >>> 24) == 0) {
                return;
            }
            if (y != row) {
                if (y == row + 1) {
                    int[] swap = previous;
                    previous = current;
                    previousCount = currentCount;
                    current = swap;
                } else {
                    previousCount = 0;
                }
                currentCount = 0;
                row = y;
            }
            for (int index = 0; index < previousCount; index++) {
                int run = previous[index];
                if (run >= 0 && out.left(run) == left && out.right(run) == right
                        && out.color(run) == color && out.bottom(run) == y) {
                    out.setBottom(run, y + 1);
                    previous[index] = -1;
                    push(run);
                    return;
                }
            }
            push(out.add(left, y, right, y + 1, color));
        }

        private void push(int run) {
            if (currentCount == current.length) {
                current = Arrays.copyOf(current, current.length * 2);
            }
            current[currentCount++] = run;
        }
    }

    /** Source-over of {@code src} onto {@code dst}: the preview's {@code Gui._fillPhys} blend. */
    static int over(int dst, int src) {
        int sa = src >>> 24;
        if (sa == 0) {
            return dst;
        }
        int da = dst >>> 24;
        if (sa == 255 || da == 0) {
            return src;
        }
        double fa = sa / 255.0D;
        double ia = 1 - fa;
        int sr = (src >>> 16) & 0xFF;
        int sg = (src >>> 8) & 0xFF;
        int sb = src & 0xFF;
        int dr = (dst >>> 16) & 0xFF;
        int dg = (dst >>> 8) & 0xFF;
        int db = dst & 0xFF;
        if (da == 255) {
            return 0xFF000000 | channel(sr * fa + dr * ia) << 16 | channel(sg * fa + dg * ia) << 8
                    | channel(sb * fa + db * ia);
        }
        // Translucent over translucent (not produced by the device art; kept exact anyway).
        double dA = da / 255.0D;
        double oa = fa + dA * ia;
        return channel(oa * 255) << 24 | channel((sr * fa + dr * dA * ia) / oa) << 16
                | channel((sg * fa + dg * dA * ia) / oa) << 8 | channel((sb * fa + db * dA * ia) / oa);
    }

    /** A colour channel stored like a {@code Uint8ClampedArray} element: clamped, ties to even. */
    private static int channel(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        return (int) Math.max(0.0D, Math.min(255.0D, Math.rint(value)));
    }

    // ---- primitives (preview pixel helpers) -----------------------------------------------------

    @FunctionalInterface
    interface PixelTest {
        boolean inside(int x, int y);
    }

    @FunctionalInterface
    interface PixelColor {
        int color(int x, int y);
    }

    /** {@code g.fill}: like the preview, an inverted rectangle is drawn with its edges swapped. */
    static void fill(Runs out, int left, int top, int right, int bottom, int color) {
        out.add(Math.min(left, right), Math.min(top, bottom), Math.max(left, right),
                Math.max(top, bottom), color);
    }

    private static void pixel(Runs out, int x, int y, int color) {
        out.add(x, y, x + 1, y + 1, color);
    }

    /** Is pixel (x, y) inside {@code rect} with corner radius {@code radius}? Tested at its centre. */
    static boolean inRoundedRect(UiRect rect, int radius, int x, int y) {
        double cx = x + 0.5D;
        double cy = y + 0.5D;
        if (cx < rect.left() || cx > rect.right() || cy < rect.top() || cy > rect.bottom()) {
            return false;
        }
        double qx = cx < rect.left() + radius ? rect.left() + radius
                : cx > rect.right() - radius ? rect.right() - radius : cx;
        double qy = cy < rect.top() + radius ? rect.top() + radius
                : cy > rect.bottom() - radius ? rect.bottom() - radius : cy;
        return (cx - qx) * (cx - qx) + (cy - qy) * (cy - qy) <= radius * radius;
    }

    /** Rounded rectangle, one run per row of whole pixels (corners read as pixel-art steps). */
    static void roundedRect(Runs out, UiRect rect, int radius, int color) {
        Merger rows = new Merger(out);
        for (int y = rect.top(); y < rect.bottom(); y++) {
            int l = rect.left();
            int r = rect.right();
            while (l < r && !inRoundedRect(rect, radius, l, y)) {
                l++;
            }
            while (r > l && !inRoundedRect(rect, radius, r - 1, y)) {
                r--;
            }
            rows.add(l, y, r, color);
        }
    }

    /**
     * Every pixel of the box where {@code inside} holds: 1px outline {@code edge}, light top-left
     * rim {@code hi}, dark bottom-right rim {@code lo} (0 = none), body {@code fill}.
     */
    static void shape(Runs out, int left, int top, int right, int bottom, PixelTest inside,
                      int edge, int hi, int lo, PixelColor fill) {
        Merger rows = new Merger(out);
        for (int y = top; y < bottom; y++) {
            int start = left;
            int open = 0;
            for (int x = left; x < right; x++) {
                int color = 0;
                if (inside.inside(x, y)) {
                    if (!inside.inside(x - 1, y) || !inside.inside(x + 1, y)
                            || !inside.inside(x, y - 1) || !inside.inside(x, y + 1)) {
                        color = edge;
                    } else if (hi != 0 && (!inside.inside(x, y - 2) || !inside.inside(x - 2, y))) {
                        color = hi;
                    } else if (lo != 0 && (!inside.inside(x, y + 2) || !inside.inside(x + 2, y))) {
                        color = lo;
                    } else {
                        color = fill.color(x, y);
                    }
                }
                if (color != open) {
                    if (open != 0) {
                        rows.add(start, y, x, open);
                    }
                    open = color;
                    start = x;
                }
            }
            if (open != 0) {
                rows.add(start, y, right, open);
            }
        }
    }

    /** A 4×4 screw head with its slot. */
    static void screw(Runs out, int x, int y) {
        fill(out, x + 1, y, x + 3, y + 4, SCREW);
        fill(out, x, y + 1, x + 4, y + 3, SCREW);
        pixel(out, x + 1, y + 1, SCREW_HEAD);
        pixel(out, x + 2, y + 2, SCREW_HEAD);
        pixel(out, x + 2, y + 1, SCREW_SLOT);
        pixel(out, x + 1, y + 2, SCREW_SLOT);
    }

    /** An LED of {@code width}×{@code height} with a 1px halo. */
    static void led(Runs out, int x, int y, int width, int height, int color) {
        fill(out, x - 1, y - 1, x + width + 1, y + height + 1, withAlpha(color, LED_HALO_ALPHA));
        fill(out, x, y, x + width, y + height, color);
    }

    static int withAlpha(int color, int alpha) {
        return (alpha & 0xFF) << 24 | (color & 0xFFFFFF);
    }

    private static UiRect rect(int left, int top, int right, int bottom) {
        return new UiRect(left, top, right, bottom);
    }

    // ---- the static device ------------------------------------------------------------------------

    /**
     * The cached device of one size and livery: {@link #under} the matte specks (drop shadow and
     * case body), {@link #over} them (side keys, bumpers, top bezel without the link LED and the
     * silkscreen, glass recess and dark glass, bottom screws), the speck bands and the glass
     * overlay drawn after the page.
     */
    public record Plate(TacticalShellLayout layout, Runs under, Runs over, List<UiRect> speckBands,
                        Runs glass, Runs underClear, Runs overClear) {
        /** Rectangles replayed per frame for the static device. */
        public int fills() {
            return under.size() + over.size();
        }

        /** How many runs at the start of {@link #under} are the drop shadow ({@link #SHADOW}). */
        public int shadowRuns() {
            int count = 0;
            while (count < under.size() && under.color(count) == SHADOW) {
                count++;
            }
            return count;
        }
    }

    /**
     * A hardware key of the bottom bezel drawn as a blank cap on a dark device (0.5.0-beta.4
     * animation): {@code cap} in {@code state}'s fill colours without its label or hatch, and an
     * unlit LED at {@code led} ({@link UiRect#EMPTY} for none).
     */
    public record BlankKey(UiRect cap, BezelKey.CapState state, UiRect led) {
        public BlankKey {
            cap = cap == null ? UiRect.EMPTY : cap;
            state = state == null ? BezelKey.CapState.RAISED : state;
            led = led == null ? UiRect.EMPTY : led;
        }
    }

    private record Key(int width, int height, Density tier, TacticalLivery.Livery livery) {
    }

    /** The cached plate of a logical {@code width}×{@code height} screen in {@code tier}. */
    public static Plate plate(int width, int height, Density tier, TacticalLivery.Livery livery) {
        Density size = Objects.requireNonNull(tier, "tier");
        TacticalLivery.Livery paint = Objects.requireNonNull(livery, "livery");
        Key key = new Key(Math.max(0, width), Math.max(0, height), size, paint);
        synchronized (CACHE) {
            Plate cached = CACHE.get(key);
            if (cached == null) {
                cached = build(key.width(), key.height(), size, paint.skin());
                CACHE.put(key, cached);
            }
            return cached;
        }
    }

    /**
     * The same plate as {@link #plate} without touching its shared cache: for a device whose size
     * changes every frame (the tactical map's animated frame), which would push the terminals'
     * static devices out of the six cached plates.
     */
    public static Plate buildUncached(int width, int height, Density tier,
                                      TacticalLivery.Livery livery) {
        Density size = Objects.requireNonNull(tier, "tier");
        TacticalLivery.Livery paint = Objects.requireNonNull(livery, "livery");
        return build(Math.max(0, width), Math.max(0, height), size, paint.skin());
    }

    /**
     * The static device as an ARGB image of {@code width}×{@code height} logical pixels (row by row):
     * the {@link Plate} runs composited over transparency, the whole case including the dark glass
     * well, without the matte specks, the vignette, the link LED, the silkscreen, the lit display,
     * the status bar and the keys.
     */
    public static int[] rasterize(int width, int height, Density tier, TacticalLivery.Livery livery) {
        Plate plate = plate(width, height, tier, livery);
        int w = plate.layout().width();
        int h = plate.layout().height();
        int[] argb = new int[w * h];
        plate.under().composite(argb, w, h);
        plate.over().composite(argb, w, h);
        return argb;
    }

    static Plate build(int width, int height, Density tier, DeviceSkin skin) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height, tier);
        DeviceMetrics k = layout.deviceMetrics();
        boolean tight = tier == Density.COMPACT;
        UiRect d = layout.device();
        UiRect s = layout.glass();
        int r = k.radius();

        Runs under = new Runs();
        // drawCase: shadow, outline, lit and shaded rims, body; a groove 3px in on the larger classes
        roundedRect(under, rect(d.left() + 2, d.top() + 3, d.right() + 3, d.bottom() + 4), r, SHADOW);
        roundedRect(under, d, r, skin.line());
        roundedRect(under, rect(d.left() + 1, d.top() + 1, d.right() - 1, d.bottom() - 1), r - 1,
                skin.caseHi());
        roundedRect(under, rect(d.left() + 2, d.top() + 2, d.right() - 1, d.bottom() - 1), r - 1,
                skin.caseLo());
        roundedRect(under, rect(d.left() + 2, d.top() + 2, d.right() - 2, d.bottom() - 2), r - 2,
                skin.caseColor());
        if (!tight) {
            roundedRect(under, rect(d.left() + 3, d.top() + 3, d.right() - 3, d.bottom() - 3),
                    r - 3, skin.caseLo());
            roundedRect(under, rect(d.left() + 4, d.top() + 4, d.right() - 3, d.bottom() - 3),
                    r - 4, skin.recessHi());
            roundedRect(under, rect(d.left() + 4, d.top() + 4, d.right() - 4, d.bottom() - 4),
                    r - 4, skin.caseColor());
        }

        Runs over = new Runs();
        sideKeys(over, d, k, skin);
        bumpers(over, d, k, skin);
        topBezel(over, layout, k, skin);
        // drawGlass: shaded top / left lip, lit bottom / right lip, the dark glass
        fill(over, s.left() - 1, s.top() - 1, s.right() + 1, s.bottom() + 1, skin.caseLo());
        fill(over, s.left(), s.top(), s.right() + 1, s.bottom() + 1, skin.recessHi());
        fill(over, s.left(), s.top(), s.right(), s.bottom(), GLASS);
        if (!tight) {
            int y = s.bottom() + 1 + Math.floorDiv(d.bottom() - s.bottom() - 1, 2) - 2;
            screw(over, s.left() + 4, y);
            screw(over, s.right() - 8, y);
        }
        return new Plate(layout, under, over, speckBands(layout), glassOverlay(layout),
                under.minus(s), over.minus(s));
    }

    private static void sideKeys(Runs out, UiRect d, DeviceMetrics k, DeviceSkin skin) {
        int bw = Math.max(1, k.bump());
        int h = d.height();
        sideKey(out, d, bw, true, d.top() + (int) Math.round(h * 0.16D),
                d.top() + (int) Math.round(h * 0.24D), skin);
        sideKey(out, d, bw, true, d.top() + (int) Math.round(h * 0.26D),
                d.top() + (int) Math.round(h * 0.34D), skin);
        sideKey(out, d, bw, false, d.top() + (int) Math.round(h * 0.16D),
                d.top() + (int) Math.round(h * 0.25D), skin);
    }

    private static void sideKey(Runs out, UiRect d, int bw, boolean left, int y0, int y1,
                                DeviceSkin skin) {
        int x0 = left ? d.left() - bw : d.right();
        fill(out, x0, y0, x0 + bw, y1, skin.rubberEdge());
        if (bw > 1) {
            fill(out, left ? x0 + 1 : x0, y0 + 1, left ? x0 + bw : x0 + bw - 1, y1 - 1,
                    skin.rubber());
        }
    }

    private static void bumpers(Runs out, UiRect d, DeviceMetrics k, DeviceSkin skin) {
        int b = k.bump();
        UiRect o = rect(d.left() - b, d.top() - b, d.right() + b, d.bottom() + b);
        int radius = k.radius() + b;
        int c = k.corner() + b;
        int[] ridges = c >= 14
                ? new int[]{(int) Math.round(c * 0.5D), (int) Math.round(c * 0.68D)}
                : new int[]{(int) Math.round(c * 0.55D)};
        int[][] corners = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
        for (int[] corner : corners) {
            boolean right = corner[0] == 1;
            boolean bottom = corner[1] == 1;
            PixelTest inside = (x, y) -> {
                int u = right ? o.right() - 1 - x : x - o.left();
                int v = bottom ? o.bottom() - 1 - y : y - o.top();
                return u >= 0 && v >= 0 && u + v <= c && inRoundedRect(o, radius, x, y);
            };
            PixelColor rubber = (x, y) -> {
                int u = right ? o.right() - 1 - x : x - o.left();
                int v = bottom ? o.bottom() - 1 - y : y - o.top();
                for (int ridge : ridges) {
                    if (u + v == ridge) {
                        return skin.rubberLo();
                    }
                }
                return skin.rubber();
            };
            shape(out, right ? o.right() - c - 1 : o.left(), bottom ? o.bottom() - c - 1 : o.top(),
                    right ? o.right() : o.left() + c + 1, bottom ? o.bottom() : o.top() + c + 1,
                    inside, skin.rubberEdge(), bottom ? 0 : skin.rubberHi(),
                    bottom ? skin.rubberLo() : 0, rubber);
        }
    }

    /** drawTopBezel without the link LED and the silkscreen text (both drawn per frame). */
    private static void topBezel(Runs out, TacticalShellLayout layout, DeviceMetrics k,
                                 DeviceSkin skin) {
        UiRect d = layout.device();
        UiRect s = layout.glass();
        boolean tight = layout.density() == Density.COMPACT;
        int cy = cameraY(layout);
        int cx = Math.floorDiv(d.left() + d.right(), 2);
        if (tight) {
            fill(out, cx - 1, cy - 1, cx + 2, cy + 2, GLASS);
            pixel(out, cx, cy, LENS);
        } else {
            fill(out, cx - 2, cy - 1, cx + 3, cy + 2, GLASS);
            fill(out, cx - 1, cy - 2, cx + 2, cy + 3, GLASS);
            pixel(out, cx, cy, LENS);
            pixel(out, cx - 1, cy - 1, LENS_HI);
            fill(out, cx + 6, cy, cx + 8, cy + 1, skin.line());
        }
        UiRect power = powerLed(layout);
        led(out, power.left(), power.top(), power.width(), power.height(), skin.powerLed());
        if (k.top() >= 10) {
            screw(out, s.left() + 4, cy - 2);
            screw(out, s.right() - 8, cy - 2);
            int sx = s.left() + 14;
            fill(out, sx, cy - 1, sx + 10, cy + 1, skin.stripe());
        }
    }

    private static int cameraY(TacticalShellLayout layout) {
        return layout.device().top() + Math.floorDiv(layout.deviceMetrics().top(), 2);
    }

    /** Lit area of the power LED on the top bezel (without its halo). */
    public static UiRect powerLed(TacticalShellLayout layout) {
        boolean tight = layout.density() == Density.COMPACT;
        int lw = tight ? 2 : 3;
        int lh = tight ? 1 : 2;
        int lx = layout.glass().right() - (tight ? 9 : 24);
        int ly = cameraY(layout) - (tight ? 0 : 1);
        return UiRect.ofSize(lx, ly, lw, lh);
    }

    /** Lit area of the link LED, 3px right of the power LED (without its halo). */
    public static UiRect linkLed(TacticalShellLayout layout) {
        UiRect power = powerLed(layout);
        return UiRect.ofSize(power.right() + 3, power.top(), power.width(), power.height());
    }

    /** The link LED in {@code color} with its halo, for a frame. */
    public static Runs linkLedRuns(TacticalShellLayout layout, int color) {
        Runs runs = new Runs();
        UiRect link = linkLed(layout);
        led(runs, link.left(), link.top(), link.width(), link.height(), color);
        return runs;
    }

    /**
     * Where the "WOK-T7" silkscreen starts (text top-left), or {@code null} on a top bezel lower
     * than 10px, which has no room for it.
     */
    public static int[] silkOrigin(TacticalShellLayout layout) {
        DeviceMetrics k = layout.deviceMetrics();
        if (k.top() < 10) {
            return null;
        }
        return new int[]{layout.glass().left() + 28,
                layout.device().top() + (int) Math.ceil((k.top() - 7) / 2.0D)};
    }

    /**
     * Case face that gets the matte specks: the inner body (case inset 2) minus the glass opening
     * and its 1px recess, as four bands; none on the compact class. The bumpers drawn afterwards
     * cover the rounded corners.
     */
    public static List<UiRect> speckBands(TacticalShellLayout layout) {
        if (layout.density() == Density.COMPACT) {
            return List.of();
        }
        UiRect d = layout.device();
        UiRect s = layout.glass();
        UiRect inner = rect(d.left() + 2, d.top() + 2, d.right() - 2, d.bottom() - 2);
        int holeL = s.left() - 1;
        int holeT = s.top() - 1;
        int holeR = s.right() + 1;
        int holeB = s.bottom() + 1;
        List<UiRect> bands = new ArrayList<>(4);
        addBand(bands, inner.left(), inner.top(), inner.right(), holeT);
        addBand(bands, inner.left(), holeB, inner.right(), inner.bottom());
        addBand(bands, inner.left(), holeT, holeL, holeB);
        addBand(bands, holeR, holeT, inner.right(), holeB);
        return List.copyOf(bands);
    }

    private static void addBand(List<UiRect> bands, int left, int top, int right, int bottom) {
        if (right > left && bottom > top) {
            bands.add(rect(left, top, right, bottom));
        }
    }

    /**
     * The glass drawn over the page, its widgets and an open modal: rim highlight on the glass's
     * top row, the bezel lip's shade on the display's top row and left column, and two faint
     * diagonal sheen bands ({@code d = dx + 1.6 dy} in [a, b) and [b + 4, b + 7)).
     */
    public static Runs glassOverlay(TacticalShellLayout layout) {
        UiRect s = layout.glass();
        UiRect p = layout.display();
        Runs runs = new Runs();
        fill(runs, s.left(), s.top(), s.right(), s.top() + 1, GLASS_HI);
        fill(runs, p.left(), p.top(), p.right(), p.top() + 1, LIP_SHADOW_TOP);
        fill(runs, p.left(), p.top() + 1, p.left() + 1, p.bottom(), LIP_SHADOW_LEFT);
        int a = (int) Math.round(p.width() * 0.15D);
        int b = a + Math.max(12, (int) Math.round(p.width() * 0.08D));
        for (int y = p.top(); y < p.bottom(); y++) {
            double dy = (y - p.top()) * 1.6D;
            sheenRow(runs, p, y, dy, a, b, SHEEN);
            sheenRow(runs, p, y, dy, b + 4, b + 7, SHEEN_FAINT);
        }
        return runs;
    }

    /** One row of a sheen band: the columns dx of {@code p} with from &le; dx + dy &lt; to. */
    private static void sheenRow(Runs runs, UiRect p, int y, double dy, int from, int to,
                                 int color) {
        int lo = firstColumn(dy, from);
        int hi = firstColumn(dy, to);
        lo = Math.max(0, lo);
        hi = Math.min(p.width(), hi);
        if (hi > lo) {
            runs.add(p.left() + lo, y, p.left() + hi, y + 1, color);
        }
    }

    /** Smallest whole dx with dx + dy &ge; bound, evaluated exactly as the preview does. */
    private static int firstColumn(double dy, int bound) {
        int dx = (int) Math.ceil(bound - dy);
        while (dx - 1 + dy >= bound) {
            dx--;
        }
        while (dx + dy < bound) {
            dx++;
        }
        return dx;
    }

    // ---- baked textures (generated by export-ui-atlas.mjs; the tests check them against these) ----

    /** The preview's {@code hash2} (kit/scene.js): a deterministic value in [0, 1]. */
    static double hash2(int x, int y, int seed) {
        int h = (int) ((long) x * 374761393L + (long) y * 668265263L + (long) seed * 2147483647L);
        h = (h ^ (h >>> 13)) * 1274126177;
        return Integer.toUnsignedLong(h ^ (h >>> 16)) / 4294967295.0D;
    }

    /** Speck colour of case pixel (x, y): {@link #SPECK_HI}, {@link #SPECK_LO} or 0. */
    static int speck(int x, int y) {
        double n = hash2(x, y, 31);
        return n < 0.07D ? SPECK_HI : n > 0.93D ? SPECK_LO : 0;
    }

    /**
     * Vignette alpha (black) at the fractional screen position (fx, fy) in [0, 1]: the preview's
     * {@code backdrop} shade.
     */
    static int vignetteAlpha(double fx, double fy) {
        double dx = (fx - 0.5D) * 2;
        double dy = (fy - 0.5D) * 2;
        double v = Math.min(1.0D, (dx * dx + dy * dy) * 0.45D);
        return v > 0.05D ? (int) Math.round(v * 96) : 0;
    }

    // ---- drawing ----------------------------------------------------------------------------------

    /**
     * What shows around the device: the dimmed world and the vignette, one pixel past the right
     * and bottom edge (a 2x layout drops the odd window pixel). No opaque background.
     */
    public static void drawBackdrop(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width + 1, height + 1, WORLD_DIM);
        TacticalTextures.stretch(graphics, TacticalTextures.DEVICE_VIGNETTE, 0, 0, width + 1,
                height + 1, VIGNETTE_SIZE, VIGNETTE_SIZE);
    }

    /**
     * {@link #drawBackdrop(GuiGraphics, int, int)} faded by {@code alpha} (0.5.0-beta.4
     * animation): the dim's alpha and the vignette's are multiplied by it; 1 (or more) draws
     * exactly the plain backdrop, 0 (or less) draws nothing.
     */
    public static void drawBackdrop(GuiGraphics graphics, int width, int height, float alpha) {
        if (!(alpha < 1.0F)) {
            drawBackdrop(graphics, width, height);
            return;
        }
        if (!(alpha > 0.0F)) {
            return;
        }
        graphics.fill(0, 0, width + 1, height + 1, backdropDim(alpha));
        TacticalTextures.blitTinted(graphics, TacticalTextures.DEVICE_VIGNETTE, 0, 0, width + 1,
                height + 1, 0.0F, 0.0F, VIGNETTE_SIZE, VIGNETTE_SIZE, VIGNETTE_SIZE, VIGNETTE_SIZE,
                withAlpha(0xFFFFFFFF, Math.round(255 * alpha)));
    }

    /** {@link #WORLD_DIM} with its alpha times {@code alpha} (pure). */
    static int backdropDim(float alpha) {
        float a = Float.isFinite(alpha) ? Math.max(0.0F, Math.min(1.0F, alpha)) : 0.0F;
        return withAlpha(WORLD_DIM, Math.round((WORLD_DIM >>> 24) * a));
    }

    /**
     * Draws the device of {@code layout} in {@code livery}: cached case, specks, the link LED in
     * {@code linkColor}, the lit display in {@code displayColor} and the silkscreen.
     */
    public static void drawDevice(GuiGraphics graphics, Font font, TacticalShellLayout layout,
                                  TacticalLivery.Livery livery, int linkColor, int displayColor) {
        drawDevice(graphics, font, layout, livery, linkColor, displayColor, 1.0F);
    }

    /**
     * {@link #drawDevice(GuiGraphics, Font, TacticalShellLayout, TacticalLivery.Livery, int, int)}
     * with the case's drop shadow faded to {@code shadowAlpha} (the opening animation fades it in
     * with the screen's wake); 1 draws exactly the same fills.
     */
    public static void drawDevice(GuiGraphics graphics, Font font, TacticalShellLayout layout,
                                  TacticalLivery.Livery livery, int linkColor, int displayColor,
                                  float shadowAlpha) {
        Plate plate = plate(layout.width(), layout.height(), layout.density(), livery);
        graphics.drawManaged(() -> plate.under().draw(graphics, shadowAlpha));
        drawSpecks(graphics, plate);
        Runs link = linkLedRuns(layout, linkColor);
        UiRect display = layout.display();
        graphics.drawManaged(() -> {
            plate.over().draw(graphics);
            link.draw(graphics);
            graphics.fill(display.left(), display.top(), display.right(), display.bottom(),
                    displayColor);
        });
        drawSilk(graphics, font, layout, livery);
    }

    /**
     * Draws a sleeping device from {@code plate} (0.5.0-beta.4 animation: scheme B's frames and its
     * put-away device): case with its shadow, specks, the link LED in {@code linkColor} (0 = none),
     * {@code keys} as blank caps and the silkscreen; no lit display and no glass overlay. With
     * {@code clearGlass} the glass opening is left transparent (the caller lays the translucent
     * glass over it), otherwise it is the dark {@link #GLASS}.
     */
    public static void drawSleepingDevice(GuiGraphics graphics, Font font, Plate plate,
                                          TacticalLivery.Livery livery, int linkColor,
                                          boolean clearGlass, List<BlankKey> keys) {
        TacticalShellLayout layout = plate.layout();
        Runs under = clearGlass ? plate.underClear() : plate.under();
        Runs over = clearGlass ? plate.overClear() : plate.over();
        graphics.drawManaged(() -> under.draw(graphics));
        drawSpecks(graphics, plate);
        Runs link = linkColor == 0 ? null : linkLedRuns(layout, linkColor);
        graphics.drawManaged(() -> {
            over.draw(graphics);
            if (link != null) {
                link.draw(graphics);
            }
        });
        drawBlankKeys(graphics, keys, livery.skin());
        drawSilk(graphics, font, layout, livery);
    }

    /**
     * Draws {@code keys} as blank caps in {@code skin}: unlit LED, cap outline, face and lips as
     * {@code BezelKey} draws them, never a label and never the disabled hatch.
     */
    public static void drawBlankKeys(GuiGraphics graphics, List<BlankKey> keys, DeviceSkin skin) {
        if (keys == null || keys.isEmpty() || skin == null) {
            return;
        }
        graphics.drawManaged(() -> {
            for (BlankKey key : keys) {
                if (!key.led().isEmpty()) {
                    graphics.fill(key.led().left(), key.led().top(), key.led().right(),
                            key.led().bottom(), LED_OFF);
                }
                BezelKey.Cap cap = BezelKey.cap(skin, key.state());
                BezelKey.drawCap(graphics, key.cap(), new BezelKey.Cap(cap.edge(), cap.face(),
                        cap.topLip(), cap.bottomLip(), cap.text(), cap.sub(), false, 0));
            }
        });
    }

    private static void drawSpecks(GuiGraphics graphics, Plate plate) {
        for (UiRect band : plate.speckBands()) {
            TacticalTextures.tileAnchored(graphics, TacticalTextures.DEVICE_SPECKS, band.left(),
                    band.top(), band.right(), band.bottom(), SPECK_TILE, SPECK_TILE);
        }
    }

    private static void drawSilk(GuiGraphics graphics, Font font, TacticalShellLayout layout,
                                 TacticalLivery.Livery livery) {
        int[] silk = silkOrigin(layout);
        if (silk != null && font != null) {
            graphics.drawString(font, SILK_TEXT, silk[0], silk[1], livery.skin().silk(), false);
        }
    }

    /** Draws the glass overlay of {@code layout} (call above the page and any modal). */
    public static void drawGlass(GuiGraphics graphics, TacticalShellLayout layout,
                                 TacticalLivery.Livery livery) {
        Plate plate = plate(layout.width(), layout.height(), layout.density(), livery);
        graphics.drawManaged(() -> plate.glass().draw(graphics));
    }
}
