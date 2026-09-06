package com.wok.infantry.client.map;

import com.wok.infantry.WokInfantryMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Process-local client registry for tactical-map area overlays supplied by optional addons. */
public final class TacticalMapAreaOverlayRegistry {
    private static final int MAX_OVERLAYS = 512;
    private static final CopyOnWriteArrayList<TacticalMapAreaOverlayProvider> PROVIDERS =
            new CopyOnWriteArrayList<>();

    private TacticalMapAreaOverlayRegistry() {
    }

    public static void register(TacticalMapAreaOverlayProvider provider) {
        TacticalMapAreaOverlayProvider value = Objects.requireNonNull(provider, "provider");
        if (!PROVIDERS.contains(value)) {
            PROVIDERS.add(value);
        }
    }

    public static void unregister(TacticalMapAreaOverlayProvider provider) {
        PROVIDERS.remove(provider);
    }

    public static List<TacticalMapAreaOverlay> overlays() {
        List<TacticalMapAreaOverlay> result = new ArrayList<>();
        for (TacticalMapAreaOverlayProvider provider : PROVIDERS) {
            try {
                List<TacticalMapAreaOverlay> supplied = provider.overlays();
                if (supplied == null) {
                    continue;
                }
                for (TacticalMapAreaOverlay overlay : supplied) {
                    if (overlay != null) {
                        result.add(overlay);
                        if (result.size() >= MAX_OVERLAYS) {
                            return List.copyOf(result);
                        }
                    }
                }
            } catch (RuntimeException exception) {
                WokInfantryMod.LOGGER.warn("Tactical map area overlay provider failed", exception);
            }
        }
        return List.copyOf(result);
    }
}
