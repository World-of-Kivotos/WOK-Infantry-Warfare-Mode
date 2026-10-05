package com.wok.infantry.network.battle;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.ClassLimitView;
import com.wok.infantry.battle.ClassQuotaView;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.FormationContextView;
import com.wok.infantry.battle.KickCooldownView;
import com.wok.infantry.battle.MemberPosition;
import com.wok.infantry.battle.MemberState;
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
    void readOnlyMissionOfAnotherFormationRoundTrips() {
        // map-render-01: an inbound strike the viewer's formation does not open travels as an
        // "active but unavailable" option so the teammate still draws the danger area.
        UUID callId = playerId(4_010);
        SupportOptionView readOnly = option(DIRECTIONAL_SUPPORT, SupportTargetMode.DIRECTIONAL,
                false, "当前编制未开放该支援", 3_600L, true);
        SupportView support = new SupportView(List.of(
                option(POINT_SUPPORT, SupportTargetMode.POINT, true, "", 0L, false),
                readOnly),
                List.of(new SupportMissionView(callId, DIRECTIONAL_SUPPORT, OVERWORLD,
                        10.5D, -20.25D, 80.5D, -20.25D, 140L, 4)),
                100L, 11L, true, "");
        BattleSnapshot original = snapshot(List.of(), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0).withSupport(support);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BattleSnapshotCodec.encode(buffer, original);
            BattleSnapshot decoded = BattleSnapshotCodec.decode(buffer);

            assertEquals(support, decoded.support());
            assertEquals(true, decoded.support().options().get(1).readOnlyMission());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
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
        assertEquals("20", BattleNetwork.PROTOCOL_VERSION);
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
    void everyMemberStateAndHealthRatioRoundTrips() {
        assertEquals("20", BattleNetwork.PROTOCOL_VERSION);
        SquadView squad = new SquadView(SquadCallsign.ALPHA, playerId(1), List.of(
                stateMember(1, true, MemberState.DEPLOYED, 0.75F),
                stateMember(2, false, MemberState.DOWNED, 0.05F),
                stateMember(3, false, MemberState.DEAD, MemberView.UNKNOWN_HEALTH_RATIO),
                stateMember(4, false, MemberState.WAITING, MemberView.UNKNOWN_HEALTH_RATIO),
                stateMember(5, false, MemberState.OFFLINE, MemberView.UNKNOWN_HEALTH_RATIO),
                stateMember(6, false, MemberState.DEPLOYED, MemberView.UNKNOWN_HEALTH_RATIO),
                stateMember(7, false, MemberState.DEPLOYED, 0.0F),
                stateMember(8, false, MemberState.DEPLOYED, 1.0F)),
                BattleRules.SQUAD_CAPACITY);
        BattleSnapshot original = snapshot(List.of(squad), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 8, 0);

        BattleSnapshot decoded = decodeBytes(encodeBytes(original));

        assertEquals(original, decoded);
        List<MemberView> members = decoded.squads().get(0).members();
        assertEquals(List.of(MemberState.DEPLOYED, MemberState.DOWNED, MemberState.DEAD,
                        MemberState.WAITING, MemberState.OFFLINE, MemberState.DEPLOYED,
                        MemberState.DEPLOYED, MemberState.DEPLOYED),
                members.stream().map(MemberView::state).toList());
        assertEquals(0.75F, members.get(0).healthRatio());
        assertEquals(0.05F, members.get(1).healthRatio());
        assertEquals(MemberView.UNKNOWN_HEALTH_RATIO, members.get(5).healthRatio());
        assertEquals(false, members.get(5).hasHealthRatio());
    }

    @Test
    void encoderRejectsMemberStateThatContradictsLegacyFlags() {
        MemberView offlineButMarkedOnline = new MemberView(playerId(2), "Ghost", true, false,
                0.0F, 20.0F, false, false, SquadCallsign.ALPHA, "assault",
                MemberState.OFFLINE, MemberView.UNKNOWN_HEALTH_RATIO);
        MemberView deployedButNotAlive = new MemberView(playerId(2), "Ghost", true, false,
                0.0F, 20.0F, false, false, SquadCallsign.ALPHA, "assault",
                MemberState.DEPLOYED, 0.5F);
        MemberView waitingButAlive = new MemberView(playerId(2), "Ghost", true, true,
                20.0F, 20.0F, false, false, SquadCallsign.ALPHA, "assault",
                MemberState.WAITING, MemberView.UNKNOWN_HEALTH_RATIO);
        for (MemberView contradictory : List.of(offlineButMarkedOnline, deployedButNotAlive,
                waitingButAlive)) {
            SquadView squad = new SquadView(SquadCallsign.ALPHA, playerId(1), List.of(
                    stateMember(1, true, MemberState.DEPLOYED, 1.0F), contradictory),
                    BattleRules.SQUAD_CAPACITY);
            assertEncodeRejected(snapshot(List.of(squad), List.of(), List.of(),
                    LEADER_PERMISSIONS, emptyDeployment(), 2, 0));
        }
    }

    @Test
    void decoderRejectsOutOfBoundsHealthRatioAndMemberState() {
        float marker = 0.123F;
        byte[] valid = encodeBytes(singleMemberSnapshot(MemberState.DEPLOYED, marker));
        byte[] markerBits = floatBytes(marker);

        for (float invalid : new float[]{1.0001F, 1.5F, -0.5F, -1.0001F, Float.NaN,
                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            byte[] malformed = replaceOnce(valid, markerBits, floatBytes(invalid));
            assertThrows(IllegalArgumentException.class, () -> decodeBytes(malformed),
                    "ratio " + invalid + " must be rejected");
        }
        assertEquals(1.0F, decodeBytes(replaceOnce(valid, markerBits, floatBytes(1.0F)))
                .squads().get(0).members().get(0).healthRatio());
        assertEquals(MemberView.UNKNOWN_HEALTH_RATIO, decodeBytes(replaceOnce(valid,
                markerBits, floatBytes(MemberView.UNKNOWN_HEALTH_RATIO)))
                .squads().get(0).members().get(0).healthRatio());

        byte[] stateField = utfBytes(MemberState.DEPLOYED.id());
        byte[] unknownState = replaceOnce(valid, stateField, utfBytes("deployex"));
        assertThrows(IllegalArgumentException.class, () -> decodeBytes(unknownState));

        byte[] contradictoryState = replaceOnce(valid, stateField,
                utfBytes(MemberState.DEAD.id()));
        assertThrows(IllegalArgumentException.class, () -> decodeBytes(contradictoryState),
                "an online, alive member cannot be reported dead");

        byte[] overlongState = replaceOnce(valid, stateField,
                utfBytes("x".repeat(BattleNetworkLimits.MAX_ENUM_ID_LENGTH + 1)));
        assertThrows(RuntimeException.class, () -> decodeBytes(overlongState),
                "state ids longer than the enum limit must not be read");
    }

    @Test
    void protocolTwentyViewerContextAndSquadClassLimitsRoundTrip() {
        assertEquals("20", BattleNetwork.PROTOCOL_VERSION);
        SquadView alpha = limitedSquad(SquadCallsign.ALPHA, 1, 3, 6,
                List.of(new ClassLimitView("support", 1, 1),
                        new ClassLimitView("assault", 4, 2),
                        new ClassLimitView("medic", 0, 0)));
        SquadView bravo = limitedSquad(SquadCallsign.BRAVO, 10, 2, 4,
                List.of(new ClassLimitView("support", 1, 1),
                        new ClassLimitView("assault", 3, 1)));
        FormationContextView context = new FormationContextView("millennium_seminar_mobile",
                "千禧年研讨会机动部队", "assault", "academy", "学院军", 40, "kaiser", "凯撒", 36);
        List<KickCooldownView> cooldowns = List.of(
                new KickCooldownView(SquadCallsign.CHARLIE, 5_000L + 59_000L),
                new KickCooldownView(SquadCallsign.BRAVO, 5_000L + 1L));
        BattleSnapshot original = snapshot(List.of(alpha, bravo), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 5, 12)
                .withViewerContext(context, "support", cooldowns);

        BattleSnapshot decoded = decodeBytes(encodeBytes(original));

        assertEquals(original, decoded);
        assertEquals(context, decoded.formationContext());
        assertEquals("support", decoded.viewerClassId());
        assertEquals(List.of(SquadCallsign.BRAVO, SquadCallsign.CHARLIE),
                decoded.kickCooldowns().stream().map(KickCooldownView::squad).toList(),
                "cooldowns are kept in call sign order");
        assertEquals(2, decoded.squad(SquadCallsign.ALPHA).classLimit("assault").used());
        assertEquals(true, decoded.squad(SquadCallsign.ALPHA).classLimit("medic").closed());
        assertEquals(59_000L, decoded.kickCooldownRemainingMillis(SquadCallsign.CHARLIE,
                5_000L));
        assertEquals(0L, decoded.kickCooldownRemainingMillis(SquadCallsign.ALPHA, 5_000L));
        assertEquals(true, decoded.formationLocked());
    }

    @Test
    void compatibilityConstructorsKeepTheProtocolNineteenShape() {
        SquadView squad = new SquadView(SquadCallsign.ALPHA, playerId(1),
                List.of(member(playerId(1), "Viewer", SquadCallsign.ALPHA, true, false)),
                BattleRules.SQUAD_CAPACITY);
        BattleSnapshot legacy = snapshot(List.of(squad), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 1, 0);

        assertEquals(List.of(), squad.classLimits());
        assertEquals(FormationContextView.EMPTY, legacy.formationContext());
        assertEquals("support", legacy.viewerClassId(),
                "the 18-argument constructor takes the viewer class from the roster");
        assertEquals(List.of(), legacy.kickCooldowns());
        assertEquals(legacy, decodeBytes(encodeBytes(legacy)));

        BattleSnapshot unassigned = snapshot(List.of(), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 0, 0);
        assertEquals("", unassigned.viewerClassId());
        assertEquals(false, unassigned.formationLocked());
        assertEquals(unassigned, decodeBytes(encodeBytes(unassigned)));
    }

    @Test
    void encoderRejectsContradictorySquadClassLimits() {
        // used must count exactly the members holding the class
        assertEncodeRejected(snapshot(List.of(limitedSquad(SquadCallsign.ALPHA, 1, 2, 8,
                        List.of(new ClassLimitView("assault", 4, 2)))), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 2, 0));
        // a limit above the squad's own capacity
        assertEncodeRejected(snapshot(List.of(limitedSquad(SquadCallsign.ALPHA, 1, 2, 4,
                        List.of(new ClassLimitView("assault", 5, 1)))), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 2, 0));
        // the same class twice
        assertEncodeRejected(snapshot(List.of(limitedSquad(SquadCallsign.ALPHA, 1, 2, 8,
                        List.of(new ClassLimitView("assault", 4, 1),
                                new ClassLimitView("assault", 2, 1)))), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 2, 0));
        // more class limits than the wire allows
        List<ClassLimitView> tooMany = new ArrayList<>();
        for (int index = 0; index <= BattleNetworkLimits.MAX_SQUAD_CLASS_LIMITS; index++) {
            tooMany.add(new ClassLimitView("class_" + index, 0, 0));
        }
        assertEncodeRejected(snapshot(List.of(limitedSquad(SquadCallsign.ALPHA, 1, 1, 8,
                        tooMany)), List.of(), List.of(), LEADER_PERMISSIONS,
                emptyDeployment(), 1, 0));
    }

    @Test
    void encoderRejectsInvalidViewerContext() {
        SquadView squad = limitedSquad(SquadCallsign.ALPHA, 1, 2, 8, List.of());
        BattleSnapshot base = snapshot(List.of(squad), List.of(), List.of(),
                LEADER_PERMISSIONS, emptyDeployment(), 2, 0);

        assertEncodeRejected(base.withViewerContext(FormationContextView.EMPTY, "assault",
                List.of()));
        assertEncodeRejected(base.withViewerContext(FormationContextView.EMPTY, "Bad Id",
                List.of()));
        // an expired cooldown is never sent
        assertEncodeRejected(base.withViewerContext(FormationContextView.EMPTY, "support",
                List.of(new KickCooldownView(SquadCallsign.BRAVO, 5_000L))));
        assertEncodeRejected(base.withViewerContext(FormationContextView.EMPTY, "support",
                List.of(new KickCooldownView(SquadCallsign.BRAVO,
                        5_000L + BattleNetworkLimits.MAX_KICK_COOLDOWN_MILLIS + 1L))));
        assertEncodeRejected(base.withViewerContext(FormationContextView.EMPTY, "support",
                List.of(new KickCooldownView(SquadCallsign.BRAVO, 6_000L),
                        new KickCooldownView(SquadCallsign.BRAVO, 7_000L))));

        BattleSnapshot unassigned = new BattleSnapshot(playerId(1), null, null, false, false,
                0, 0, BattleRules.FACTION_CAPACITY, BattleRules.SQUAD_CAPACITY, List.of(),
                List.of(), List.of(), LEADER_PERMISSIONS, List.of(), emptyDeployment(),
                5_000L, 7L);
        assertEncodeRejected(unassigned.withViewerContext(FormationContextView.EMPTY, "",
                List.of(new KickCooldownView(SquadCallsign.ALPHA, 6_000L))));
    }

    @Test
    void formationContextIsClippedBeforeItCanBreakTheEncoder() {
        String longName = "名".repeat(FormationContextView.MAX_NAME_LENGTH + 20);
        FormationContextView context = new FormationContextView("Default", longName + "\n",
                "ASSAULT", "academy", "学院军\u0007", 400, "kaiser!", "凯撒", -3);

        assertEquals("default", context.formationId());
        assertEquals(FormationContextView.MAX_NAME_LENGTH, context.formationName().length());
        assertEquals("assault", context.defaultClassId());
        assertEquals("学院军", context.factionName());
        assertEquals(FormationContextView.MAX_CAPACITY, context.factionCapacity());
        assertEquals("", context.enemyFactionId(), "malformed ids become empty");
        assertEquals(0, context.enemyFactionCapacity());

        BattleSnapshot snapshot = snapshot(List.of(), List.of(), List.of(), LEADER_PERMISSIONS,
                emptyDeployment(), 0, 0).withFormationContext(context);
        assertEquals(snapshot, decodeBytes(encodeBytes(snapshot)));
    }

    @Test
    void decoderRejectsUnsanitizedContextTextAndOverlongCooldownLists() {
        FormationContextView context = new FormationContextView("default", "常规编制",
                "assault", "academy", "Academy", 40, "kaiser", "Kaiser", 40);
        BattleSnapshot snapshot = snapshot(List.of(), List.of(), List.of(), LEADER_PERMISSIONS,
                emptyDeployment(), 0, 0).withFormationContext(context);
        byte[] valid = encodeBytes(snapshot);

        byte[] upperCaseId = replaceOnce(valid, utfBytes("academy"), utfBytes("ACADEMY"));
        assertThrows(IllegalArgumentException.class, () -> decodeBytes(upperCaseId));
        byte[] paddedName = replaceOnce(valid, utfBytes("Academy"), utfBytes(" Acad "));
        assertThrows(IllegalArgumentException.class, () -> decodeBytes(paddedName));

        // last byte is the cooldown count (0); a count above the call sign total is refused
        byte[] tooManyCooldowns = valid.clone();
        tooManyCooldowns[tooManyCooldowns.length - 1] =
                (byte) (BattleNetworkLimits.MAX_KICK_COOLDOWNS + 1);
        assertThrows(IllegalArgumentException.class, () -> decodeBytes(tooManyCooldowns));
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

    /** A squad whose first member leads (class support) and the rest are assault. */
    private static SquadView limitedSquad(SquadCallsign callsign, int firstPlayerIndex,
                                          int memberCount, int capacity,
                                          List<ClassLimitView> limits) {
        List<MemberView> members = new ArrayList<>();
        for (int offset = 0; offset < memberCount; offset++) {
            int playerIndex = firstPlayerIndex + offset;
            members.add(member(playerId(playerIndex), "Player" + playerIndex, callsign,
                    offset == 0, false));
        }
        return new SquadView(callsign, members.get(0).playerId(), members, capacity, limits);
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

    /** A member whose legacy flags agree with {@code state}. */
    private static MemberView stateMember(int index, boolean leader, MemberState state,
                                          float ratio) {
        boolean online = state != MemberState.OFFLINE;
        boolean alive = state.hasVitals();
        return new MemberView(playerId(index), "Player" + index, online, alive,
                alive ? 10.0F : 0.0F, 20.0F, leader, false, SquadCallsign.ALPHA,
                leader ? "support" : "assault", state, ratio);
    }

    private static BattleSnapshot singleMemberSnapshot(MemberState state, float ratio) {
        SquadView squad = new SquadView(SquadCallsign.ALPHA, playerId(1),
                List.of(stateMember(1, true, state, ratio)), BattleRules.SQUAD_CAPACITY);
        return snapshot(List.of(squad), List.of(), List.of(), LEADER_PERMISSIONS,
                emptyDeployment(), 1, 0);
    }

    private static byte[] encodeBytes(BattleSnapshot snapshot) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BattleSnapshotCodec.encode(buffer, snapshot);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    private static BattleSnapshot decodeBytes(byte[] bytes) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            BattleSnapshot decoded = BattleSnapshotCodec.decode(buffer);
            assertEquals(0, buffer.readableBytes());
            return decoded;
        } finally {
            buffer.release();
        }
    }

    private static byte[] floatBytes(float value) {
        return java.nio.ByteBuffer.allocate(Float.BYTES).putFloat(value).array();
    }

    private static byte[] utfBytes(String value) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUtf(value);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    /** Replaces the single occurrence of {@code pattern}; fails if it is absent or ambiguous. */
    private static byte[] replaceOnce(byte[] source, byte[] pattern, byte[] replacement) {
        int found = -1;
        for (int start = 0; start + pattern.length <= source.length; start++) {
            if (Arrays.equals(source, start, start + pattern.length,
                    pattern, 0, pattern.length)) {
                if (found >= 0) {
                    throw new AssertionError("Byte pattern is not unique in the fixture");
                }
                found = start;
            }
        }
        if (found < 0) {
            throw new AssertionError("Byte pattern is absent from the fixture");
        }
        byte[] result = new byte[source.length - pattern.length + replacement.length];
        System.arraycopy(source, 0, result, 0, found);
        System.arraycopy(replacement, 0, result, found, replacement.length);
        System.arraycopy(source, found + pattern.length, result, found + replacement.length,
                source.length - found - pattern.length);
        return result;
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
