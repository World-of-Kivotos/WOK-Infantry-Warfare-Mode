package com.wok.infantry.battle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotMarkerQuotaTest {
    @Test
    void satelliteContactsAndManualMarkersEachReserveHalfOfTheSnapshot() {
        assertEquals(32, BattleRules.RESERVED_SUPPORT_INTEL_SNAPSHOT_SLOTS);
        assertEquals(BattleRules.MAX_MARKERS_PER_FACTION,
                BattleRules.RESERVED_SUPPORT_INTEL_SNAPSHOT_SLOTS * 2,
                "manual markers keep the other half of the faction snapshot");
    }

    @Test
    void fullIntelAndFullManualSplitEvenly() {
        List<String> merged = BattleRules.mergeSnapshotMarkers(
                markers("intel", 64), markers("manual", 64));

        assertEquals(64, merged.size());
        assertEquals(markers("intel", 32), merged.subList(0, 32));
        assertEquals(range("manual", 32, 64), merged.subList(32, 64));
    }

    @Test
    void unusedIntelReserveIsGivenToManualMarkers() {
        List<String> merged = BattleRules.mergeSnapshotMarkers(
                markers("intel", 10), markers("manual", 64));

        assertEquals(64, merged.size());
        assertEquals(markers("intel", 10), merged.subList(0, 10));
        assertEquals(range("manual", 10, 64), merged.subList(10, 64));
    }

    @Test
    void unusedManualReserveIsGivenToIntel() {
        assertEquals(markers("intel", 64), BattleRules.mergeSnapshotMarkers(
                markers("intel", 64), List.of()));

        List<String> merged = BattleRules.mergeSnapshotMarkers(
                markers("intel", 64), markers("manual", 10));
        assertEquals(64, merged.size());
        assertEquals(markers("intel", 54), merged.subList(0, 54));
        assertEquals(markers("manual", 10), merged.subList(54, 64));
    }

    @Test
    void manualMarkersAloneStillFillTheSnapshot() {
        assertEquals(markers("manual", 64), BattleRules.mergeSnapshotMarkers(
                List.of(), markers("manual", 64)));
        assertTrue(BattleRules.mergeSnapshotMarkers(List.of(), List.of()).isEmpty());
    }

    @Test
    void overlappingSupportCallsCannotPushManualMarkersBelowTheirReserve() {
        // Several concurrent calls may each publish up to the per-faction limit.
        List<String> merged = BattleRules.mergeSnapshotMarkers(
                markers("intel", 150), markers("manual", 40));

        assertEquals(64, merged.size());
        assertEquals(markers("intel", 32), merged.subList(0, 32));
        assertEquals(range("manual", 8, 40), merged.subList(32, 64));
    }

    @Test
    void aJustPlacedManualMarkerIsShownWhenOlderMarkersExceedTheReserve() {
        // 40 live manual markers (oldest first) plus a 40-contact recon sweep; a squad leader
        // then places marker number 41, which the server accepts (41 < 64).
        List<String> manual = markers("manual", 41);
        List<String> merged = BattleRules.mergeSnapshotMarkers(markers("intel", 40), manual);

        assertEquals(64, merged.size());
        assertTrue(merged.contains("manual-40"), "the newest marker must reach the snapshot");
        assertEquals(range("manual", 9, 41), merged.subList(32, 64),
                "the oldest manual markers give way first and draw order stays ascending");
    }

    @Test
    void everyCombinationStaysBoundedAndKeepsBothInputOrders() {
        for (int intelCount = 0; intelCount <= 80; intelCount++) {
            for (int manualCount = 0; manualCount <= 80; manualCount++) {
                List<String> intel = markers("intel", intelCount);
                List<String> manual = markers("manual", manualCount);
                List<String> merged = BattleRules.mergeSnapshotMarkers(intel, manual);

                int intelShown = (int) merged.stream()
                        .filter(marker -> marker.startsWith("intel")).count();
                int manualShown = merged.size() - intelShown;
                String label = intelCount + "+" + manualCount;
                assertTrue(merged.size() <= BattleRules.MAX_MARKERS_PER_FACTION, label);
                assertEquals(Math.min(intelCount + manualCount,
                        BattleRules.MAX_MARKERS_PER_FACTION), merged.size(),
                        "no slot may stay empty while either side still has markers: " + label);
                assertTrue(intelShown >= Math.min(intelCount,
                        BattleRules.RESERVED_SUPPORT_INTEL_SNAPSHOT_SLOTS), label);
                assertTrue(manualShown >= Math.min(manualCount,
                        BattleRules.MAX_MARKERS_PER_FACTION
                                - BattleRules.RESERVED_SUPPORT_INTEL_SNAPSHOT_SLOTS), label);
                assertEquals(intel.subList(0, intelShown), merged.subList(0, intelShown),
                        "intel must keep publication order: " + label);
                assertEquals(manual.subList(manualCount - manualShown, manualCount),
                        merged.subList(intelShown, merged.size()),
                        "manual markers must keep the newest in creation order: " + label);
            }
        }
    }

    private static List<String> markers(String prefix, int count) {
        return range(prefix, 0, count);
    }

    private static List<String> range(String prefix, int fromInclusive, int toExclusive) {
        List<String> markers = new ArrayList<>(Math.max(0, toExclusive - fromInclusive));
        for (int index = fromInclusive; index < toExclusive; index++) {
            markers.add(prefix + '-' + index);
        }
        return markers;
    }
}
