package com.wok.infantry.ammo.transport;

/** Immutable, bounded state for one configured supply vehicle. */
public record SupplyCargoState(int capacity, int remaining) {
    public SupplyCargoState {
        capacity = Math.max(0, Math.min(SupplyTransportRules.MAX_VEHICLE_CAPACITY, capacity));
        remaining = Math.max(0, Math.min(capacity, remaining));
    }

    public static SupplyCargoState full(int capacity) {
        return new SupplyCargoState(capacity, capacity);
    }

    public SupplyCargoState withCapacity(int newCapacity) {
        return new SupplyCargoState(newCapacity, remaining);
    }

    public SupplyCargoState takeOne() {
        return remaining > 0 ? new SupplyCargoState(capacity, remaining - 1) : this;
    }

    public boolean empty() {
        return remaining == 0;
    }
}
