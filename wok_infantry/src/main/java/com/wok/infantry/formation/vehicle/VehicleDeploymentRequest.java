package com.wok.infantry.formation.vehicle;

import com.wok.infantry.formation.FormationVehicleDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Immutable server-authored request for one formation vehicle batch. */
public record VehicleDeploymentRequest(
        UUID sessionId,
        String factionId,
        String formationId,
        ResourceLocation dimension,
        Vec3 basePosition,
        float baseYaw,
        List<VehicleAllocationSpec> vehicles
) {
    public VehicleDeploymentRequest {
        // Preserve invalid/null entries for the planner to reject with an ActionResult instead
        // of leaking a constructor NullPointerException across a command/event boundary.
        vehicles = vehicles == null ? null
                : Collections.unmodifiableList(new ArrayList<>(vehicles));
    }

    public static VehicleDeploymentRequest fromViews(
            UUID sessionId, String factionId, String formationId,
            ResourceLocation dimension, Vec3 basePosition, float baseYaw,
            List<? extends VehicleDefinitionView> vehicles) {
        List<VehicleAllocationSpec> copied = vehicles == null ? null : vehicles.stream()
                .map(VehicleAllocationSpec::copyOf)
                .toList();
        return new VehicleDeploymentRequest(sessionId, factionId, formationId,
                dimension, basePosition, baseYaw, copied);
    }

    public static VehicleDeploymentRequest fromFormationDefinitions(
            UUID sessionId, String factionId, String formationId,
            ResourceLocation dimension, Vec3 basePosition, float baseYaw,
            List<FormationVehicleDefinition> vehicles) {
        List<VehicleAllocationSpec> copied = vehicles == null ? null : vehicles.stream()
                .map(VehicleAllocationSpec::fromFormation)
                .toList();
        return new VehicleDeploymentRequest(sessionId, factionId, formationId,
                dimension, basePosition, baseYaw, copied);
    }
}
