package com.wok.infantry.network.battle.packet.s2c;

import com.wok.infantry.integration.tacz.WeaponTuning;
import com.wok.infantry.network.battle.client.WeaponTuningClientPacketBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Opens the admin editor for one server-identified main-hand gun ItemStack. */
public record OpenWeaponTuningPacket(UUID weaponId, Component weaponName,
                                     WeaponTuning tuning) {
    public OpenWeaponTuningPacket {
        Objects.requireNonNull(weaponId, "weaponId");
        Objects.requireNonNull(weaponName, "weaponName");
        Objects.requireNonNull(tuning, "tuning");
        if (!tuning.valid()) {
            throw new IllegalArgumentException("Invalid weapon tuning view");
        }
    }

    public static void encode(OpenWeaponTuningPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.weaponId);
        buffer.writeComponent(packet.weaponName);
        writeTuning(buffer, packet.tuning);
    }

    public static OpenWeaponTuningPacket decode(FriendlyByteBuf buffer) {
        UUID weaponId = buffer.readUUID();
        Component weaponName = buffer.readComponent();
        WeaponTuning tuning = readTuning(buffer);
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing weapon tuning data");
        }
        return new OpenWeaponTuningPacket(weaponId, weaponName, tuning);
    }

    public static void handle(OpenWeaponTuningPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> WeaponTuningClientPacketBridge.open(
                        packet.weaponId, packet.weaponName, packet.tuning));
    }

    private static void writeTuning(FriendlyByteBuf buffer, WeaponTuning tuning) {
        buffer.writeFloat(tuning.adsSpeedScale());
        buffer.writeFloat(tuning.verticalRecoilScale());
        buffer.writeFloat(tuning.horizontalRecoilScale());
        buffer.writeFloat(tuning.spreadScale());
    }

    private static WeaponTuning readTuning(FriendlyByteBuf buffer) {
        WeaponTuning tuning = new WeaponTuning(buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat());
        if (!tuning.valid()) {
            throw new IllegalArgumentException("Weapon tuning view is out of range");
        }
        return tuning;
    }
}
