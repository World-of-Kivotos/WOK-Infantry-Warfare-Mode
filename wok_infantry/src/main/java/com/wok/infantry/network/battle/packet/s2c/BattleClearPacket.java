package com.wok.infantry.network.battle.packet.s2c;

import com.wok.infantry.network.battle.client.BattleClientPacketBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Clears all faction-sensitive client battle state, for example when a battle service stops. */
public record BattleClearPacket() {
    public static void encode(BattleClearPacket packet, FriendlyByteBuf buffer) {
    }

    public static BattleClearPacket decode(FriendlyByteBuf buffer) {
        return new BattleClearPacket();
    }

    public static void handle(BattleClearPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BattleClientPacketBridge::clear);
    }
}
