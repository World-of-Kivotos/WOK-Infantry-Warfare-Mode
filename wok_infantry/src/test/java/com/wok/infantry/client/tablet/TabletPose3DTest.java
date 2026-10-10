package com.wok.infantry.client.tablet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wok.infantry.client.tablet.TabletPose3D.Context;
import com.wok.infantry.client.tablet.TabletPose3D.Face;
import com.wok.infantry.client.tablet.TabletPose3D.FrameQuery;
import com.wok.infantry.client.tablet.TabletPose3D.Pose;
import com.wok.infantry.client.tablet.TabletPose3D.RectPx;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static com.wok.infantry.client.tablet.TabletVectors.near;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scheme A ({@link TabletPose3D}) against the preview: per-tier geometry, the scalar curves, the
 * zoom curve, plane rectangles, raise poses and 976 full frames ({@code vectors.frames}), value by
 * value; then the DESIGN 6.6 properties (k_read, round trips, rotation 0 at READ, identity at 1,
 * no four arms, the occlusion rule, hands gone by 0.97).
 */
class TabletPose3DTest {
    private static final List<String> TIERS = List.of("320x240", "480x270", "640x360", "960x540", "960x720");

    static Context context(String tier, TabletScreenKind kind) {
        TabletUnits term = TabletVectors.termUnits(tier);
        TabletUnits units = kind.isTerminal() ? term : TabletVectors.mapUnits(tier);
        return Context.of(term, units, kind, TabletPose3D.P11_HAND, false);
    }

    // ---- tiers ----------------------------------------------------------------------------------

    @Test
    void tierGeometryMatchesThePreview() {
        for (String id : TIERS) {
            JsonObject t = TabletVectors.tier(id);
            Context term = context(id, TabletScreenKind.SQUAD);
            Context map = context(id, TabletScreenKind.FULLSCREEN);
            JsonObject face = t.getAsJsonObject("face");
            Face fc = term.face();
            near(face.get("aspect"), fc.aspect(), () -> id + " aspect");
            near(face.get("Wf"), fc.wf(), () -> id + " Wf");
            near(face.get("Hf"), fc.hf(), () -> id + " Hf");
            near(face.get("d"), fc.d(), () -> id + " d");
            near(face.get("widthU"), fc.widthU(), () -> id + " widthU");
            near(face.get("heightU"), fc.heightU(), () -> id + " heightU");
            JsonObject ri = t.getAsJsonObject("readInfo");
            near(ri.get("k"), term.kRead(), () -> id + " k_read");
            assertEquals(ri.get("sampling").getAsString().equals("smooth"), term.termSmooth(), id);
            assertEquals(t.get("termSampling").getAsString().equals("smooth"), term.readSmooth(), id);
            assertEquals(t.get("readSamplingMap").getAsString().equals("smooth"), map.readSmooth(), id);
            near(t.getAsJsonObject("endTerm").get("s"), term.end().s(), () -> id + " endTerm.s");
            near(t.getAsJsonObject("endTerm").get("cy"), term.end().cy(), () -> id + " endTerm.cy");
            near(t.getAsJsonObject("endMap").get("s"), map.end().s(), () -> id + " endMap.s");
            near(t.getAsJsonObject("endMap").get("cy"), map.end().cy(), () -> id + " endMap.cy");
            JsonObject zt = t.getAsJsonObject("zoomTerm");
            near(zt.get("o"), term.zoom().o(), () -> id + " zoomTerm.o");
            near(zt.get("peakAt"), term.zoom().peakAt(), () -> id + " zoomTerm.peakAt");
            near(zt.get("limit"), term.zoom().limit(), () -> id + " zoomTerm.limit");
            near(t.getAsJsonObject("zoomMap").get("o"), map.zoom().o(), () -> id + " zoomMap.o");
            near(t.getAsJsonObject("zoomMap").get("peakAt"), map.zoom().peakAt(), () -> id + " zoomMap.peakAt");
            TabletPose3D.End rr = TabletPose3D.readRect(term.kRead());
            near(t.getAsJsonObject("readRect").get("s"), rr.s(), () -> id + " readRect.s");
            near(t.getAsJsonObject("readRect").get("cy"), rr.cy(), () -> id + " readRect.cy");
            near(t.get("kf70"), TabletPose3D.kf(TabletPose3D.P11_HAND), () -> id + " kf70");
            near(t.get("kf80"), TabletPose3D.kf(t.get("p11At80").getAsDouble()), () -> id + " kf80");
            double[] inset = TabletPose3D.deviceInset(fc, term.geom());
            near(t.getAsJsonObject("deviceInset").get("x"), inset[0], () -> id + " inset.x");
            near(t.getAsJsonObject("deviceInset").get("y"), inset[1], () -> id + " inset.y");
            near(t.get("handsDx"), TabletPose3D.handsDx(fc), () -> id + " handsDx");
            near(t.get("handsDy"), TabletPose3D.handsDy(fc), () -> id + " handsDy");
            near(t.get("handsDz"), TabletPose3D.handsDz(fc), () -> id + " handsDz");
            JsonObject f3 = t.getAsJsonObject("face3D");
            TabletD2Geometry g = term.geom();
            double[] tl = TabletPose3D.planePoint(fc, g, g.e().left(), g.e().top());
            double[] br = TabletPose3D.planePoint(fc, g, g.e().right(), g.e().bottom());
            near(f3.get("x0"), tl[0], () -> id + " face3D.x0");
            near(f3.get("yTop"), tl[1], () -> id + " face3D.yTop");
            near(f3.get("x1"), br[0], () -> id + " face3D.x1");
            near(f3.get("yBottom"), br[1], () -> id + " face3D.yBottom");
            near(f3.get("thick"), TabletAnimationModel.D2_THICK_U * TabletAnimationModel.U, () -> id + " thick");
        }
    }

