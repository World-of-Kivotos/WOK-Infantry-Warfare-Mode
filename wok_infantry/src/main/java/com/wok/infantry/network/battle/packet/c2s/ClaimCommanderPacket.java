package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.network.ServerRequestLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClaimCommanderPacket() {
    public static void encode(ClaimCommanderPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClaimCommanderPacket decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Claim commander packet must be empty");
        }
        return new ClaimCommanderPacket();
    }

    public static void handle(ClaimCommanderPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.SQUAD_ACTION)) {
            return;
        }
        BattleService service = BattleServerPacketSupport.serviceFor(sender);
        if (service == null || !BattleServerPacketSupport.ensurePlayer(service, sender)) {
            return;
        }
        boolean wasCommander = service.isCommander(sender.getUUID());
        ActionResult result = service.claimCommander(sender);
        BattleServerPacketSupport.finish(sender, service, result,
                !wasCommander && service.isCommander(sender.getUUID()));
    }
}
