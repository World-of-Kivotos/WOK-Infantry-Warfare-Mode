package com.wok.infantry.uitest.fixtures;

import com.wok.infantry.client.map.TacticalSupportMapPresentation;
import com.wok.infantry.client.map.TacticalSupportMapPresentationRegistry;
import com.wok.infantry.support.SupportMissionView;
import com.wok.infantry.support.SupportOptionView;
import com.wok.infantry.support.SupportTargetMode;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/**
 * Commander-support catalog of the live tactical-map captures: the Millennium satellite, F-15EX
 * JDAM and F-16C Paveway calls of {@code wok_commander_support}, which the isolated core client does
 * not load. Display names are what the add-on's server sends.
 */
public final class SupportFixtures {
    public static final ResourceLocation RECON_ID = new ResourceLocation(
            "wok_commander_support", "recon_satellite");
    public static final ResourceLocation JDAM_ID = new ResourceLocation(
            "wok_commander_support", "millennium_f15ex_jdam_1000lb");
    public static final ResourceLocation PAVEWAY_ID = new ResourceLocation(
            "wok_commander_support", "f16c_gbu12_paveway_500lb");
    /** Paveway danger area and its designation (照射许可) ring. */
    public static final double PAVEWAY_RADIUS = 80.0D;
    public static final double PAVEWAY_GUIDANCE_RADIUS = 64.0D;
    public static final UUID JDAM_MISSION = UUID.fromString("a9185f44-0dbd-4b0f-8ba7-d62f4397a14c");
    public static final UUID PAVEWAY_MISSION =
            UUID.fromString("5b0c6f1e-7d42-4f0e-9a51-3c8e2f6d7b19");

    private SupportFixtures() {
    }

    /** Registers the map presentations the add-on would register (beta.5 guidance ring included). */
    public static void registerPresentations() {
        TacticalSupportMapPresentationRegistry.register(RECON_ID,
                TacticalSupportMapPresentation.INTELLIGENCE);
        TacticalSupportMapPresentationRegistry.register(JDAM_ID,
                TacticalSupportMapPresentation.OFFENSIVE);
        TacticalSupportMapPresentationRegistry.register(PAVEWAY_ID,
                TacticalSupportMapPresentation.OFFENSIVE);
        TacticalSupportMapPresentationRegistry.registerGuidanceRadius(PAVEWAY_ID,
                PAVEWAY_GUIDANCE_RADIUS);
    }

    /** The three ready options. */
    public static List<SupportOptionView> options() {
        return List.of(
                new SupportOptionView(RECON_ID,
                        "support.wok_commander_support.recon_satellite",
                        "侦察卫星", "侦察卫星", SupportTargetMode.POINT, 150.0D,
                        true, "", 0L, false),
                new SupportOptionView(JDAM_ID,
                        "support.wok_commander_support.millennium_f15ex_jdam_1000lb",
                        "千禧年 F-15EX 杰达姆 1000磅空袭", "F-15EX JDAM空袭",
                        SupportTargetMode.POINT, 32.0D, true, "", 0L, true),
                new SupportOptionView(PAVEWAY_ID,
                        "support.wok_commander_support.f16c_gbu12_paveway_500lb",
                        "F-16C GBU-12 宝石路 II 500磅精准空袭", "F-16C 宝石路空袭",
                        SupportTargetMode.POINT, PAVEWAY_RADIUS, true, "", 0L, true));
    }

    /** JDAM call in flight towards (x, z) of {@code dimension}. */
    public static SupportMissionView jdamMission(ResourceLocation dimension, double x, double z,
                                                 long serverTick) {
        return new SupportMissionView(JDAM_MISSION, JDAM_ID, dimension, x, z, x, z,
                serverTick + 80L, 5);
    }

    /** Paveway call executing over (x, z) of {@code dimension}. */
    public static SupportMissionView pavewayMission(ResourceLocation dimension, double x, double z,
                                                    long serverTick) {
        return new SupportMissionView(PAVEWAY_MISSION, PAVEWAY_ID, dimension, x, z, x, z,
                serverTick, 40);
    }
}
