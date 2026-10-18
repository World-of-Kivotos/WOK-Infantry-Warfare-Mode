package com.wok.infantry.integration.journeymap;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.screen.TacticalMapScreen;
import journeymap.api.v2.client.IClientAPI;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.Objects;

/**
 * Keeps JourneyMap 6 in terrain-engine-only mode.
 *
 * <p>WOK步战 owns the player-facing tactical map. JourneyMap continues mapping and serving
 * terrain tiles through its public API, but its minimap is gated off and attempts to open its
 * fullscreen map are redirected to {@link TacticalMapScreen}.</p>
 */
public final class JourneyMapUiPolicy {
    static final String JOURNEYMAP_FULLSCREEN_CLASS =
            "journeymap.client.ui.fullscreen.Fullscreen";

    private static volatile IClientAPI api;

    private JourneyMapUiPolicy() {
    }

    public static void install(IClientAPI clientApi) {
        api = Objects.requireNonNull(clientApi, "clientApi");
        enforceMinimapHidden();
        WokInfantryMod.LOGGER.info(
                "JourneyMap native map UI hidden; WOK tactical map remains available on M");
    }

    /** Reasserts the API gate after JourneyMap connection/config lifecycle changes. */
    public static void enforceMinimapHidden() {
        IClientAPI currentApi = api;
        if (currentApi == null) {
            return;
        }
        try {
            if (currentApi.minimapEnabled()) {
                currentApi.toggleMinimap(false);
            }
        } catch (RuntimeException exception) {
            WokInfantryMod.LOGGER.warn("Could not suppress JourneyMap's native minimap", exception);
        }
    }

    public static boolean nativeMinimapHidden() {
        IClientAPI currentApi = api;
        if (currentApi == null) {
            return false;
        }
        try {
            return !currentApi.minimapEnabled();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen requested = event.getNewScreen();
        if (requested == null || !isJourneyMapFullscreen(requested.getClass().getName())) {
            return;
        }
        event.setNewScreen(new TacticalMapScreen(event.getCurrentScreen()));
    }

    static boolean isJourneyMapFullscreen(String className) {
        return JOURNEYMAP_FULLSCREEN_CLASS.equals(className);
    }

    static void resetForTests() {
        api = null;
    }
}
