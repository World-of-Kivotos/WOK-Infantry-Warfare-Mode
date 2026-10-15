package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wok.infantry.WokInfantryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * The smooth downscale of scheme A (DESIGN 3.1, IMPL_PLAN D12; review V6): on the tiers where no
 * whole pixel ratio reads well (GUI 2 and GUI 1, and the map on every tier) the page is drawn at
 * its own size into a window-sized target and then laid into the screen rectangle with linear
 * filtering, mip-mapped below half size. Only for the opening's 0.61 ≤ p &lt; 1; p = 1 hands over
 * at the identity without it.
 *
 * <p>Alpha: GUI blits write their own alpha ({@code defaultBlendFunc}), so over the opaque case a
 * matte speck would leave a translucent texel. The colours are right premultiplied values anyway
 * (everything is drawn over transparent black), so after the page the device body's alpha is set
 * back to 1 through the front-face mask (alpha only, no blending): around the case only the drop
 * shadow, a modal's dim or a tooltip keep their own coverage. A full-screen page is opaque and gets
 * alpha 1 everywhere. The target is laid back premultiplied ({@code ONE, ONE_MINUS_SRC_ALPHA}).
 *
 * <p>Created lazily, resized with the window, released on logout. Render thread only.
 */
final class TabletScreenTarget {
    private static TextureTarget target;
    private static boolean active;
    private static boolean failed;

    private TabletScreenTarget() {
    }

    /** Whether the page is being drawn into the target now. */
    static boolean active() {
        return active;
    }

    /**
     * Flushes what is batched, binds the window-sized target and clears it to transparent; the
     * page then draws as usual. Returns {@code false} (nothing changed) when no target can be had.
     */
    static boolean begin(GuiGraphics graphics) {
        if (active || failed) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        try {
            graphics.flush();
            if (target == null) {
                target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            } else if (target.width != main.width || target.height != main.height) {
                target.resize(main.width, main.height, Minecraft.ON_OSX);
            }
            target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            active = true;
            return true;
        } catch (RuntimeException exception) {
            failed = true;
            WokInfantryMod.LOGGER.warn("[TabletAnim] smooth downscale target unavailable", exception);
            main.bindWrite(true);
            return false;
        }
    }

    /**
     * Ends the page: fixes the alpha ({@code mask} = the front-face texture laid over the layout
     * area {@code maskW × maskH} GUI units, or alpha 1 everywhere when {@code opaque}), binds the
     * main target again and lays the picture into {@code rect} (physical pixels) at GUI scale
     * {@code guiScale}.
     */
    static void end(GuiGraphics graphics, TabletPose3D.RectPx rect, int guiScale, boolean opaque,
                    int maskTexture, float maskW, float maskH) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        try {
            graphics.flush();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            if (opaque) {
                RenderSystem.colorMask(false, false, false, true);
                RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
                RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            } else if (maskTexture > 0) {
                TabletGl.textureShader(maskTexture);
                RenderSystem.disableBlend();
                RenderSystem.colorMask(false, false, false, true);
                TabletGl.filter(maskTexture, false, false);
                TabletGl.texQuad(new Matrix4f(), 0.0F, 0.0F, maskW, maskH, 0.0F, 0.0F, 1.0F, 1.0F);
            }
            RenderSystem.colorMask(true, true, true, true);
            main.bindWrite(true);
            active = false;
            draw(rect, guiScale, opaque);
        } finally {
            active = false;
            main.bindWrite(true);
            TabletGl.restore();
        }
    }

    /** Leaves the target without laying it back (a frame that threw). */
    static void abort() {
        if (active) {
            active = false;
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            TabletGl.restore();
        }
    }

    private static void draw(TabletPose3D.RectPx rect, int guiScale, boolean opaque) {
        if (rect == null || target == null) {
            return;
        }
        float s = Math.max(1, guiScale);
        float x0 = (float) (rect.x() / s);
        float y0 = (float) (rect.y() / s);
        float x1 = (float) ((rect.x() + rect.w()) / s);
        float y1 = (float) ((rect.y() + rect.h()) / s);
        int id = target.getColorTextureId();
        TabletGl.textureShader(id);
        if (opaque) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
        TabletGl.filter(id, true, TabletCapturePlan.mipmap(rect.w() / Math.max(1, target.width)));
        // The target's rows run bottom-up: v 1 is the top of the window.
        TabletGl.texQuad(new Matrix4f(), x0, y0, x1, y1, 0.0F, 1.0F, 1.0F, 0.0F);
        // Leave the colour texture as RenderTarget expects it (no mip levels sampled).
        TabletGl.filter(id, true, false);
    }

    /** Frees the target (logout, resource reload). */
    static void release() {
        abort();
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        failed = false;
    }
}
