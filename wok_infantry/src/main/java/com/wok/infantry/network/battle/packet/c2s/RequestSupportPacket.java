package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import com.wok.infantry.support.SupportService;
import com.wok.infantry.support.SupportTarget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** A commander intent. Authorization, terrain height and projectile creation remain server-side. */
public record RequestSupportPacket(UUID requestId, ResourceLocation supportId,
                                   ResourceLocation dimension,
                                   double startX, double startZ,
                                   double endX, double endZ) {
    public RequestSupportPacket {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(supportId, "supportId");
        Objects.requireNonNull(dimension, "dimension");
        requireCoordinate(startX, "start x");
        requireCoordinate(startZ, "start z");
        requireCoordinate(endX, "end x");
        requireCoordinate(endZ, "end z");
    }

    public RequestSupportPacket(UUID requestId, ResourceLocation supportId,
                                SupportTarget target) {
        this(requestId, supportId, target.dimension(), target.startX(), target.startZ(),
                target.endX(), target.endZ());
    }

    public SupportTarget target() {
        return new SupportTarget(dimension, startX, startZ, endX, endZ);
    }

    public static void encode(RequestSupportPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.requestId);
        buffer.writeUtf(packet.supportId.toString(), BattleNetworkLimits.MAX_SUPPORT_ID_LENGTH);
        buffer.writeUtf(packet.dimension.toString(), BattleNetworkLimits.MAX_DIMENSION_ID_LENGTH);
        buffer.writeDouble(packet.startX);
        buffer.writeDouble(packet.startZ);
        buffer.writeDouble(packet.endX);
        buffer.writeDouble(packet.endZ);
    }

    public static RequestSupportPacket decode(FriendlyByteBuf buffer) {
        UUID requestId = buffer.readUUID();
        String encodedSupportId = buffer.readUtf(BattleNetworkLimits.MAX_SUPPORT_ID_LENGTH);
        ResourceLocation supportId = ResourceLocation.tryParse(encodedSupportId);
        if (supportId == null) {
            throw new IllegalArgumentException("Invalid support id: " + encodedSupportId);
        }
        String encodedDimension = buffer.readUtf(BattleNetworkLimits.MAX_DIMENSION_ID_LENGTH);
        ResourceLocation dimension = ResourceLocation.tryParse(encodedDimension);
        if (dimension == null) {
            throw new IllegalArgumentException("Invalid support dimension: " + encodedDimension);
        }
        RequestSupportPacket decoded = new RequestSupportPacket(requestId, supportId, dimension,
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble());
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing support packet data");
        }
        return decoded;
    }

    public static void handle(RequestSupportPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.SUPPORT_ACTION)) {
            return;
        }
        BattleService battle = BattleServerPacketSupport.serviceFor(sender);
        if (battle == null || !BattleServerPacketSupport.ensurePlayer(battle, sender)) {
            return;
        }
        SupportService support = SupportService.get(sender).orElse(null);
        if (support == null) {
            BattleServerPacketSupport.finish(sender, battle, ActionResult.failure(
                    ActionResult.Code.INVALID_TARGET, "Support service is not running"));
            return;
        }
        ActionResult result = support.requestSupport(sender, packet.requestId,
                packet.supportId, packet.target());
        BattleServerPacketSupport.finish(sender, battle, result);
    }

    private static void requireCoordinate(double value, String field) {
        if (!Double.isFinite(value) || Math.abs(value) > BattleNetworkLimits.MAX_COORDINATE) {
            throw new IllegalArgumentException("Invalid support " + field + ": " + value);
        }
    }
}