    @Test
    void kReadAndSamplingPerTier() {
        // DESIGN 3.1 / 9.1 item 22: 0.667 pixel, 0.5 pixel, 0.667 pixel, 0.55 smooth, 0.55 smooth
        double[] k = {2.0D / 3.0D, 0.5D, 2.0D / 3.0D, 0.55D, 0.55D};
        boolean[] smooth = {false, false, false, true, true};
        for (int i = 0; i < TIERS.size(); i++) {
            Context c = context(TIERS.get(i), TabletScreenKind.SQUAD);
            assertEquals(k[i], c.kRead(), 1e-12, TIERS.get(i));
            assertEquals(smooth[i], c.readSmooth(), TIERS.get(i));
            assertTrue(context(TIERS.get(i), TabletScreenKind.FULLSCREEN).readSmooth(),
                    TIERS.get(i) + " map always smooth");
        }
    }

    // ---- scalar curves ------------------------------------------------------------------------

    @Test
    void scalarCurvesMatchThePreview() {
        JsonObject curves = TabletVectors.object("curves");
        double[] ps = TabletVectors.doubles(curves.get("p"));
        double[] breath = TabletVectors.doubles(curves.get("breathAt"));
        double[] lift = TabletVectors.doubles(curves.get("gunLift"));
        double[] gun = TabletVectors.doubles(curves.get("gunProgress"));
        double[] arm = TabletVectors.doubles(curves.get("emptyArmProgress"));
        double[] exit = TabletVectors.doubles(curves.get("handsExit"));
        for (int i = 0; i < ps.length; i++) {
            double p = exact(ps[i]);
            near(breath[i], TabletPose3D.breathAt(p), () -> "breathAt(" + p + ")");
            near(lift[i], TabletPose3D.gunLift(p), () -> "gunLift(" + p + ")");
            near(gun[i], TabletPose3D.gunProgress(p, TabletAnimationModel.EASE_GUN), () -> "gunProgress(" + p + ")");
            near(arm[i], TabletPose3D.gunProgress(p, TabletAnimationModel.EASE_EMPTY_ARM), () -> "arm(" + p + ")");
            near(exit[i], TabletPose3D.handsExit(p), () -> "handsExit(" + p + ")");
        }
        for (JsonElement element : curves.getAsJsonArray("gunOps")) {
            JsonObject rec = element.getAsJsonObject();
            double p = exact(rec.get("p").getAsDouble());
            List<TabletOp> ops = TabletPose3D.gunOps(p, TabletAnimationModel.EASE_GUN);
            assertOps(rec.getAsJsonArray("ops"), ops, () -> "gunOps(" + p + ")");
            TabletVectors.matrix(rec.get("m"), TabletMat4.ops(ops, null), () -> "gunOps(" + p + ") matrix");
        }
    }

    /** The curve inputs were i / 200 or short decimals. */
    private static double exact(double p) {
        double grid = Math.round(p * 200.0D) / 200.0D;
        return Math.abs(grid - p) < 1e-11 ? grid : p;
    }

