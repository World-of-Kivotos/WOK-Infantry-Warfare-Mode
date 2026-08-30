package com.wok.infantry.network.stamina;

import com.wok.infantry.stamina.StaminaSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StaminaSyncPacket(StaminaSnapshot snapshot) {
    public static void encode(StaminaSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.snapshot.arms());
        buffer.writeFloat(packet.snapshot.legs());
        buffer.writeBoolean(packet.snapshot.enabled());
    }

    public static StaminaSyncPacket decode(FriendlyByteBuf buffer) {
        StaminaSnapshot snapshot = new StaminaSnapshot(buffer.readFloat(), buffer.readFloat(),
                buffer.readBoolean());
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing stamina sync data");
        }
        return new StaminaSyncPacket(snapshot);
    }

    public static void handle(StaminaSyncPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> StaminaClientPacketBridge.apply(packet.snapshot));
    }
}
