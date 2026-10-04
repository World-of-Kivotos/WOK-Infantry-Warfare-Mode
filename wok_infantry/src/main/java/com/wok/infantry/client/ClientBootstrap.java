package com.wok.infantry.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.KeyBindingDefaults.Binding;
import com.wok.infantry.client.KeyBindingDefaults.LabelSource;
import com.wok.infantry.config.InfantryClientConfig;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.battle.client.BattleClientNetworkBridge;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.packet.c2s.OpenWeaponTuningEditorPacket;
import com.wok.infantry.network.formation.client.FormationClientNetworkBridge;
import com.wok.infantry.network.serverbound.OpenLoadoutPacket;
import com.wok.infantry.integration.journeymap.JourneyMapUiPolicy;
import com.wok.infantry.integration.xaero.XaeroMinimapIntegration;
import com.wok.infantry.integration.xaero.XaeroWorldMapPolicy;
import com.wok.infantry.integration.tacz.TaczAdsSpeedAdapter;
import com.wok.infantry.client.screen.AdminLoadoutScreen;
import com.wok.infantry.client.screen.FormationSelectionScreen;
import com.wok.infantry.client.screen.PlayerLoadoutScreen;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalBoardChrome;
import com.wok.infantry.client.screen.TacticalMapScreen;
import com.wok.infantry.client.screen.WeaponTuningScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

