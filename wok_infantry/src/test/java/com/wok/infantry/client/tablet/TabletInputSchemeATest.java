package com.wok.infantry.client.tablet;

import com.wok.infantry.client.tablet.TabletInput.Delivery;
import com.wok.infantry.client.tablet.TabletMotion.Input;
import com.wok.infantry.client.tablet.TabletMotion.State;
import com.wok.infantry.client.tablet.TabletPose3D.Context;
import com.wok.infantry.client.tablet.TabletPose3D.FrameQuery;
import com.wok.infantry.client.tablet.TabletPose3D.RectPx;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mouse input while scheme A comes out (DESIGN 2.5, IMPL_PLAN 4.3 "输入"): before READ every press
 * is swallowed and jumps to the end; from READ a press on the lit page is moved back from the
 * frame's rectangle (u = (mouse − origin) / s, in GUI units), one on the wake bands or outside the
 * page is swallowed.
 */
class TabletInputSchemeATest {
    private static Delivery press(TabletFrame f, double guiX, double guiY, int guiScale) {
        return TabletInput.decide(TabletPath.A3D, State.OPENING, f.p(), Input.MOUSE_DOWN,
                TabletScreenKind.SQUAD, null, null, f, guiX, guiY, f.ctx().term().factor(),
                guiScale, false);
    }

    @Test
    void beforeReadEveryPressJumpsToTheEnd() {
        Context c = TabletPose3DTest.context("480x270", TabletScreenKind.SQUAD);
        for (double p : new double[]{0.0D, 0.3D, 0.6D}) {
            TabletFrame f = TabletPose3D.frame(p, c, FrameQuery.opening(TabletHand.GUN));
            assertEquals(Delivery.BLOCK, press(f, 240, 135, 4), "p=" + p);
        }
    }

    @Test
    void aPressOnThePageIsMovedBackFromTheFrameRectangle() {
        for (String tier : new String[]{"320x240", "480x270", "640x360", "960x540", "960x720"}) {
            Context c = TabletPose3DTest.context(tier, TabletScreenKind.SQUAD);
            int s = c.term().guiScale();
            TabletFrame f = TabletPose3D.frame(0.87D, c, FrameQuery.opening(TabletHand.GUN));
            RectPx r = f.ui().rectPx();
            // The centre of the page on screen is the centre of the identity page.
            double cx = (r.x() + r.w() / 2.0D) / s;
            double cy = (r.y() + r.h() / 2.0D) / s;
            Delivery d = press(f, cx, cy, s);
            assertTrue(d.cancel() && d.deliver() && d.jump(), tier);
            assertEquals(c.term().pxW() / 2.0D / s, d.x(), 1e-6, tier + " x");
            assertEquals(c.term().pxH() / 2.0D / s, d.y(), 1e-6, tier + " y");
            // A GUI point p maps to (p·s − origin) / scale / s.
            Delivery corner = press(f, (r.x() + 10.0D) / s, (r.y() + 20.0D) / s, s);
            assertEquals(10.0D / r.scale() / s, corner.x(), 1e-6, tier + " corner x");
            assertEquals(20.0D / r.scale() / s, corner.y(), 1e-6, tier + " corner y");
        }
    }

    @Test
    void aPressOutsideThePageOrOnTheWakeBandsIsSwallowed() {
        Context c = TabletPose3DTest.context("640x360", TabletScreenKind.SQUAD);
        int s = c.term().guiScale();
        TabletFrame f = TabletPose3D.frame(0.87D, c, FrameQuery.opening(TabletHand.GUN));
        RectPx r = f.ui().rectPx();
        assertTrue(r.x() > 2, "the page does not fill the window yet");
        assertEquals(Delivery.BLOCK, press(f, 1.0D / s, 1.0D / s, s), "the world beside the page");
        // Early in the wake the bands still cover the glass top and bottom.
        TabletFrame wake = TabletPose3D.frame(0.62D, c, FrameQuery.opening(TabletHand.GUN));
        RectPx g = wake.glassPx();
        double x = g.x() + g.w() / 2.0D;
        assertFalse(TabletPose3D.contentVisible(wake, x, g.y() + 1.0D), "band at the top");
        assertEquals(Delivery.BLOCK, press(wake, x / s, (g.y() + 1.0D) / s, s), "band swallowed");
        Delivery middle = press(wake, x / s, (g.y() + g.h() / 2.0D) / s, s);
        assertTrue(middle.deliver(), "the opened middle is lit");
    }

    @Test
    void nothingIsInterceptedOnceShownOrWhileClosing() {
        Context c = TabletPose3DTest.context("480x270", TabletScreenKind.SQUAD);
        TabletFrame f = TabletPose3D.frame(0.87D, c, FrameQuery.opening(TabletHand.GUN));
        for (State state : new State[]{State.SHOWN, State.CLOSING, State.IDLE}) {
            assertEquals(Delivery.PASS, TabletInput.decide(TabletPath.A3D, state, 0.87D,
                    Input.MOUSE_DOWN, TabletScreenKind.SQUAD, null, null, f, 100, 100, 1, 4, false),
                    state.name());
        }
    }
}
