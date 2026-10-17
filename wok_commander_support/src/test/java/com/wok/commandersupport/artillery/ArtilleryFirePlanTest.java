package com.wok.commandersupport.artillery;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtilleryFirePlanTest {
    private static final List<ArtilleryProfile> ALL = List.of(ArtilleryProfile.HOWITZER_3ROUND,
            ArtilleryProfile.RAPID_105, ArtilleryProfile.FIVE_ROUND_105);
    private static final double EPSILON = 1.0E-9D;
    private static final UUID CALL = UUID.fromString("7f3c2b1a-9d8e-4f60-8a1b-2c3d4e5f6a7b");

    @Test
    void wavesAndRoundsMatchEachProfile() {
        assertShape(ArtilleryProfile.HOWITZER_3ROUND, 5, 3);
        assertShape(ArtilleryProfile.RAPID_105, 3, 3);
        assertShape(ArtilleryProfile.FIVE_ROUND_105, 4, 5);
    }

    @Test
    void everyRoundWhistlesExactlyThreeStepsBeforeItLands() {
        for (ArtilleryProfile profile : ALL) {
            List<ArtilleryRound> rounds = ArtilleryFirePlan.rounds(profile, CALL, 0.0D, 0.0D);
            List<ArtilleryStep> table = ArtilleryFirePlan.stepTable(profile, rounds);
            assertEquals(profile.stepCount(), table.size());
            for (ArtilleryRound round : rounds) {
                assertEquals(profile.impactStep(round.wave(), round.round()), round.impactStep());
                assertEquals(round.impactStep() - 3, round.whistleStep());
                for (ArtilleryStep step : table) {
                    assertEquals(step.index() == round.impactStep(),
                            step.impacts().contains(round), profile.id() + " " + round);
                    assertEquals(step.index() == round.whistleStep(),
                            step.whistles().contains(round), profile.id() + " " + round);
                }
            }
            int impacts = 0;
            int whistles = 0;
            for (ArtilleryStep step : table) {
                impacts += step.impacts().size();
                whistles += step.whistles().size();
            }
            assertEquals(profile.totalRounds(), impacts);
            assertEquals(profile.totalRounds(), whistles);
        }
    }

    @Test
    void stepZeroStartsTheFirstWhistleAndTheLastStepLandsTheLastRound() {
        for (ArtilleryProfile profile : ALL) {
            List<ArtilleryRound> rounds = ArtilleryFirePlan.rounds(profile, CALL, 5.0D, -9.0D);
            List<ArtilleryStep> table = ArtilleryFirePlan.stepTable(profile, rounds);
            ArtilleryStep first = table.get(0);
            assertEquals(1, first.whistles().size());
            assertEquals(0, first.whistles().get(0).wave());
            assertEquals(0, first.whistles().get(0).round());
            for (int stepIndex = 0; stepIndex < ArtilleryProfile.WHISTLE_LEAD_STEPS;
                 stepIndex++) {
                assertTrue(table.get(stepIndex).impacts().isEmpty(),
                        "nothing lands before the first whistle has run 1.5 s");
            }
            ArtilleryStep last = table.get(table.size() - 1);
            assertEquals(1, last.impacts().size());
            assertEquals(profile.waves() - 1, last.impacts().get(0).wave());
            assertEquals(profile.roundsPerWave() - 1, last.impacts().get(0).round());
            assertTrue(last.whistles().isEmpty());
            assertTrue(ArtilleryFirePlan.step(rounds, profile.stepCount()).isEmpty());
        }
    }

    @Test
    void everyImpactStaysInsideTheScatterEnvelope() {
        Random calls = new Random(20261004L);
        for (ArtilleryProfile profile : ALL) {
            double limit = profile.areaRadius() + profile.scatterMax();
            double farthest = 0.0D;
            for (int sample = 0; sample < 2_000; sample++) {
                UUID callId = new UUID(calls.nextLong(), calls.nextLong());
                for (ArtilleryRound round : ArtilleryFirePlan.rounds(profile, callId,
                        1_000.5D, -2_000.25D)) {
                    double distance = round.distanceTo(1_000.5D, -2_000.25D);
                    assertTrue(distance <= limit + EPSILON,
                            profile.id() + " round at " + distance + " > " + limit);
                    farthest = Math.max(farthest, distance);
                }
            }
            assertEquals(limit, profile.maxImpactDistance());
            assertTrue(farthest > profile.areaRadius(),
                    "the scatter must actually spread rounds beyond the aim circle");
            assertTrue(limit + profile.explosionRadius() <= profile.dangerRadius() + EPSILON);
        }
    }

    @Test
    void aimPointsAreUniformInsideTheAreaCircle() {
        int inner = 0;
        int samples = 20_000;
        for (int sample = 0; sample < samples; sample++) {
            ArtilleryFirePlan.Offset aim = ArtilleryFirePlan.aimOffset(30.0D,
                    ArtilleryFirePlan.seed(CALL, sample, ArtilleryFirePlan.AIM_POINT_SLOT));
            assertTrue(aim.length() < 30.0D + EPSILON);
            if (aim.length() <= 15.0D) {
                inner++;
            }
        }
        // Uniform over the area: a quarter of the points lie within half the radius.
        double innerShare = inner / (double) samples;
        assertTrue(innerShare > 0.23D && innerShare < 0.27D, "inner share " + innerShare);
        assertEquals(0.0D, ArtilleryFirePlan.aimOffset(0.0D, 42L).length());
    }

    @Test
    void scatterIsGaussianAndTruncatedByRedrawingNotClamping() {
        double sigma = 10.0D;
        double max = 5.0D;
        int nearCap = 0;
        int inner = 0;
        for (long seed = 0; seed < 5_000L; seed++) {
            double length = ArtilleryFirePlan.scatterOffset(sigma, max, seed).length();
            assertTrue(length <= max + EPSILON, "offset " + length + " beyond the cap");
            if (Math.abs(length - max) < 1.0E-6D) {
                nearCap++;
            }
            if (length < max / 2.0D) {
                inner++;
            }
        }
        assertEquals(0, nearCap, "rejected draws must be redrawn, not pulled onto the cap");
        assertTrue(inner > 500, "redrawn offsets still fill the inside of the cap");
    }

    @Test
    void scatterUsesSigmaPerAxis() {
        // Truncated Rayleigh: E[r^2 | r <= c sigma] = 2 sigma^2 (1 - (c^2/2) e^(-c^2/2)
        // / (1 - e^(-c^2/2))). For the 105 profiles c = 9 / 3.5.
        double sigma = 3.5D;
        double max = 9.0D;
        double half = (max / sigma) * (max / sigma) / 2.0D;
        double expected = 2.0D * sigma * sigma
                * (1.0D - half * Math.exp(-half) / (1.0D - Math.exp(-half)));
        double sum = 0.0D;
        int samples = 8_000;
        for (int sample = 0; sample < samples; sample++) {
            double length = ArtilleryFirePlan.scatterOffset(sigma, max,
                    ArtilleryFirePlan.seed(CALL, 0, sample)).length();
            sum += length * length;
        }
        double mean = sum / samples;
        assertTrue(mean > expected * 0.93D && mean < expected * 1.07D,
                "mean squared offset " + mean + " vs " + expected);
    }

    @Test
    void zeroSigmaLandsEveryRoundOfAWaveOnItsAimPoint() {
        ArtilleryProfile tight = new ArtilleryProfile(ArtilleryProfile.RAPID_105.id(),
                "名称", "短名", 100L, 60, 3, 3, 60, 10.0D, 0.0D, 9.0D,
                150.0F, 7.0F, "MEDIUM", ArtilleryProfile.Caliber.HOWITZER_105);
        List<ArtilleryRound> rounds = ArtilleryFirePlan.rounds(tight, CALL, 0.0D, 0.0D);
        for (ArtilleryRound round : rounds) {
            ArtilleryRound first = rounds.get(round.wave() * 3);
            assertEquals(first.x(), round.x(), EPSILON);
            assertEquals(first.z(), round.z(), EPSILON);
        }
    }

    @Test
    void sameCallIdReproducesThePlanAndOtherCallsDiffer() {
        for (ArtilleryProfile profile : ALL) {
            List<ArtilleryRound> first = ArtilleryFirePlan.rounds(profile, CALL, 12.0D, 34.0D);
            List<ArtilleryRound> again = ArtilleryFirePlan.rounds(profile,
                    UUID.fromString(CALL.toString()), 12.0D, 34.0D);
            assertEquals(first, again);
            List<ArtilleryRound> other = ArtilleryFirePlan.rounds(profile,
                    UUID.fromString("7f3c2b1a-9d8e-4f60-8a1b-2c3d4e5f6a7c"), 12.0D, 34.0D);
            assertNotEquals(first, other);
            // Moving the target moves the plan rigidly.
            List<ArtilleryRound> moved = ArtilleryFirePlan.rounds(profile, CALL, 112.0D, 34.0D);
            for (int index = 0; index < first.size(); index++) {
                assertEquals(first.get(index).x() + 100.0D, moved.get(index).x(), EPSILON);
                assertEquals(first.get(index).z(), moved.get(index).z(), EPSILON);
            }
        }
    }

    @Test
    void seedsDependOnBothCallIdHalvesTheWaveAndTheSlot() {
        UUID highChanged = new UUID(CALL.getMostSignificantBits() ^ 1L,
                CALL.getLeastSignificantBits());
        UUID lowChanged = new UUID(CALL.getMostSignificantBits(),
                CALL.getLeastSignificantBits() ^ 1L);
        Set<Long> seeds = new HashSet<>();
        seeds.add(ArtilleryFirePlan.seed(CALL, 0, 0));
        seeds.add(ArtilleryFirePlan.seed(highChanged, 0, 0));
        seeds.add(ArtilleryFirePlan.seed(lowChanged, 0, 0));
        seeds.add(ArtilleryFirePlan.seed(CALL, 1, 0));
        seeds.add(ArtilleryFirePlan.seed(CALL, 0, 1));
        seeds.add(ArtilleryFirePlan.seed(CALL, 0, ArtilleryFirePlan.AIM_POINT_SLOT));
        assertEquals(6, seeds.size());
        assertEquals(ArtilleryFirePlan.seed(CALL, 3, 2), ArtilleryFirePlan.seed(CALL, 3, 2));
    }

    @Test
    void roundsOfOneWaveShareTheirAimButNotTheirOffsets() {
        List<ArtilleryRound> rounds = ArtilleryFirePlan.rounds(ArtilleryProfile.FIVE_ROUND_105,
                CALL, 0.0D, 0.0D);
        List<String> points = new ArrayList<>();
        for (ArtilleryRound round : rounds) {
            points.add(round.x() + "," + round.z());
        }
        assertEquals(points.size(), new HashSet<>(points).size(),
                "no two rounds land on exactly the same point");
    }

    @Test
    void impactSitsAQuarterBlockAboveTheSurfaceInsideTheBuildRange() {
        assertEquals(64.25D, ArtilleryFirePlan.impactY(64, -64, 320));
        assertEquals(318.0D, ArtilleryFirePlan.impactY(320, -64, 320));
        assertEquals(-63.0D, ArtilleryFirePlan.impactY(-200, -64, 320));
    }

    @Test
    void invalidInputFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> ArtilleryFirePlan.rounds(
                ArtilleryProfile.RAPID_105, CALL, Double.NaN, 0.0D));
        assertThrows(NullPointerException.class, () -> ArtilleryFirePlan.rounds(
                ArtilleryProfile.RAPID_105, null, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> ArtilleryFirePlan.scatterOffset(-1.0D, 9.0D, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> ArtilleryFirePlan.aimOffset(Double.POSITIVE_INFINITY, 1L));
        assertFalse(ArtilleryFirePlan.step(List.of(), 0).impacts().iterator().hasNext());
    }

    private static void assertShape(ArtilleryProfile profile, int waves, int roundsPerWave) {
        List<ArtilleryRound> rounds = ArtilleryFirePlan.rounds(profile, CALL, 0.0D, 0.0D);
        assertEquals(waves * roundsPerWave, rounds.size());
        Set<String> seen = new HashSet<>();
        for (ArtilleryRound round : rounds) {
            assertTrue(round.wave() >= 0 && round.wave() < waves);
            assertTrue(round.round() >= 0 && round.round() < roundsPerWave);
            assertTrue(seen.add(round.wave() + "/" + round.round()));
        }
        for (int wave = 0; wave < waves; wave++) {
            int perWave = 0;
            for (ArtilleryRound round : rounds) {
                if (round.wave() == wave) {
                    perWave++;
                }
            }
            assertEquals(roundsPerWave, perWave, profile.id() + " wave " + wave);
        }
    }
}
