package com.wok.vehiclehealth.network;

import com.wok.vehiclehealth.client.ClientOpticsInterference;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record OpticsInterferencePacket(UUID vehicleId, int durationTicks, float strength) {
    public static void encode(OpticsInterferencePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.vehicleId);
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeFloat(packet.strength);
    }

    public static OpticsInterferencePacket decode(FriendlyByteBuf buffer) {
        return new OpticsInterferencePacket(
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readFloat());
    }

    public static void handle(OpticsInterferencePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientOpticsInterference.accept(
                        packet.vehicleId,
                        packet.durationTicks,
                        packet.strength));
        contextSupplier.get().setPacketHandled(true);
    }
}
