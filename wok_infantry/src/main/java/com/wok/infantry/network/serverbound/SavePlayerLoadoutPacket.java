package com.wok.infantry.network.serverbound;

import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SavePlayerLoadoutPacket(String classId,
                                      Map<String, String> selections,
                                      boolean apply) {
    private static final int MAX_SELECTIONS = 16;

    public static void encode(SavePlayerLoadoutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.classId, 64);
        buffer.writeVarInt(packet.selections.size());
        packet.selections.forEach((slot, entry) -> {
            buffer.writeUtf(slot, 64);
            buffer.writeUtf(entry, 64);
        });
        buffer.writeBoolean(packet.apply);
    }

    public static SavePlayerLoadoutPacket decode(FriendlyByteBuf buffer) {
        String classId = buffer.readUtf(64);
        int encodedSize = buffer.readVarInt();
        if (encodedSize < 0 || encodedSize > 128) {
            throw new IllegalArgumentException("Invalid loadout selection count: " + encodedSize);
        }
        Map<String, String> selections = new LinkedHashMap<>();
        for (int index = 0; index < encodedSize; index++) {
            String slot = buffer.readUtf(64);
            String entry = buffer.readUtf(64);
            if (index < MAX_SELECTIONS) {
                selections.put(slot, entry);
            }
        }
        return new SavePlayerLoadoutPacket(classId, selections, buffer.readBoolean());
    }

    public static void handle(SavePlayerLoadoutPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender != null) {
            LoadoutService.get(sender).ifPresent(service ->
                    service.savePlayerSelection(sender, packet.classId,
                            packet.selections, packet.apply));
        }
    }
}
