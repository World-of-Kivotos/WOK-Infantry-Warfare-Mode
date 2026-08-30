package com.wok.infantry.formation.vehicle;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/** Server-authored ownership read from a WOK-managed vehicle entity. */
public record VehicleOwnership(
        UUID sessionId,
        String factionId,
        String formationId,
        String allocationId,
        ResourceLocation entityTypeId,
        ResourceLocation dimension
) {
    public VehicleOwnership {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(formationId, "formationId");
        Objects.requireNonNull(allocationId, "allocationId");
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        Objects.requireNonNull(dimension, "dimension");
    }

    public VehicleAllocationKey allocationKey() {
        return new VehicleAllocationKey(sessionId, factionId, formationId, allocationId);
    }
}
