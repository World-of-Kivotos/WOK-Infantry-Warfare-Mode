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
import com.wok.infantry.deployment.DeploymentView;
import com.wok.infantry.network.battle.packet.s2c.BattleSnapshotPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleSnapshotFanoutTest {
    private static final int HEARTBEAT_ROUNDS = 600;
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final List<ClassQuotaView> CLASS_QUOTAS = List.of(
            new ClassQuotaView("assault", "Assault", 8, 6),
            new ClassQuotaView("support", "Support", 2, 2));

    @Test
    void eightyViewerFullFactionFanoutRoundTripsWithoutEnemyUuidLeakage() {
        FactionFixture blue = fullFaction(Faction.BLUE);
        FactionFixture red = fullFaction(Faction.RED);
        List<ViewerScenario> scenarios = viewerScenarios(blue, red);
        int packetCount = 0;
        long encodedByteCount = 0L;

        for (ViewerScenario scenario : scenarios) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                BattleSnapshotPacket.encode(new BattleSnapshotPacket(
                        scenario.snapshot(), BattleOpenTarget.NONE), buffer);
                encodedByteCount += buffer.readableBytes();

                BattleSnapshotPacket decodedPacket = BattleSnapshotPacket.decode(buffer);
                assertRoundTripScenario(decodedPacket, scenario,
                        scenario.context("round trip"));
                assertEquals(0, buffer.readableBytes(), scenario.context("round trip"));
                packetCount++;
            } finally {
                buffer.release();
            }
        }

        assertEquals(BattleRules.FACTION_CAPACITY * Faction.values().length, packetCount);
        assertTrue(encodedByteCount > 0L);
    }

    /**
     * Repeatable capacity workload for the full {@link BattleSnapshot} DTO through
     * {@link BattleSnapshotPacket#encode(BattleSnapshotPacket, FriendlyByteBuf)}. This does
     * not exercise Forge SimpleChannel dispatch, sockets/TCP, or 80 real client connections.
     */
    @Test
    void sixHundredHeartbeatRoundsEncodeEightyViewerDtosIntoFriendlyByteBufs() {
        FactionFixture blue = fullFaction(Faction.BLUE);
        FactionFixture red = fullFaction(Faction.RED);
        List<ViewerScenario> scenarios = viewerScenarios(blue, red);
        int expectedViewerCount = BattleRules.FACTION_CAPACITY * Faction.values().length;
        long expectedPacketCount = (long) HEARTBEAT_ROUNDS * expectedViewerCount;
        assertEquals(80, expectedViewerCount);
        assertEquals(expectedViewerCount, scenarios.size());

        long packetCount = 0L;
        long encodedByteCount = 0L;
        long encodingNanos = 0L;
        long expectedBytesPerHeartbeat = -1L;
        int minimumPacketBytes = Integer.MAX_VALUE;
        int maximumPacketBytes = 0;
        long startedAtNanos = System.nanoTime();

        for (int heartbeatRound = 0; heartbeatRound < HEARTBEAT_ROUNDS;
             heartbeatRound++) {
            long roundEncodedBytes = 0L;
            for (ViewerScenario scenario : scenarios) {
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    long encodeStartedAtNanos = System.nanoTime();
                    BattleSnapshotPacket.encode(new BattleSnapshotPacket(
                            scenario.snapshot(), BattleOpenTarget.NONE), buffer);
                    encodingNanos += System.nanoTime() - encodeStartedAtNanos;
                    int packetBytes = buffer.readableBytes();
                    roundEncodedBytes += packetBytes;
                    encodedByteCount += packetBytes;
                    minimumPacketBytes = Math.min(minimumPacketBytes, packetBytes);
                    maximumPacketBytes = Math.max(maximumPacketBytes, packetBytes);
                    packetCount++;

                    // Decode the boundary rounds so the long-running encode workload keeps
                    // the original cross-faction UUID isolation guarantee without making
                    // assertion bookkeeping dominate every heartbeat measurement.
                    if (heartbeatRound == 0 || heartbeatRound == HEARTBEAT_ROUNDS - 1) {
                        BattleSnapshotPacket decodedPacket =
                                BattleSnapshotPacket.decode(buffer);
                        assertRoundTripScenario(decodedPacket, scenario,
                                scenario.context("heartbeat " + heartbeatRound));
                    }
                } finally {
                    buffer.release();
                }
            }

            if (expectedBytesPerHeartbeat < 0L) {
                expectedBytesPerHeartbeat = roundEncodedBytes;
            } else {
                assertEquals(expectedBytesPerHeartbeat, roundEncodedBytes,
                        "Full 80-viewer heartbeat byte count changed at round "
                                + heartbeatRound);
            }
        }

        long workloadElapsedNanos = System.nanoTime() - startedAtNanos;
        double encodeElapsedMillis = encodingNanos / 1_000_000.0D;
        double workloadElapsedMillis = workloadElapsedNanos / 1_000_000.0D;
        double encodedMebibytes = encodedByteCount / (1024.0D * 1024.0D);
        double throughputMebibytesPerSecond = encodedMebibytes
                / Math.max(encodingNanos / 1_000_000_000.0D, 0.000_001D);
        System.out.printf(Locale.ROOT,
                "[wok_infantry DTO/FriendlyByteBuf heartbeat capacity] "
                        + "rounds=%d viewers=%d packets=%d "
                        + "encodedBytes=%d packetBytes=%d..%d encodeElapsedMs=%.3f "
                        + "workloadElapsedMs=%.3f throughputMiBps=%.3f%n",
                HEARTBEAT_ROUNDS, scenarios.size(), packetCount, encodedByteCount,
                minimumPacketBytes, maximumPacketBytes, encodeElapsedMillis,
                workloadElapsedMillis,
                throughputMebibytesPerSecond);

        // These are workload-integrity assertions, deliberately not machine-speed gates.
        assertEquals(expectedPacketCount, packetCount);
        assertEquals(expectedBytesPerHeartbeat * HEARTBEAT_ROUNDS, encodedByteCount);
        assertTrue(expectedBytesPerHeartbeat > 0L);
        assertTrue(minimumPacketBytes > 0);
        assertTrue(maximumPacketBytes >= minimumPacketBytes);
        assertTrue(encodingNanos > 0L);
        assertTrue(workloadElapsedNanos > 0L);
    }

    private static List<ViewerScenario> viewerScenarios(FactionFixture blue,
                                                        FactionFixture red) {
        List<ViewerScenario> scenarios = new ArrayList<>();
        for (FactionFixture friendly : List.of(blue, red)) {
            FactionFixture enemy = friendly.faction() == Faction.BLUE ? red : blue;
            for (int viewerIndex = 0; viewerIndex < friendly.members().size();
                 viewerIndex++) {
                MemberView viewer = friendly.members().get(viewerIndex);
                scenarios.add(new ViewerScenario(friendly, enemy, viewer, viewerIndex,
                        snapshotFor(friendly, viewer, viewerIndex)));
            }
        }
        return List.copyOf(scenarios);
    }

    private static void assertRoundTripScenario(BattleSnapshotPacket decodedPacket,
                                                ViewerScenario scenario,
                                                String assertionContext) {
        BattleSnapshot decoded = decodedPacket.snapshot();
        assertEquals(BattleOpenTarget.NONE, decodedPacket.openTarget(), assertionContext);
        assertEquals(scenario.snapshot(), decoded, assertionContext);
        assertEquals(scenario.friendly().faction(), decoded.faction(), assertionContext);
        assertEquals(scenario.viewer().playerId(), decoded.viewerId(), assertionContext);
        assertEquals(scenario.viewer().squad(), decoded.ownSquad(), assertionContext);
        assertEquals(BattleRules.FACTION_CAPACITY,
                decoded.factionMemberCount(), assertionContext);
        assertEquals(BattleRules.FACTION_CAPACITY,
                decoded.enemyFactionMemberCount(), assertionContext);
        assertEquals(BattleNetworkLimits.MAX_SQUADS,
                decoded.squads().size(), assertionContext);
        assertTrue(decoded.squads().stream().allMatch(squad ->
                        squad.members().size() == BattleRules.SQUAD_CAPACITY),
                assertionContext);
        assertNoEnemyUuidLeakage(decoded, scenario.friendly(), scenario.enemy(),
                assertionContext);
    }

    private static void assertNoEnemyUuidLeakage(BattleSnapshot decoded,
                                                 FactionFixture friendly,
                                                 FactionFixture enemy,
                                                 String assertionContext) {
        Set<UUID> rosterIds = new HashSet<>();
        Set<UUID> exposedUuids = new HashSet<>();
        exposedUuids.add(decoded.viewerId());
        for (SquadView squad : decoded.squads()) {
            if (squad.leaderId() != null) {
                exposedUuids.add(squad.leaderId());
            }
            for (MemberView member : squad.members()) {
                rosterIds.add(member.playerId());
                exposedUuids.add(member.playerId());
            }
        }
        assertEquals(friendly.playerIds(), rosterIds, assertionContext);

        Set<UUID> positionIds = new HashSet<>();
        for (MemberPosition position : decoded.alliedPositions()) {
            positionIds.add(position.playerId());
            exposedUuids.add(position.playerId());
        }
        assertEquals(BattleRules.FACTION_CAPACITY,
                decoded.alliedPositions().size(), assertionContext);
        assertEquals(friendly.playerIds(), positionIds, assertionContext);

        assertEquals(BattleNetworkLimits.MAX_MARKERS,
                decoded.markers().size(), assertionContext);
        assertTrue(decoded.markers().stream().allMatch(marker ->
                        marker.faction() == decoded.faction()),
                assertionContext);
        for (TacticalMarker marker : decoded.markers()) {
            exposedUuids.add(marker.id());
            exposedUuids.add(marker.creatorId());
            assertTrue(friendly.playerIds().contains(marker.creatorId()),
                    assertionContext);
        }

        assertTrue(Collections.disjoint(exposedUuids, enemy.playerIds()),
                assertionContext + " leaked an enemy UUID");
    }

    private static FactionFixture fullFaction(Faction faction) {
        List<SquadView> squads = new ArrayList<>();
        List<MemberView> allMembers = new ArrayList<>();
        List<MemberPosition> positions = new ArrayList<>();
        long playerNamespace = faction == Faction.BLUE ? 1L : 2L;
        int playerIndex = 0;

        for (SquadCallsign callsign : SquadCallsign.values()) {
            List<MemberView> members = new ArrayList<>();
            for (int squadIndex = 0; squadIndex < BattleRules.SQUAD_CAPACITY;
                 squadIndex++) {
                UUID playerId = new UUID(playerNamespace, playerIndex + 1L);
                boolean leader = squadIndex == 0;
                boolean commander = playerIndex == 0;
                MemberView member = new MemberView(playerId,
                        faction.id() + "-player-" + (playerIndex + 1),
                        true, true, 20.0F, 20.0F, leader, commander,
                        callsign, leader ? "support" : "assault");
                members.add(member);
                allMembers.add(member);
                positions.add(new MemberPosition(playerId, OVERWORLD,
                        128.0D + playerIndex * 4.0D, 64.0D,
                        256.0D + playerIndex * 3.0D, playerIndex * 9.0F));
                playerIndex++;
            }
            squads.add(new SquadView(callsign, members.get(0).playerId(), members,
                    BattleRules.SQUAD_CAPACITY));
        }

        List<TacticalMarker> markers = new ArrayList<>();
        TacticalMarkerType[] markerTypes = TacticalMarkerType.values();
        for (int markerIndex = 0; markerIndex < BattleNetworkLimits.MAX_MARKERS;
             markerIndex++) {
            TacticalMarkerType type = markerTypes[markerIndex % markerTypes.length];
            double x = 1_000.0D + markerIndex * 8.0D;
            double z = 2_000.0D + markerIndex * 6.0D;
            double endX = type == TacticalMarkerType.ATTACK_DIRECTION ? x + 32.0D : x;
            double endZ = z;
            MemberView creator = allMembers.get(markerIndex % allMembers.size());
            long createdAt = 10_000L + markerIndex;
            markers.add(new TacticalMarker(
                    new UUID(1_000L + faction.ordinal(), markerIndex + 1L),
                    faction, type, OVERWORLD, x, 64.0D, z, endX, endZ,
                    creator.playerId(), creator.squad(), createdAt,
                    createdAt + BattleRules.DEFAULT_MARKER_TTL_MILLIS));
        }

        return new FactionFixture(faction, List.copyOf(squads),
                List.copyOf(allMembers), List.copyOf(positions), List.copyOf(markers),
                Set.copyOf(allMembers.stream().map(MemberView::playerId).toList()));
    }

    private static BattleSnapshot snapshotFor(FactionFixture faction, MemberView viewer,
                                               int viewerIndex) {
        PermissionView permissions = new PermissionView(false, false, true,
                viewer.leader(), viewer.leader() || viewer.commander(),
                viewer.commander(), viewer.commander());
        DeploymentView deployment = new DeploymentView(DeploymentPhase.WAITING,
                viewerIndex, 1_000L, 1_000L, 1_200L, null,
                true, true, false, false, List.of());
        return new BattleSnapshot(viewer.playerId(), faction.faction(), viewer.squad(),
                viewer.leader(), viewer.commander(), BattleRules.FACTION_CAPACITY,
                BattleRules.FACTION_CAPACITY, BattleRules.FACTION_CAPACITY,
                BattleRules.SQUAD_CAPACITY, faction.squads(), faction.positions(),
                faction.markers(), permissions, CLASS_QUOTAS, deployment,
                100_000L + viewerIndex, viewerIndex);
    }

    private record FactionFixture(Faction faction, List<SquadView> squads,
                                  List<MemberView> members,
                                  List<MemberPosition> positions,
                                  List<TacticalMarker> markers,
                                  Set<UUID> playerIds) {
    }

    private record ViewerScenario(FactionFixture friendly, FactionFixture enemy,
                                  MemberView viewer, int viewerIndex,
                                  BattleSnapshot snapshot) {
        private String context(String operation) {
            return friendly.faction() + " viewer " + viewerIndex + " " + operation;
        }
    }
}
