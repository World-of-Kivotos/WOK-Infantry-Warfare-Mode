package com.wok.commandersupport.drone;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Deterministic flight path of the recon drone, computed identically on the server and on every
 * client from a few synchronised parameters and the level game time.
 *
 * <p>The drone circles the target at a fixed radius and altitude; after its last mission step
 * it rolls out of the turn and climbs away until it removes itself. A drone that is shot down
 * keeps its forward momentum and falls under gravity for at most {@link #CRASH_MAX_TICKS}.
 * Every function here is pure: no level, chunk or entity is read.</p>
 *
 * <p>Angles follow Minecraft: yaw 0 faces +Z (south) and grows towards -X (west). A clockwise
 * orbit (seen from above with north up) moves from east to south, so its heading yaw grows and
 * the drone banks right, towards the centre.</p>
 */
public final class ReconDroneFlight {
    public static final double ORBIT_RADIUS = 24.0D;
    /** Cruise speed along the orbit, in blocks per tick. */
    public static final double CRUISE_SPEED = 0.6D;
    /** Minimum height above the target surface. */
    public static final int HEIGHT_ABOVE_TARGET = 64;
    /** Minimum clearance above the highest sampled surface under the orbit. */
    public static final int CLEARANCE_ABOVE_ORBIT = 40;
    /** The orbit stays this far below the build limit. */
    public static final int CEILING_MARGIN = 16;
    public static final int HEIGHT_SAMPLES = 16;
    /** Half the hit box width: chunks under the wing tips must be ready as well. */
    public static final double AIRFRAME_MARGIN = 2.0D;
    public static final float ORBIT_BANK_DEGREES = 22.0F;
    /** Ticks the drone flies away after its last mission step before removing itself. */
    public static final int DEPARTURE_TICKS = 100;
    public static final double DEPARTURE_SPEED = 0.9D;
    public static final double DEPARTURE_CLIMB_PER_TICK = 0.15D;
    static final int DEPARTURE_ACCELERATION_TICKS = 30;
    static final int DEPARTURE_ROLL_OUT_TICKS = 20;
    static final int DEPARTURE_PITCH_UP_TICKS = 10;
    static final float DEPARTURE_PITCH_UP_DEGREES = 6.0F;
    /** Longest fall after a shoot-down before the wreck is removed in the air. */
    public static final int CRASH_MAX_TICKS = 100;
    static final double CRASH_GRAVITY = 0.045D;
    static final double CRASH_HORIZONTAL_DRAG = 0.985D;
    static final double CRASH_VERTICAL_DRAG = 0.98D;
    static final float CRASH_ROLL_DEGREES_PER_TICK = 9.0F;
    static final float CRASH_PITCH_DEGREES_PER_TICK = 4.0F;
    static final float CRASH_MAX_PITCH_DEGREES = 60.0F;

    private static final double TWO_PI = Math.PI * 2.0D;

    private ReconDroneFlight() {
    }

    /**
     * Synchronised orbit parameters. {@code phase} is the start angle in radians measured from
     * +X towards +Z; a clockwise orbit increases it.
     */
    public record Orbit(double centerX, double centerZ, double radius, double altitude,
                        double phase, boolean clockwise) {
        public Orbit {
            if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)
                    || !Double.isFinite(radius) || !Double.isFinite(altitude)
                    || !Double.isFinite(phase)) {
                throw new IllegalArgumentException("Drone orbit must be finite");
            }
            if (radius <= 0.0D) {
                throw new IllegalArgumentException("Drone orbit radius must be positive");
            }
        }

        /** +1 for clockwise (seen from above), -1 otherwise. */
        public int direction() {
            return clockwise ? 1 : -1;
        }

        public double angularSpeed() {
            return CRUISE_SPEED / radius;
        }
    }

    /** Position and velocity (blocks per tick) at one instant. */
    public record Pose(double x, double y, double z, double vx, double vy, double vz) {
        /** Heading of the horizontal velocity; 0 when the drone has no horizontal speed. */
        public float yawDegrees() {
            if (Math.abs(vx) < 1.0E-9D && Math.abs(vz) < 1.0E-9D) {
                return 0.0F;
            }
            return (float) (Math.toDegrees(Math.atan2(vz, vx)) - 90.0D);
        }
    }

    /** Position on the orbit {@code ticks} after launch (fractional ticks are allowed). */
    public static Pose orbitPose(Orbit orbit, double ticks) {
        double angle = orbit.phase() + orbit.direction() * orbit.angularSpeed() * ticks;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double speed = orbit.direction() * CRUISE_SPEED;
        return new Pose(orbit.centerX() + orbit.radius() * cos, orbit.altitude(),
                orbit.centerZ() + orbit.radius() * sin, -speed * sin, 0.0D, speed * cos);
    }

    /**
     * Powered flight: the orbit until {@code departTick} (negative while still on station),
     * then a straight climbing departure along the heading the drone had at that tick.
     */
    public static Pose flightPose(Orbit orbit, int departTick, double ticks) {
        if (departTick < 0 || ticks <= departTick) {
            return orbitPose(orbit, ticks);
        }
        Pose start = orbitPose(orbit, departTick);
        double elapsed = ticks - departTick;
        double horizontal = Math.hypot(start.vx(), start.vz());
        double unitX = start.vx() / horizontal;
        double unitZ = start.vz() / horizontal;
        double distance = departureDistance(elapsed);
        double speed = departureSpeed(elapsed);
        return new Pose(start.x() + unitX * distance,
                start.y() + DEPARTURE_CLIMB_PER_TICK * elapsed,
                start.z() + unitZ * distance,
                unitX * speed, DEPARTURE_CLIMB_PER_TICK, unitZ * speed);
    }

    /** Ground distance covered {@code elapsed} ticks into the departure. */
    static double departureDistance(double elapsed) {
        double acceleration = (DEPARTURE_SPEED - CRUISE_SPEED) / DEPARTURE_ACCELERATION_TICKS;
        if (elapsed <= DEPARTURE_ACCELERATION_TICKS) {
            return CRUISE_SPEED * elapsed + 0.5D * acceleration * elapsed * elapsed;
        }
        double accelerated = CRUISE_SPEED * DEPARTURE_ACCELERATION_TICKS
                + 0.5D * acceleration * DEPARTURE_ACCELERATION_TICKS
                * DEPARTURE_ACCELERATION_TICKS;
        return accelerated + DEPARTURE_SPEED * (elapsed - DEPARTURE_ACCELERATION_TICKS);
    }

    static double departureSpeed(double elapsed) {
        double fraction = Math.min(1.0D, Math.max(0.0D,
                elapsed / DEPARTURE_ACCELERATION_TICKS));
        return CRUISE_SPEED + (DEPARTURE_SPEED - CRUISE_SPEED) * fraction;
    }

    /**
     * Unpowered fall {@code ticks} whole ticks after a shoot-down that happened at
     * {@code start}: drag slows the forward momentum while gravity pulls the wreck down.
     */
    public static Pose crashPose(Pose start, int ticks) {
        int steps = Math.max(0, Math.min(CRASH_MAX_TICKS, ticks));
        double x = start.x();
        double y = start.y();
        double z = start.z();
        double vx = start.vx();
        double vy = start.vy();
        double vz = start.vz();
        for (int step = 0; step < steps; step++) {
            vx *= CRASH_HORIZONTAL_DRAG;
            vz *= CRASH_HORIZONTAL_DRAG;
            vy = (vy - CRASH_GRAVITY) * CRASH_VERTICAL_DRAG;
            x += vx;
            y += vy;
            z += vz;
        }
        return new Pose(x, y, z, vx, vy, vz);
    }

    /**
     * Visual bank, positive with the right wing down: the steady turn into the orbit, rolled
     * out during the departure, and a spiral roll once shot down.
     */
    public static float bankDegrees(Orbit orbit, int departTick, int crashTick, double ticks) {
        if (crashTick >= 0 && ticks > crashTick) {
            return poweredBank(orbit, departTick, crashTick)
                    + orbit.direction() * CRASH_ROLL_DEGREES_PER_TICK
                    * (float) (ticks - crashTick);
        }
        return poweredBank(orbit, departTick, ticks);
    }

    private static float poweredBank(Orbit orbit, int departTick, double ticks) {
        float cruise = orbit.direction() * ORBIT_BANK_DEGREES;
        if (departTick < 0 || ticks <= departTick) {
            return cruise;
        }
        double remaining = 1.0D - (ticks - departTick) / DEPARTURE_ROLL_OUT_TICKS;
        return cruise * (float) Math.max(0.0D, remaining);
    }

    /** Visual pitch, positive nose down: a slight climb on departure, a dive once shot down. */
    public static float pitchDownDegrees(int departTick, int crashTick, double ticks) {
        if (crashTick >= 0 && ticks > crashTick) {
            float start = poweredPitchDown(departTick, crashTick);
            return Math.min(CRASH_MAX_PITCH_DEGREES,
                    start + CRASH_PITCH_DEGREES_PER_TICK * (float) (ticks - crashTick));
        }
        return poweredPitchDown(departTick, ticks);
    }

    private static float poweredPitchDown(int departTick, double ticks) {
        if (departTick < 0 || ticks <= departTick) {
            return 0.0F;
        }
        double fraction = Math.min(1.0D, (ticks - departTick) / DEPARTURE_PITCH_UP_TICKS);
        return -DEPARTURE_PITCH_UP_DEGREES * (float) fraction;
    }

    /** Start angle derived from the call id, so every client computes the same orbit. */
    public static double launchPhase(UUID callId) {
        long bits = callId.getLeastSignificantBits() >>> 11;
        return bits * 0x1.0p-53 * TWO_PI;
    }

    public static boolean launchClockwise(UUID callId) {
        return (callId.getMostSignificantBits() & 1L) == 0L;
    }

    /** Evenly spaced points on the orbit circle, as {x, z} pairs. */
    public static List<double[]> orbitSamplePoints(double centerX, double centerZ,
                                                   double radius, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("At least one orbit sample is required");
        }
        List<double[]> points = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            double angle = TWO_PI * index / count;
            points.add(new double[]{centerX + radius * Math.cos(angle),
                    centerZ + radius * Math.sin(angle)});
        }
        return points;
    }

    /**
     * Every chunk the airframe can occupy while circling, packed like
     * {@code ChunkPos.asLong}: the square around the orbit widened by the wing-tip margin.
     */
    public static long[] orbitChunks(double centerX, double centerZ, double radius,
                                     double margin) {
        double reach = radius + margin;
        int minChunkX = Math.floorDiv((int) Math.floor(centerX - reach), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(centerX + reach), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(centerZ - reach), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(centerZ + reach), 16);
        long[] chunks = new long[(maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1)];
        int index = 0;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks[index++] = packChunk(chunkX, chunkZ);
            }
        }
        return chunks;
    }

    /** Same packing as {@code ChunkPos.asLong(int, int)}. */
    public static long packChunk(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    public static int chunkX(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    public static int chunkZ(long packed) {
        return (int) (packed >>> 32);
    }

    /**
     * Cruise altitude: at least {@link #HEIGHT_ABOVE_TARGET} above the target surface and
     * {@link #CLEARANCE_ABOVE_ORBIT} above the highest known surface under the orbit, capped
     * {@link #CEILING_MARGIN} below the build limit. {@code orbitSurfaces} entries are null for
     * columns whose chunk was not loaded; those are ignored rather than read. Empty when the
     * target surface itself is unknown.
     */
    public static OptionalDouble cruiseAltitude(Integer targetSurface,
                                                List<Integer> orbitSurfaces,
                                                int maxBuildHeight) {
        if (targetSurface == null) {
            return OptionalDouble.empty();
        }
        int altitude = targetSurface + HEIGHT_ABOVE_TARGET;
        if (orbitSurfaces != null) {
            for (Integer surface : orbitSurfaces) {
                if (surface != null) {
                    altitude = Math.max(altitude, surface + CLEARANCE_ABOVE_ORBIT);
                }
            }
        }
        return OptionalDouble.of(Math.min(altitude, maxBuildHeight - CEILING_MARGIN));
    }

    /**
     * Last game tick the drone may exist: one departure after the final mission step. It bounds
     * a drone whose mission vanished without a callback (battle reset, crashed scheduler).
     */
    public static long expireGameTime(long launchGameTime, int stepCount,
                                      int stepIntervalTicks) {
        return launchGameTime + (long) (stepCount - 1) * stepIntervalTicks + DEPARTURE_TICKS;
    }
}
