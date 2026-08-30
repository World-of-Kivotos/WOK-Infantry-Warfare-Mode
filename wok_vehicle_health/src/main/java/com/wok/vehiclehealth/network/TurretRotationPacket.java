package com.wok.vehiclehealth.network;

import com.wok.vehiclehealth.client.ClientTurretAuthority;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-authoritative damaged-turret orientation used by both the model and gunner camera. */
public record TurretRotationPacket(int entityId, float yaw, float pitch) {
    public static void encode(TurretRotationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeFloat(packet.yaw);
        buffer.writeFloat(packet.pitch);
    }

    public static TurretRotationPacket decode(FriendlyByteBuf buffer) {
        return new TurretRotationPacket(buffer.readVarInt(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(TurretRotationPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> applyClient(packet));
        contextSupplier.get().setPacketHandled(true);
    }

    private static void applyClient(TurretRotationPacket packet) {
        ClientTurretAuthority.accept(packet.entityId, packet.yaw, packet.pitch);
    }
}
