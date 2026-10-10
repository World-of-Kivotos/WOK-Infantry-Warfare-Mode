package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wok.infantry.client.screen.DeviceArt;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Scheme A's screen effects on the glass opening S (DESIGN 3.5, preview {@code compose.drawA}
 * 197–226), in physical pixels: the backlight mask over the opened middle, the GLASS bands top and
 * bottom that open from the middle, and the two 1-GUI-pixel LIGHT edges where the bands end
 * (merged into one line while the bands are still shut). The C1 close replays the same effects
 * backwards.
 *
 * <p>{@link #wakeFills} is the pure geometry; {@link #draw} fills it under a {@code 1/S} scale, so
 * every fill lands on whole physical pixels.
 */
public final class TabletEffects2D {
    /** One fill in physical pixels; {@code argb} already carries its alpha. */
    public record Fill(int x, int y, int w, int h, int argb) {
    }

    private TabletEffects2D() {
    }

    /**
     * The wake fills of a frame (none when neither the wake nor the mask is on).
     *
     * @param glass    the glass opening S in physical pixels (edges snapped)
     * @param fx       the frame's effects
     * @param guiScale window GUI scale S (one GUI pixel = S physical pixels)
     */
    public static List<Fill> wakeFills(TabletPose3D.RectPx glass, TabletFrame.Effects fx,
                                       int guiScale) {
        if (glass == null || fx == null || !fx.wake().on() && !fx.mask().on()) {
            return List.of();
        }
        TabletFrame.Wake wake = fx.wake();
        int x0 = (int) Math.round(glass.x());
        int y0 = (int) Math.round(glass.y());
        int w = (int) Math.round(glass.x() + glass.w()) - x0;
        int y1 = (int) Math.round(glass.y() + glass.h());
        int h = y1 - y0;
        if (w <= 0 || h <= 0) {
            return List.of();
        }
        List<Fill> fills = new ArrayList<>(5);
        int band = wake.on() ? (int) Math.round(wake.bandFrac() * h / 2.0D) : 0;
        if (fx.mask().on() && h - 2 * band > 0) {
            add(fills, x0, y0 + band, w, h - 2 * band, TabletAnimationModel.BACKLIGHT,
                    fx.mask().alpha());
        }
        if (!wake.on()) {
            return fills;
        }
        if (band > 0) {
            add(fills, x0, y0, w, band, DeviceArt.GLASS, 1.0D);
            add(fills, x0, y1 - band, w, band, DeviceArt.GLASS, 1.0D);
        }
        int edge = Math.max(1, Math.max(1, guiScale) * wake.edgePx());
        int top = y0 + band;
        int bottom = y1 - band;
        if (bottom - top < 2 * edge) {
            add(fills, x0, (int) Math.round((top + bottom) / 2.0D - edge / 2.0D), w, edge,
                    TabletAnimationModel.LIGHT, wake.edgeAlpha());
        } else {
            add(fills, x0, top, w, edge, TabletAnimationModel.LIGHT, wake.edgeAlpha());
            add(fills, x0, bottom - edge, w, edge, TabletAnimationModel.LIGHT, wake.edgeAlpha());
        }
        return fills;
    }

    private static void add(List<Fill> fills, int x, int y, int w, int h, int color, double alpha) {
        double a = ((color >>> 24) & 0xFF) * Math.max(0.0D, Math.min(1.0D, alpha));
        int argb = (int) Math.round(a) << 24 | (color & 0xFFFFFF);
        if (w > 0 && h > 0 && (argb >>> 24) != 0) {
            fills.add(new Fill(x, y, w, h, argb));
        }
    }

    /**
     * Fills {@code fills} (physical pixels) on top of the GUI at depth {@code z}: the current pose
     * is the GUI pose (one unit = one GUI pixel), scaled by {@code 1/S} here.
     */
    public static void draw(GuiGraphics graphics, List<Fill> fills, int guiScale, float z) {
        if (fills.isEmpty()) {
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, z);
        float inverse = 1.0F / Math.max(1, guiScale);
        pose.scale(inverse, inverse, 1.0F);
        for (Fill fill : fills) {
            graphics.fill(fill.x(), fill.y(), fill.x() + fill.w(), fill.y() + fill.h(), fill.argb());
        }
        pose.popPose();
    }
}
