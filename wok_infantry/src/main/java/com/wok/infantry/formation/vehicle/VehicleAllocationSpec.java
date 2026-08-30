package com.wok.infantry.formation.vehicle;

import com.wok.infantry.formation.FormationVehicleDefinition;
import net.minecraft.resources.ResourceLocation;

/** A provider-neutral vehicle allocation copied from one formation definition. */
public record VehicleAllocationSpec(
        String allocationId,
        String entityId,
        double offsetX,
        double offsetY,
        double offsetZ,
        float yawOffset
) implements VehicleDefinitionView {
    public static VehicleAllocationSpec of(String allocationId, ResourceLocation entityId,
                                           double offsetX, double offsetY, double offsetZ,
                                           float yawOffset) {
        return new VehicleAllocationSpec(allocationId,
                entityId == null ? null : entityId.toString(),
                offsetX, offsetY, offsetZ, yawOffset);
    }

    public static VehicleAllocationSpec copyOf(VehicleDefinitionView definition) {
        if (definition == null) {
            return null;
        }
        return new VehicleAllocationSpec(definition.allocationId(), definition.entityId(),
                definition.offsetX(), definition.offsetY(), definition.offsetZ(),
                definition.yawOffset());
    }

    /** Explicit adapter for the server-owned formation catalog model. */
    public static VehicleAllocationSpec fromFormation(FormationVehicleDefinition definition) {
        if (definition == null) {
            return null;
        }
        return new VehicleAllocationSpec(definition.id(), definition.entityId(),
                definition.offsetX(), definition.offsetY(), definition.offsetZ(),
                definition.yaw());
    }
}
