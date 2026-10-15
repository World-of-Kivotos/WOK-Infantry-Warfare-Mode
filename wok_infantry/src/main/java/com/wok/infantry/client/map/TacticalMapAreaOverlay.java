package com.wok.infantry.client.map;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Client-only, provider-neutral area displayed on the WOK tactical map.
 * Optional addons own the data and lifecycle; the infantry core only renders it.
 */
public record TacticalMapAreaOverlay(
        String id,
        ResourceLocation dimension,
        double minX,
        double minZ,
        double maxX,
        double maxZ,
        Component label,
        Component detail,
        int color,
        double progress,
        boolean locked) {

    public TacticalMapAreaOverlay {
        id = Objects.requireNonNull(id, "id");
        dimension = Objects.requireNonNull(dimension, "dimension");
        label = Objects.requireNonNull(label, "label");
        detail = Objects.requireNonNull(detail, "detail");
        if (!Double.isFinite(minX) || !Double.isFinite(minZ)
                || !Double.isFinite(maxX) || !Double.isFinite(maxZ)) {
            throw new IllegalArgumentException("Area coordinates must be finite");
        }
        if (maxX < minX || maxZ < minZ) {
            throw new IllegalArgumentException("Area maximum must not precede minimum");
        }
        progress = Math.max(0.0D, Math.min(1.0D,
                Double.isFinite(progress) ? progress : 0.0D));
    }
}
