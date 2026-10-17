package com.wok.infantry.integration.tacz;

/** Pure stance response shared with regression tests. Standing immediately loses the benefit. */
public final class ProneStability {
    public static float advance(float current, boolean prone, boolean moving, int settleTicks) {
        if (!prone) return 0;
        float step = 1.0F / Math.max(1, settleTicks);
        return Math.max(0, Math.min(1, current + (moving ? -2 * step : step)));
    }
    public static float recoilMultiplier(float nativeProne, float stability) {
        if (!Float.isFinite(nativeProne) || nativeProne < 0 || nativeProne >= 1) return nativeProne;
        return 1.0F + (nativeProne - 1.0F) * Math.max(0, Math.min(1, stability));
    }
    private ProneStability() {}
}
