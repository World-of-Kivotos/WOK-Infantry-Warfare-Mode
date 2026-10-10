package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wok.infantry.client.screen.DeviceArt;
import com.wok.infantry.client.screen.TacticalLivery;
import com.wok.infantry.client.screen.TacticalShellLayout;
import com.wok.infantry.client.screen.UiRect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheme B's tactical map (DESIGN 4.2): the old inner screen 0.8 → 1, framed by a D2 device whose
 * lit display P is exactly the inner screen. The frame is drawn from a {@link DeviceArt} plate of a
 * layout sized so that its P is the inner screen (in terminal layout pixels, the terminal's size
 * class), scaled by {@code cs} and moved onto the inner screen. Its size changes every frame of the
 * zoom, so the plates live in a small cache of their own, never in {@link DeviceArt#plate}'s.
 *
 * <p>Full-screen pages (the map, the loadout page) are not {@code TacticalScreen}s: the controller
 * draws the frame in {@code ScreenEvent.Render.Pre}, clips and moves the page there, and draws the
 * sleep layer and the scan line in {@code Render.Post}; the put-away frame is drawn in the HUD layer.
 */
public final class TabletMapFrame {
    /** Depth of the screen effects: above the page's own layers and its tooltip. */
    static final float EFFECT_Z = 500.0F;
    private static final int CACHE_SIZE = 8;
    private static final Map<Key, DeviceArt.Plate> CACHE = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, DeviceArt.Plate> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private record Key(int width, int height, TacticalShellLayout.Density density,
                       TacticalLivery.Livery livery) {
    }

    /**
     * Where the frame's plate goes: a layout of {@code layoutW}×{@code layoutH} terminal pixels in
     * {@code density} whose display P is the inner screen, drawn at {@code (originX, originY)} map
     * pixels scaled by {@code cs}.
     */
    public record Art(int layoutW, int layoutH, TacticalShellLayout.Density density, int cs,
                      int originX, int originY) {
        /** The plate layout (terminal pixels). */
        public TacticalShellLayout layout() {
            return TacticalShellLayout.compute(layoutW, layoutH, density);
        }

        /** A rectangle of the plate layout in map pixels. */
        public UiRect place(UiRect layoutRect) {
            return new UiRect(originX + layoutRect.left() * cs, originY + layoutRect.top() * cs,
                    originX + layoutRect.right() * cs, originY + layoutRect.bottom() * cs);
        }
    }

    private TabletMapFrame() {
    }

    /** The plate placement of frame {@code f} (pure). */
    public static Art art(TabletPath2D.MapFrame f) {
        TacticalShellLayout.Density density = f.geom().density();
        TacticalShellLayout.DeviceMetrics k = TacticalShellLayout.DeviceMetrics.of(density);
        int pw = Math.max(0, (int) Math.floor(f.artW() + 1.0E-9D));
        int ph = Math.max(0, (int) Math.floor(f.artH() + 1.0E-9D));
        int w = pw + 2 * (k.marginX() + k.side() + k.glassInset());
        int h = ph + k.marginTop() + k.marginBottom() + k.top() + k.bottom() + 2 * k.glassInset();
        int px = k.marginX() + k.side() + k.glassInset();
        int py = k.marginTop() + k.top() + k.glassInset();
        int cs = Math.max(1, f.cs());
        return new Art(w, h, density, cs, f.inner().x() - px * cs, f.inner().y() - py * cs);
    }

    private static DeviceArt.Plate plate(Art art, TacticalLivery.Livery livery) {
        Key key = new Key(art.layoutW(), art.layoutH(), art.density(), livery);
        synchronized (CACHE) {
            DeviceArt.Plate plate = CACHE.get(key);
            if (plate == null) {
                plate = DeviceArt.buildUncached(art.layoutW(), art.layoutH(), art.density(), livery);
                CACHE.put(key, plate);
            }
            return plate;
        }
    }

    /** Drops the cached frames (logout, window resize). */
    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    /**
     * Draws the backdrop (alpha {@code f.backdrop()}) and, while the frame is on screen, the D2
     * frame: opaque dark glass while opening, a clear glass well while putting it away.
     */
    public static void drawFrame(GuiGraphics graphics, Font font, TabletPath2D.MapFrame f,
                                 TacticalLivery.Livery livery, int linkColor) {
        DeviceArt.drawBackdrop(graphics, f.w(), f.h(), (float) f.backdrop());
        if (!f.visible()) {
            return;
        }
        Art art = art(f);
        if (art.layoutW() <= 0 || art.layoutH() <= 0) {
            return;
        }
        DeviceArt.Plate plate = plate(art, livery);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(art.originX(), art.originY(), 0.0F);
        if (art.cs() != 1) {
            pose.scale(art.cs(), art.cs(), 1.0F);
        }
        DeviceArt.drawSleepingDevice(graphics, font, plate, livery, linkColor,
                f.glass() == TabletPath2D.MapGlass.CLEAR, List.of());
        pose.popPose();
    }

    /**
     * Over the drawn page while opening: the sleep layer (GLASS, alpha {@code sleepAlpha}) and the
     * scan line ({@code cs} map pixels of LIGHT); while putting it away: the clear glass (GLASS
     * alpha 0.20) over the glass opening.
     */
    public static void drawEffects(GuiGraphics graphics, TabletPath2D.MapFrame f) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, EFFECT_Z);
        TabletPath2D.Box inner = f.inner();
        if (f.content()) {
            if (f.sleepAlpha() > 0.0D) {
                fill(graphics, inner, DeviceArt.GLASS, f.sleepAlpha());
            }
            if (f.hasScan()) {
                graphics.fill(inner.x(), f.scanY(), inner.right(), f.scanY() + Math.max(1, f.cs()),
                        TabletAnimationModel.LIGHT);
            }
        } else if (f.visible()) {
            fill(graphics, f.s(), DeviceArt.GLASS, f.sleepAlpha());
        }
        pose.popPose();
    }

    /** Fills {@code box} with {@code argb} at {@code alpha} times its own alpha. */
    static void fill(GuiGraphics graphics, TabletPath2D.Box box, int argb, double alpha) {
        int a = (int) Math.round(((argb >>> 24) & 0xFF) * Math.max(0.0D, Math.min(1.0D, alpha)));
        if (a <= 0 || box.w() <= 0 || box.h() <= 0) {
            return;
        }
        graphics.fill(box.x(), box.y(), box.right(), box.bottom(), (a << 24) | (argb & 0xFFFFFF));
    }
}
