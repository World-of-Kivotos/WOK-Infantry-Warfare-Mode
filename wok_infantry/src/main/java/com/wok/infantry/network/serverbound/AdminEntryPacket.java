package com.wok.infantry.network.serverbound;

import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AdminEntryPacket(Action action, String classId, String slotId,
                               String factionId, String formationId,
                               String originalEntryId, String entryId,
                               String displayName, String itemId,
                               int count, int ammoReserveLimit, String snbt) {
    public enum Action { UPSERT, DELETE, CAPTURE_MAIN_HAND }

    public static AdminEntryPacket upsert(String classId, String slotId,
                                          String originalEntryId, LoadoutEntry entry) {
        return upsert(classId, slotId, originalEntryId, entry, "", "");
    }

    public static AdminEntryPacket upsert(String classId, String slotId,
                                          String originalEntryId, LoadoutEntry entry,
                                          String factionId, String formationId) {
        return new AdminEntryPacket(Action.UPSERT, classId, slotId, factionId, formationId,
                originalEntryId,
                entry.id(), entry.displayName(), entry.itemId(), entry.count(),
                entry.configuredAmmoReserveLimit(), entry.snbt());
    }

    public static AdminEntryPacket delete(String classId, String slotId, String entryId) {
        return new AdminEntryPacket(Action.DELETE, classId, slotId, "", "", entryId,
                entryId, "", "minecraft:air", 1,
                LoadoutEntry.DEFAULT_AMMO_RESERVE_LIMIT, "");
    }

    public static AdminEntryPacket captureMainHand(String classId, String slotId,
                                                   String originalEntryId,
                                                   String entryId, String displayName) {
        return captureMainHand(classId, slotId, originalEntryId, entryId, displayName,
                "", "", LoadoutEntry.DEFAULT_AMMO_RESERVE_LIMIT);
    }

    public static AdminEntryPacket captureMainHand(String classId, String slotId,
                                                   String originalEntryId,
                                                   String entryId, String displayName,
                                                   String factionId, String formationId,
                                                   int ammoReserveLimit) {
        return new AdminEntryPacket(Action.CAPTURE_MAIN_HAND, classId, slotId,
                factionId, formationId,
                originalEntryId, entryId, displayName, "minecraft:air", 1,
                ammoReserveLimit, "");
    }

    public static void encode(AdminEntryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.classId, 64);
        buffer.writeUtf(packet.slotId, 64);
        buffer.writeUtf(packet.factionId, 64);
        buffer.writeUtf(packet.formationId, 64);
        buffer.writeUtf(packet.originalEntryId, 64);
        buffer.writeUtf(packet.entryId, 64);
        buffer.writeUtf(packet.displayName, 80);
        buffer.writeUtf(packet.itemId, 256);
        buffer.writeVarInt(packet.count);
        buffer.writeVarInt(packet.ammoReserveLimit);
        buffer.writeUtf(packet.snbt, 32_767);
    }

    public static AdminEntryPacket decode(FriendlyByteBuf buffer) {
        return new AdminEntryPacket(buffer.readEnum(Action.class), buffer.readUtf(64),
                buffer.readUtf(64), buffer.readUtf(64), buffer.readUtf(64),
                buffer.readUtf(64), buffer.readUtf(64),
                buffer.readUtf(80), buffer.readUtf(256), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readUtf(32_767));
    }

    public static void handle(AdminEntryPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.ADMIN_MUTATION)) {
            return;
        }
        LoadoutService.get(sender).ifPresent(service -> {
            switch (packet.action) {
                case DELETE -> service.deleteEntry(sender, packet.classId,
                        packet.slotId, packet.entryId);
                case CAPTURE_MAIN_HAND -> service.captureMainHandEntry(sender,
                        packet.classId, packet.slotId, packet.originalEntryId,
                        packet.entryId, packet.displayName, packet.factionId,
                        packet.formationId, packet.ammoReserveLimit);
                case UPSERT -> service.upsertEntry(sender, packet.classId, packet.slotId,
                        new LoadoutEntry(packet.entryId, packet.displayName,
                                packet.itemId, packet.count, packet.snbt,
                                packet.ammoReserveLimit),
                        packet.originalEntryId, packet.factionId, packet.formationId);
            }
        });
    }
}
