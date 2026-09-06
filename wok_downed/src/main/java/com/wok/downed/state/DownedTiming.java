package com.wok.downed.state;

public final class DownedTiming {
    public static int secondsToTicks(int seconds) {
        return Math.max(1, seconds) * 20;
    }

    public static int progressPercent(int totalTicks, int remainingTicks) {
        int safeTotal = Math.max(1, totalTicks);
        int elapsed = Math.max(0, safeTotal - Math.max(0, remainingTicks));
        return Math.min(100, Math.round(elapsed * 100.0F / safeTotal));
    }

    public static boolean movedTooFar(double distanceSquared, double tolerance) {
        double safeTolerance = Math.max(0.0D, tolerance);
        return distanceSquared > safeTolerance * safeTolerance;
    }

    private DownedTiming() {
    }
}
