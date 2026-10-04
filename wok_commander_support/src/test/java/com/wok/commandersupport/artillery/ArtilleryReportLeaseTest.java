package com.wok.commandersupport.artillery;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the report queue with the provider's lease rules on the core's mission clock: a running
 * mission never loses a report, a mission that silently stops loses the rest shortly after.
 */
class ArtilleryReportLeaseTest {
    private static final List<ArtilleryProfile> ALL = List.of(ArtilleryProfile.HOWITZER_3ROUND,
            ArtilleryProfile.RAPID_105, ArtilleryProfile.FIVE_ROUND_105);
    private static final UUID CALL = UUID.fromString("5d6e7f80-91a2-4b3c-8d4e-5f60718293a4");
    private static final long ACCEPTED_AT = 12_345L;

    @Test
    void leasesCoverTheNextExpectedStepPlusGrace() {
        assertEquals(20L, ArtilleryReportScheduler.LEASE_GRACE_TICKS);
        assertEquals(1_000L + 170L + 20L,
                ArtilleryReportScheduler.acceptanceLease(1_000L, 170L));
        assertEquals(1_000L + 10L + 20L, ArtilleryReportScheduler.stepLease(1_000L));
        assertEquals(Long.MAX_VALUE,
                ArtilleryReportScheduler.acceptanceLease(Long.MAX_VALUE - 5L, 170L));
    }

    @Test
    void aMissionThatKeepsSteppingHearsEveryReport() {
        for (ArtilleryProfile profile : ALL) {
            for (int stepDelay = 0; stepDelay <= 3; stepDelay++) {
                Run run = simulate(profile, profile.stepCount(), stepDelay);
                assertEquals(profile.totalRounds() - 1, run.played().size(),
                        profile.id() + " with steps " + stepDelay + " ticks late");
                assertTrue(run.expired().isEmpty(), profile.id().toString());
            }
        }
    }

    @Test
    void aMissionCancelledBeforeStepZeroOnlyKeepsReportsInsideTheInboundLease() {
        for (ArtilleryProfile profile : ALL) {
            Run run = simulate(profile, 0, 0);
            long lease = ArtilleryReportScheduler.acceptanceLease(ACCEPTED_AT,
                    profile.inboundTicks());
            long expected = ArtilleryReportPlan.reports(profile).stream()
                    .filter(report -> report.delayTicks() > 0L
                            && ACCEPTED_AT + report.delayTicks() <= lease)
                    .count();
            assertEquals(expected, run.played().size(), profile.id().toString());
            assertTrue(expected < profile.totalRounds() - 1,
                    profile.id() + " must have reports after step 0 to drop");
            assertEquals(List.of(CALL), run.expired());
        }
    }

    @Test
    void aMissionThatStopsSteppingDropsTheRestAfterTheGrace() {
        ArtilleryProfile profile = ArtilleryProfile.HOWITZER_3ROUND;
        // Steps 0..4 run, then a battle reset clears the mission without any cleanup call.
        Run run = simulate(profile, 5, 0);
        long lastStep = ACCEPTED_AT + profile.inboundTicks() + 4L * 10L;
        long lease = ArtilleryReportScheduler.stepLease(lastStep);
        for (long dueAt : run.played()) {
            assertTrue(dueAt <= lease, "report at " + dueAt + " after lease " + lease);
        }
        assertEquals(List.of(CALL), run.expired());
        // Wave 2 fires at step 3 and is still heard; waves 3 and 4 are dropped.
        assertEquals(2 + 3 + 3, run.played().size());
    }

    /**
     * Ticks the queue with the scheduler running before the mission step of the same tick (the
     * worse order), mission steps arriving {@code stepDelay} ticks late, and only the first
     * {@code stepsRun} steps ever executing.
     */
    private static Run simulate(ArtilleryProfile profile, int stepsRun, int stepDelay) {
        ReportQueue<Long> queue = new ReportQueue<>(ArtilleryReportScheduler.MAX_GROUPS);
        List<ReportQueue.Timed<Long>> later = new ArrayList<>();
        for (ArtilleryReportPlan.Report report : ArtilleryReportPlan.reports(profile)) {
            if (report.delayTicks() > 0L) {
                long dueAt = ACCEPTED_AT + report.delayTicks();
                later.add(new ReportQueue.Timed<>(dueAt, dueAt));
            }
        }
        queue.schedule(CALL, later, ArtilleryReportScheduler.acceptanceLease(ACCEPTED_AT,
                profile.inboundTicks()));
        List<Long> played = new ArrayList<>();
        List<UUID> expired = new ArrayList<>();
        long end = ACCEPTED_AT + profile.preparationTicks()
                + (long) profile.waves() * profile.waveIntervalTicks() + 200L;
        for (long now = ACCEPTED_AT; now <= end; now++) {
            ReportQueue.Drain<Long> drained = queue.drain(now);
            for (ReportQueue.Due<Long> due : drained.due()) {
                assertEquals(now, due.cue().longValue(), "reports play on their own tick");
                played.add(due.cue());
            }
            expired.addAll(drained.expired());
            long sinceStepZero = now - (ACCEPTED_AT + profile.inboundTicks() + stepDelay);
            if (sinceStepZero >= 0L && sinceStepZero % ArtilleryProfile.STEP_INTERVAL_TICKS == 0L) {
                int stepIndex = (int) (sinceStepZero / ArtilleryProfile.STEP_INTERVAL_TICKS);
                if (stepIndex < stepsRun) {
                    if (stepIndex == profile.stepCount() - 1) {
                        queue.cancel(CALL);
                    } else {
                        queue.renew(CALL, ArtilleryReportScheduler.stepLease(now));
                    }
                }
            }
        }
        return new Run(played, expired);
    }

    private record Run(List<Long> played, List<UUID> expired) {
    }
}
