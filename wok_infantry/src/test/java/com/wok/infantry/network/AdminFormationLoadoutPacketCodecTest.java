package com.wok.infantry.network;

import com.wok.infantry.formation.FormationLoadoutEditAction;
import com.wok.infantry.network.serverbound.AdminFormationLoadoutPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AdminFormationLoadoutPacketCodecTest {
    @Test
    void strictAllowListIntentRoundTrips() {
        AdminFormationLoadoutPacket expected = new AdminFormationLoadoutPacket(
                FormationLoadoutEditAction.EXCLUSIVE, "academy",
                "millennium_seminar_mobile", "assault", "primary", "m4a1");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        AdminFormationLoadoutPacket.encode(expected, buffer);

        assertEquals(expected, AdminFormationLoadoutPacket.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }
}
