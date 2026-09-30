package com.wok.infantry.deployment;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Inclusive block cuboid selected by the administrator's base tool. */
public record BaseSupplyArea(ResourceLocation dimension, BlockPos min, BlockPos max) {
    public BaseSupplyArea {
        if (dimension == null || min == null || max == null) throw new IllegalArgumentException("Missing area");
        BlockPos first = min;
        min = new BlockPos(Math.min(first.getX(), max.getX()), Math.min(first.getY(), max.getY()),
                Math.min(first.getZ(), max.getZ()));
        max = new BlockPos(Math.max(first.getX(), max.getX()), Math.max(first.getY(), max.getY()),
                Math.max(first.getZ(), max.getZ()));
        if ((long) max.getX() - min.getX() > 1024 || (long) max.getY() - min.getY() > 1024
                || (long) max.getZ() - min.getZ() > 1024) throw new IllegalArgumentException("Area too large");
    }

    public boolean contains(ResourceLocation world, BlockPos position) {
        return dimension.equals(world) && position.getX() >= min.getX() && position.getX() <= max.getX()
                && position.getY() >= min.getY() && position.getY() <= max.getY()
                && position.getZ() >= min.getZ() && position.getZ() <= max.getZ();
    }

    public boolean overlaps(BaseSupplyArea other) {
        return dimension.equals(other.dimension) && min.getX() <= other.max.getX() && max.getX() >= other.min.getX()
                && min.getY() <= other.max.getY() && max.getY() >= other.min.getY()
                && min.getZ() <= other.max.getZ() && max.getZ() >= other.min.getZ();
    }
}
