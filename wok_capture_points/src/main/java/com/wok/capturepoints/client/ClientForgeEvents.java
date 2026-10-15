package com.wok.capturepoints.client;

import com.wok.capturepoints.WokCapturePointsMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WokCapturePointsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCaptureState.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderGuiPre(RenderGuiEvent.Pre event) {
        CaptureHudOverlay.onRenderGuiPre(event);
    }

    /** Lowest priority without cancelled events: only boss bars that are really drawn count. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void bossEventProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        CaptureHudOverlay.onBossEventProgress(event);
    }
}
