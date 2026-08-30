package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntentPacketBoundaryTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final ResourceLocation POINT_SUPPORT =
            ResourceLocation.fromNamespaceAndPath("wok_infantry", "test_point");
    private static final ResourceLocation DIRECTIONAL_SUPPORT =
            ResourceLocation.fromNamespaceAndPath("wok_infantry", "test_directional");

    @Test
    void everySquadIntentRoundTripsWithOnlyItsRequiredAuthorityInputs() {
        UUID target = new UUID(4L, 2L);
        assertRoundTrips(SquadActionPacket.create(SquadCallsign.ALPHA));
        assertRoundTrips(SquadActionPacket.join(SquadCallsign.ECHO));
        assertRoundTrips(SquadActionPacket.leave());
        assertRoundTrips(SquadActionPacket.disband());
        assertRoundTrips(SquadActionPacket.promote(target));
        assertRoundTrips(SquadActionPacket.kick(target));
    }

    @Test
    void squadAndCommanderIntentsRejectTrailingOrMissingPayload() {
        FriendlyByteBuf squadTrailing = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SquadActionPacket.encode(SquadActionPacket.leave(), squadTrailing);
            squadTrailing.writeByte(0x7F);
            assertThrows(IllegalArgumentException.class,
                    () -> SquadActionPacket.decode(squadTrailing));
        } finally {
            squadTrailing.release();
        }

        FriendlyByteBuf commanderMissingTarget = new FriendlyByteBuf(Unpooled.buffer());
        try {
            commanderMissingTarget.writeUtf("transfer");
            assertThrows(IndexOutOfBoundsException.class,
                    () -> CommanderActionPacket.decode(commanderMissingTarget));
        } finally {
            commanderMissingTarget.release();
        }

        assertThrows(IllegalArgumentException.class,
                () -> new CommanderActionPacket(CommanderActionPacket.Action.RESIGN,
                        UUID.randomUUID()));
    }

    @Test
    void deploymentSelectionRequiresExactlyOneUuid() {
        FriendlyByteBuf shortPacket = new FriendlyByteBuf(Unpooled.buffer());
        try {
            shortPacket.writeLong(1L);
            assertThrows(IllegalArgumentException.class,
                    () -> SelectDeploymentPointPacket.decode(shortPacket));
        } finally {
            shortPacket.release();
        }

        FriendlyByteBuf trailingPacket = new FriendlyByteBuf(Unpooled.buffer());
        try {
            trailingPacket.writeUUID(UUID.randomUUID());
            trailingPacket.writeByte(1);
            assertThrows(IllegalArgumentException.class,
                    () -> SelectDeploymentPointPacket.decode(trailingPacket));
        } finally {
            trailingPacket.release();
        }
    }

    @Test
    void silentSnapshotRefreshRequiresAnEmptyPacket() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeByte(1);
            assertThrows(IllegalArgumentException.class,
                    () -> BattleSnapshotRequestPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void markerIntentBoundsCoordinatesTtlAndPointGeometry() {
        CreateMarkerPacket point = new CreateMarkerPacket(TacticalMarkerType.IFV, OVERWORLD,
                10.0D, 20.0D, 9_999.0D, -9_999.0D, 0L);
        assertEquals(point.x(), point.endX());
        assertEquals(point.z(), point.endZ());

        assertThrows(IllegalArgumentException.class, () -> new CreateMarkerPacket(
                TacticalMarkerType.TANK, OVERWORLD, Double.NaN, 0.0D,
                0.0D, 0.0D, 0L));
        assertThrows(IllegalArgumentException.class, () -> new CreateMarkerPacket(
                TacticalMarkerType.TANK, OVERWORLD,
                BattleRules.MAX_COORDINATE + 1.0D, 0.0D,
                0.0D, 0.0D, 0L));
        assertThrows(IllegalArgumentException.class, () -> new CreateMarkerPacket(
                TacticalMarkerType.TANK, OVERWORLD, 0.0D, 0.0D,
                0.0D, 0.0D, BattleRules.MAX_MARKER_TTL_MILLIS + 1L));
    }

    @Test
    void markerIntentRejectsTrailingPayload() {
        CreateMarkerPacket packet = new CreateMarkerPacket(
                TacticalMarkerType.ATTACK_DIRECTION, OVERWORLD,
                10.0D, 20.0D, 110.0D, 20.0D, 0L);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CreateMarkerPacket.encode(packet, buffer);
            buffer.writeByte(0x7F);

            assertThrows(IllegalArgumentException.class,
                    () -> CreateMarkerPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void supportIntentRoundTripsAndBoundsAuthorityInputs() {
        UUID requestId = new UUID(9L, 4L);
        RequestSupportPacket directional = new RequestSupportPacket(requestId,
                DIRECTIONAL_SUPPORT, OVERWORLD,
                10.0D, 20.0D, 110.0D, 20.0D);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            RequestSupportPacket.encode(directional, buffer);
            assertEquals(directional, RequestSupportPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }

        RequestSupportPacket point = new RequestSupportPacket(requestId,
                POINT_SUPPORT, OVERWORLD,
                5.0D, 6.0D, 999.0D, -999.0D);
        assertEquals(999.0D, point.endX(),
                "the packet must not guess target mode before server registry lookup");
        assertEquals(-999.0D, point.endZ());
        FriendlyByteBuf nonCanonicalPoint = new FriendlyByteBuf(Unpooled.buffer());
        try {
            nonCanonicalPoint.writeUUID(requestId);
            nonCanonicalPoint.writeUtf(POINT_SUPPORT.toString(),
                    BattleNetworkLimits.MAX_SUPPORT_ID_LENGTH);
            nonCanonicalPoint.writeUtf(OVERWORLD.toString(),
                    BattleNetworkLimits.MAX_DIMENSION_ID_LENGTH);
            nonCanonicalPoint.writeDouble(5.0D);
            nonCanonicalPoint.writeDouble(6.0D);
            nonCanonicalPoint.writeDouble(500.0D);
            nonCanonicalPoint.writeDouble(600.0D);
            RequestSupportPacket decoded = RequestSupportPacket.decode(nonCanonicalPoint);
            assertEquals(500.0D, decoded.endX());
            assertEquals(600.0D, decoded.endZ());
        } finally {
            nonCanonicalPoint.release();
        }

        RequestSupportPacket exactBoundary = new RequestSupportPacket(requestId,
                DIRECTIONAL_SUPPORT, OVERWORLD,
                BattleRules.MAX_COORDINATE, -BattleRules.MAX_COORDINATE,
                -BattleRules.MAX_COORDINATE, BattleRules.MAX_COORDINATE);
        FriendlyByteBuf boundaryBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            RequestSupportPacket.encode(exactBoundary, boundaryBuffer);
            assertEquals(exactBoundary, RequestSupportPacket.decode(boundaryBuffer));
        } finally {
            boundaryBuffer.release();
        }

        assertThrows(IllegalArgumentException.class, () -> new RequestSupportPacket(
                requestId, DIRECTIONAL_SUPPORT, OVERWORLD,
                Double.NaN, 0.0D, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> new RequestSupportPacket(
                requestId, DIRECTIONAL_SUPPORT, OVERWORLD,
                0.0D, Double.POSITIVE_INFINITY, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> new RequestSupportPacket(
                requestId, DIRECTIONAL_SUPPORT, OVERWORLD,
                0.0D, 0.0D, Double.NEGATIVE_INFINITY, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> new RequestSupportPacket(
                requestId, DIRECTIONAL_SUPPORT, OVERWORLD,
                BattleRules.MAX_COORDINATE + 1.0D, 0.0D, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> new RequestSupportPacket(
                requestId, DIRECTIONAL_SUPPORT, OVERWORLD,
                0.0D, 0.0D, -BattleRules.MAX_COORDINATE - 1.0D, 1.0D));
    }

    @Test
    void supportIntentRejectsTrailingPayload() {
        RequestSupportPacket packet = new RequestSupportPacket(UUID.randomUUID(),
                DIRECTIONAL_SUPPORT, OVERWORLD,
                10.0D, 20.0D, 80.0D, 20.0D);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            RequestSupportPacket.encode(packet, buffer);
            buffer.writeByte(0x7F);
            assertThrows(IllegalArgumentException.class,
                    () -> RequestSupportPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    private static void assertRoundTrips(SquadActionPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SquadActionPacket.encode(packet, buffer);
            assertEquals(packet, SquadActionPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
