package com.wok.infantry.network.serverbound;

import com.wok.infantry.loadout.LoadoutInventoryTarget;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** Bounded administrator mutations for one profession's ordered, dynamic equipment slots. */
public record AdminSlotPacket(Action action, String classId, String slotId,
                              String displayName, LoadoutInventoryTarget target,
                              boolean required) {
    public enum Action { CREATE, UPDATE, MOVE_LEFT, MOVE_RIGHT, DELETE }

    public AdminSlotPacket {
        Objects.requireNonNull(action, "action");
        classId = Objects.requireNonNullElse(classId, "");
        slotId = Objects.requireNonNullElse(slotId, "");
        displayName = Objects.requireNonNullElse(displayName, "");
        target = Objects.requireNonNullElse(target, LoadoutInventoryTarget.HOTBAR_1);
    }

    public static void encode(AdminSlotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.classId, 64);
        buffer.writeUtf(packet.slotId, 64);
        buffer.writeUtf(packet.displayName, 40);
        buffer.writeEnum(packet.target);
        buffer.writeBoolean(packet.required);
    }

    public static AdminSlotPacket decode(FriendlyByteBuf buffer) {
        return new AdminSlotPacket(buffer.readEnum(Action.class), buffer.readUtf(64),
                buffer.readUtf(64), buffer.readUtf(40),
                buffer.readEnum(LoadoutInventoryTarget.class), buffer.readBoolean());
    }

    public static void handle(AdminSlotPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.ADMIN_MUTATION)) {
            return;
        }
        LoadoutService.get(sender).ifPresent(service -> service.editSlot(sender,
                packet.classId, packet.slotId, packet.displayName, packet.target,
                packet.required, packet.action));
    }
}
