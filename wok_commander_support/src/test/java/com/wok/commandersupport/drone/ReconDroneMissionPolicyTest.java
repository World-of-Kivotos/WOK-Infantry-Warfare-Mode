package com.wok.commandersupport.drone;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.support.adapter.SupportSpawnException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconDroneMissionPolicyTest {
    private static final int STEP_COUNT = ReconDroneProvider.STEP_COUNT;

    @Test
    void theSortieScansEveryTenSecondsTwelveTimes() {
        List<Integer> scans = IntStream.range(0, STEP_COUNT)
                .filter(ReconDroneMissionPolicy::isScanStep).boxed().toList();

        assertEquals(12, scans.size());
        assertEquals(List.of(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55), scans);
        assertEquals(10 * 20, ReconDroneMissionPolicy.SCAN_EVERY_STEPS
                * ReconDroneProvider.STEP_INTERVAL_TICKS);
        assertFalse(ReconDroneMissionPolicy.isScanStep(-5));
    }

    @Test
    void onlyTheLastStepSendsTheDroneHome() {
        List<Integer> finals = IntStream.range(0, STEP_COUNT)
                .filter(step -> ReconDroneMissionPolicy.isFinalStep(step, STEP_COUNT))
                .boxed().toList();

        assertEquals(List.of(59), finals);
        assertFalse(ReconDroneMissionPolicy.isScanStep(59), "the last step only departs");
    }

    @Test
    void aShootDownIsReportedEvenAfterTheWreckIsGone() {
        assertEquals(ReconDroneMissionPolicy.Status.SHOT_DOWN,
                ReconDroneMissionPolicy.status(true, false, false, false));
        assertEquals(ReconDroneMissionPolicy.Status.SHOT_DOWN,
                ReconDroneMissionPolicy.status(true, true, true, true));
        // Still falling, even if the link was never recorded.
        assertEquals(ReconDroneMissionPolicy.Status.SHOT_DOWN,
                ReconDroneMissionPolicy.status(false, true, true, true));
        assertEquals(ReconDroneMissionPolicy.Status.SHOT_DOWN,
                ReconDroneMissionPolicy.status(false, true, true, false));
    }

    @Test
    void aMissingOrFrozenDroneHasLostItsSignal() {
        assertEquals(ReconDroneMissionPolicy.Status.SIGNAL_LOST,
                ReconDroneMissionPolicy.status(false, false, false, false));
        assertEquals(ReconDroneMissionPolicy.Status.SIGNAL_LOST,
                ReconDroneMissionPolicy.status(false, true, false, false),
                "a drone in a chunk that no longer ticks is out of contact");
        assertEquals(ReconDroneMissionPolicy.Status.ACTIVE,
                ReconDroneMissionPolicy.status(false, true, false, true));
    }

    @Test
    void lostDronesEndTheMissionWithoutRefundOrCircuitTrip() {
        SupportSpawnException shotDown = ReconDroneMissionPolicy.outcome(
                ReconDroneMissionPolicy.Status.SHOT_DOWN);
        assertEquals("侦察无人机已被击落", shotDown.getMessage());
        assertFalse(shotDown.refundCooldown());
        assertFalse(shotDown.providerBroken());

        SupportSpawnException lost = ReconDroneMissionPolicy.outcome(
                ReconDroneMissionPolicy.Status.SIGNAL_LOST);
        assertEquals("侦察无人机信号丢失", lost.getMessage());
        assertFalse(lost.refundCooldown());
        assertFalse(lost.providerBroken());

        // Both pass through the step classification unchanged after launch.
        assertSame(shotDown, ReconDroneMissionPolicy.classify(7, shotDown));
        assertSame(lost, ReconDroneMissionPolicy.classify(58, lost));
        assertThrows(IllegalArgumentException.class,
                () -> ReconDroneMissionPolicy.outcome(ReconDroneMissionPolicy.Status.ACTIVE));
    }

    @Test
    void aLaunchThatNeverReachedTheMapReturnsTheCooldown() {
        for (SupportSpawnException launchFailure : List.of(
                ReconDroneMissionPolicy.airspaceUnloaded(),
                ReconDroneMissionPolicy.launchRejected())) {
            assertTrue(launchFailure.refundCooldown(), launchFailure.getMessage());
            assertFalse(launchFailure.providerBroken(), launchFailure.getMessage());
            assertSame(launchFailure, ReconDroneMissionPolicy.classify(0, launchFailure));
        }

        // A missing commander or battle, or a rejected first publication, also refunds at step 0.
        for (SupportSpawnException ordinary : List.of(ReconDroneMissionPolicy.noCommander(),
                ReconDroneMissionPolicy.noBattle(),
                SupportSpawnException.endMission("临时情报只能由在线指挥官支援任务发布"))) {
            SupportSpawnException classified = ReconDroneMissionPolicy.classify(0, ordinary);
            assertTrue(classified.refundCooldown());
            assertFalse(classified.providerBroken());
            assertEquals(ordinary.getMessage(), classified.getMessage());
            assertSame(ordinary, classified.getCause());
            // From step 1 on the call is spent.
            assertSame(ordinary, ReconDroneMissionPolicy.classify(5, ordinary));
        }
    }

    @Test
    void faultsTripTheCircuitAndRefundOnlyAtLaunch() {
        SupportSpawnException brokenBatch =
                SupportSpawnException.providerBroken("临时情报目标超出支援扫描区域", null, false);
        SupportSpawnException atLaunch = ReconDroneMissionPolicy.classify(0, brokenBatch);
        assertTrue(atLaunch.providerBroken());
        assertTrue(atLaunch.refundCooldown());
        assertSame(brokenBatch, ReconDroneMissionPolicy.classify(10, brokenBatch));

        IllegalStateException runtime = new IllegalStateException("boom");
        SupportSpawnException launchFault = ReconDroneMissionPolicy.classify(0, runtime);
        assertTrue(launchFault.providerBroken());
        assertTrue(launchFault.refundCooldown());
        assertEquals("侦察无人机执行异常", launchFault.getMessage());
        assertSame(runtime, launchFault.getCause());

        SupportSpawnException laterFault = ReconDroneMissionPolicy.classify(20,
                new NoClassDefFoundError("missing"));
        assertTrue(laterFault.providerBroken());
        assertFalse(laterFault.refundCooldown());

        assertThrows(NullPointerException.class,
                () -> ReconDroneMissionPolicy.classify(0, null));
        assertThrows(IllegalArgumentException.class,
                () -> ReconDroneMissionPolicy.classify(-1, runtime));
    }

    @Test
    void scansPublishOnlyWhileTheRequesterStaysInTheAcceptedFaction() {
        assertFalse(ReconDroneMissionPolicy.requesterLeftMissionFaction(Faction.BLUE,
                Faction.BLUE));
        assertFalse(ReconDroneMissionPolicy.requesterLeftMissionFaction(Faction.RED,
                Faction.RED));
        assertTrue(ReconDroneMissionPolicy.requesterLeftMissionFaction(Faction.BLUE,
                Faction.RED));
        assertTrue(ReconDroneMissionPolicy.requesterLeftMissionFaction(Faction.BLUE, null));
        assertTrue(ReconDroneMissionPolicy.requesterLeftMissionFaction(null, Faction.BLUE));
    }
}
