package com.wok.bodyhealth.prone;

/** How a land-prone player is being drawn, which picks the hit layout. */
public enum ProneMode {
    /** Not lying on land; the original hitbox applies. */
    NONE,
    /** TAA phase 1: dropping from standing to prone. */
    TAA_ENTER,
    /** TAA phase 2: lying, possibly crawling. */
    TAA_PRONE,
    /** TAA phase 4: turning the body in place towards a new anchor. */
    TAA_REORIENT,
    /** TAA phase 3: getting back up. */
    TAA_EXIT,
    /** TAA is installed but its state cannot be read; assume a steady prone pose. */
    TAA_ASSUMED,
    /** Swimming pose without a TAA state, e.g. a one-block gap, drawn by the vanilla renderer. */
    VANILLA_CRAWL;

    public static final int TAA_PHASE_ENTER = 1;
    public static final int TAA_PHASE_PRONE = 2;
    public static final int TAA_PHASE_EXIT = 3;
    public static final int TAA_PHASE_REORIENT = 4;

    /** Maps a TAA {@code ProneServer$State.phase()}; unknown phases count as lying prone. */
    public static ProneMode fromTaaPhase(int phase) {
        return switch (phase) {
            case TAA_PHASE_ENTER -> TAA_ENTER;
            case TAA_PHASE_EXIT -> TAA_EXIT;
            case TAA_PHASE_REORIENT -> TAA_REORIENT;
            default -> TAA_PRONE;
        };
    }

    public boolean isTaa() {
        return this != NONE && this != VANILLA_CRAWL;
    }
}
