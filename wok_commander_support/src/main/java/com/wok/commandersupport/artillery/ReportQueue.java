package com.wok.commandersupport.artillery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure delayed-cue queue grouped by mission call id; nothing is persisted.
 *
 * <p>A group lives only while its lease holds. The provider renews the lease after every mission
 * step, so the remaining cues of a mission that silently stopped stepping (cancelled by the core
 * before step 0, which calls no cleanup, or cleared by a battle reset) are dropped once the lease
 * runs out instead of playing for a barrage that never comes. An emptied group is removed at once.
 * The group count is capped; scheduling past the cap drops the oldest group.</p>
 *
 * @param <T> cue payload
 */
final class ReportQueue<T> {
    private final int maxGroups;
    private final LinkedHashMap<UUID, Group<T>> groups = new LinkedHashMap<>();

    ReportQueue(int maxGroups) {
        if (maxGroups < 1) {
            throw new IllegalArgumentException("Report queue needs room for one group");
        }
        this.maxGroups = maxGroups;
    }

    /** Replaces the call's cues; they play in due order, equal times in the given order. */
    synchronized void schedule(UUID callId, Collection<Timed<T>> cues, long leaseUntil) {
        Objects.requireNonNull(callId, "callId");
        List<Timed<T>> sorted = new ArrayList<>(Objects.requireNonNull(cues, "cues"));
        for (Timed<T> cue : sorted) {
            Objects.requireNonNull(cue, "cue");
        }
        sorted.sort(Comparator.comparingLong(Timed::dueAt));
        groups.remove(callId);
        if (sorted.isEmpty()) {
            return;
        }
        groups.put(callId, new Group<>(new ArrayDeque<>(sorted), leaseUntil));
        Iterator<UUID> oldest = groups.keySet().iterator();
        while (groups.size() > maxGroups && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
    }

    /** Extends the lease (never shortens it). Returns false when the call has nothing pending. */
    synchronized boolean renew(UUID callId, long leaseUntil) {
        Group<T> group = groups.get(callId);
        if (group == null) {
            return false;
        }
        group.leaseUntil = Math.max(group.leaseUntil, leaseUntil);
        return true;
    }

    /** Drops every remaining cue of the call. Returns whether anything was pending. */
    synchronized boolean cancel(UUID callId) {
        return callId != null && groups.remove(callId) != null;
    }

    synchronized void clear() {
        groups.clear();
    }

    synchronized boolean isEmpty() {
        return groups.isEmpty();
    }

    synchronized int pending(UUID callId) {
        Group<T> group = groups.get(callId);
        return group == null ? 0 : group.cues.size();
    }

    synchronized int groupCount() {
        return groups.size();
    }

    /**
     * Removes and returns every cue due at {@code now}, earliest first; groups whose lease ended
     * before {@code now} are dropped whole and reported as expired.
     */
    synchronized Drain<T> drain(long now) {
        List<Due<T>> due = new ArrayList<>();
        List<UUID> expired = new ArrayList<>();
        Iterator<Map.Entry<UUID, Group<T>>> iterator = groups.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Group<T>> entry = iterator.next();
            Group<T> group = entry.getValue();
            if (now > group.leaseUntil) {
                expired.add(entry.getKey());
                iterator.remove();
                continue;
            }
            while (!group.cues.isEmpty() && group.cues.peekFirst().dueAt() <= now) {
                Timed<T> cue = group.cues.pollFirst();
                due.add(new Due<>(entry.getKey(), cue.dueAt(), cue.cue()));
            }
            if (group.cues.isEmpty()) {
                iterator.remove();
            }
        }
        due.sort(Comparator.comparingLong(Due::dueAt));
        return new Drain<>(List.copyOf(due), List.copyOf(expired));
    }

    /** A cue and the game tick at which it becomes due. */
    record Timed<T>(long dueAt, T cue) {
        Timed {
            Objects.requireNonNull(cue, "cue");
        }
    }

    /** A cue taken out of the queue for playback. */
    record Due<T>(UUID callId, long dueAt, T cue) {
    }

    /** Result of one {@link #drain}: cues to play now and calls whose lease ran out. */
    record Drain<T>(List<Due<T>> due, List<UUID> expired) {
    }

    private static final class Group<T> {
        private final ArrayDeque<Timed<T>> cues;
        private long leaseUntil;

        private Group(ArrayDeque<Timed<T>> cues, long leaseUntil) {
            this.cues = cues;
            this.leaseUntil = leaseUntil;
        }
    }
}