    static void assertOps(JsonArray expected, List<TabletOp> actual, Supplier<String> what) {
        assertEquals(expected.size(), actual.size(), what);
        for (int i = 0; i < expected.size(); i++) {
            JsonArray e = expected.get(i).getAsJsonArray();
            TabletOp op = actual.get(i);
            String kind = e.get(0).getAsString();
            int index = i;
            Supplier<String> at = () -> what.get() + " op " + index + " " + kind;
            switch (kind) {
                case "translate" -> assertEquals(TabletOp.Kind.TRANSLATE, op.kind(), at);
                case "scale" -> assertEquals(TabletOp.Kind.SCALE, op.kind(), at);
                case "rotX" -> assertEquals(TabletOp.Kind.ROT_X, op.kind(), at);
                case "rotY" -> assertEquals(TabletOp.Kind.ROT_Y, op.kind(), at);
                case "rotZ" -> assertEquals(TabletOp.Kind.ROT_Z, op.kind(), at);
                case "identity" -> assertEquals(TabletOp.Kind.IDENTITY, op.kind(), at);
                default -> throw new AssertionError(kind);
            }
            if (e.size() > 1) {
                near(e.get(1), op.a(), at);
            }
            if (e.size() > 2) {
                near(e.get(2), op.b(), at);
                near(e.get(3), op.c(), at);
            }
        }
    }

    @Test
    void zoomCurveMatchesThePreview() {
        for (JsonElement element : TabletVectors.array("zoomZ")) {
            JsonObject rec = element.getAsJsonObject();
            String id = rec.get("tier").getAsString() + " " + rec.get("screen").getAsString();
            double ovz = rec.get("ovz").getAsDouble();
            double peak = rec.get("peakAt").getAsDouble();
            for (double[] s : TabletVectors.pairs(rec.get("samples"))) {
                double x = Math.round(s[0] * 40.0D) / 40.0D;
                near(s[1], TabletPose3D.zoomZ(x, ovz, peak), () -> id + " zoomZ(" + x + ")");
            }
        }
    }

    @Test
    void planeRectanglesMatchThePreview() {
        for (JsonElement element : TabletVectors.array("rectAt")) {
            JsonObject rec = element.getAsJsonObject();
            String tier = rec.get("tier").getAsString();
            TabletScreenKind kind = TabletVectors.screen(rec.get("screen").getAsString());
            boolean close = rec.get("curve").getAsString().equals("close");
            Context c = context(tier, kind);
            JsonArray samples = rec.getAsJsonArray("samples");
            for (JsonElement se : samples) {
                double[] s = TabletVectors.doubles(se);
                TabletPose3D.Rect r = TabletPose3D.rectAt(s[0], c.kRead(), c.end(), close, c.zoom());
                String what = tier + " " + kind + " " + (close ? "close" : "open") + " p=" + s[0];
                near(s[1], r.s(), () -> what + " s");
                near(s[2], r.cy(), () -> what + " cy");
                near(s[3], r.zoom(), () -> what + " zoom");
                near(s[4], r.sway(), () -> what + " sway");
            }
        }
    }

    @Test
    void raisePosesMatchThePreview() {
        for (JsonElement element : TabletVectors.array("raisePoses")) {
            JsonObject rec = element.getAsJsonObject();
            String tier = rec.get("tier").getAsString();
            boolean close = rec.get("curve").getAsString().equals("close");
            Context c = context(tier, TabletScreenKind.SQUAD);
            TabletPose3D.End rr = TabletPose3D.readRect(c.kRead());
            Pose read = TabletPose3D.poseFromRectPx(TabletPose3D.rectPx(c.kRead(), rr.cy(), c.term()),
                    c.term(), c.face());
            JsonObject rp = rec.getAsJsonObject("readPose");
            near(rp.get("x"), read.x(), () -> tier + " READ x");
            near(rp.get("y"), read.y(), () -> tier + " READ y");
            near(rp.get("z"), read.z(), () -> tier + " READ z");
            List<Pose> keys = TabletPose3D.keyPoses(c.face(), c.kRead(), read);
            JsonArray ek = rec.getAsJsonArray("keys");
            for (int i = 0; i < ek.size(); i++) {
                JsonObject e = ek.get(i).getAsJsonObject();
                Pose k = keys.get(i);
                assertEquals(e.get("id").getAsString(), k.id());
                assertPose(e, k, () -> tier + " key " + k.id());
            }
            for (JsonElement se : rec.getAsJsonArray("samples")) {
                double[] s = TabletVectors.doubles(se);
                Pose q = TabletPose3D.poseAt(s[0], c.face(), c.kRead(), c.term(), close, c.end(), c.zoom(), 0.0D);
                String what = tier + " " + (close ? "close" : "open") + " p=" + s[0];
                near(s[1], q.x(), () -> what + " x");
                near(s[2], q.y(), () -> what + " y");
                near(s[3], q.z(), () -> what + " z");
                near(s[4], q.yaw(), () -> what + " yaw");
                near(s[5], q.pitch(), () -> what + " pitch");
                near(s[6], q.roll(), () -> what + " roll");
            }
        }
    }

