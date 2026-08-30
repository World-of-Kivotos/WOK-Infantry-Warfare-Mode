package com.wok.infantry.client;

import com.wok.infantry.client.hud.SquadHudOverlay;
import com.wok.infantry.client.hud.StaminaHudOverlay;
import com.wok.infantry.client.render.SquadWorldMarkerRenderer;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
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
        modBus.addListener(SquadHudOverlay::register);
        modBus.addListener(StaminaHudOverlay::register);
        MinecraftForge.EVENT_BUS.addListener(SquadWorldMarkerRenderer::onRenderPlayer);
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
