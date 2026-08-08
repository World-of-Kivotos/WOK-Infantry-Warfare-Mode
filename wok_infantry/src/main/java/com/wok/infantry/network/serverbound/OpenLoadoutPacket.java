package com.wok.infantry.network.serverbound;

import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenLoadoutPacket(boolean administrator) {
    public static void encode(OpenLoadoutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.administrator);
    }

    public static OpenLoadoutPacket decode(FriendlyByteBuf buffer) {
        return new OpenLoadoutPacket(buffer.readBoolean());
    }

    public static void handle(OpenLoadoutPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null) {
            return;
        }
        LoadoutService.get(sender).ifPresent(service -> {
            if (packet.administrator) {
                service.openAdminScreen(sender);
            } else {
                service.openPlayerScreen(sender);
            }
        });
    }
}
