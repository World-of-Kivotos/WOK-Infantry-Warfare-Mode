package com.wok.infantry.client.hud;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.screen.UiRect;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Optional WOK步战附属-占点 bridge (client only): whether its capture panel is on screen and
 * where, so the core HUD can move out of its way. The core never links against the add-on: it
 * checks {@link ModList} first, then reflects; any failure is logged once and the panel is
 * treated as hidden.
 *
 * <p>Preferred source is a future {@code com.wok.capturepoints.api.CaptureHudApi.panelRect(int,
 * int)} returning {@code {left, top, width, height}} in GUI pixels (null while hidden). Until the
 * add-on provides it, the panel rectangle mirrors {@code CaptureHudOverlay} of 0.1.0-alpha.3
 * (lines 28–36 and 66–73): with the core installed, screens up to 360 wide get the compact panel
 * at [140, w − 8) × [26, 61), wider screens the wide panel centred at
 * {@code pw = min(330, max(190, w − 24))} × [28, 80). Keep {@link #mirroredPanel} in step with the
 * add-on; {@code CaptureHudBridgeTest} pins these numbers.
 */
public final class CaptureHudBridge {
    public static final String CAPTURE_MOD_ID = "wok_capture_points";
    static final String STATE_CLASS = "com.wok.capturepoints.client.ClientCaptureState";
    static final String API_CLASS = "com.wok.capturepoints.api.CaptureHudApi";
    /** Mirrored alpha.3 geometry, GUI pixels. */
    static final int COMPACT_MAX_WIDTH = 360;
    static final int COMPACT_LEFT = 140;
    static final int COMPACT_RIGHT_INSET = 8;
    static final int COMPACT_TOP = 26;
    static final int COMPACT_HEIGHT = 35;
    static final int WIDE_TOP = 28;
    static final int WIDE_HEIGHT = 52;
    static final int WIDE_MIN_WIDTH = 190;
    static final int WIDE_MAX_WIDTH = 330;
    static final int WIDE_MARGIN = 24;

    private static boolean resolved;
    private static Method insidePoint;
    private static Method panelRect;

    private CaptureHudBridge() {
    }

    /**
     * The capture panel in GUI pixels while it is drawn, otherwise null. Call from the client
     * render thread only.
     */
    public static UiRect panelRect(int guiWidth, int guiHeight) {
        if (!resolve()) {
            return null;
        }
        try {
            if (panelRect != null) {
                Object result = panelRect.invoke(null, guiWidth, guiHeight);
                return result instanceof int[] slot && slot.length == 4 && slot[2] > 0
                        && slot[3] > 0 ? UiRect.ofSize(slot[0], slot[1], slot[2], slot[3]) : null;
            }
            return insidePoint != null && insidePoint.invoke(null) != null
                    ? mirroredPanel(guiWidth) : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            WokInfantryMod.LOGGER.error("WOK Capture Points HUD bridge failed; the core HUD no "
                    + "longer moves out of the capture panel's way.", exception);
            insidePoint = null;
            panelRect = null;
            return null;
        }
    }

    /** The alpha.3 capture panel rectangle (GUI pixels) for a {@code guiWidth} wide screen. */
    public static UiRect mirroredPanel(int guiWidth) {
        if (guiWidth <= COMPACT_MAX_WIDTH) {
            return UiRect.of(COMPACT_LEFT, COMPACT_TOP, Math.max(COMPACT_LEFT,
                    guiWidth - COMPACT_RIGHT_INSET), COMPACT_TOP + COMPACT_HEIGHT);
        }
        int width = Math.min(WIDE_MAX_WIDTH, Math.max(WIDE_MIN_WIDTH, guiWidth - WIDE_MARGIN));
        int left = (guiWidth - width) / 2;
        return UiRect.of(left, WIDE_TOP, left + width, WIDE_TOP + WIDE_HEIGHT);
    }

    private static boolean resolve() {
        if (!resolved) {
            resolved = true;
            if (ModList.get() != null && ModList.get().isLoaded(CAPTURE_MOD_ID)) {
                try {
                    panelRect = Class.forName(API_CLASS).getMethod("panelRect", int.class,
                            int.class);
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    panelRect = null;
                }
                if (panelRect == null) {
                    try {
                        insidePoint = Class.forName(STATE_CLASS).getMethod("insidePoint");
                    } catch (ReflectiveOperationException | LinkageError exception) {
                        WokInfantryMod.LOGGER.info("Installed WOK Capture Points exposes no "
                                + "capture state; the core HUD keeps its default places.");
                    }
                }
            }
        }
        return panelRect != null || insidePoint != null;
    }
}
