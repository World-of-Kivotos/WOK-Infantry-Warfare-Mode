package com.wok.infantry.client.tablet;

import com.wok.infantry.client.screen.UiRect;

import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_CLOSE_GLASS_ALPHA;
import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_EASE_LIFT;
import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_EASE_ZOOM;
import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_K0;
import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_LIFT_END;
import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_SCAN_FROM;
import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_SCAN_SPAN;
import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_SLEEP_FROM;
import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_SLEEP_SPAN;
import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_START_FRAC;
import static com.wok.infantry.client.tablet.TabletAnimationModel.BM_ZOOM_FROM;
import static com.wok.infantry.client.tablet.TabletAnimationModel.B_BACKDROP_TO;
import static com.wok.infantry.client.tablet.TabletAnimationModel.B_CLOSE_GLASS_ALPHA;
import static com.wok.infantry.client.tablet.TabletAnimationModel.B_EASE_LIFT;
import static com.wok.infantry.client.tablet.TabletAnimationModel.B_LIFT_END;
import static com.wok.infantry.client.tablet.TabletAnimationModel.B_SCAN_ALPHA;
import static com.wok.infantry.client.tablet.TabletAnimationModel.B_SCAN_FROM;
import static com.wok.infantry.client.tablet.TabletAnimationModel.B_SCAN_TO;
import static com.wok.infantry.client.tablet.TabletAnimationModel.QUICK_EASE;
import static com.wok.infantry.client.tablet.TabletAnimationModel.QUICK_FROM;
import static com.wok.infantry.client.tablet.TabletEasing.clamp01;

/**
 * Scheme B ("the screen lifts the tablet") and the quick setting, in whole logical pixels (DESIGN
 * §4, preview {@code page/path2d.js}). The page itself never scales: it only moves by whole
 * pixels. Every output is an integer lp; multiply by S·f for physical pixels.
 *
 * <ul>
 *   <li>Terminal ({@link #termFrame}): the whole D2 device (device + page) slides up from below
 *   the bottom edge to its D region; the glass is dark first, then lights up by a scan line.
 *   Closing: the glass turns clear (GLASS α 0.20) and the device falls.</li>
 *   <li>Map ({@link #mapFrame}): the old inner screen 0.8 → 1, content moved and clipped only;
 *   the frame is a D2 device whose lit display P is the inner screen.</li>
 * </ul>
 */
public final class TabletPath2D {
    /** An integer lp box {@code x, y, w, h} (the preview's {@code R(x, y, w, h)}). */
    public record Box(int x, int y, int w, int h) {
        public static Box of(UiRect rect) {
            return new Box(rect.left(), rect.top(), rect.right() - rect.left(),
                    rect.bottom() - rect.top());
        }

        public Box shift(int dx, int dy) {
            return new Box(x + dx, y + dy, w, h);
        }

        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    /** Glass of the terminal device. */
    public enum TermGlass {
        /** Opaque sleeping GLASS. */
        DARK,
        /** Lit above {@code scanY}, sleeping below. */
        SCAN,
        /** Fully lit (the page shows). */
        LIT,
        /** Closing: clear GLASS α 0.20. */
        CLEAR
    }

    /** Glass of the map frame. */
    public enum MapGlass { OPAQUE, CLEAR }

    /** Inner-screen phase of the map. */
    public enum MapPhase { LIFT, HOLD, ZOOM, DONE }

