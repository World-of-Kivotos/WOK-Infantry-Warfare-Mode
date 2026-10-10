package com.wok.infantry.client.tablet;

/**
 * The fade-out of a cut sound (DESIGN 3.6 S5, {@code a.sfx.cut}): the gain falls with a
 * {@value TabletAnimationModel#SFX_CUT_TAU_MS} ms time constant and the sound stops from
 * {@value TabletAnimationModel#SFX_CUT_STOP_MS} ms after the cut on. Pure.
 *
 * <p>The game can only change a playing sound's volume when the sound engine ticks it (20 times a
 * second, DESIGN 7 #32), so the curve is sampled once per tick: each tick sets the volume to
 * {@code exp(−t/τ)} (0 from 60 ms on), and the sound stops on the first tick from 60 ms on whose
 * previous tick already left it below {@value #SILENT} of its volume. So it is never stopped
 * from an audible level (a hard stop clicks); a stop lands 1–2 ticks after the cut (about 60–150
 * ms instead of the preview's exact 60).
 */
public final class TabletSoundFade {
    /** Below this fraction of the start volume a stop is inaudible. */
    public static final double SILENT = 0.02D;

    /**
     * What one tick does.
     *
     * @param gain fraction of the start volume this tick (0 when stopping)
     * @param stop stop the sound now
     */
    public record Step(double gain, boolean stop) {
        public static final Step STOP = new Step(0.0D, true);
    }

    /**
     * The volume of one playing sound, tick by tick ({@link TabletSoundInstance} keeps one; pure so
     * the tick sequence can be tested without a sound engine).
     */
    public static final class Fader {
        private final double startVolume;
        private double volume;
        private double fadeAt = Double.NaN;
        private double lastGain = 1.0D;
        private boolean stopped;

        public Fader(double startVolume) {
            this.startVolume = startVolume;
            this.volume = startVolume;
        }

        /** Starts the fade at {@code now} (once; a later cut keeps the first time). */
        public void fadeOut(double now) {
            if (Double.isNaN(fadeAt)) {
                fadeAt = now;
            }
        }

        public boolean fading() {
            return !Double.isNaN(fadeAt);
        }

        /** One engine tick at {@code now}; returns whether the sound must stop now. */
        public boolean tick(double now) {
            if (stopped) {
                return true;
            }
            if (!fading()) {
                return false;
            }
            Step step = step(now - fadeAt, lastGain);
            if (step.stop()) {
                stopped = true;
                volume = 0.0D;
                return true;
            }
            lastGain = step.gain();
            volume = startVolume * step.gain();
            return false;
        }

        /** The volume for this tick. */
        public double volume() {
            return volume;
        }

        public boolean stopped() {
            return stopped;
        }
    }

    private TabletSoundFade() {
    }

    /**
     * The step for a tick {@code sinceCutMs} after the cut; {@code lastGain}: the gain the
     * previous tick left (1 before the first tick after the cut).
     */
    public static Step step(double sinceCutMs, double lastGain) {
        double t = Math.max(0.0D, sinceCutMs);
        if (t >= TabletAnimationModel.SFX_CUT_STOP_MS) {
            return lastGain < SILENT ? Step.STOP : new Step(0.0D, false);
        }
        return new Step(gain(t), false);
    }

    /** {@code exp(−t/τ)}, 1 at the cut. */
    public static double gain(double sinceCutMs) {
        return Math.exp(-Math.max(0.0D, sinceCutMs) / TabletAnimationModel.SFX_CUT_TAU_MS);
    }
}
