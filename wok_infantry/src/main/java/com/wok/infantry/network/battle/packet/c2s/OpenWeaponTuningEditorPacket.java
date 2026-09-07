package com.wok.infantry.network.battle.packet.c2s;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.server.BattleCommands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Configurable-key request; all permission and held-gun checks remain server-side. */
public record OpenWeaponTuningEditorPacket() {
    public static void encode(OpenWeaponTuningEditorPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenWeaponTuningEditorPacket decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected weapon editor request data");
        }
        return new OpenWeaponTuningEditorPacket();
    }

    public static void handle(OpenWeaponTuningEditorPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || sender.getServer() == null
                || !sender.getServer().isSameThread()
                || !ServerRequestLimiter.allow(sender, ServerRequestLimiter.Kind.OPEN_UI)) {
            return;
        }
        try {
            BattleCommands.openHeldWeaponEditor(sender.createCommandSourceStack());
        } catch (CommandSyntaxException exception) {
            // A real ServerPlayer source should always satisfy getPlayerOrException().
            sender.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.wok_infantry.weapon_tuning.unavailable"), false);
        }
    }
}