    /**
     * One terminal frame (lp).
     *
     * @param dy            the device's downward offset (0 = resting in its D region)
     * @param dy0           start offset: the device top (bumpers included) right at the bottom edge
     * @param backdrop      alpha factor of the D2 backdrop
     * @param scanY         scan line y (shifted), or {@link Integer#MIN_VALUE} when there is none
     * @param identity      p = 1 opening: identical to the static D2 picture
     * @param offsetX       content shift x (clicks are remapped by it)
     * @param offsetY       content shift y
     * @param hideHud       HUD hidden by the white list
     */
    public record TermFrame(double p, boolean open, boolean quick, int w, int h, int unit,
                            TabletD2Geometry geom, int dy, int dy0, Box d, Box s, Box display,
                            Box e, double backdrop, TermGlass glass, int scanY, double scanAlpha,
                            double closeGlassAlpha, boolean content, boolean identity,
                            int offsetX, int offsetY, boolean visible, boolean hideCrosshair,
                            boolean hideHud, String phaseName) {
        public boolean hasScan() {
            return scanY != Integer.MIN_VALUE;
        }
    }

    /**
     * One map frame (map lp).
     *
     * @param cs        device pixel size in map lp (2 for the 960×720 map, whose terminal is 2x)
     * @param rest      resting inner screen (round(0.8·W) × round(0.8·H), centred)
     * @param inner     inner screen = the frame's lit display P = the clip window
     * @param sleepAlpha alpha of the sleeping GLASS layer over the inner screen
     * @param artW      inner width in terminal lp (for drawing the D2 frame around it)
     */
    public record MapFrame(double p, boolean open, boolean quick, int w, int h, int unit, int cs,
                           TabletD2Geometry geom, Box rest, Box inner, Box d, Box s, Box e, int dy,
                           double k, MapPhase phase, int offsetX, int offsetY, double backdrop,
                           double sleepAlpha, MapGlass glass, boolean content, int scanY,
                           boolean identity, double artW, double artH, boolean visible,
                           boolean hideCrosshair, boolean hideHud, String phaseName) {
        public boolean hasScan() {
            return scanY != Integer.MIN_VALUE;
        }

        /** The lit display P (= the inner screen). */
        public Box display() {
            return inner;
        }
    }

    /** Frame thickness of the map's D2 device (map lp). */
    public record Bezel(int side, int top, int bot, int bump, int glass) {
    }

    private TabletPath2D() {
    }

    private static int round(double value) {
        return (int) Math.round(value);
    }

    // =========================================================================================
    // Terminal
    // =========================================================================================

    /** Lift start: the device top (with its bumpers) right at the bottom edge, {@code H − E.t}. */
    public static int termDy0(TabletD2Geometry g) {
        return g.h() - g.e().top();
    }

    /** A terminal frame for the layout of {@code term} (preview {@code termFrame}). */
    public static TermFrame termFrame(double progress, TabletUnits term, boolean open, boolean quick) {
        TabletD2Geometry g = TabletD2Geometry.of(term.lpW(), term.lpH());
        double p = clamp01(progress);
        double s = quick ? QUICK_EASE.apply((p - QUICK_FROM) / (1.0D - QUICK_FROM))
                : B_EASE_LIFT.apply(p / B_LIFT_END);
        int dy0 = termDy0(g);
        int dy = p >= 1.0D ? 0 : round(dy0 * (1.0D - s));
        double backdrop = quick ? clamp01((p - QUICK_FROM) / (1.0D - QUICK_FROM))
                : clamp01(p / B_BACKDROP_TO);
        boolean scanning = open && !quick && p >= B_SCAN_FROM && p < 1.0D;
        int scanY = scanning ? g.s().top() + dy
                + round(clamp01((p - B_SCAN_FROM) / (B_SCAN_TO - B_SCAN_FROM)) * g.s().height())
                : Integer.MIN_VALUE;
        TermGlass glass = !open ? TermGlass.CLEAR : quick || p >= 1.0D ? TermGlass.LIT
                : scanning ? TermGlass.SCAN : TermGlass.DARK;
        Box d = Box.of(g.d()).shift(0, dy);
        Box sBox = Box.of(g.s()).shift(0, dy);
        Box pBox = Box.of(g.p()).shift(0, dy);
        Box e = Box.of(g.e()).shift(0, dy);
        String phase = TabletMotion.phaseName(quick ? TabletPath.QUICK : TabletPath.B2D,
                open ? 1 : -1, p, null, TabletScreenKind.SQUAD);
        return new TermFrame(p, open, quick, g.w(), g.h(), term.sf(), g, dy, dy0, d, sBox, pBox, e,
                backdrop, glass, scanY, B_SCAN_ALPHA, B_CLOSE_GLASS_ALPHA, open,
                open && p >= 1.0D, 0, dy, p > 0.0D && e.y() < g.h(), open, open, phase);
    }

