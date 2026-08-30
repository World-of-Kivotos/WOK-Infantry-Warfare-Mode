package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.BattleService;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import com.wok.infantry.network.battle.BattleOpenTarget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/** Requests a fresh filtered snapshot and asks the client to open a battle screen. */
public record BattleOpenPacket(BattleOpenTarget openTarget) {
    public BattleOpenPacket {
        Objects.requireNonNull(openTarget, "openTarget");
        if (openTarget == BattleOpenTarget.NONE) {
            throw new IllegalArgumentException("Use BattleSnapshotRequestPacket for a silent refresh");
        }
    }

    public static void encode(BattleOpenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.openTarget.name().toLowerCase(Locale.ROOT),
                BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
    }

    public static BattleOpenPacket decode(FriendlyByteBuf buffer) {
        String encoded = buffer.readUtf(BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        BattleOpenPacket decoded;
        try {
            decoded = new BattleOpenPacket(
                    BattleOpenTarget.valueOf(encoded.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid battle open target: " + encoded, exception);
        }
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing battle-open data");
        }
        return decoded;
    }

    public static void handle(BattleOpenPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.OPEN_UI)) {
            return;
        }
        BattleService service = BattleServerPacketSupport.serviceFor(sender);
        if (service != null && BattleServerPacketSupport.ensurePlayer(service, sender)) {
            BattleNetwork.sendSnapshotToPlayer(service, sender, packet.openTarget);
        }
    }
}
