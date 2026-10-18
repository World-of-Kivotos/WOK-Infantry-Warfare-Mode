package com.wok.infantry.network;

import com.wok.infantry.network.stamina.StaminaNetwork;
import com.wok.infantry.network.stamina.StaminaSyncPacket;
import com.wok.infantry.stamina.StaminaSnapshot;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaminaSyncPacketTest {
    @Test
    void recoveryLockRoundTripsInsteadOfBeingInferredFromNonzeroStamina() {
        var expected = new StaminaSyncPacket(new StaminaSnapshot(49.0F, 10.0F, true, true));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            StaminaSyncPacket.encode(expected, buffer);
            assertEquals(expected, StaminaSyncPacket.decode(buffer));
            assertEquals("2", StaminaNetwork.PROTOCOL_VERSION);
        } finally { buffer.release(); }
    }

    @Test
    void truncatedAndTrailingPayloadsAreRejected() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeFloat(100).writeFloat(0).writeBoolean(true);
            assertThrows(IndexOutOfBoundsException.class, () -> StaminaSyncPacket.decode(buffer));
            buffer.clear();
            StaminaSyncPacket.encode(new StaminaSyncPacket(new StaminaSnapshot(100, 100, true)), buffer);
            buffer.writeByte(1);
            assertThrows(IllegalArgumentException.class, () -> StaminaSyncPacket.decode(buffer));
        } finally { buffer.release(); }
    }
}
