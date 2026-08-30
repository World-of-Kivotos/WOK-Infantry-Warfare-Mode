package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.battle.BattleNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Client intent to select one opaque, server-issued deployment point ID. */
public record SelectDeploymentPointPacket(UUID pointId) {
    private static final int UUID_BYTES = 16;

    public SelectDeploymentPointPacket {
        Objects.requireNonNull(pointId, "pointId");
    }

    public static void encode(SelectDeploymentPointPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.pointId);
    }

    public static SelectDeploymentPointPacket decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != UUID_BYTES) {
            throw new IllegalArgumentException("Invalid deployment point packet length");
        }
        return new SelectDeploymentPointPacket(buffer.readUUID());
    }

    public static void handle(SelectDeploymentPointPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || sender.getServer() == null
                || !sender.getServer().isSameThread()
                || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.DEPLOYMENT_SELECTION)) {
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
                : deployment.selectPoint(sender, packet.pointId);
        BattleNetwork.finishDeploymentAction(battle, sender, result, false);
    }
}
