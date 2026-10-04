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

/**
 * Formation catalog for one viewer. {@code openScreen} asks the client to show the vote page
 * while a choice is pending; {@code lockNotice} (formation protocol 5) tells it that the
 * faction's shared formation was just applied to this viewer (lock, late join, administrator
 * assignment), so it records the lock for the HUD and opens the deployment page unless another
 * mod's screen is open.
 */
public record FormationCatalogPacket(FormationSelectionSnapshot snapshot, boolean openScreen,
                                     boolean lockNotice) {
    public FormationCatalogPacket(FormationSelectionSnapshot snapshot, boolean openScreen) {
        this(snapshot, openScreen, false);
    }

    public FormationCatalogPacket {
        Objects.requireNonNull(snapshot, "snapshot");
    }

    public static void encode(FormationCatalogPacket packet, FriendlyByteBuf buffer) {
        FormationSelectionCodec.encode(buffer, packet.snapshot);
        buffer.writeBoolean(packet.openScreen);
        buffer.writeBoolean(packet.lockNotice);
    }

    public static FormationCatalogPacket decode(FriendlyByteBuf buffer) {
        FormationSelectionSnapshot snapshot = FormationSelectionCodec.decode(buffer);
        boolean open = buffer.readBoolean();
        boolean lockNotice = buffer.readBoolean();
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing formation catalog data");
        }
        return new FormationCatalogPacket(snapshot, open, lockNotice);
    }

    public static void handle(FormationCatalogPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                FormationClientPacketBridge.apply(packet.snapshot, packet.openScreen,
                        packet.lockNotice));
    }
}
