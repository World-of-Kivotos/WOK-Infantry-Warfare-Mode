package com.wok.vehiclehealth.network;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.wok.vehiclehealth.WokVehicleHealthMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class VehicleModuleNetwork {
    private static final String PROTOCOL = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(WokVehicleHealthMod.MOD_ID, "vehicle_modules"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();
    private static int packetId;

    public static void init() {
        CHANNEL.messageBuilder(OpticsInterferencePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpticsInterferencePacket::encode)
                .decoder(OpticsInterferencePacket::decode)
                .consumerMainThread(OpticsInterferencePacket::handle)
                .add();
        CHANNEL.messageBuilder(TurretRotationPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TurretRotationPacket::encode)
                .decoder(TurretRotationPacket::decode)
                .consumerMainThread(TurretRotationPacket::handle)
                .add();
    }

    public static void sendOpticsInterference(VehicleEntity vehicle, int durationTicks, float strength) {
        OpticsInterferencePacket packet = new OpticsInterferencePacket(
                vehicle.getUUID(), durationTicks, strength);
        for (var passenger : vehicle.getPassengers()) {
            if (passenger instanceof ServerPlayer player) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    public static void sendTurretRotation(VehicleEntity vehicle) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> vehicle),
                new TurretRotationPacket(vehicle.getId(), vehicle.getTurretYRot(),
                        vehicle.getTurretXRot()));
    }

    private VehicleModuleNetwork() {
    }
}
