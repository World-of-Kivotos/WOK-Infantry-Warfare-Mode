package com.wok.infantry.network.battle.packet.s2c;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.battle.BattleSnapshotCodec;
import com.wok.infantry.network.battle.client.BattleClientPacketBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

public record BattleSnapshotPacket(BattleSnapshot snapshot, BattleOpenTarget openTarget) {
    public BattleSnapshotPacket {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(openTarget, "openTarget");
    }

    public static void encode(BattleSnapshotPacket packet, FriendlyByteBuf buffer) {
        BattleSnapshotCodec.encode(buffer, packet.snapshot);
        buffer.writeUtf(packet.openTarget.name().toLowerCase(Locale.ROOT),
                BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
    }

    public static BattleSnapshotPacket decode(FriendlyByteBuf buffer) {
        BattleSnapshot snapshot = BattleSnapshotCodec.decode(buffer);
        String encodedTarget = buffer.readUtf(BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        BattleOpenTarget openTarget;
        try {
            openTarget = BattleOpenTarget.valueOf(encodedTarget.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid battle open target: " + encodedTarget,
                    exception);
        }
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing battle snapshot data");
        }
        return new BattleSnapshotPacket(snapshot, openTarget);
    }

    public static void handle(BattleSnapshotPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                BattleClientPacketBridge.applySnapshot(packet.snapshot, packet.openTarget));
    }
}
