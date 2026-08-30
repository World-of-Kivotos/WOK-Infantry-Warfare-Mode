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
    public static final String PROTOCOL_VERSION = "4";

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
        Objects.requireNonNull(player, "player");
        FormationService service = FormationService.get(player).orElse(null);
        if (service == null) {
            sendToPlayer(player, new FormationSelectionResultPacket(false,
                    "阵营编制服务尚未就绪"));
            return;
        }
        sendToPlayer(player, new FormationCatalogPacket(service.snapshotFor(player), openScreen));
    }

    public static void finishSelection(ServerPlayer player, ActionResult result) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(result, "result");
        sendToPlayer(player, new FormationSelectionResultPacket(result.success(),
                result.message().isBlank() ? result.code().name() : result.message()));
        if (!result.success()) {
            player.displayClientMessage(Component.literal(result.message()), false);
            sendSnapshotToPlayer(player, true);
            return;
        }
        sendSnapshotToPlayer(player, false);
        BattleService.get(player).ifPresent(service -> BattleNetwork.sendSnapshotToPlayer(
                service, player, BattleOpenTarget.DEPLOYMENT));
    }

    public static void finishVote(ServerPlayer player, ActionResult result) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(result, "result");
        sendToPlayer(player, new FormationSelectionResultPacket(result.success(),
                result.message().isBlank() ? result.code().name() : result.message()));
        if (!result.message().isBlank()) {
            player.displayClientMessage(Component.literal(result.message()), false);
        }
        sendSnapshotToPlayer(player, true);
        if (result.success()) {
            BattleService.get(player).flatMap(battle -> battle.factionOf(player.getUUID()))
                    .ifPresent(faction -> player.server.getPlayerList().getPlayers().stream()
                            .filter(other -> !other.getUUID().equals(player.getUUID()))
                            .filter(other -> BattleService.get(other)
                                    .flatMap(battle -> battle.factionOf(other.getUUID()))
                                    .filter(faction::equals).isPresent())
                            .forEach(other -> sendSnapshotToPlayer(other, false)));
        }
    }

    public static void finishFactionSelection(ServerPlayer player, ActionResult result) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(result, "result");
        sendToPlayer(player, new FormationSelectionResultPacket(result.success(),
                result.message().isBlank() ? result.code().name() : result.message()));
        if (!result.message().isBlank()) {
            player.displayClientMessage(Component.literal(result.message()), false);
        }
        sendSnapshotToPlayer(player, result.success());
    }

    private static synchronized void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("FormationNetwork.init() has not been called");
        }
    }
}
