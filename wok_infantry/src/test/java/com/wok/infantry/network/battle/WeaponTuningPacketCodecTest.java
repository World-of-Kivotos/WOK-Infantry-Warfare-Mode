package com.wok.infantry.network.battle;

import com.wok.infantry.integration.tacz.WeaponTuning;
import com.wok.infantry.network.battle.packet.c2s.ApplyWeaponTuningPacket;
import com.wok.infantry.network.battle.packet.c2s.OpenWeaponTuningEditorPacket;
import com.wok.infantry.network.battle.packet.s2c.OpenWeaponTuningPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class WeaponTuningPacketCodecTest {
    private static final UUID WEAPON_ID = UUID.fromString(
            "7c52f61f-1ad4-4ed1-8f48-a5f41e0ce104");
    private static final WeaponTuning TUNING = new WeaponTuning(
            0.70F, 15.00F, 14.95F, 0.65F);

    @Test
    void openPacketRoundTripsAllPerWeaponFields() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            OpenWeaponTuningPacket.encode(new OpenWeaponTuningPacket(
                    WEAPON_ID, Component.literal("Test Rifle"), TUNING), buffer);
            OpenWeaponTuningPacket decoded = OpenWeaponTuningPacket.decode(buffer);
            assertEquals(WEAPON_ID, decoded.weaponId());
            assertEquals("Test Rifle", decoded.weaponName().getString());
            assertEquals(TUNING, decoded.tuning());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void applyPacketRoundTripsExactGunIdentityAndValues() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ApplyWeaponTuningPacket.encode(new ApplyWeaponTuningPacket(
                    WEAPON_ID, TUNING), buffer);
            assertEquals(new ApplyWeaponTuningPacket(WEAPON_ID, TUNING),
                    ApplyWeaponTuningPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void applyDecoderRejectsOutOfRangeMultiplier() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUUID(WEAPON_ID);
            buffer.writeFloat(0.24F);
            buffer.writeFloat(1.0F);
            buffer.writeFloat(1.0F);
            buffer.writeFloat(1.0F);
            assertThrows(IllegalArgumentException.class,
                    () -> ApplyWeaponTuningPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void applyDecoderRejectsRecoilAboveFifteenTimes() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUUID(WEAPON_ID);
            buffer.writeFloat(1.0F);
            buffer.writeFloat(15.01F);
            buffer.writeFloat(1.0F);
            buffer.writeFloat(1.0F);
            assertThrows(IllegalArgumentException.class,
                    () -> ApplyWeaponTuningPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void keybindOpenRequestHasAnEmptyStrictPayload() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            OpenWeaponTuningEditorPacket.encode(
                    new OpenWeaponTuningEditorPacket(), buffer);
            assertEquals(new OpenWeaponTuningEditorPacket(),
                    OpenWeaponTuningEditorPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void keybindOpenDecoderRejectsTrailingData() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeByte(1);
            assertThrows(IllegalArgumentException.class,
                    () -> OpenWeaponTuningEditorPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
