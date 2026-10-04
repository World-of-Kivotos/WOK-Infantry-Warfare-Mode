package com.wok.infantry.client;

import com.wok.infantry.client.hud.BattleStripOverlay;
import com.wok.infantry.client.hud.FormationVoteHudOverlay;
import com.wok.infantry.client.hud.HudFrame;
import com.wok.infantry.client.hud.SquadHudOverlay;
import com.wok.infantry.client.hud.StaminaHudOverlay;
import com.wok.infantry.client.render.SquadWorldMarkerRenderer;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

/** Single registration/opening surface for ClientBootstrap and key bindings. */
public final class ClientBattleUi {
    private static boolean registered;

    private ClientBattleUi() {
    }

    public static void register(IEventBus modBus) {
        if (registered) {
            return;
        }
        registered = true;
        modBus.addListener(ClientBattleUi::registerOverlays);
        MinecraftForge.EVENT_BUS.addListener(SquadWorldMarkerRenderer::onRenderPlayer);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true,
                RenderGuiEvent.Pre.class, HudFrame::onRenderGuiPre);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                RenderGuiOverlayEvent.Pre.class, BattleStripOverlay::onOverlayPre);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                RenderGuiOverlayEvent.Post.class, BattleStripOverlay::onOverlayPost);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                RenderGuiEvent.Post.class, BattleStripOverlay::onGuiPost);
    }

    /**
     * Every core HUD overlay sits just below the vanilla F3 text, so F3, the chat and the player
     * list (and add-on overlays registered above all) draw over it. Order, bottom to top: battle
     * strip, formation ballot, squad roster, stamina. Ids squad_roster, stamina and tickets are
     * kept from earlier versions.
     */
    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        BattleStripOverlay.register(event);
        FormationVoteHudOverlay.register(event);
        SquadHudOverlay.register(event);
        StaminaHudOverlay.register(event);
    }

    public static void openSquadScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new SquadScreen(minecraft.screen));
    }

    public static void openTacticalMap() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new TacticalMapScreen(minecraft.screen));
    }
}
