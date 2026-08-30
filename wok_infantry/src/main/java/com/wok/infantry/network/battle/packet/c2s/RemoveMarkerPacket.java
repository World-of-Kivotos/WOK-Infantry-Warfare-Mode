package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.network.ServerRequestLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public record RemoveMarkerPacket(UUID markerId) {
    private static final int UUID_BYTES = 16;

    public RemoveMarkerPacket {
        Objects.requireNonNull(markerId, "markerId");
    }

    public static void encode(RemoveMarkerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.markerId);
    }

    public static RemoveMarkerPacket decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != UUID_BYTES) {
            throw new IllegalArgumentException("Invalid remove-marker packet length");
        }
        return new RemoveMarkerPacket(buffer.readUUID());
    }

    public static void handle(RemoveMarkerPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.MAP_MARKER)) {
            return;
        }
        BattleService service = BattleServerPacketSupport.serviceFor(sender);
        if (service == null || !BattleServerPacketSupport.ensurePlayer(service, sender)) {
            return;
        }
        ActionResult result = service.removeMarker(sender, packet.markerId);
        BattleServerPacketSupport.finish(sender, service, result);
    }
}
