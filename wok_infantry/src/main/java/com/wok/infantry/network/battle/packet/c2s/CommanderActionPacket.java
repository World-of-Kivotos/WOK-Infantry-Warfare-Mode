package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Server-authoritative commander resignation and transfer intents from the squad UI. */
public record CommanderActionPacket(Action action, UUID targetPlayerId) {
    public CommanderActionPacket {
        Objects.requireNonNull(action, "action");
        if (action == Action.TRANSFER) {
            Objects.requireNonNull(targetPlayerId, "targetPlayerId");
        } else if (targetPlayerId != null) {
            throw new IllegalArgumentException("Resign does not accept a target player");
        }
    }

    public static CommanderActionPacket resign() {
        return new CommanderActionPacket(Action.RESIGN, null);
    }

    public static CommanderActionPacket transfer(UUID targetPlayerId) {
        return new CommanderActionPacket(Action.TRANSFER,
                Objects.requireNonNull(targetPlayerId, "targetPlayerId"));
    }

    public static void encode(CommanderActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action.name().toLowerCase(Locale.ROOT),
                BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        if (packet.action == Action.TRANSFER) {
            buffer.writeUUID(packet.targetPlayerId);
        }
    }

    public static CommanderActionPacket decode(FriendlyByteBuf buffer) {
        String encodedAction = buffer.readUtf(BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        Action action;
        try {
            action = Action.valueOf(encodedAction.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid commander action: " + encodedAction,
                    exception);
        }
        CommanderActionPacket packet = action == Action.TRANSFER
                ? transfer(buffer.readUUID()) : resign();
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing commander action data");
        }
        return packet;
    }

    public static void handle(CommanderActionPacket packet,
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
            case RESIGN -> service.resignCommander(sender);
            case TRANSFER -> service.transferCommander(sender, packet.targetPlayerId);
        };
        BattleServerPacketSupport.finish(sender, service, result);
    }

    public enum Action {
        RESIGN,
        TRANSFER
    }
}
