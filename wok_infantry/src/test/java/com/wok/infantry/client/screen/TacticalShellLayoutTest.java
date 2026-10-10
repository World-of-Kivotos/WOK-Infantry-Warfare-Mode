package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalShellLayout.Density;
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
     * GUI-1 window laid out at 2x (480×360) and the raw 960×720 with the 2x option off.
     */
    @ParameterizedTest(name = "{0}x{1} -> {2}")
    @CsvSource({
            "320, 240, COMPACT",
            "480, 270, COMPACT",
            "640, 360, STANDARD",
            "640, 336, STANDARD",
            "960, 540, ROOMY",
            "480, 360, STANDARD",
            "960, 720, ROOMY"
    })
    void headerBodyAndFooterNeverOverlapAndStayOnScreen(int width, int height, Density density) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        Metrics m = layout.metrics();
        UiRect screen = UiRect.of(0, 0, width, height);

        assertEquals(density, m.density());
        assertEquals(m.headerHeight(), layout.header().height());
        assertEquals(m.footerHeight(), layout.footer().height());
        assertTrue(screen.contains(layout.header()), "header inside screen");
        assertTrue(screen.contains(layout.body()), "body inside screen");
        assertTrue(screen.contains(layout.footer()), "footer inside screen");
        assertFalse(layout.header().intersects(layout.body()), "header/body overlap");
        assertFalse(layout.body().intersects(layout.footer()), "body/footer overlap");
        assertFalse(layout.header().intersects(layout.footer()), "header/footer overlap");
        // The board is outlined 1px outside the body; that outline must stay in the gaps.
        UiRect outline = layout.body().inset(-1);
        assertFalse(outline.intersects(layout.header()), "body outline touches header");
        assertFalse(outline.intersects(layout.footer()), "body outline touches footer");
        assertEquals(layout.header().bottom() + m.gap(), layout.body().top());
        assertEquals(layout.footer().top() - m.gap(), layout.body().bottom());
        assertEquals(height - m.margin(), layout.footer().bottom());
        assertTrue(layout.body().height() >= 140, "usable board at " + width + "x" + height);
        assertTrue(layout.body().contains(layout.content()));
    }

    /** Device regions nest outside in; header and footer stay the status bar and the bezel. */
    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({
            "320, 240", "480, 270", "640, 360", "640, 336", "960, 540", "480, 360", "960, 720"
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
    }

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

        assertEquals(UiRect.of(3, 3, 317, 19), layout.header());
        assertEquals(UiRect.of(3, 21, 317, 223), layout.body());
        assertEquals(UiRect.of(3, 225, 317, 237), layout.footer());
        assertEquals(UiRect.of(5, 23, 315, 221), layout.content());
    }

    @Test
    void degenerateSizesNeverInvertTheBody() {
        TacticalShellLayout layout = TacticalShellLayout.compute(40, 30);

        assertTrue(layout.body().bottom() >= layout.body().top());
        assertTrue(layout.footer().bottom() >= layout.footer().top());
        assertFalse(layout.header().intersects(layout.footer()));
    }
}
