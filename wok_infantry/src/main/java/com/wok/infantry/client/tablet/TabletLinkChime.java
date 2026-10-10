package com.wok.infantry.client.tablet;

import java.util.List;

/**
 * "Searching" then "ready" (DESIGN 3.6, preview {@code app.js} {@code A.searching}): when the
 * "ready" cue comes while the tablet has no battle link yet (first login, no snapshot), the
 * controller does not play it but starts this chime. It ticks
 * {@value TabletAnimationModel#SFX_SEARCH} every {@value TabletAnimationModel#SFX_SEARCH_EVERY_MS}
 * ms, at most {@value TabletAnimationModel#SFX_SEARCH_MAX_TICKS} times (review S9 c), and plays
 * "ready" once the link is up while the tablet is still opening or shown. A close or the end of
 * the animation stops it ({@link #stop}). Pure: the caller passes the time in milliseconds.
 */
public final class TabletLinkChime {
    /** The sound played when the link comes up. */
    public static final String READY = "ready";

    private boolean active;
    private double next;
    private int ticks;

    /** Starts searching at {@code atMs} (the first tick is due at once); no-op while searching. */
    public void start(double atMs) {
        if (active) {
            return;
        }
        active = true;
        next = atMs;
        ticks = 0;
    }

    /** Stops without a sound (closed, put away, reset). */
    public void stop() {
        active = false;
    }

    public boolean active() {
        return active;
    }

    /** Ticks played since the last {@link #start}. */
    public int ticks() {
        return ticks;
    }

    /**
     * The sounds due at {@code now}: "ready" when the link is up (then the chime ends; silent when
     * the tablet is no longer opening or shown), otherwise "search" when the next tick is due and
     * fewer than the maximum have played. After a missed tick (a long frame, e.g. while the world
     * loads on first login) the next one is due half an interval later at the earliest, so two
     * ticks never come back to back (the preview's {@code max(next + 500, now − 250)} would play
     * the missed one on the very next frame).
     */
    public List<String> update(double now, boolean linkOk, TabletMotion.State state) {
        if (!active) {
            return List.of();
        }
        if (linkOk) {
            active = false;
            return state == TabletMotion.State.OPENING || state == TabletMotion.State.SHOWN
                    ? List.of(READY) : List.of();
        }
        if (now >= next && ticks < TabletAnimationModel.SFX_SEARCH_MAX_TICKS) {
            ticks++;
            next = Math.max(next + TabletAnimationModel.SFX_SEARCH_EVERY_MS,
                    now + TabletAnimationModel.SFX_SEARCH_EVERY_MS / 2.0D);
            return List.of(TabletAnimationModel.SFX_SEARCH);
        }
        return List.of();
    }
}
