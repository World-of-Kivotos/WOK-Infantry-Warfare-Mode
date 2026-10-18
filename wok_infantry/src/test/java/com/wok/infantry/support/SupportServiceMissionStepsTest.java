package com.wok.infantry.support;

import com.wok.infantry.support.adapter.SupportSpawnException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportServiceMissionStepsTest {
    private static final long NOW = 1_000L;

    @Test
    void budgetCapsProviderCallbacksAcrossBackloggedSteps() {
        SupportService.MissionCursor cursor = new SupportService.MissionCursor(
                NOW - 10L, 0, 6, 1);
        List<Integer> executed = new ArrayList<>();

        SupportService.StepRun run = SupportService.runDueSteps(cursor, NOW, 4,
                executed::add);

        assertEquals(4, run.callbacks());
        assertNull(run.end());
        assertEquals(List.of(0, 1, 2, 3), executed);
        assertEquals(4, cursor.nextStepIndex());
        assertEquals(2, cursor.remainingSteps());
        assertEquals(NOW - 6L, cursor.nextStepAtGameTick());

        SupportService.StepRun idle = SupportService.runDueSteps(cursor, NOW, 0,
                executed::add);
        assertEquals(0, idle.callbacks());
        assertNull(idle.end());
    }

    @Test
    void cursorAdvancesByTheStepIntervalAndWaitsForTheNextDueTick() {
        SupportService.MissionCursor cursor = new SupportService.MissionCursor(
                NOW, 0, 3, 20);
        List<Integer> executed = new ArrayList<>();

        SupportService.StepRun first = SupportService.runDueSteps(cursor, NOW, 4,
                executed::add);
        assertEquals(1, first.callbacks());
        assertNull(first.end());
        assertEquals(NOW + 20L, cursor.nextStepAtGameTick());

        SupportService.StepRun early = SupportService.runDueSteps(cursor, NOW + 19L, 4,
                executed::add);
        assertEquals(0, early.callbacks());
        assertNull(early.end());

        SupportService.runDueSteps(cursor, NOW + 20L, 4, executed::add);
        SupportService.StepRun last = SupportService.runDueSteps(cursor, NOW + 40L, 4,
                executed::add);
        assertEquals(1, last.callbacks());
        assertSame(SupportService.MissionEnd.COMPLETED, last.end());
        assertTrue(last.end().completed());
        assertEquals(List.of(0, 1, 2), executed);
        assertEquals(0, cursor.remainingSteps());
    }

    @Test
    void endMissionStopsWithoutRefundOrCircuitBreak() {
        SupportService.MissionCursor cursor = new SupportService.MissionCursor(
                NOW, 0, 4, 1);

        SupportService.StepRun run = SupportService.runDueSteps(cursor, NOW, 4,
                step -> {
                    throw SupportSpawnException.endMission("战局服务不可用");
                });

        assertEquals(1, run.callbacks());
        SupportService.MissionEnd end = run.end();
        assertFalse(end.completed());
        assertFalse(end.refundCooldown());
        assertFalse(end.providerBroken());
        assertEquals("战局服务不可用", end.reason());
        assertEquals(0, cursor.nextStepIndex());
    }

    @Test
    void notDeliveredRefundsOnlyBeforeAnyStepCompleted() {
        SupportService.MissionCursor unstarted = new SupportService.MissionCursor(
                NOW, 0, 4, 1);
        SupportService.MissionEnd refunded = SupportService.runDueSteps(unstarted, NOW, 4,
                step -> {
                    throw SupportSpawnException.notDelivered("没有友军持续照射");
                }).end();
        assertFalse(refunded.completed());
        assertTrue(refunded.refundCooldown());
        assertFalse(refunded.providerBroken());
        assertEquals("没有友军持续照射", refunded.reason());

        SupportService.MissionCursor started = new SupportService.MissionCursor(
                NOW - 5L, 0, 4, 1);
        SupportService.StepRun lateRun = SupportService.runDueSteps(started, NOW, 4,
                step -> {
                    if (step == 2) {
                        throw SupportSpawnException.notDelivered("弹体已丢失");
                    }
                });
        assertEquals(3, lateRun.callbacks());
        assertFalse(lateRun.end().refundCooldown());
        assertEquals(2, started.nextStepIndex());
    }

    @Test
    void providerBrokenEndsTheMissionAndKeepsTheDeclaredRefundBeforeDelivery() {
        SupportService.MissionEnd keep = SupportService.runDueSteps(
                new SupportService.MissionCursor(NOW, 0, 2, 1), NOW, 4, step -> {
                    throw SupportSpawnException.providerBroken("适配器异常", null, false);
                }).end();
        assertTrue(keep.providerBroken());
        assertFalse(keep.refundCooldown());

        SupportService.MissionEnd refund = SupportService.runDueSteps(
                new SupportService.MissionCursor(NOW, 0, 2, 1), NOW, 4, step -> {
                    throw SupportSpawnException.providerBroken("适配器异常", null, true);
                }).end();
        assertTrue(refund.providerBroken());
        assertTrue(refund.refundCooldown());
        assertFalse(refund.completed());
    }

    @Test
    void coreCancellationRefundsOnlyBeforeTheFirstCompletedStep() {
        assertTrue(SupportService.refundOnCoreCancel(0));
        assertFalse(SupportService.refundOnCoreCancel(1));
        assertFalse(SupportService.refundOnCoreCancel(5));

        SupportService.MissionEnd beforeDelivery =
                SupportService.MissionEnd.coreCancelled("支援覆盖范围存在未加载区块", 0);
        assertTrue(beforeDelivery.refundCooldown());
        assertFalse(beforeDelivery.providerBroken());
        assertFalse(SupportService.MissionEnd.coreCancelled("呼叫者已不在受理阵营", 2)
                .refundCooldown());

        SupportService.MissionEnd crashed = SupportService.MissionEnd.crashed("支援任务执行异常");
        assertTrue(crashed.providerBroken());
        assertFalse(crashed.refundCooldown());
    }

    @Test
    void exactlyTheNonRefundingCoreCancellationsAskTheProviderToCleanUp() {
        // Before step 0 nothing is in the world and the cooldown comes back; afterwards the
        // provider removes what the mission placed and the cooldown stays spent.
        assertFalse(SupportService.abandonOnCoreCancel(0));
        for (int nextStep = 0; nextStep <= 61; nextStep++) {
            assertEquals(!SupportService.refundOnCoreCancel(nextStep),
                    SupportService.abandonOnCoreCancel(nextStep), "step " + nextStep);
        }
    }

    @Test
    void requesterNoticeDescribesTheRefundOutcome() {
        assertEquals("[支援] 侦察卫星 未能投送：没有友军持续照射，冷却已返还",
                SupportService.failureNotice("侦察卫星", "没有友军持续照射", true, true));
        assertEquals("[支援] 侦察卫星 未能投送：没有友军持续照射",
                SupportService.failureNotice("侦察卫星", "没有友军持续照射", true, false));
        assertEquals("[支援] 侦察卫星 任务中止：战局服务不可用",
                SupportService.failureNotice("侦察卫星", "战局服务不可用", false, false));
        assertEquals("[支援] 侦察卫星 任务中止：原因未知",
                SupportService.failureNotice("侦察卫星", null, false, false));

        String bounded = SupportService.failureNotice("侦察卫星", "原".repeat(500),
                false, false);
        assertEquals("[支援] 侦察卫星 任务中止：".length()
                + SupportOptionView.MAX_AVAILABILITY_REASON_LENGTH, bounded.length());
    }
}
