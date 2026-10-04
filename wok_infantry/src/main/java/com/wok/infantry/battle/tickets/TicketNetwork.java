package com.wok.infantry.battle.tickets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TicketNetwork {
    /** Manpower channel protocol: 2 adds the full-bar value ({@link Snapshot#max}). */
    public static final String PROTOCOL_VERSION = "2";
    public static final int MAX_TICKETS = 1_000_000;
    private static final long SUPPLY_HINT_NANOS = 5_000_000_000L;

    /**
     * Manpower of both sides as the viewer sees it.
     *
     * @param max     full manpower bar: the server's current {@code tickets.initial} setting, or a
     *                side's value when that is higher ({@code TicketService.barMaximum}); it is
     *                not stored per round, so changing the setting mid-round rescales the bar
     * @param visible the viewer has a faction, so the battle strip is shown
     */
    public record Snapshot(int blue, int red, int max, boolean visible) {
        public Snapshot {
            if (blue < 0 || red < 0 || blue > MAX_TICKETS || red > MAX_TICKETS)
                throw new IllegalArgumentException("Invalid manpower values");
            if (max < 1 || max > MAX_TICKETS || max < blue || max < red)
                throw new IllegalArgumentException("Invalid manpower maximum");
        }

        /** Protocol-1 shape: the bar maximum is the larger side (at least 1). */
        public Snapshot(int blue, int red, boolean visible) {
            this(blue, red, Math.max(1, Math.max(blue, red)), visible);
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(blue); buffer.writeVarInt(red); buffer.writeVarInt(max);
            buffer.writeBoolean(visible);
        }
        public static Snapshot decode(FriendlyByteBuf buffer) {
            Snapshot snapshot = new Snapshot(buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readBoolean());
            if (buffer.readableBytes() != 0) throw new IllegalArgumentException("Trailing manpower data");
            return snapshot;
        }

        /** Remaining share of {@code value} on this snapshot's bar, 0–1. */
        public float ratio(int value) {
            return Math.max(0.0F, Math.min(1.0F, value / (float) max));
        }
    }
    private static final Snapshot EMPTY = new Snapshot(0, 0, 1, false);
    private static volatile Snapshot clientSnapshot = EMPTY;
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
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals).simpleChannel();
    public static void init() {
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Snapshot::encode).decoder(Snapshot::decode)
                .consumerMainThread((snapshot, context) -> acceptClient(snapshot)).add();
        CHANNEL.messageBuilder(SupplyHint.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((hint, buffer) -> buffer.writeUtf(hint.text(), 256)).decoder(SupplyHint::decode)
                .consumerMainThread((hint, context) -> showSupplyHint(hint.text())).add();
    }
    public static Snapshot snapshot() { return clientSnapshot; }
    public static String supplyHint() { return System.nanoTime() < supplyHintExpiry ? supplyHint : ""; }

    /** Client side: stores a received manpower snapshot (also the UI acceptance hook). */
    public static void acceptClient(Snapshot snapshot) {
        clientSnapshot = snapshot == null ? EMPTY : snapshot;
    }

    /** Client side: shows a base-supply notice for five seconds (also the UI acceptance hook). */
    public static void showSupplyHint(String text) {
        supplyHint = text == null ? "" : text;
        supplyHintExpiry = supplyHint.isEmpty() ? 0L : System.nanoTime() + SUPPLY_HINT_NANOS;
    }

    public static void clearClient() {
        clientSnapshot = EMPTY; supplyHint = ""; supplyHintExpiry = 0;
    }
    public static void sendSupplyHint(ServerPlayer player, String text) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SupplyHint(text));
    }
    public static void send(ServerPlayer player, Snapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot);
    }
    private TicketNetwork() {}
}
