package com.wok.infantry.battle.tickets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TicketNetwork {
    public record Snapshot(int blue, int red, boolean visible) {
        public Snapshot {
            if (blue < 0 || red < 0 || blue > 1_000_000 || red > 1_000_000)
                throw new IllegalArgumentException("Invalid manpower values");
        }
        public void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(blue); buffer.writeVarInt(red); buffer.writeBoolean(visible); }
        public static Snapshot decode(FriendlyByteBuf buffer) {
            Snapshot snapshot = new Snapshot(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean());
            if (buffer.readableBytes() != 0) throw new IllegalArgumentException("Trailing manpower data");
            return snapshot;
        }
    }
    private static volatile Snapshot clientSnapshot = new Snapshot(0, 0, false);
    private record SupplyHint(String text) {
        private static SupplyHint decode(FriendlyByteBuf buffer) {
            SupplyHint hint = new SupplyHint(buffer.readUtf(256));
            if (buffer.readableBytes() != 0) throw new IllegalArgumentException("Trailing supply hint data");
            return hint;
        }
    }
    private static volatile String supplyHint = "";
    private static volatile long supplyHintExpiry;
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath("wok_infantry", "tickets"))
            .networkProtocolVersion(() -> "1").clientAcceptedVersions("1"::equals)
            .serverAcceptedVersions("1"::equals).simpleChannel();
    public static void init() {
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Snapshot::encode).decoder(Snapshot::decode)
                .consumerMainThread((snapshot, context) -> clientSnapshot = snapshot).add();
        CHANNEL.messageBuilder(SupplyHint.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((hint, buffer) -> buffer.writeUtf(hint.text(), 256)).decoder(SupplyHint::decode)
                .consumerMainThread((hint, context) -> {
                    supplyHint = hint.text();
                    supplyHintExpiry = System.nanoTime() + 5_000_000_000L;
                }).add();
    }
    public static Snapshot snapshot() { return clientSnapshot; }
    public static String supplyHint() { return System.nanoTime() < supplyHintExpiry ? supplyHint : ""; }
    public static void clearClient() {
        clientSnapshot = new Snapshot(0, 0, false); supplyHint = ""; supplyHintExpiry = 0;
    }
    public static void sendSupplyHint(ServerPlayer player, String text) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SupplyHint(text));
    }
    public static void send(ServerPlayer player, Snapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot);
    }
    private TicketNetwork() {}
}
