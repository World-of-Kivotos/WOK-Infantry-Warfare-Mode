package com.wok.infantry.network.clientbound;

import com.wok.infantry.client.ClientPacketHandler;
import com.wok.infantry.configtransfer.CatalogTransferResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public record CatalogTransferResultPacket(int requestId, CatalogTransferResult result) {
    public static void encode(CatalogTransferResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.requestId);
        buffer.writeBoolean(packet.result.success());
        buffer.writeUtf(packet.result.message(), 2048);
        buffer.writeUtf(packet.result.token(), 36);
        buffer.writeVarInt(packet.result.files().size());
        for (String name : packet.result.files()) buffer.writeUtf(name, 69);
        buffer.writeVarInt(packet.result.page());
        buffer.writeVarInt(packet.result.pages());
    }

    public static CatalogTransferResultPacket decode(FriendlyByteBuf buffer) {
        int id = buffer.readInt();
        boolean success = buffer.readBoolean();
        String message = buffer.readUtf(2048);
        String token = buffer.readUtf(36);
        int count = buffer.readVarInt();
        if (count < 0 || count > 8) throw new IllegalArgumentException("Too many catalog filenames");
        var names = new ArrayList<String>();
        for (int index = 0; index < count; index++) names.add(buffer.readUtf(69));
        int page = buffer.readVarInt();
        int pages = buffer.readVarInt();
        if (page < 0 || pages < 0 || buffer.readableBytes() != 0) throw new IllegalArgumentException("Invalid catalog response");
        return new CatalogTransferResultPacket(id, new CatalogTransferResult(success, message, token, names, page, pages));
    }

    public static void handle(CatalogTransferResultPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCatalogResult(packet));
    }
}
