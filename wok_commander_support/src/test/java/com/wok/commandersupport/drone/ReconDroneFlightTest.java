package com.wok.commandersupport.drone;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconDroneFlightTest {
    private static final double EPSILON = 1.0E-9D;
    private static final ReconDroneFlight.Orbit CLOCKWISE = new ReconDroneFlight.Orbit(
            100.5D, -200.25D, ReconDroneFlight.ORBIT_RADIUS, 140.0D, 1.0D, true);
    private static final ReconDroneFlight.Orbit COUNTER_CLOCKWISE = new ReconDroneFlight.Orbit(
            -3000.0D, 512.0D, ReconDroneFlight.ORBIT_RADIUS, 96.0D, 4.0D, false);

    @Test
    void orbitIsDeterministicAndKeepsItsRadiusAndAltitude() {
        for (double ticks = 0.0D; ticks <= 2_460.0D; ticks += 36.75D) {
            for (ReconDroneFlight.Orbit orbit : List.of(CLOCKWISE, COUNTER_CLOCKWISE)) {
                ReconDroneFlight.Pose pose = ReconDroneFlight.orbitPose(orbit, ticks);
                assertEquals(pose, ReconDroneFlight.orbitPose(orbit, ticks),
                        "the same clock must give the same position on every side");
                assertEquals(ReconDroneFlight.ORBIT_RADIUS, Math.hypot(
                        pose.x() - orbit.centerX(), pose.z() - orbit.centerZ()), EPSILON);
                assertEquals(orbit.altitude(), pose.y(), EPSILON);
            }
        }
    }

    @Test
    void orbitFliesAtCruiseSpeedAlongItsTangent() {
        for (double ticks = 0.0D; ticks < 300.0D; ticks += 13.0D) {
            ReconDroneFlight.Pose pose = ReconDroneFlight.orbitPose(CLOCKWISE, ticks);
            assertEquals(ReconDroneFlight.CRUISE_SPEED, Math.hypot(pose.vx(), pose.vz()),
                    EPSILON);
            double radialX = pose.x() - CLOCKWISE.centerX();
            double radialZ = pose.z() - CLOCKWISE.centerZ();
            assertEquals(0.0D, radialX * pose.vx() + radialZ * pose.vz(), 1.0E-7D);

            ReconDroneFlight.Pose next = ReconDroneFlight.orbitPose(CLOCKWISE, ticks + 1.0D);
            assertEquals(ReconDroneFlight.CRUISE_SPEED,
                    Math.hypot(next.x() - pose.x(), next.z() - pose.z()), 1.0E-3D);
        }
    }

    @Test
    void headingFollowsTheVelocityInMinecraftYaw() {
        assertEquals(0.0F, new ReconDroneFlight.Pose(0, 0, 0, 0, 0, 1).yawDegrees(), 1.0E-4F);
        assertEquals(90.0F, wrap(new ReconDroneFlight.Pose(0, 0, 0, -1, 0, 0).yawDegrees()),
                1.0E-4F);
        assertEquals(-90.0F, wrap(new ReconDroneFlight.Pose(0, 0, 0, 1, 0, 0).yawDegrees()),
                1.0E-4F);
        assertEquals(180.0F, Math.abs(wrap(
                new ReconDroneFlight.Pose(0, 0, 0, 0, 0, -1).yawDegrees())), 1.0E-4F);
        assertEquals(0.0F, new ReconDroneFlight.Pose(0, 0, 0, 0, -1, 0).yawDegrees());
    }

    @Test
    void theDroneTurnsAndBanksTowardsTheCentre() {
        for (ReconDroneFlight.Orbit orbit : List.of(CLOCKWISE, COUNTER_CLOCKWISE)) {
            ReconDroneFlight.Pose pose = ReconDroneFlight.orbitPose(orbit, 50.0D);
            double yaw = Math.toRadians(pose.yawDegrees());
            double rightX = -Math.cos(yaw);
            double rightZ = -Math.sin(yaw);
            double toCentre = (orbit.centerX() - pose.x()) * rightX
                    + (orbit.centerZ() - pose.z()) * rightZ;
            float yawRate = wrap(ReconDroneFlight.orbitPose(orbit, 51.0D).yawDegrees()
                    - pose.yawDegrees());
            float bank = ReconDroneFlight.bankDegrees(orbit, -1, -1, 50.0D);
            if (orbit.clockwise()) {
                assertTrue(toCentre > 23.0D, "a clockwise orbit keeps the centre on the right");
                assertTrue(yawRate > 0.0F, "a right turn increases the yaw");
                assertEquals(ReconDroneFlight.ORBIT_BANK_DEGREES, bank, 1.0E-4F);
            } else {
                assertTrue(toCentre < -23.0D);
                assertTrue(yawRate < 0.0F);
                assertEquals(-ReconDroneFlight.ORBIT_BANK_DEGREES, bank, 1.0E-4F);
            }
            assertEquals(0.0F, ReconDroneFlight.pitchDownDegrees(-1, -1, 50.0D));
        }
    }

    @Test
    void departureRollsOutOfTheOrbitAndClimbsAwayWithoutAJump() {
        int departTick = 2_360;
        ReconDroneFlight.Pose onStation = ReconDroneFlight.orbitPose(CLOCKWISE, departTick);
        assertEquals(onStation, ReconDroneFlight.flightPose(CLOCKWISE, departTick, departTick));
        assertEquals(ReconDroneFlight.orbitPose(CLOCKWISE, 1_000.0D),
                ReconDroneFlight.flightPose(CLOCKWISE, departTick, 1_000.0D));
        assertEquals(ReconDroneFlight.orbitPose(CLOCKWISE, 1_000.0D),
                ReconDroneFlight.flightPose(CLOCKWISE, -1, 1_000.0D));

        ReconDroneFlight.Pose justAfter = ReconDroneFlight.flightPose(CLOCKWISE, departTick,
                departTick + 0.001D);
        assertTrue(distance(onStation, justAfter) < 0.01D);

        ReconDroneFlight.Pose later = ReconDroneFlight.flightPose(CLOCKWISE, departTick,
                departTick + 50.0D);
        assertEquals(onStation.y() + 50.0D * ReconDroneFlight.DEPARTURE_CLIMB_PER_TICK,
                later.y(), EPSILON);
        assertEquals(ReconDroneFlight.departureDistance(50.0D),
                Math.hypot(later.x() - onStation.x(), later.z() - onStation.z()), 1.0E-6D);
        assertEquals(ReconDroneFlight.DEPARTURE_SPEED, Math.hypot(later.vx(), later.vz()),
                EPSILON);
        assertEquals(onStation.yawDegrees(), later.yawDegrees(), 1.0E-3F,
                "the departure is a straight line along the last heading");
        // 40.5 blocks along the tangent from a point 24 blocks off the centre.
        assertTrue(Math.hypot(later.x() - CLOCKWISE.centerX(), later.z() - CLOCKWISE.centerZ())
                > ReconDroneFlight.ORBIT_RADIUS + 20.0D);

        assertEquals(ReconDroneFlight.CRUISE_SPEED, ReconDroneFlight.departureSpeed(0.0D),
                EPSILON);
        // Accelerates from 0.6 to 0.9 blocks per tick over the first 30 ticks.
        assertEquals(6.5D, ReconDroneFlight.departureDistance(10.0D), EPSILON);
        assertEquals(40.5D, ReconDroneFlight.departureDistance(50.0D), EPSILON);
        assertEquals(ReconDroneFlight.ORBIT_BANK_DEGREES,
                ReconDroneFlight.bankDegrees(CLOCKWISE, departTick, -1, departTick), 1.0E-4F);
        assertEquals(0.0F, ReconDroneFlight.bankDegrees(CLOCKWISE, departTick, -1,
                departTick + 25.0D), 1.0E-4F);
        assertEquals(-6.0F, ReconDroneFlight.pitchDownDegrees(departTick, -1,
                departTick + 40.0D), 1.0E-4F);
    }

    @Test
    void aShotDownDroneKeepsItsMomentumAndFallsWithinTheCrashWindow() {
        ReconDroneFlight.Pose start = new ReconDroneFlight.Pose(0.0D, 200.0D, 0.0D,
                ReconDroneFlight.CRUISE_SPEED, 0.0D, 0.0D);
        assertEquals(start, ReconDroneFlight.crashPose(start, 0));
        assertEquals(start, ReconDroneFlight.crashPose(start, -5));

        ReconDroneFlight.Pose early = ReconDroneFlight.crashPose(start, 10);
        ReconDroneFlight.Pose late = ReconDroneFlight.crashPose(start, 40);
        assertTrue(early.x() > 5.0D, "forward momentum carries the wreck on");
        assertTrue(late.x() > early.x());
        assertTrue(late.y() < early.y() && early.y() < start.y());
        assertTrue(late.vy() < early.vy() && early.vy() < 0.0D, "the fall accelerates");
        assertEquals(0.0D, late.z(), EPSILON);
        assertEquals(start.yawDegrees(), late.yawDegrees(), 1.0E-4F);

        ReconDroneFlight.Pose capped = ReconDroneFlight.crashPose(start,
                ReconDroneFlight.CRASH_MAX_TICKS);
        assertEquals(capped, ReconDroneFlight.crashPose(start, 10_000));
        assertTrue(start.y() - capped.y() > ReconDroneFlight.HEIGHT_ABOVE_TARGET
                        + ReconDroneFlight.CLEARANCE_ABOVE_ORBIT,
                "a wreck reaches the ground from any cruise altitude before the time-out");
    }

    @Test
    void aShotDownDroneSpiralsNoseDown() {
        int crashTick = 500;
        float bankAtHit = ReconDroneFlight.bankDegrees(CLOCKWISE, -1, crashTick, crashTick);
        assertEquals(ReconDroneFlight.ORBIT_BANK_DEGREES, bankAtHit, 1.0E-4F);
        assertTrue(ReconDroneFlight.bankDegrees(CLOCKWISE, -1, crashTick, crashTick + 10.0D)
                > bankAtHit + 80.0F);
        assertTrue(ReconDroneFlight.bankDegrees(COUNTER_CLOCKWISE, -1, crashTick,
                crashTick + 10.0D) < -80.0F);
        assertEquals(20.0F, ReconDroneFlight.pitchDownDegrees(-1, crashTick, crashTick + 5.0D),
                1.0E-4F);
        assertEquals(60.0F, ReconDroneFlight.pitchDownDegrees(-1, crashTick, crashTick + 90.0D),
                1.0E-4F);
    }

    @Test
    void cruiseAltitudeTakesTheHigherRuleAndStaysBelowTheCeiling() {
        assertEquals(OptionalDouble.of(134.0D),
                ReconDroneFlight.cruiseAltitude(70, List.of(), 320));
        assertEquals(OptionalDouble.of(160.0D),
                ReconDroneFlight.cruiseAltitude(70, Arrays.asList(80, null, 120), 320),
                "40 blocks above the highest ground under the orbit wins over target + 64");
        assertEquals(OptionalDouble.of(154.0D),
                ReconDroneFlight.cruiseAltitude(70, Arrays.asList(80, null, 120), 170),
                "capped 16 blocks below the build limit");
        // Unloaded columns are skipped, never read.
        assertEquals(OptionalDouble.of(134.0D),
                ReconDroneFlight.cruiseAltitude(70, Arrays.asList(null, null), 320));
        assertEquals(OptionalDouble.of(134.0D), ReconDroneFlight.cruiseAltitude(70, null, 320));
        assertTrue(ReconDroneFlight.cruiseAltitude(null, List.of(100), 320).isEmpty());
    }

    @Test
    void orbitSamplesLieOnTheOrbitCircle() {
        List<double[]> samples = ReconDroneFlight.orbitSamplePoints(10.0D, -20.0D,
                ReconDroneFlight.ORBIT_RADIUS, ReconDroneFlight.HEIGHT_SAMPLES);
        assertEquals(16, samples.size());
        for (double[] sample : samples) {
            assertEquals(ReconDroneFlight.ORBIT_RADIUS,
                    Math.hypot(sample[0] - 10.0D, sample[1] + 20.0D), EPSILON);
        }
        assertThrows(IllegalArgumentException.class,
                () -> ReconDroneFlight.orbitSamplePoints(0, 0, 24, 0));
    }

    @Test
    void orbitChunksCoverTheWholeOrbitAndUseChunkPosPacking() {
        for (double[] centre : List.of(new double[]{0.5D, 0.5D}, new double[]{-8.5D, -1000.2D},
                new double[]{29_999_000.0D, -29_999_000.0D})) {
            long[] packed = ReconDroneFlight.orbitChunks(centre[0], centre[1],
                    ReconDroneFlight.ORBIT_RADIUS, ReconDroneFlight.AIRFRAME_MARGIN);
            Set<Long> chunks = Arrays.stream(packed).boxed().collect(Collectors.toSet());
            assertEquals(packed.length, chunks.size());
            assertTrue(packed.length <= 25, "at most a 5x5 chunk square is checked");

            List<double[]> points = new ArrayList<>(ReconDroneFlight.orbitSamplePoints(
                    centre[0], centre[1], ReconDroneFlight.ORBIT_RADIUS
                            + ReconDroneFlight.AIRFRAME_MARGIN, 64));
            points.add(centre);
            for (double[] point : points) {
                int chunkX = Math.floorDiv((int) Math.floor(point[0]), 16);
                int chunkZ = Math.floorDiv((int) Math.floor(point[1]), 16);
                assertTrue(chunks.contains(ChunkPos.asLong(chunkX, chunkZ)),
                        "orbit chunk " + chunkX + ", " + chunkZ + " is not checked");
            }
            for (long chunk : packed) {
                assertEquals(chunk, ChunkPos.asLong(ReconDroneFlight.chunkX(chunk),
                        ReconDroneFlight.chunkZ(chunk)));
            }
        }
    }

    @Test
    void theDroneExpiresOneDepartureAfterItsLastStep() {
        assertEquals(1_000L + 59L * 40L + 100L,
                ReconDroneFlight.expireGameTime(1_000L, 60, 40));
    }

    @Test
    void launchParametersAreDerivedFromTheCallId() {
        UUID callId = UUID.fromString("3f2c4e1a-9b7d-4c21-8e55-0a1b2c3d4e5f");
        assertEquals(ReconDroneFlight.launchPhase(callId), ReconDroneFlight.launchPhase(
                UUID.fromString(callId.toString())));
        assertEquals(ReconDroneFlight.launchClockwise(callId),
                ReconDroneFlight.launchClockwise(UUID.fromString(callId.toString())));
        boolean sawClockwise = false;
        boolean sawCounterClockwise = false;
        for (int index = 0; index < 64; index++) {
            UUID random = new UUID(0x9E3779B97F4A7C15L * (index + 1),
                    0xBF58476D1CE4E5B9L * (index + 7));
            double phase = ReconDroneFlight.launchPhase(random);
            assertTrue(phase >= 0.0D && phase < Math.PI * 2.0D);
            sawClockwise |= ReconDroneFlight.launchClockwise(random);
            sawCounterClockwise |= !ReconDroneFlight.launchClockwise(random);
        }
        assertTrue(sawClockwise && sawCounterClockwise);
    }

    @Test
    void invalidOrbitsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReconDroneFlight.Orbit(0, 0, 0, 100, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> new ReconDroneFlight.Orbit(Double.NaN, 0, 24, 100, 0, true));
        assertFalse(new ReconDroneFlight.Orbit(0, 0, 24, 100, 0, false).clockwise());
    }

    private static double distance(ReconDroneFlight.Pose a, ReconDroneFlight.Pose b) {
        return Math.sqrt(Math.pow(a.x() - b.x(), 2) + Math.pow(a.y() - b.y(), 2)
                + Math.pow(a.z() - b.z(), 2));
    }

    private static float wrap(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }
}
