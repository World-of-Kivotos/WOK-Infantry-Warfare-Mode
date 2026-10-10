package com.wok.infantry.client.tablet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wok.infantry.client.tablet.TabletPose3D.Context;
import com.wok.infantry.client.tablet.TabletPose3D.FrameQuery;
import com.wok.infantry.client.tablet.TabletPose3D.RectPx;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The C1 close's copy and replay ({@link TabletCapturePlan}; IMPL_PLAN 4.4
 * {@code TabletFrameCapturePlanTest}): copied only from a frame that showed the page and never
 * from the wake band; the block is the E region of the last plane (terminal) or the map rectangle
 * (full-screen page) in whole pixels; it replays in place on the first frame and shrinks with the
 * frame's rectangle; the 0.61 frame belongs to the 3D tablet.
 */
class TabletCapturePlanTest {
    private static final List<String> TIERS = List.of("320x240", "480x270", "640x360", "960x540", "960x720");

    @Test
    void copiedOnlyFromAFrameThatShowedThePageOutsideTheWakeBand() {
        assertTrue(TabletCapturePlan.wants(TabletPath.A3D, true, 1.0D));
        assertTrue(TabletCapturePlan.wants(TabletPath.A3D, true, 0.87D));
        assertTrue(TabletCapturePlan.wants(TabletPath.A3D, true, 0.74D), "0.74 is the zoom");
        assertFalse(TabletCapturePlan.wants(TabletPath.A3D, true, 0.70D), "wake band");
        assertFalse(TabletCapturePlan.wants(TabletPath.A3D, true, 0.61D), "READ: nothing drawn yet");
        assertFalse(TabletCapturePlan.wants(TabletPath.A3D, true, 0.4D));
        assertFalse(TabletCapturePlan.wants(TabletPath.A3D, false, 1.0D), "capture off");
        assertFalse(TabletCapturePlan.wants(TabletPath.B2D, true, 1.0D), "scheme B");
        // Same band as the motion's skipWake.
        for (double p = 0.6D; p <= 1.0D; p += 0.005D) {
            TabletMotion m = new TabletMotion(TabletMotion.Config.DEFAULT);
            m.open(0.0D);
            m.freeze(p);
            m.update(1.0D);
            m.close(2.0D);
            boolean motionCaptures = m.closeFrom().capture() && p > TabletAnimationModel.SEG_RAISE_TO + 1e-9;
            assertEquals(motionCaptures, TabletCapturePlan.wants(TabletPath.A3D, true, p), "p=" + p);
        }
    }

    @Test
    void blockIsTheEOfTheLastPlaneOrTheMapRectangleInWholePixels() {
        for (String tier : TIERS) {
            for (TabletScreenKind kind : new TabletScreenKind[]{TabletScreenKind.SQUAD, TabletScreenKind.FULLSCREEN}) {
                Context c = TabletPose3DTest.context(tier, kind);
                for (double last : new double[]{1.0D, 0.95D, 0.87D, 0.74D}) {
                    TabletFrame from = TabletPose3D.frame(last, c, new FrameQuery(false,
                            TabletMotion.Curve.OPEN, true, last, TabletHand.GUN, 0.0D));
                    TabletCapturePlan.Region r = TabletCapturePlan.region(from, c.term().pxW(),
                            c.term().pxH());
                    String what = tier + " " + kind + " from " + last;
                    assertFalse(r.isEmpty(), what);
                    RectPx box = kind.isTerminal()
                            ? TabletPose3D.subRectPx(from.capture().fromPlanePx(), c.geom(), c.geom().e())
                            : from.capture().fromRectPx();
                    assertTrue(r.x() <= Math.max(0.0D, box.x()) + 1e-9 && r.y() <= Math.max(0.0D, box.y()) + 1e-9,
                            what + " covers the top-left");
                    assertTrue(r.right() >= Math.min(c.term().pxW(), box.x() + box.w()) - 1e-9
                            && r.bottom() >= Math.min(c.term().pxH(), box.y() + box.h()) - 1e-9,
                            what + " covers the bottom-right");
                    assertTrue(r.x() >= 0 && r.y() >= 0 && r.right() <= c.term().pxW()
                            && r.bottom() <= c.term().pxH(), what + " inside the window");
                    assertTrue(r.w() - box.w() < 2.0D + 1e-9 || r.w() == c.term().pxW(), what + " at most one pixel more each side");
                    if (kind == TabletScreenKind.FULLSCREEN && last == 1.0D) {
                        assertEquals(new TabletCapturePlan.Region(0, 0, c.term().pxW(), c.term().pxH()), r,
                                what + " a shown map is the whole window");
                    }
                }
            }
        }
    }

