package com.wok.infantry.client.screen;

/**
 * Pure geometry of the tactical tablet: three size classes and the regions of the device, which
 * never overlap (the body's 1px outline stays inside the gaps).
 *
 * <p>Ported from the preview's {@code uiMetrics}/{@code UI.shell} ({@code kit/ui.js}):
 * <ul>
 *   <li>{@link Density#COMPACT} (紧凑): width &lt; 400 or height &lt; 280, e.g. 320×240, 480×270;</li>
 *   <li>{@link Density#ROOMY} (宽松): width &ge; 800 and height &ge; 500, e.g. 960×540;</li>
 *   <li>{@link Density#STANDARD} (标准): everything else, e.g. 640×360, 640×336, 480×360.</li>
 * </ul>
 * Sizes are logical layout pixels: a {@link TacticalScreen} at GUI scale 1 lays out 960×720 as
 * 480×360 (see {@link UiScale}).
 *
 * <p>Regions, outside in (the D2 device of the preview's {@code 17-device.js},
 * {@code geom(w, h, 2)}): the {@link #device} case D floats on the dimmed world with
 * {@link DeviceMetrics} margins, the {@link #glass} opening S sits inside its bezel, and the lit
 * {@link #display} P is S inset by the glass rim. On the display the {@link #status} bar takes the
 * top rows and the board {@link #body} the rest (one gap from every edge); the {@link #bezel} is
 * the case below the glass (under its 1px recess lip), where the page keys sit. {@link #header()}
 * and {@link #footer()} remain as aliases of the status bar and the bezel.
 */
