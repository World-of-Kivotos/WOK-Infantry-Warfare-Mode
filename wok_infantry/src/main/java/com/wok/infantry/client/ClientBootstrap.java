package com.wok.infantry.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.serverbound.OpenLoadoutPacket;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

public final class ClientBootstrap {
    private static final KeyMapping OPEN_LOADOUT = new KeyMapping(
            "key.wok_infantry.open_loadout",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.wok_infantry");

    private ClientBootstrap() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientBootstrap::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(ClientBootstrap::onClientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_LOADOUT);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (OPEN_LOADOUT.consumeClick()) {
            LoadoutNetwork.sendToServer(new OpenLoadoutPacket(false));
        }
    }
}