    private static void assertPose(JsonObject e, Pose q, Supplier<String> what) {
        near(e.get("x"), q.x(), () -> what.get() + " x");
        near(e.get("y"), q.y(), () -> what.get() + " y");
        near(e.get("z"), q.z(), () -> what.get() + " z");
        near(e.get("yaw"), q.yaw(), () -> what.get() + " yaw");
        near(e.get("pitch"), q.pitch(), () -> what.get() + " pitch");
        near(e.get("roll"), q.roll(), () -> what.get() + " roll");
    }

    // ---- full frames --------------------------------------------------------------------------

    static FrameQuery query(JsonObject ctx, TabletHand hand) {
        boolean open = !ctx.get("dir").getAsString().equals("close");
        TabletMotion.Curve curve = ctx.has("curve") && ctx.get("curve").getAsString().equals("close")
                ? TabletMotion.Curve.CLOSE : TabletMotion.Curve.OPEN;
        boolean capture = !ctx.has("capture") || ctx.get("capture").getAsBoolean();
        double from = ctx.has("captureFromP") ? ctx.get("captureFromP").getAsDouble() : 1.0D;
        return new FrameQuery(open, curve, capture, from, hand, 0.0D);
    }

    @Test
    void framesMatchThePreview() {
        int count = 0;
        for (JsonElement element : TabletVectors.array("frames")) {
            JsonObject group = element.getAsJsonObject();
            String tier = group.get("tier").getAsString();
            TabletScreenKind kind = TabletVectors.screen(group.get("screen").getAsString());
            TabletHand hand = TabletHand.byPreviewName(group.get("hand").getAsString());
            Context c = context(tier, kind);
            FrameQuery q = query(group.getAsJsonObject("ctx"), hand);
            for (JsonElement fe : group.getAsJsonArray("frames")) {
                JsonObject e = fe.getAsJsonObject();
                double p = e.get("p").getAsDouble();
                String what = tier + " " + kind + " " + hand + " " + group.get("variant").getAsString() + " p=" + p;
                assertFrame(e, TabletPose3D.frame(p, c, q), what);
                count++;
            }
        }
        assertEquals(976, count, "frames compared");
    }

