package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalShellLayout.Density;
import com.wok.infantry.client.screen.TacticalShellLayout.DeviceMetrics;
import com.wok.infantry.client.screen.TacticalShellLayout.Metrics;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalShellLayoutTest {
    /**
     * The five acceptance tiers, the user's real 1920×1008 at GUI 3 (640×336), the 960×720
     * GUI-1 window laid out at 2x (480×360), the default window at 2x (427×240) and the raw
     * 960×720 with the 2x option off.
     */
    @ParameterizedTest(name = "{0}x{1} -> {2}")
    @CsvSource({
            "320, 240, COMPACT",
            "427, 240, COMPACT",
            "480, 270, COMPACT",
            "640, 360, STANDARD",
            "640, 336, STANDARD",
            "960, 540, ROOMY",
            "480, 360, STANDARD",
            "960, 720, ROOMY"
    })
    void statusBodyAndBezelNeverOverlapAndStayOnTheDevice(int width, int height, Density density) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        Metrics m = layout.metrics();
        DeviceMetrics k = layout.deviceMetrics();
        UiRect screen = UiRect.of(0, 0, width, height);

        assertEquals(density, m.density());
        assertEquals(k.statusHeight(), layout.status().height());
        assertTrue(screen.contains(layout.device()), "device inside screen");
        assertTrue(layout.display().contains(layout.status()), "status bar on the display");
        assertTrue(layout.display().contains(layout.body()), "body on the display");
        assertTrue(layout.device().contains(layout.bezel()), "bezel on the case");
        assertFalse(layout.status().intersects(layout.body()), "status/body overlap");
        assertFalse(layout.body().intersects(layout.bezel()), "body/bezel overlap");
        assertFalse(layout.status().intersects(layout.bezel()), "status/bezel overlap");
        assertFalse(layout.glass().intersects(layout.bezel()), "the bezel starts below the glass");
        // The board is outlined 1px outside the body; that outline must stay in the gaps.
        UiRect outline = layout.body().inset(-1);
        assertFalse(outline.intersects(layout.status()), "body outline touches the status bar");
        assertTrue(layout.display().contains(outline), "body outline stays on the display");
        assertEquals(layout.status().bottom() + m.gap(), layout.body().top());
        assertEquals(layout.display().bottom() - m.gap(), layout.body().bottom());
        assertEquals(layout.display().left() + m.gap(), layout.body().left());
        assertEquals(layout.display().right() - m.gap(), layout.body().right());
        assertEquals(layout.device().bottom(), layout.bezel().bottom());
        assertEquals(layout.glass().bottom() + 1, layout.bezel().top(), "below the recess lip");
        assertTrue(layout.body().height() >= 140, "usable board at " + width + "x" + height);
        assertTrue(layout.body().contains(layout.content()));
    }

    /** Device regions nest outside in; header and footer stay the status bar and the bezel. */
    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({
            "320, 240", "427, 240", "480, 270", "640, 360", "640, 336", "960, 540", "480, 360",
            "960, 720"
    })
    void deviceRegionsNestInsideTheScreen(int width, int height) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        UiRect screen = UiRect.of(0, 0, width, height);

        assertEquals(layout.status(), layout.header());
        assertEquals(layout.bezel(), layout.footer());
        assertEquals(layout.metrics().density(), layout.density());
        assertTrue(screen.contains(layout.device()), "device inside screen");
        assertTrue(layout.device().contains(layout.glass()), "glass inside device");
        assertTrue(layout.glass().contains(layout.display()), "display inside glass");
        assertTrue(layout.display().contains(layout.status()), "status bar on the display");
        assertTrue(layout.display().contains(layout.body()), "board on the display");
        assertTrue(layout.device().contains(layout.bezel()), "bezel on the device");
        assertFalse(layout.status().intersects(layout.bezel()), "status/bezel overlap");
        // Bumpers and side keys stick out of the case by `bump`; they must stay on screen too.
        int bump = layout.deviceMetrics().bump();
        assertTrue(screen.contains(layout.device().inset(-bump)), "bumpers inside screen");
    }

    /** The D2 constant table (preview 17-device.js geom(w, h, 2)). */
    @Test
    void deviceMetricsFollowThePreviewGeometry() {
        assertEquals(new DeviceMetrics(2, 2, 2, 5, 6, 18, 1, 10, 1, 10, 4),
                DeviceMetrics.of(Density.COMPACT));
        assertEquals(new DeviceMetrics(12, 6, 5, 10, 10, 24, 2, 11, 2, 17, 6),
                DeviceMetrics.of(Density.STANDARD));
        assertEquals(new DeviceMetrics(28, 12, 10, 15, 14, 30, 2, 12, 3, 26, 8),
                DeviceMetrics.of(Density.ROOMY));
        assertEquals(DeviceMetrics.of(Density.STANDARD),
                TacticalShellLayout.compute(640, 360).deviceMetrics());
    }

    /** Former full-screen metrics keep their meaning (FormationScreenLayout.narrow uses them). */
    @Test
    void metricsFollowThePreviewTable() {
        assertEquals(new Metrics(Density.COMPACT, 3, 2, 3, 14, 14, 16, 12, 12),
                TacticalShellLayout.compute(320, 240).metrics());
        assertEquals(new Metrics(Density.STANDARD, 6, 4, 5, 18, 18, 20, 14, 14),
                TacticalShellLayout.compute(640, 360).metrics());
        assertEquals(new Metrics(Density.ROOMY, 10, 6, 8, 20, 20, 24, 16, 14),
                TacticalShellLayout.compute(960, 540).metrics());
    }

    @Test
    void densityBoundariesMatchUiMetrics() {
        assertEquals(Density.COMPACT, TacticalShellLayout.density(399, 400));
        assertEquals(Density.COMPACT, TacticalShellLayout.density(800, 279));
        assertEquals(Density.STANDARD, TacticalShellLayout.density(400, 280));
        assertEquals(Density.STANDARD, TacticalShellLayout.density(799, 600));
        assertEquals(Density.STANDARD, TacticalShellLayout.density(900, 499));
        assertEquals(Density.ROOMY, TacticalShellLayout.density(800, 500));
    }

    @Test
    void compactTierGeometryIsExact() {
        TacticalShellLayout layout = TacticalShellLayout.compute(320, 240);

        assertEquals(UiRect.of(2, 2, 318, 238), layout.device());
        assertEquals(UiRect.of(7, 8, 313, 220), layout.glass());
        assertEquals(UiRect.of(8, 9, 312, 219), layout.display());
        assertEquals(UiRect.of(8, 9, 312, 19), layout.status());
        assertEquals(UiRect.of(10, 21, 310, 217), layout.body());
        assertEquals(UiRect.of(7, 221, 313, 238), layout.bezel());
        assertEquals(UiRect.of(12, 23, 308, 215), layout.content());
    }

    @Test
    void standardTierGeometryIsExact() {
        // 960×720 at GUI 1 under the minimum 2x (plan 4.8: content 416×284).
        TacticalShellLayout layout = TacticalShellLayout.compute(480, 360);

        assertEquals(UiRect.of(12, 6, 468, 355), layout.device());
        assertEquals(UiRect.of(22, 16, 458, 331), layout.glass());
        assertEquals(UiRect.of(24, 18, 456, 329), layout.display());
        assertEquals(UiRect.of(24, 18, 456, 29), layout.status());
        assertEquals(UiRect.of(28, 33, 452, 325), layout.body());
        assertEquals(UiRect.of(22, 332, 458, 355), layout.bezel());
        assertEquals(UiRect.of(32, 37, 448, 321), layout.content());
    }

    @Test
    void roomyTierGeometryIsExact() {
        TacticalShellLayout layout = TacticalShellLayout.compute(960, 540);

        assertEquals(UiRect.of(28, 12, 932, 530), layout.device());
        assertEquals(UiRect.of(43, 26, 917, 500), layout.glass());
        assertEquals(UiRect.of(45, 28, 915, 498), layout.display());
        assertEquals(UiRect.of(45, 28, 915, 40), layout.status());
        assertEquals(UiRect.of(51, 46, 909, 492), layout.body());
        assertEquals(UiRect.of(43, 501, 917, 530), layout.bezel());
        assertEquals(UiRect.of(57, 52, 903, 486), layout.content());
    }

    /** Plan 4.8: the content size the page painters get on every tier. */
    @ParameterizedTest(name = "{0}x{1} -> {2}x{3}")
    @CsvSource({
            "320, 240, 296, 192",
            "427, 240, 403, 192",
            "480, 270, 456, 222",
            "480, 360, 416, 284",
            "640, 336, 576, 260",
            "640, 360, 576, 284",
            "960, 540, 846, 434"
    })
    void contentMatchesThePlanTable(int width, int height, int contentWidth, int contentHeight) {
        UiRect content = TacticalShellLayout.compute(width, height).content();

        assertEquals(contentWidth, content.width());
        assertEquals(contentHeight, content.height());
    }

    @Test
    void anExplicitSizeClassKeepsItsConstants() {
        // A device animation may hold one size class while the screen changes size.
        TacticalShellLayout held = TacticalShellLayout.compute(480, 360, Density.ROOMY);

        assertEquals(Density.ROOMY, held.density());
        assertEquals(DeviceMetrics.of(Density.ROOMY), held.deviceMetrics());
        assertEquals(UiRect.of(28, 12, 452, 350), held.device());
        assertEquals(TacticalShellLayout.compute(480, 360),
                TacticalShellLayout.compute(480, 360, null), "null picks the screen's own class");
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"0, 0", "40, 30", "1, 400", "400, 1", "-5, -5", "60, 250"})
    void degenerateSizesNeverInvertARegion(int width, int height) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);

        for (UiRect region : new UiRect[]{layout.device(), layout.glass(), layout.display(),
                layout.status(), layout.body(), layout.bezel()}) {
            assertTrue(region.right() >= region.left() && region.bottom() >= region.top(),
                    region + " at " + width + "x" + height);
        }
        assertTrue(layout.device().contains(layout.glass()));
        assertTrue(layout.glass().contains(layout.display()));
        assertTrue(layout.display().contains(layout.body()));
        assertFalse(layout.status().intersects(layout.bezel()));
    }
}
