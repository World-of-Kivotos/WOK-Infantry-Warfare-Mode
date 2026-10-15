package com.wok.infantry.network;

import com.wok.infantry.configtransfer.CatalogTransferAction;
import com.wok.infantry.configtransfer.CatalogTransferResult;
import com.wok.infantry.network.clientbound.CatalogTransferResultPacket;
import com.wok.infantry.network.serverbound.AdminCatalogTransferPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CatalogTransferPacketTest {
    @Test void requestRoundTripForEveryAction() {
        for (var action : CatalogTransferAction.values()) {
            var packet = new AdminCatalogTransferPacket(42, action, "学院军.json", UUID.randomUUID().toString(), 2);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                AdminCatalogTransferPacket.encode(packet, buffer);
                assertEquals(packet, AdminCatalogTransferPacket.decode(buffer));
            } finally { buffer.release(); }
        }
    }

    @Test void summaryRoundTripContainsNoConfigurationPayload() {
        var packet = new CatalogTransferResultPacket(42, new CatalogTransferResult(true, "2 阵营 / 5 编制",
                UUID.randomUUID().toString(), List.of("学院军.json", "caesar.json"), 0, 3));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CatalogTransferResultPacket.encode(packet, buffer);
            assertEquals(packet, CatalogTransferResultPacket.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test void rejectsTrailingDataAndNegativePage() {
        for (boolean trailing : new boolean[]{true, false}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                AdminCatalogTransferPacket.encode(new AdminCatalogTransferPacket(1, CatalogTransferAction.LIST,
                        "", "", trailing ? 0 : -1), buffer);
                if (trailing) buffer.writeByte(0);
                assertThrows(IllegalArgumentException.class, () -> AdminCatalogTransferPacket.decode(buffer));
            } finally { buffer.release(); }
        }
    }
}
