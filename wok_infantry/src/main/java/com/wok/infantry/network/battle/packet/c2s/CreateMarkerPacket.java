package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** Marker type and geometry are client intent; creator, faction and squad are server-derived. */
public record CreateMarkerPacket(TacticalMarkerType type, ResourceLocation dimension,
                                 double x, double z, double endX, double endZ,
                                 long requestedTtlMillis) {
    public CreateMarkerPacket {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(dimension, "dimension");
        requireCoordinate(x, "x");
        requireCoordinate(z, "z");
        requireCoordinate(endX, "end x");
        requireCoordinate(endZ, "end z");
        if (type != TacticalMarkerType.ATTACK_DIRECTION) {
            endX = x;
            endZ = z;
        }
        if (requestedTtlMillis < 0L
                || requestedTtlMillis > BattleRules.MAX_MARKER_TTL_MILLIS) {
            throw new IllegalArgumentException("Invalid marker TTL: " + requestedTtlMillis);
        }
    }

    public static void encode(CreateMarkerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.type.id(), BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        buffer.writeUtf(packet.dimension.toString(), BattleNetworkLimits.MAX_DIMENSION_ID_LENGTH);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.z);
        buffer.writeDouble(packet.endX);
        buffer.writeDouble(packet.endZ);
        buffer.writeVarLong(packet.requestedTtlMillis);
    }

    public static CreateMarkerPacket decode(FriendlyByteBuf buffer) {
        String encodedType = buffer.readUtf(BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        TacticalMarkerType type = TacticalMarkerType.byId(encodedType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid tactical marker type: " + encodedType));
        String encodedDimension = buffer.readUtf(BattleNetworkLimits.MAX_DIMENSION_ID_LENGTH);
        ResourceLocation dimension = ResourceLocation.tryParse(encodedDimension);
        if (dimension == null) {
            throw new IllegalArgumentException("Invalid marker dimension: " + encodedDimension);
        }
        CreateMarkerPacket decoded = new CreateMarkerPacket(type, dimension,
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readVarLong());
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing marker packet data");
        }
        return decoded;
    }

    public static void handle(CreateMarkerPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.MAP_MARKER)) {
            return;
        }
        BattleService service = BattleServerPacketSupport.serviceFor(sender);
        if (service == null || !BattleServerPacketSupport.ensurePlayer(service, sender)) {
            return;
        }
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION,
                packet.dimension);
        if (dimensionKey.equals(DeploymentService.HOLDING_LEVEL)
                || dimensionKey.equals(DeploymentService.LOBBY_LEVEL)) {
            BattleServerPacketSupport.reject(sender, service, ActionResult.Code.INVALID_MARKER,
                    "Internal waiting dimensions cannot contain tactical markers");
            return;
        }
        ServerLevel level = sender.getServer().getLevel(dimensionKey);
        DeploymentPoint waitingBase = DeploymentService.get(sender)
                .filter(deployment -> deployment.isWaitingParticipant(sender.getUUID()))
                .flatMap(deployment -> deployment.pointsFor(sender).stream()
                        .filter(point -> point.dimension().equals(packet.dimension))
                        .findFirst()).orElse(null);
        boolean currentDimension = sender.serverLevel().dimension().location()
                .equals(packet.dimension);
        if (level == null || !currentDimension && waitingBase == null) {
            BattleServerPacketSupport.reject(sender, service, ActionResult.Code.INVALID_MARKER,
                    "Markers can only be placed in your current or selected deployment dimension");
            return;
        }
        double markerY = currentDimension ? sender.getY() : waitingBase.position().getY();
        BlockPos markerBlock = BlockPos.containing(packet.x, markerY, packet.z);
        if (!level.getWorldBorder().isWithinBounds(markerBlock)) {
            BattleServerPacketSupport.reject(sender, service, ActionResult.Code.INVALID_MARKER,
                    "Marker is outside the world border");
            return;
        }
        BlockPos markerEndBlock = BlockPos.containing(packet.endX, markerY, packet.endZ);
        if (!level.getWorldBorder().isWithinBounds(markerEndBlock)) {
            BattleServerPacketSupport.reject(sender, service, ActionResult.Code.INVALID_MARKER,
                    "Marker endpoint is outside the world border");
            return;
        }

        long ttl = packet.requestedTtlMillis == 0L
                ? BattleRules.DEFAULT_MARKER_TTL_MILLIS
                : Math.max(BattleRules.MIN_MARKER_TTL_MILLIS,
                Math.min(BattleRules.MAX_MARKER_TTL_MILLIS, packet.requestedTtlMillis));
        ActionResult result = service.createMarker(sender, packet.type, dimensionKey,
                new Vec3(packet.x, markerY, packet.z),
                new Vec3(packet.endX, markerY, packet.endZ), ttl);
        BattleServerPacketSupport.finish(sender, service, result);
    }

    private static void requireCoordinate(double value, String field) {
        if (!Double.isFinite(value) || Math.abs(value) > BattleNetworkLimits.MAX_COORDINATE) {
            throw new IllegalArgumentException("Invalid marker " + field + ": " + value);
        }
    }
}
