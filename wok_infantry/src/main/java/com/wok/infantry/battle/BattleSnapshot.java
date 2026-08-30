package com.wok.infantry.battle;

import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentView;
import com.wok.infantry.support.SupportView;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A faction-filtered client DTO. It intentionally has no enemy roster, position, squad, class,
 * commander or marker fields.
 */
public record BattleSnapshot(
        UUID viewerId,
        Faction faction,
        SquadCallsign ownSquad,
        boolean squadLeader,
        boolean commander,
        int factionMemberCount,
        int enemyFactionMemberCount,
        int factionCapacity,
        int squadCapacity,
        List<SquadView> squads,
        List<MemberPosition> alliedPositions,
        List<TacticalMarker> markers,
        PermissionView permissions,
        List<ClassQuotaView> classQuotas,
        SupportView support,
        DeploymentView deployment,
        long serverTimeMillis,
        long revision
) {
    public BattleSnapshot {
        Objects.requireNonNull(viewerId, "viewerId");
        squads = List.copyOf(Objects.requireNonNullElse(squads, List.of()));
        alliedPositions = List.copyOf(Objects.requireNonNullElse(alliedPositions, List.of()));
        markers = List.copyOf(Objects.requireNonNullElse(markers, List.of()));
        Objects.requireNonNull(permissions, "permissions");
        classQuotas = List.copyOf(Objects.requireNonNullElse(classQuotas, List.of()));
        Objects.requireNonNull(support, "support");
        Objects.requireNonNull(deployment, "deployment");
    }

    /** Compatibility constructor for snapshots created before support state joined the wire DTO. */
    public BattleSnapshot(UUID viewerId, Faction faction, SquadCallsign ownSquad,
                          boolean squadLeader, boolean commander, int factionMemberCount,
                          int enemyFactionMemberCount, int factionCapacity, int squadCapacity,
                          List<SquadView> squads, List<MemberPosition> alliedPositions,
                          List<TacticalMarker> markers, PermissionView permissions,
                          List<ClassQuotaView> classQuotas, DeploymentView deployment,
                          long serverTimeMillis, long revision) {
        this(viewerId, faction, ownSquad, squadLeader, commander, factionMemberCount,
                enemyFactionMemberCount, factionCapacity, squadCapacity, squads,
                alliedPositions, markers, permissions, classQuotas, SupportView.unavailable(),
                deployment, serverTimeMillis, revision);
    }

    /**
     * Compatibility constructor for server-side battle snapshots before the viewer-specific
     * deployment state is attached by the network boundary.
     */
    public BattleSnapshot(UUID viewerId, Faction faction, SquadCallsign ownSquad,
                          boolean squadLeader, boolean commander, int factionMemberCount,
                          int enemyFactionMemberCount, int factionCapacity, int squadCapacity,
                          List<SquadView> squads, List<MemberPosition> alliedPositions,
                          List<TacticalMarker> markers, PermissionView permissions,
                          List<ClassQuotaView> classQuotas, long serverTimeMillis, long revision) {
        this(viewerId, faction, ownSquad, squadLeader, commander, factionMemberCount,
                enemyFactionMemberCount, factionCapacity, squadCapacity, squads,
                alliedPositions, markers, permissions, classQuotas, SupportView.unavailable(),
                unavailableDeployment(), serverTimeMillis, revision);
    }

    public BattleSnapshot withDeployment(DeploymentView viewerDeployment) {
        return new BattleSnapshot(viewerId, faction, ownSquad, squadLeader, commander,
                factionMemberCount, enemyFactionMemberCount, factionCapacity, squadCapacity,
                squads, alliedPositions, markers, permissions, classQuotas, support,
                Objects.requireNonNull(viewerDeployment, "viewerDeployment"), serverTimeMillis,
                revision);
    }

    public BattleSnapshot withSupport(SupportView viewerSupport) {
        return new BattleSnapshot(viewerId, faction, ownSquad, squadLeader, commander,
                factionMemberCount, enemyFactionMemberCount, factionCapacity, squadCapacity,
                squads, alliedPositions, markers, permissions, classQuotas,
                Objects.requireNonNull(viewerSupport, "viewerSupport"), deployment,
                serverTimeMillis, revision);
    }

    private static DeploymentView unavailableDeployment() {
        return new DeploymentView(DeploymentPhase.WAITING, 0L, 0L, 0L, 0L, null,
                true, true, false, false, List.of());
    }
}
