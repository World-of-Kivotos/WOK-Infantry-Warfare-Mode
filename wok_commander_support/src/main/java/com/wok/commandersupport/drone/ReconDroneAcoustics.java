package com.wok.commandersupport.drone;

/**
 * Engine loudness of the recon drone. The client loop plays without built-in attenuation, so
 * the wrapped engine's own 256-block fall-off does not apply; this linear fall-off makes the
 * engine audible for about "flight height + 64" blocks instead.
 */
public final class ReconDroneAcoustics {
    public static final float ENGINE_PITCH = 1.5F;
    public static final float ENGINE_MAX_VOLUME = 1.0F;
    /** Audible beyond the flight height above the target. */
    public static final double ENGINE_AUDIBLE_MARGIN = 64.0D;
    /** The engine dies away over this many ticks after a shoot-down. */
    public static final int CRASH_FADE_TICKS = 20;
    static final float CRASH_MIN_PITCH = 0.8F;

    private ReconDroneAcoustics() {
    }

    /** Distance at which the engine falls silent for a drone flying this high over the target. */
    public static double engineRange(double heightAboveTarget) {
        double height = Double.isFinite(heightAboveTarget)
                ? Math.max(0.0D, heightAboveTarget) : ReconDroneFlight.HEIGHT_ABOVE_TARGET;
        return height + ENGINE_AUDIBLE_MARGIN;
    }

    /**
     * Linear fall-off from full volume at the drone to silence at {@code range};
     * {@code crashTicks} is negative while the engine runs and fades it out after a shoot-down.
     */
    public static float engineVolume(double distance, double range, double crashTicks) {
        if (!Double.isFinite(distance) || !Double.isFinite(range) || range <= 0.0D
                || distance >= range) {
            return 0.0F;
        }
        double proximity = 1.0D - Math.max(0.0D, distance) / range;
        double fade = crashTicks < 0.0D ? 1.0D
                : Math.max(0.0D, 1.0D - crashTicks / CRASH_FADE_TICKS);
        return (float) (ENGINE_MAX_VOLUME * proximity * fade);
    }

    /** The engine winds down while the wreck falls. */
    public static float enginePitch(double crashTicks) {
        if (crashTicks < 0.0D) {
            return ENGINE_PITCH;
        }
        double fraction = Math.min(1.0D, crashTicks / CRASH_FADE_TICKS);
        return (float) (ENGINE_PITCH - (ENGINE_PITCH - CRASH_MIN_PITCH) * fraction);
    }

    /** True once the engine has faded out completely after a shoot-down. */
    public static boolean engineSilenced(double crashTicks) {
        return crashTicks >= CRASH_FADE_TICKS;
    }
}
