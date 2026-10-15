package com.wok.infantry.client.tablet;

import com.wok.infantry.client.screen.TacticalBoardChrome;

/**
 * Marker of the tablet family (DESIGN 2.4): screens the "take out the tablet" animation plays for.
 * Implemented by {@code TacticalScreen} (squad pages, formation page, uiTest pages),
 * {@code TacticalMapScreen} and {@code PlayerLoadoutScreen}. Switching between two tablet screens
 * never animates; entering one from the world opens the tablet, leaving to the world puts it away.
 */
public interface TabletSurface {
    /** How the animation treats this screen; full-screen pages unless they say otherwise. */
    default TabletScreenKind tabletKind() {
        return TabletScreenKind.FULLSCREEN;
    }

    /** The link LED the raised 3D tablet shows for this screen (OK unless it waits for data). */
    default TacticalBoardChrome.LinkState tabletLink() {
        return TacticalBoardChrome.LinkState.OK;
    }

    /** The kind of {@code screen}, or {@code null} for no screen or a screen outside the family. */
    static TabletScreenKind kindOf(Object screen) {
        return screen instanceof TabletSurface surface ? surface.tabletKind() : null;
    }
}
