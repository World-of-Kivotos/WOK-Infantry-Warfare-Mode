package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.BattleService;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleOpenTarget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Silent refresh used by already-open screens and HUD state. */
public record BattleSnapshotRequestPacket() {
    public static void encode(BattleSnapshotRequestPacket packet, FriendlyByteBuf buffer) {
    }

    public static BattleSnapshotRequestPacket decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Snapshot request packet must be empty");
        }
        return new BattleSnapshotRequestPacket();
    }

    public static void handle(BattleSnapshotRequestPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.SNAPSHOT)) {
            return;
        }
        BattleService service = BattleServerPacketSupport.serviceFor(sender);
        if (service != null && BattleServerPacketSupport.ensurePlayer(service, sender)
                && ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.SNAPSHOT_RESPONSE)) {
            BattleNetwork.sendSnapshotToPlayer(service, sender, BattleOpenTarget.NONE);
        }
    }
}
