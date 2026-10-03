package com.wok.bodyhealth.prone;

import net.minecraft.util.Mth;

/** Phase timing and yaw formulas of TAA's {@code ProneTacticalAnimationClient.visual}. */
public final class TaaPhaseMath {
    /** Per-tick share of TAA's 0.06 s exponential heading low-pass. */
    public static final double HEADING_ALPHA = 1.0D - Math.exp(-0.05D / 0.06D);

    public static double smoothstep(double x) {
        double c = Mth.clamp(x, 0.0D, 1.0D);
        return c * c * (3.0D - 2.0D * c);
    }

    /** Phase progress like {@code ProneClient.progress}, delayed by {@code lagTicks} and clamped to [0, 1]. */
    public static double progress(long sampleTime, int lagTicks, long start, int duration) {
        double t = (double) (sampleTime - lagTicks - start) / Math.max(2, duration);
        return Mth.clamp(t, 0.0D, 1.0D);
    }

    public static float reorientYaw(float anchor, float target, double t) {
        return (float) (anchor + Mth.wrapDegrees(target - anchor) * smoothstep((t - 0.16D) / 0.84D));
    }

    /** How far the turn-start pose has decayed to the neutral pose. */
    public static double reorientPoseWeight(double t) {
        return smoothstep(t / 0.82D);
    }

    /** Body yaw while getting up: the anchor hands over to the body yaw as {@code prone} drops. */
    public static float exitYaw(float anchor, float bodyYaw, double prone) {
        return (float) (anchor + Mth.wrapDegrees(bodyYaw - anchor) * smoothstep((1.0D - prone - 0.35D) / 0.65D));
    }

    public static double enterAimScale(double t) {
        return smoothstep((t - 0.4D) / 0.6D);
    }

    public static double enterAimWeight(double t) {
        return smoothstep((t - 0.65D) / 0.35D);
    }

    public static double exitAimWeight(double t) {
        return 1.0D - smoothstep(t / 0.24D);
    }

    public static float headingStep(float heading, float anchor) {
        return (float) (heading + Mth.wrapDegrees(anchor - heading) * HEADING_ALPHA);
    }

    private TaaPhaseMath() {
    }
}
