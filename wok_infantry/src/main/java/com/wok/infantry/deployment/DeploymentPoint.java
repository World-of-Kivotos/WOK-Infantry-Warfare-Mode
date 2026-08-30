package com.wok.infantry.deployment;

import com.wok.infantry.battle.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/** A server-configured deployment point. Clients select only {@link #id()}, never coordinates. */
public record DeploymentPoint(
        UUID id,
        Faction faction,
        ResourceLocation dimension,
        BlockPos position,
        float yaw,
        int supplyRadius,
        DeploymentPointKind kind
) {
    public static final int DEFAULT_SUPPLY_RADIUS = 8;

    public DeploymentPoint {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(kind, "kind");
        position = position.immutable();
        if (!Float.isFinite(yaw)) {
            throw new IllegalArgumentException("Deployment point yaw must be finite");
        }
        if (supplyRadius < 1 || supplyRadius > 64) {
            throw new IllegalArgumentException("Supply radius must be 1-64 blocks");
        }
    }

    /** Compatibility constructor for permanent main-base call sites. */
    public DeploymentPoint(UUID id, Faction faction, ResourceLocation dimension,
                           BlockPos position, float yaw, int supplyRadius) {
        this(id, faction, dimension, position, yaw, supplyRadius,
                DeploymentPointKind.MAIN_BASE);
    }
}
