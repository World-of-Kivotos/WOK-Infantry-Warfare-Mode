package com.wok.infantry.network.serverbound;

import com.wok.infantry.configtransfer.CatalogTransferAction;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.clientbound.CatalogTransferResultPacket;
import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AdminCatalogTransferPacket(int requestId, CatalogTransferAction action,
                                         String name, String token, int page) {
    public static void encode(AdminCatalogTransferPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.requestId);
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.name, 69);
        buffer.writeUtf(packet.token, 36);
        buffer.writeVarInt(packet.page);
    }

    public static AdminCatalogTransferPacket decode(FriendlyByteBuf buffer) {
        var packet = new AdminCatalogTransferPacket(buffer.readInt(), buffer.readEnum(CatalogTransferAction.class),
                buffer.readUtf(69), buffer.readUtf(36), buffer.readVarInt());
        if (packet.page < 0 || buffer.readableBytes() != 0) throw new IllegalArgumentException("Invalid catalog request");
        return packet;
    }

    public static void handle(AdminCatalogTransferPacket packet, Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        if (player == null || !player.hasPermissions(LoadoutService.ADMIN_PERMISSION_LEVEL)) return;
        LoadoutService.get(player).ifPresent(service -> LoadoutNetwork.sendToPlayer(player,
                new CatalogTransferResultPacket(packet.requestId, service.catalogTransfers().execute(
                        player.createCommandSourceStack(), packet.action, packet.name, packet.token, packet.page))));
    }
}
