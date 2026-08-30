package com.wok.infantry.network.stamina;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.stamina.StaminaSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** A small independent channel so stamina revisions do not churn the battle protocol. */
public final class StaminaNetwork {
    public static final ResourceLocation CHANNEL_NAME =
            ResourceLocation.fromNamespaceAndPath(WokInfantryMod.MOD_ID, "stamina");
    public static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_NAME)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();
    private static boolean initialized;

    private StaminaNetwork() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        CHANNEL.messageBuilder(StaminaSyncPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StaminaSyncPacket::encode)
                .decoder(StaminaSyncPacket::decode)
                .consumerMainThread(StaminaSyncPacket::handle)
                .add();
        initialized = true;
    }

    public static void send(ServerPlayer player, StaminaSnapshot snapshot) {
        if (initialized) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new StaminaSyncPacket(snapshot));
        }
    }
}