    private static void assertFrame(JsonObject e, TabletFrame f, String what) {
        near(e.get("p"), f.p(), () -> what + " p");
        assertEquals(e.get("phase").getAsString(), f.phase(), what + " phase");
        JsonObject pose = e.getAsJsonObject("pose");
        assertPose(pose, f.tablet().pose(), () -> what + " pose");
        assertEquals(pose.get("id").getAsString(), f.tablet().pose().id(), what + " pose id");
        TabletVectors.matrix(e.get("tabletM"), f.tablet().matrix(), () -> what + " tabletM");
        if (TabletVectors.isNull(e.get("rect"))) {
            assertNull(f.rect(), what + " rect");
        } else {
            JsonObject rect = e.getAsJsonObject("rect");
            assertNotNull(f.rect(), what + " rect");
            near(rect.get("s"), f.rect().s(), () -> what + " rect.s");
            near(rect.get("cy"), f.rect().cy(), () -> what + " rect.cy");
            near(rect.get("zoom"), f.rect().zoom(), () -> what + " rect.zoom");
        }
        TabletVectors.rectPx(e.get("planePx"), f.planePx(), () -> what + " planePx");
        TabletVectors.rectPx(e.get("rectPx"), f.rectPx(), () -> what + " rectPx");
        TabletVectors.rectPx(e.get("devicePx"), f.devicePx(), () -> what + " devicePx");
        TabletVectors.rectPx(e.get("glassPx"), f.glassPx(), () -> what + " glassPx");
        JsonObject tablet = e.getAsJsonObject("tablet");
        assertEquals(tablet.get("draw").getAsBoolean(), f.tablet().draw(), what + " tablet.draw");
        assertEquals(tablet.get("inWindow").getAsBoolean(), f.tablet().inWindow(), what + " inWindow");
        assertEquals(tablet.get("covered").getAsBoolean(), f.tablet().covered(), what + " covered");
        JsonObject hands = e.getAsJsonObject("hands");
        assertEquals(hands.get("draw").getAsBoolean(), f.hands().draw(), what + " hands.draw");
        near(hands.get("exit"), f.hands().exit(), () -> what + " hands.exit");
        TabletVectors.matrix(hands.get("rightPre"), f.hands().rightPre(), () -> what + " rightPre");
        TabletVectors.matrix(hands.get("leftPre"), f.hands().leftPre(), () -> what + " leftPre");
        JsonObject gun = e.getAsJsonObject("gun");
        assertEquals(gun.get("draw").getAsBoolean(), f.gun().draw(), what + " gun.draw");
        near(gun.get("e"), f.gun().e(), () -> what + " gun.e");
        JsonObject arm = e.getAsJsonObject("emptyArm");
        assertEquals(arm.get("draw").getAsBoolean(), f.emptyArm().draw(), what + " emptyArm.draw");
        near(arm.get("e"), f.emptyArm().e(), () -> what + " emptyArm.e");
        JsonObject fx = e.getAsJsonObject("fx");
        JsonObject wake = fx.getAsJsonObject("wake");
        assertEquals(wake.get("on").getAsBoolean(), f.fx().wake().on(), what + " wake.on");
        near(wake.get("open"), f.fx().wake().open(), () -> what + " wake.open");
        near(wake.get("bandFrac"), f.fx().wake().bandFrac(), () -> what + " wake.bandFrac");
        JsonObject mask = fx.getAsJsonObject("mask");
        assertEquals(mask.get("on").getAsBoolean(), f.fx().mask().on(), what + " mask.on");
        near(mask.get("alpha"), f.fx().mask().alpha(), () -> what + " mask.alpha");
        JsonObject backdrop = fx.getAsJsonObject("backdrop");
        assertEquals(backdrop.get("on").getAsBoolean(), f.fx().backdrop().on(), what + " backdrop.on");
        near(backdrop.get("alpha"), f.fx().backdrop().alpha(), () -> what + " backdrop.alpha");
        near(fx.get("shadow"), f.fx().shadowAlpha(), () -> what + " shadow");
        JsonObject sleep = fx.getAsJsonObject("sleep");
        assertEquals(sleep.get("on").getAsBoolean(), f.fx().sleep().on(), what + " sleep.on");
        near(sleep.get("widthFrac"), f.fx().sleep().widthFrac(), () -> what + " sleep.widthFrac");
        near(sleep.get("px"), f.fx().sleep().px(), () -> what + " sleep.px");
        near(sleep.get("heightBlocks"), f.fx().sleep().heightBlocks(), () -> what + " sleep.heightBlocks");
        JsonObject ui = e.getAsJsonObject("ui");
        assertEquals(ui.get("draw").getAsBoolean(), f.ui().draw(), what + " ui.draw");
        assertEquals(ui.get("identity").getAsBoolean(), f.ui().identity(), what + " ui.identity");
        assertEquals(ui.get("smooth").getAsBoolean(), f.ui().smooth(), what + " ui.smooth");
        JsonObject cap = e.getAsJsonObject("capture");
        assertEquals(cap.get("draw").getAsBoolean(), f.capture().draw(), what + " capture.draw");
        assertEquals(cap.get("smooth").getAsBoolean(), f.capture().smooth(), what + " capture.smooth");
        near(TabletVectors.num(cap, "fromP"), f.capture().fromP(), TabletVectors.DOUBLE, () -> what + " fromP");
        TabletVectors.rectPx(cap.get("fromRectPx"), f.capture().fromRectPx(), () -> what + " fromRectPx");
        if (!TabletVectors.isNull(cap.get("fromSmooth"))) {
            assertEquals(cap.get("fromSmooth").getAsBoolean(), f.capture().fromSmooth(), what + " fromSmooth");
        }
        JsonObject hud = e.getAsJsonObject("hud");
        assertEquals(hud.get("hidden").getAsBoolean(), f.hud().hidden(), what + " hud.hidden");
        assertEquals(hud.get("crosshairHidden").getAsBoolean(), f.hud().crosshairHidden(), what + " crosshair");
        if (TabletVectors.isNull(e.get("deviceQuadPx"))) {
            assertNull(f.quadPx(), what + " quad");
        } else {
            double[][] quad = TabletVectors.pairs(e.get("deviceQuadPx"));
            assertNotNull(f.quadPx(), what + " quad");
            for (int i = 0; i < 4; i++) {
                int index = i;
                near(quad[i][0], f.quadPx()[i][0], TabletVectors.PX, () -> what + " quad " + index + ".x");
                near(quad[i][1], f.quadPx()[i][1], TabletVectors.PX, () -> what + " quad " + index + ".y");
            }
        }
    }

