package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wok.infantry.client.screen.DeviceArt;
import com.wok.infantry.client.screen.TacticalScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The device scope of one {@link TacticalScreen} frame while the tablet comes out on scheme B or
 * the quick setting (DESIGN 4.1, 4.3): the device and its page move down by whole layout pixels
 * ({@code translate(0, dy)} on the screen's pose, which already holds the 2x layout scale), and the
 * glass opening S is dark (GLASS) or lit down to the scan line.
 *
 * <p>{@link #drawEffects()} first ends the move, then draws the glass in layout pixels at
 * {@link #EFFECT_Z} (above the modal, the glass overlay and the tooltip); {@link #close()} only
 * ends a move that is still open (a frame that threw).
 */
final class TabletDeviceScope implements TacticalScreen.DeviceScope {
    /** Depth of the screen effects: above the modal (300), the glass (350) and the tooltip. */
    static final float EFFECT_Z = 500.0F;

    private final PoseStack pose;
    private final Runnable glass;
    private boolean moved;

    /**
     * Moves {@code pose} by {@code dy} layout pixels; {@code glass} draws the frame's glass after
     * the move has ended (unit-test seam: a pose without a {@link GuiGraphics}).
     */
    TabletDeviceScope(PoseStack pose, int dy, Runnable glass) {
        this.pose = pose;
        this.glass = glass;
        if (dy != 0) {
            pose.pushPose();
            pose.translate(0.0F, dy, 0.0F);
            moved = true;
        }
    }

    /** The scope of terminal frame {@code frame}; no scope for the static picture. */
    static TacticalScreen.DeviceScope of(GuiGraphics graphics, TabletPath2D.TermFrame frame) {
        if (frame == null || frame.identity() || !needed(frame)) {
            return TacticalScreen.DeviceScope.NONE;
        }
        return new TabletDeviceScope(graphics.pose(), frame.dy(), () -> drawGlass(graphics, frame));
    }

    /** Whether frame {@code f} changes anything: a move or glass that is not fully lit. */
    static boolean needed(TabletPath2D.TermFrame f) {
        return f.dy() != 0 || f.glass() != TabletPath2D.TermGlass.LIT;
    }

    @Override
    public void drawEffects() {
        end();
        glass.run();
    }

    @Override
    public void close() {
        end();
    }

    private void end() {
        if (moved) {
            moved = false;
            pose.popPose();
        }
    }

    /** The glass of an opening terminal frame, in layout pixels: dark, or dark below the scan line. */
    static void drawGlass(GuiGraphics graphics, TabletPath2D.TermFrame f) {
        TabletPath2D.Box s = f.s();
        if (f.glass() != TabletPath2D.TermGlass.DARK && f.glass() != TabletPath2D.TermGlass.SCAN) {
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, EFFECT_Z);
        if (f.glass() == TabletPath2D.TermGlass.DARK) {
            graphics.fill(s.x(), s.y(), s.right(), s.bottom(), DeviceArt.GLASS);
        } else {
            if (f.scanY() < s.bottom()) {
                // GuiGraphics.fill swaps an inverted rectangle, so never pass one.
                graphics.fill(s.x(), f.scanY(), s.right(), s.bottom(), DeviceArt.GLASS);
            }
            TabletMapFrame.fill(graphics, new TabletPath2D.Box(s.x(), f.scanY(), s.w(), 1),
                    TabletAnimationModel.LIGHT, f.scanAlpha());
        }
        pose.popPose();
    }
}
