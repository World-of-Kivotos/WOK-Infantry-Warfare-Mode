package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.function.Supplier;

/**
 * Small GL helpers of the animation's 2D passes (smooth downscale, the C1 copy and its replay):
 * textured and coloured quads with an exact blend, and texture filters. Render thread only.
 *
 * <p>A core shader applies the blend mode written in its JSON whenever it differs from the last
 * one applied, which would replace a blend set by hand before the draw. {@link #shader} applies the
 * shader once first, so the blend set afterwards is the one the draw uses.
 */
final class TabletGl {
    private TabletGl() {
    }

    /**
     * Selects {@code supplier}'s shader with its JSON blend already applied. Applying binds the
     * shader's samplers as they were last set, which may be a texture deleted since (a resized
     * target, a released face): a sampled shader is therefore given its texture first.
     */
    private static void shader(Supplier<ShaderInstance> supplier, int texture) {
        RenderSystem.setShader(supplier);
        if (texture > 0) {
            RenderSystem.setShaderTexture(0, texture);
        }
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            if (texture > 0) {
                shader.setSampler("Sampler0", texture);
            }
            shader.apply();
            shader.clear();
        }
    }

    /** The position-texture shader (discards texels with alpha 0) sampling {@code texture}. */
    static void textureShader(int texture) {
        shader(GameRenderer::getPositionTexShader, texture);
    }

    /** The position-colour shader (no texture). */
    static void colorShader() {
        shader(GameRenderer::getPositionColorShader, 0);
    }

    /**
     * A quad with texture coordinates: (x0, y0) gets (u0, v0), (x1, y1) gets (u1, v1); corners in
     * the order top-left, bottom-left, bottom-right, top-right of a y-down space.
     */
    static void texQuad(Matrix4f m, float x0, float y0, float x1, float y1, float u0, float v0,
                        float u1, float v1) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(m, x0, y0, 0.0F).uv(u0, v0).endVertex();
        builder.vertex(m, x0, y1, 0.0F).uv(u0, v1).endVertex();
        builder.vertex(m, x1, y1, 0.0F).uv(u1, v1).endVertex();
        builder.vertex(m, x1, y0, 0.0F).uv(u1, v0).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    /** A flat quad in {@code argb}. */
    static void colorQuad(Matrix4f m, float x0, float y0, float x1, float y1, int argb) {
        int a = argb >>> 24;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(m, x0, y0, 0.0F).color(r, g, b, a).endVertex();
        builder.vertex(m, x0, y1, 0.0F).color(r, g, b, a).endVertex();
        builder.vertex(m, x1, y1, 0.0F).color(r, g, b, a).endVertex();
        builder.vertex(m, x1, y0, 0.0F).color(r, g, b, a).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    /** Sets the filters of texture {@code id} (and builds its mip levels when {@code mipmap}). */
    static void filter(int id, boolean linear, boolean mipmap) {
        RenderSystem.bindTexture(id);
        if (mipmap) {
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }
        int min = linear ? (mipmap ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR)
                : (mipmap ? GL11.GL_NEAREST_MIPMAP_NEAREST : GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, min);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                linear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
    }

    /** Back to the state the GUI helpers leave: no blend (default function), all channels, cull on. */
    static void restore() {
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
}
