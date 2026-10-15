package com.wok.infantry.client.tablet;

import com.wok.infantry.client.screen.DeviceArt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Scheme A's wake effects on the glass (preview {@code compose.drawA}), in physical pixels. */
class TabletEffects2DTest {
    private static final List<String> TIERS = List.of("320x240", "480x270", "640x360", "960x540",
            "960x720");

    private static TabletFrame open(String tier, double p) {
        TabletPose3D.Context c = TabletPose3DTest.context(tier, TabletScreenKind.SQUAD);
        return TabletPose3D.frame(p, c, TabletPose3D.FrameQuery.opening(TabletHand.GUN));
    }

    @Test
    void outsideTheWakeBandNothingIsDrawn() {
        for (String tier : TIERS) {
            for (double p : new double[]{0.3D, 0.6D, 0.74D, 0.8D, 1.0D}) {
                TabletFrame f = open(tier, p);
                int s = f.ctx().units().guiScale();
                assertTrue(TabletEffects2D.wakeFills(f.glassPx(), f.fx(), s).isEmpty(),
                        tier + " p=" + p);
            }
        }
    }

    @Test
    void atReadTheGlassIsShutWithOneLightLineInTheMiddle() {
        for (String tier : TIERS) {
            TabletFrame f = open(tier, 0.61D);
            int s = f.ctx().units().guiScale();
            List<TabletEffects2D.Fill> fills = TabletEffects2D.wakeFills(f.glassPx(), f.fx(), s);
            TabletPose3D.RectPx g = f.glassPx();
            int glassFills = 0;
            int light = 0;
            for (TabletEffects2D.Fill fill : fills) {
                if ((fill.argb() & 0xFFFFFF) == (DeviceArt.GLASS & 0xFFFFFF)) {
                    glassFills++;
                    assertEquals(0xFF, fill.argb() >>> 24, "the bands are opaque");
                } else if ((fill.argb() & 0xFFFFFF) == (TabletAnimationModel.LIGHT & 0xFFFFFF)) {
                    light++;
                    assertEquals(s, fill.h(), tier + " one GUI pixel");
                    assertEquals(Math.round(255 * TabletAnimationModel.WAKE_EDGE_ALPHA),
                            fill.argb() >>> 24);
                }
            }
            assertEquals(2, glassFills, tier + " both bands");
            assertEquals(1, light, tier + " the two edges are still one line");
            int covered = 0;
            for (TabletEffects2D.Fill fill : fills) {
                if ((fill.argb() & 0xFFFFFF) == (DeviceArt.GLASS & 0xFFFFFF)) {
                    covered += fill.h();
                }
            }
            int h = (int) Math.round(g.y() + g.h()) - (int) Math.round(g.y());
            assertTrue(covered >= h - 1, tier + " the bands meet (" + covered + " of " + h + ")");
        }
    }

    @Test
    void whileOpeningTheBandsAreSymmetricAndEverythingStaysOnTheGlass() {
        for (String tier : TIERS) {
            for (int step = 0; step <= 26; step++) {
                double p = 0.61D + step * 0.005D;
                TabletFrame f = open(tier, p);
                int s = f.ctx().units().guiScale();
                TabletPose3D.RectPx g = f.glassPx();
                int x0 = (int) Math.round(g.x());
                int y0 = (int) Math.round(g.y());
                int x1 = (int) Math.round(g.x() + g.w());
                int y1 = (int) Math.round(g.y() + g.h());
                List<TabletEffects2D.Fill> fills = TabletEffects2D.wakeFills(g, f.fx(), s);
                TabletEffects2D.Fill top = null;
                TabletEffects2D.Fill bottom = null;
                for (TabletEffects2D.Fill fill : fills) {
                    String what = tier + " p=" + p + " " + fill;
                    assertTrue(fill.x() >= x0 && fill.x() + fill.w() <= x1, what + " x");
                    assertTrue(fill.y() >= y0 && fill.y() + fill.h() <= y1, what + " y");
                    if ((fill.argb() & 0xFFFFFF) == (DeviceArt.GLASS & 0xFFFFFF)) {
                        if (fill.y() == y0) {
                            top = fill;
                        } else {
                            bottom = fill;
                        }
                    }
                }
                if (top != null || bottom != null) {
                    assertTrue(top != null && bottom != null, tier + " p=" + p + " two bands");
                    assertEquals(top.h(), bottom.h(), tier + " p=" + p + " symmetric");
                    assertEquals(y1, bottom.y() + bottom.h(), tier + " p=" + p + " bottom band");
                }
            }
        }
    }
}
