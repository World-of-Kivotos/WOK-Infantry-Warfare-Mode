package com.wok.infantry.network;

import com.wok.infantry.network.serverbound.AdminClassLoadoutCopyPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AdminClassLoadoutCopyPacketCodecTest {
    @Test
    void sourceAndTargetProfessionRoundTripWithoutTrailingData() {
        AdminClassLoadoutCopyPacket expected = new AdminClassLoadoutCopyPacket(
                "academy", "default", "assault",
                "caesar", "guards", "leader");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        AdminClassLoadoutCopyPacket.encode(expected, buffer);

        assertEquals(expected, AdminClassLoadoutCopyPacket.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void trailingDataIsRejected() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        AdminClassLoadoutCopyPacket.encode(new AdminClassLoadoutCopyPacket(
                "academy", "default", "assault",
                "caesar", "guards", "leader"), buffer);
        buffer.writeByte(1);

        assertThrows(IllegalArgumentException.class,
                () -> AdminClassLoadoutCopyPacket.decode(buffer));
    }
}
