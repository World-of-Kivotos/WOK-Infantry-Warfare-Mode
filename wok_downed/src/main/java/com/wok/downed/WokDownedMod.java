package com.wok.downed;

import com.mojang.logging.LogUtils;
import com.wok.downed.config.DownedConfig;
import com.wok.downed.registry.DownedEffects;
import com.wok.downed.registry.DownedSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WokDownedMod.MOD_ID)
public final class WokDownedMod {
    public static final String MOD_ID = "wok_downed";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WokDownedMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        DownedEffects.register(modBus);
        DownedSounds.register(modBus);
        context.registerConfig(ModConfig.Type.SERVER, DownedConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(com.wok.downed.event.DownedEvents.class);
        // Optional TaCZ linkage: registers a listener only when TaCZ is installed, and reaches its
        // event reflectively so this module keeps working without it.
        com.wok.downed.compat.TaczSidearmCompat.install();
    }
}
