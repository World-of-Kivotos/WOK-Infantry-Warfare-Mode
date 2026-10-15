package com.wok.infantry.client.tablet;

import com.wok.infantry.client.tablet.TabletPose3D.RectPx;

/**
 * Where the C1 close copies its picture from and where it replays it (DESIGN 3.4 / 6.3
 * {@code TabletFrameCapture}, preview {@code compose.captureSnapshot / drawCapture}), plus the
 * smooth-downscale level rule (DESIGN 3.1). Pure logic; the GL copies live in
 * {@link TabletFrameCapture} and {@link TabletScreenTarget}.
 */
public final class TabletCapturePlan {
    private static final double EPS = 1.0E-9D;

    private TabletCapturePlan() {
    }

    /** A block of whole physical pixels of the window (top-left origin). */
    public record Region(int x, int y, int w, int h) {
        public boolean isEmpty() {
            return w <= 0 || h <= 0;
        }

        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }
    }

    /**
     * Whether a close on {@code path} from the last drawn progress {@code lastP} copies the screen
     * (C1): only scheme A with capture on, only from a frame that showed the page (p &gt; 0.61), and
     * never from the wake band (0.61 &lt; p &lt; 0.74: the bands and the mask are in that frame;
     * the motion then starts at 0.61 instead, {@code TabletMotion.close} skipWake).
     */
    public static boolean wants(TabletPath path, boolean capture, double lastP) {
        if (path != TabletPath.A3D || !capture || !(lastP > TabletAnimationModel.SEG_RAISE_TO + EPS)) {
            return false;
        }
        return !(lastP > TabletAnimationModel.CAPTURE_SKIP_WAKE_FROM + EPS
                && lastP < TabletAnimationModel.CAPTURE_SKIP_WAKE_TO - EPS);
    }

    /**
     * The block to copy for a close that starts from frame {@code from} (a closing frame at its own
     * {@code captureFromP}, so {@code from.capture()} holds the last drawn rectangles): a terminal's
     * E region inside that frame's plane (the device with its bumpers; the drop shadow is drawn
     * anew on replay), a full-screen page's map rectangle; edges rounded outwards, clipped to the
     * {@code pxW × pxH} window. Empty when nothing was on screen.
     */
    public static Region region(TabletFrame from, int pxW, int pxH) {
        if (from == null || !from.capture().draw() || from.capture().fromRectPx() == null) {
            return new Region(0, 0, 0, 0);
        }
        RectPx box;
        if (from.ctx().map()) {
            box = from.capture().fromRectPx();
        } else {
            TabletD2Geometry g = from.ctx().geom();
            box = TabletPose3D.subRectPx(from.capture().fromPlanePx(), g, g.e());
        }
        return outward(box, pxW, pxH);
    }

    /** {@code box} rounded outwards to whole pixels and clipped to the window. */
    public static Region outward(RectPx box, int pxW, int pxH) {
        int x0 = Math.max(0, (int) Math.floor(box.x() + EPS));
        int y0 = Math.max(0, (int) Math.floor(box.y() + EPS));
        int x1 = Math.min(pxW, (int) Math.ceil(box.x() + box.w() - EPS));
        int y1 = Math.min(pxH, (int) Math.ceil(box.y() + box.h() - EPS));
        return new Region(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    }

    /**
     * Where the copied {@code region} lands when the rectangle it was copied from ({@code from})
     * is now {@code now}: the same scale k = now.w / from.w about the rectangles' origins
     * ({x, y, w, h}, physical pixels). On the first frame (now = from) it is the region itself.
     */
    public static double[] replay(Region region, RectPx from, RectPx now) {
        double k = from.w() > 0.0D ? now.w() / from.w() : 1.0D;
        double ky = from.h() > 0.0D ? now.h() / from.h() : k;
        return new double[]{now.x() + (region.x() - from.x()) * k,
                now.y() + (region.y() - from.y()) * ky, region.w() * k, region.h() * ky};
    }

    /**
     * Where the device's layout picture ({@code planeW × planeH} layout pixels, whole plane)
     * lies inside the copied region, in the region's own pixels (x right, y down): the mask that
     * clears the world around the case is drawn there.
     */
    public static double[] planeInRegion(Region region, RectPx plane) {
        return new double[]{plane.x() - region.x(), plane.y() - region.y(), plane.w(), plane.h()};
    }

    /**
     * Smooth downscale (DESIGN 3.1): below a ratio of 0.5 a linear sample skips texels, so the
     * texture is mip-mapped first (the preview halves the picture step by step).
     */
    public static boolean mipmap(double scale) {
        return scale > 0.0D && scale < TabletAnimationModel.SMOOTH_HALVE_BELOW;
    }
}
