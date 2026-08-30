package com.wok.infantry.integration.journeymap;

import journeymap.api.v2.client.IClientAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JourneyMapUiPolicyTest {
    @AfterEach
    void resetPolicy() {
        JourneyMapUiPolicy.resetForTests();
    }

    @Test
    void installDisablesTheNativeMinimapThroughThePublicApi() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        AtomicInteger toggles = new AtomicInteger();
        IClientAPI api = (IClientAPI) Proxy.newProxyInstance(
                IClientAPI.class.getClassLoader(), new Class<?>[]{IClientAPI.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "minimapEnabled" -> enabled.get();
                    case "toggleMinimap" -> {
                        toggles.incrementAndGet();
                        enabled.set((boolean) arguments[0]);
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
                });

        JourneyMapUiPolicy.install(api);

        assertFalse(enabled.get());
        assertTrue(JourneyMapUiPolicy.nativeMinimapHidden());
        assertEquals(1, toggles.get());
        JourneyMapUiPolicy.enforceMinimapHidden();
        assertEquals(1, toggles.get(), "an already-hidden minimap must not be toggled repeatedly");
    }

    @Test
    void onlyJourneyMapSixFullscreenIsRedirected() {
        assertTrue(JourneyMapUiPolicy.isJourneyMapFullscreen(
                "journeymap.client.ui.fullscreen.Fullscreen"));
        assertFalse(JourneyMapUiPolicy.isJourneyMapFullscreen(
                "journeymap.client.ui.waypointmanager.WaypointManager"));
        assertFalse(JourneyMapUiPolicy.isJourneyMapFullscreen(
                "com.wok.infantry.client.screen.TacticalMapScreen"));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
