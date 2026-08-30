package com.wok.infantry.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.battle.client.BattleClientNetworkBridge;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.packet.c2s.OpenWeaponTuningEditorPacket;
import com.wok.infantry.network.formation.client.FormationClientNetworkBridge;
import com.wok.infantry.network.serverbound.OpenLoadoutPacket;
import com.wok.infantry.integration.journeymap.JourneyMapUiPolicy;
import com.wok.infantry.integration.xaero.XaeroMinimapIntegration;
import com.wok.infantry.integration.tacz.TaczAdsSpeedAdapter;
import com.wok.infantry.client.screen.AdminLoadoutScreen;
import com.wok.infantry.client.screen.PlayerLoadoutScreen;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalMapScreen;
import com.wok.infantry.client.screen.WeaponTuningScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

public final class ClientBootstrap {
    private static final KeyMapping OPEN_LOADOUT = new KeyMapping(
            "key.wok_infantry.open_loadout",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.wok_infantry");
    private static final KeyMapping OPEN_ADMIN_LOADOUT = new KeyMapping(
            "key.wok_infantry.open_admin_loadout",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.categories.wok_infantry");
    private static final KeyMapping OPEN_SQUAD = new KeyMapping(
            "key.wok_infantry.open_squad",
            InputConstants.Type.KEYSYM,
            // JourneyMap uses J for its own fullscreen map by default.
            GLFW.GLFW_KEY_K,
            "key.categories.wok_infantry");
    private static final KeyMapping OPEN_TACTICAL_MAP = new KeyMapping(
            "key.wok_infantry.open_tactical_map",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.wok_infantry");
    private static final KeyMapping OPEN_WEAPON_TUNING = new KeyMapping(
            "key.wok_infantry.open_weapon_tuning",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.wok_infantry");

    private ClientBootstrap() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientBootstrap::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(ClientBootstrap::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientBootstrap::onLoggingOut);
        if (ModList.get().isLoaded("journeymap")) {
            // Keep every direct JourneyMap API reference behind its optional-mod gate. This lets
            // the same production JAR start with Xaero as the only client terrain provider.
            MinecraftForge.EVENT_BUS.addListener(JourneyMapUiPolicy::onScreenOpening);
        }
        XaeroMinimapIntegration.install();
        ClientBattleUi.register(modBus);
        BattleClientNetworkBridge.install();
        FormationClientNetworkBridge.install();
        ClientStaminaState.install();
        ClientStaminaController.register();
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_LOADOUT);
        event.register(OPEN_ADMIN_LOADOUT);
        event.register(OPEN_SQUAD);
        event.register(OPEN_TACTICAL_MAP);
        event.register(OPEN_WEAPON_TUNING);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (ModList.get().isLoaded("journeymap")) {
            JourneyMapUiPolicy.enforceMinimapHidden();
        }
        XaeroMinimapIntegration.onClientTick();
        TaczAdsSpeedAdapter.clientTick(Minecraft.getInstance().player);
        while (OPEN_LOADOUT.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null && minecraft.player != null
                    && !(minecraft.screen instanceof PlayerLoadoutScreen)) {
                LoadoutNetwork.sendToServer(new OpenLoadoutPacket(false));
            }
        }
        while (OPEN_ADMIN_LOADOUT.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null && minecraft.player != null
                    && !(minecraft.screen instanceof AdminLoadoutScreen)) {
                LoadoutNetwork.sendToServer(new OpenLoadoutPacket(true));
            }
        }
        while (OPEN_SQUAD.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null && minecraft.player != null
                    && !(minecraft.screen instanceof SquadScreen)) {
                BattleClientNetworkBridge.openSquadScreen();
            }
        }
        while (OPEN_TACTICAL_MAP.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null && minecraft.player != null
                    && !(minecraft.screen instanceof TacticalMapScreen)) {
                BattleClientNetworkBridge.openMapScreen();
            }
        }
        while (OPEN_WEAPON_TUNING.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null && minecraft.player != null
                    && !(minecraft.screen instanceof WeaponTuningScreen)) {
                BattleNetwork.sendToServer(new OpenWeaponTuningEditorPacket());
            }
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBattleState.clear();
        ClientFormationState.clear();
        ClientLoadoutState.update(null);
        TaczAdsSpeedAdapter.resetClientTracking();
        ClientStaminaController.reset();
        ClientStaminaState.clear();
    }
}
