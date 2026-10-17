package com.wok.infantry.network;

import com.wok.infantry.network.serverbound.AdminEntryPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AdminEntryPacketCodecTest {
    @Test
    void captureMainHandIntentRoundTripsWithoutClientItemData() {
        AdminEntryPacket expected = AdminEntryPacket.captureMainHand(
                "assault", "primary", "old_m4a1", "m4a1_acog", "M4A1 ACOG",
                "academy", "millennium_seminar_mobile", 420);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        AdminEntryPacket.encode(expected, buffer);

        assertEquals(expected, AdminEntryPacket.decode(buffer));
        assertEquals(0, buffer.readableBytes());
        assertEquals("minecraft:air", expected.itemId());
        assertEquals("", expected.snbt());
        assertEquals(420, expected.ammoReserveLimit());
    }
}
