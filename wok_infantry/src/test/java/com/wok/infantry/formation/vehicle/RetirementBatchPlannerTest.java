package com.wok.infantry.formation.vehicle;

import com.wok.infantry.battle.ActionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetirementBatchPlannerTest {
    @Test
    void laterPredictableConflictDiscardsEveryEarlierProvisionalCandidate() {
        AtomicInteger inspections = new AtomicInteger();

        RetirementBatchPlanner.Plan<String, String> plan =
                RetirementBatchPlanner.preflight(List.of("loaded", "unloaded", "conflict"),
                        key -> {
                            inspections.incrementAndGet();
                            if ("loaded".equals(key)) {
                                return RetirementBatchPlanner.Inspection.loaded("entity");
                            }
                            if ("unloaded".equals(key)) {
                                return RetirementBatchPlanner.Inspection.unloaded();
                            }
                            return RetirementBatchPlanner.Inspection.failure(
                                    ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                                            "ownership conflict"));
                        });

        assertFalse(plan.result().success());
        assertEquals(3, inspections.get());
        assertTrue(plan.loaded().isEmpty());
        assertTrue(plan.unloaded().isEmpty());
    }

    @Test
    void successfulPreflightReturnsCompleteLoadedAndTombstonePlansInBatchOrder() {
        RetirementBatchPlanner.Plan<String, String> plan =
                RetirementBatchPlanner.preflight(List.of("first", "offline", "second"),
                        key -> "offline".equals(key)
                                ? RetirementBatchPlanner.Inspection.unloaded()
                                : RetirementBatchPlanner.Inspection.loaded(key + "-entity"));

        assertTrue(plan.result().success());
        assertEquals(List.of("first", "second"), List.copyOf(plan.loaded().keySet()));
        assertEquals(List.of("offline"), plan.unloaded());
    }

    @Test
    void inspectionExceptionFailsClosedWithoutReturningEarlierCandidates() {
        RetirementBatchPlanner.Plan<String, String> plan =
                RetirementBatchPlanner.preflight(List.of("first", "throws"),
                        key -> {
                            if ("throws".equals(key)) {
                                throw new IllegalStateException("world lookup failed");
                            }
                            return RetirementBatchPlanner.Inspection.loaded("entity");
                        });

        assertFalse(plan.result().success());
        assertTrue(plan.loaded().isEmpty());
        assertTrue(plan.unloaded().isEmpty());
    }
}
