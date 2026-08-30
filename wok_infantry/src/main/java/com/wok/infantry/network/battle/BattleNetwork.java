package com.wok.infantry.network.battle;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.network.battle.packet.c2s.BattleOpenPacket;
import com.wok.infantry.network.battle.packet.c2s.BattleSnapshotRequestPacket;
import com.wok.infantry.network.battle.packet.c2s.AssignClassPacket;
import com.wok.infantry.network.battle.packet.c2s.ClaimCommanderPacket;
import com.wok.infantry.network.battle.packet.c2s.CommanderActionPacket;
import com.wok.infantry.network.battle.packet.c2s.CreateMarkerPacket;
import com.wok.infantry.network.battle.packet.c2s.DeployPacket;
import com.wok.infantry.network.battle.packet.c2s.RedeployPacket;
import com.wok.infantry.network.battle.packet.c2s.RemoveMarkerPacket;
import com.wok.infantry.network.battle.packet.c2s.ResupplyPacket;
import com.wok.infantry.network.battle.packet.c2s.RequestSupportPacket;
import com.wok.infantry.network.battle.packet.c2s.OpenWeaponTuningEditorPacket;
import com.wok.infantry.network.battle.packet.c2s.SelectDeploymentPointPacket;
import com.wok.infantry.network.battle.packet.c2s.SelectAmmoSupplyGunPacket;
import com.wok.infantry.network.battle.packet.c2s.SupplyVehicleAmmoPacket;
import com.wok.infantry.network.battle.packet.c2s.SquadActionPacket;
import com.wok.infantry.network.battle.packet.c2s.ApplyWeaponTuningPacket;
import com.wok.infantry.network.battle.packet.s2c.BattleClearPacket;
import com.wok.infantry.network.battle.packet.s2c.BattleActionFeedbackPacket;
import com.wok.infantry.network.battle.packet.s2c.BattleSnapshotPacket;
import com.wok.infantry.network.battle.packet.s2c.OpenAmmoSupplyPacket;
import com.wok.infantry.network.battle.packet.s2c.OpenWeaponTuningPacket;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Objects;
import java.util.Map;

/** Dedicated, versioned battle channel. It is intentionally independent of LoadoutNetwork. */
public final class BattleNetwork {
    public static final ResourceLocation CHANNEL_NAME =
            ResourceLocation.fromNamespaceAndPath(WokInfantryMod.MOD_ID, "battle");
    public static final String PROTOCOL_VERSION = "15";

