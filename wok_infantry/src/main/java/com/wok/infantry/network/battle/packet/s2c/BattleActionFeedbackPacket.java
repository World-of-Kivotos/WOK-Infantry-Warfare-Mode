package com.wok.infantry.network.battle.packet.s2c;

import com.wok.infantry.network.battle.BattleNetworkLimits;
import com.wok.infantry.network.battle.client.BattleClientPacketBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** Short action feedback rendered inside the opaque squad/map screens. */
public record BattleActionFeedbackPacket(boolean success, String message) {
    public BattleActionFeedbackPacket {
        message = Objects.requireNonNullElse(message, "").trim();
        if (message.length() > BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Battle action feedback is too long");
        }
    }

    public static void encode(BattleActionFeedbackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.success);
        buffer.writeUtf(packet.message, BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH);
    }

    public static BattleActionFeedbackPacket decode(FriendlyByteBuf buffer) {
        return new BattleActionFeedbackPacket(buffer.readBoolean(),
                buffer.readUtf(BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH));
    }

    public static void handle(BattleActionFeedbackPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                BattleClientPacketBridge.feedback(packet.success, packet.message));
    }
}
