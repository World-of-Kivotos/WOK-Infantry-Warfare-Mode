package com.wok.trauma.network;

import com.wok.trauma.WokTraumaMod;
import com.wok.trauma.item.TreatmentSelection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TraumaNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(WokTraumaMod.MOD_ID, "treatment"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();
    private static int packetId;

    public static void init() {
        CHANNEL.messageBuilder(
                        SelectTreatmentPartPacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectTreatmentPartPacket::encode)
                .decoder(SelectTreatmentPartPacket::decode)
                .consumerMainThread(SelectTreatmentPartPacket::handle)
                .add();
    }

    public static void sendToServer(TreatmentSelection selection) {
        CHANNEL.sendToServer(new SelectTreatmentPartPacket(selection.ordinal()));
    }

    private TraumaNetwork() {
    }
}
