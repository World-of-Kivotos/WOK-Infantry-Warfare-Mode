package com.wok.commandersupport.artillery;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Pure gun-report timetable and virtual battery position.
 *
 * <p>Every wave is announced by one report per round: wave {@code w} fires at
 * {@code w * waveIntervalTicks} ticks after acceptance, its guns {@code reportSpacingTicks} apart,
 * so the first report of the barrage sounds at the moment of acceptance and every wave lands
 * exactly {@code preparationTicks} after its first report.</p>
 */
final class ArtilleryReportPlan {
    /** Closer than this (horizontally) the commander gives no usable bearing to the battery. */
    static final double MIN_BEARING_DISTANCE = 1.0D;

    private ArtilleryReportPlan() {
    }

    /** All reports of the barrage, earliest first. */
    static List<Report> reports(ArtilleryProfile profile) {
        Objects.requireNonNull(profile, "profile");
        List<Report> reports = new ArrayList<>(profile.totalRounds());
        for (int wave = 0; wave < profile.waves(); wave++) {
            for (int gun = 0; gun < profile.roundsPerWave(); gun++) {
                reports.add(new Report(wave, gun, profile.reportDelayTicks(wave, gun)));
            }
        }
        // Stable: equal delays keep wave order.
        reports.sort(Comparator.comparingLong(Report::delayTicks));
        return List.copyOf(reports);
    }

    /**
     * The virtual battery: {@code offset} blocks from the target toward the commander's position
     * at acceptance, {@link ArtilleryProfile#BATTERY_HEIGHT} above {@code surfaceY}. A commander
     * standing on the target (or an unusable position) gets a fixed bearing derived from the call
     * id instead.
     */
    static Vec3 batteryPosition(double targetX, double targetZ, double commanderX,
                                double commanderZ, double surfaceY, double offset,
                                UUID callId) {
        Objects.requireNonNull(callId, "callId");
        if (!Double.isFinite(targetX) || !Double.isFinite(targetZ)
                || !Double.isFinite(surfaceY) || !Double.isFinite(offset) || offset < 0.0D) {
            throw new IllegalArgumentException("Invalid artillery battery geometry");
        }
        double dx = commanderX - targetX;
        double dz = commanderZ - targetZ;
        double distance = Math.hypot(dx, dz);
        double unitX;
        double unitZ;
        if (Double.isFinite(distance) && distance >= MIN_BEARING_DISTANCE) {
            unitX = dx / distance;
            unitZ = dz / distance;
        } else {
            double angle = 2.0D * Math.PI
                    * ((callId.getMostSignificantBits() >>> 11) * 0x1.0p-53);
            unitX = Math.cos(angle);
            unitZ = Math.sin(angle);
        }
        return new Vec3(targetX + unitX * offset, surfaceY + ArtilleryProfile.BATTERY_HEIGHT,
                targetZ + unitZ * offset);
    }

    /**
     * Ground height the battery is placed above: the loaded surface at the target when known,
     * otherwise the commander's height, otherwise sea level. Never requires an unloaded chunk.
     */
    static double referenceSurfaceY(OptionalInt targetSurfaceY, double commanderY, int seaLevel) {
        if (targetSurfaceY != null && targetSurfaceY.isPresent()) {
            return targetSurfaceY.getAsInt();
        }
        return Double.isFinite(commanderY) ? commanderY : seaLevel;
    }

    /** One gun report, {@code delayTicks} after acceptance. */
    record Report(int wave, int gun, long delayTicks) {
    }
}
