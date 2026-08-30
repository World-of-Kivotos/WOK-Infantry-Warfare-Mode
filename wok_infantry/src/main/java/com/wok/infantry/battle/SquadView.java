package com.wok.infantry.battle;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SquadView(
        SquadCallsign callsign,
        UUID leaderId,
        List<MemberView> members,
        int capacity
) {
    public SquadView {
        Objects.requireNonNull(callsign, "callsign");
        members = List.copyOf(Objects.requireNonNullElse(members, List.of()));
    }

    public boolean active() {
        return !members.isEmpty();
    }
}
