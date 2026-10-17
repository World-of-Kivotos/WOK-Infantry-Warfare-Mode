package com.wok.infantry.client.tablet;

import com.wok.infantry.client.screen.TacticalShellLayout;
import com.wok.infantry.client.screen.UiRect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Scheme B's map frame (DESIGN 4.2): the plate drawn around the inner screen is a D2 device whose
 * display P, glass S and case D land exactly on the frame's boxes, at every tier and progress.
 */
class TabletMapFrameTest {
    private static final List<String> TIERS = List.of("320x240", "480x270", "640x360", "960x540",
            "960x720");

    private static void same(TabletPath2D.Box expected, UiRect actual, String what) {
        assertEquals(new UiRect(expected.x(), expected.y(), expected.right(), expected.bottom()),
                actual, what);
    }

    @Test
    void thePlateLandsOnTheFrameBoxes() {
        int checked = 0;
        for (String tier : TIERS) {
            TabletUnits term = TabletVectors.termUnits(tier);
            TabletUnits map = TabletVectors.mapUnits(tier);
            for (boolean quick : new boolean[]{false, true}) {
                for (boolean open : new boolean[]{true, false}) {
                    for (int step = 0; step < 100; step++) {
                        double p = step / 100.0D;
                        TabletPath2D.MapFrame f = TabletPath2D.mapFrame(p, map, term, open, quick);
                        TabletMapFrame.Art art = TabletMapFrame.art(f);
                        TacticalShellLayout layout = art.layout();
                        String what = tier + (quick ? " quick" : "") + (open ? " open" : " close")
                                + " p=" + p;
                        assertEquals(f.geom().density(), layout.density(), what + " size class");
                        assertEquals(f.cs(), art.cs(), what + " cs");
                        same(f.inner(), art.place(layout.display()), what + " P = inner screen");
                        same(f.s(), art.place(layout.glass()), what + " S");
                        same(f.d(), art.place(layout.device()), what + " D");
                        checked++;
                    }
                }
            }
        }
        assertEquals(TIERS.size() * 2 * 2 * 100, checked);
    }

    @Test
    void theGuiOneMapFrameIsDrawnAtTwiceTheTerminalPixels() {
        TabletPath2D.MapFrame f = TabletPath2D.mapFrame(0.3D, TabletVectors.mapUnits("960x720"),
                TabletVectors.termUnits("960x720"), true, false);
        TabletMapFrame.Art art = TabletMapFrame.art(f);
        assertEquals(2, art.cs());
        assertEquals(f.inner().w() / 2, art.layout().display().width());
        assertEquals(f.inner().h() / 2, art.layout().display().height());
    }
}
