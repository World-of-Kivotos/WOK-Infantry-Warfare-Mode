package com.wok.bodyhealth.network;

import com.wok.bodyhealth.client.ClientBodyHealthState;
import com.wok.bodyhealth.health.BodyHealthSnapshot;
import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncBodyHealthPacket(BodyHealthSnapshot snapshot) {
    public static void encode(SyncBodyHealthPacket packet, FriendlyByteBuf buffer) {
        for (float value : packet.snapshot.current()) {
            buffer.writeFloat(value);
        }
        for (float value : packet.snapshot.maximum()) {
            buffer.writeFloat(value);
        }
    }

    public static SyncBodyHealthPacket decode(FriendlyByteBuf buffer) {
        int size = BodyPart.values().length;
        float[] current = new float[size];
        float[] maximum = new float[size];
        for (int index = 0; index < size; index++) {
            current[index] = buffer.readFloat();
        }
        for (int index = 0; index < size; index++) {
            maximum[index] = buffer.readFloat();
        }
        return new SyncBodyHealthPacket(new BodyHealthSnapshot(current, maximum));
    }

    public static void handle(SyncBodyHealthPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientBodyHealthState.accept(packet.snapshot));
        contextSupplier.get().setPacketHandled(true);
    }
}