    @Test
    void replayStartsInPlaceAndFollowsTheFrameRectangle() {
        for (JsonElement element : TabletVectors.array("frames")) {
            JsonObject group = element.getAsJsonObject();
            if (!group.get("variant").getAsString().equals("closeC1")) {
                continue;
            }
            String tier = group.get("tier").getAsString();
            TabletScreenKind kind = TabletVectors.screen(group.get("screen").getAsString());
            TabletHand hand = TabletHand.byPreviewName(group.get("hand").getAsString());
            Context c = TabletPose3DTest.context(tier, kind);
            FrameQuery q = TabletPose3DTest.query(group.getAsJsonObject("ctx"), hand);
            TabletFrame first = TabletPose3D.frame(q.captureFromP(), c, q);
            TabletCapturePlan.Region region = TabletCapturePlan.region(first, c.term().pxW(),
                    c.term().pxH());
            double[] start = TabletCapturePlan.replay(region, first.capture().fromRectPx(),
                    first.capture().rectPx());
            assertEquals(region.x(), start[0], 1e-9, tier + " " + kind + " in place x");
            assertEquals(region.y(), start[1], 1e-9, tier + " " + kind + " in place y");
            assertEquals(region.w(), start[2], 1e-9);
            assertEquals(region.h(), start[3], 1e-9);
            for (JsonElement fe : group.getAsJsonArray("frames")) {
                JsonObject e = fe.getAsJsonObject();
                double p = e.get("p").getAsDouble();
                TabletFrame f = TabletPose3D.frame(p, c, q);
                boolean draw = e.getAsJsonObject("capture").get("draw").getAsBoolean();
                assertEquals(draw, f.capture().draw(), tier + " p=" + p);
                if (!draw) {
                    if (Math.abs(p - 0.61D) < 1e-9) {
                        assertTrue(f.tablet().draw() || kind != TabletScreenKind.SQUAD,
                                tier + " the 0.61 frame is the 3D tablet's");
                    }
                    continue;
                }
                double[] at = TabletCapturePlan.replay(region, f.capture().fromRectPx(),
                        f.capture().rectPx());
                RectPx now = f.capture().rectPx();
                RectPx from = f.capture().fromRectPx();
                double k = now.w() / from.w();
                assertEquals(now.x() + (region.x() - from.x()) * k, at[0], 1e-9, tier + " p=" + p);
                assertEquals(region.w() * k, at[2], 1e-9, tier + " p=" + p);
                assertTrue(k <= 1.0D + 1e-9, tier + " p=" + p + " shrinks");
            }
        }
    }

    @Test
    void mipmapsBelowHalfSize() {
        assertFalse(TabletCapturePlan.mipmap(1.0D));
        assertFalse(TabletCapturePlan.mipmap(0.55D));
        assertFalse(TabletCapturePlan.mipmap(0.5D));
        assertTrue(TabletCapturePlan.mipmap(0.48D), "the 960x720 map reads at about 0.48");
        assertFalse(TabletCapturePlan.mipmap(0.0D));
    }

    @Test
    void planeInRegionIsRelativeToTheBlock() {
        TabletCapturePlan.Region r = new TabletCapturePlan.Region(10, 20, 300, 200);
        double[] at = TabletCapturePlan.planeInRegion(r, new RectPx(5.5D, 18.0D, 320.0D, 240.0D, 1.0D));
        assertEquals(-4.5D, at[0], 0.0D);
        assertEquals(-2.0D, at[1], 0.0D);
        assertEquals(320.0D, at[2], 0.0D);
        assertEquals(240.0D, at[3], 0.0D);
    }
}
