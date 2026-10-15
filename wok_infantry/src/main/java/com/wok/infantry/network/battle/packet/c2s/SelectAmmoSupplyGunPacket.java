package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.ammo.AmmoSupplyService;
import com.wok.infantry.ammo.AmmoSupplyView;
import com.wok.infantry.network.ServerRequestLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SelectAmmoSupplyGunPacket(AmmoSupplyView.Target target, int inventorySlot) {
    public static void encode(SelectAmmoSupplyGunPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.target.kind().ordinal());
        buffer.writeLong(packet.target.value());
        buffer.writeByte(packet.inventorySlot);
    }

    public static SelectAmmoSupplyGunPacket decode(FriendlyByteBuf buffer) {
        int kindOrdinal = buffer.readUnsignedByte();
        AmmoSupplyView.TargetKind[] kinds = AmmoSupplyView.TargetKind.values();
        if (kindOrdinal >= kinds.length) {
            throw new IllegalArgumentException("Invalid ammo supply target kind");
        }
        AmmoSupplyView.Target target = new AmmoSupplyView.Target(kinds[kindOrdinal],
                buffer.readLong());
        int slot = buffer.readUnsignedByte();
        if ((slot > 35 && slot != 40) || buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Invalid ammo supply selection packet");
        }
        return new SelectAmmoSupplyGunPacket(target, slot);
    }

    public static void handle(SelectAmmoSupplyGunPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || sender.getServer() == null
                || !sender.getServer().isSameThread()
                || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.DEPLOYMENT_ACTION)) {
            return;
        }
        AmmoSupplyService.selectGun(sender, packet.target, packet.inventorySlot);
    }
}
