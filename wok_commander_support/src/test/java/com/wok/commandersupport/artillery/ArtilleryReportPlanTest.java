package com.wok.commandersupport.artillery;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtilleryReportPlanTest {
    private static final List<ArtilleryProfile> ALL = List.of(ArtilleryProfile.HOWITZER_3ROUND,
            ArtilleryProfile.RAPID_105, ArtilleryProfile.FIVE_ROUND_105);
    private static final UUID CALL = UUID.fromString("0b9c8d7e-6f5a-4b3c-9d2e-1f0a9b8c7d6e");

    @Test
    void everyWaveFiresOneReportPerRoundAndTheFirstSoundsOnAcceptance() {
        assertTimetable(ArtilleryProfile.HOWITZER_3ROUND, new long[][]{
                {0, 6, 12}, {100, 106, 112}, {200, 206, 212}, {300, 306, 312},
                {400, 406, 412}});
        assertTimetable(ArtilleryProfile.RAPID_105, new long[][]{
                {0, 3, 6}, {60, 63, 66}, {120, 123, 126}});
        assertTimetable(ArtilleryProfile.FIVE_ROUND_105, new long[][]{
                {0, 3, 6, 9, 12}, {80, 83, 86, 89, 92}, {160, 163, 166, 169, 172},
                {240, 243, 246, 249, 252}});
    }

    @Test
    void reportsAreOrderedAndEveryWaveIsHeardBeforeItsWhistle() {
        for (ArtilleryProfile profile : ALL) {
            List<ArtilleryReportPlan.Report> reports = ArtilleryReportPlan.reports(profile);
            assertEquals(profile.totalRounds(), reports.size());
            assertEquals(0L, reports.get(0).delayTicks(), "the first gun fires on acceptance");
            for (int index = 1; index < reports.size(); index++) {
                assertTrue(reports.get(index - 1).delayTicks()
                        <= reports.get(index).delayTicks());
            }
            for (ArtilleryReportPlan.Report report : reports) {
                long whistleStart = profile.inboundTicks() + (long) profile.whistleStep(
                        report.wave(), 0) * ArtilleryProfile.STEP_INTERVAL_TICKS;
                assertTrue(report.delayTicks() < whistleStart,
                        profile.id() + " report " + report + " after its whistle");
            }
        }
    }

    @Test
    void batterySitsTowardTheCommanderAtTheCaliberOffset() {
        Vec3 battery = ArtilleryReportPlan.batteryPosition(100.0D, 200.0D, 100.0D, 260.0D,
                70.0D, 180.0D, CALL);

        assertEquals(100.0D, battery.x, 1.0E-9D);
        assertEquals(380.0D, battery.z, 1.0E-9D);
        assertEquals(110.0D, battery.y, 1.0E-9D, "40 blocks above the known surface");

        Vec3 diagonal = ArtilleryReportPlan.batteryPosition(0.0D, 0.0D, -30.0D, -40.0D,
                64.0D, 120.0D, CALL);
        assertEquals(-72.0D, diagonal.x, 1.0E-9D);
        assertEquals(-96.0D, diagonal.z, 1.0E-9D);
    }

    @Test
    void aCommanderOnTheTargetGetsAStableBearingFromTheCallId() {
        Vec3 first = ArtilleryReportPlan.batteryPosition(10.0D, 10.0D, 10.2D, 9.9D,
                64.0D, 120.0D, CALL);
        Vec3 again = ArtilleryReportPlan.batteryPosition(10.0D, 10.0D, 10.0D, 10.0D,
                64.0D, 120.0D, CALL);
        Vec3 unusable = ArtilleryReportPlan.batteryPosition(10.0D, 10.0D, Double.NaN,
                10.0D, 64.0D, 120.0D, CALL);

        assertEquals(first, again);
        assertEquals(first, unusable);
        assertEquals(120.0D, Math.hypot(first.x - 10.0D, first.z - 10.0D), 1.0E-9D);
        Set<String> bearings = new HashSet<>();
        for (int index = 0; index < 8; index++) {
            Vec3 battery = ArtilleryReportPlan.batteryPosition(0.0D, 0.0D, 0.0D, 0.0D,
                    64.0D, 120.0D, new UUID(index * 0x1234_5678_9ABC_DEFL, index));
            bearings.add(Math.round(battery.x) + "," + Math.round(battery.z));
        }
        assertTrue(bearings.size() > 1, "different calls should not all share one bearing");
    }

    @Test
    void batteryHeightNeverNeedsAnUnloadedChunk() {
        assertEquals(72.0D, ArtilleryReportPlan.referenceSurfaceY(OptionalInt.of(72), 90.0D,
                63));
        assertEquals(90.0D, ArtilleryReportPlan.referenceSurfaceY(OptionalInt.empty(), 90.0D,
                63));
        assertEquals(63.0D, ArtilleryReportPlan.referenceSurfaceY(OptionalInt.empty(),
                Double.NaN, 63));
        assertThrows(IllegalArgumentException.class, () -> ArtilleryReportPlan.batteryPosition(
                0.0D, 0.0D, 1.0D, 1.0D, Double.NaN, 120.0D, CALL));
    }

    private static void assertTimetable(ArtilleryProfile profile, long[][] expected) {
        List<ArtilleryReportPlan.Report> reports = ArtilleryReportPlan.reports(profile);
        assertEquals(expected.length, profile.waves());
        int index = 0;
        for (int wave = 0; wave < expected.length; wave++) {
            assertEquals(profile.roundsPerWave(), expected[wave].length);
            for (int gun = 0; gun < expected[wave].length; gun++) {
                ArtilleryReportPlan.Report report = reports.get(index++);
                assertEquals(wave, report.wave(), profile.id().toString());
                assertEquals(gun, report.gun(), profile.id().toString());
                assertEquals(expected[wave][gun], report.delayTicks(),
                        profile.id() + " wave " + wave + " gun " + gun);
            }
        }
        assertEquals(reports.size(), index);
    }
}
