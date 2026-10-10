package com.wok.infantry.client.tablet;

import com.wok.infantry.client.screen.TacticalShellLayout;
import com.wok.infantry.client.screen.UiRect;

/**
 * The D2 device geometry of a terminal layout (preview {@code spec.d2geom}), in integer logical
 * pixels: D = case ({@code device()}), S = glass opening ({@code glass()}), P = lit display
 * ({@code display()}), E = D grown by the corner bumpers ({@code deviceMetrics().bump()}).
 *
 * @param w       layout width (lp)
 * @param h       layout height (lp)
 * @param density size class
 * @param k       device constants of the size class
 */
public record TabletD2Geometry(int w, int h, TacticalShellLayout.Density density,
                               TacticalShellLayout.DeviceMetrics k,
                               UiRect d, UiRect s, UiRect p, UiRect e) {
    /** The geometry {@link TacticalShellLayout#compute(int, int)} gives a {@code lpW}×{@code lpH} layout. */
    public static TabletD2Geometry of(int lpW, int lpH) {
        TacticalShellLayout layout = TacticalShellLayout.compute(lpW, lpH);
        TacticalShellLayout.DeviceMetrics k = layout.deviceMetrics();
        UiRect d = layout.device();
        int bump = k.bump();
        UiRect e = new UiRect(d.left() - bump, d.top() - bump, d.right() + bump, d.bottom() + bump);
        return new TabletD2Geometry(layout.width(), layout.height(), layout.density(), k, d,
                layout.glass(), layout.display(), e);
    }
}
