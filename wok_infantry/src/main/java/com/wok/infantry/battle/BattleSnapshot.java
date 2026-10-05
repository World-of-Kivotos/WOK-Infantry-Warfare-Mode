package com.wok.infantry.battle;

import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentView;
import com.wok.infantry.support.SupportView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A faction-filtered client DTO. It intentionally has no enemy roster, position, squad, class,
 * commander or marker fields; of the enemy only the member count and the public faction name and
 * player cap (in {@link #formationContext()}) are visible.
 *
 * <p>Protocol 20 appended the viewer context: {@code formationContext} (public names of the
 * shared formation and both factions), {@code viewerClassId} (the viewer's reserved class, also
 * outside a squad; empty without a formation) and {@code kickCooldowns} (the viewer's own squad
 * rejoin cooldowns, one per call sign, sorted by call sign).
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
        long revision,
        FormationContextView formationContext,
        String viewerClassId,
        List<KickCooldownView> kickCooldowns
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
        formationContext = Objects.requireNonNullElse(formationContext,
                FormationContextView.EMPTY);
        viewerClassId = Objects.requireNonNullElse(viewerClassId, "");
        List<KickCooldownView> sortedCooldowns = new ArrayList<>(
                Objects.requireNonNullElse(kickCooldowns, List.of()));
        sortedCooldowns.sort(Comparator.comparingInt(cooldown -> cooldown.squad().ordinal()));
        kickCooldowns = List.copyOf(sortedCooldowns);
    }

    /**
     * Compatibility constructor for snapshots created before protocol 20: no formation context,
     * no kick cooldowns, and the viewer class taken from the viewer's roster entry (if any).
     */
    public BattleSnapshot(UUID viewerId, Faction faction, SquadCallsign ownSquad,
                          boolean squadLeader, boolean commander, int factionMemberCount,
                          int enemyFactionMemberCount, int factionCapacity, int squadCapacity,
                          List<SquadView> squads, List<MemberPosition> alliedPositions,
                          List<TacticalMarker> markers, PermissionView permissions,
                          List<ClassQuotaView> classQuotas, SupportView support,
                          DeploymentView deployment, long serverTimeMillis, long revision) {
        this(viewerId, faction, ownSquad, squadLeader, commander, factionMemberCount,
                enemyFactionMemberCount, factionCapacity, squadCapacity, squads,
                alliedPositions, markers, permissions, classQuotas, support, deployment,
                serverTimeMillis, revision, FormationContextView.EMPTY,
                rosterClassId(viewerId, squads), List.of());
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
                revision, formationContext, viewerClassId, kickCooldowns);
    }

    public BattleSnapshot withSupport(SupportView viewerSupport) {
        return new BattleSnapshot(viewerId, faction, ownSquad, squadLeader, commander,
                factionMemberCount, enemyFactionMemberCount, factionCapacity, squadCapacity,
                squads, alliedPositions, markers, permissions, classQuotas,
                Objects.requireNonNull(viewerSupport, "viewerSupport"), deployment,
                serverTimeMillis, revision, formationContext, viewerClassId, kickCooldowns);
    }

    /** Same snapshot with another formation context (the revision is left unchanged). */
    public BattleSnapshot withFormationContext(FormationContextView context) {
        return withViewerContext(Objects.requireNonNull(context, "context"), viewerClassId,
                kickCooldowns);
    }

    /**
     * Same snapshot with the protocol 20 viewer context replaced (the revision is left
     * unchanged; the server computes it from the same values).
     */
    public BattleSnapshot withViewerContext(FormationContextView context, String classId,
                                            List<KickCooldownView> cooldowns) {
        return new BattleSnapshot(viewerId, faction, ownSquad, squadLeader, commander,
                factionMemberCount, enemyFactionMemberCount, factionCapacity, squadCapacity,
                squads, alliedPositions, markers, permissions, classQuotas, support, deployment,
                serverTimeMillis, revision, context, classId, cooldowns);
    }

    /**
     * The faction's formation is known: the context names one, or the server sent call signs
     * (it sends none before the formation vote is locked).
     */
    public boolean formationLocked() {
        return formationContext.hasFormation() || !squads.isEmpty();
    }

    /** The squad of the given call sign as sent (configured squads only), or {@code null}. */
    public SquadView squad(SquadCallsign callsign) {
        if (callsign == null) {
            return null;
        }
        for (SquadView squad : squads) {
            if (squad.callsign() == callsign) {
                return squad;
            }
        }
        return null;
    }

    /** The viewer's kick cooldown for {@code squad}, or {@code null}. */
    public KickCooldownView kickCooldown(SquadCallsign squad) {
        for (KickCooldownView cooldown : kickCooldowns) {
            if (cooldown.squad() == squad) {
                return cooldown;
            }
        }
        return null;
    }

    /** Milliseconds the viewer must still wait to create or join {@code squad}; 0 when free. */
    public long kickCooldownRemainingMillis(SquadCallsign squad, long nowServerMillis) {
        KickCooldownView cooldown = kickCooldown(squad);
        return cooldown == null ? 0L : cooldown.remainingMillis(nowServerMillis);
    }

    private static String rosterClassId(UUID viewerId, List<SquadView> squads) {
        if (viewerId == null || squads == null) {
            return "";
        }
        for (SquadView squad : squads) {
            for (MemberView member : squad.members()) {
                if (member.playerId().equals(viewerId)) {
                    return member.classId();
                }
            }
        }
        return "";
    }

    private static DeploymentView unavailableDeployment() {
        return new DeploymentView(DeploymentPhase.WAITING, 0L, 0L, 0L, 0L, null,
                true, true, false, false, List.of());
    }
}
