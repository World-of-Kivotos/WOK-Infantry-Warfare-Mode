package com.wok.trauma.network;

import com.wok.trauma.compat.bodyhealth.BodyHealthCompat;
import com.wok.trauma.item.MedicalKitItem;
import com.wok.trauma.item.TreatmentSelection;
import com.wok.trauma.item.TreatmentSelectionStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SelectTreatmentPartPacket(int selectionOrdinal) {

    public static void encode(SelectTreatmentPartPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.selectionOrdinal);
    }

    public static SelectTreatmentPartPacket decode(FriendlyByteBuf buffer) {
        return new SelectTreatmentPartPacket(buffer.readVarInt());
    }

    public static void handle(
            SelectTreatmentPartPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        TreatmentSelection selection = TreatmentSelection.fromOrdinal(packet.selectionOrdinal);
        if (player != null
                && selection != null
                && BodyHealthCompat.isAvailable()
                && isHoldingMedicalKit(player)) {
            TreatmentSelectionStore.set(player, selection);
        }
        context.setPacketHandled(true);
    }

    private static boolean isHoldingMedicalKit(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof MedicalKitItem
                || player.getOffhandItem().getItem() instanceof MedicalKitItem;
    }
}
