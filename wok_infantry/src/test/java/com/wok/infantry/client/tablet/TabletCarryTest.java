package com.wok.infantry.client.tablet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wok.infantry.client.tablet.TabletPose3D.Context;
import com.wok.infantry.client.tablet.TabletPose3D.FrameQuery;
import com.wok.infantry.client.tablet.TabletPose3D.Pose;
import org.junit.jupiter.api.Test;

import static com.wok.infantry.client.tablet.TabletVectors.near;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Java patch D4 (review leftover ②): closing in the wake band carries the plane's sink into the
 * close and lets it fade over 100 ms ({@code vectors.javaExtras.skipWakeCarry}), so the first close
 * frame shows exactly the last open frame instead of jumping up to 13 px.
 */
class TabletCarryTest {
    private static TabletMotion closedInTheWakeBand(double pLast, double now) {
        TabletMotion m = new TabletMotion();
        m.open(0.0D);
        m.freeze(pLast);
        m.update(now - 1.0D);
        m.unfreeze(now - 1.0D);
        assertEquals(pLast, m.p(), 1e-12);
        m.close(now);
        return m;
    }

    @Test
    void carryAndDecayMatchTheDefinition() {
        JsonObject extra = TabletVectors.object("javaExtras").getAsJsonObject("skipWakeCarry");
        assertEquals(TabletAnimationModel.SKIP_WAKE_CARRY_MS, extra.get("decayMs").getAsDouble(), 0.0D);
        assertEquals(extra.get("ease").getAsString(), TabletAnimationModel.SKIP_WAKE_CARRY_EASE.previewName());
        for (JsonElement element : extra.getAsJsonArray("samples")) {
            JsonObject s = element.getAsJsonObject();
            String tier = s.get("tier").getAsString();
            double pLast = s.get("pLast").getAsDouble();
            Context c = TabletPose3DTest.context(tier, TabletScreenKind.SQUAD);
            near(s.get("carry"), TabletPose3D.breathAt(pLast), () -> tier + " carry at " + pLast);
            near(s.get("carryPx"), TabletPose3D.breathAt(pLast) * c.term().pxH(), () -> tier + " carry px");
            TabletPose3D.End rr = TabletPose3D.readRect(c.kRead());
            Pose read = TabletPose3D.poseFromRectPx(TabletPose3D.rectPx(c.kRead(), rr.cy(), c.term()),
                    c.term(), c.face());
            near(s.get("readZ"), read.z(), () -> tier + " READ z");
            near(s.get("dyBlocksAtRead"), -TabletPose3D.breathAt(pLast) * c.face().hf() * Math.abs(read.z())
                    / c.face().d(), () -> tier + " Δy at READ");
            double start = 1000.0D;
            TabletMotion m = closedInTheWakeBand(pLast, start);
            assertEquals(TabletMotion.State.CLOSING, m.state());
            assertEquals(0.61D, m.p(), 1e-12, "a wake-band close starts at 0.61");
            for (double[] d : TabletVectors.pairs(s.get("decay"))) {
                near(d[1], m.carry(start + d[0]), () -> tier + " p=" + pLast + " carry at +" + d[0] + " ms");
            }
            assertEquals(0.0D, m.carry(start + 100.0D), 0.0D);
        }
    }

    @Test
    void firstCloseFrameShowsTheLastOpenFrame() {
        for (String tier : new String[]{"320x240", "480x270", "640x360", "960x540", "960x720"}) {
            Context c = TabletPose3DTest.context(tier, TabletScreenKind.SQUAD);
            for (double pLast : new double[]{0.62D, 0.64D, 0.655D, 0.7D, 0.73D}) {
                TabletFrame last = TabletPose3D.frame(pLast, c, FrameQuery.opening(TabletHand.GUN));
                double now = 2000.0D;
                TabletMotion m = closedInTheWakeBand(pLast, now);
                TabletMotion.PoseContext pc = m.poseContext();
                FrameQuery q = new FrameQuery(false, pc.curve(), pc.capture(), pc.captureFromP(),
                        TabletHand.GUN, m.carry(now));
                TabletFrame first = TabletPose3D.frame(m.p(), c, q);
                String what = tier + " p_last=" + pLast;
                assertEquals(last.rect().cy(), first.rect().cy(), 1e-9, what + " plane cy");
                assertEquals(last.rect().s(), first.rect().s(), 1e-9, what + " plane s");
                assertEquals(last.planePx(), first.planePx(), what + " plane px");
                assertEquals(last.tablet().pose().y(), first.tablet().pose().y(), 1e-9, what + " 3D y");
                assertEquals(last.tablet().pose().z(), first.tablet().pose().z(), 1e-9, what + " 3D z");
                // the preview without the carry jumps by the whole sink
                FrameQuery noCarry = new FrameQuery(false, pc.curve(), pc.capture(), pc.captureFromP(),
                        TabletHand.GUN, 0.0D);
                double jump = Math.abs(TabletPose3D.frame(m.p(), c, noCarry).rect().cy() - last.rect().cy());
                assertEquals(TabletPose3D.breathAt(pLast), jump, 1e-12, what + " preview jump");
                // a frame into the raise segment: the 3D pose is moved by −carry·Hf·|z|/d
                double later = now + 50.0D;
                m.update(later);
                assertTrue(m.p() < 0.61D, what + " raise segment");
                double carry = m.carry(later);
                assertTrue(carry > 0.0D && carry < TabletPose3D.breathAt(pLast), what + " fading");
                TabletFrame moved = TabletPose3D.frame(m.p(), c, new FrameQuery(false, pc.curve(), false,
                        Double.NaN, TabletHand.GUN, carry));
                TabletFrame plain = TabletPose3D.frame(m.p(), c, new FrameQuery(false, pc.curve(), false,
                        Double.NaN, TabletHand.GUN, 0.0D));
                Pose a = moved.tablet().pose();
                Pose b = plain.tablet().pose();
                assertEquals(b.y() - carry * c.face().hf() * Math.abs(b.z()) / c.face().d(), a.y(), 1e-12, what);
                assertEquals(b.x(), a.x(), 0.0D);
                assertEquals(b.z(), a.z(), 0.0D);
            }
        }
    }

    @Test
    void nothingIsCarriedOutsideTheWakeBandOrOnTheCloseCurve() {
        TabletMotion m = new TabletMotion();
        m.open(0.0D);
        m.freeze(0.5D);
        m.update(10.0D);
        m.unfreeze(10.0D);
        m.close(20.0D);
        assertEquals(0.0D, m.carry(20.0D), 0.0D, "raise segment: nothing to carry");
        // a close from SHOWN reopened and closed again in the wake band keeps the close curve: no sink
        TabletMotion r = new TabletMotion();
        r.open(0.0D);
        for (double t = 0; t <= 1400; t += TabletVectors.DT) {
            r.update(t);
        }
        r.close(1500.0D);
        r.update(1700.0D);
        r.open(1700.0D);
        r.freeze(0.65D);
        r.update(1710.0D);
        r.unfreeze(1710.0D);
        r.close(1720.0D);
        assertEquals(TabletMotion.Curve.CLOSE, r.curve());
        assertEquals(0.0D, r.carry(1720.0D), 0.0D);
        // the carry ends with the animation
        TabletMotion done = closedInTheWakeBand(0.655D, 500.0D);
        done.reset(510.0D, true);
        assertEquals(0.0D, done.carry(510.0D), 0.0D);
    }
}
