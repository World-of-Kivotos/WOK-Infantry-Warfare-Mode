package com.wok.infantry.client.map;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TacticalMapTerrainRequestTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test
    void exactMaximumTileConvertsChunkBoundsToBlocks() {
        TacticalMapTerrainRequest request = new TacticalMapTerrainRequest(
                OVERWORLD, TacticalMapTerrainRequest.Style.TOPOGRAPHY,
                -16, -8, 16, 24, 8, false);

        assertEquals(-256.0D, request.minBlockX());
        assertEquals(-128.0D, request.minBlockZ());
        assertEquals(256.0D, request.maxBlockX());
        assertEquals(384.0D, request.maxBlockZ());
    }

    @Test
    void rejectsOversizedOrEmptyBounds() {
        assertThrows(IllegalArgumentException.class, () -> new TacticalMapTerrainRequest(
                OVERWORLD, TacticalMapTerrainRequest.Style.DAY,
                0, 0, 33, 1, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new TacticalMapTerrainRequest(
                OVERWORLD, TacticalMapTerrainRequest.Style.DAY,
                5, 5, 5, 6, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new TacticalMapTerrainRequest(
                OVERWORLD, TacticalMapTerrainRequest.Style.DAY,
                Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 1, 0, false));
    }

    @Test
    void providerZoomIsInclusiveZeroThroughEight() {
        new TacticalMapTerrainRequest(OVERWORLD, TacticalMapTerrainRequest.Style.BIOME,
                0, 0, 1, 1, 0, true);
        new TacticalMapTerrainRequest(OVERWORLD, TacticalMapTerrainRequest.Style.BIOME,
                0, 0, 1, 1, 8, true);

        assertThrows(IllegalArgumentException.class, () -> new TacticalMapTerrainRequest(
                OVERWORLD, TacticalMapTerrainRequest.Style.BIOME,
                0, 0, 1, 1, -1, true));
        assertThrows(IllegalArgumentException.class, () -> new TacticalMapTerrainRequest(
                OVERWORLD, TacticalMapTerrainRequest.Style.BIOME,
                0, 0, 1, 1, 9, true));
    }
}
