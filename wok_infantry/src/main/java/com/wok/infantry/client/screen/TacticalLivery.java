package com.wok.infantry.client.screen;

/**
 * Faction livery of the WOK步战 tablet: the device paint ({@link DeviceSkin}) and the screen
 * palette ({@link TacticalPalette}) of the viewer's side (P3, preview {@code 18-device-livery.js}):
 * the blue side is painted as the Academy (navy), the red side as Caesar (true red), and a viewer
 * without a faction sees the pale Neutral livery.
 *
 * <p>Until the faction resolver exists every screen is {@link Livery#NEUTRAL} (or the livery
 * pinned for UI acceptance) and every livery still resolves to the A palette.
 */
public final class TacticalLivery {
    /** The three paint schemes of the device and its screens. */
    public enum Livery {
        /** Blue side: navy case, blue selection. */
        ACADEMY,
        /** Red side: true-red case, deep crimson selection. */
        CAESAR,
        /** No faction yet (or a spectating administrator): pale steel case, graphite selection. */
        NEUTRAL;

        /** Screen palette of this livery in {@code scope}. */
        public TacticalPalette palette(Scope scope) {
            return TacticalPalette.A.withScope(scope);
        }

        /** Device paint of this livery. */
        public DeviceSkin skin() {
            return DeviceSkin.forLivery(this);
        }
    }

    /**
     * Page scope of a palette. A scope may override a few tokens on top of the livery, e.g. the
     * Caesar map selects in graphite so a red selection never reads as an enemy marker.
     */
    public enum Scope {
        /** Every board page (squad terminal, formation page, deployment mini-map). */
        BOARD,
        /** The tactical map page. */
        MAP
    }

    private static volatile Livery pinned;

    private TacticalLivery() {
    }

    /** Livery of the local viewer, read once per frame by {@link TacticalScreen}. */
    public static Livery current() {
        Livery pin = pinned;
        return pin != null ? pin : Livery.NEUTRAL;
    }

    /**
     * UI acceptance only: every screen uses {@code livery} regardless of the battle state;
     * {@code null} returns to the viewer's own livery.
     */
    public static void pinForAcceptance(Livery livery) {
        pinned = livery;
    }
}
