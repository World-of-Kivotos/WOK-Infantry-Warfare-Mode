package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wok.infantry.client.screen.DeviceSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.client.ForgeHooksClient;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws scheme A's 3D layer inside the first-person hand pass (DESIGN 3.5, IMPL_PLAN 4.3): the D2
 * device (front face from the {@link TabletTextures} image, full bright; case sides and back plate
 * lit like an entity), the sleep line, and both hands holding it from behind.
 *
 * <p>Everything is built from the identity (IMPL_PLAN step 2): kf first, then the tablet pose, so
 * the tablet never inherits view bobbing, the TaCZ camera or the stamina sway. The hands are the
 * player's own arms ({@code ForgeHooksClient.renderSpecificFirstPersonArm} first, so armour and
 * backpack sleeves follow, IMPL_PLAN D8), each under the tablet matrix times its first six chain
 * operations; {@code renderRightHand / renderLeftHand} applies the model part itself. An invisible
 * player holds the tablet without hands. Render thread only.
 */
final class TabletRenderer {
    private TabletRenderer() {
    }

    /**
     * Draws one frame of the 3D layer on {@code pose} (pushed and popped here).
     *
     * @param kf      FOV compensation of this hand pass ({@code 1 / (P11 · tan 35°)})
     * @param face    the uploaded front face ({@code null}: no device, hands only)
     * @param skin    the livery's case paint
     * @param tablet  draw the device
     * @param hands   draw the hands
     */
    static void draw(PoseStack pose, MultiBufferSource buffers, int light, TabletFrame frame,
                     double kf, TabletTextures.Face face, DeviceSkin skin,
                     AbstractClientPlayer player, boolean tablet, boolean hands) {
        pose.pushPose();
        try {
            TabletPoseOps.apply(pose, TabletPose3D.tabletOps(frame.tablet().pose(), kf));
            if (tablet && face != null && skin != null) {
                drawDevice(pose, buffers, light, frame, face, skin);
            }
            if (hands && player != null && !player.isInvisible()) {
                TabletPose3D.Face fc = frame.ctx().face();
                drawArm(pose, buffers, light, player, true, fc, frame);
                drawArm(pose, buffers, light, player, false, fc, frame);
            }
        } finally {
            pose.popPose();
        }
    }

