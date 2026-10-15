package com.wok.infantry.battle.tickets;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketNetworkSnapshotTest {
    @Test
    void protocolTwoCarriesTheBarMaximum() {
        assertEquals("2", TicketNetwork.PROTOCOL_VERSION);
        TicketNetwork.Snapshot snapshot = new TicketNetwork.Snapshot(412, 377, 500, true);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        snapshot.encode(buffer);
        assertEquals(snapshot, TicketNetwork.Snapshot.decode(buffer));
    }

    @Test
    void decodeRejectsTrailingBytesAndImpossibleValues() {
        FriendlyByteBuf trailing = new FriendlyByteBuf(Unpooled.buffer());
        new TicketNetwork.Snapshot(1, 2, 3, false).encode(trailing);
        trailing.writeByte(0);
        assertThrows(IllegalArgumentException.class, () -> TicketNetwork.Snapshot.decode(trailing));

        FriendlyByteBuf belowSide = new FriendlyByteBuf(Unpooled.buffer());
        belowSide.writeVarInt(600);
        belowSide.writeVarInt(10);
        belowSide.writeVarInt(500);
        belowSide.writeBoolean(true);
        assertThrows(IllegalArgumentException.class, () -> TicketNetwork.Snapshot.decode(belowSide),
                "the maximum can never be below a side's value");

        FriendlyByteBuf protocolOne = new FriendlyByteBuf(Unpooled.buffer());
        protocolOne.writeVarInt(5);
        protocolOne.writeVarInt(5);
        protocolOne.writeBoolean(true);
        assertThrows(RuntimeException.class, () -> TicketNetwork.Snapshot.decode(protocolOne),
                "a protocol-1 payload is not accepted as protocol 2");

        assertThrows(IllegalArgumentException.class,
                () -> new TicketNetwork.Snapshot(0, 0, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> new TicketNetwork.Snapshot(-1, 0, 10, false));
        assertThrows(IllegalArgumentException.class,
                () -> new TicketNetwork.Snapshot(0, 0, TicketNetwork.MAX_TICKETS + 1, false));
    }

    @Test
    void ratiosAndTheCompatibilityConstructor() {
        TicketNetwork.Snapshot snapshot = new TicketNetwork.Snapshot(250, 0, 500, true);
        assertEquals(0.5F, snapshot.ratio(snapshot.blue()));
        assertEquals(0.0F, snapshot.ratio(snapshot.red()));
        assertEquals(1.0F, snapshot.ratio(9_999));
        assertEquals(new TicketNetwork.Snapshot(30, 40, 40, true),
                new TicketNetwork.Snapshot(30, 40, true));
        assertEquals(1, new TicketNetwork.Snapshot(0, 0, false).max());
    }

    @Test
    void serverBarMaximumIsTheStartingManpowerWithinBounds() {
        assertEquals(500, TicketService.barMaximum(500, 412, 377));
        assertEquals(600, TicketService.barMaximum(500, 600, 10), "a side above the setting");
        assertEquals(1, TicketService.barMaximum(0, 0, 0));
        assertEquals(TicketNetwork.MAX_TICKETS,
                TicketService.barMaximum(Integer.MAX_VALUE, 0, 0));
    }
}