    @Test
    void contentVisibilityMatchesThePreview() {
        for (JsonElement element : TabletVectors.array("contentVisible")) {
            JsonObject rec = element.getAsJsonObject();
            String tier = rec.get("tier").getAsString();
            double p = rec.get("p").getAsDouble();
            TabletFrame f = TabletPose3D.frame(p, context(tier, TabletScreenKind.SQUAD),
                    FrameQuery.opening(TabletHand.GUN));
            TabletVectors.rectPx(rec.get("glassPx"), f.glassPx(), () -> tier + " p=" + p + " glassPx");
            for (JsonElement pe : rec.getAsJsonArray("points")) {
                JsonArray pt = pe.getAsJsonArray();
                double x = pt.get(0).getAsDouble();
                double y = pt.get(1).getAsDouble();
                assertEquals(pt.get(2).getAsBoolean(), TabletPose3D.contentVisible(f, x, y),
                        tier + " p=" + p + " (" + x + ", " + y + ")");
            }
        }
    }

    // ---- DESIGN 6.6 properties ----------------------------------------------------------------

    @Test
    void rectangleAndPoseRoundTrip() {
        for (String id : TIERS) {
            Face fc = context(id, TabletScreenKind.SQUAD).face();
            for (double s : new double[]{0.5D, 0.55D, 2.0D / 3.0D, 0.9D, 1.0D, 1.15D}) {
                for (double cy : new double[]{0.4D, 0.5D, 0.645D}) {
                    Pose pose = TabletPose3D.poseFromRect(s, cy, fc);
                    double[] back = TabletPose3D.rectFromPose(pose, fc);
                    assertEquals(s, back[0], 1e-12, id);
                    assertEquals(0.5D, back[1], 1e-12, id);
                    assertEquals(cy, back[2], 1e-12, id);
                }
            }
        }
    }

    @Test
    void readHasNoRotationAndTheEndIsTheIdentity() {
        for (String id : TIERS) {
            for (TabletScreenKind kind : new TabletScreenKind[]{TabletScreenKind.SQUAD, TabletScreenKind.FULLSCREEN}) {
                Context c = context(id, kind);
                for (TabletMotion.Curve curve : TabletMotion.Curve.values()) {
                    FrameQuery q = new FrameQuery(curve == TabletMotion.Curve.OPEN, curve, true, 1.0D,
                            TabletHand.GUN, 0.0D);
                    Pose read = TabletPose3D.frame(0.61D, c, q).tablet().pose();
                    assertEquals(0.0D, read.yaw(), 0.0D, id + " " + curve);
                    assertEquals(0.0D, read.pitch(), 0.0D, id + " " + curve);
                    assertEquals(0.0D, read.roll(), 0.0D, id + " " + curve);
                }
                TabletFrame end = TabletPose3D.frame(1.0D, c, FrameQuery.opening(TabletHand.GUN));
                assertTrue(end.ui().identity(), id + " identity at p = 1");
                assertFalse(end.ui().smooth(), id + " no offscreen target at p = 1");
                assertFalse(end.tablet().draw(), id + " no 3D at p = 1");
                assertEquals(new RectPx(0, 0, c.units().pxW(), c.units().pxH(), 1.0D), end.rectPx(), id);
            }
        }
    }

    @Test
    void handsAreMirroredLeftToRight() {
        Context c = context("480x270", TabletScreenKind.SQUAD);
        List<TabletOp> right = TabletPose3D.handLocalOps(true, c.face(), 0.65D, c.geom());
        List<TabletOp> left = TabletPose3D.handLocalOps(false, c.face(), 0.65D, c.geom());
        assertEquals(-right.get(0).a(), left.get(0).a(), 1e-15, "x mirrored");
        assertEquals(right.get(0).b(), left.get(0).b(), 1e-15);
        assertEquals(180.0D - right.get(2).a(), left.get(2).a(), 1e-12, "chain yaw 92° / 88°");
        assertEquals(6, TabletPose3D.handPreOps(true, c.face(), 0.65D, c.geom()).size());
    }

