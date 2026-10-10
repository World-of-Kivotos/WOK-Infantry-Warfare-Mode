package com.wok.infantry.client.tablet;

import com.wok.infantry.client.tablet.TabletMotion.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vanilla vignette on scheme A ({@link TabletHudPolicy#hidesVignette}): it would darken the 3D
 * tablet but not the 2D device that takes over at READ, so on scheme A it leaves and returns with
 * the HUD; schemes B, quick and off keep it as part of the world (B2's white list).
 */
class TabletHudVignetteTest {
    @Test
    void schemeAHidesTheVignetteWithTheHud() {
        for (double p : new double[]{0.0D, 0.3D, 0.61D, 0.9D, 0.999D}) {
            assertTrue(TabletHudPolicy.hidesVignette(State.OPENING, TabletPath.A3D, p, true), "open " + p);
        }
        assertFalse(TabletHudPolicy.hidesVignette(State.OPENING, TabletPath.A3D, 1.0D, true),
                "p = 1 is the static picture");
        assertFalse(TabletHudPolicy.hidesVignette(State.SHOWN, TabletPath.A3D, 1.0D, true),
                "shown: as on every other setting");
        assertTrue(TabletHudPolicy.hidesVignette(State.CLOSING, TabletPath.A3D, 1.0D, false));
        assertTrue(TabletHudPolicy.hidesVignette(State.CLOSING, TabletPath.A3D, 0.5D, false));
        assertTrue(TabletHudPolicy.hidesVignette(State.CLOSING, TabletPath.A3D, 0.18D, false));
        assertFalse(TabletHudPolicy.hidesVignette(State.CLOSING, TabletPath.A3D, 0.17D, false),
                "back with the HUD at K0");
        assertFalse(TabletHudPolicy.hidesVignette(State.IDLE, TabletPath.A3D, 0.0D, false));
    }

    @Test
    void otherPathsKeepTheVignette() {
        for (TabletPath path : new TabletPath[]{TabletPath.B2D, TabletPath.QUICK, TabletPath.OFF}) {
            assertFalse(TabletHudPolicy.hidesVignette(State.OPENING, path, 0.5D, true), path.name());
            assertFalse(TabletHudPolicy.hidesVignette(State.SHOWN, path, 1.0D, true), path.name());
        }
        assertFalse(TabletHudPolicy.cancels(TabletHudPolicy.VIGNETTE, TabletHudPolicy.Visibility.ALL),
                "the white list itself still keeps it");
    }
}
