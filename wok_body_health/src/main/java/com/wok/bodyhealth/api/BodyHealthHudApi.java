package com.wok.bodyhealth.api;

import com.wok.bodyhealth.client.BodyHealthHudLayout;
import com.wok.bodyhealth.client.BodyHealthOverlay;
import net.minecraft.client.Minecraft;

/**
 * Client-only, reflection-friendly bridge for HUDs that sit in the strip
 * reserved under the body-health figure, such as WOK步战核心's stamina panel.
 * Call it only from client rendering code.
 */
public final class BodyHealthHudApi {
    /**
     * Returns {@code {left, top, width, height}} of the companion strip for this
     * frame, centred under the figure and kept left of the hotbar, or
     * {@code null} while the body-health HUD is hidden so the caller can use
     * its own placement.
     */
    public static int[] companionSlot(int screenWidth, int screenHeight) {
        BodyHealthHudLayout.Layout layout = BodyHealthOverlay.currentLayout(
                Minecraft.getInstance(), screenWidth, screenHeight);
        if (layout == null) {
            return null;
        }
        return new int[] {
                layout.companionLeft(),
                layout.companionTop(),
                BodyHealthHudLayout.COMPANION_WIDTH,
                BodyHealthHudLayout.COMPANION_HEIGHT
        };
    }

    private BodyHealthHudApi() {
    }
}
