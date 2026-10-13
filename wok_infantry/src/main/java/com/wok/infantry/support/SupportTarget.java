package com.wok.infantry.support;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** X/Z-only client intent. The authoritative server resolves terrain height. */
public record SupportTarget(
        ResourceLocation dimension,
        double startX,
        double startZ,
        double endX,
        double endZ
) {
    public static final double MIN_DIRECTION_LENGTH = 16.0D;
    public static final double MAX_DIRECTION_LENGTH = 512.0D;

    public SupportTarget {
        Objects.requireNonNull(dimension, "dimension");
        if (!Double.isFinite(startX) || !Double.isFinite(startZ)
                || !Double.isFinite(endX) || !Double.isFinite(endZ)) {
            throw new IllegalArgumentException("Support target coordinates must be finite");
        }
    }

    public static SupportTarget point(ResourceLocation dimension, double x, double z) {
        return new SupportTarget(dimension, x, z, x, z);
    }

    public static SupportTarget directional(ResourceLocation dimension,
                                            double startX, double startZ,
                                            double endX, double endZ) {
        return new SupportTarget(dimension, startX, startZ, endX, endZ);
    }

    public double deltaX() {
        return endX - startX;
    }

    public double deltaZ() {
        return endZ - startZ;
    }

    public double lengthSquared() {
        return deltaX() * deltaX() + deltaZ() * deltaZ();
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    /** Shared UI, wire and server contract for directional support geometry. */
    public static boolean isValidDirection(double startX, double startZ,
                                           double endX, double endZ) {
        if (!Double.isFinite(startX) || !Double.isFinite(startZ)
                || !Double.isFinite(endX) || !Double.isFinite(endZ)) {
            return false;
        }
        double deltaX = endX - startX;
        double deltaZ = endZ - startZ;
        double lengthSquared = deltaX * deltaX + deltaZ * deltaZ;
        return Double.isFinite(lengthSquared)
                && lengthSquared >= MIN_DIRECTION_LENGTH * MIN_DIRECTION_LENGTH
                && lengthSquared <= MAX_DIRECTION_LENGTH * MAX_DIRECTION_LENGTH;
    }
}
