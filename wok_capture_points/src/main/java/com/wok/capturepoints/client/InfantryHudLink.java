package com.wok.capturepoints.client;

import com.wok.capturepoints.WokCapturePointsMod;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Optional link to WOK步战核心's client HUD API ({@code com.wok.infantry.client.hud.InfantryHudApi}),
 * by reflection only, so this add-on keeps loading without the core:
 * <ul>
 *     <li>{@code rendersCapturePoints()} (core 0.5.0-beta.2+): true while the core shows the point
 *     in its battle strip; this add-on then draws nothing. Missing (older core) or false (the
 *     core's {@code hud.showBattleStrip} is off): this add-on draws its thin strip.</li>
 *     <li>{@code slot("top_center_next", w, h)} (core 0.4.0-beta.1+): where the thin strip goes
 *     under the core's top plates.</li>
 * </ul>
 * Each call that fails is logged once and then treated as missing. Client render thread only.
 */
final class InfantryHudLink {
    static final String CORE_MOD_ID = "wok_infantry";
    static final String API_CLASS = "com.wok.infantry.client.hud.InfantryHudApi";
    static final String TOP_CENTER_NEXT = "top_center_next";

    private static boolean resolved;
    private static boolean coreLoaded;
    private static Method rendersCapturePoints;
    private static Method slot;

    private InfantryHudLink() {
    }

    static boolean coreLoaded() {
        resolve();
        return coreLoaded;
    }

    /** Whether the installed core shows the capture point itself this frame. */
    static boolean coreRendersCapturePoints() {
        resolve();
        Method method = rendersCapturePoints;
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            WokCapturePointsMod.LOGGER.error("WOK Infantry HUD API rendersCapturePoints() failed; "
                    + "the capture strip is drawn by this add-on from now on.", exception);
            rendersCapturePoints = null;
            return false;
        }
    }

    /** The core's {@code top_center_next} slot in GUI pixels, or null. */
    static int[] topCenterNext(int guiWidth, int guiHeight) {
        resolve();
        Method method = slot;
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(null, TOP_CENTER_NEXT, guiWidth, guiHeight);
            return result instanceof int[] rect && rect.length == 4 ? rect : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            WokCapturePointsMod.LOGGER.error("WOK Infantry HUD API slot() failed; the capture "
                    + "strip keeps its own place from now on.", exception);
            slot = null;
            return null;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        ModList mods = ModList.get();
        if (mods == null) {
            return;
        }
        resolved = true;
        coreLoaded = mods.isLoaded(CORE_MOD_ID);
        if (!coreLoaded) {
            return;
        }
        Class<?> api;
        try {
            api = Class.forName(API_CLASS);
        } catch (ReflectiveOperationException | LinkageError exception) {
            WokCapturePointsMod.LOGGER.info("Installed WOK Infantry has no InfantryHudApi; the "
                    + "capture strip keeps its own place.");
            return;
        }
        try {
            slot = api.getMethod("slot", String.class, int.class, int.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            slot = null;
        }
        try {
            rendersCapturePoints = api.getMethod("rendersCapturePoints");
        } catch (ReflectiveOperationException | LinkageError exception) {
            rendersCapturePoints = null;
            WokCapturePointsMod.LOGGER.info("Installed WOK Infantry does not show capture points "
                    + "in its battle strip (before 0.5.0-beta.2); this add-on draws its own "
                    + "strip.");
        }
    }
}
