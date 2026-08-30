package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.ammo.AmmoSupplyService;
import com.wok.infantry.network.ServerRequestLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** Explicit request for one nearby SBW vehicle weapon/ammunition/round amount. */
public record SupplyVehicleAmmoPacket(int stationEntityId, int vehicleEntityId,
                                      String weaponKey, int consumerIndex,
                                      int requestedRounds) {
    private static final int MAX_WEAPON_KEY_LENGTH = 128;
    private static final int MAX_REQUESTED_ROUNDS = 10_000;

    public SupplyVehicleAmmoPacket {
        Objects.requireNonNull(weaponKey, "weaponKey");
    }

    public static void encode(SupplyVehicleAmmoPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.stationEntityId);
        buffer.writeVarInt(packet.vehicleEntityId);
        buffer.writeUtf(packet.weaponKey, MAX_WEAPON_KEY_LENGTH);
        buffer.writeByte(packet.consumerIndex);
        buffer.writeVarInt(packet.requestedRounds);
    }

    public static SupplyVehicleAmmoPacket decode(FriendlyByteBuf buffer) {
        int stationEntityId = buffer.readVarInt();
        int vehicleEntityId = buffer.readVarInt();
        String weaponKey = buffer.readUtf(MAX_WEAPON_KEY_LENGTH);
        int consumerIndex = buffer.readUnsignedByte();
        int requestedRounds = buffer.readVarInt();
        if (stationEntityId < 0 || vehicleEntityId < 0 || weaponKey.isBlank()
                || requestedRounds < 1 || requestedRounds > MAX_REQUESTED_ROUNDS
                || buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Invalid vehicle ammunition request");
        }
        return new SupplyVehicleAmmoPacket(stationEntityId, vehicleEntityId, weaponKey,
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
        AmmoSupplyService.supplyVehicleAmmo(sender, packet.stationEntityId,
                packet.vehicleEntityId, packet.weaponKey, packet.consumerIndex,
                packet.requestedRounds);
    }
}
