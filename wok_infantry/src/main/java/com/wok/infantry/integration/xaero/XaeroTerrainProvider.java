package com.wok.infantry.integration.xaero;

import com.mojang.blaze3d.platform.NativeImage;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.map.TacticalMapTerrainProvider;
import com.wok.infantry.client.map.TacticalMapTerrainRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Xaero 24.x local-terrain adapter.
 *
 * <p>Xaero Minimap does not expose a public rectangular tile API comparable to JourneyMap 6.
 * This adapter therefore reads its already-computed in-memory surface tiles through an isolated,
 * version-checked reflection boundary and fills any currently loaded gaps from vanilla client
 * chunks. It never bundles or rewrites Xaero classes, map data or configuration.</p>
 */
final class XaeroTerrainProvider implements TacticalMapTerrainProvider {
    private static final int BLOCKS_PER_CHUNK = 16;
    private static final int XAERO_COLOR_LEVEL = 0;
    private static final int UNKNOWN_COLOR = nativeColor(126, 140, 134);

    private volatile XaeroTileBridge tileBridge;
    private volatile boolean tileBridgeUnavailable;

    @Override
    public String id() {
        return "xaero";
    }

    @Override
    public boolean isReady() {
        Minecraft client = Minecraft.getInstance();
        return XaeroMinimapIntegration.isXaeroLoaded()
                && client.level != null && client.player != null;
    }

