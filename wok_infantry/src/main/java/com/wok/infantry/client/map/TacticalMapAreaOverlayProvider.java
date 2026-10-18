package com.wok.infantry.client.map;

import java.util.List;

/** Optional addons implement this interface to place persistent areas on the tactical map. */
@FunctionalInterface
public interface TacticalMapAreaOverlayProvider {
    List<TacticalMapAreaOverlay> overlays();
}