    private static void drawDevice(PoseStack pose, MultiBufferSource buffers, int light,
                                   TabletFrame frame, TabletTextures.Face face, DeviceSkin skin) {
        TabletPose3D.Face fc = frame.ctx().face();
        TabletD2Geometry g = frame.ctx().geom();
        TabletDeviceModel.FaceQuad q = TabletDeviceModel.faceQuad(fc, g);
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();

        VertexConsumer front = buffers.getBuffer(TabletRenderTypes.face(face.location(),
                face.key().linear()));
        faceVertex(front, m, q.x0(), q.yTop(), 0.0D, q.u0(), q.v0(), 0xFFFFFFFF);
        faceVertex(front, m, q.x0(), q.yBottom(), 0.0D, q.u0(), q.v1(), 0xFFFFFFFF);
        faceVertex(front, m, q.x1(), q.yBottom(), 0.0D, q.u1(), q.v1(), 0xFFFFFFFF);
        faceVertex(front, m, q.x1(), q.yTop(), 0.0D, q.u1(), q.v0(), 0xFFFFFFFF);

        TabletDeviceModel.Mesh mesh = face.mesh();
        int w = mesh.width();
        int h = mesh.height();
        double t = -q.thick();
        VertexConsumer body = buffers.getBuffer(TabletRenderTypes.body(TabletTextures.white()));
        for (TabletDeviceModel.Edge e : mesh.edges()) {
            int color = e.wall() == TabletDeviceModel.Wall.CASE ? skin.caseColor() : skin.rubber();
            double ax = TabletDeviceModel.planeX(fc, w, e.x0());
            double ay = TabletDeviceModel.planeY(fc, h, e.y0());
            double bx = TabletDeviceModel.planeX(fc, w, e.x1());
            double by = TabletDeviceModel.planeY(fc, h, e.y1());
            float nx = e.dy();
            float ny = e.dx();
            bodyVertex(body, m, n, ax, ay, 0.0D, color, light, nx, ny, 0.0F);
            bodyVertex(body, m, n, ax, ay, t, color, light, nx, ny, 0.0F);
            bodyVertex(body, m, n, bx, by, t, color, light, nx, ny, 0.0F);
            bodyVertex(body, m, n, bx, by, 0.0D, color, light, nx, ny, 0.0F);
        }
        int back = skin.caseLo();
        for (TabletDeviceModel.BackRect r : mesh.back()) {
            double x0 = TabletDeviceModel.planeX(fc, w, r.left());
            double x1 = TabletDeviceModel.planeX(fc, w, r.right());
            double y0 = TabletDeviceModel.planeY(fc, h, r.top());
            double y1 = TabletDeviceModel.planeY(fc, h, r.bottom());
            bodyVertex(body, m, n, x0, y0, t, back, light, 0.0F, 0.0F, -1.0F);
            bodyVertex(body, m, n, x1, y0, t, back, light, 0.0F, 0.0F, -1.0F);
            bodyVertex(body, m, n, x1, y1, t, back, light, 0.0F, 0.0F, -1.0F);
            bodyVertex(body, m, n, x0, y1, t, back, light, 0.0F, 0.0F, -1.0F);
        }

        TabletFrame.Sleep sleep = frame.fx().sleep();
        if (sleep.on() && sleep.widthFrac() > 0.0D) {
            double half = q.sleepWidth() * sleep.widthFrac() / 2.0D;
            double halfH = sleep.heightBlocks() / 2.0D;
            int argb = (int) Math.round(255.0D * Math.max(0.0D, Math.min(1.0D, sleep.alpha()))) << 24
                    | (TabletAnimationModel.LIGHT & 0xFFFFFF);
            double z = TabletDeviceModel.SLEEP_LINE_Z;
            VertexConsumer line = buffers.getBuffer(TabletRenderTypes.face(TabletTextures.white(),
                    false));
            faceVertex(line, m, q.sleepCenterX() - half, q.sleepCenterY() + halfH, z, 0.0D, 0.0D, argb);
            faceVertex(line, m, q.sleepCenterX() - half, q.sleepCenterY() - halfH, z, 0.0D, 1.0D, argb);
            faceVertex(line, m, q.sleepCenterX() + half, q.sleepCenterY() - halfH, z, 1.0D, 1.0D, argb);
            faceVertex(line, m, q.sleepCenterX() + half, q.sleepCenterY() + halfH, z, 1.0D, 0.0D, argb);
        }
    }

    private static void faceVertex(VertexConsumer vc, Matrix4f m, double x, double y, double z,
                                   double u, double v, int argb) {
        vc.vertex(m, (float) x, (float) y, (float) z)
                .color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, argb >>> 24)
                .uv((float) u, (float) v).endVertex();
    }

    private static void bodyVertex(VertexConsumer vc, Matrix4f m, Matrix3f n, double x, double y,
                                   double z, int argb, int light, float nx, float ny, float nz) {
        vc.vertex(m, (float) x, (float) y, (float) z)
                .color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, 0xFF)
                .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(n, nx, ny, nz).endVertex();
    }

    private static void drawArm(PoseStack pose, MultiBufferSource buffers, int light,
                                AbstractClientPlayer player, boolean right, TabletPose3D.Face fc,
                                TabletFrame frame) {
        EntityRenderer<? super AbstractClientPlayer> renderer = Minecraft.getInstance()
                .getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        pose.pushPose();
        try {
            TabletPoseOps.apply(pose, TabletPose3D.handPreOps(right, fc, frame.p(),
                    frame.ctx().geom()));
            HumanoidArm arm = right ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
            if (!ForgeHooksClient.renderSpecificFirstPersonArm(pose, buffers, light, player, arm)) {
                if (right) {
                    playerRenderer.renderRightHand(pose, buffers, light, player);
                } else {
                    playerRenderer.renderLeftHand(pose, buffers, light, player);
                }
            }
        } finally {
            pose.popPose();
        }
    }
}
