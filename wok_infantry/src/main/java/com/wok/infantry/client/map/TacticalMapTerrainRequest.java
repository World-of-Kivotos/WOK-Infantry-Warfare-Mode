package com.wok.infantry.client.map;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Loader- and map-mod-neutral terrain request.
 *
 * <p>Chunk bounds use a north-west inclusive, south-east exclusive rectangle. The provider zoom
 * is an abstract detail of the selected backend; the JourneyMap adapter maps the 0-8 value directly
 * to its public API.</p>
 */
public record TacticalMapTerrainRequest(
        ResourceLocation dimension,
        Style style,
        int startChunkX,
        int startChunkZ,
        int endChunkX,
        int endChunkZ,
        int providerZoom,
        boolean showProviderGrid
) {
    public static final int MAX_CHUNK_SPAN = 32;

    public TacticalMapTerrainRequest {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(style, "style");
        if (endChunkX <= startChunkX || endChunkZ <= startChunkZ) {
            throw new IllegalArgumentException("Terrain request bounds must have positive area");
        }
        if ((long) endChunkX - startChunkX > MAX_CHUNK_SPAN
                || (long) endChunkZ - startChunkZ > MAX_CHUNK_SPAN) {
            throw new IllegalArgumentException("Terrain request may cover at most 32x32 chunks");
        }
        if (providerZoom < 0 || providerZoom > 8) {
            throw new IllegalArgumentException("Terrain provider zoom must be between 0 and 8");
        }
    }

    public double minBlockX() {
        return startChunkX * 16.0D;
    }

    public double minBlockZ() {
        return startChunkZ * 16.0D;
    }

    public double maxBlockX() {
        return endChunkX * 16.0D;
    }

    public double maxBlockZ() {
        return endChunkZ * 16.0D;
    }

    public enum Style {
        DAY,
        TOPOGRAPHY,
        BIOME
    }
}
