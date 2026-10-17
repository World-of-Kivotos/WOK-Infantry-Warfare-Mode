package com.wok.infantry.formation.vehicle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Fully resolved but not yet world-validated target for one allocation. */
public record VehicleSpawnPlan(
        String allocationId,
        ResourceLocation entityId,
        ResourceLocation dimension,
        Vec3 position,
        float yaw
) {
}
