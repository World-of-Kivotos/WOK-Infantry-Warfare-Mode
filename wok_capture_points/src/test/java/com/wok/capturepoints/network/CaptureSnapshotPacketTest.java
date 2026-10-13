package com.wok.capturepoints.network;

import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaptureSnapshotPacketTest {
    @Test
    void roundTripsBoundedObjectiveSnapshot() {
        CaptureSnapshotPacket packet = fixture();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        CaptureSnapshotPacket.encode(packet, buffer);
        assertEquals(packet, CaptureSnapshotPacket.decode(buffer));
    }

    @Test
    void rejectsTrailingData() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        CaptureSnapshotPacket.encode(fixture(), buffer);
        buffer.writeByte(7);
        assertThrows(IllegalArgumentException.class,
                () -> CaptureSnapshotPacket.decode(buffer));
    }

    private static CaptureSnapshotPacket fixture() {
        CapturePointView point = new CapturePointView("a", "A点",
                ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                new BlockPos(1, 60, 2), new BlockPos(12, 75, 14), 0, 90,
                true, 0.25D, CaptureTeam.NEUTRAL, CaptureTeam.BLUE,
                3, 1, 2, true, false);
        return new CaptureSnapshotPacket(List.of(point), "a", true, 90);
    }
}