public record TacticalShellLayout(int width, int height, Metrics metrics,
                                  UiRect device, UiRect glass, UiRect display,
                                  UiRect status, UiRect body, UiRect bezel) {
    /** Size class of a logical screen. */
    public enum Density {
        COMPACT,
        STANDARD,
        ROOMY
    }

    /**
     * Spacing and control sizes of one size class.
     *
     * @param margin        outer margin of the former full-screen frame; full-screen layouts such
     *                      as {@code FormationScreenLayout.narrow()} still measure with it
     * @param gap           gap between the status bar, board and display edge, and between panels
     * @param pad           inner padding of panels
     * @param buttonHeight  standard key height
     * @param rowHeight     list row height
     * @param headerHeight  header strip height of the former full-screen frame
     * @param footerHeight  footer strip height of the former full-screen frame
     * @param sectionHeight section strip height of panels
     */
    public record Metrics(Density density, int margin, int gap, int pad, int buttonHeight,
                          int rowHeight, int headerHeight, int footerHeight, int sectionHeight) {
        public static Metrics of(Density density) {
            return switch (density) {
                case COMPACT -> new Metrics(density, 3, 2, 3, 14, 14, 16, 12, 12);
                case STANDARD -> new Metrics(density, 6, 4, 5, 18, 18, 20, 14, 14);
                case ROOMY -> new Metrics(density, 10, 6, 8, 20, 20, 24, 16, 14);
            };
        }

        public static Metrics forSize(int width, int height) {
            return of(TacticalShellLayout.density(width, height));
        }

        /** The preview's {@code M.tight}. */
        public boolean tight() {
            return density == Density.COMPACT;
        }

        /** The preview's {@code M.roomy}. */
        public boolean roomy() {
            return density == Density.ROOMY;
        }
    }

    /**
     * The D2 device constants of one size class (preview {@code 17-device.js} {@code geom(w, h, 2)}).
     *
     * @param marginX      world visible left and right of the case
     * @param marginTop    world visible above the case
     * @param marginBottom world visible below the case
     * @param side         left / right bezel between the case edge and the glass
     * @param top          top bezel (camera, LEDs, screws, faction stripe, silkscreen)
     * @param bottom       bottom bezel (page keys)
     * @param glassInset   glass rim between the opening and the lit display
     * @param statusHeight status bar height at the top of the display
     * @param bump         how far the corner bumpers and side keys stick out of the case
     * @param corner       bumper length along each case edge
     * @param radius       corner radius of the case
     */
    public record DeviceMetrics(int marginX, int marginTop, int marginBottom, int side, int top,
                                int bottom, int glassInset, int statusHeight, int bump,
                                int corner, int radius) {
        public static DeviceMetrics of(Density density) {
            return switch (density) {
                case COMPACT -> new DeviceMetrics(2, 2, 2, 5, 6, 18, 1, 10, 1, 10, 4);
                case STANDARD -> new DeviceMetrics(12, 6, 5, 10, 10, 24, 2, 11, 2, 17, 6);
                case ROOMY -> new DeviceMetrics(28, 12, 10, 15, 14, 30, 2, 12, 3, 26, 8);
            };
        }
    }

    public static Density density(int width, int height) {
        if (width < 400 || height < 280) {
            return Density.COMPACT;
        }
        if (width >= 800 && height >= 500) {
            return Density.ROOMY;
        }
        return Density.STANDARD;
    }

    /** Shell geometry for a logical screen of {@code width}×{@code height}. */
    public static TacticalShellLayout compute(int width, int height) {
        int w = Math.max(0, width);
        int h = Math.max(0, height);
        return compute(w, h, density(w, h));
    }

    /**
     * Shell geometry for a logical screen of {@code width}×{@code height} drawn in the given size
     * class (the one {@link #density(int, int)} picks, unless a caller such as a device animation
     * keeps a class on purpose). Every region is clamped into its parent, so even degenerate
     * sizes give nested, never inverted rectangles.
     */
    public static TacticalShellLayout compute(int width, int height, Density density) {
        int w = Math.max(0, width);
        int h = Math.max(0, height);
        Density size = density == null ? density(w, h) : density;
        Metrics m = Metrics.of(size);
        DeviceMetrics k = DeviceMetrics.of(size);
        UiRect screen = new UiRect(0, 0, w, h);
        UiRect device = within(screen, k.marginX(), k.marginTop(), w - k.marginX(),
                h - k.marginBottom());
        UiRect glass = within(device, device.left() + k.side(), device.top() + k.top(),
                device.right() - k.side(), device.bottom() - k.bottom());
        UiRect display = within(glass, glass.left() + k.glassInset(),
                glass.top() + k.glassInset(), glass.right() - k.glassInset(),
                glass.bottom() - k.glassInset());
        UiRect status = within(display, display.left(), display.top(), display.right(),
                display.top() + k.statusHeight());
        UiRect body = within(display, display.left() + m.gap(), status.bottom() + m.gap(),
                display.right() - m.gap(), display.bottom() - m.gap());
        // The glass recess has a 1px lit lip on its bottom row; the bezel starts below it.
        UiRect bezel = within(device, glass.left(), glass.bottom() + 1, glass.right(),
                device.bottom());
        return new TacticalShellLayout(w, h, m, device, glass, display, status, body, bezel);
    }

    /** [left, right) × [top, bottom) clamped into {@code parent}, never inverted. */
    private static UiRect within(UiRect parent, int left, int top, int right, int bottom) {
        int l = clamp(left, parent.left(), parent.right());
        int t = clamp(top, parent.top(), parent.bottom());
        int r = clamp(right, l, parent.right());
        int b = clamp(bottom, t, parent.bottom());
        return new UiRect(l, t, r, b);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(Math.max(min, max), value));
    }

    /** The D2 device constants of this layout's size class. */
    public DeviceMetrics deviceMetrics() {
        return DeviceMetrics.of(metrics.density());
    }

    /** Alias of {@link #status}: the strip at the top of the display (the former header). */
    public UiRect header() {
        return status;
    }

    /** Alias of {@link #bezel}: the case below the glass with the page keys (the former footer). */
    public UiRect footer() {
        return bezel;
    }

    /** The body inset by one gap, the usual area for panels ({@code UI.inset(body, M.gap)}). */
    public UiRect content() {
        return body.inset(metrics.gap());
    }

    public Density density() {
        return metrics.density();
    }

    public boolean tight() {
        return metrics.tight();
    }

    public boolean roomy() {
        return metrics.roomy();
    }
}
