package com.wok.infantry.deployment;

import com.wok.infantry.battle.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** One physical, cardinal vehicle-batch origin owned by a battle faction. */
public record VehicleDeploymentPoint(
        Faction faction,
        ResourceLocation dimension,
        BlockPos anchorPosition,
        Direction facing
) {
    public VehicleDeploymentPoint {
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(anchorPosition, "anchorPosition");
        Objects.requireNonNull(facing, "facing");
        anchorPosition = anchorPosition.immutable();
        if (!facing.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Vehicle deployment facing must be horizontal");
        }
    }

    public float yaw() {
        return facing.toYRot();
    }
}
