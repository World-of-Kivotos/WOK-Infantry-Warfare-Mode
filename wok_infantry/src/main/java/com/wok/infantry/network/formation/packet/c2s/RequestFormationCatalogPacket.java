package com.wok.infantry.network.formation.packet.c2s;

import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.formation.FormationNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestFormationCatalogPacket() {
    /**
     * Whether the reply asks the client to open the vote page. Since 0.4.0-beta.1 the client opens
     * the page itself before asking (terminal key, map key, terminal tab, retry key), so the reply
     * only refreshes the catalog: a reply that arrives after the player already closed the page
     * (high latency, low TPS) must not bring it back.
     */
    public static final boolean REPLY_OPENS_SCREEN = false;

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
                ServerRequestLimiter.Kind.FORMATION_CATALOG)) {
            FormationNetwork.sendSnapshotToPlayer(sender, REPLY_OPENS_SCREEN);
        }
    }
}
