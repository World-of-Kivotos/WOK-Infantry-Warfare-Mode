package com.wok.infantry.client.screen;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Device backdrop and glass hooks of {@link TacticalScreen}. */
class TacticalScreenDeviceTest {
    private static final class BareScreen extends TacticalScreen {
        BareScreen() {
            super(Component.literal("device"));
        }

        @Override
        protected void initTactical() {
        }
    }

    @Test
    void glassSitsBetweenTheModalAndTheTooltip() {
        // The modal is drawn at z 300 (TacticalScreen.MODAL_Z), the tooltip at TacticalTooltip.Z.
        assertTrue(TacticalScreen.GLASS_Z > 300.0F, "the glass covers an open modal");
        assertTrue(TacticalScreen.GLASS_Z < TacticalTooltip.Z, "tooltips stay above the glass");
    }

    @Test
    void noGlassWithoutADrawnDevice() {
        // A frame that never called drawShell (no layout yet) lays no glass over the world.
        BareScreen screen = new BareScreen();
        assertDoesNotThrow(() -> screen.renderGlassOverlay(null, 0.0F));
    }
}
