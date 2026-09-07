package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A compact intent packet. Faction, current squad, leader status and all ownership are resolved
 * again by {@link BattleService}; the client only names the desired squad or target player.
 */
public record SquadActionPacket(Action action, SquadCallsign callsign, UUID targetPlayerId) {
    public SquadActionPacket {
        Objects.requireNonNull(action, "action");
        switch (action) {
            case CREATE, JOIN -> Objects.requireNonNull(callsign, "callsign");
            case PROMOTE, KICK -> Objects.requireNonNull(targetPlayerId, "targetPlayerId");
            case LEAVE, DISBAND -> {
            }
        }
    }

    public static SquadActionPacket create(SquadCallsign callsign) {
        return new SquadActionPacket(Action.CREATE, callsign, null);
    }

    public static SquadActionPacket join(SquadCallsign callsign) {
        return new SquadActionPacket(Action.JOIN, callsign, null);
    }

    public static SquadActionPacket leave() {
        return new SquadActionPacket(Action.LEAVE, null, null);
    }

    public static SquadActionPacket disband() {
        return new SquadActionPacket(Action.DISBAND, null, null);
    }

    public static SquadActionPacket promote(UUID playerId) {
        return new SquadActionPacket(Action.PROMOTE, null, playerId);
    }

    public static SquadActionPacket kick(UUID playerId) {
        return new SquadActionPacket(Action.KICK, null, playerId);
    }

    public static void encode(SquadActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action.name().toLowerCase(Locale.ROOT),
                BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        switch (packet.action) {
            case CREATE, JOIN -> buffer.writeUtf(packet.callsign.id(),
                    BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
            case PROMOTE, KICK -> buffer.writeUUID(packet.targetPlayerId);
            case LEAVE, DISBAND -> {
            }
        }
    }

    public static SquadActionPacket decode(FriendlyByteBuf buffer) {
        String encodedAction = buffer.readUtf(BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        Action action;
        try {
            action = Action.valueOf(encodedAction.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid squad action: " + encodedAction, exception);
        }
        SquadActionPacket decoded = switch (action) {
            case CREATE, JOIN -> {
                String encodedCallsign = buffer.readUtf(BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
                SquadCallsign callsign = SquadCallsign.byId(encodedCallsign)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Invalid squad callsign: " + encodedCallsign));
                yield new SquadActionPacket(action, callsign, null);
            }
            case LEAVE -> leave();
            case DISBAND -> disband();
            case PROMOTE, KICK -> new SquadActionPacket(action, null, buffer.readUUID());
        };
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing squad action data");
        }
        return decoded;
    }

    public static void handle(SquadActionPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.SQUAD_ACTION)) {
            return;
        }
        BattleService service = BattleServerPacketSupport.serviceFor(sender);
        if (service == null || !BattleServerPacketSupport.ensurePlayer(service, sender)) {
            return;
        }
        ActionResult result = switch (packet.action) {
            case CREATE -> service.createSquad(sender, packet.callsign);
            case JOIN -> service.joinSquad(sender, packet.callsign);
            case LEAVE -> service.leaveSquad(sender);
            case DISBAND -> service.disbandSquad(sender);
            case PROMOTE -> service.transferLeadership(sender, packet.targetPlayerId);
            case KICK -> service.kickMember(sender, packet.targetPlayerId);
        };
        BattleServerPacketSupport.finish(sender, service, result);
    }

    public enum Action {
        CREATE,
        JOIN,
        LEAVE,
        DISBAND,
        PROMOTE,
        KICK
    }
}
