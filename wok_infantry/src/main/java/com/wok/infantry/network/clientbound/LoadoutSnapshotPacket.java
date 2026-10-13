package com.wok.infantry.network.clientbound;

import com.google.gson.Gson;
import com.wok.infantry.client.ClientPacketHandler;
import com.wok.infantry.loadout.LoadoutSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record LoadoutSnapshotPacket(LoadoutSnapshot snapshot, OpenTarget openTarget) {
    private static final Gson GSON = new Gson();

    public enum OpenTarget {
        NONE,
        PLAYER,
        ADMIN,
        REFRESH_ADMIN,
        REFRESH_CATALOG
    }

    public static void encode(LoadoutSnapshotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(GSON.toJson(packet.snapshot), 1_048_576);
        buffer.writeEnum(packet.openTarget);
    }

    public static LoadoutSnapshotPacket decode(FriendlyByteBuf buffer) {
        LoadoutSnapshot snapshot = GSON.fromJson(buffer.readUtf(1_048_576), LoadoutSnapshot.class);
        return new LoadoutSnapshotPacket(snapshot, buffer.readEnum(OpenTarget.class));
    }

    public static void handle(LoadoutSnapshotPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPacketHandler.handleSnapshot(packet.snapshot, packet.openTarget));
    }
}
