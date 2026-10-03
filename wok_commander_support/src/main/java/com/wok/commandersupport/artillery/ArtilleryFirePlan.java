package com.wok.commandersupport.artillery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * Pure, reproducible fire plan of one barrage.
 *
 * <p>Each wave aims at a point drawn uniformly inside the {@code areaRadius} circle around the
 * target ({@code r = R * sqrt(u)}); each round then adds an isotropic Gaussian offset with
 * {@code scatterSigma} per axis, redrawn while it exceeds {@code scatterMax}. Every impact
 * therefore lies within {@code areaRadius + scatterMax} of the target. Every draw has its own
 * {@link SplittableRandom} seeded from the call id (both halves), the wave and the round, so the
 * same call always produces the same plan and every step can rebuild it without stored state.</p>
 */
final class ArtilleryFirePlan {
    /** Seed slot of a wave's aim point; rounds use their own index (0 and up). */
    static final int AIM_POINT_SLOT = -1;
    /**
     * Redraws allowed before an offset falls back to the aim point. With the shipped profiles
     * ({@code scatterMax} about 2.57 sigma) one draw is rejected 3.7 % of the time, so the
     * fallback is never reached in practice; it only bounds the loop.
     */
    static final int MAX_SCATTER_DRAWS = 64;

    private static final long SEED_SALT = 0x6A09E667F3BCC909L;
    private static final long WAVE_GAMMA = 0x9E3779B97F4A7C15L;
    private static final long SLOT_GAMMA = 0xD1B54A32D192ED03L;
    private static final double FULL_TURN = 2.0D * Math.PI;

    private ArtilleryFirePlan() {
    }

    /** Every round of the barrage in firing order (wave, then round). */
    static List<ArtilleryRound> rounds(ArtilleryProfile profile, UUID callId,
                                       double centerX, double centerZ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(callId, "callId");
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)) {
            throw new IllegalArgumentException("Artillery target must be finite");
        }
        List<ArtilleryRound> rounds = new ArrayList<>(profile.totalRounds());
        for (int wave = 0; wave < profile.waves(); wave++) {
            Offset aim = aimOffset(profile.areaRadius(), seed(callId, wave, AIM_POINT_SLOT));
            for (int round = 0; round < profile.roundsPerWave(); round++) {
                Offset scatter = scatterOffset(profile.scatterSigma(), profile.scatterMax(),
                        seed(callId, wave, round));
                rounds.add(new ArtilleryRound(wave, round,
                        centerX + aim.dx() + scatter.dx(),
                        centerZ + aim.dz() + scatter.dz(),
                        profile.whistleStep(wave, round), profile.impactStep(wave, round)));
            }
        }
        return List.copyOf(rounds);
    }

    /** Rounds that land on, and whistles that start on, mission step {@code stepIndex}. */
    static ArtilleryStep step(List<ArtilleryRound> rounds, int stepIndex) {
        Objects.requireNonNull(rounds, "rounds");
        List<ArtilleryRound> impacts = new ArrayList<>();
        List<ArtilleryRound> whistles = new ArrayList<>();
        for (ArtilleryRound round : rounds) {
            if (round.impactStep() == stepIndex) {
                impacts.add(round);
            }
            if (round.whistleStep() == stepIndex) {
                whistles.add(round);
            }
        }
        return new ArtilleryStep(stepIndex, impacts, whistles);
    }

    /** The whole mission, one entry per step from 0 to {@code stepCount - 1}. */
    static List<ArtilleryStep> stepTable(ArtilleryProfile profile, List<ArtilleryRound> rounds) {
        Objects.requireNonNull(profile, "profile");
        List<ArtilleryStep> table = new ArrayList<>(profile.stepCount());
        for (int stepIndex = 0; stepIndex < profile.stepCount(); stepIndex++) {
            table.add(step(rounds, stepIndex));
        }
        return List.copyOf(table);
    }

    /** Uniform point in a disc of {@code radius}: strictly inside it unless the radius is 0. */
    static Offset aimOffset(double radius, long seed) {
        if (!Double.isFinite(radius) || radius < 0.0D) {
            throw new IllegalArgumentException("Artillery aim radius must be finite");
        }
        SplittableRandom random = new SplittableRandom(seed);
        double distance = radius * Math.sqrt(random.nextDouble());
        double angle = FULL_TURN * random.nextDouble();
        return new Offset(distance * Math.cos(angle), distance * Math.sin(angle));
    }

    /**
     * Isotropic 2-D normal offset ({@code sigma} per axis, Box-Muller in polar form) truncated by
     * rejection at {@code max}: an offset beyond the cap is redrawn, never clamped onto it.
     */
    static Offset scatterOffset(double sigma, double max, long seed) {
        if (!Double.isFinite(sigma) || sigma < 0.0D || !Double.isFinite(max) || max < 0.0D) {
            throw new IllegalArgumentException("Artillery scatter must be finite");
        }
        SplittableRandom random = new SplittableRandom(seed);
        for (int draw = 0; draw < MAX_SCATTER_DRAWS; draw++) {
            // 1 - u lies in (0, 1], so the logarithm is finite and never positive.
            double distance = sigma * Math.sqrt(-2.0D * Math.log(1.0D - random.nextDouble()));
            double angle = FULL_TURN * random.nextDouble();
            if (distance <= max) {
                return new Offset(distance * Math.cos(angle), distance * Math.sin(angle));
            }
        }
        return Offset.ZERO;
    }

    /** Independent seed for one draw: both halves of the call id, the wave and the slot. */
    static long seed(UUID callId, int wave, int slot) {
        Objects.requireNonNull(callId, "callId");
        long hash = mix64(callId.getMostSignificantBits() ^ SEED_SALT);
        hash = mix64(hash ^ callId.getLeastSignificantBits());
        hash = mix64(hash + WAVE_GAMMA * (wave + 1L));
        return mix64(hash + SLOT_GAMMA * (slot + 2L));
    }

    /**
     * Height of an explosion on a column whose motion-blocking surface (first free block, as
     * {@code Level.getHeight} reports it) is {@code surfaceY}: a quarter block above the ground,
     * kept inside the build range.
     */
    static double impactY(int surfaceY, int minBuildHeight, int maxBuildHeight) {
        if (maxBuildHeight - minBuildHeight < 3) {
            throw new IllegalArgumentException("Build range is too small for an impact");
        }
        return Math.min(maxBuildHeight - 2.0D,
                Math.max(minBuildHeight + 1.0D, surfaceY + 0.25D));
    }

    /** SplitMix64 finalizer. */
    private static long mix64(long value) {
        long mixed = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    /** Horizontal offset from a reference point. */
    record Offset(double dx, double dz) {
        static final Offset ZERO = new Offset(0.0D, 0.0D);

        double length() {
            return Math.hypot(dx, dz);
        }
    }
}