    // fist samples (model pixels) of the arm cuboid, as the preview's fistSamples
    private static double[][] fistSamples(boolean right, int n, boolean whole, boolean sleeve) {
        double[] b = TabletAnimationModel.armBox(right);
        double s = sleeve ? 0.0D : TabletAnimationModel.HANDS_SLEEVE;
        double x0 = b[0] + s;
        double y0 = whole ? b[1] + s : TabletAnimationModel.HANDS_FIST_FROM_Y;
        double z0 = b[2] + s;
        double x1 = b[3] - s;
        double y1 = b[4] - s;
        double z1 = b[5] - s;
        double[][] out = new double[(n + 1) * (n + 1) * (n + 1)][];
        int k = 0;
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                for (int q = 0; q <= n; q++) {
                    out[k++] = new double[]{x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * j / n,
                            z0 + (z1 - z0) * q / n};
                }
            }
        }
        return out;
    }

    /** Fist / arm sample points of both hands on screen (preview fistStats.visible / armVisible). */
    private static int visibleSamples(TabletFrame f, boolean whole, int n) {
        int visible = 0;
        for (boolean right : new boolean[]{true, false}) {
            double[] m = right ? f.hands().rightMatrix() : f.hands().leftMatrix();
            for (double[] q : fistSamples(right, n, whole, true)) {
                double[] v = TabletMat4.transformPoint(m, q[0], q[1], q[2]);
                if (-v[2] < 0.05D) {
                    continue;
                }
                double[] uv = TabletPose3D.projectFrac(v, f.ctx().units().aspect(), f.ctx().p11());
                if (uv[0] > 0 && uv[0] < 1 && uv[1] > 0 && uv[1] < 1) {
                    visible++;
                }
            }
        }
        return visible;
    }

    @Test
    void neverFourArms() {
        // While the gun is still in the picture (press-down e < 0.45) or the vanilla empty arm is,
        // the tablet's fists are not visible.
        double[] box = TabletAnimationModel.armBox(true);
        for (String id : TIERS) {
            for (TabletScreenKind kind : new TabletScreenKind[]{TabletScreenKind.SQUAD, TabletScreenKind.FULLSCREEN}) {
                Context c = context(id, kind);
                for (TabletHand hand : new TabletHand[]{TabletHand.GUN, TabletHand.EMPTY}) {
                    for (double p = 0.1725D; p < 0.30D; p += 0.0025D) {
                        TabletFrame f = TabletPose3D.frame(p, c, FrameQuery.opening(hand));
                        boolean firstPersonVisible;
                        if (hand == TabletHand.GUN) {
                            firstPersonVisible = f.gun().e() < 0.45D;
                        } else {
                            firstPersonVisible = false;
                            for (double x : new double[]{box[0], box[3]}) {
                                for (double y : new double[]{box[1], box[4]}) {
                                    for (double z : new double[]{box[2], box[5]}) {
                                        double[] v = TabletMat4.transformPoint(f.emptyArm().matrix(), x, y, z);
                                        if (-v[2] <= 0.05D) {
                                            continue;
                                        }
                                        double[] uv = TabletPose3D.projectFrac(v, c.units().aspect(), c.p11());
                                        firstPersonVisible |= uv[0] > 0 && uv[0] < 1 && uv[1] > 0 && uv[1] < 1;
                                    }
                                }
                            }
                        }
                        if (!firstPersonVisible || !f.hands().draw()) {
                            continue;
                        }
                        double at = p;
                        assertEquals(0, visibleSamples(f, false, 6),
                                () -> id + " " + kind + " " + hand + " p=" + at + ": fists visible with the gun / arm");
                    }
                }
            }
        }
    }

    @Test
    void fistsInsideTheDeviceAreBehindTheBackPlate() {
        double limit = -TabletAnimationModel.D2_THICK_U * TabletAnimationModel.U - TabletAnimationModel.HANDS_BEHIND_PLATE;
        int inside = 0;
        for (String id : TIERS) {
            for (TabletScreenKind kind : new TabletScreenKind[]{TabletScreenKind.SQUAD, TabletScreenKind.FULLSCREEN}) {
                Context c = context(id, kind);
                for (double p = 0.61D; p < 1.0D; p += 0.01D) {
                    TabletFrame f = TabletPose3D.frame(p, c, FrameQuery.opening(TabletHand.GUN));
                    if (!f.hands().draw()) {
                        continue;
                    }
                    RectPx e = TabletPose3D.subRectPx(f.planePx(), c.geom(), c.geom().e());
                    for (boolean right : new boolean[]{true, false}) {
                        double[] local = TabletMat4.ops(right ? f.hands().rightOps() : f.hands().leftOps(), null);
                        for (double[] q : fistSamples(right, 4, true, true)) {
                            double[] lp = TabletMat4.transformPoint(local, q[0], q[1], q[2]);
                            double[] vp = TabletMat4.transformPoint(f.tablet().matrix(), lp[0], lp[1], lp[2]);
                            if (-vp[2] < 0.05D) {
                                continue;
                            }
                            double[] uv = TabletPose3D.projectFrac(vp, c.units().aspect(), c.p11());
                            double x = uv[0] * c.units().pxW();
                            double y = uv[1] * c.units().pxH();
                            if (x <= e.x() || x >= e.x() + e.w() || y <= e.y() || y >= e.y() + e.h()) {
                                continue;
                            }
                            inside++;
                            double at = p;
                            assertTrue(lp[2] < limit, () -> id + " " + kind + " p=" + at + " fist in front of the plate z=" + lp[2]);
                        }
                    }
                }
            }
        }
        assertTrue(inside > 0, "some fist samples are behind the device");
    }

    @Test
    void handsHaveLeftThePictureBy097() {
        for (String id : TIERS) {
            for (TabletScreenKind kind : new TabletScreenKind[]{TabletScreenKind.SQUAD, TabletScreenKind.FULLSCREEN}) {
                Context c = context(id, kind);
                for (double p : new double[]{0.97D, 0.985D, 0.999D}) {
                    TabletFrame f = TabletPose3D.frame(p, c, FrameQuery.opening(TabletHand.GUN));
                    assertEquals(0, visibleSamples(f, false, 6), id + " " + kind + " p=" + p + " fists");
                    assertEquals(0, visibleSamples(f, true, 6), id + " " + kind + " p=" + p + " arms");
                }
                assertTrue(visibleSamples(TabletPose3D.frame(TabletAnimationModel.KEYS.get(1).p(), c,
                        FrameQuery.opening(TabletHand.GUN)), false, 6) > 0, id + " hands in the picture at K1");
            }
        }
    }

    @Test
    void zoomOvershootPeaksOnceAndTheCloseCurveNeverOvershoots() {
        for (String id : TIERS) {
            Context c = context(id, TabletScreenKind.SQUAD);
            double peak = 0.0D;
            double previous = 0.0D;
            boolean falling = false;
            for (double p = 0.74D; p <= 1.0D + 1e-12; p += 0.002D) {
                double s = TabletPose3D.rectAt(Math.min(1.0D, p), c.kRead(), c.end(), false, c.zoom()).s();
                if (s < previous - 1e-12) {
                    falling = true;
                } else if (falling) {
                    assertTrue(s <= previous + 1e-12, id + " one peak only");
                }
                peak = Math.max(peak, s);
                previous = s;
                double closeS = TabletPose3D.rectAt(Math.min(1.0D, p), c.kRead(), c.end(), true, c.zoom()).s();
                assertTrue(closeS <= c.end().s() + 1e-12, id + " close curve stays below the end");
            }
            assertEquals(c.end().s() * (1.0D + c.zoom().o()), peak, 2e-4, id + " peak = end × (1 + o)");
            assertEquals(c.end().s(), TabletPose3D.rectAt(1.0D, c.kRead(), c.end(), false, c.zoom()).s(), 0.0D);
        }
    }

    @Test
    void breathSinksOnlyOnTheOpenCurveAndIsZeroAtBothEnds() {
        assertEquals(0.0D, TabletPose3D.breathAt(0.61D), 0.0D);
        assertEquals(0.0D, TabletPose3D.breathAt(0.74D), 0.0D);
        assertEquals(TabletAnimationModel.BREATH_AMP, TabletPose3D.breathAt(0.655D), 1e-15);
        Context c = context("480x270", TabletScreenKind.SQUAD);
        assertEquals(0.0D, TabletPose3D.rectAt(0.655D, c.kRead(), c.end(), true, c.zoom()).sway(), 0.0D);
        assertTrue(TabletPose3D.rectAt(0.655D, c.kRead(), c.end(), false, c.zoom()).sway() > 0.0D);
        assertEquals(0.0D, TabletPose3D.gunLift(0.0D), 0.0D);
        assertEquals(0.0D, TabletPose3D.gunLift(TabletAnimationModel.GUN_LIFT_TO_P), 0.0D);
    }
}
