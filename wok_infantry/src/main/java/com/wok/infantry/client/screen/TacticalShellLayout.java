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
 * <p>Regions, outside in (the device shell of the preview's {@code 17-device.js} {@code geom}):
 * {@link #device} is the case, {@link #glass} the glass opening in it, {@link #display} the lit
 * screen behind the glass; on the display the {@link #status} bar sits above the board
 * {@link #body}, and the {@link #bezel} carries the page keys. With the full-screen frame the
 * device is the whole screen, the glass and display are the area inside the outer margin, the
 * status bar is the header strip and the bezel is the footer strip.
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
     * @param margin        outer margin between the screen edge and the header/body/footer
     * @param gap           gap between the header, body and footer, and between panels
     * @param pad           inner padding of panels
     * @param buttonHeight  standard key height
     * @param rowHeight     list row height
     * @param headerHeight  header strip height
     * @param footerHeight  footer strip height
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
        Metrics m = Metrics.forSize(w, h);
        int headerBottom = m.margin() + m.headerHeight();
        int footerTop = Math.max(headerBottom, h - m.margin() - m.footerHeight());
        UiRect header = new UiRect(m.margin(), m.margin(), w - m.margin(), headerBottom);
        UiRect footer = new UiRect(m.margin(), footerTop, w - m.margin(),
                Math.max(footerTop, h - m.margin()));
        int bodyTop = headerBottom + m.gap();
        int bodyBottom = Math.max(bodyTop, footerTop - m.gap());
        UiRect body = new UiRect(m.margin(), bodyTop, w - m.margin(), bodyBottom);
        UiRect device = new UiRect(0, 0, w, h);
        UiRect glass = new UiRect(m.margin(), m.margin(), w - m.margin(),
                Math.max(m.margin(), h - m.margin()));
        return new TacticalShellLayout(w, h, m, device, glass, glass, header, body, footer);
    }

    /** The status strip at the top of the display (the former header). */
    public UiRect header() {
        return status;
    }

    /** The strip that carries the page keys (the former footer). */
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
