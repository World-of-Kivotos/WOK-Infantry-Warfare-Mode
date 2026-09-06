package com.wok.commandersupport;

import com.mojang.logging.LogUtils;
import com.wok.commandersupport.airstrike.MillenniumJdamProvider;
import com.wok.commandersupport.airstrike.F16PavewayProvider;
import com.wok.commandersupport.recon.ReconSatelliteProvider;
import com.wok.commandersupport.registry.CommanderSupportSounds;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import com.wok.infantry.support.adapter.SupportProviders;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WokCommanderSupportMod.MOD_ID)
public final class WokCommanderSupportMod {
    public static final String MOD_ID = "wok_commander_support";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation RECON_SATELLITE_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "recon_satellite");
    public static final ResourceLocation MILLENNIUM_JDAM_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID,
                    "millennium_f15ex_jdam_1000lb");
    public static final ResourceLocation F16C_PAVEWAY_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID,
                    "f16c_gbu12_paveway_500lb");
    public static final long RECON_COOLDOWN_TICKS = 10L * 60L * 20L;
    public static final int RECON_SCAN_STEPS = 6;
    public static final int RECON_SCAN_INTERVAL_TICKS = 5 * 20;
    public static final double RECON_RADIUS = 150.0D;
    public static final long MILLENNIUM_JDAM_COOLDOWN_TICKS = 15L * 60L * 20L;
    public static final long MILLENNIUM_JDAM_INBOUND_TICKS = 10L * 20L;
    public static final int MILLENNIUM_JDAM_FLIGHT_TICKS = 20;
    public static final int MILLENNIUM_JDAM_DELAY_FUSE_TICKS = 2 * 20;
    public static final int MILLENNIUM_JDAM_STEP_INTERVAL_TICKS = 20;
    public static final int MILLENNIUM_JDAM_STEP_COUNT = 4;
    public static final double MILLENNIUM_JDAM_RADIUS = 32.0D;
    public static final long F16C_PAVEWAY_COOLDOWN_TICKS = 15L * 60L * 20L;
    public static final long F16C_PAVEWAY_INBOUND_TICKS = 10L * 20L;
    public static final int F16C_PAVEWAY_FLIGHT_TICKS = 3 * 20;
    public static final int F16C_PAVEWAY_STEP_INTERVAL_TICKS = 1;
    public static final int F16C_PAVEWAY_STEP_COUNT =
            F16C_PAVEWAY_FLIGHT_TICKS + 1;
    public static final double F16C_PAVEWAY_GUIDANCE_RADIUS = 64.0D;

    public WokCommanderSupportMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CommanderSupportSounds.SOUND_EVENTS.register(modBus);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SupportProviders.register(reconSatelliteDefinition(),
                    new ReconSatelliteProvider());
            SupportProviders.register(millenniumJdamDefinition(),
                    new MillenniumJdamProvider());
            SupportProviders.register(f16cPavewayDefinition(),
                    new F16PavewayProvider());
        });
    }

    public static SupportDefinition reconSatelliteDefinition() {
        return new SupportDefinition(RECON_SATELLITE_ID,
                "support.wok_commander_support.recon_satellite",
                "侦察卫星", "卫星侦察", SupportTargetMode.POINT,
                RECON_COOLDOWN_TICKS, 0L, RECON_SCAN_STEPS,
                RECON_SCAN_INTERVAL_TICKS, RECON_RADIUS);
    }

    public static SupportDefinition millenniumJdamDefinition() {
        return new SupportDefinition(MILLENNIUM_JDAM_ID,
                "support.wok_commander_support.millennium_f15ex_jdam_1000lb",
                "千禧年 F-15EX 杰达姆 1000磅空袭", "F-15EX JDAM空袭",
                SupportTargetMode.POINT, MILLENNIUM_JDAM_COOLDOWN_TICKS,
                MILLENNIUM_JDAM_INBOUND_TICKS, MILLENNIUM_JDAM_STEP_COUNT,
                MILLENNIUM_JDAM_STEP_INTERVAL_TICKS,
                MILLENNIUM_JDAM_RADIUS);
    }

    public static SupportDefinition f16cPavewayDefinition() {
        return new SupportDefinition(F16C_PAVEWAY_ID,
                "support.wok_commander_support.f16c_gbu12_paveway_500lb",
                "F-16C GBU-12 宝石路 II 500磅精准空袭", "F-16C 宝石路空袭",
                SupportTargetMode.POINT, F16C_PAVEWAY_COOLDOWN_TICKS,
                F16C_PAVEWAY_INBOUND_TICKS, F16C_PAVEWAY_STEP_COUNT,
                F16C_PAVEWAY_STEP_INTERVAL_TICKS,
                F16C_PAVEWAY_GUIDANCE_RADIUS);
    }
}
