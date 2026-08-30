package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.battle.BattleNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Requests that the authoritative deployment service abandon the current life. */
public record RedeployPacket() {
    public static void encode(RedeployPacket packet, FriendlyByteBuf buffer) {
    }

    public static RedeployPacket decode(FriendlyByteBuf buffer) {
        requireEmpty(buffer);
        return new RedeployPacket();
    }

    public static void handle(RedeployPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || sender.getServer() == null
                || !sender.getServer().isSameThread()
                || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.DEPLOYMENT_ACTION)) {
            return;
        }
        BattleService battle = BattleServerPacketSupport.serviceFor(sender);
        if (battle == null || !BattleServerPacketSupport.ensurePlayer(battle, sender)) {
            return;
        }
        DeploymentService deployment = DeploymentService.get(sender).orElse(null);
        ActionResult result = deployment == null
                ? ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                "部署服务尚未启动")
                : deployment.redeploy(sender);
        BattleNetwork.finishDeploymentAction(battle, sender, result, result.success());
    }

    private static void requireEmpty(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Redeploy packet must be empty");
        }
    }
}
