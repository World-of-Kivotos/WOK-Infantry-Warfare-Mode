package com.wok.infantry.client.tablet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.tablet.TabletPose3D.Context;
import com.wok.infantry.client.tablet.TabletPose3D.FrameQuery;
import com.wok.infantry.client.tablet.TabletPose3D.RectPx;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the game draws is what the preview computed (IMPL_PLAN 4.3 {@code TabletRenderMathTest}):
 * the tablet and hand matrices replayed on a real {@link PoseStack} ({@link TabletPoseOps}, the
 * renderer's path) match {@code vectors.frames} within float precision; the front face's D corners
 * through that matrix and the 70° hand projection land on {@code deviceQuadPx} (≤ 0.5 px); the
 * whole-pixel transform of the 2D layer ({@link TabletReadScope#pushPixel}) puts the device on the
 * frame's {@code devicePx}; and the arms the renderer draws (its stack plus the model part) keep
 * the occlusion rule: whatever of a fist falls into E is behind the back plate.
 */
class TabletRenderPathTest {
    /** Float matrices (meta.tolerance.floatMatrix), relative above magnitude 1. */
    private static final double FLOAT = 2e-5;

    private static void nearMatrix(double[] expected, Matrix4f actual, Supplier<String> what) {
        float[] a = new float[16];
        actual.get(a);
        for (int i = 0; i < 16; i++) {
            double tol = FLOAT * Math.max(1.0D, Math.abs(expected[i]));
            int index = i;
            assertTrue(Math.abs(expected[i] - a[i]) <= tol, () -> what.get() + "[" + index + "]: "
                    + expected[index] + " vs " + a[index]);
        }
    }

    private static Context context(String tier, TabletScreenKind kind) {
        return TabletPose3DTest.context(tier, kind);
    }

    @Test
    void tabletAndHandMatricesOnAPoseStackMatchThePreview() {
        int frames = 0;
        for (JsonElement element : TabletVectors.array("frames")) {
            JsonObject group = element.getAsJsonObject();
            String tier = group.get("tier").getAsString();
            TabletScreenKind kind = TabletVectors.screen(group.get("screen").getAsString());
            TabletHand hand = TabletHand.byPreviewName(group.get("hand").getAsString());
            Context c = context(tier, kind);
            FrameQuery q = TabletPose3DTest.query(group.getAsJsonObject("ctx"), hand);
            for (JsonElement fe : group.getAsJsonArray("frames")) {
                JsonObject e = fe.getAsJsonObject();
                double p = e.get("p").getAsDouble();
                String what = tier + " " + kind + " " + group.get("variant").getAsString() + " p=" + p;
                TabletFrame f = TabletPose3D.frame(p, c, q);
                PoseStack pose = new PoseStack();
                // The hand pass's stack already holds view bobbing and sway: the ops start from identity.
                pose.mulPose(Axis.XP.rotationDegrees(3.0F));
                pose.translate(0.02D, -0.01D, 0.03D);
                TabletPoseOps.apply(pose, TabletPose3D.tabletOps(f.tablet().pose(), c.kf()));
                nearMatrix(TabletVectors.doubles(e.get("tabletM")), pose.last().pose(),
                        () -> what + " tabletM");
                for (boolean right : new boolean[]{true, false}) {
                    pose.pushPose();
                    TabletPoseOps.apply(pose, TabletPose3D.handPreOps(right, c.face(), p, c.geom()));
                    JsonObject hands = e.getAsJsonObject("hands");
                    nearMatrix(TabletVectors.doubles(hands.get(right ? "rightPre" : "leftPre")),
                            pose.last().pose(), () -> what + (right ? " rightPre" : " leftPre"));
                    pose.popPose();
                }
                frames++;
            }
        }
        assertEquals(976, frames);
    }

    @Test
    void normalMatrixStaysTheRotationOfThePose() {
        Context c = context("640x360", TabletScreenKind.SQUAD);
        for (double p = 0.18D; p < 0.61D; p += 0.04D) {
            TabletFrame f = TabletPose3D.frame(p, c, FrameQuery.opening(TabletHand.GUN));
            PoseStack pose = new PoseStack();
            TabletPoseOps.apply(pose, TabletPose3D.tabletOps(f.tablet().pose(), c.kf()));
            Matrix3f n = pose.last().normal();
            Matrix3f r = new Matrix3f(pose.last().pose());
            // kf = 1 at 70°: a pure rotation, whose normal matrix is itself.
            for (int col = 0; col < 3; col++) {
                for (int row = 0; row < 3; row++) {
                    assertEquals(r.get(col, row), n.get(col, row), 1e-5, "p=" + p);
                }
            }
        }
    }

    @Test
    void frontFaceCornersProjectOntoTheDeviceQuad() {
        int checked = 0;
        for (JsonElement element : TabletVectors.array("frames")) {
            JsonObject group = element.getAsJsonObject();
            String tier = group.get("tier").getAsString();
            TabletScreenKind kind = TabletVectors.screen(group.get("screen").getAsString());
            TabletHand hand = TabletHand.byPreviewName(group.get("hand").getAsString());
            Context c = context(tier, kind);
            FrameQuery q = TabletPose3DTest.query(group.getAsJsonObject("ctx"), hand);
            Matrix4f projection = new Matrix4f().setPerspective((float) Math.toRadians(70.0D),
                    (float) c.units().aspect(), 0.05F, 100.0F);
            for (JsonElement fe : group.getAsJsonArray("frames")) {
                JsonObject e = fe.getAsJsonObject();
                if (TabletVectors.isNull(e.get("deviceQuadPx"))) {
                    continue;
                }
                double p = e.get("p").getAsDouble();
                TabletFrame f = TabletPose3D.frame(p, c, q);
                PoseStack pose = new PoseStack();
                TabletPoseOps.apply(pose, TabletPose3D.tabletOps(f.tablet().pose(), c.kf()));
                double[][] quad = TabletVectors.pairs(e.get("deviceQuadPx"));
                UiRect d = c.geom().d();
                int[][] corners = {{d.left(), d.top()}, {d.right(), d.top()}, {d.right(), d.bottom()},
                        {d.left(), d.bottom()}};
                for (int i = 0; i < 4; i++) {
                    Vector4f v = new Vector4f(
                            (float) TabletDeviceModel.planeX(c.face(), c.geom().w(), corners[i][0]),
                            (float) TabletDeviceModel.planeY(c.face(), c.geom().h(), corners[i][1]),
                            0.0F, 1.0F);
                    pose.last().pose().transform(v);
                    projection.transform(v);
                    double x = (v.x() / v.w() * 0.5D + 0.5D) * c.units().pxW();
                    double y = (0.5D - v.y() / v.w() * 0.5D) * c.units().pxH();
                    int index = i;
                    assertTrue(Math.abs(x - quad[i][0]) <= 0.5D && Math.abs(y - quad[i][1]) <= 0.5D,
                            () -> tier + " " + kind + " p=" + p + " corner " + index + " at (" + x
                                    + ", " + y + ") vs (" + quad[index][0] + ", " + quad[index][1] + ")");
                }
                checked++;
            }
        }
        assertTrue(checked > 100, "corners checked: " + checked);
    }

    @Test
    void pixelTransformPutsTheDeviceOnItsFrameRectangle() {
        for (String tier : List.of("320x240", "480x270", "640x360", "960x540", "960x720")) {
            Context c = context(tier, TabletScreenKind.SQUAD);
            TabletUnits u = c.term();
            for (double p = 0.61D; p < 1.0D; p += 0.03D) {
                TabletFrame f = TabletPose3D.frame(p, c, FrameQuery.opening(TabletHand.GUN));
                assertNotNull(f.ui().rectPx(), tier + " p=" + p);
                // The screen's own pose: GUI units, then the layout factor.
                PoseStack pose = new PoseStack();
                pose.scale(u.factor(), u.factor(), 1.0F);
                TabletReadScope.pushPixel(pose, f.ui().rectPx(), u.guiScale(), u.factor());
                UiRect d = c.geom().d();
                Vector3f a = pose.last().pose().transformPosition(new Vector3f(d.left(), d.top(), 0));
                Vector3f b = pose.last().pose().transformPosition(new Vector3f(d.right(), d.bottom(), 0));
                RectPx dev = f.devicePx();
                double s = u.guiScale();
                String what = tier + " p=" + p;
                assertEquals(dev.x(), a.x() * s, 2e-3, what + " left");
                assertEquals(dev.y(), a.y() * s, 2e-3, what + " top");
                assertEquals(dev.x() + dev.w(), b.x() * s, 2e-3, what + " right");
                assertEquals(dev.y() + dev.h(), b.y() * s, 2e-3, what + " bottom");
            }
        }
    }

    @Test
    void readPositionIsAWholePixelRatioOnThePixelTiers() {
        for (String tier : List.of("320x240", "480x270", "640x360")) {
            Context c = context(tier, TabletScreenKind.SQUAD);
            TabletFrame f = TabletPose3D.frame(0.61D, c, FrameQuery.opening(TabletHand.GUN));
            double perLayout = f.ui().rectPx().scale() * c.term().sf();
            assertEquals(Math.rint(perLayout), perLayout, 1e-9, tier + " physical px per layout px");
            assertTrue(perLayout >= 2.0D - 1e-9, tier + " at least 2 px per layout px");
            assertEquals(Math.rint(f.ui().rectPx().x()), f.ui().rectPx().x(), 0.0D, tier + " x snapped");
            assertEquals(Math.rint(f.ui().rectPx().y()), f.ui().rectPx().y(), 0.0D, tier + " y snapped");
            assertTrue(!f.ui().smooth(), tier + " pixel tier");
        }
        for (String tier : List.of("960x540", "960x720")) {
            Context c = context(tier, TabletScreenKind.SQUAD);
            assertTrue(TabletPose3D.frame(0.75D, c, FrameQuery.opening(TabletHand.GUN)).ui().smooth(),
                    tier + " smooth tier");
        }
    }

    /** Fist samples of the arm cuboid (model pixels), with the 0.25 px sleeve. */
    private static double[][] fist(boolean right) {
        double[] b = TabletAnimationModel.armBox(right);
        double y0 = TabletAnimationModel.HANDS_FIST_FROM_Y;
        int n = 4;
        double[][] out = new double[(n + 1) * (n + 1) * (n + 1)][];
        int k = 0;
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                for (int q = 0; q <= n; q++) {
                    out[k++] = new double[]{b[0] + (b[3] - b[0]) * i / n, y0 + (b[4] - y0) * j / n,
                            b[2] + (b[5] - b[2]) * q / n};
                }
            }
        }
        return out;
    }

    @Test
    void armsTheRendererDrawsKeepTheOcclusionRule() {
        double limit = -TabletDeviceModel.thickness() - TabletAnimationModel.HANDS_BEHIND_PLATE;
        int inside = 0;
        for (String tier : List.of("320x240", "480x270", "640x360", "960x540", "960x720")) {
            Context c = context(tier, TabletScreenKind.SQUAD);
            Matrix4f projection = new Matrix4f().setPerspective((float) Math.toRadians(70.0D),
                    (float) c.units().aspect(), 0.05F, 100.0F);
            for (double p = 0.61D; p < 0.97D; p += 0.02D) {
                TabletFrame f = TabletPose3D.frame(p, c, FrameQuery.opening(TabletHand.GUN));
                RectPx e = TabletPose3D.subRectPx(f.planePx(), c.geom(), c.geom().e());
                for (boolean right : new boolean[]{true, false}) {
                    // The renderer's stack: tablet ops, the six chain ops, then what
                    // renderRightHand / renderLeftHand applies (the arm part's pivot and bob roll).
                    PoseStack pose = new PoseStack();
                    TabletPoseOps.apply(pose, TabletPose3D.tabletOps(f.tablet().pose(), c.kf()));
                    Matrix4f tablet = new Matrix4f(pose.last().pose());
                    TabletPoseOps.apply(pose, TabletPose3D.handPreOps(right, c.face(), p, c.geom()));
                    pose.translate((right ? TabletAnimationModel.HANDS_PIVOT_RIGHT_X
                            : TabletAnimationModel.HANDS_PIVOT_LEFT_X) / 16.0D,
                            TabletAnimationModel.HANDS_PIVOT_Y / 16.0D, 0.0D);
                    pose.mulPose(Axis.ZP.rotation((float) ((right ? 1 : -1)
                            * TabletAnimationModel.HANDS_BOB_Z_ROT)));
                    pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                    Matrix4f arm = pose.last().pose();
                    Matrix4f toTablet = new Matrix4f(tablet).invert().mul(arm);
                    for (double[] s : fist(right)) {
                        Vector4f v = new Vector4f((float) s[0], (float) s[1], (float) s[2], 1.0F);
                        Vector4f local = new Vector4f(v);
                        toTablet.transform(local);
                        arm.transform(v);
                        if (-v.z() < 0.05F) {
                            continue;
                        }
                        projection.transform(v);
                        double x = (v.x() / v.w() * 0.5D + 0.5D) * c.units().pxW();
                        double y = (0.5D - v.y() / v.w() * 0.5D) * c.units().pxH();
                        if (x <= e.x() || x >= e.x() + e.w() || y <= e.y() || y >= e.y() + e.h()) {
                            continue;
                        }
                        inside++;
                        double at = p;
                        assertTrue(local.z() < limit + 1e-4, () -> tier + " p=" + at
                                + " fist in front of the back plate z=" + local.z());
                    }
                }
            }
        }
        assertTrue(inside > 0, "fist samples fall into E");
    }
}
