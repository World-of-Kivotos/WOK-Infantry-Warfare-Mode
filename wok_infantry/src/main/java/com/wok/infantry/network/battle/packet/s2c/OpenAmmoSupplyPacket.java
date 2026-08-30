package com.wok.infantry.network.battle.packet.s2c;

import com.wok.infantry.ammo.AmmoSupplyView;
import com.wok.infantry.network.battle.client.AmmoSupplyClientPacketBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record OpenAmmoSupplyPacket(AmmoSupplyView view) {
    private static final int MAX_GUN_OPTIONS = 37;
    private static final int MAX_VEHICLE_OPTIONS = 128;
    private static final int MAX_AMMO_ID_LENGTH = 128;
    private static final int MAX_WEAPON_KEY_LENGTH = 128;

    public static void encode(OpenAmmoSupplyPacket packet, FriendlyByteBuf buffer) {
        AmmoSupplyView view = packet.view;
        buffer.writeByte(view.target().kind().ordinal());
        buffer.writeLong(view.target().value());
        buffer.writeVarInt(view.capacityPoints());
        buffer.writeVarInt(view.remainingPoints());
        buffer.writeVarInt(view.guns().size());
        for (AmmoSupplyView.GunOption gun : view.guns()) {
            buffer.writeByte(gun.inventorySlot());
            buffer.writeComponent(gun.gunName());
            buffer.writeComponent(gun.ammunitionName());
            buffer.writeUtf(gun.ammunitionId(), MAX_AMMO_ID_LENGTH);
            buffer.writeVarInt(gun.pointsPerRound());
            buffer.writeVarInt(gun.currentRounds());
            buffer.writeVarInt(gun.reserveLimit());
            buffer.writeVarInt(gun.receivableRounds());
        }
        buffer.writeVarInt(view.vehicleAmmunition().size());
        for (AmmoSupplyView.VehicleAmmoOption ammunition : view.vehicleAmmunition()) {
            buffer.writeVarInt(ammunition.vehicleEntityId());
            buffer.writeUtf(ammunition.weaponKey(), MAX_WEAPON_KEY_LENGTH);
            buffer.writeByte(ammunition.consumerIndex());
            buffer.writeComponent(ammunition.vehicleName());
            buffer.writeComponent(ammunition.weaponName());
            buffer.writeComponent(ammunition.ammunitionName());
            buffer.writeVarInt(ammunition.pointsPerRound());
            buffer.writeVarInt(ammunition.currentRounds());
            buffer.writeVarInt(ammunition.roundStep());
            buffer.writeVarInt(ammunition.maxRounds());
        }
    }

    public static OpenAmmoSupplyPacket decode(FriendlyByteBuf buffer) {
        int kindOrdinal = buffer.readUnsignedByte();
        AmmoSupplyView.TargetKind[] kinds = AmmoSupplyView.TargetKind.values();
        if (kindOrdinal >= kinds.length) {
            throw new IllegalArgumentException("Invalid ammo supply target kind");
        }
        AmmoSupplyView.Target target = new AmmoSupplyView.Target(kinds[kindOrdinal],
                buffer.readLong());
        int capacity = readBounded(buffer, 0, 100_000, "capacity");
        int remaining = readBounded(buffer, 0, capacity, "remaining points");
        int count = readBounded(buffer, 0, MAX_GUN_OPTIONS, "gun option count");
        List<AmmoSupplyView.GunOption> guns = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int slot = buffer.readUnsignedByte();
            Component gunName = buffer.readComponent();
            Component ammoName = buffer.readComponent();
            String ammoId = buffer.readUtf(MAX_AMMO_ID_LENGTH);
            int cost = readBounded(buffer, 1, 10_000, "round cost");
            int current = readBounded(buffer, 0, 1_000_000, "current rounds");
            int reserveLimit = readBounded(buffer, 1, 4_096, "gun reserve limit");
            int receivable = readBounded(buffer, 0, 1_000_000, "receivable rounds");
            guns.add(new AmmoSupplyView.GunOption(slot, gunName, ammoName, ammoId, cost,
                    current, reserveLimit, receivable));
        }
        int vehicleCount = readBounded(buffer, 0, MAX_VEHICLE_OPTIONS,
                "vehicle ammunition option count");
        List<AmmoSupplyView.VehicleAmmoOption> vehicleAmmunition =
                new ArrayList<>(vehicleCount);
        for (int i = 0; i < vehicleCount; i++) {
            int vehicleEntityId = readBounded(buffer, 0, Integer.MAX_VALUE,
                    "vehicle entity id");
            String weaponKey = buffer.readUtf(MAX_WEAPON_KEY_LENGTH);
            int consumerIndex = buffer.readUnsignedByte();
            Component vehicleName = buffer.readComponent();
            Component weaponName = buffer.readComponent();
            Component ammunitionName = buffer.readComponent();
            int cost = readBounded(buffer, 1, 10_000, "vehicle round cost");
            int current = readBounded(buffer, 0, Integer.MAX_VALUE,
                    "vehicle current rounds");
            int roundStep = readBounded(buffer, 1, 10_000, "vehicle round step");
            int maxRounds = readBounded(buffer, 0, 10_000, "vehicle max rounds");
            vehicleAmmunition.add(new AmmoSupplyView.VehicleAmmoOption(vehicleEntityId,
                    weaponKey, consumerIndex, vehicleName, weaponName, ammunitionName,
                    cost, current, roundStep, maxRounds));
        }
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing ammo supply data");
        }
        return new OpenAmmoSupplyPacket(new AmmoSupplyView(target, capacity, remaining,
                guns, vehicleAmmunition));
    }

    public static void handle(OpenAmmoSupplyPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AmmoSupplyClientPacketBridge.open(packet.view));
    }

    private static int readBounded(FriendlyByteBuf buffer, int min, int max, String field) {
        int value = buffer.readVarInt();
        if (value < min || value > max) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
        return value;
    }
}