    // =========================================================================================
    // Map
    // =========================================================================================

    /** Resting inner screen: {@code cs·round(0.8·W/cs)} × …, centred. */
    public static Box rest(int w, int h, int cs) {
        int c = Math.max(1, cs);
        int rw = c * round(BM_K0 * w / c);
        int rh = c * round(BM_K0 * h / c);
        return new Box(round((w - rw) / 2.0D), round((h - rh) / 2.0D), rw, rh);
    }

    /** Lift start: inner top = round(0.78·H) + top (the device top is then at 0.78H). */
    public static int startTop(int h, int top) {
        return round(BM_START_FRAC * h) + top;
    }

    /** Inner screen, content shift and zoom factor of the map at {@code p}. */
    public record Inner(Box inner, int dy, double k, MapPhase phase, Box rest) {
    }

    public static Inner innerAt(double p, int w, int h, int top, int cs) {
        int c = Math.max(1, cs);
        Box r = rest(w, h, c);
        if (p <= BM_LIFT_END) {
            double s = BM_EASE_LIFT.apply(p / BM_LIFT_END);
            int st = startTop(h, top);
            int t = round(st + (r.y() - st) * s);
            return new Inner(new Box(r.x(), t, r.w(), r.h()), t - r.y(), BM_K0, MapPhase.LIFT, r);
        }
        if (p < BM_ZOOM_FROM) {
            return new Inner(r, 0, BM_K0, MapPhase.HOLD, r);
        }
        double s = BM_EASE_ZOOM.apply((p - BM_ZOOM_FROM) / (1.0D - BM_ZOOM_FROM));
        double k = BM_K0 + (1.0D - BM_K0) * s;
        int iw = p >= 1.0D ? w : c * round(k * w / c);
        int ih = p >= 1.0D ? h : c * round(k * h / c);
        Box inner = new Box(round((w - iw) / 2.0D), round((h - ih) / 2.0D), iw, ih);
        return new Inner(inner, 0, p >= 1.0D ? 1.0D : k, p >= 1.0D ? MapPhase.DONE : MapPhase.ZOOM, r);
    }

    /** Frame thickness: sides (side + glass)·cs, top (top + glass)·cs, bottom (bot + glass)·cs. */
    public static Bezel mapBezel(TabletD2Geometry g, int cs) {
        var k = g.k();
        return new Bezel((k.side() + k.glassInset()) * cs, (k.top() + k.glassInset()) * cs,
                (k.bottom() + k.glassInset()) * cs, k.bump() * cs, k.glassInset() * cs);
    }

    /** {@code max(1, round(f_terminal / f_map))}. */
    public static int mapCs(TabletUnits map, TabletUnits term) {
        return Math.max(1, round((double) term.factor() / map.factor()));
    }

