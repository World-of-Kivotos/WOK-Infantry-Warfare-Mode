package com.wok.infantry.client.screen;

/**
 * A hardware key at an end of the device's bottom bezel: Esc on the left (acts like pressing
 * Esc) and R on the right (the screen's refresh action), registered with
 * {@link TacticalScreen#setBezelKeys}. Probe ids {@link TacticalBoardChrome#ESC_KEY_UI_ID} and
 * {@link TacticalBoardChrome#REFRESH_KEY_UI_ID}.
 *
 * <p>Not drawn yet: the screens still show these keys as footer hints.
 */
public final class BezelKey {
    /** Which end of the bezel the key sits on. */
    public enum Role {
        ESC,
        REFRESH
    }

    private BezelKey() {
    }
}
