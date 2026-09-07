package com.wok.infantry.network.serverbound;

import com.wok.infantry.formation.FormationLoadoutEditAction;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Administrator intent for one formation's per-slot loadout allow-list. */
public record AdminFormationLoadoutPacket(FormationLoadoutEditAction action,
                                          String factionId,
                                          String formationId,
                                          String classId,
                                          String slotId,
                                          String entryId) {
    public static void encode(AdminFormationLoadoutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.factionId, 64);
        buffer.writeUtf(packet.formationId, 64);
        buffer.writeUtf(packet.classId, 64);
        buffer.writeUtf(packet.slotId, 64);
        buffer.writeUtf(packet.entryId, 64);
    }

    public static AdminFormationLoadoutPacket decode(FriendlyByteBuf buffer) {
        return new AdminFormationLoadoutPacket(buffer.readEnum(FormationLoadoutEditAction.class),
                buffer.readUtf(64), buffer.readUtf(64), buffer.readUtf(64),
                buffer.readUtf(64), buffer.readUtf(64));
    }

    public static void handle(AdminFormationLoadoutPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.ADMIN_MUTATION)) {
            return;
        }
        LoadoutService.get(sender).ifPresent(service -> service.editFormationLoadoutRule(sender,
                packet.factionId, packet.formationId, packet.classId, packet.slotId,
                packet.entryId, packet.action));
    }
}
