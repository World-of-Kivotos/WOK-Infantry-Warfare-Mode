package com.wok.infantry.client.screen;

/**
 * Pure value scale of a {@link TacticalBoardSlider}: maps values to track positions (0–1) and
 * back, snaps them and steps them for the keyboard and the mouse wheel.
 *
 * <ul>
 *   <li>{@link #linear} / {@link #mapped}: a continuous range, optionally snapped to multiples of
 *   {@code step} counted from {@code min}; {@code max} is always reachable even when it is not on
 *   the grid. {@link #LOGARITHMIC} spreads ratios such as 25%–1500% evenly.</li>
 *   <li>{@link #detents} ("整档"): exactly {@code count} positions {@code first + i·step}, evenly
 *   spaced on the track, so the knob always sits on a detent and one key press moves one detent
 *   (the ammo supply's "N 发" in whole packages).</li>
 * </ul>
 */
public final class TacticalSliderScale {
    /** Value ⇄ position mapping of a continuous scale. */
    public interface Mapping {
        /** Position 0–1 of {@code value} (already inside [min, max]). */
        double toPosition(double value, double min, double max);

        /** Value of {@code position} (0–1) before snapping. */
        double toValue(double position, double min, double max);
    }

    /** Even spacing. */
    public static final Mapping LINEAR = new Mapping() {
        @Override
        public double toPosition(double value, double min, double max) {
            return max > min ? (value - min) / (max - min) : 0.0D;
        }

        @Override
        public double toValue(double position, double min, double max) {
            return min + position * (max - min);
        }
    };

    /** Even spacing of ratios ({@code ln}); falls back to {@link #LINEAR} unless 0 &lt; min &lt; max. */
    public static final Mapping LOGARITHMIC = new Mapping() {
        @Override
        public double toPosition(double value, double min, double max) {
            if (!(min > 0.0D) || !(max > min) || !(value > 0.0D)) {
                return LINEAR.toPosition(value, min, max);
            }
            return (Math.log(value) - Math.log(min)) / (Math.log(max) - Math.log(min));
        }

        @Override
        public double toValue(double position, double min, double max) {
            if (!(min > 0.0D) || !(max > min)) {
                return LINEAR.toValue(position, min, max);
            }
            return Math.exp(Math.log(min) + position * (Math.log(max) - Math.log(min)));
        }
    };

    private final double min;
    private final double max;
    private final double step;
    private final Mapping mapping;
    /** Number of detents, or -1 for a continuous scale. */
    private final int detents;

    private TacticalSliderScale(double min, double max, double step, Mapping mapping, int detents) {
        this.min = min;
        this.max = max;
        this.step = step;
        this.mapping = mapping;
        this.detents = detents;
    }

    /** Continuous range snapped to {@code step} (0 = no snapping). */
    public static TacticalSliderScale linear(double min, double max, double step) {
        return mapped(min, max, step, LINEAR);
    }

    /** Continuous range with its own value ⇄ position mapping, snapped in value space. */
    public static TacticalSliderScale mapped(double min, double max, double step, Mapping mapping) {
        double low = finite(min, 0.0D);
        double high = Math.max(low, finite(max, low));
        double safeStep = finite(step, 0.0D) > 0.0D ? step : 0.0D;
        return new TacticalSliderScale(low, high, safeStep, mapping == null ? LINEAR : mapping, -1);
    }

    /**
     * Whole detents ("整档"): {@code count} values {@code first, first + step, …}. A count of 0
     * gives an empty scale (value {@code first}, the slider should be disabled).
     */
    public static TacticalSliderScale detents(double first, double step, int count) {
        double start = finite(first, 0.0D);
        double safeStep = finite(step, 0.0D) > 0.0D ? step : 1.0D;
        int safeCount = Math.max(0, count);
        double last = safeCount <= 1 ? start : start + (safeCount - 1) * safeStep;
        return new TacticalSliderScale(start, clean(last), safeStep, LINEAR, safeCount);
    }

