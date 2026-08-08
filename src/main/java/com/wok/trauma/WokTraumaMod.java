package com.wok.trauma;

import com.mojang.logging.LogUtils;
import com.wok.trauma.config.TraumaConfig;
import com.wok.trauma.event.TraumaEvents;
import com.wok.trauma.item.TreatmentSelectionStore;
import com.wok.trauma.network.TraumaNetwork;
import com.wok.trauma.registry.ModCreativeTabs;
import com.wok.trauma.registry.ModEffects;
import com.wok.trauma.registry.ModItems;
import com.wok.trauma.registry.ModParticles;
import com.wok.trauma.registry.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WokTraumaMod.MOD_ID)
public final class WokTraumaMod {
    public static final String MOD_ID = "wok_trauma";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WokTraumaMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModEffects.register(modBus);
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        ModParticles.register(modBus);
        ModSounds.register(modBus);
        TraumaNetwork.init();

        MinecraftForge.EVENT_BUS.addListener(TraumaEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(TraumaEvents::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(TraumaEvents::onMobEffectApplicable);
        MinecraftForge.EVENT_BUS.addListener(TreatmentSelectionStore::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(TreatmentSelectionStore::onServerStopping);
        context.registerConfig(ModConfig.Type.COMMON, TraumaConfig.SPEC);

        if (ModList.get().isLoaded("tacz")) {
            loadTaczCompat();
        } else {
            LOGGER.info("TaCZ is not installed; gunshot trauma integration is disabled.");
        }
    }

    private static void loadTaczCompat() {
        try {
            Class<?> compat = Class.forName("com.wok.trauma.compat.tacz.TaczCompat");
            compat.getMethod("register").invoke(null);
            LOGGER.info("TaCZ trauma integration enabled.");
        } catch (ReflectiveOperationException exception) {
            // TaCZ is an optional integration. A changed or incompatible TaCZ
            // API must not prevent the standalone trauma mechanics and medical
            // items from loading.
            LOGGER.error(
                    "TaCZ is installed, but trauma integration could not start; "
                            + "WOK Trauma will continue without gun integration.",
                    exception);
        }
    }
}
