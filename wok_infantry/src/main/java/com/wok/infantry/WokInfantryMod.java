package com.wok.infantry;

import com.mojang.logging.LogUtils;
import com.wok.infantry.client.ClientBootstrap;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.server.LoadoutCommands;
import com.wok.infantry.server.LoadoutService;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WokInfantryMod.MOD_ID)
public final class WokInfantryMod {
    public static final String MOD_ID = "wok_infantry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WokInfantryMod(FMLJavaModLoadingContext context) {
        LoadoutNetwork.init();
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientBootstrap.register(context.getModEventBus()));
    }

    private void onServerStarted(ServerStartedEvent event) {
        LoadoutService.start(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        LoadoutService.stop(event.getServer());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        LoadoutCommands.register(event.getDispatcher());
    }
}
