package com.wok.infantryarmor;

import com.wok.infantryarmor.armor.PlateArmorDamageHandler;
import com.wok.infantryarmor.armor.PlateArmorEquipmentHandler;
import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.shield.PlasmaShieldHandler;
import com.wok.infantryarmor.shield.network.PlasmaShieldNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Standalone entry point for the WOK Infantry armor add-on. */
@Mod(WokInfantryArmorMod.MODID)
public final class WokInfantryArmorMod {

    public static final String MODID = "wok_infantry_armor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public WokInfantryArmorMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        ArmorerItems.register(modBus);
        ArmorerCreativeTab.register(modBus);
        ArmorerSounds.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,
                ArmorerConfig.SPEC, "wok-infantry-armor.toml");

        forgeBus.register(new PlateArmorDamageHandler());
        forgeBus.register(new PlateArmorEquipmentHandler());
        forgeBus.register(new PlasmaShieldHandler());
        modBus.addListener(this::commonSetup);

        if (ModList.get().isLoaded("tacz")) {
            com.wok.infantryarmor.armor.integration.PlateArmorTaczIntegrationBootstrap.assemble(forgeBus);
        }

        LOGGER.info("Registered 54 plate armors, {} helmets, 18 plasma shields and 3 compatibility shield aliases",
                HelmetVariant.values().length);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PlasmaShieldNetwork::register);
    }
}
