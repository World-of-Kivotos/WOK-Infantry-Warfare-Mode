package com.wok.bodyhealth.prone;

/**
 * Config values the pure layer needs, assembled from {@code BodyHealthConfig} by the glue.
 *
 * @param headMargin          added to every head half-axis (hat layer plus helmet)
 * @param torsoMargin         added to every torso half-axis (jacket layer plus vest)
 * @param limbMargin          added to every arm and leg half-axis (sleeve and trouser layers)
 * @param transitionLagTicks  ticks subtracted from TAA phase progress
 * @param vanillaForwardShift f offset of the vanilla crawl model (-0.4 with tacz-tweaks' visual tweak)
 * @param sbwLegShots         whether SBW hits on a leg segment keep SBW's leg-shot effects
 */
public record ProneHitSettings(boolean enabled, double headMargin, double torsoMargin, double limbMargin,
                               int transitionLagTicks, double vanillaForwardShift,
                               boolean sbwLegShots, boolean debugLog) {
    public static final ProneHitSettings DEFAULTS =
            new ProneHitSettings(true, 0.0625D, 0.03125D, 0.015625D, 0, -0.4D, true, false);
    /** Used while the config is not loaded yet. */
    public static final ProneHitSettings DISABLED =
            new ProneHitSettings(false, 0.0625D, 0.03125D, 0.015625D, 0, -0.4D, true, false);

    public double margin(SegmentId id) {
        return switch (id) {
            case HEAD -> headMargin;
            case TORSO -> torsoMargin;
            default -> limbMargin;
        };
    }

    /** Largest of the three margins. */
    public double maxMargin() {
        return Math.max(headMargin, Math.max(torsoMargin, limbMargin));
    }
}
