package com.wok.infantry.network.battle;

import com.wok.infantry.ammo.AmmoSupplyView;
import com.wok.infantry.network.battle.packet.c2s.SupplyVehicleAmmoPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyVehicleAmmoPacketTest {
    @Test
    void independentLargeBlockTargetRoundTrips() {
        AmmoSupplyView.Target target = AmmoSupplyView.Target.largeBlock(
                new BlockPos(12, 70, -25));
        SupplyVehicleAmmoPacket original = new SupplyVehicleAmmoPacket(
                target, 42, "main_cannon", 3, 16);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SupplyVehicleAmmoPacket.encode(original, buffer);
            SupplyVehicleAmmoPacket decoded = SupplyVehicleAmmoPacket.decode(buffer);

            assertEquals(original, decoded);
            assertEquals(new BlockPos(12, 70, -25), decoded.stationTarget().blockPos());
            assertTrue(decoded.stationTarget().isLarge());
        } finally {
            buffer.release();
        }
    }

    @Test
    void vehicleSupplyRejectsNonLargeTargets() {
        SupplyVehicleAmmoPacket invalid = new SupplyVehicleAmmoPacket(
                AmmoSupplyView.Target.mediumCrate(BlockPos.ZERO),
                42, "main_cannon", 0, 1);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SupplyVehicleAmmoPacket.encode(invalid, buffer);
            assertThrows(IllegalArgumentException.class,
                    () -> SupplyVehicleAmmoPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
