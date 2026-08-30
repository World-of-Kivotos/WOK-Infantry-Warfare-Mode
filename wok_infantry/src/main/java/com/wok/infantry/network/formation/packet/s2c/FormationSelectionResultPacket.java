package com.wok.infantry.network.formation.packet.s2c;

import com.wok.infantry.network.formation.FormationSelectionCodec;
import com.wok.infantry.network.formation.client.FormationClientPacketBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

public record FormationSelectionResultPacket(boolean success, String message) {
    public FormationSelectionResultPacket {
        message = Objects.requireNonNullElse(message, "");
        if (message.length() > FormationSelectionCodec.MAX_REASON) {
            message = message.substring(0, FormationSelectionCodec.MAX_REASON);
        }
    }

    public static void encode(FormationSelectionResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.success);
        buffer.writeUtf(packet.message, FormationSelectionCodec.MAX_REASON);
    }

    public static FormationSelectionResultPacket decode(FriendlyByteBuf buffer) {
        FormationSelectionResultPacket packet = new FormationSelectionResultPacket(
                buffer.readBoolean(), buffer.readUtf(FormationSelectionCodec.MAX_REASON));
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected formation result data");
        }
        return packet;
    }

    public static void handle(FormationSelectionResultPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                FormationClientPacketBridge.feedback(packet.success, packet.message));
    }
}
