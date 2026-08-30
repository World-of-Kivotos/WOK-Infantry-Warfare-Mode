package com.wok.infantry.network;

import com.wok.infantry.formation.FormationClassEditAction;
import com.wok.infantry.network.serverbound.AdminClassPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AdminClassPacketCodecTest {
    @Test
    void formationOwnedProfessionRoundTrips() {
        AdminClassPacket expected = new AdminClassPacket(FormationClassEditAction.CREATE,
                "academy", "millennium_seminar_mobile", "custom_a1b2c3",
                "突破手", 2);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        AdminClassPacket.encode(expected, buffer);

        assertEquals(expected, AdminClassPacket.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void professionMoveRequestRoundTrips() {
        AdminClassPacket expected = new AdminClassPacket(FormationClassEditAction.MOVE_UP,
                "academy", "default", "squad_leader", "队长", 1);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        AdminClassPacket.encode(expected, buffer);

        assertEquals(expected, AdminClassPacket.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void trailingProfessionSettingsAreRejected() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        AdminClassPacket.encode(new AdminClassPacket(FormationClassEditAction.UPDATE,
                "academy", "default", "support", "支援兵", 2), buffer);
        buffer.writeByte(1);

        assertThrows(IllegalArgumentException.class, () -> AdminClassPacket.decode(buffer));
    }
}
