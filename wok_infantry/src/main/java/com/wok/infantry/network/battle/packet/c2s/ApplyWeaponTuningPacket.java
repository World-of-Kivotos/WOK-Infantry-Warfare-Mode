package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.integration.tacz.TaczAdsSpeedAdapter;
import com.wok.infantry.integration.tacz.WeaponTuning;
import com.wok.infantry.network.ServerRequestLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Server-authoritative update for the exact gun that opened the editor. */
public record ApplyWeaponTuningPacket(UUID weaponId, WeaponTuning tuning) {
    public ApplyWeaponTuningPacket {
        Objects.requireNonNull(weaponId, "weaponId");
        Objects.requireNonNull(tuning, "tuning");
    }

    public static void encode(ApplyWeaponTuningPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.weaponId);
        buffer.writeFloat(packet.tuning.adsSpeedScale());
        buffer.writeFloat(packet.tuning.verticalRecoilScale());
        buffer.writeFloat(packet.tuning.horizontalRecoilScale());
        buffer.writeFloat(packet.tuning.spreadScale());
    }

    public static ApplyWeaponTuningPacket decode(FriendlyByteBuf buffer) {
        UUID weaponId = buffer.readUUID();
        WeaponTuning tuning = new WeaponTuning(buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat());
        if (!tuning.valid() || buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Invalid weapon tuning request");
        }
        return new ApplyWeaponTuningPacket(weaponId, tuning);
    }

    public static void handle(ApplyWeaponTuningPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || sender.getServer() == null
                || !sender.getServer().isSameThread()
                || !sender.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)
                || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.ADMIN_MUTATION)) {
            return;
        }
        ItemStack stack = sender.getMainHandItem();
        if (!TaczAdsSpeedAdapter.isGun(stack)
                || !TaczAdsSpeedAdapter.matchesEditorId(stack, packet.weaponId)) {
            sender.displayClientMessage(Component.translatable(
                    "message.wok_infantry.weapon_tuning.weapon_changed"), false);
            return;
        }
        if (!TaczAdsSpeedAdapter.applyTuning(stack, packet.tuning)) {
            sender.displayClientMessage(Component.translatable(
                    "message.wok_infantry.weapon_tuning.invalid"), false);
            return;
        }
        sender.getInventory().setChanged();
        sender.inventoryMenu.broadcastChanges();
        if (sender.containerMenu != sender.inventoryMenu) {
            sender.containerMenu.broadcastChanges();
        }
        TaczAdsSpeedAdapter.refresh(sender, stack);
        sender.displayClientMessage(Component.translatable(
                packet.tuning.defaultValues()
                        ? "message.wok_infantry.weapon_tuning.reset"
                        : "message.wok_infantry.weapon_tuning.applied",
                stack.getHoverName()), false);
    }
}
