package com.wok.infantry.battle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Viewer-visible structural fingerprint of a {@link BattleSnapshot}. Clients rebuild cached
 * widgets only when it changes, so it must ignore values that tick every heartbeat: member
 * health, the health ratio, the server clock and kick cooldown countdowns. Those are read from
 * the newest snapshot at render time instead.
 */
public final class BattleSnapshotRevision {
    private BattleSnapshotRevision() {
    }

    /** Same composition as before protocol 19, with the roster replaced by its projection. */
    public static long visible(Faction faction, String formationId, SquadCallsign ownSquad,
                               boolean leader, boolean commander, int factionCount,
                               int enemyCount, List<SquadView> squads,
                               List<TacticalMarker> markers, PermissionView permissions,
                               List<ClassQuotaView> quotas, long supportStructuralRevision) {
        return visible(faction, formationId, ownSquad, leader, commander, factionCount,
                enemyCount, squads, markers, permissions, quotas, supportStructuralRevision,
                FormationContextView.EMPTY, "", Set.of());
    }

    /**
     * Protocol 20 composition: also the formation context, the viewer's class and which call
     * signs are under a kick cooldown (not how long is left, so the countdown never rebuilds a
     * screen; the expiry itself does, because it re-enables "join").
     */
    public static long visible(Faction faction, String formationId, SquadCallsign ownSquad,
                               boolean leader, boolean commander, int factionCount,
                               int enemyCount, List<SquadView> squads,
                               List<TacticalMarker> markers, PermissionView permissions,
                               List<ClassQuotaView> quotas, long supportStructuralRevision,
                               FormationContextView formationContext, String viewerClassId,
                               Collection<SquadCallsign> kickCooldownSquads) {
        Set<SquadCallsign> cooledDown = EnumSet.noneOf(SquadCallsign.class);
        if (kickCooldownSquads != null) {
            kickCooldownSquads.stream().filter(Objects::nonNull).forEach(cooledDown::add);
        }
        return Integer.toUnsignedLong(Objects.hash(faction, formationId, ownSquad, leader,
                commander, factionCount, enemyCount, rosterStructure(squads), markers,
                permissions, quotas, supportStructuralRevision,
                Objects.requireNonNullElse(formationContext, FormationContextView.EMPTY),
                Objects.requireNonNullElse(viewerClassId, ""), cooledDown));
    }

    /**
     * Roster projection: identity, relationships, class, connection and state of every member,
     * plus each squad's class limits, without the health fields.
     */
    public static List<SquadShape> rosterStructure(List<SquadView> squads) {
        if (squads == null || squads.isEmpty()) {
            return List.of();
        }
        List<SquadShape> shapes = new ArrayList<>(squads.size());
        for (SquadView squad : squads) {
            List<MemberShape> members = new ArrayList<>(squad.members().size());
            for (MemberView member : squad.members()) {
                members.add(new MemberShape(member.playerId(), member.name(), member.online(),
                        member.alive(), member.state(), member.leader(), member.commander(),
                        member.squad(), member.classId()));
            }
            shapes.add(new SquadShape(squad.callsign(), squad.leaderId(), squad.capacity(),
                    List.copyOf(members), squad.classLimits()));
        }
        return List.copyOf(shapes);
    }

    public record SquadShape(SquadCallsign callsign, UUID leaderId, int capacity,
                             List<MemberShape> members, List<ClassLimitView> classLimits) {
    }

    public record MemberShape(UUID playerId, String name, boolean online, boolean alive,
                              MemberState state, boolean leader, boolean commander,
                              SquadCallsign squad, String classId) {
    }
}
