package com.wok.infantry.deployment;

import com.wok.infantry.battle.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/** Server-persistent identity for one block-backed, faction deployment point. */
record FieldDeploymentPoint(
        UUID id,
        Faction faction,
        ResourceLocation dimension,
        BlockPos anchorPosition,
        BlockPos spawnPosition,
        float yaw
) {
    FieldDeploymentPoint {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(anchorPosition, "anchorPosition");
        Objects.requireNonNull(spawnPosition, "spawnPosition");
        anchorPosition = anchorPosition.immutable();
        spawnPosition = spawnPosition.immutable();
        int deltaX = Math.abs(spawnPosition.getX() - anchorPosition.getX());
        int deltaY = spawnPosition.getY() - anchorPosition.getY();
        int deltaZ = Math.abs(spawnPosition.getZ() - anchorPosition.getZ());
        if (deltaX > 2 || deltaZ > 2 || deltaY < 1 || deltaY > 3) {
            throw new IllegalArgumentException(
                    "Field deployment spawn must stay within the beacon safety scan");
        }
        if (!Float.isFinite(yaw) || yaw < 0.0F || yaw >= 360.0F) {
            throw new IllegalArgumentException("Field deployment point yaw must be in [0, 360)");
        }
    }
}
