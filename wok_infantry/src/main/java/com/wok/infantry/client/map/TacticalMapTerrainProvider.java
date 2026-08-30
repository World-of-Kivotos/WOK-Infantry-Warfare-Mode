package com.wok.infantry.client.map;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.function.Consumer;

/**
 * Client terrain source for the WOK tactical map.
 *
 * <p>The provider must invoke the callback once. A non-null image transfers ownership to the
 * callback; the receiver is responsible for eventually closing it. A null image means that the
 * requested terrain is currently unavailable and the tactical map should keep its grid fallback.</p>
 */
public interface TacticalMapTerrainProvider {
    String id();

    boolean isReady();

    void requestTile(TacticalMapTerrainRequest request, Consumer<NativeImage> callback);
}
