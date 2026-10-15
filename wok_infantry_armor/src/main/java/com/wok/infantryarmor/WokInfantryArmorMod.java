package com.wok.infantryarmor;

import com.wok.infantryarmor.armor.PlateArmorDamageHandler;
import com.wok.infantryarmor.armor.PlateArmorEquipmentHandler;
import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.settings.ArmorItemSettings;
import com.wok.infantryarmor.command.ArmorAdminCommands;
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
        // 先注册主配置，再注册逐件配置；逐件配置的默认值依赖主配置（客户端同步顺序不保证，读取方另有代数校验）。
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,
                ArmorerConfig.SPEC, "wok-infantry-armor.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,
                ArmorItemConfig.SPEC, ArmorItemConfig.FILE_NAME);
        ArmorItemSettings.registerConfigListeners(modBus);

        forgeBus.register(new PlateArmorDamageHandler());
        forgeBus.register(new PlateArmorEquipmentHandler());
        forgeBus.register(new PlasmaShieldHandler());
        forgeBus.addListener(ArmorAdminCommands::register);
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
