package com.wok.bodyhealth.client;

import com.wok.bodyhealth.WokBodyHealthMod;
import com.wok.bodyhealth.config.BodyHealthConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = WokBodyHealthMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ClientForgeEvents {
    @SubscribeEvent
    public static void hideVanillaHearts(RenderGuiOverlayEvent.Pre event) {
        if (BodyHealthConfig.REPLACE_VANILLA_HEARTS.get()
                && ClientBodyHealthState.get() != null
                && event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
        }
    }

    private ClientForgeEvents() {
    }
}
