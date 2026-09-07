package com.wok.infantry;

import com.mojang.logging.LogUtils;
import com.wok.infantry.client.ClientBootstrap;
import com.wok.infantry.config.InfantryClientConfig;
import com.wok.infantry.config.InfantryServerConfig;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.formation.FormationNetwork;
import com.wok.infantry.network.stamina.StaminaNetwork;
import com.wok.infantry.integration.tacz.TaczAdsSpeedAdapter;
import com.wok.infantry.registry.InfantryBlocks;
import com.wok.infantry.registry.InfantryBlockEntities;
import com.wok.infantry.registry.InfantryCreativeTabs;
import com.wok.infantry.registry.InfantryItems;
import com.wok.infantry.server.LoadoutCommands;
import com.wok.infantry.server.LoadoutService;
import com.wok.infantry.server.FormationService;
import com.wok.infantry.stamina.StaminaEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;

@Mod(WokInfantryMod.MOD_ID)
public final class WokInfantryMod {
    public static final String MOD_ID = "wok_infantry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WokInfantryMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        context.registerConfig(ModConfig.Type.CLIENT, InfantryClientConfig.SPEC);
        context.registerConfig(ModConfig.Type.SERVER, InfantryServerConfig.SPEC);
        InfantryBlocks.register(modBus);
        InfantryBlockEntities.register(modBus);
        InfantryItems.register(modBus);
        InfantryCreativeTabs.register(modBus);
        LoadoutNetwork.init();
        BattleNetwork.init();
        FormationNetwork.init();
        StaminaNetwork.init();
        TaczAdsSpeedAdapter.register();
        MinecraftForge.EVENT_BUS.addListener(StaminaEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(StaminaEvents::onPlayerJump);
        MinecraftForge.EVENT_BUS.addListener(StaminaEvents::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(StaminaEvents::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(StaminaEvents::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(StaminaEvents::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientBootstrap.register(modBus));
    }

    private void onServerStarted(ServerStartedEvent event) {
        FormationService.start(event.getServer());
        LoadoutService.start(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        LoadoutService.stop(event.getServer());
        FormationService.stop(event.getServer());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        LoadoutCommands.register(event.getDispatcher());
    }
}
