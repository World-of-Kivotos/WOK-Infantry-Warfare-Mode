package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.TacticalPalette;

/**
 * uiTest-only bridge to the package-private HUD probe summaries, so the acceptance can compute
 * what an untouched HUD reports and compare it with a capture. Lives in the uiTest source set and
 * never ships.
 */
public final class UiTestHud {
    private UiTestHud() {
    }

    /**
     * The stamina bar's probe summary of {@code state} ({@code arms=… legs=…}, bright blink phase)
     * as the A palette draws it, whatever palette is active: the reference a capture taken after
     * a tablet terminal was open must still match.
     */
    public static String staminaSummaryInA(StaminaBarModel.State state) {
        try (TacticalPalette.Applied a = TacticalPalette.push(TacticalPalette.A)) {
            return StaminaHudOverlay.summary("arms", StaminaBarModel.arms(state, true)) + " "
                    + StaminaHudOverlay.summary("legs", StaminaBarModel.legs(state, true));
        }
    }
}
