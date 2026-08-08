package com.wok.infantry.network.serverbound;

import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AdminClassPacket(String classId, String displayName, boolean enabled) {
    public static void encode(AdminClassPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.classId, 64);
        buffer.writeUtf(packet.displayName, 40);
        buffer.writeBoolean(packet.enabled);
    }

    public static AdminClassPacket decode(FriendlyByteBuf buffer) {
        return new AdminClassPacket(buffer.readUtf(64), buffer.readUtf(40), buffer.readBoolean());
    }

    public static void handle(AdminClassPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender != null) {
            LoadoutService.get(sender).ifPresent(service ->
                    service.updateClass(sender, packet.classId,
                            packet.displayName, packet.enabled));
        }
    }
}
