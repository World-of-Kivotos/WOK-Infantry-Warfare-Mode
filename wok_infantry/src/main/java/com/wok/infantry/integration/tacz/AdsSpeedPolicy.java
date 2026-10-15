package com.wok.infantry.integration.tacz;

/** Pure conversion rules for per-weapon ADS speed modifiers. */
final class AdsSpeedPolicy {
    static final int MIN_SLOWDOWN_PERCENT = 0;
    static final int MAX_SLOWDOWN_PERCENT = 90;

    private AdsSpeedPolicy() {
    }

    static float speedScaleForSlowdown(int slowdownPercent) {
        if (slowdownPercent < MIN_SLOWDOWN_PERCENT
                || slowdownPercent > MAX_SLOWDOWN_PERCENT) {
            throw new IllegalArgumentException("ADS slowdown must be between 0 and 90 percent");
        }
        return (100.0F - slowdownPercent) / 100.0F;
    }

    static float adjustedAimTime(float currentAimTime, float speedScale) {
        if (!Float.isFinite(currentAimTime) || currentAimTime <= 0.0F) {
            throw new IllegalArgumentException("ADS time must be finite and positive");
        }
        if (!Float.isFinite(speedScale) || speedScale <= 0.0F || speedScale > 1.0F) {
            throw new IllegalArgumentException("ADS speed scale must be in (0, 1]");
        }
        return currentAimTime / speedScale;
    }
}
