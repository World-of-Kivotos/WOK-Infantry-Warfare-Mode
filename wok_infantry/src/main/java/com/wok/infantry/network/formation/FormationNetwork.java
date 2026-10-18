package com.wok.infantry.network.formation;

import com.mojang.logging.LogUtils;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.battle.PerRecipientDelivery;
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
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Independent versioned channel for the pre-battle roster selection state machine. */
public final class FormationNetwork {
    public static final ResourceLocation CHANNEL_NAME = ResourceLocation.fromNamespaceAndPath(
            WokInfantryMod.MOD_ID, "formation");
    /**
     * 5 (core 0.4.0-beta.1): every viewer receives each faction's ballot phase and locked
     * formation, formations carry a structured detail with public names, the catalog has a
     * support name table and a lock-notice flag. Client and server must both be 0.4.0-beta.1.
     */
    public static final String PROTOCOL_VERSION = "5";

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_NAME)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final PerRecipientDelivery DELIVERY_FAILURES = new PerRecipientDelivery();
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
     * A failing battle snapshot is logged and does not stop the catalog.
     */
    public static void sendFormationApplied(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (BattleNetwork.isInitialized()) {
            deliverSafely(player, recipient -> BattleService.get(recipient).ifPresent(service ->
                    BattleNetwork.sendSnapshotToPlayer(service, recipient,
                            BattleOpenTarget.NONE)));
        }
        deliverSafely(player, recipient -> sendSnapshotToPlayer(recipient, false, true));
    }

    /**
     * Runs {@code delivery} for every recipient; a failure for one player is logged (once per
     * signature and window) and never stops the others. Every formation fan-out goes through
     * here: vote opened or locked, catalog reload or transfer, battle reset, teammate refresh.
     */
    public static void forEachRecipient(Iterable<ServerPlayer> recipients,
                                        Consumer<ServerPlayer> delivery) {
        PerRecipientDelivery.deliverEach(recipients, delivery, FormationNetwork::reportFailure);
    }

    private static void deliverSafely(ServerPlayer player, Consumer<ServerPlayer> delivery) {
        forEachRecipient(List.of(player), delivery);
    }

    private static void reportFailure(ServerPlayer player, RuntimeException failure) {
        String playerName = player.getGameProfile().getName();
        if (DELIVERY_FAILURES.firstReport(failure)) {
            LOGGER.error("Could not send the formation catalog or battle snapshot to {}; the "
                    + "other players in this pass still received theirs. Repeats of this "
                    + "failure within five minutes are logged at debug level only.", playerName,
                    failure);
        } else {
            LOGGER.debug("Formation delivery to {} failed again: {}", playerName,
                    PerRecipientDelivery.signature(failure));
        }
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

    /**
     * Answer to the player's own faction-and-formation request ({@code select}). {@code before}
     * is the player's state taken before the request: only a real change pushes the formation
     * and refreshes the old and new faction's members; a repeat answers the sender alone.
     */
    public static void finishSelection(ServerPlayer player, ActionResult result,
                                       FormationSeatState before) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(before, "before");
        sendResult(player, result);
        FormationSeatState after = FormationSeatState.of(player);
        if (!result.success()) {
            if (!result.message().isBlank()) {
                player.displayClientMessage(Component.literal(result.message()), false);
            }
            deliverSafely(player, recipient -> sendSnapshotToPlayer(recipient, true));
            // The faction may have been joined before the vote part was refused.
            refreshFactionMembers(player, before.affectedFactions(after));
            return;
        }
        if (before.seatChanged(after) && hasFormation(player)) {
            sendFormationApplied(player);
        } else {
            // Faction only while the faction still votes, or nothing changed: the vote page.
            deliverSafely(player, recipient -> sendSnapshotToPlayer(recipient, true));
        }
        refreshFactionMembers(player, before.affectedFactions(after));
    }

    /** Answer to a vote; teammates refresh only when the player's vote really changed. */
    public static void finishVote(ServerPlayer player, ActionResult result,
                                  FormationSeatState before) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(before, "before");
        sendResult(player, result);
        if (!result.message().isBlank()) {
            player.displayClientMessage(Component.literal(result.message()), false);
        }
        deliverSafely(player, recipient -> sendSnapshotToPlayer(recipient, true));
        refreshFactionMembers(player, before.affectedFactions(FormationSeatState.of(player)));
    }

    /**
     * Answer to a faction choice. Joining after the lock applies the locked formation (vote-01);
     * only a real join refreshes the faction's members ("已投 n/人数" and the population changed).
     * Choosing the faction the player is already in answers the sender alone, without a battle
     * snapshot or teammate refresh (NET-1).
     */
    public static void finishFactionSelection(ServerPlayer player, ActionResult result,
                                              FormationSeatState before) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(before, "before");
        sendResult(player, result);
        if (!result.message().isBlank()) {
            player.displayClientMessage(Component.literal(result.message()), false);
        }
        FormationSeatState after = FormationSeatState.of(player);
        if (result.success() && before.seatChanged(after) && hasFormation(player)) {
            sendFormationApplied(player);
        } else {
            deliverSafely(player, recipient -> sendSnapshotToPlayer(recipient, result.success()));
        }
        refreshFactionMembers(player, before.affectedFactions(after));
    }

    /**
     * An administrator assignment of {@code target} succeeded. When the target's faction or
     * formation really changed, the target gets a receipt in its own words with public names
     * (never the internal battle side), then its formation or the vote page, and the online
     * members of the old and the new faction refresh their catalogs. An assignment that changed
     * nothing pushes nothing to anyone.
     *
     * @return whether the target's faction or formation changed
     */
    public static boolean finishAdminAssignment(ServerPlayer target, FormationSeatState before) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(before, "before");
        FormationSeatState after = FormationSeatState.of(target);
        if (!before.seatChanged(after)) {
            return false;
        }
        String receipt = FormationService.get(target)
                .map(service -> service.assignmentReceipt(target.getUUID()))
                .orElse(FormationService.adminAssignmentReceipt("", ""));
        sendResult(target, ActionResult.ok(receipt));
        target.displayClientMessage(Component.literal(receipt), false);
        if (hasFormation(target)) {
            sendFormationApplied(target);
        } else {
            deliverSafely(target, recipient -> sendSnapshotToPlayer(recipient, true));
        }
        refreshFactionMembers(target, before.affectedFactions(after));
        return true;
    }

    private static boolean hasFormation(ServerPlayer player) {
        return FormationService.get(player)
                .flatMap(service -> service.selectedFormation(player.getUUID())).isPresent();
    }

    /** Catalog refresh for the other online members of {@code factions}, one player at a time. */
    private static void refreshFactionMembers(ServerPlayer actor, Set<Faction> factions) {
        if (factions.isEmpty()) {
            return;
        }
        BattleService battle = BattleService.get(actor).orElse(null);
        if (battle == null) {
            return;
        }
        UUID actorId = actor.getUUID();
        forEachRecipient(actor.server.getPlayerList().getPlayers().stream()
                        .filter(other -> !other.getUUID().equals(actorId))
                        .filter(other -> battle.factionOf(other.getUUID())
                                .filter(factions::contains).isPresent())
                        .toList(),
                other -> sendSnapshotToPlayer(other, false));
    }

    private static synchronized void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("FormationNetwork.init() has not been called");
        }
    }
}
