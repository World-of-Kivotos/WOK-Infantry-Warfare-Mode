package com.wok.infantry.network.formation;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.formation.packet.c2s.RequestFormationCatalogPacket;
import com.wok.infantry.network.formation.packet.c2s.SelectFormationPacket;
import com.wok.infantry.network.formation.packet.c2s.CastFormationVotePacket;
import com.wok.infantry.network.formation.packet.c2s.SelectFactionPacket;
import com.wok.infantry.network.formation.packet.s2c.FormationCatalogPacket;
import com.wok.infantry.network.formation.packet.s2c.FormationSelectionResultPacket;
import com.wok.infantry.server.FormationService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Objects;

/** Independent versioned channel for the pre-battle roster selection state machine. */
public final class FormationNetwork {
    public static final ResourceLocation CHANNEL_NAME = ResourceLocation.fromNamespaceAndPath(
            WokInfantryMod.MOD_ID, "formation");
    /**
     * 5 (core 0.3.0-beta.8): every viewer receives each faction's ballot phase and locked
     * formation, formations carry a structured detail with public names, the catalog has a
     * support name table and a lock-notice flag. Client and server must both be beta.8.
     */
    public static final String PROTOCOL_VERSION = "5";

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_NAME)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();
    private static boolean initialized;

    private FormationNetwork() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        CHANNEL.messageBuilder(RequestFormationCatalogPacket.class, 0,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestFormationCatalogPacket::encode)
                .decoder(RequestFormationCatalogPacket::decode)
                .consumerMainThread(RequestFormationCatalogPacket::handle)
                .add();
        CHANNEL.messageBuilder(SelectFormationPacket.class, 1,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectFormationPacket::encode)
                .decoder(SelectFormationPacket::decode)
                .consumerMainThread(SelectFormationPacket::handle)
                .add();
        CHANNEL.messageBuilder(CastFormationVotePacket.class, 2,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(CastFormationVotePacket::encode)
                .decoder(CastFormationVotePacket::decode)
                .consumerMainThread(CastFormationVotePacket::handle)
                .add();
        CHANNEL.messageBuilder(SelectFactionPacket.class, 3,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectFactionPacket::encode)
                .decoder(SelectFactionPacket::decode)
                .consumerMainThread(SelectFactionPacket::handle)
                .add();
        CHANNEL.messageBuilder(FormationCatalogPacket.class, 8,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FormationCatalogPacket::encode)
                .decoder(FormationCatalogPacket::decode)
                .consumerMainThread(FormationCatalogPacket::handle)
                .add();
        CHANNEL.messageBuilder(FormationSelectionResultPacket.class, 9,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FormationSelectionResultPacket::encode)
                .decoder(FormationSelectionResultPacket::decode)
                .consumerMainThread(FormationSelectionResultPacket::handle)
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
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> Objects.requireNonNull(player, "player")),
                Objects.requireNonNull(packet, "packet"));
    }

    public static void sendSnapshotToPlayer(ServerPlayer player, boolean openScreen) {
        sendSnapshotToPlayer(player, openScreen, false);
    }

    /**
     * Sends the viewer's catalog. {@code lockNotice} marks that the faction's shared formation
     * was just applied to this player; the client then records the lock and opens the deployment
     * page (unless another mod's screen is open, then the HUD shows the notice).
     */
    public static void sendSnapshotToPlayer(ServerPlayer player, boolean openScreen,
                                            boolean lockNotice) {
        Objects.requireNonNull(player, "player");
        FormationService service = FormationService.get(player).orElse(null);
        if (service == null) {
            sendToPlayer(player, new FormationSelectionResultPacket(false,
                    "阵营编制服务尚未就绪"));
            return;
        }
        sendToPlayer(player, new FormationCatalogPacket(service.snapshotFor(player), openScreen,
                lockNotice));
    }

    /**
     * The player's formation was just assigned (lock, late join, administrator assignment):
     * first the battle snapshot, so the deployment page the client opens already shows the new
     * formation, then the catalog with the lock notice. The server no longer pushes the
     * deployment page itself; the client decides whether a WOK terminal may be replaced.
     */
    public static void sendFormationApplied(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (BattleNetwork.isInitialized()) {
            BattleService.get(player).ifPresent(service -> BattleNetwork.sendSnapshotToPlayer(
                    service, player, BattleOpenTarget.NONE));
        }
        sendSnapshotToPlayer(player, false, true);
    }

    /**
     * Result receipt shown in the vote page footer. An empty success sends nothing; an empty
     * failure says that it failed instead of leaking the result code's enum name.
     */
    public static void sendResult(ServerPlayer player, ActionResult result) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(result, "result");
        if (result.message().isBlank() && result.success()) {
            return;
        }
        sendToPlayer(player, new FormationSelectionResultPacket(result.success(),
                result.message().isBlank() ? "操作未完成" : result.message()));
    }

    public static void finishSelection(ServerPlayer player, ActionResult result) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(result, "result");
        sendResult(player, result);
        if (!result.success()) {
            if (!result.message().isBlank()) {
                player.displayClientMessage(Component.literal(result.message()), false);
            }
            sendSnapshotToPlayer(player, true);
            return;
        }
        if (hasFormation(player)) {
            sendFormationApplied(player);
        } else {
            // Faction-only assignment while the faction still votes: show the vote page.
            sendSnapshotToPlayer(player, true);
        }
    }

    public static void finishVote(ServerPlayer player, ActionResult result) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(result, "result");
        sendResult(player, result);
        if (!result.message().isBlank()) {
            player.displayClientMessage(Component.literal(result.message()), false);
        }
        sendSnapshotToPlayer(player, true);
        if (result.success()) {
            refreshFactionMates(player);
        }
    }

    public static void finishFactionSelection(ServerPlayer player, ActionResult result) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(result, "result");
        sendResult(player, result);
        if (!result.message().isBlank()) {
            player.displayClientMessage(Component.literal(result.message()), false);
        }
        if (result.success() && hasFormation(player)) {
            // Joined after the lock: the locked formation is already applied (vote-01).
            sendFormationApplied(player);
        } else {
            sendSnapshotToPlayer(player, result.success());
        }
        if (result.success()) {
            // Population and "已投 n/人数" changed for everyone already in the faction.
            refreshFactionMates(player);
        }
    }

    private static boolean hasFormation(ServerPlayer player) {
        return FormationService.get(player)
                .flatMap(service -> service.selectedFormation(player.getUUID())).isPresent();
    }

    private static void refreshFactionMates(ServerPlayer player) {
        BattleService.get(player).flatMap(battle -> battle.factionOf(player.getUUID()))
                .ifPresent(faction -> player.server.getPlayerList().getPlayers().stream()
                        .filter(other -> !other.getUUID().equals(player.getUUID()))
                        .filter(other -> BattleService.get(other)
                                .flatMap(battle -> battle.factionOf(other.getUUID()))
                                .filter(faction::equals).isPresent())
                        .forEach(other -> sendSnapshotToPlayer(other, false)));
    }

    private static synchronized void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("FormationNetwork.init() has not been called");
        }
    }
}
