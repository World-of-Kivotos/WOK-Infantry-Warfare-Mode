package com.wok.capturepoints.network;

import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import com.wok.capturepoints.client.ClientCaptureState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record CaptureSnapshotPacket(
        List<CapturePointView> points,
        String insidePointId,
        boolean sequential,
        int defaultCaptureSeconds) {

    private static final int MAX_POINTS = 512;

    public static void encode(CaptureSnapshotPacket packet, FriendlyByteBuf buffer) {
        int size = Math.min(MAX_POINTS, packet.points.size());
        buffer.writeVarInt(size);
        for (int index = 0; index < size; index++) {
            CapturePointView point = packet.points.get(index);
            buffer.writeUtf(point.id(), 64);
            buffer.writeUtf(point.displayName(), 96);
            buffer.writeResourceLocation(point.dimension());
            buffer.writeBlockPos(point.min());
            buffer.writeBlockPos(point.max());
            buffer.writeVarInt(point.order());
            buffer.writeVarInt(point.captureSeconds());
            buffer.writeBoolean(point.enabled());
            buffer.writeDouble(point.control());
            buffer.writeEnum(point.owner());
            buffer.writeEnum(point.activeTeam());
            buffer.writeVarInt(point.bluePlayers());
            buffer.writeVarInt(point.redPlayers());
            buffer.writeVarInt(point.speedMultiplier());
            buffer.writeBoolean(point.blueAllowed());
            buffer.writeBoolean(point.redAllowed());
        }
        buffer.writeUtf(packet.insidePointId == null ? "" : packet.insidePointId, 64);
        buffer.writeBoolean(packet.sequential);
        buffer.writeVarInt(packet.defaultCaptureSeconds);
    }

    public static CaptureSnapshotPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_POINTS) {
            throw new IllegalArgumentException("Invalid capture point count: " + size);
        }
        List<CapturePointView> points = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            String id = buffer.readUtf(64);
            String displayName = buffer.readUtf(96);
            ResourceLocation dimension = buffer.readResourceLocation();
            BlockPos min = buffer.readBlockPos();
            BlockPos max = buffer.readBlockPos();
            int order = buffer.readVarInt();
            int seconds = buffer.readVarInt();
            boolean enabled = buffer.readBoolean();
            double control = buffer.readDouble();
            CaptureTeam owner = buffer.readEnum(CaptureTeam.class);
            CaptureTeam active = buffer.readEnum(CaptureTeam.class);
            int blue = buffer.readVarInt();
            int red = buffer.readVarInt();
            int speed = buffer.readVarInt();
            boolean blueAllowed = buffer.readBoolean();
            boolean redAllowed = buffer.readBoolean();
            points.add(new CapturePointView(id, displayName, dimension, min, max,
                    order, seconds, enabled, control, owner, active, blue, red, speed,
                    blueAllowed, redAllowed));
        }
        String inside = buffer.readUtf(64);
        CaptureSnapshotPacket packet = new CaptureSnapshotPacket(List.copyOf(points),
                inside.isBlank() ? null : inside, buffer.readBoolean(), buffer.readVarInt());
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing capture snapshot data");
        }
        return packet;
    }

    public static void handle(CaptureSnapshotPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientCaptureState.update(packet)));
        context.setPacketHandled(true);
    }
}
