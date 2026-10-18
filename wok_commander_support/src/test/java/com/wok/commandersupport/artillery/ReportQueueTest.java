package com.wok.commandersupport.artillery;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportQueueTest {
    private static final UUID FIRST = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-4000-8000-000000000002");

    @Test
    void cuesPlayOnceWhenDueAndInTimeOrderAcrossCalls() {
        ReportQueue<String> queue = new ReportQueue<>(8);
        queue.schedule(FIRST, List.of(timed(110, "a2"), timed(100, "a1")), 1_000L);
        queue.schedule(SECOND, List.of(timed(105, "b1")), 1_000L);

        assertTrue(cues(queue.drain(99L)).isEmpty());
        assertEquals(List.of("a1"), cues(queue.drain(100L)));
        assertEquals(List.of("b1", "a2"), cues(queue.drain(120L)),
                "a late drain still plays both, earliest first");
        assertTrue(cues(queue.drain(500L)).isEmpty());
        assertTrue(queue.isEmpty(), "emptied groups are removed");
    }

    @Test
    void equalTimesKeepTheScheduledOrder() {
        ReportQueue<String> queue = new ReportQueue<>(8);
        queue.schedule(FIRST, List.of(timed(5, "x"), timed(5, "y"), timed(5, "z")), 100L);

        assertEquals(List.of("x", "y", "z"), cues(queue.drain(5L)));
    }

    @Test
    void cancelDropsOnlyThatCall() {
        ReportQueue<String> queue = new ReportQueue<>(8);
        queue.schedule(FIRST, List.of(timed(10, "a")), 100L);
        queue.schedule(SECOND, List.of(timed(10, "b")), 100L);

        assertTrue(queue.cancel(FIRST));
        assertFalse(queue.cancel(FIRST), "cancelling twice is harmless");
        assertFalse(queue.cancel(null));
        assertEquals(0, queue.pending(FIRST));
        assertEquals(List.of("b"), cues(queue.drain(10L)));
    }

    @Test
    void anExpiredLeaseDropsTheRemainingCuesAndReportsTheCall() {
        ReportQueue<String> queue = new ReportQueue<>(8);
        queue.schedule(FIRST, List.of(timed(10, "early"), timed(60, "late")), 30L);

        assertEquals(List.of("early"), cues(queue.drain(10L)));
        ReportQueue.Drain<String> drained = queue.drain(31L);
        assertTrue(drained.due().isEmpty());
        assertEquals(List.of(FIRST), drained.expired());
        assertTrue(queue.isEmpty());
        assertTrue(cues(queue.drain(60L)).isEmpty(), "the dropped cue never plays");
    }

    @Test
    void renewingExtendsButNeverShortensTheLease() {
        ReportQueue<String> queue = new ReportQueue<>(8);
        queue.schedule(FIRST, List.of(timed(60, "late")), 30L);

        assertTrue(queue.renew(FIRST, 70L));
        assertTrue(queue.renew(FIRST, 40L));
        assertFalse(queue.renew(SECOND, 70L), "nothing pending for an unknown call");
        assertTrue(queue.drain(55L).expired().isEmpty());
        assertEquals(List.of("late"), cues(queue.drain(60L)));
    }

    @Test
    void reschedulingReplacesACallAndTheGroupCountIsCapped() {
        ReportQueue<String> queue = new ReportQueue<>(2);
        queue.schedule(FIRST, List.of(timed(10, "old")), 100L);
        queue.schedule(FIRST, List.of(timed(20, "new")), 100L);
        assertEquals(1, queue.groupCount());
        assertEquals(1, queue.pending(FIRST));

        queue.schedule(SECOND, List.of(timed(10, "b")), 100L);
        UUID third = UUID.randomUUID();
        queue.schedule(third, List.of(timed(10, "c")), 100L);
        assertEquals(2, queue.groupCount());
        assertEquals(0, queue.pending(FIRST), "the oldest call gives way");

        queue.schedule(SECOND, List.of(), 100L);
        assertEquals(0, queue.pending(SECOND), "an empty schedule clears the call");
        queue.clear();
        assertTrue(queue.isEmpty());
    }

    @Test
    void invalidInputFailsClosed() {
        ReportQueue<String> queue = new ReportQueue<>(1);
        assertThrows(IllegalArgumentException.class, () -> new ReportQueue<String>(0));
        assertThrows(NullPointerException.class, () -> queue.schedule(null, List.of(), 1L));
        assertThrows(NullPointerException.class, () -> queue.schedule(FIRST, null, 1L));
        List<ReportQueue.Timed<String>> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> queue.schedule(FIRST, withNull, 1L));
        assertThrows(NullPointerException.class, () -> new ReportQueue.Timed<String>(1L, null));
    }

    private static ReportQueue.Timed<String> timed(long dueAt, String cue) {
        return new ReportQueue.Timed<>(dueAt, cue);
    }

    private static List<String> cues(ReportQueue.Drain<String> drained) {
        List<String> cues = new ArrayList<>();
        for (ReportQueue.Due<String> due : drained.due()) {
            cues.add(due.cue());
        }
        return cues;
    }
}