    public static final int C2S_OPEN_ID = 0;
    public static final int C2S_SNAPSHOT_REQUEST_ID = 1;
    public static final int C2S_SQUAD_ACTION_ID = 2;
    public static final int C2S_CREATE_MARKER_ID = 3;
    public static final int C2S_REMOVE_MARKER_ID = 4;
    public static final int C2S_CLAIM_COMMANDER_ID = 5;
    public static final int C2S_ASSIGN_CLASS_ID = 6;
    public static final int C2S_SELECT_DEPLOYMENT_POINT_ID = 7;
    public static final int C2S_DEPLOY_ID = 8;
    public static final int C2S_REDEPLOY_ID = 9;
    public static final int C2S_RESUPPLY_ID = 10;
    public static final int C2S_COMMANDER_ACTION_ID = 12;
    public static final int C2S_REQUEST_SUPPORT_ID = 13;
    public static final int C2S_SELECT_AMMO_SUPPLY_GUN_ID = 14;
    public static final int C2S_SUPPLY_VEHICLE_AMMO_ID = 15;
    public static final int S2C_SNAPSHOT_ID = 16;
    public static final int S2C_CLEAR_ID = 17;
    public static final int S2C_ACTION_FEEDBACK_ID = 18;
    public static final int S2C_OPEN_AMMO_SUPPLY_ID = 19;
    public static final int C2S_APPLY_WEAPON_TUNING_ID = 20;
    public static final int S2C_OPEN_WEAPON_TUNING_ID = 21;
    public static final int C2S_OPEN_WEAPON_TUNING_ID = 22;

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_NAME)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static boolean initialized;

    private BattleNetwork() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        CHANNEL.messageBuilder(BattleOpenPacket.class, C2S_OPEN_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(BattleOpenPacket::encode)
                .decoder(BattleOpenPacket::decode)
                .consumerMainThread(BattleOpenPacket::handle)
                .add();
        CHANNEL.messageBuilder(BattleSnapshotRequestPacket.class, C2S_SNAPSHOT_REQUEST_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(BattleSnapshotRequestPacket::encode)
                .decoder(BattleSnapshotRequestPacket::decode)
                .consumerMainThread(BattleSnapshotRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(SquadActionPacket.class, C2S_SQUAD_ACTION_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(SquadActionPacket::encode)
                .decoder(SquadActionPacket::decode)
                .consumerMainThread(SquadActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(CreateMarkerPacket.class, C2S_CREATE_MARKER_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(CreateMarkerPacket::encode)
                .decoder(CreateMarkerPacket::decode)
                .consumerMainThread(CreateMarkerPacket::handle)
                .add();
        CHANNEL.messageBuilder(RemoveMarkerPacket.class, C2S_REMOVE_MARKER_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(RemoveMarkerPacket::encode)
                .decoder(RemoveMarkerPacket::decode)
                .consumerMainThread(RemoveMarkerPacket::handle)
                .add();
        CHANNEL.messageBuilder(ClaimCommanderPacket.class, C2S_CLAIM_COMMANDER_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClaimCommanderPacket::encode)
                .decoder(ClaimCommanderPacket::decode)
                .consumerMainThread(ClaimCommanderPacket::handle)
                .add();
        CHANNEL.messageBuilder(AssignClassPacket.class, C2S_ASSIGN_CLASS_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(AssignClassPacket::encode)
                .decoder(AssignClassPacket::decode)
                .consumerMainThread(AssignClassPacket::handle)
                .add();
        CHANNEL.messageBuilder(SelectDeploymentPointPacket.class,
                        C2S_SELECT_DEPLOYMENT_POINT_ID, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectDeploymentPointPacket::encode)
                .decoder(SelectDeploymentPointPacket::decode)
                .consumerMainThread(SelectDeploymentPointPacket::handle)
                .add();
        CHANNEL.messageBuilder(DeployPacket.class, C2S_DEPLOY_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeployPacket::encode)
                .decoder(DeployPacket::decode)
                .consumerMainThread(DeployPacket::handle)
                .add();
        CHANNEL.messageBuilder(RedeployPacket.class, C2S_REDEPLOY_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(RedeployPacket::encode)
                .decoder(RedeployPacket::decode)
                .consumerMainThread(RedeployPacket::handle)
                .add();
        CHANNEL.messageBuilder(ResupplyPacket.class, C2S_RESUPPLY_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(ResupplyPacket::encode)
                .decoder(ResupplyPacket::decode)
                .consumerMainThread(ResupplyPacket::handle)
                .add();
        CHANNEL.messageBuilder(CommanderActionPacket.class, C2S_COMMANDER_ACTION_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(CommanderActionPacket::encode)
                .decoder(CommanderActionPacket::decode)
                .consumerMainThread(CommanderActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestSupportPacket.class, C2S_REQUEST_SUPPORT_ID,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestSupportPacket::encode)
                .decoder(RequestSupportPacket::decode)
                .consumerMainThread(RequestSupportPacket::handle)
                .add();
        CHANNEL.messageBuilder(SelectAmmoSupplyGunPacket.class,
                        C2S_SELECT_AMMO_SUPPLY_GUN_ID, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectAmmoSupplyGunPacket::encode)
                .decoder(SelectAmmoSupplyGunPacket::decode)
                .consumerMainThread(SelectAmmoSupplyGunPacket::handle)
                .add();
        CHANNEL.messageBuilder(SupplyVehicleAmmoPacket.class,
                        C2S_SUPPLY_VEHICLE_AMMO_ID, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SupplyVehicleAmmoPacket::encode)
                .decoder(SupplyVehicleAmmoPacket::decode)
                .consumerMainThread(SupplyVehicleAmmoPacket::handle)
                .add();
        CHANNEL.messageBuilder(BattleSnapshotPacket.class, S2C_SNAPSHOT_ID,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BattleSnapshotPacket::encode)
                .decoder(BattleSnapshotPacket::decode)
                .consumerMainThread(BattleSnapshotPacket::handle)
                .add();
        CHANNEL.messageBuilder(BattleClearPacket.class, S2C_CLEAR_ID,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BattleClearPacket::encode)
                .decoder(BattleClearPacket::decode)
                .consumerMainThread(BattleClearPacket::handle)
                .add();
        CHANNEL.messageBuilder(BattleActionFeedbackPacket.class, S2C_ACTION_FEEDBACK_ID,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BattleActionFeedbackPacket::encode)
                .decoder(BattleActionFeedbackPacket::decode)
                .consumerMainThread(BattleActionFeedbackPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenAmmoSupplyPacket.class, S2C_OPEN_AMMO_SUPPLY_ID,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenAmmoSupplyPacket::encode)
                .decoder(OpenAmmoSupplyPacket::decode)
                .consumerMainThread(OpenAmmoSupplyPacket::handle)
                .add();
        CHANNEL.messageBuilder(ApplyWeaponTuningPacket.class,
                        C2S_APPLY_WEAPON_TUNING_ID, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ApplyWeaponTuningPacket::encode)
                .decoder(ApplyWeaponTuningPacket::decode)
                .consumerMainThread(ApplyWeaponTuningPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenWeaponTuningPacket.class,
                        S2C_OPEN_WEAPON_TUNING_ID, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenWeaponTuningPacket::encode)
                .decoder(OpenWeaponTuningPacket::decode)
                .consumerMainThread(OpenWeaponTuningPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenWeaponTuningEditorPacket.class,
                        C2S_OPEN_WEAPON_TUNING_ID, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenWeaponTuningEditorPacket::encode)
                .decoder(OpenWeaponTuningEditorPacket::decode)
                .consumerMainThread(OpenWeaponTuningEditorPacket::handle)
                .add();
        initialized = true;
    }

    public static synchronized boolean isInitialized() {
        return initialized;
    }

    public static void sendToServer(Object packet) {
        ensureInitialized();
        CHANNEL.sendToServer(Objects.requireNonNull(packet, "packet"));
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        ensureInitialized();
        Objects.requireNonNull(player, "player");
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                Objects.requireNonNull(packet, "packet"));
    }

    /** Always builds the snapshot for the receiving player, preserving service-side filtering. */
    public static void sendSnapshotToPlayer(BattleService service, ServerPlayer player,
                                            BattleOpenTarget openTarget) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(openTarget, "openTarget");
        if (service.server() != player.getServer()) {
            throw new IllegalArgumentException("BattleService and player belong to different servers");
        }
        if (service.factionOf(player.getUUID()).isEmpty()) {
            sendClearToPlayer(player);
            return;
        }
        LoadoutService loadoutService = LoadoutService.get(player).orElse(null);
        Map<String, Integer> classLimits = loadoutService == null
                ? BattleRules.DEFAULT_CLASS_LIMITS : loadoutService.classLimits(player);
        Map<String, String> classDisplayNames = loadoutService == null
                ? Map.of() : loadoutService.classDisplayNames(player);
        BattleSnapshot snapshot = service.snapshotFor(player, classLimits, classDisplayNames);
        DeploymentService deploymentService = DeploymentService.get(player)
                .orElseThrow(() -> new IllegalStateException("Deployment service is not running"));
        sendToPlayer(player, new BattleSnapshotPacket(
                snapshot.withDeployment(deploymentService.viewFor(player)), openTarget));
    }

    public static void sendSnapshotToPlayer(ServerPlayer player, BattleOpenTarget openTarget) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = Objects.requireNonNull(player.getServer(), "player server");
        runOnServer(server, () -> BattleService.get(player).ifPresentOrElse(
                service -> sendSnapshotToPlayer(service, player, openTarget),
                () -> sendToPlayer(player, new BattleClearPacket())));
    }

    /**
     * Rebuilds one independently filtered snapshot per connected player. Never broadcast a single
     * player's snapshot with PacketDistributor.ALL.
     */
    public static void broadcastSnapshots(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        runOnServer(server, () -> BattleService.get(server).ifPresentOrElse(
                service -> broadcastSnapshots(service, server),
                () -> broadcastClear(server)));
    }

    /** Main-thread service hook for mutations that affect more than the initiating player. */
    public static void broadcastSnapshots(BattleService service, MinecraftServer server) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(server, "server");
        if (service.server() != server) {
            throw new IllegalArgumentException("BattleService belongs to a different server");
        }
        runOnServer(server, () -> server.getPlayerList().getPlayers().forEach(player ->
                sendSnapshotToPlayer(service, player, BattleOpenTarget.NONE)));
    }

    /**
     * Refreshes only the actor's faction. Each recipient still gets a separately built,
     * viewer-filtered battle and deployment snapshot.
     */
    public static void broadcastFactionSnapshots(BattleService service, ServerPlayer actor) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(actor, "actor");
        MinecraftServer server = Objects.requireNonNull(actor.getServer(), "actor server");
        if (service.server() != server) {
            throw new IllegalArgumentException("BattleService belongs to a different server");
        }
        runOnServer(server, () -> {
            Faction faction = service.factionOf(actor.getUUID()).orElse(null);
            if (faction == null) {
                sendSnapshotToPlayer(service, actor, BattleOpenTarget.NONE);
                return;
            }
            server.getPlayerList().getPlayers().stream()
                    .filter(player -> service.factionOf(player.getUUID())
                            .filter(faction::equals).isPresent())
                    .forEach(player -> sendSnapshotToPlayer(service, player,
                            BattleOpenTarget.NONE));
        });
    }

    /** Shared result routing for deployment intents handled on the server main thread. */
    public static void finishDeploymentAction(BattleService service, ServerPlayer actor,
                                              ActionResult result,
                                              boolean factionVisibleChange) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(result, "result");
        MinecraftServer server = Objects.requireNonNull(actor.getServer(), "actor server");
        if (service.server() != server) {
            throw new IllegalArgumentException("BattleService belongs to a different server");
        }
        runOnServer(server, () -> {
            String message = result.message().isBlank() ? result.code().name() : result.message();
            if (message.length() > BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH) {
                message = message.substring(0, BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH);
            }
            sendToPlayer(actor, new BattleActionFeedbackPacket(result.success(), message));
            if (!result.success()) {
                actor.displayClientMessage(Component.literal(message), false);
            } else if (ServerRequestLimiter.allow(actor,
                    ServerRequestLimiter.Kind.SNAPSHOT_RESPONSE)) {
                // Other faction members receive the change on the fixed one-second heartbeat.
                // This coalesces simultaneous 40v40 mutations and avoids request amplification.
                sendSnapshotToPlayer(service, actor, BattleOpenTarget.NONE);
            }
        });
    }

    public static void sendClearToPlayer(ServerPlayer player) {
        sendToPlayer(player, new BattleClearPacket());
    }

    public static void broadcastClear(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        runOnServer(server, () -> server.getPlayerList().getPlayers().forEach(
                player -> sendToPlayer(player, new BattleClearPacket())));
    }

    private static void runOnServer(MinecraftServer server, Runnable action) {
        if (server.isSameThread()) {
            action.run();
        } else {
            server.execute(action);
        }
    }

    private static synchronized void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("BattleNetwork.init() has not been called");
        }
    }
}
