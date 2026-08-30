package com.wok.infantry.network.serverbound;

import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Requests a server-authoritative deep copy from one formation-owned profession to another. */
public record AdminClassLoadoutCopyPacket(String sourceFactionId,
                                          String sourceFormationId,
                                          String sourceClassId,
                                          String targetFactionId,
                                          String targetFormationId,
                                          String targetClassId) {
    public static void encode(AdminClassLoadoutCopyPacket packet,
                              FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourceFactionId, 64);
        buffer.writeUtf(packet.sourceFormationId, 64);
        buffer.writeUtf(packet.sourceClassId, 64);
        buffer.writeUtf(packet.targetFactionId, 64);
        buffer.writeUtf(packet.targetFormationId, 64);
        buffer.writeUtf(packet.targetClassId, 64);
    }

    public static AdminClassLoadoutCopyPacket decode(FriendlyByteBuf buffer) {
        AdminClassLoadoutCopyPacket packet = new AdminClassLoadoutCopyPacket(
                buffer.readUtf(64), buffer.readUtf(64), buffer.readUtf(64),
                buffer.readUtf(64), buffer.readUtf(64), buffer.readUtf(64));
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("兵种配装复制数据包包含多余数据");
        }
        return packet;
    }

    public static void handle(AdminClassLoadoutCopyPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender != null && ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.ADMIN_MUTATION)) {
            LoadoutService.get(sender).ifPresent(service -> service.copyClassLoadout(sender,
                    packet.sourceFactionId, packet.sourceFormationId, packet.sourceClassId,
                    packet.targetFactionId, packet.targetFormationId, packet.targetClassId));
        }
    }
}
