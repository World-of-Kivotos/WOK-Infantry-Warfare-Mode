package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.wok.infantry.WokInfantryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * The C1 close (DESIGN 3.4, 6.3; IMPL_PLAN 4.4, D13): on the frame the tablet screen goes, before
 * anything of the new frame is drawn ({@code RenderTickEvent} START), the main target still holds
 * the last finished frame. The block of it the device (terminal: its E region; full-screen page:
 * the map rectangle) took is copied once into a texture of its own; for a terminal the world
 * around the case is cleared with the front-face mask (the copy becomes premultiplied, the rounded
 * corners and bumper slopes transparent). The HUD layer then replays the texture shrinking from
 * where it was into the hands ({@link TabletCapturePlan#replay}), with the backdrop, the drop
 * shadow and the sleep effects drawn by the controller around it.
 *
 * <p>If no copy can be made the controller takes the old close from READ instead. Released when
 * the close ends, on logout and on a resource reload. Render thread only.
 */
final class TabletFrameCapture {
    private static TextureTarget target;
    private static TabletCapturePlan.Region region;
    private static boolean ready;
    private static boolean mipmapped;
    private static boolean logged;

    private TabletFrameCapture() {
    }

    /** Whether a copy is ready to replay. */
    static boolean ready() {
        return ready && target != null;
    }

    /** The copied block. */
    static TabletCapturePlan.Region region() {
        return region;
    }

    /**
     * Copies {@code block} of the main target. {@code maskTexture} (0 = none: a full-screen page)
     * is the front-face image laid over {@code plane} (physical pixels of the window), sampled
     * linearly when {@code linear}. Returns whether the copy is ready.
     */
    static boolean capture(TabletCapturePlan.Region block, int maskTexture, TabletPose3D.RectPx plane,
                           boolean linear, double fromP) {
        ready = false;
        mipmapped = false;
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        if (block == null || main == null) {
            return false;
        }
        TabletCapturePlan.Region r = clip(block, main.width, main.height);
        if (r.isEmpty()) {
            return false;
        }
        PoseStack modelView = RenderSystem.getModelViewStack();
        boolean projectionSaved = false;
        boolean modelViewPushed = false;
        try {
            if (target == null) {
                target = new TextureTarget(r.w(), r.h(), false, Minecraft.ON_OSX);
            } else if (target.width != r.w() || target.height != r.h()) {
                target.resize(r.w(), r.h(), Minecraft.ON_OSX);
            }
            int srcY = main.height - (r.y() + r.h());
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            GlStateManager._glBlitFrameBuffer(r.x(), srcY, r.x() + r.w(), srcY + r.h(), 0, 0, r.w(),
                    r.h(), GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

            target.bindWrite(true);
            RenderSystem.backupProjectionMatrix();
            projectionSaved = true;
            RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
            modelView.pushPose();
            modelViewPushed = true;
            modelView.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            // Alpha: the main target's alpha is meaningless; 1 for an opaque page, else the mask.
            RenderSystem.colorMask(false, false, false, true);
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, maskTexture > 0 ? 0.0F : 1.0F);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            if (maskTexture > 0 && plane != null) {
                double[] at = TabletCapturePlan.planeInRegion(r, plane);
                float x0 = ndcX(at[0], r.w());
                float x1 = ndcX(at[0] + at[2], r.w());
                float y0 = ndcY(at[1], r.h());
                float y1 = ndcY(at[1] + at[3], r.h());
                TabletGl.textureShader(maskTexture);
                RenderSystem.disableBlend();
                TabletGl.filter(maskTexture, linear, false);
                TabletGl.texQuad(new Matrix4f(), x0, y0, x1, y1, 0.0F, 0.0F, 1.0F, 1.0F);
                // Premultiply: every colour times the alpha the mask left (0 around the case).
                RenderSystem.colorMask(true, true, true, false);
                TabletGl.colorShader();
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO,
                        GlStateManager.DestFactor.DST_ALPHA, GlStateManager.SourceFactor.ZERO,
                        GlStateManager.DestFactor.ONE);
                TabletGl.colorQuad(new Matrix4f(), -1.0F, 1.0F, 1.0F, -1.0F, 0xFFFFFFFF);
            }
            region = r;
            ready = true;
            if (!logged) {
                logged = true;
                WokInfantryMod.LOGGER.info("[TabletAnim] capture ok {}x{} at ({}, {}) from p={}",
                        r.w(), r.h(), r.x(), r.y(), String.format("%.3f", fromP));
            }
            return true;
        } catch (RuntimeException exception) {
            WokInfantryMod.LOGGER.warn("[TabletAnim] capture failed; this close starts at READ",
                    exception);
            ready = false;
            return false;
        } finally {
            if (modelViewPushed) {
                modelView.popPose();
                RenderSystem.applyModelViewMatrix();
            }
            if (projectionSaved) {
                RenderSystem.restoreProjectionMatrix();
            }
            main.bindWrite(true);
            TabletGl.restore();
            RenderSystem.enableDepthTest();
        }
    }

    private static TabletCapturePlan.Region clip(TabletCapturePlan.Region r, int w, int h) {
        int x0 = Math.max(0, r.x());
        int y0 = Math.max(0, r.y());
        int x1 = Math.min(w, r.right());
        int y1 = Math.min(h, r.bottom());
        return new TabletCapturePlan.Region(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    }

    /** Region pixel x → NDC. */
    private static float ndcX(double x, int w) {
        return (float) (2.0D * x / w - 1.0D);
    }

    /** Region pixel y (down) → NDC (up); row 0 of the copy is the region's bottom row. */
    private static float ndcY(double y, int h) {
        return (float) (1.0D - 2.0D * y / h);
    }

    /**
     * Replays the copy on the HUD pose of {@code graphics}: the block moved and scaled as the
     * rectangle it came from ({@code from}) is now {@code now}, premultiplied, nearest or linear.
     */
    static void replay(GuiGraphics graphics, TabletPose3D.RectPx from, TabletPose3D.RectPx now,
                       boolean linear, int guiScale) {
        if (!ready() || from == null || now == null) {
            return;
        }
        double[] r = TabletCapturePlan.replay(region, from, now);
        float s = Math.max(1, guiScale);
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        int id = target.getColorTextureId();
        TabletGl.textureShader(id);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        boolean mip = linear && TabletCapturePlan.mipmap(r[2] / Math.max(1, region.w()));
        if (mip && !mipmapped) {
            TabletGl.filter(id, true, true);
            mipmapped = true;
        } else {
            RenderSystem.bindTexture(id);
            int min = linear ? (mipmapped && mip ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR)
                    : GL11.GL_NEAREST;
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, min);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                    linear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
        }
        RenderSystem.setShaderTexture(0, id);
        Matrix4f m = graphics.pose().last().pose();
        TabletGl.texQuad(m, (float) (r[0] / s), (float) (r[1] / s), (float) ((r[0] + r[2]) / s),
                (float) ((r[1] + r[3]) / s), 0.0F, 1.0F, 1.0F, 0.0F);
        TabletGl.restore();
    }

    /** Drops the copy (the close ended); the target stays for the next one. */
    static void drop() {
        ready = false;
        mipmapped = false;
    }

    /** Frees the target (logout, resource reload). */
    static void release() {
        drop();
        region = null;
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
