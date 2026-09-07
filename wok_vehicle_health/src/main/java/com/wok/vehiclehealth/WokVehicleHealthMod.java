package com.wok.vehiclehealth;

import com.mojang.logging.LogUtils;
import com.wok.vehiclehealth.config.VehicleModuleConfig;
import com.wok.vehiclehealth.network.VehicleModuleNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WokVehicleHealthMod.MOD_ID)
public final class WokVehicleHealthMod {
    public static final String MOD_ID = "wok_vehicle_health";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WokVehicleHealthMod(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, VehicleModuleConfig.SPEC);
        VehicleModuleNetwork.init();
    }
}
