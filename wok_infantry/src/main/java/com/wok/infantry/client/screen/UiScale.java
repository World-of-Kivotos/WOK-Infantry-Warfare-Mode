package com.wok.infantry.client.screen;

import com.mojang.blaze3d.platform.Window;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.config.InfantryClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Minimum 2x presentation for WOK步战 screens and HUD.
 *
 * <p>At GUI scale 1 the CJK unifont is drawn at 8 physical pixels and cannot be read. When the
 * window GUI scale is 1, the window is at least 640×480 and the client option
 * {@code ui.minimumScale2x} is on (default), WOK UI is laid out at half the size and drawn with
 * a 2x pose, so 960×720 becomes a 480×360 layout. In every other case the factor is 1 and the
 * UI behaves exactly as before.
 *
 * <p>{@link TacticalScreen} applies the factor to layout, drawing, mouse input and tooltips. Code
 * drawing under that pose must clip with {@link #enableScissor}, because
 * {@link GuiGraphics#enableScissor} ignores the pose. HUD overlays call {@link #hudFactor()}
 * and scale their pose and screen size themselves.
 */
public final class UiScale {
    /** Smallest logical layout the 2x rule may produce (the 320×240 tier). */
    public static final int MIN_LAYOUT_WIDTH = 320;
    public static final int MIN_LAYOUT_HEIGHT = 240;

    private UiScale() {
    }

    /**
     * Pure rule: 2 when {@code enabled}, the window GUI scale is 1 and half the scaled window is
     * still at least 320×240; otherwise 1.
     */
    public static int factorFor(double guiScale, int guiScaledWidth, int guiScaledHeight,
                                boolean enabled) {
        if (!enabled || !(Math.abs(guiScale - 1.0D) < 1.0E-6D)) {
            return 1;
        }
        return guiScaledWidth / 2 >= MIN_LAYOUT_WIDTH && guiScaledHeight / 2 >= MIN_LAYOUT_HEIGHT
                ? 2 : 1;
    }

    /** Factor for the current window and client option; 1 when no client is running. */
    public static int factor() {
        return factor(Minecraft.getInstance());
    }

    public static int factor(Minecraft minecraft) {
        if (minecraft == null || minecraft.getWindow() == null) {
            return 1;
        }
        Window window = minecraft.getWindow();
        return factorFor(window.getGuiScale(), window.getGuiScaledWidth(),
                window.getGuiScaledHeight(), InfantryClientConfig.minimumScale2x());
    }

    /** Factor HUD overlays should apply; the same single switch as screens. */
    public static int hudFactor() {
        return factor();
    }

    /** Physical pixels per logical WOK pixel: window GUI scale × {@link #factor()}. */
    public static double effectiveGuiScale() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return 1.0D;
        }
        return minecraft.getWindow().getGuiScale() * factor(minecraft);
    }

    /** Logical layout size of a GUI-scaled window dimension. */
    public static int layoutSize(int guiScaledSize, int factor) {
        return guiScaledSize / Math.max(1, factor);
    }

    /** GUI-scaled coordinate (mouse event) to the logical layout coordinate. */
    public static double toLayout(double guiCoordinate, int factor) {
        return guiCoordinate / Math.max(1, factor);
    }

    /** GUI-scaled pixel (render mouse position) to the logical layout pixel, rounding down. */
    public static int toLayout(int guiCoordinate, int factor) {
        return Math.floorDiv(guiCoordinate, Math.max(1, factor));
    }

    /** Logical layout coordinate back to GUI-scaled coordinates. */
    public static int toGui(int layoutCoordinate, int factor) {
        return layoutCoordinate * Math.max(1, factor);
    }

    /**
     * Pose-aware scissor: [left, right) x [top, bottom) is given in the current pose space (like
     * {@code fill}) and transformed to GUI-scaled coordinates before clipping. This is the only
     * clipping entry point WOK UI code should use.
     */
    public static void enableScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        int[] bounds = scissorBounds(graphics.pose().last().pose(), left, top, right, bottom);
        graphics.enableScissor(bounds[0], bounds[1], bounds[2], bounds[3]);
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.clipGui(bounds[0], bounds[1], bounds[2], bounds[3]);
        }
    }

    public static void enableScissor(GuiGraphics graphics, UiRect rect) {
        enableScissor(graphics, rect.left(), rect.top(), rect.right(), rect.bottom());
    }

    public static void disableScissor(GuiGraphics graphics) {
        graphics.disableScissor();
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.unclip();
        }
    }

    /**
     * Transforms a pose-space rectangle by {@code pose} (scale and translation) into the GUI
     * rectangle {left, top, right, bottom}; outer edges round outwards so nothing drawn inside the
     * rectangle is clipped.
     */
    static int[] scissorBounds(Matrix4f pose, int left, int top, int right, int bottom) {
        Vector3f a = pose.transformPosition(new Vector3f(left, top, 0.0F));
        Vector3f b = pose.transformPosition(new Vector3f(right, bottom, 0.0F));
        int l = (int) Math.floor(Math.min(a.x(), b.x()) + 1.0E-4F);
        int t = (int) Math.floor(Math.min(a.y(), b.y()) + 1.0E-4F);
        int r = (int) Math.ceil(Math.max(a.x(), b.x()) - 1.0E-4F);
        int btm = (int) Math.ceil(Math.max(a.y(), b.y()) - 1.0E-4F);
        return new int[]{l, t, Math.max(l, r), Math.max(t, btm)};
    }
}
