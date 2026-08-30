package com.wok.infantry.network.formation.packet.c2s;

import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.formation.FormationNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestFormationCatalogPacket() {
    public static void encode(RequestFormationCatalogPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestFormationCatalogPacket decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected formation catalog request data");
        }
        return new RequestFormationCatalogPacket();
    }

    public static void handle(RequestFormationCatalogPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender != null && ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.OPEN_UI)) {
            FormationNetwork.sendSnapshotToPlayer(sender, true);
        }
    }
}
