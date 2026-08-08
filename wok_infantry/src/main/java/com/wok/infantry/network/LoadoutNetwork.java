package com.wok.infantry.network;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.network.clientbound.LoadoutSnapshotPacket;
import com.wok.infantry.network.serverbound.AdminClassPacket;
import com.wok.infantry.network.serverbound.AdminEntryPacket;
import com.wok.infantry.network.serverbound.OpenLoadoutPacket;
import com.wok.infantry.network.serverbound.SavePlayerLoadoutPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class LoadoutNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(WokInfantryMod.MOD_ID, "loadouts"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();
    private static int packetId;

    private LoadoutNetwork() {
    }

    public static void init() {
        CHANNEL.messageBuilder(OpenLoadoutPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenLoadoutPacket::encode)
                .decoder(OpenLoadoutPacket::decode)
                .consumerMainThread(OpenLoadoutPacket::handle)
                .add();
        CHANNEL.messageBuilder(SavePlayerLoadoutPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SavePlayerLoadoutPacket::encode)
                .decoder(SavePlayerLoadoutPacket::decode)
                .consumerMainThread(SavePlayerLoadoutPacket::handle)
                .add();
        CHANNEL.messageBuilder(AdminEntryPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AdminEntryPacket::encode)
                .decoder(AdminEntryPacket::decode)
                .consumerMainThread(AdminEntryPacket::handle)
                .add();
        CHANNEL.messageBuilder(AdminClassPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AdminClassPacket::encode)
                .decoder(AdminClassPacket::decode)
                .consumerMainThread(AdminClassPacket::handle)
                .add();
        CHANNEL.messageBuilder(LoadoutSnapshotPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LoadoutSnapshotPacket::encode)
                .decoder(LoadoutSnapshotPacket::decode)
                .consumerMainThread(LoadoutSnapshotPacket::handle)
                .add();
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