    /**
     * A map frame (preview {@code mapFrame}). The frame parts follow the terminal's layout factor
     * and size class: the 960×720 map has cs = 2 and the terminal's STANDARD class.
     */
    public static MapFrame mapFrame(double progress, TabletUnits map, TabletUnits term, boolean open,
                                    boolean quick) {
        TabletD2Geometry g = TabletD2Geometry.of(term.lpW(), term.lpH());
        int w = map.lpW();
        int h = map.lpH();
        int cs = mapCs(map, term);
        double p = clamp01(progress);
        Bezel bz = mapBezel(g, cs);
        Inner a = innerAt(p, w, h, bz.top() + bz.bump(), cs);
        Box inner = a.inner();
        Box d = new Box(inner.x() - bz.side(), inner.y() - bz.top(), inner.w() + 2 * bz.side(),
                inner.h() + bz.top() + bz.bot());
        Box e = new Box(d.x() - bz.bump(), d.y() - bz.bump(), d.w() + 2 * bz.bump(),
                d.h() + 2 * bz.bump());
        Box s = new Box(inner.x() - bz.glass(), inner.y() - bz.glass(), inner.w() + 2 * bz.glass(),
                inner.h() + 2 * bz.glass());
        boolean scanOn = open && p > BM_SCAN_FROM && p < BM_SCAN_FROM + BM_SCAN_SPAN;
        double backdrop = quick ? BM_EASE_ZOOM.apply((p - BM_ZOOM_FROM) / (1.0D - BM_ZOOM_FROM))
                : clamp01(p / BM_LIFT_END);
        double sleepAlpha = open ? 1.0D - clamp01((p - BM_SLEEP_FROM) / BM_SLEEP_SPAN)
                : BM_CLOSE_GLASS_ALPHA;
        int scanY = scanOn ? inner.y() + round(clamp01((p - BM_SCAN_FROM) / BM_SCAN_SPAN) * inner.h())
                : Integer.MIN_VALUE;
        boolean visible = p > 0.0D && e.x() < w && e.y() < h && e.x() + e.w() > 0 && e.y() + e.h() > 0;
        String phase = TabletMotion.phaseName(quick ? TabletPath.QUICK : TabletPath.B2D,
                open ? 1 : -1, p, null, TabletScreenKind.FULLSCREEN);
        return new MapFrame(p, open, quick, w, h, map.sf(), cs, g, a.rest(), inner, d, s, e, a.dy(),
                a.k(), a.phase(), inner.x(), inner.y(), backdrop, sleepAlpha,
                open ? MapGlass.OPAQUE : MapGlass.CLEAR, open, scanY, open && p >= 1.0D,
                inner.w() / (double) cs, inner.h() / (double) cs, visible, open, open, phase);
    }

    // =========================================================================================
    // Visibility of a click (§2.5, review F2)
    // =========================================================================================

    /**
     * Whether the lp point is visible on this terminal frame: outside the glass opening S (bezel
     * keys, case, world) always; inside, LIT everywhere, SCAN above the scan line, DARK nowhere.
     */
    public static boolean termVisible(TermFrame f, double lx, double ly) {
        Box s = f.s();
        boolean inS = s.contains(lx, ly);
        if (!inS || f.glass() == TermGlass.LIT) {
            return true;
        }
        if (f.glass() == TermGlass.SCAN) {
            return ly < f.scanY();
        }
        return false;
    }

    /** Map: inside the inner screen hidden while the sleep layer has not faded or below the scan line. */
    public static boolean mapVisible(MapFrame f, double lx, double ly) {
        Box i = f.inner();
        if (!i.contains(lx, ly) || !f.content()) {
            return true;
        }
        return !(f.sleepAlpha() > 0.0D) && (!f.hasScan() || ly < f.scanY());
    }

    /** Lift distance in physical pixels: terminal dy0 × S·f. */
    public static int termLiftTravelPx(TabletUnits term) {
        TabletD2Geometry g = TabletD2Geometry.of(term.lpW(), term.lpH());
        return termDy0(g) * term.sf();
    }

    /** Lift distance in physical pixels: map (startTop − rest.y) × S. */
    public static int mapLiftTravelPx(TabletUnits map, TabletUnits term) {
        TabletD2Geometry g = TabletD2Geometry.of(term.lpW(), term.lpH());
        int cs = mapCs(map, term);
        Bezel bz = mapBezel(g, cs);
        return (startTop(map.lpH(), bz.top() + bz.bump()) - rest(map.lpW(), map.lpH(), cs).y())
                * map.sf();
    }
}
