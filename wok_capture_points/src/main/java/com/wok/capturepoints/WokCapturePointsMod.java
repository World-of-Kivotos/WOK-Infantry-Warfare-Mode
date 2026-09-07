package com.wok.capturepoints;

import com.mojang.logging.LogUtils;
import com.wok.capturepoints.config.CaptureConfig;
import com.wok.capturepoints.event.CaptureEvents;
import com.wok.capturepoints.network.CaptureNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WokCapturePointsMod.MOD_ID)
public final class WokCapturePointsMod {
    public static final String MOD_ID = "wok_capture_points";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WokCapturePointsMod(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, CaptureConfig.SPEC);
        CaptureNetwork.register();
        MinecraftForge.EVENT_BUS.register(CaptureEvents.class);
    }
}
