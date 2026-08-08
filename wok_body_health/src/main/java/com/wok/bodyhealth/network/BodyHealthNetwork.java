package com.wok.bodyhealth.network;

import com.wok.bodyhealth.WokBodyHealthMod;
import com.wok.bodyhealth.health.BodyHealthSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BodyHealthNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(WokBodyHealthMod.MOD_ID, "body_health"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();
    private static int packetId;

    public static void init() {
        CHANNEL.messageBuilder(SyncBodyHealthPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBodyHealthPacket::encode)
                .decoder(SyncBodyHealthPacket::decode)
                .consumerMainThread(SyncBodyHealthPacket::handle)
                .add();
    }

    public static void sync(ServerPlayer player, BodyHealthSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncBodyHealthPacket(snapshot));
    }

    private BodyHealthNetwork() {
    }
}
