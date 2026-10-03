package com.wok.commandersupport.artillery;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * One howitzer fire mission: every number of a barrage skill in one immutable place.
 *
 * <p>Timing (game ticks, accepted at tick {@code A}): wave {@code w} is fired at
 * {@code A + w * waveIntervalTicks} and lands {@code preparationTicks} later. Mission steps are
 * {@value #STEP_INTERVAL_TICKS} ticks apart and start after {@link #inboundTicks()}
 * ({@code preparationTicks - 30}), so step {@code k} runs at {@code A + inbound + 10k}. Round
 * {@code r} of wave {@code w} lands on step {@link #impactStep(int, int)}
 * ({@code 3 + w * waveIntervalTicks / 10 + r}, half a second between rounds of one wave) and its
 * incoming whistle starts exactly {@value #WHISTLE_LEAD_STEPS} steps (1.5 s) earlier. The mission
 * ends on the step of the last impact.</p>
 *
 * <p>Every impact lies within {@code areaRadius + scatterMax} of the target, so the definition
 * radius ({@link #dangerRadius()}) adds the blast radius on top: the map shows the whole danger
 * area and the core keeps every chunk an impact reads loaded.</p>
 */
public record ArtilleryProfile(
        ResourceLocation id,
        String fallbackName,
        String shortName,
        long cooldownTicks,
        int preparationTicks,
        int waves,
        int roundsPerWave,
        int waveIntervalTicks,
        double areaRadius,
        double scatterSigma,
        double scatterMax,
        float damage,
        float explosionRadius,
        String explosionParticle,
        Caliber caliber
) {
    /** Half a second between mission steps, which is also the gap between rounds of one wave. */
    public static final int STEP_INTERVAL_TICKS = 10;
    /** The incoming whistle starts this many steps (1.5 s) before its round lands. */
    public static final int WHISTLE_LEAD_STEPS = 3;
    public static final int WHISTLE_LEAD_TICKS = WHISTLE_LEAD_STEPS * STEP_INTERVAL_TICKS;
    /** The whistle is played this far above the surface at the impact point. */
    public static final double WHISTLE_HEIGHT = 20.0D;
    public static final float INCOMING_VOLUME = 0.7F;
    /** The virtual battery sits this far above the known surface near the target. */
    public static final double BATTERY_HEIGHT = 40.0D;
    /** Server playback volume of one gun report; reach is set by the event range and sounds.json. */
    public static final float REPORT_VOLUME = 1.0F;
    /**
     * Players this far beyond the danger area must still hear the reports: the audible reach of a
     * report has to cover {@code batteryOffset + dangerRadius + REPORT_AUDIBLE_MARGIN}.
     */
    public static final double REPORT_AUDIBLE_MARGIN = 64.0D;

    // Cooldowns follow the faction table of 《步战模式公示表》: one global value per skill.
    /** 5 waves x 3 rounds of 155 mm-class fire, 10 s to land, 3 min cooldown. */
    public static final ArtilleryProfile HOWITZER_3ROUND = new ArtilleryProfile(
            WokCommanderSupportMod.HOWITZER_3ROUND_ID,
            "三连发榴弹炮击", "三连发炮击",
            3_600L, 200, 5, 3, 100,
            30.0D, 7.0D, 18.0D,
            250.0F, 10.0F, "LARGE", Caliber.HOWITZER_155);
    /** 3 waves x 3 rounds of 105 mm fire, 3 s to land, 2 min cooldown. */
    public static final ArtilleryProfile RAPID_105 = new ArtilleryProfile(
            WokCommanderSupportMod.HOWITZER_105_RAPID_ID,
            "快速三连发105毫米榴弹炮打击", "105快速三连发",
            2_400L, 60, 3, 3, 60,
            10.0D, 3.5D, 9.0D,
            150.0F, 7.0F, "MEDIUM", Caliber.HOWITZER_105);
    /** 4 waves x 5 rounds of 105 mm fire, 6 s to land, 5 min cooldown. */
    public static final ArtilleryProfile FIVE_ROUND_105 = new ArtilleryProfile(
            WokCommanderSupportMod.HOWITZER_105_5ROUND_ID,
            "五连发105毫米榴弹炮打击", "105五连发",
            6_000L, 120, 4, 5, 80,
            10.0D, 3.5D, 9.0D,
            150.0F, 7.0F, "MEDIUM", Caliber.HOWITZER_105);

    public ArtilleryProfile {
        Objects.requireNonNull(id, "id");
        requireText(fallbackName, "fallbackName");
        requireText(shortName, "shortName");
        requireText(explosionParticle, "explosionParticle");
        Objects.requireNonNull(caliber, "caliber");
        if (cooldownTicks < 0L) {
            throw new IllegalArgumentException("Artillery cooldown cannot be negative");
        }
        if (preparationTicks < WHISTLE_LEAD_TICKS) {
            throw new IllegalArgumentException("Artillery preparation must cover the "
                    + WHISTLE_LEAD_TICKS + "-tick whistle lead");
        }
        if (waves < 1 || roundsPerWave < 1) {
            throw new IllegalArgumentException("Artillery needs at least one wave and one round");
        }
        if (waveIntervalTicks < STEP_INTERVAL_TICKS
                || waveIntervalTicks % STEP_INTERVAL_TICKS != 0) {
            throw new IllegalArgumentException("Artillery wave interval must be a positive "
                    + "multiple of " + STEP_INTERVAL_TICKS + " ticks");
        }
        requireRange(areaRadius, "areaRadius");
        requireRange(scatterSigma, "scatterSigma");
        requireRange(scatterMax, "scatterMax");
        if (!Float.isFinite(damage) || damage <= 0.0F
                || !Float.isFinite(explosionRadius) || explosionRadius <= 0.0F) {
            throw new IllegalArgumentException("Artillery explosion must be finite and positive");
        }
        long steps = WHISTLE_LEAD_STEPS
                + (long) (waves - 1) * (waveIntervalTicks / STEP_INTERVAL_TICKS)
                + roundsPerWave;
        if (steps > SupportDefinition.MAX_STEPS) {
            throw new IllegalArgumentException("Artillery mission exceeds "
                    + SupportDefinition.MAX_STEPS + " steps");
        }
        if (areaRadius + scatterMax + explosionRadius > SupportDefinition.MAX_RADIUS) {
            throw new IllegalArgumentException("Artillery danger area exceeds "
                    + SupportDefinition.MAX_RADIUS + " blocks");
        }
    }

    /** One global value per skill; formations never override it. */
    public SupportDefinition definition() {
        return new SupportDefinition(id, translationKey(), fallbackName, shortName,
                SupportTargetMode.POINT, cooldownTicks, inboundTicks(), stepCount(),
                STEP_INTERVAL_TICKS, dangerRadius(), true);
    }

    public String translationKey() {
        return "support." + id.getNamespace() + "." + id.getPath();
    }

    /** Ticks from acceptance to step 0, which starts the first whistle. */
    public long inboundTicks() {
        return preparationTicks - (long) WHISTLE_LEAD_TICKS;
    }

    /** One step per half second up to and including the last impact. */
    public int stepCount() {
        return impactStep(waves - 1, roundsPerWave - 1) + 1;
    }

    /** Scatter envelope plus blast radius: the whole area a round can hurt. */
    public double dangerRadius() {
        return areaRadius + scatterMax + explosionRadius;
    }

    /** Upper bound of every impact's horizontal distance from the target. */
    public double maxImpactDistance() {
        return areaRadius + scatterMax;
    }

    public int totalRounds() {
        return waves * roundsPerWave;
    }

    public int stepsPerWaveInterval() {
        return waveIntervalTicks / STEP_INTERVAL_TICKS;
    }

    /** Mission step on which round {@code round} of wave {@code wave} lands. */
    public int impactStep(int wave, int round) {
        requireRound(wave, round);
        return WHISTLE_LEAD_STEPS + wave * stepsPerWaveInterval() + round;
    }

    /** Mission step on which the incoming whistle of that round starts. */
    public int whistleStep(int wave, int round) {
        return impactStep(wave, round) - WHISTLE_LEAD_STEPS;
    }

    /** Ticks after acceptance at which gun {@code gun} of wave {@code wave} fires. */
    public long reportDelayTicks(int wave, int gun) {
        requireRound(wave, gun);
        return (long) wave * waveIntervalTicks + (long) gun * caliber.reportSpacingTicks();
    }

    public int reportSpacingTicks() {
        return caliber.reportSpacingTicks();
    }

    public double batteryOffset() {
        return caliber.batteryOffset();
    }

    public float incomingPitch() {
        return caliber.incomingPitch();
    }

    private void requireRound(int wave, int round) {
        if (wave < 0 || wave >= waves || round < 0 || round >= roundsPerWave) {
            throw new IllegalArgumentException("Round " + wave + "/" + round
                    + " is outside the " + waves + "x" + roundsPerWave + " barrage");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Artillery " + field + " must not be blank");
        }
    }

    private static void requireRange(double value, String field) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException("Artillery " + field
                    + " must be finite and non-negative");
        }
    }

    /**
     * Gun class: how the battery sounds and where it stands. One wave fires one report per round,
     * {@code reportSpacingTicks} apart, from {@code batteryOffset} blocks away from the target
     * toward the commander.
     */
    public enum Caliber {
        HOWITZER_155(6, 180.0D, 0.95F),
        HOWITZER_105(3, 120.0D, 1.15F);

        private final int reportSpacingTicks;
        private final double batteryOffset;
        private final float incomingPitch;

        Caliber(int reportSpacingTicks, double batteryOffset, float incomingPitch) {
            this.reportSpacingTicks = reportSpacingTicks;
            this.batteryOffset = batteryOffset;
            this.incomingPitch = incomingPitch;
        }

        public int reportSpacingTicks() {
            return reportSpacingTicks;
        }

        public double batteryOffset() {
            return batteryOffset;
        }

        public float incomingPitch() {
            return incomingPitch;
        }
    }
}