    @Override
    public void requestTile(TacticalMapTerrainRequest request,
                            Consumer<NativeImage> callback) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            NativeImage image = null;
            try {
                image = generateTile(client, request);
            } catch (RuntimeException exception) {
                WokInfantryMod.LOGGER.warn("Could not generate Xaero tactical terrain tile",
                        exception);
            }
            callback.accept(image);
        });
    }

    private NativeImage generateTile(Minecraft client, TacticalMapTerrainRequest request) {
        ClientLevel level = client.level;
        if (level == null || !level.dimension().location().equals(request.dimension())) {
            return null;
        }

        int sampleScale = 1 << Math.max(0, request.providerZoom() - 5);
        int blockWidth = (request.endChunkX() - request.startChunkX()) * BLOCKS_PER_CHUNK;
        int blockHeight = (request.endChunkZ() - request.startChunkZ()) * BLOCKS_PER_CHUNK;
        int width = Math.max(1, (blockWidth + sampleScale - 1) / sampleScale);
        int height = Math.max(1, (blockHeight + sampleScale - 1) / sampleScale);
        int startBlockX = request.startChunkX() * BLOCKS_PER_CHUNK;
        int startBlockZ = request.startChunkZ() * BLOCKS_PER_CHUNK;

        Map<Long, XaeroTileColors> xaeroTiles = xaeroTileSnapshot();
        Map<Long, LevelChunk> loadedChunks = new HashMap<>();
        Set<Long> missingChunks = new HashSet<>();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        NativeImage image = new NativeImage(width, height, true);
        boolean wroteTerrain = false;
        try {
            for (int pixelZ = 0; pixelZ < height; pixelZ++) {
                int blockZ = startBlockZ + pixelZ * sampleScale + sampleScale / 2;
                for (int pixelX = 0; pixelX < width; pixelX++) {
                    int blockX = startBlockX + pixelX * sampleScale + sampleScale / 2;
                    int color = xaeroColor(xaeroTiles, blockX, blockZ);
                    if (color == 0) {
                        color = vanillaColor(level, loadedChunks, missingChunks,
                                position, blockX, blockZ);
                    }
                    if (color == 0) {
                        color = UNKNOWN_COLOR;
                    } else {
                        wroteTerrain = true;
                    }
                    image.setPixelRGBA(pixelX, pixelZ, color);
                }
            }
            if (!wroteTerrain) {
                image.close();
                return null;
            }
            return image;
        } catch (RuntimeException exception) {
            image.close();
            throw exception;
        }
    }

    private Map<Long, XaeroTileColors> xaeroTileSnapshot() {
        if (tileBridgeUnavailable) {
            return Map.of();
        }
        try {
            XaeroTileBridge current = tileBridge;
            if (current == null) {
                current = XaeroTileBridge.load();
                tileBridge = current;
            }
            return current.snapshot();
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException
                 | IllegalAccessException | InvocationTargetException exception) {
            tileBridgeUnavailable = true;
            WokInfantryMod.LOGGER.warn(
                    "Xaero tile cache API differs from the tested 24.x layout; "
                            + "using loaded vanilla terrain fallback", exception);
            return Map.of();
        } catch (LinkageError error) {
            tileBridgeUnavailable = true;
            WokInfantryMod.LOGGER.warn(
                    "Xaero tile cache is unavailable; using loaded vanilla terrain fallback",
                    error);
            return Map.of();
        }
    }

    private static int xaeroColor(Map<Long, XaeroTileColors> tiles, int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, BLOCKS_PER_CHUNK);
        int chunkZ = Math.floorDiv(blockZ, BLOCKS_PER_CHUNK);
        XaeroTileColors tile = tiles.get(chunkKey(chunkX, chunkZ));
        if (tile == null) {
            return 0;
        }
        int localX = Math.floorMod(blockX, BLOCKS_PER_CHUNK);
        int localZ = Math.floorMod(blockZ, BLOCKS_PER_CHUNK);
        return nativeColor(tile.red()[localX][localZ] & 0xFF,
                tile.green()[localX][localZ] & 0xFF,
                tile.blue()[localX][localZ] & 0xFF);
    }

    private static int vanillaColor(ClientLevel level, Map<Long, LevelChunk> loadedChunks,
                                    Set<Long> missingChunks, BlockPos.MutableBlockPos position,
                                    int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, BLOCKS_PER_CHUNK);
        int chunkZ = Math.floorDiv(blockZ, BLOCKS_PER_CHUNK);
        long key = chunkKey(chunkX, chunkZ);
        if (missingChunks.contains(key)) {
            return 0;
        }
        LevelChunk chunk = loadedChunks.get(key);
        if (chunk == null) {
            chunk = level.getChunkSource().getChunk(
                    chunkX, chunkZ, ChunkStatus.FULL, false);
            if (chunk == null) {
                missingChunks.add(key);
                return 0;
            }
            loadedChunks.put(key, chunk);
        }

        int localX = Math.floorMod(blockX, BLOCKS_PER_CHUNK);
        int localZ = Math.floorMod(blockZ, BLOCKS_PER_CHUNK);
        int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) - 1;
        if (surfaceY < level.getMinBuildHeight()) {
            return 0;
        }
        position.set(blockX, surfaceY, blockZ);
        MapColor mapColor = chunk.getBlockState(position).getMapColor(level, position);
        if (mapColor == MapColor.NONE) {
            return 0;
        }
        int argb = mapColor.calculateRGBColor(MapColor.Brightness.NORMAL);
        return nativeColor((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF);
    }

    /** NativeImage's RGBA integer stores red in the least-significant byte. */
    private static int nativeColor(int red, int green, int blue) {
        return 0xFF000000 | (blue & 0xFF) << 16 | (green & 0xFF) << 8 | (red & 0xFF);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | (long) chunkZ << 32;
    }

    private record XaeroTileColors(byte[][] red, byte[][] green, byte[][] blue) {
    }

    private record XaeroTileBridge(Method currentSession, Method getProcessor,
                                   Method getWriter, Method getLoadedBlocks,
                                   Method getTile, Method tileX, Method tileZ,
                                   Method hasTerrain, Method isSuccess,
                                   Field red, Field green, Field blue) {
        private static XaeroTileBridge load() throws ClassNotFoundException,
                NoSuchMethodException, NoSuchFieldException {
            ClassLoader loader = XaeroTerrainProvider.class.getClassLoader();
            Class<?> session = Class.forName("xaero.common.XaeroMinimapSession", false, loader);
            Class<?> processor = Class.forName(
                    "xaero.common.minimap.MinimapProcessor", false, loader);
            Class<?> writer = Class.forName(
                    "xaero.common.minimap.write.MinimapWriter", false, loader);
            Class<?> mapChunk = Class.forName(
                    "xaero.common.minimap.region.MinimapChunk", false, loader);
            Class<?> tile = Class.forName(
                    "xaero.common.minimap.region.MinimapTile", false, loader);
            Field red = tile.getDeclaredField("red");
            Field green = tile.getDeclaredField("green");
            Field blue = tile.getDeclaredField("blue");
            red.setAccessible(true);
            green.setAccessible(true);
            blue.setAccessible(true);
            return new XaeroTileBridge(
                    session.getMethod("getCurrentSession"),
                    session.getMethod("getMinimapProcessor"),
                    processor.getMethod("getMinimapWriter"),
                    writer.getMethod("getLoadedBlocks"),
                    mapChunk.getMethod("getTile", int.class, int.class),
                    tile.getMethod("getX"), tile.getMethod("getZ"),
                    tile.getMethod("hasTerrain"), tile.getMethod("isSuccess"),
                    red, green, blue);
        }

        private Map<Long, XaeroTileColors> snapshot()
                throws InvocationTargetException, IllegalAccessException {
            Object session = currentSession.invoke(null);
            if (session == null) {
                return Map.of();
            }
            Object processor = getProcessor.invoke(session);
            Object writer = processor == null ? null : getWriter.invoke(processor);
            Object loaded = writer == null ? null : getLoadedBlocks.invoke(writer);
            if (!(loaded instanceof Object[] rows)) {
                return Map.of();
            }

            Map<Long, XaeroTileColors> result = new HashMap<>();
            for (Object rowValue : rows) {
                if (!(rowValue instanceof Object[] row)) {
                    continue;
                }
                for (Object mapChunk : row) {
                    if (mapChunk == null) {
                        continue;
                    }
                    for (int localX = 0; localX < 4; localX++) {
                        for (int localZ = 0; localZ < 4; localZ++) {
                            Object tile = getTile.invoke(mapChunk, localX, localZ);
                            if (tile == null || !Boolean.TRUE.equals(hasTerrain.invoke(tile))
                                    || !Boolean.TRUE.equals(isSuccess.invoke(tile))) {
                                continue;
                            }
                            int chunkX = (Integer) tileX.invoke(tile);
                            int chunkZ = (Integer) tileZ.invoke(tile);
                            byte[][][] reds = (byte[][][]) red.get(tile);
                            byte[][][] greens = (byte[][][]) green.get(tile);
                            byte[][][] blues = (byte[][][]) blue.get(tile);
                            result.put(chunkKey(chunkX, chunkZ), new XaeroTileColors(
                                    reds[XAERO_COLOR_LEVEL], greens[XAERO_COLOR_LEVEL],
                                    blues[XAERO_COLOR_LEVEL]));
                        }
                    }
                }
            }
            return Map.copyOf(result);
        }
    }
}
