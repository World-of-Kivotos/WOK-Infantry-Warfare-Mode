package com.wok.bodyhealth.prone;

/**
 * Vanilla one-block-gap crawl without a TAA state ({@code k7_segments.js vanilla}). The limbs
 * use the envelope of the whole swim-stroke cycle because the server cannot know the arm phase
 * the client is drawing; the head is the box around its 45-degree forward tilt.
 */
public final class VanillaCrawlLayout {
    private static final BodyLayout ENVELOPE = new BodyLayout(new LocalObb[]{
            LocalObb.ofExtents(0.241D, 0.904D, -0.234D, 0.234D, 0.134D, 0.797D),    // HEAD
            LocalObb.ofExtents(-0.296D, 0.407D, -0.234D, 0.234D, 0.183D, 0.417D),   // TORSO
            LocalObb.ofExtents(-0.051D, 0.888D, 0.120D, 0.905D, -0.298D, 0.466D),   // RIGHT_ARM
            LocalObb.ofExtents(-0.051D, 0.888D, -0.905D, -0.120D, -0.298D, 0.466D), // LEFT_ARM
            LocalObb.ofExtents(-1.009D, -0.261D, -0.006D, 0.229D, -0.020D, 0.620D), // RIGHT_LEG
            LocalObb.ofExtents(-1.009D, -0.261D, -0.229D, 0.006D, -0.020D, 0.620D)  // LEFT_LEG
    });
    private static final double REACH = ENVELOPE.reach();

    /**
     * @param forwardShift added to every f; -0.4 matches tacz-tweaks' {@code crawl.visualTweak}
     *                     model offset, 0 the plain vanilla renderer
     */
    public static BodyLayout envelope(double forwardShift) {
        return ENVELOPE.shiftF(forwardShift);
    }

    /** Bound on {@link BodyLayout#reach} of {@link #envelope}; the shift moves every centre by at most its size. */
    public static double reach(double forwardShift) {
        return REACH + Math.abs(forwardShift);
    }

    private VanillaCrawlLayout() {
    }
}
