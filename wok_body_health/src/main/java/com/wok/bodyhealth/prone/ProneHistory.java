package com.wok.bodyhealth.prone;

import java.util.Arrays;
import java.util.Objects;

/** Ring buffer of one player's snapshots; index 0 is the newest. Server thread only. */
public final class ProneHistory {
    /** Five seconds of ticks, which covers TaCZ's default 1000 ms rewind window with room to spare. */
    public static final int CAPACITY = 100;

    private final ProneSample[] ring = new ProneSample[CAPACITY];
    private int newest = -1;
    private int size;

    public void push(ProneSample s) {
        Objects.requireNonNull(s, "sample");
        newest = (newest + 1) % CAPACITY;
        ring[newest] = s;
        if (size < CAPACITY) {
            size++;
        }
    }

    public int size() {
        return size;
    }

    /** @throws IndexOutOfBoundsException unless {@code 0 <= ticksBack < size()} */
    public ProneSample get(int ticksBack) {
        Objects.checkIndex(ticksBack, size);
        return ring[Math.floorMod(newest - ticksBack, CAPACITY)];
    }

    /** Newest snapshot, or null when empty. */
    public ProneSample latest() {
        return size == 0 ? null : ring[newest];
    }

    public void clear() {
        Arrays.fill(ring, null);
        newest = -1;
        size = 0;
    }
}
