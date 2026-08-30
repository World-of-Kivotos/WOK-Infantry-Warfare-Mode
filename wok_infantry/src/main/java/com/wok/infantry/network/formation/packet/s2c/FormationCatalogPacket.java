package com.wok.infantry.network.formation.packet.s2c;

import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.network.formation.FormationSelectionCodec;
import com.wok.infantry.network.formation.client.FormationClientPacketBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

public record FormationCatalogPacket(FormationSelectionSnapshot snapshot, boolean openScreen) {
    public FormationCatalogPacket {
        Objects.requireNonNull(snapshot, "snapshot");
    }

    public static void encode(FormationCatalogPacket packet, FriendlyByteBuf buffer) {
        FormationSelectionCodec.encode(buffer, packet.snapshot);
        buffer.writeBoolean(packet.openScreen);
    }

    public static FormationCatalogPacket decode(FriendlyByteBuf buffer) {
        FormationSelectionSnapshot snapshot = FormationSelectionCodec.decode(buffer);
        boolean open = buffer.readBoolean();
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing formation catalog data");
        }
        return new FormationCatalogPacket(snapshot, open);
    }

    public static void handle(FormationCatalogPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                FormationClientPacketBridge.apply(packet.snapshot, packet.openScreen));
    }
}
