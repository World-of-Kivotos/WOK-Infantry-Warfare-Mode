package com.wok.infantry.network.battle;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.ClassQuotaView;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.MemberPosition;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.PermissionView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.battle.TacticalMarker;
import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentPointKind;
import com.wok.infantry.deployment.DeploymentView;
import com.wok.infantry.network.battle.packet.s2c.BattleSnapshotPacket;
import com.wok.infantry.support.SupportMissionView;
import com.wok.infantry.support.SupportOptionView;
import com.wok.infantry.support.SupportTargetMode;
import com.wok.infantry.support.SupportView;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleSnapshotCodecTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final ResourceLocation POINT_SUPPORT =
            ResourceLocation.fromNamespaceAndPath("wok_infantry", "test_point");
    private static final ResourceLocation DIRECTIONAL_SUPPORT =
            ResourceLocation.fromNamespaceAndPath("wok_infantry", "test_directional");
    private static final ResourceLocation SECOND_DIRECTIONAL_SUPPORT =
            ResourceLocation.fromNamespaceAndPath("wok_infantry", "test_directional_second");
    private static final PermissionView LEADER_PERMISSIONS = new PermissionView(
            false, false, true, true, true, false, true);

    @Test
    void exactFortyPlayerFactionRoundTripsAtEverySquadBoundary() {
        List<SquadView> squads = fullFactionSquads();
        List<MemberPosition> positions = squads.stream()
                .flatMap(squad -> squad.members().stream())
                .map(member -> new MemberPosition(member.playerId(), OVERWORLD,
                        10.0D, 64.0D, 20.0D, 90.0F))
                .toList();
        UUID creatorId = squads.get(0).leaderId();
        TacticalMarker marker = new TacticalMarker(UUID.nameUUIDFromBytes(new byte[]{1}),
                Faction.BLUE, TacticalMarkerType.TANK, OVERWORLD,
                100.0D, 64.0D, 200.0D, 100.0D, 200.0D,
                creatorId, SquadCallsign.ALPHA, 1_000L, 2_000L);
        BattleSnapshot original = snapshot(squads, positions, List.of(marker),
                LEADER_PERMISSIONS, emptyDeployment(), 40, 40);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BattleSnapshotCodec.encode(buffer, original);
            BattleSnapshot decoded = BattleSnapshotCodec.decode(buffer);

            assertEquals(original, decoded);
            assertEquals(5, decoded.squads().size());
            assertEquals(40, decoded.squads().stream()
                    .mapToInt(squad -> squad.members().size()).sum());
            assertEquals(40, decoded.alliedPositions().size());
            assertEquals(LEADER_PERMISSIONS, decoded.permissions());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void encoderRejectsSixthSquadAndNinthMember() {
        List<SquadView> sixSquads = new ArrayList<>(fullFactionSquads());
        sixSquads.add(new SquadView(SquadCallsign.ALPHA, null, List.of(),
                BattleRules.SQUAD_CAPACITY));
        assertEncodeRejected(snapshot(sixSquads, List.of(), List.of(), LEADER_PERMISSIONS,
                emptyDeployment(), 40, 40));

        List<MemberView> nineMembers = membersFor(SquadCallsign.ALPHA, 9, 100);
        SquadView oversizedSquad = new SquadView(SquadCallsign.ALPHA,
                nineMembers.get(0).playerId(), nineMembers, BattleRules.SQUAD_CAPACITY);
        assertEncodeRejected(snapshot(List.of(oversizedSquad), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 9, 0));
    }

    @Test
    void encoderRejectsMoreThanFortyAlliedPositions() {
        List<MemberPosition> positions = new ArrayList<>();
        for (int index = 0; index <= BattleRules.FACTION_CAPACITY; index++) {
            positions.add(new MemberPosition(playerId(1_000 + index), OVERWORLD,
                    index, 64.0D, index, 0.0F));
        }

        assertEncodeRejected(snapshot(List.of(), positions, List.of(), LEADER_PERMISSIONS,
                emptyDeployment(), 40, 0));
    }

    @Test
    void rosterBoundaryRejectsContradictorySameSquadRelationships() {
        MemberView alphaViewer = member(playerId(1), "Viewer", SquadCallsign.ALPHA,
                true, true);
        SquadView alpha = new SquadView(SquadCallsign.ALPHA, alphaViewer.playerId(),
                List.of(alphaViewer), BattleRules.SQUAD_CAPACITY);

        SquadView duplicateAlpha = new SquadView(SquadCallsign.ALPHA, null, List.of(),
                BattleRules.SQUAD_CAPACITY);
        assertEncodeRejected(snapshot(List.of(alpha, duplicateAlpha), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 2, 0));

        MemberView duplicateBravo = member(playerId(1), "Duplicate", SquadCallsign.BRAVO,
                true, false);
        SquadView bravoWithDuplicate = new SquadView(SquadCallsign.BRAVO,
                duplicateBravo.playerId(), List.of(duplicateBravo), BattleRules.SQUAD_CAPACITY);
        assertEncodeRejected(snapshot(List.of(alpha, bravoWithDuplicate), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 2, 0));

        MemberView mismatched = member(playerId(2), "Mismatch", SquadCallsign.BRAVO,
                true, false);
        SquadView mismatchedContainer = new SquadView(SquadCallsign.ALPHA,
                mismatched.playerId(), List.of(mismatched), BattleRules.SQUAD_CAPACITY);
        assertEncodeRejected(snapshot(List.of(mismatchedContainer), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 1, 0));

        BattleSnapshot missingViewer = new BattleSnapshot(playerId(1), Faction.BLUE,
                SquadCallsign.ALPHA, true, false, 1, 0,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY,
                List.of(new SquadView(SquadCallsign.ALPHA, playerId(2),
                        List.of(member(playerId(2), "Other", SquadCallsign.ALPHA,
                                true, false)), BattleRules.SQUAD_CAPACITY)),
                List.of(), List.of(), LEADER_PERMISSIONS, List.of(), emptyDeployment(),
                5_000L, 7L);
        assertEncodeRejected(missingViewer);
    }

    @Test
    void deploymentBoundaryRejectsEnemyOrUnissuedSelection() {
        DeploymentPoint enemyPoint = new DeploymentPoint(playerId(2_001), Faction.RED,
                OVERWORLD, new BlockPos(0, 64, 0), 0.0F,
                DeploymentPoint.DEFAULT_SUPPLY_RADIUS);
        DeploymentView enemyDeployment = deployment(enemyPoint.id(), List.of(enemyPoint));
        assertEncodeRejected(snapshot(List.of(), List.of(), List.of(), LEADER_PERMISSIONS,
                enemyDeployment, 0, 0));

        DeploymentPoint alliedPoint = new DeploymentPoint(playerId(2_002), Faction.BLUE,
                OVERWORLD, new BlockPos(0, 64, 0), 0.0F,
                DeploymentPoint.DEFAULT_SUPPLY_RADIUS);
        DeploymentView unissuedSelection = deployment(playerId(2_003), List.of(alliedPoint));
        assertEncodeRejected(snapshot(List.of(), List.of(), List.of(), LEADER_PERMISSIONS,
                unissuedSelection, 0, 0));
    }

    @Test
    void mainBaseAndFieldBeaconKindsRoundTrip() {
        DeploymentPoint mainBase = new DeploymentPoint(playerId(2_101), Faction.BLUE,
                OVERWORLD, new BlockPos(0, 64, 0), 0.0F,
                DeploymentPoint.DEFAULT_SUPPLY_RADIUS, DeploymentPointKind.MAIN_BASE);
        DeploymentPoint fieldBeacon = new DeploymentPoint(playerId(2_102), Faction.BLUE,
                OVERWORLD, new BlockPos(32, 65, 0), 180.0F,
                DeploymentPoint.DEFAULT_SUPPLY_RADIUS, DeploymentPointKind.FIELD_BEACON);
        BattleSnapshot original = snapshot(List.of(), List.of(), List.of(), LEADER_PERMISSIONS,
                deployment(fieldBeacon.id(), List.of(mainBase, fieldBeacon)), 0, 0);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BattleSnapshotCodec.encode(buffer, original);
            BattleSnapshot decoded = BattleSnapshotCodec.decode(buffer);

            assertEquals(List.of(mainBase, fieldBeacon), decoded.deployment().points());
            assertEquals(DeploymentPointKind.FIELD_BEACON,
                    decoded.deployment().points().get(1).kind());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void snapshotBoundaryRejectsEnemyTacticalMarker() {
        TacticalMarker enemyMarker = new TacticalMarker(playerId(3_001), Faction.RED,
                TacticalMarkerType.INFANTRY, OVERWORLD,
                100.0D, 64.0D, 200.0D, 100.0D, 200.0D,
                playerId(3_002), SquadCallsign.BRAVO, 1_000L, 2_000L);

        assertEncodeRejected(snapshot(List.of(), List.of(), List.of(enemyMarker),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0));
    }

    @Test
    void supportViewRoundTripsAndDeploymentReplacementPreservesIt() {
        UUID callId = playerId(4_001);
        SupportView support = new SupportView(List.of(
                option(POINT_SUPPORT, SupportTargetMode.POINT,
                        true, "", 2_400L, false),
                option(DIRECTIONAL_SUPPORT, SupportTargetMode.DIRECTIONAL,
                        true, "", 3_600L, true),
                option(SECOND_DIRECTIONAL_SUPPORT, SupportTargetMode.DIRECTIONAL,
                        false, "provider missing", 0L, false)),
                List.of(new SupportMissionView(callId,
                        DIRECTIONAL_SUPPORT, OVERWORLD,
                        10.5D, -20.25D, 80.5D, -20.25D, 140L, 4)),
                100L, 9L, true, "");
        BattleSnapshot original = snapshot(List.of(), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0).withSupport(support);
        DeploymentPoint point = new DeploymentPoint(playerId(4_002), Faction.BLUE,
                OVERWORLD, new BlockPos(8, 64, 8), 45.0F,
                DeploymentPoint.DEFAULT_SUPPLY_RADIUS);
        BattleSnapshot withDeployment = original.withDeployment(
                deployment(point.id(), List.of(point)));

        assertEquals(support, withDeployment.support(),
                "withDeployment must not discard faction-filtered support state");

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BattleSnapshotCodec.encode(buffer, withDeployment);
            BattleSnapshot decoded = BattleSnapshotCodec.decode(buffer);

            assertEquals(withDeployment, decoded);
            assertEquals(support, decoded.support());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void dynamicSupportCatalogUsesCurrentProtocolAndItsExactWireLimit() {
        assertEquals("18", BattleNetwork.PROTOCOL_VERSION);
        assertEquals(32, BattleNetworkLimits.MAX_SUPPORT_OPTIONS);

        List<SupportOptionView> options = new ArrayList<>();
        for (int index = 0; index <= BattleNetworkLimits.MAX_SUPPORT_OPTIONS; index++) {
            ResourceLocation supportId = ResourceLocation.fromNamespaceAndPath(
                    "wok_infantry", "catalog_boundary_" + index);
            options.add(option(supportId, SupportTargetMode.POINT,
                    true, "", 0L, false));
        }

        BattleSnapshot atLimit = snapshot(List.of(), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0).withSupport(
                new SupportView(options.subList(0,
                        BattleNetworkLimits.MAX_SUPPORT_OPTIONS), List.of(),
                        100L, 1L, true, ""));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BattleSnapshotCodec.encode(buffer, atLimit);
            BattleSnapshot decoded = BattleSnapshotCodec.decode(buffer);
            assertEquals(BattleNetworkLimits.MAX_SUPPORT_OPTIONS,
                    decoded.support().options().size());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }

        BattleSnapshot aboveLimit = snapshot(List.of(), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0).withSupport(
                new SupportView(options, List.of(), 100L, 2L, true, ""));
        assertEncodeRejected(aboveLimit);
    }

    @Test
    void supportEncoderRejectsMissionAndFieldOverflows() {
        List<SupportOptionView> tooManyOptions = new ArrayList<>();
        List<SupportMissionView> tooManyMissions = new ArrayList<>();
        for (int index = 0;
             index <= BattleNetworkLimits.MAX_ACTIVE_SUPPORT_MISSIONS; index++) {
            ResourceLocation supportId = ResourceLocation.fromNamespaceAndPath(
                    "wok_infantry", "test_dynamic_" + index);
            tooManyOptions.add(option(supportId, SupportTargetMode.DIRECTIONAL,
                    true, "", 0L, true));
            tooManyMissions.add(new SupportMissionView(playerId(4_100 + index),
                    supportId, OVERWORLD,
                    0.0D, 0.0D, 20.0D, 0.0D, 200L, 1));
        }
        assertEncodeRejected(snapshot(List.of(), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0).withSupport(
                new SupportView(tooManyOptions, tooManyMissions, 100L, 1L,
                        true, "")));

        SupportOptionView activeDirectional = option(DIRECTIONAL_SUPPORT,
                SupportTargetMode.DIRECTIONAL, true, "", 1_800L, true);

        SupportMissionView outsideWorldBoundary = new SupportMissionView(playerId(4_200),
                DIRECTIONAL_SUPPORT, OVERWORLD,
                BattleNetworkLimits.MAX_COORDINATE + 1.0D, 0.0D,
                0.0D, 0.0D, 200L, 1);
        assertEncodeRejected(snapshot(List.of(), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0).withSupport(
                new SupportView(List.of(activeDirectional), List.of(outsideWorldBoundary),
                        100L, 2L, true, "")));

        SupportOptionView activePoint = option(POINT_SUPPORT, SupportTargetMode.POINT,
                true, "", 2_400L, true);
        SupportMissionView nonNormalizedPoint = new SupportMissionView(playerId(4_201),
                POINT_SUPPORT, OVERWORLD,
                1.0D, 2.0D, 3.0D, 4.0D, 200L, 1);
        assertEncodeRejected(snapshot(List.of(), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0).withSupport(
                new SupportView(List.of(activePoint), List.of(nonNormalizedPoint),
                        100L, 3L, true, "")));

        SupportMissionView tooShortDirection = new SupportMissionView(playerId(4_204),
                DIRECTIONAL_SUPPORT, OVERWORLD,
                0.0D, 0.0D, 15.999D, 0.0D, 200L, 1);
        assertEncodeRejected(snapshot(List.of(), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0).withSupport(
                new SupportView(List.of(activeDirectional), List.of(tooShortDirection),
                        100L, 4L, true, "")));

        SupportMissionView tooLongDirection = new SupportMissionView(playerId(4_205),
                DIRECTIONAL_SUPPORT, OVERWORLD,
                0.0D, 0.0D, 512.001D, 0.0D, 200L, 1);
        assertEncodeRejected(snapshot(List.of(), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0).withSupport(
                new SupportView(List.of(activeDirectional), List.of(tooLongDirection),
                        100L, 5L, true, "")));

        List<SupportMissionView> duplicateSupportId = List.of(
                new SupportMissionView(playerId(4_202), DIRECTIONAL_SUPPORT,
                        OVERWORLD, 0.0D, 0.0D, 20.0D, 0.0D, 200L, 2),
                new SupportMissionView(playerId(4_203), DIRECTIONAL_SUPPORT,
                        OVERWORLD, 20.0D, 0.0D, 40.0D, 0.0D, 220L, 2));
        assertThrows(IllegalArgumentException.class, () -> new SupportView(
                List.of(activeDirectional), duplicateSupportId, 100L, 6L,
                true, ""));
    }

    @Test
    void supportDecoderRejectsDuplicateTypesMissionsAndListOverflows() {
        assertMalformedSupportRejected(buffer -> {
            writeRawSupportHeader(buffer, true, "");
            buffer.writeVarInt(2);
            writeRawOption(buffer, POINT_SUPPORT, SupportTargetMode.POINT, false);
            writeRawOption(buffer, POINT_SUPPORT, SupportTargetMode.POINT, false);
        });

        UUID duplicateCallId = playerId(4_300);
        assertMalformedSupportRejected(buffer -> {
            writeRawSupportHeader(buffer, true, "");
            buffer.writeVarInt(2);
            writeRawOption(buffer, DIRECTIONAL_SUPPORT,
                    SupportTargetMode.DIRECTIONAL, true);
            writeRawOption(buffer, SECOND_DIRECTIONAL_SUPPORT,
                    SupportTargetMode.DIRECTIONAL, true);
            buffer.writeVarInt(2);
            writeRawMission(buffer, duplicateCallId, DIRECTIONAL_SUPPORT);
            buffer.writeUUID(duplicateCallId);
        });

        assertMalformedSupportRejected(buffer -> {
            writeRawSupportHeader(buffer, true, "");
            buffer.writeVarInt(BattleNetworkLimits.MAX_SUPPORT_OPTIONS + 1);
        });
        assertMalformedSupportRejected(buffer -> {
            writeRawSupportHeader(buffer, true, "");
            buffer.writeVarInt(0);
            buffer.writeVarInt(BattleNetworkLimits.MAX_ACTIVE_SUPPORT_MISSIONS + 1);
        });
        assertMalformedSupportRejected(buffer -> {
            writeRawSupportHeader(buffer, true, "");
            buffer.writeVarInt(1);
            writeRawOption(buffer, POINT_SUPPORT, SupportTargetMode.POINT, false);
            buffer.writeVarInt(1);
            buffer.writeUUID(playerId(4_301));
            buffer.writeUtf(DIRECTIONAL_SUPPORT.toString(),
                    BattleNetworkLimits.MAX_SUPPORT_ID_LENGTH);
        });
    }

    @Test
    void completeSnapshotPacketRejectsTrailingPayload() {
        BattleSnapshotPacket packet = new BattleSnapshotPacket(
                snapshot(List.of(), List.of(), List.of(), LEADER_PERMISSIONS,
                        emptyDeployment(), 0, 0),
                BattleOpenTarget.MAP);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BattleSnapshotPacket.encode(packet, buffer);
            buffer.writeByte(0x7F);

            assertThrows(IllegalArgumentException.class,
                    () -> BattleSnapshotPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    private static List<SquadView> fullFactionSquads() {
        List<SquadView> squads = new ArrayList<>();
        int firstPlayerIndex = 1;
        for (SquadCallsign callsign : SquadCallsign.values()) {
            List<MemberView> members = membersFor(callsign, BattleRules.SQUAD_CAPACITY,
                    firstPlayerIndex);
            squads.add(new SquadView(callsign, members.get(0).playerId(), members,
                    BattleRules.SQUAD_CAPACITY));
            firstPlayerIndex += BattleRules.SQUAD_CAPACITY;
        }
        return squads;
    }

    private static List<MemberView> membersFor(SquadCallsign callsign, int count,
                                                int firstPlayerIndex) {
        List<MemberView> members = new ArrayList<>();
        for (int offset = 0; offset < count; offset++) {
            int playerIndex = firstPlayerIndex + offset;
            members.add(member(playerId(playerIndex), "Player" + playerIndex, callsign,
                    offset == 0, callsign == SquadCallsign.ALPHA && offset == 0));
        }
        return members;
    }

    private static MemberView member(UUID playerId, String name, SquadCallsign callsign,
                                     boolean leader, boolean commander) {
        return new MemberView(playerId, name, true, true, 20.0F, 20.0F,
                leader, commander, callsign, leader ? "support" : "assault");
    }

    private static BattleSnapshot snapshot(List<SquadView> squads,
                                             List<MemberPosition> positions,
                                             List<TacticalMarker> markers,
                                             PermissionView permissions,
                                             DeploymentView deployment,
                                             int factionMemberCount,
                                             int enemyFactionMemberCount) {
        MemberView viewer = squads.stream().flatMap(squad -> squad.members().stream())
                .filter(member -> member.playerId().equals(playerId(1)))
                .findFirst().orElse(null);
        SquadCallsign ownSquad = viewer == null ? null : viewer.squad();
        return new BattleSnapshot(playerId(1), Faction.BLUE, ownSquad,
                viewer != null && viewer.leader(), viewer != null && viewer.commander(),
                factionMemberCount, enemyFactionMemberCount,
                BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY,
                squads, positions, markers, permissions,
                Arrays.stream(new ClassQuotaView[]{
                        new ClassQuotaView("assault", "突击兵", 8, 7),
                        new ClassQuotaView("support", "支援兵", 2, 1)})
                        .toList(),
                deployment, 5_000L, 7L);
    }

    private static DeploymentView emptyDeployment() {
        return deployment(null, List.of());
    }

    private static DeploymentView deployment(UUID selectedPointId,
                                              List<DeploymentPoint> points) {
        return new DeploymentView(DeploymentPhase.READY, 1L,
                100L, 100L, 200L, selectedPointId,
                true, true, true, false, points);
    }

    private static void assertEncodeRejected(BattleSnapshot snapshot) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> BattleSnapshotCodec.encode(buffer, snapshot));
        } finally {
            buffer.release();
        }
    }

    private static void assertMalformedSupportRejected(Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf malformed = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BattleSnapshotCodec.encode(encoded, snapshot(List.of(), List.of(), List.of(),
                    LEADER_PERMISSIONS, emptyDeployment(), 0, 0));
            skipToSupport(encoded);
            malformed.writeBytes(encoded, 0, encoded.readerIndex());
            writer.accept(malformed);
            assertThrows(IllegalArgumentException.class,
                    () -> BattleSnapshotCodec.decode(malformed));
        } finally {
            encoded.release();
            malformed.release();
        }
    }

    private static void skipToSupport(FriendlyByteBuf buffer) {
        buffer.readUUID();
        if (buffer.readBoolean()) {
            buffer.readUtf(BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        }
        if (buffer.readBoolean()) {
            buffer.readUtf(BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        }
        buffer.readBoolean();
        buffer.readBoolean();
        buffer.readVarInt();
        buffer.readVarInt();
        buffer.readVarInt();
        buffer.readVarInt();
        if (buffer.readVarInt() != 0 || buffer.readVarInt() != 0
                || buffer.readVarInt() != 0) {
            throw new AssertionError("Malformed-support fixture must have empty battle lists");
        }
        for (int index = 0; index < 7; index++) {
            buffer.readBoolean();
        }
        int quotaCount = buffer.readVarInt();
        for (int index = 0; index < quotaCount; index++) {
            buffer.readUtf(BattleNetworkLimits.MAX_CLASS_ID_LENGTH);
            buffer.readUtf(BattleNetworkLimits.MAX_CLASS_DISPLAY_NAME_LENGTH);
            buffer.readVarInt();
            buffer.readVarInt();
        }
    }

    private static SupportOptionView option(ResourceLocation id, SupportTargetMode mode,
                                            boolean available, String reason,
                                            long readyAt, boolean active) {
        return new SupportOptionView(id, "support." + id.getNamespace() + "."
                + id.getPath(), "Fixture support", "Fixture", mode, 16.0D,
                available, reason, readyAt, active);
    }

    private static void writeRawSupportHeader(FriendlyByteBuf buffer,
                                              boolean serviceAvailable,
                                              String serviceMessage) {
        buffer.writeBoolean(serviceAvailable);
        buffer.writeUtf(serviceMessage,
                BattleNetworkLimits.MAX_SUPPORT_SERVICE_MESSAGE_LENGTH);
    }

    private static void writeRawOption(FriendlyByteBuf buffer, ResourceLocation id,
                                       SupportTargetMode mode, boolean active) {
        buffer.writeUtf(id.toString(), BattleNetworkLimits.MAX_SUPPORT_ID_LENGTH);
        buffer.writeUtf("support." + id.getNamespace() + "." + id.getPath(),
                BattleNetworkLimits.MAX_SUPPORT_TRANSLATION_KEY_LENGTH);
        buffer.writeUtf("Fixture support",
                BattleNetworkLimits.MAX_SUPPORT_FALLBACK_NAME_LENGTH);
        buffer.writeUtf("Fixture", BattleNetworkLimits.MAX_SUPPORT_SHORT_NAME_LENGTH);
        buffer.writeUtf(mode.name().toLowerCase(java.util.Locale.ROOT),
                BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        buffer.writeDouble(16.0D);
        buffer.writeBoolean(true);
        buffer.writeUtf("", BattleNetworkLimits.MAX_SUPPORT_REASON_LENGTH);
        buffer.writeLong(0L);
        buffer.writeBoolean(active);
    }

    private static void writeRawMission(FriendlyByteBuf buffer, UUID callId,
                                        ResourceLocation supportId) {
        buffer.writeUUID(callId);
        buffer.writeUtf(supportId.toString(), BattleNetworkLimits.MAX_SUPPORT_ID_LENGTH);
        buffer.writeUtf(OVERWORLD.toString(), BattleNetworkLimits.MAX_DIMENSION_ID_LENGTH);
        buffer.writeDouble(0.0D);
        buffer.writeDouble(0.0D);
        buffer.writeDouble(20.0D);
        buffer.writeDouble(0.0D);
        buffer.writeLong(100L);
        buffer.writeVarInt(1);
    }

    private static UUID playerId(int index) {
        return new UUID(0L, index);
    }
}
