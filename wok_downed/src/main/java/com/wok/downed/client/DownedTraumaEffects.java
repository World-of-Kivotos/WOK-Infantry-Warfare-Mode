package com.wok.downed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wok.downed.WokDownedMod;
import com.wok.downed.config.DownedConfig;
import com.wok.downed.state.DownedService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * The downed trauma presentation: a vignette pulling the edges to black, a narrowed view and a
 * heavy camera shake, so being down reads as a wound rather than as hidden UI.
 *
 * <p>Cosmetic and client-side only. Values come from the {@code trauma} config section so a
 * server that wants it milder, or a player who cannot stand the effect, can tune it without
 * touching code.
 *
 * <p>The vignette is four trapezoid strips (top, bottom, left, right). Each strip runs from the
 * clear inner rectangle out to the screen border with a per-vertex alpha ramp, so the gradient is
 * interpolated by the vertex pipeline and needs no shader pack support.
 */
@Mod.EventBusSubscriber(modid = WokDownedMod.MOD_ID, value = Dist.CLIENT)
public final class DownedTraumaEffects {
    /** Fraction of half-width/half-height that stays completely clear. */
    private static final float CLEAR_WIDTH = 0.46F;
    private static final float CLEAR_HEIGHT = 0.40F;
    private static long tick;

    public static void reset() {
        tick = 0L;
    }

    private static boolean active() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && DownedService.isDowned(player);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!active()) {
            reset();
            return;
        }
        tick++;
    }

    /** Narrows the field of view while downed. */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (!active() || !DownedConfig.VIGNETTE_ENABLED.get()) {
            return;
        }
        event.setNewFovModifier(
                event.getNewFovModifier() * DownedConfig.FOV_MULTIPLIER.get().floatValue());
    }

    /** Tilts and jitters the camera so the world itself feels unstable. */
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active() || !DownedConfig.VIGNETTE_ENABLED.get()) {
            return;
        }
        double amplitude = DownedConfig.SHAKE_AMPLITUDE.get();
        if (amplitude <= 0.0D) {
            return;
        }
        double t = tick + event.getPartialTick();
        // Two frequencies: a slow sway plus a fast tremor, so the shake is not a plain sine.
        float yaw = (float) (amplitude * (0.7D * Math.sin(t * 0.19D) + 0.3D * Math.sin(t * 0.53D)));
        float pitch = (float) (amplitude * 0.8D
                * (0.7D * Math.cos(t * 0.23D) + 0.3D * Math.cos(t * 0.61D)));
        float roll = (float) (amplitude * 0.45D * Math.sin(t * 0.11D));
        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!active() || !DownedConfig.VIGNETTE_ENABLED.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        Matrix4f matrix = event.getGuiGraphics().pose().last().pose();
        drawGreyWash(width, height,
                DownedConfig.VIGNETTE_DESATURATE.get().floatValue());
        drawVignette(matrix, width, height,
                DownedConfig.VIGNETTE_DARKNESS.get().floatValue());
        DownedTinnitus.tick(minecraft);
    }

    /**
     * Pulls the frame toward black and white.
     *
     * <p>1.20.1's {@code GuiGraphics} only blits by texture location, so a per-pixel colour matrix
     * cannot be applied to the finished frame without owning the render pipeline (which shader
     * packs also hook). A neutral grey wash is used instead: it removes the saturation that hue
     * carries while staying compatible with Embeddium/Oculus. The cost is that it also lifts the
     * blacks slightly, which is why the vignette is applied after it and stays fully opaque at
     * the borders.
     */
    private static void drawGreyWash(int width, int height, float strength) {
        if (strength <= 0.0F) {
            return;
        }
        int alpha = Math.round(255.0F * Math.min(1.0F, strength) * 0.55F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // Neutral grey with the alpha carrying the requested strength.
        buffer.vertex(0.0F, height, 0.0F).color(0.42F, 0.42F, 0.42F, alpha / 255.0F).endVertex();
        buffer.vertex(width, height, 0.0F).color(0.42F, 0.42F, 0.42F, alpha / 255.0F).endVertex();
        buffer.vertex(width, 0.0F, 0.0F).color(0.42F, 0.42F, 0.42F, alpha / 255.0F).endVertex();
        buffer.vertex(0.0F, 0.0F, 0.0F).color(0.42F, 0.42F, 0.42F, alpha / 255.0F).endVertex();
        tesselator.end();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawVignette(Matrix4f matrix, int width, int height, float darkness) {
        if (darkness <= 0.0F) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float w = width;
        float h = height;
        float clearX = w * (1.0F - CLEAR_WIDTH) * 0.5F;
        float clearY = h * (1.0F - CLEAR_HEIGHT) * 0.5F;
        float clearW = w - clearX * 2.0F;
        float clearH = h - clearY * 2.0F;
        // Extra weight away from the horizon keeps the centre readable while the corners go dark.
        float top = darkness;
        float bottom = darkness * 1.05F;
        float sides = darkness * 0.95F;

        // Top strip: clear edge at clearY, opaque at y = 0.
        ramp(buffer, matrix,
                clearX, clearY, clearX, 0.0F,
                clearX + clearW, clearY, clearX + clearW, 0.0F, top);
        // Bottom strip.
        ramp(buffer, matrix,
                clearX, clearY + clearH, clearX, h,
                clearX + clearW, clearY + clearH, clearX + clearW, h, bottom);
        // Left strip (runs between the two horizontal strips so no seam overlaps).
        ramp(buffer, matrix,
                clearX, 0.0F, 0.0F, 0.0F,
                clearX, h, 0.0F, h, sides);
        // Right strip.
        ramp(buffer, matrix,
                clearX + clearW, 0.0F, w, 0.0F,
                clearX + clearW, h, w, h, sides);

        tesselator.end();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * One gradient quad between an inner edge (fully transparent) and an outer edge (opaque).
     * The two edge points are given explicitly so the strips join without a visible seam.
     */
    private static void ramp(BufferBuilder buffer, Matrix4f matrix,
                             float inX0, float inY0, float outX0, float outY0,
                             float inX1, float inY1, float outX1, float outY1, float alpha) {
        buffer.vertex(matrix, outX0, outY0, 0.0F).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
        buffer.vertex(matrix, outX1, outY1, 0.0F).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
        buffer.vertex(matrix, inX1, inY1, 0.0F).color(0.0F, 0.0F, 0.0F, 0.0F).endVertex();
        buffer.vertex(matrix, inX0, inY0, 0.0F).color(0.0F, 0.0F, 0.0F, 0.0F).endVertex();
    }

    private DownedTraumaEffects() {
    }
}
