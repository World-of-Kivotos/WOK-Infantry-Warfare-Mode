package com.wok.infantry.formation.vehicle;

import java.util.Objects;
import java.util.UUID;

/** Stable identity of one formation allocation within one battle session. */
public record VehicleAllocationKey(
        UUID sessionId,
        String factionId,
        String formationId,
        String allocationId
) {
    public VehicleAllocationKey {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(formationId, "formationId");
        Objects.requireNonNull(allocationId, "allocationId");
    }
}
