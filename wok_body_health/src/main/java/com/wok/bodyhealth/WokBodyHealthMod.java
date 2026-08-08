package com.wok.bodyhealth;

import com.mojang.logging.LogUtils;
import com.wok.bodyhealth.config.BodyHealthConfig;
import com.wok.bodyhealth.event.BodyHealthEvents;
import com.wok.bodyhealth.network.BodyHealthNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WokBodyHealthMod.MOD_ID)
public final class WokBodyHealthMod {
    public static final String MOD_ID = "wok_body_health";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WokBodyHealthMod(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, BodyHealthConfig.SPEC);
        BodyHealthNetwork.init();

        MinecraftForge.EVENT_BUS.addListener(
                EventPriority.LOWEST, BodyHealthEvents::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(BodyHealthEvents::onLivingHeal);
        MinecraftForge.EVENT_BUS.addListener(BodyHealthEvents::onLivingUseTotem);
        MinecraftForge.EVENT_BUS.addListener(BodyHealthEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(BodyHealthEvents::onLivingJump);
        MinecraftForge.EVENT_BUS.addListener(BodyHealthEvents::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(BodyHealthEvents::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(BodyHealthEvents::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(BodyHealthEvents::onPlayerChangedDimension);

        if (ModList.get().isLoaded("tacz")) {
            loadTaczCompat();
        } else {
            LOGGER.info("TaCZ is not installed; precise gunshot hit locations are disabled.");
        }
    }

    private static void loadTaczCompat() {
        try {
            Class<?> compat = Class.forName("com.wok.bodyhealth.compat.tacz.TaczCompat");
            compat.getMethod("register").invoke(null);
            LOGGER.info("TaCZ body-part hit detection enabled.");
        } catch (ReflectiveOperationException exception) {
            // Precise TaCZ hit coordinates are optional. If its API changes,
            // generic projectile/body-region detection remains available and
            // the core seven-part health system must still start.
            LOGGER.error(
                    "TaCZ is installed, but precise body-part hit detection could not start; "
                            + "WOK Body Health will continue with generic hit detection.",
                    exception);
        }
    }
}
