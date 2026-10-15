package com.wok.infantry.formation.vehicle;

import com.wok.infantry.battle.ActionResult;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Action status plus the stable entity UUID assigned to every requested allocation. */
public record VehicleDeploymentResult(ActionResult result, Map<String, UUID> entities) {
    public VehicleDeploymentResult {
        Objects.requireNonNull(result, "result");
        entities = entities == null ? Map.of() : Map.copyOf(entities);
    }

    public static VehicleDeploymentResult success(String message, Map<String, UUID> entities) {
        return new VehicleDeploymentResult(ActionResult.ok(message), entities);
    }

    public static VehicleDeploymentResult failure(ActionResult result) {
        return new VehicleDeploymentResult(result, Map.of());
    }
}
