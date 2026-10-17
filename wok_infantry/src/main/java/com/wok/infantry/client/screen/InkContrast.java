package com.wok.infantry.client.screen;

/**
 * Pure colour arithmetic for paint that is solved from other paint instead of copied from the
 * preview (the derived key colours of {@link DeviceSkin}): WCAG 2 contrast and a channel mix.
 */
final class InkContrast {
    private InkContrast() {
    }

    /**
     * WCAG 2 contrast of {@code ink} drawn on {@code ground}: a translucent ink is composited onto
     * the ground first, the ground counts as opaque.
     */
    static double ratio(int ink, int ground) {
        int base = ground | 0xFF000000;
        double a = luminance(over(ink, base));
        double b = luminance(base);
        return (Math.max(a, b) + 0.05D) / (Math.min(a, b) + 0.05D);
    }

    /**
     * Channel-wise mix of two opaque colours: {@code t} 0 gives {@code from}, 1 gives {@code to}
     * (clamped), rounded like the preview's {@code mixColor}; the result is opaque.
     */
    static int mix(int from, int to, double t) {
        double k = t > 0.0D ? Math.min(1.0D, t) : 0.0D;
        int result = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int a = (from >>> shift) & 0xFF;
            int b = (to >>> shift) & 0xFF;
            result |= (int) Math.round(a + (b - a) * k) << shift;
        }
        return result;
    }

    private static int over(int top, int ground) {
        double alpha = (top >>> 24) / 255.0D;
        int color = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            double channel = ((top >>> shift) & 0xFF) * alpha
                    + ((ground >>> shift) & 0xFF) * (1.0D - alpha);
            color |= (int) Math.round(channel) << shift;
        }
        return color;
    }

    private static double luminance(int color) {
        return 0.2126D * linear((color >>> 16) & 0xFF) + 0.7152D * linear((color >>> 8) & 0xFF)
                + 0.0722D * linear(color & 0xFF);
    }

    private static double linear(int channel) {
        double c = channel / 255.0D;
        return c <= 0.04045D ? c / 12.92D : Math.pow((c + 0.055D) / 1.055D, 2.4D);
    }
}
