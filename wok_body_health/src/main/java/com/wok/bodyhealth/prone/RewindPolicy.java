package com.wok.bodyhealth.prone;

/**
 * How a gun mod compensates latency and sweeps the target box.
 *
 * @param hitboxOffset TaCZ {@code SERVER_HITBOX_OFFSET}; the box is swept by v and moved by
 *                     {@code (offset - 5) * v}
 * @param maxSaveTicks how many history entries the gun mod keeps, capped by the caller to
 *                     {@link ProneHistory#CAPACITY}
 */
public record RewindPolicy(boolean latencyCompensation, double hitboxOffset, int maxSaveTicks) {
    /** SBW always rewinds, keeps 20 ticks and moves the box by -2v. */
    public static final RewindPolicy SBW = new RewindPolicy(true, 3.0D, 20);

    public double shiftFactor() {
        return hitboxOffset - 5.0D;
    }
}
