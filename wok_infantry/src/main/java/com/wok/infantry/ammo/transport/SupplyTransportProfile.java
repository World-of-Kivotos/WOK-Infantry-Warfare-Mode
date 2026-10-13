package com.wok.infantry.ammo.transport;

import java.util.Objects;

/** Cargo type and full-load quantity assigned to one vehicle entity type. */
public record SupplyTransportProfile(SupplyCargoType cargoType, int capacity) {
    public SupplyTransportProfile {
        cargoType = Objects.requireNonNull(cargoType, "cargoType");
        if (capacity < 1 || capacity > SupplyTransportRules.MAX_VEHICLE_CAPACITY) {
            throw new IllegalArgumentException("capacity outside supported range");
        }
    }
}
