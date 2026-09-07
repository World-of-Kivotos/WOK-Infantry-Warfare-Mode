package com.wok.infantry.network.serverbound;

import com.wok.infantry.formation.FormationClassEditAction;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AdminClassPacket(FormationClassEditAction action,
                               String factionId, String formationId,
                               String classId, String displayName, int squadLimit) {
    public static void encode(AdminClassPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.factionId, 64);
        buffer.writeUtf(packet.formationId, 64);
        buffer.writeUtf(packet.classId, 64);
        buffer.writeUtf(packet.displayName, 40);
        buffer.writeVarInt(packet.squadLimit);
    }

    public static AdminClassPacket decode(FriendlyByteBuf buffer) {
        AdminClassPacket packet = new AdminClassPacket(
                buffer.readEnum(FormationClassEditAction.class),
                buffer.readUtf(64), buffer.readUtf(64), buffer.readUtf(64),
                buffer.readUtf(40), buffer.readVarInt());
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("职业设置数据包包含多余数据");
        }
        return packet;
    }

    public static void handle(AdminClassPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender != null && ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.ADMIN_MUTATION)) {
            LoadoutService.get(sender).ifPresent(service ->
                    service.editFormationClass(sender, packet.factionId, packet.formationId,
                            packet.classId, packet.displayName, packet.squadLimit,
                            packet.action));
        }
    }
}
