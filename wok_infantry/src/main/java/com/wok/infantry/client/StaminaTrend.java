package com.wok.infantry.client;

import com.wok.infantry.stamina.StaminaSnapshot;

import java.util.ArrayDeque;

/**
 * Recent history of the server's stamina snapshots for the stamina bar: the remnant of what was
 * just used (the highest value of the last {@value #GHOST_MILLIS} ms, so a draining pool shows a
 * short faded tail that follows it down and fades out once it stops), whether a pool is recovering
 * (it rose in the last {@value #RISE_MILLIS} ms; snapshots come every 0.1 s while values change)
 * and whether sprint was unlocked in the last {@value #UNLOCK_MILLIS} ms. Pure and thread-safe:
 * snapshots arrive on the network thread, the HUD reads on the render thread; times are passed in.
 */
public final class StaminaTrend {
    public static final long GHOST_MILLIS = 600L;
    public static final long RISE_MILLIS = 400L;
    public static final long UNLOCK_MILLIS = 1000L;
    /** Changes smaller than this are noise, not a drain or a recovery. */
    static final float EPSILON = 0.01F;
    private static final int MAX_SAMPLES = 64;

    /** Remnants (equal to the current value when nothing was just used) and flags of one moment. */
    public record View(float armsGhost, float legsGhost, boolean armsRising, boolean legsRising,
                       boolean unlocked) {
    }

    private record Sample(long at, float arms, float legs) {
    }

    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private StaminaSnapshot last;
    private boolean armsRose;
    private long armsRiseAt;
    private boolean legsRose;
    private long legsRiseAt;
    private boolean unlockSeen;
    private long unlockedAt;

    /** Records the snapshot that arrived at {@code now} (milliseconds of a monotonic clock). */
    public synchronized void record(StaminaSnapshot snapshot, long now) {
        if (snapshot == null) {
            return;
        }
        if (!snapshot.enabled()) {
            clear();
            last = snapshot;
            return;
        }
        if (last != null && last.enabled()) {
            if (snapshot.arms() > last.arms() + EPSILON) {
                armsRose = true;
                armsRiseAt = now;
            } else if (snapshot.arms() < last.arms() - EPSILON) {
                armsRose = false;
            }
            if (snapshot.legs() > last.legs() + EPSILON) {
                legsRose = true;
                legsRiseAt = now;
            } else if (snapshot.legs() < last.legs() - EPSILON) {
                legsRose = false;
            }
            if (last.sprintBlocked() && !snapshot.sprintBlocked()) {
                unlockSeen = true;
                unlockedAt = now;
            } else if (snapshot.sprintBlocked()) {
                unlockSeen = false;
            }
        }
        last = snapshot;
        samples.addLast(new Sample(now, snapshot.arms(), snapshot.legs()));
        prune(now);
    }

    /** The trend at {@code now}. */
    public synchronized View view(long now) {
        if (last == null) {
            return new View(0.0F, 0.0F, false, false, false);
        }
        float armsGhost = last.arms();
        float legsGhost = last.legs();
        for (Sample sample : samples) {
            if (now - sample.at() <= GHOST_MILLIS) {
                armsGhost = Math.max(armsGhost, sample.arms());
                legsGhost = Math.max(legsGhost, sample.legs());
            }
        }
        return new View(armsGhost, legsGhost, armsRose && now - armsRiseAt <= RISE_MILLIS,
                legsRose && now - legsRiseAt <= RISE_MILLIS,
                unlockSeen && !last.sprintBlocked() && now - unlockedAt < UNLOCK_MILLIS);
    }

    /** Forgets everything (disconnect, stamina switched off). */
    public synchronized void clear() {
        samples.clear();
        last = null;
        armsRose = false;
        legsRose = false;
        unlockSeen = false;
    }

    private void prune(long now) {
        while (samples.size() > 1 && (samples.size() > MAX_SAMPLES
                || now - samples.peekFirst().at() > GHOST_MILLIS)) {
            samples.removeFirst();
        }
    }
}
