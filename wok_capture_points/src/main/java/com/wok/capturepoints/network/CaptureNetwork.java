package com.wok.capturepoints.network;

import com.wok.capturepoints.WokCapturePointsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CaptureNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(WokCapturePointsMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private CaptureNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(CaptureSnapshotPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CaptureSnapshotPacket::encode)
                .decoder(CaptureSnapshotPacket::decode)
                .consumerMainThread(CaptureSnapshotPacket::handle)
                .add();
    }

    public static void send(ServerPlayer player, CaptureSnapshotPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
