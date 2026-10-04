package com.wok.infantry.client.hud;

import com.wok.infantry.WokInfantryMod;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Optional WOK步战附属-部位血量 bridge. The stamina panel occupies the strip that
 * body health reserves under its figure; the core never links against it.
 * Called only from the client render thread.
 */
final class BodyHealthHudBridge {
    private static final String BODY_HEALTH_MOD_ID = "wok_body_health";
    private static final String API_CLASS = "com.wok.bodyhealth.api.BodyHealthHudApi";

    private static boolean resolved;
    private static Method companionSlot;

    /** Returns {@code {left, top, width, height}}, or null when the core should place the panel itself. */
    static int[] companionSlot(int screenWidth, int screenHeight) {
        Method method = resolve();
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(null, screenWidth, screenHeight);
            return result instanceof int[] slot && slot.length == 4 ? slot : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            WokInfantryMod.LOGGER.error(
                    "WOK Body Health HUD bridge failed; the stamina panel keeps its own position.",
                    exception);
            companionSlot = null;
            return null;
        }
    }

    /** Whether the body-health HUD is on screen this frame (it then hosts the stamina row). */
    static boolean visible(int screenWidth, int screenHeight) {
        return companionSlot(screenWidth, screenHeight) != null;
    }

    private static Method resolve() {
        if (!resolved) {
            resolved = true;
            if (ModList.get().isLoaded(BODY_HEALTH_MOD_ID)) {
                try {
                    companionSlot = Class.forName(API_CLASS)
                            .getMethod("companionSlot", int.class, int.class);
                } catch (ReflectiveOperationException | LinkageError exception) {
                    WokInfantryMod.LOGGER.info(
                            "Installed WOK Body Health has no HUD slot API; the stamina panel keeps its own position.");
                }
            }
        }
        return companionSlot;
    }

    private BodyHealthHudBridge() {
    }
}
