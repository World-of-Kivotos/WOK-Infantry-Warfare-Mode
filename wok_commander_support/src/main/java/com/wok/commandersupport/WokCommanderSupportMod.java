package com.wok.commandersupport;

import com.mojang.logging.LogUtils;
import com.wok.commandersupport.airstrike.MillenniumJdamProvider;
import com.wok.commandersupport.airstrike.F16PavewayProvider;
import com.wok.commandersupport.airstrike.OrphanedShellCleanup;
import com.wok.commandersupport.artillery.ArtilleryBarrageProvider;
import com.wok.commandersupport.artillery.ArtilleryProfile;
import com.wok.commandersupport.drone.ReconDroneProvider;
import com.wok.commandersupport.recon.ReconSatelliteProvider;
import com.wok.commandersupport.registry.CommanderSupportEntities;
import com.wok.commandersupport.registry.CommanderSupportSounds;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import com.wok.infantry.support.adapter.SupportProviders;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.List;

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
    public static final ResourceLocation RECON_DRONE_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "recon_drone");
    public static final ResourceLocation HOWITZER_3ROUND_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "howitzer_3round_barrage");
    public static final ResourceLocation HOWITZER_105_RAPID_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID,
                    "howitzer_105mm_rapid_3round_barrage");
    public static final ResourceLocation HOWITZER_105_5ROUND_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID,
                    "howitzer_105mm_5round_barrage");
    // Cooldowns follow the faction table of 《步战模式公示表》. Each skill has exactly one global
    // value and formations never override it: the satellite, the F-15EX JDAM and the F-16C
    // Paveway recharge in 10 minutes; the drone (ReconDroneProvider) and the howitzer barrages
    // (ArtilleryProfile) keep their own table values next to their definitions.
    public static final long RECON_COOLDOWN_TICKS = 10L * 60L * 20L;
    public static final int RECON_SCAN_STEPS = 6;
    public static final int RECON_SCAN_INTERVAL_TICKS = 5 * 20;
    public static final double RECON_RADIUS = 150.0D;
    public static final long MILLENNIUM_JDAM_COOLDOWN_TICKS = 10L * 60L * 20L;
    public static final long MILLENNIUM_JDAM_INBOUND_TICKS = 10L * 20L;
    public static final int MILLENNIUM_JDAM_FLIGHT_TICKS = 20;
    public static final int MILLENNIUM_JDAM_DELAY_FUSE_TICKS = 2 * 20;
    public static final int MILLENNIUM_JDAM_STEP_INTERVAL_TICKS = 20;
    public static final int MILLENNIUM_JDAM_STEP_COUNT = 4;
    public static final double MILLENNIUM_JDAM_RADIUS = 32.0D;
    public static final long F16C_PAVEWAY_COOLDOWN_TICKS = 10L * 60L * 20L;
    public static final long F16C_PAVEWAY_INBOUND_TICKS = 10L * 20L;
    public static final int F16C_PAVEWAY_FLIGHT_TICKS = 3 * 20;
    public static final int F16C_PAVEWAY_STEP_INTERVAL_TICKS = 1;
    public static final int F16C_PAVEWAY_STEP_COUNT =
            F16C_PAVEWAY_FLIGHT_TICKS + 1;
    /** Designation permit: an allied laser spot must stay within this radius of the call point. */
    public static final double F16C_PAVEWAY_GUIDANCE_RADIUS = 64.0D;
    /**
     * Area that can be hit: a spot on the permit edge plus the 500 lb blast radius. Used as the
     * support definition radius so the map and the loaded-footprint check cover every impact.
     */
    public static final double F16C_PAVEWAY_DANGER_RADIUS =
            F16C_PAVEWAY_GUIDANCE_RADIUS + F16PavewayProvider.EXPLOSION_RADIUS;

    public WokCommanderSupportMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CommanderSupportSounds.SOUND_EVENTS.register(modBus);
        CommanderSupportEntities.ENTITY_TYPES.register(modBus);
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(OrphanedShellCleanup::onEntityJoinLevel);
        // ArtilleryReportScheduler（炮击的延迟炮声）用 @Mod.EventBusSubscriber 自己挂在 Forge
        // 事件总线上（服务端 tick、开服前清空、停服后清空）。这里不要再 addListener，
        // 否则同一批炮声会在一个 tick 内被推进两次。
        // ReconDroneWatchdog（无人机到期与停摆兜底）同样用 @Mod.EventBusSubscriber 自注册，
        // 这里也不要再 addListener。
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SupportProviders.register(reconSatelliteDefinition(),
                    new ReconSatelliteProvider());
            SupportProviders.register(millenniumJdamDefinition(),
                    new MillenniumJdamProvider());
            SupportProviders.register(f16cPavewayDefinition(),
                    new F16PavewayProvider());
            SupportProviders.register(reconDroneDefinition(),
                    new ReconDroneProvider());
            for (ArtilleryProfile profile : artilleryProfiles()) {
                SupportProviders.register(profile.definition(),
                        new ArtilleryBarrageProvider(profile));
            }
        });
    }

    /**
     * The satellite only reads entities that are already loaded and never touches blocks or
     * heights, so it does not require its footprint chunks to be loaded.
     */
    public static SupportDefinition reconSatelliteDefinition() {
        return new SupportDefinition(RECON_SATELLITE_ID,
                "support.wok_commander_support.recon_satellite",
                "侦察卫星", "卫星侦察", SupportTargetMode.POINT,
                RECON_COOLDOWN_TICKS, 0L, RECON_SCAN_STEPS,
                RECON_SCAN_INTERVAL_TICKS, RECON_RADIUS, false);
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
                F16C_PAVEWAY_DANGER_RADIUS);
    }

    /** The drone's timing and footprint live next to its provider. */
    public static SupportDefinition reconDroneDefinition() {
        return ReconDroneProvider.definition();
    }

    /** The three howitzer barrages share one provider, each with its own profile. */
    public static List<ArtilleryProfile> artilleryProfiles() {
        return List.of(ArtilleryProfile.HOWITZER_3ROUND, ArtilleryProfile.RAPID_105,
                ArtilleryProfile.FIVE_ROUND_105);
    }

    /** Every definition this module registers, in registration order. */
    public static List<SupportDefinition> definitions() {
        return List.of(reconSatelliteDefinition(), millenniumJdamDefinition(),
                f16cPavewayDefinition(), reconDroneDefinition(),
                ArtilleryProfile.HOWITZER_3ROUND.definition(),
                ArtilleryProfile.RAPID_105.definition(),
                ArtilleryProfile.FIVE_ROUND_105.definition());
    }
}
