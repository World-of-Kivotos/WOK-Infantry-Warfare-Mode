package com.wok.infantry.battle;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One squad of the viewer's formation.
 *
 * @param classLimits per-class limits of this squad and how many members hold each class
 *                    (battle protocol 20), in formation class order; empty when unknown
 */
public record SquadView(
        SquadCallsign callsign,
        UUID leaderId,
        List<MemberView> members,
        int capacity,
        List<ClassLimitView> classLimits
) {
    public SquadView {
        Objects.requireNonNull(callsign, "callsign");
        members = List.copyOf(Objects.requireNonNullElse(members, List.of()));
        classLimits = List.copyOf(Objects.requireNonNullElse(classLimits, List.of()));
    }

    /** Compatibility constructor for callers written before protocol 20 (no class limits). */
    public SquadView(SquadCallsign callsign, UUID leaderId, List<MemberView> members,
                     int capacity) {
        this(callsign, leaderId, members, capacity, List.of());
    }

    public boolean active() {
        return !members.isEmpty();
    }

    /** No free slot left (the squad's own capacity, never the formation-wide maximum). */
    public boolean full() {
        return members.size() >= capacity;
    }

    /** This squad's limit for {@code classId}, or {@code null} when the squad lists none. */
    public ClassLimitView classLimit(String classId) {
        if (classId == null) {
            return null;
        }
        for (ClassLimitView limit : classLimits) {
            if (limit.classId().equals(classId)) {
                return limit;
            }
        }
        return null;
    }
}
