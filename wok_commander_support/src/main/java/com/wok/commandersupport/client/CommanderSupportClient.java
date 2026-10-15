package com.wok.commandersupport.client;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.registry.CommanderSupportEntities;
import com.wok.infantry.client.map.TacticalSupportMapPresentation;
import com.wok.infantry.client.map.TacticalSupportMapPresentationRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only wiring: map semantics (without changing the support wire protocol) and the drone's
 * renderer and model layer.
 */
@Mod.EventBusSubscriber(modid = WokCommanderSupportMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CommanderSupportClient {
    private CommanderSupportClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CommanderSupportClient::registerMapPresentations);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ReconDroneModel.LAYER_LOCATION,
                ReconDroneModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CommanderSupportEntities.RECON_DRONE.get(),
                ReconDroneRenderer::new);
    }

    static void registerMapPresentations() {
        TacticalSupportMapPresentationRegistry.register(
                WokCommanderSupportMod.RECON_SATELLITE_ID,
                TacticalSupportMapPresentation.INTELLIGENCE);
        TacticalSupportMapPresentationRegistry.register(
                WokCommanderSupportMod.MILLENNIUM_JDAM_ID,
                TacticalSupportMapPresentation.OFFENSIVE);
        TacticalSupportMapPresentationRegistry.register(
                WokCommanderSupportMod.F16C_PAVEWAY_ID,
                TacticalSupportMapPresentation.OFFENSIVE);
        // The drone only gathers intel; the barrages are fire missions on the target area.
        TacticalSupportMapPresentationRegistry.register(
                WokCommanderSupportMod.RECON_DRONE_ID,
                TacticalSupportMapPresentation.INTELLIGENCE);
        TacticalSupportMapPresentationRegistry.register(
                WokCommanderSupportMod.HOWITZER_3ROUND_ID,
                TacticalSupportMapPresentation.OFFENSIVE);
        TacticalSupportMapPresentationRegistry.register(
                WokCommanderSupportMod.HOWITZER_105_RAPID_ID,
                TacticalSupportMapPresentation.OFFENSIVE);
        TacticalSupportMapPresentationRegistry.register(
                WokCommanderSupportMod.HOWITZER_105_5ROUND_ID,
                TacticalSupportMapPresentation.OFFENSIVE);
        // The definition radius is the danger area (permit + blast); the map also shows the
        // smaller zone in which an allied designator must hold the laser spot.
        TacticalSupportMapPresentationRegistry.registerGuidanceRadius(
                WokCommanderSupportMod.F16C_PAVEWAY_ID,
                WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS);
    }
}
