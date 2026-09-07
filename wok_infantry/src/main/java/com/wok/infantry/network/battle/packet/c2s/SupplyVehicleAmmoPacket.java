package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.ammo.AmmoSupplyService;
import com.wok.infantry.ammo.AmmoSupplyView;
import com.wok.infantry.network.ServerRequestLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** Explicit request for one nearby SBW vehicle weapon/ammunition/round amount. */
public record SupplyVehicleAmmoPacket(AmmoSupplyView.Target stationTarget,
                                      int vehicleEntityId,
                                      String weaponKey, int consumerIndex,
                                      int requestedRounds) {
    private static final int MAX_WEAPON_KEY_LENGTH = 128;
    private static final int MAX_REQUESTED_ROUNDS = 10_000;

    public SupplyVehicleAmmoPacket {
        Objects.requireNonNull(stationTarget, "stationTarget");
        Objects.requireNonNull(weaponKey, "weaponKey");
    }

    public static void encode(SupplyVehicleAmmoPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.stationTarget.kind().ordinal());
        buffer.writeLong(packet.stationTarget.value());
        buffer.writeVarInt(packet.vehicleEntityId);
        buffer.writeUtf(packet.weaponKey, MAX_WEAPON_KEY_LENGTH);
        buffer.writeByte(packet.consumerIndex);
        buffer.writeVarInt(packet.requestedRounds);
    }

    public static SupplyVehicleAmmoPacket decode(FriendlyByteBuf buffer) {
        int kindOrdinal = buffer.readUnsignedByte();
        AmmoSupplyView.TargetKind[] kinds = AmmoSupplyView.TargetKind.values();
        if (kindOrdinal >= kinds.length) {
            throw new IllegalArgumentException("Invalid large supply target kind");
        }
        AmmoSupplyView.Target stationTarget = new AmmoSupplyView.Target(
                kinds[kindOrdinal], buffer.readLong());
        int vehicleEntityId = buffer.readVarInt();
        String weaponKey = buffer.readUtf(MAX_WEAPON_KEY_LENGTH);
        int consumerIndex = buffer.readUnsignedByte();
        int requestedRounds = buffer.readVarInt();
        if (!stationTarget.isLarge() || vehicleEntityId < 0 || weaponKey.isBlank()
                || requestedRounds < 1 || requestedRounds > MAX_REQUESTED_ROUNDS
                || buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Invalid vehicle ammunition request");
        }
        return new SupplyVehicleAmmoPacket(stationTarget, vehicleEntityId, weaponKey,
                consumerIndex, requestedRounds);
    }

    public static void handle(SupplyVehicleAmmoPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || sender.getServer() == null
                || !sender.getServer().isSameThread()
                || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.DEPLOYMENT_ACTION)) {
            return;
        }
        AmmoSupplyService.supplyVehicleAmmo(sender, packet.stationTarget,
                packet.vehicleEntityId, packet.weaponKey, packet.consumerIndex,
                packet.requestedRounds);
    }
}