    /**
     * Ammo-style detents: {@code step, 2·step, …} up to the largest multiple of {@code step} not
     * above {@code maximum}. Empty (value 0) when {@code maximum < step}.
     */
    public static TacticalSliderScale roundDetents(int step, int maximum) {
        int safeStep = Math.max(1, step);
        int count = Math.max(0, maximum) / safeStep;
        return count == 0 ? detents(0.0D, safeStep, 0) : detents(safeStep, safeStep, count);
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    /** Snapping grid (0 for none); the distance between detents for a detent scale. */
    public double step() {
        return step;
    }

    public Mapping mapping() {
        return mapping;
    }

    public boolean detented() {
        return detents >= 0;
    }

    /** Number of detents (0 for an empty detent scale, -1 for a continuous scale). */
    public int detentCount() {
        return detents;
    }

    /** True for a detent scale without any detent. */
    public boolean empty() {
        return detents == 0;
    }

    /** {@code value} inside [min, max]; NaN becomes {@code min}. */
    public double clamp(double value) {
        if (!(value >= min)) {
            return min;
        }
        return Math.min(max, value);
    }

    /** The valid value nearest to {@code value} (on the grid or detent). */
    public double snap(double value) {
        if (detented()) {
            return detentValue(detentIndex(value));
        }
        double clamped = clamp(value);
        if (step > 0.0D) {
            double onGrid = clamp(min + Math.round((clamped - min) / step) * step);
            // max counts as one more grid line, so it stays reachable when it is off the grid.
            clamped = Math.abs(max - clamped) < Math.abs(onGrid - clamped) ? max : onGrid;
        }
        return clean(clamped);
    }

    /** Track position 0–1 of {@code value}. */
    public double toPosition(double value) {
        if (detented()) {
            return detents <= 1 ? 0.0D : (double) detentIndex(value) / (detents - 1);
        }
        double position = mapping.toPosition(clamp(value), min, max);
        return Double.isFinite(position) ? Math.max(0.0D, Math.min(1.0D, position)) : 0.0D;
    }

    /** Snapped value of the track position {@code position} (clamped to 0–1). */
    public double fromPosition(double position) {
        double p = Double.isFinite(position) ? Math.max(0.0D, Math.min(1.0D, position)) : 0.0D;
        if (detented()) {
            return detents <= 1 ? detentValue(0) : detentValue((int) Math.round(p * (detents - 1)));
        }
        return snap(mapping.toValue(p, min, max));
    }

    /**
     * {@code steps} grid steps (or detents) away from {@code value}, clamped. A value between two
     * grid lines first moves to the neighbouring line in that direction, so no line is skipped.
     * Without a grid one step is 1% of the range.
     */
    public double offset(double value, int steps) {
        if (steps == 0) {
            return snap(value);
        }
        if (detented()) {
            return detentValue(detentIndex(value) + steps);
        }
        if (step <= 0.0D) {
            return clean(clamp(clamp(value) + steps * (max - min) / 100.0D));
        }
        double grid = (clamp(value) - min) / step;
        long base = steps > 0 ? (long) Math.floor(grid + 1.0E-9D) : (long) Math.ceil(grid - 1.0E-9D);
        return clean(clamp(min + (base + steps) * step));
    }

    /** Index of the detent nearest to {@code value} (0 for a continuous or empty scale). */
    public int detentIndex(double value) {
        if (detents <= 1) {
            return 0;
        }
        double safe = Double.isFinite(value) ? value : min;
        long index = Math.round((safe - min) / step);
        return (int) Math.max(0, Math.min(detents - 1, index));
    }

    /** Value of detent {@code index} (clamped to the existing detents). */
    public double detentValue(int index) {
        if (detents <= 1) {
            return min;
        }
        int safe = Math.max(0, Math.min(detents - 1, index));
        return clean(min + safe * step);
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    /** Removes binary noise such as 0.30000000000000004 from grid arithmetic. */
    static double clean(double value) {
        if (!Double.isFinite(value) || Math.abs(value) >= 1.0E6D) {
            return value;
        }
        return Math.rint(value * 1.0E9D) / 1.0E9D;
    }
}
