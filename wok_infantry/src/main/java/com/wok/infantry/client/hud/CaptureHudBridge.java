package com.wok.infantry.client.hud;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.screen.UiRect;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Optional WOK步战附属-占点 bridge (client only). The core never links against the add-on: it
 * checks {@link ModList} first, then reflects; each call that fails is logged once and then
 * treated as missing.
 *
 * <p>Two things are read:
 * <ul>
 *     <li>{@link #objective()}: the point the viewer stands in, from
 *     {@code com.wok.capturepoints.api.CaptureHudApi.currentPoint()} (add-on 0.1.0-alpha.4+), shown
 *     by the core as the battle strip's objective tile. While the core can read it
 *     ({@link #readsObjective()}) and its strip is on,
 *     {@link InfantryHudApi#rendersCapturePoints()} tells the add-on so, and the add-on draws
 *     nothing itself.</li>
 *     <li>{@link #panelRect}: where the add-on's own capture HUD is on screen, so the core HUD
 *     moves out of its way. Preferred source is {@code CaptureHudApi.panelRect(int, int)}
 *     returning {@code {left, top, width, height}} in GUI pixels (null while the add-on draws
 *     nothing, e.g. while the core shows the point). Without it (add-on 0.1.0-alpha.3) the panel
 *     rectangle mirrors alpha.3's {@code CaptureHudOverlay} (lines 28–36 and 66–73): with the core
 *     installed, screens up to 360 wide get the compact panel at [140, w − 8) × [26, 61), wider
 *     screens the wide panel centred at {@code pw = min(330, max(190, w − 24))} × [28, 80). Keep
 *     {@link #mirroredPanel} in step with alpha.3; {@code CaptureHudBridgeTest} pins these
 *     numbers.</li>
 * </ul>
 *
 * <p>Fallbacks: when reading the point fails, the core stops showing it and
 * {@code rendersCapturePoints()} turns false, so the add-on draws its own strip again and reports
 * it through {@code panelRect}; when {@code panelRect} fails, the core keeps the alpha.3
 * rectangle while the viewer stands in a point (the existing avoidance).
 */
public final class CaptureHudBridge {
    public static final String CAPTURE_MOD_ID = "wok_capture_points";
    /** The add-on version whose panel {@link #mirroredPanel} mirrors. */
    static final String MIRRORED_VERSION = "0.1.0-alpha.3";
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
    private static Method currentPoint;
    private static Object lastMap;
    private static CaptureObjective lastObjective;
    private static boolean pinned;
    private static CaptureObjective pinnedObjective;

    private CaptureHudBridge() {
    }

    /**
     * UI acceptance only: shows {@code objective} as the point the viewer stands in, as if a new
     * add-on reported it, and reports no add-on panel; {@code null} returns to the add-on.
     */
    public static void pinForAcceptance(CaptureObjective objective) {
        pinnedObjective = objective;
        pinned = objective != null;
    }

    /**
     * The point the viewer stands in, or null (none, add-on missing or older than 0.1.0-alpha.4,
     * or its description broke the contract). Client render thread only.
     */
    public static CaptureObjective objective() {
        if (pinned) {
            return pinnedObjective;
        }
        if (!resolve() || currentPoint == null) {
            return null;
        }
        try {
            Object result = currentPoint.invoke(null);
            if (result == null) {
                lastMap = null;
                lastObjective = null;
                return null;
            }
            if (result == lastMap) {
                return lastObjective;
            }
            if (!(result instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("currentPoint() returned "
                        + result.getClass().getName());
            }
            CaptureObjective objective = CaptureObjective.fromMap(map);
            lastMap = result;
            lastObjective = objective;
            return objective;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            WokInfantryMod.LOGGER.error("WOK Capture Points HUD API currentPoint() failed; the "
                    + "battle strip no longer shows capture points and the add-on draws its "
                    + "own strip again.", exception);
            currentPoint = null;
            lastMap = null;
            lastObjective = null;
            return null;
        }
    }

    /**
     * Whether the core can read the point the viewer stands in (an add-on with
     * {@code CaptureHudApi.currentPoint()} that has not failed, or the acceptance pin).
     */
    public static boolean readsObjective() {
        return pinned || resolve() && currentPoint != null;
    }

    /**
     * The add-on's own capture HUD in GUI pixels while it is drawn, otherwise null. Call from the
     * client render thread only.
     */
    public static UiRect panelRect(int guiWidth, int guiHeight) {
        if (pinned || !resolve()) {
            return null;
        }
        if (panelRect != null) {
            try {
                Object result = panelRect.invoke(null, guiWidth, guiHeight);
                return result instanceof int[] slot && slot.length == 4 && slot[2] > 0
                        && slot[3] > 0 ? UiRect.ofSize(slot[0], slot[1], slot[2], slot[3])
                        : null;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                WokInfantryMod.LOGGER.error("WOK Capture Points HUD API panelRect() failed; the "
                        + "core HUD falls back to moving out of the 0.1.0-alpha.3 panel's way.",
                        exception);
                panelRect = null;
                insidePoint = method(STATE_CLASS, "insidePoint");
            }
        }
        if (insidePoint == null) {
            return null;
        }
        try {
            return insidePoint.invoke(null) != null ? mirroredPanel(guiWidth) : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            WokInfantryMod.LOGGER.error("WOK Capture Points HUD bridge failed; the core HUD no "
                    + "longer moves out of the capture panel's way.", exception);
            insidePoint = null;
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
            ModList mods = ModList.get();
            if (mods == null) {
                return false;
            }
            resolved = true;
            if (mods.isLoaded(CAPTURE_MOD_ID)) {
                currentPoint = method(API_CLASS, "currentPoint");
                try {
                    panelRect = Class.forName(API_CLASS).getMethod("panelRect", int.class,
                            int.class);
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    panelRect = null;
                }
                if (panelRect == null) {
                    insidePoint = method(STATE_CLASS, "insidePoint");
                    if (insidePoint == null) {
                        WokInfantryMod.LOGGER.info("Installed WOK Capture Points exposes no "
                                + "capture state; the core HUD keeps its default places.");
                    }
                    String version = mods.getModContainerById(CAPTURE_MOD_ID)
                            .map(container -> container.getModInfo().getVersion().toString())
                            .orElse("?");
                    if (insidePoint != null && !MIRRORED_VERSION.equals(version)) {
                        WokInfantryMod.LOGGER.info("WOK Capture Points {} has no "
                                + "CaptureHudApi.panelRect; the core HUD assumes its panel sits "
                                + "where {} draws it.", version, MIRRORED_VERSION);
                    }
                }
                if (currentPoint == null) {
                    WokInfantryMod.LOGGER.info("Installed WOK Capture Points has no "
                            + "CaptureHudApi.currentPoint (before 0.1.0-alpha.4); it keeps "
                            + "drawing its own capture HUD.");
                }
            }
        }
        return currentPoint != null || panelRect != null || insidePoint != null;
    }

    private static Method method(String className, String name) {
        try {
            return Class.forName(className).getMethod(name);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }
}
