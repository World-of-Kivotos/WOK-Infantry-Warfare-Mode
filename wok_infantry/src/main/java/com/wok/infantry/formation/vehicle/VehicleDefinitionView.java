package com.wok.infantry.formation.vehicle;

/**
 * Minimal adapter boundary between formation configuration and the optional vehicle provider.
 * Formation definitions can implement this interface directly or be copied into
 * {@link VehicleAllocationSpec}; the provider never needs to know their JSON model.
 */
public interface VehicleDefinitionView {
    String allocationId();

    String entityId();

    double offsetX();

    double offsetY();

    double offsetZ();

    float yawOffset();
}
