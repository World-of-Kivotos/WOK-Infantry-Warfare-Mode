package com.wok.infantry.deployment;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadCallsign;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/** Server-persistent identity for one squad-owned, block-backed rally radio. */
record RallyDeploymentPoint(UUID id, Faction faction, String formationId,
                            SquadCallsign squad, ResourceLocation dimension,
                            BlockPos anchorPosition, BlockPos spawnPosition, float yaw) {
    RallyDeploymentPoint {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(faction, "faction");
        formationId = Objects.requireNonNull(formationId, "formationId").trim();
        Objects.requireNonNull(squad, "squad");
        Objects.requireNonNull(dimension, "dimension");
        anchorPosition = Objects.requireNonNull(anchorPosition, "anchorPosition").immutable();
        spawnPosition = Objects.requireNonNull(spawnPosition, "spawnPosition").immutable();
        if (formationId.isEmpty()) {
            throw new IllegalArgumentException("Rally formation id cannot be blank");
        }
        int deltaX = Math.abs(spawnPosition.getX() - anchorPosition.getX());
        int deltaY = spawnPosition.getY() - anchorPosition.getY();
        int deltaZ = Math.abs(spawnPosition.getZ() - anchorPosition.getZ());
        // Ground-level neighbors are valid for the low radio. Retain +3 for older saved
        // points produced by the previous scan, which started one block above the anchor.
        if (deltaX > 2 || deltaZ > 2 || deltaY < 0 || deltaY > 3) {
            throw new IllegalArgumentException("Rally spawn must stay within the safety scan");
        }
        if (!Float.isFinite(yaw) || yaw < 0.0F || yaw >= 360.0F) {
            throw new IllegalArgumentException("Rally yaw must be in [0, 360)");
        }
    }
}
