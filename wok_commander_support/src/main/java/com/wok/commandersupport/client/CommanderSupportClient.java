package com.wok.commandersupport.client;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.client.map.TacticalSupportMapPresentation;
import com.wok.infantry.client.map.TacticalSupportMapPresentationRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registers client-only map semantics without changing the support wire protocol. */
@Mod.EventBusSubscriber(modid = WokCommanderSupportMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CommanderSupportClient {
    private CommanderSupportClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CommanderSupportClient::registerMapPresentations);
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
    }
}
