package com.wok.downed.state;

/**
 * Detects a short sneak tap from the per-tick sneak flag. The sneak held while starting the drag
 * (sneak + use) is ignored until it has been let go once, and a long hold (crouching behind cover)
 * is not a tap, so only a deliberate press-and-release counts.
 */
public final class SneakTap {
    public static final int MAX_TAP_TICKS = 10;

    private boolean armed;
    private int heldTicks;

    /** Feeds one tick of the sneak flag; returns true on the tick a tap is completed. */
    public boolean update(boolean sneaking) {
        if (sneaking) {
            if (armed) {
                heldTicks++;
            }
            return false;
        }
        boolean tapped = armed && heldTicks > 0 && heldTicks <= MAX_TAP_TICKS;
        armed = true;
        heldTicks = 0;
        return tapped;
    }
}
