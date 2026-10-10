package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wok.infantry.client.screen.TacticalScreen;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * The device scope of one {@link TacticalScreen} frame of scheme A between READ and the end of the
 * zoom (0.61 ≤ p &lt; 1; DESIGN 3.1, 3.5; IMPL_PLAN 4.3): the device and its page are drawn into the
 * plane rectangle of the frame, and the wake effects go over the glass opening afterwards.
 *
 * <ul>
 *   <li><b>Pixel</b> (a whole pixel ratio at READ): the screen's pose, which already holds the
 *   layout factor f, is post-multiplied by {@code translate(x/(S·f), y/(S·f)) · scale(s)}, the
 *   same as translating by (x, y) physical pixels and scaling the identity picture by s.</li>
 *   <li><b>Smooth</b> (GUI 2, GUI 1): the page draws at its own size into
 *   {@link TabletScreenTarget}, which lays it into the rectangle with linear filtering. When the
 *   target cannot be had the frame falls back to the pixel transform.</li>
 * </ul>
 *
 * <p>{@link #drawEffects()} ends the transform first and then fills the wake bands, the mask and the
 * edge lines in physical pixels at {@link #EFFECT_Z}; {@link #close()} only undoes what is still
 * open (a frame that threw).
 */
final class TabletReadScope implements TacticalScreen.DeviceScope {
    /** Depth of the screen effects: above the modal, the glass and the tooltip. */
    static final float EFFECT_Z = TabletDeviceScope.EFFECT_Z;

    private final GuiGraphics graphics;
    private final PoseStack pose;
    private final TabletFrame frame;
    private final int guiScale;
    private final int factor;
    private final int maskTexture;
    private final boolean smooth;
    private boolean open;

    private TabletReadScope(GuiGraphics graphics, PoseStack pose, TabletFrame frame, int guiScale,
                            int factor, int maskTexture, boolean smooth) {
        this.graphics = graphics;
        this.pose = pose;
        this.frame = frame;
        this.guiScale = Math.max(1, guiScale);
        this.factor = Math.max(1, factor);
        this.maskTexture = maskTexture;
        this.smooth = smooth;
    }

    /**
     * The scope of frame {@code f} (none when the page is not drawn or is at the identity).
     * {@code maskTexture}: the front-face texture that fixes the device's alpha in the smooth
     * target (0 = none).
     */
    static TacticalScreen.DeviceScope of(GuiGraphics graphics, TabletFrame f, int guiScale,
                                         int factor, int maskTexture) {
        if (f == null || !f.ui().draw() || f.ui().identity() || f.ui().rectPx() == null) {
            return TacticalScreen.DeviceScope.NONE;
        }
        boolean smooth = f.ui().smooth() && TabletScreenTarget.begin(graphics);
        TabletReadScope scope = new TabletReadScope(graphics, graphics.pose(), f, guiScale, factor,
                maskTexture, smooth);
        if (!smooth) {
            scope.push();
        }
        scope.open = true;
        return scope;
    }

    /** Pushes the pixel transform of {@link #frame} on {@code pose} (unit-test seam). */
    void push() {
        pushPixel(pose, frame.ui().rectPx(), guiScale, factor);
    }

    /**
     * {@code translate(x/(S·f), y/(S·f), 0) · scale(s, s, 1)} on a pose that already scales by the
     * layout factor f: layout point (x, y) then lands on physical pixel (rect.x + s·S·f·x, …).
     */
    static void pushPixel(PoseStack pose, TabletPose3D.RectPx rect, int guiScale, int factor) {
        double unit = (double) Math.max(1, guiScale) * Math.max(1, factor);
        pose.pushPose();
        pose.translate(rect.x() / unit, rect.y() / unit, 0.0D);
        float s = (float) rect.scale();
        pose.scale(s, s, 1.0F);
    }

    @Override
    public void drawEffects() {
        end(true);
        List<TabletEffects2D.Fill> fills = TabletEffects2D.wakeFills(frame.glassPx(), frame.fx(),
                guiScale);
        TabletEffects2D.draw(graphics, fills, guiScale * factor, EFFECT_Z);
    }

    @Override
    public void close() {
        end(false);
    }

    private void end(boolean finished) {
        if (!open) {
            return;
        }
        open = false;
        if (!smooth) {
            pose.popPose();
            return;
        }
        if (!finished) {
            TabletScreenTarget.abort();
            return;
        }
        TabletD2Geometry g = frame.ctx().geom();
        TabletScreenTarget.end(graphics, frame.ui().rectPx(), guiScale, false, maskTexture,
                (float) g.w() * factor, (float) g.h() * factor);
    }
}