public final class ClientBootstrap {
    // Default keys, conflict context and routing live in KeyBindingDefaults (unit tested).
    // The tactical map has no WOK default when Xaero's World Map can be redirected: Xaero's own M
    // opens the tactical map instead (XaeroWorldMapPolicy). An unknown Xaero build without the
    // map screen class counts as not installed here, so WOK keeps its M.
    private static final boolean XAERO_WORLD_MAP_INSTALLED = XaeroWorldMapPolicy.redirectAvailable();
    private static final KeyMapping OPEN_TERMINAL = mapping(Binding.TERMINAL);
    private static final KeyMapping OPEN_LOADOUT = mapping(Binding.LOADOUT);
    private static final KeyMapping OPEN_ADMIN_LOADOUT = mapping(Binding.ADMIN_LOADOUT);
    private static final KeyMapping OPEN_SQUAD = mapping(Binding.SQUAD);
    private static final KeyMapping OPEN_TACTICAL_MAP = mapping(Binding.TACTICAL_MAP);
    private static final KeyMapping OPEN_WEAPON_TUNING = mapping(Binding.WEAPON_TUNING);
    private static final Map<Binding, KeyMapping> MAPPINGS = new EnumMap<>(Map.of(
            Binding.TERMINAL, OPEN_TERMINAL,
            Binding.SQUAD, OPEN_SQUAD,
            Binding.TACTICAL_MAP, OPEN_TACTICAL_MAP,
            Binding.LOADOUT, OPEN_LOADOUT,
            Binding.ADMIN_LOADOUT, OPEN_ADMIN_LOADOUT,
            Binding.WEAPON_TUNING, OPEN_WEAPON_TUNING));
    private static boolean keyDefaultsMigrated;

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
        XaeroWorldMapPolicy.install();
        ClientBattleUi.register(modBus);
        BattleClientNetworkBridge.install();
        FormationClientNetworkBridge.install();
        ClientStaminaState.install();
        ClientStaminaController.register();
    }

    /** The WOK key mapping of {@code binding}. */
    public static KeyMapping keyMapping(Binding binding) {
        return MAPPINGS.get(Objects.requireNonNull(binding, "binding"));
    }

    /**
     * Name of the key that currently triggers {@code binding} as the player has bound it,
     * including a Forge modifier such as Ctrl (for footers and HUD hints), or {@code null} when
     * nothing is bound. The terminal and the former squad key stand in for each other, and the
     * tactical map shows Xaero's open-map key while that key is redirected to it.
     */
    public static Component keyLabel(Binding binding) {
        KeyMapping mapping = effectiveMapping(binding);
        if (mapping == null) {
            return null;
        }
        // A one- or two-pixel glyph such as the grave accent is named as printed on the key.
        Component label = mapping.getTranslatedKeyMessage();
        String readable = KeyBindingDefaults.readableKeyNameKey(label.getString());
        return readable == null ? label : Component.translatable(readable);
    }

    /** Footer hint {@code [key] action} for {@code binding}, or {@code null} when unbound. */
    public static TacticalBoardChrome.KeyHint keyHint(Binding binding, Component action) {
        Component label = keyLabel(binding);
        return label == null ? null : TacticalBoardChrome.KeyHint.of(label, action);
    }

    /**
     * Whether a key press received by a screen is a key of {@code binding} (any of its bound
     * {@link KeyBindingDefaults#labelSources label sources}), e.g. to close the tactical map with
     * the key that opened it. Works while a screen is open, unlike the in-game mappings.
     */
    public static boolean isKey(Binding binding, int keyCode, int scanCode) {
        for (LabelSource source : KeyBindingDefaults.labelSources(binding,
                XaeroWorldMapPolicy.redirectActive())) {
            KeyMapping mapping = resolve(source);
            if (mapping != null && !mapping.isUnbound() && mapping.matches(keyCode, scanCode)
                    && (mapping.getKeyModifier() == KeyModifier.NONE
                    || mapping.getKeyModifier().isActive(null))) {
                return true;
            }
        }
        return false;
    }

    private static KeyMapping mapping(Binding binding) {
        return new KeyMapping(binding.mappingName(), binding.conflictContext(),
                InputConstants.Type.KEYSYM,
                KeyBindingDefaults.defaultKey(binding, XAERO_WORLD_MAP_INSTALLED),
                KeyBindingDefaults.CATEGORY);
    }

    private static KeyMapping effectiveMapping(Binding binding) {
        return KeyBindingDefaults.effectiveMapping(binding, XaeroWorldMapPolicy.redirectActive(),
                ClientBootstrap::resolve, mapping -> !mapping.isUnbound());
    }

    private static KeyMapping resolve(LabelSource source) {
        return source == LabelSource.XAERO_OPEN_MAP ? XaeroWorldMapPolicy.openMapKey()
                : MAPPINGS.get(source.binding());
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        // The terminal comes first: the UI acceptance harness falls back to the first bound
        // key.wok_infantry.*terminal* mapping when the old squad mapping is unbound.
        event.register(OPEN_TERMINAL);
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
        ClientProneStability.tick();
        migrateKeyDefaultsOnce(Minecraft.getInstance());
        boolean terminalPressed = false;
        while (OPEN_TERMINAL.consumeClick()) {
            terminalPressed = true;
        }
        // The former squad key now follows the terminal route, so one press of either (or of
        // both, when bound to the same key) opens one screen.
        while (OPEN_SQUAD.consumeClick()) {
            terminalPressed = true;
        }
        if (terminalPressed) {
            openTerminal(Minecraft.getInstance());
        }
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
                    && !(minecraft.screen instanceof AdminLoadoutScreen)
                    && mayPress(minecraft, Binding.ADMIN_LOADOUT)) {
                LoadoutNetwork.sendToServer(new OpenLoadoutPacket(true));
            }
        }
        boolean mapPressed = false;
        while (OPEN_TACTICAL_MAP.consumeClick()) {
            mapPressed = true;
        }
        if (mapPressed) {
            openTacticalMap(Minecraft.getInstance());
        }
        while (OPEN_WEAPON_TUNING.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null && minecraft.player != null
                    && !(minecraft.screen instanceof WeaponTuningScreen)
                    && mayPress(minecraft, Binding.WEAPON_TUNING)) {
                BattleNetwork.sendToServer(new OpenWeaponTuningEditorPacket());
            }
        }
    }

    /** Squad page, or the formation page while the player has no formation yet. */
    private static void openTerminal(Minecraft minecraft) {
        if (minecraft.getConnection() == null || minecraft.player == null) {
            return;
        }
        switch (KeyBindingDefaults.terminalRoute(inBattle(), formationRequired())) {
            case SQUAD -> {
                if (!(minecraft.screen instanceof SquadScreen)) {
                    BattleClientNetworkBridge.openSquadScreen();
                }
            }
            case FORMATION -> openFormation(minecraft);
        }
    }

    /** Tactical map; formation page outside a battle; nothing when Xaero handles the press. */
    private static void openTacticalMap(Minecraft minecraft) {
        if (minecraft.getConnection() == null || minecraft.player == null
                || minecraft.screen instanceof TacticalMapScreen) {
            return;
        }
        switch (KeyBindingDefaults.mapRoute(
                XaeroWorldMapPolicy.handlesSamePress(OPEN_TACTICAL_MAP), inBattle(),
                formationRequired())) {
            case MAP -> BattleClientNetworkBridge.openMapScreen();
            case FORMATION -> openFormation(minecraft);
            case LEAVE_TO_XAERO -> {
            }
        }
    }

    /**
     * Formation entry of the terminal and map keys while the player has no formation
     * (player-08): the vote page opens at once (with the cached catalog, or waiting for it) and
     * asks the server for the current one. If the server answers that nothing needs to be chosen
     * any more, the page gives way to the squad page.
     */
    private static void openFormation(Minecraft minecraft) {
        if (!(minecraft.screen instanceof FormationSelectionScreen)) {
            FormationClientNetworkBridge.openFromTerminalKey(minecraft.screen);
        }
    }

    private static boolean inBattle() {
        return KeyBindingDefaults.inBattle(ClientBattleState.snapshot());
    }

    private static Boolean formationRequired() {
        FormationSelectionSnapshot formation = ClientFormationState.snapshot();
        return formation == null ? null : formation.selectionRequired();
    }

    /**
     * Moves WOK mappings still exactly on their pre-0.3.0-beta.8 default key to the new default,
     * once per client (recorded in {@code keys.defaultsRevision} of the client config). Runs on
     * the first client tick with both options.txt and the client config loaded, normally on the
     * title screen.
     */
    private static void migrateKeyDefaultsOnce(Minecraft minecraft) {
        if (keyDefaultsMigrated || minecraft.options == null) {
            return;
        }
        int applied = InfantryClientConfig.keyDefaultsRevision();
        if (applied < 0) {
            return;
        }
        keyDefaultsMigrated = true;
        if (applied >= KeyBindingDefaults.DEFAULTS_REVISION) {
            return;
        }
        // WOK's old M only gives way when Xaero's redirected open-map key is really on M as well,
        // and only while the redirect is switched on in the client config (loaded by now): with
        // tacticalMap.redirectXaeroWorldMap = false Xaero's M never reaches the tactical map.
        KeyMapping xaeroOpenMap = XaeroWorldMapPolicy.openMapKey();
        boolean xaeroTakesMapKey = xaeroOpenMap != null && KeyBindingDefaults.xaeroTakesOverMapKey(
                XaeroWorldMapPolicy.redirectActive(),
                xaeroOpenMap.getKey().getType() == InputConstants.Type.KEYSYM,
                xaeroOpenMap.getKey().getValue(),
                xaeroOpenMap.getKeyModifier() != KeyModifier.NONE);
        StringBuilder moved = new StringBuilder();
        for (Map.Entry<Binding, KeyMapping> entry : MAPPINGS.entrySet()) {
            KeyMapping mapping = entry.getValue();
            InputConstants.Key key = mapping.getKey();
            OptionalInt target = KeyBindingDefaults.migratedKey(entry.getKey(),
                    key.getType() == InputConstants.Type.KEYSYM, key.getValue(),
                    mapping.getKeyModifier() != KeyModifier.NONE, xaeroTakesMapKey);
            if (target.isPresent()) {
                mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(target.getAsInt()));
                moved.append(moved.isEmpty() ? "" : ", ").append(mapping.getName());
            }
        }
        if (!moved.isEmpty()) {
            KeyMapping.resetMapping();
            minecraft.options.save();
            WokInfantryMod.LOGGER.info("Moved WOK key mappings still on their old default key to "
                    + "the 0.3.0-beta.8 defaults: {}", moved);
        }
        InfantryClientConfig.setKeyDefaultsRevision(KeyBindingDefaults.DEFAULTS_REVISION);
    }

    /** Administrator keys are dropped silently without permission level 2 (server re-checks). */
    private static boolean mayPress(Minecraft minecraft, Binding binding) {
        return KeyBindingDefaults.allowsPress(binding, minecraft.player::hasPermissions);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBattleState.clear();
        ClientFormationState.clear();
        ClientLoadoutState.update(null);
        AdminLoadoutScreen.clearSessionMemory();
        TaczAdsSpeedAdapter.resetClientTracking();
        ClientStaminaController.reset();
        ClientStaminaState.clear();
        com.wok.infantry.battle.tickets.TicketNetwork.clearClient();
    }
}
