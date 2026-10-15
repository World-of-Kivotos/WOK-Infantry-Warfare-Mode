package com.wok.infantry.integration.journeymap;

import com.mojang.blaze3d.platform.NativeImage;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.map.TacticalMapTerrainProvider;
import com.wok.infantry.client.map.TacticalMapTerrainRegistry;
import com.wok.infantry.client.map.TacticalMapTerrainRequest;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.MappingEvent;
import journeymap.api.v2.common.Context;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * JourneyMap discovers this class from bytecode via {@link JourneyMapPlugin}.
 *
 * <p>WOK declares JourneyMap 6 as a required client dependency but keeps the API compile-only,
 * so no JourneyMap classes are copied into the WOK production JAR.</p>
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public final class WokJourneyMapPlugin implements IClientPlugin, TacticalMapTerrainProvider {
    private volatile IClientAPI api;
    private volatile boolean mapping;

    @Override
    public void initialize(IClientAPI clientApi) {
        api = clientApi;
        ClientEventRegistry.MAPPING_EVENT.subscribe(WokInfantryMod.MOD_ID, this::onMappingEvent);
        TacticalMapTerrainRegistry.register(this);
        JourneyMapUiPolicy.install(clientApi);
        WokInfantryMod.LOGGER.info("JourneyMap tactical terrain integration initialized");
    }

    @Override
    public String getModId() {
        return WokInfantryMod.MOD_ID;
    }

    @Override
    public String id() {
        return "journeymap";
    }

    @Override
    public boolean isReady() {
        return api != null && mapping;
    }

    @Override
    public void requestTile(TacticalMapTerrainRequest request,
                            Consumer<NativeImage> callback) {
        IClientAPI currentApi = api;
        if (currentApi == null || !mapping) {
            callback.accept(null);
            return;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                request.dimension());
        // WOK keeps the rectangle end-exclusive, while JourneyMap's public contract uses the
        // south-east chunk itself as an inclusive endpoint.
        currentApi.requestMapTile(WokInfantryMod.MOD_ID, dimension, mapType(request.style()),
                new ChunkPos(request.startChunkX(), request.startChunkZ()),
                new ChunkPos(request.endChunkX() - 1, request.endChunkZ() - 1),
                null, request.providerZoom(), request.showProviderGrid(), callback);
    }

    private void onMappingEvent(MappingEvent event) {
        mapping = event.getStage() == MappingEvent.Stage.MAPPING_STARTED;
        JourneyMapUiPolicy.enforceMinimapHidden();
    }

    private static Context.MapType mapType(TacticalMapTerrainRequest.Style style) {
        return switch (style) {
            case DAY -> Context.MapType.Day;
            case TOPOGRAPHY -> Context.MapType.Topo;
            case BIOME -> Context.MapType.Biome;
        };
    }
}
