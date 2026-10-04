package com.wok.infantry.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Viewer-visible structural fingerprint of a {@link BattleSnapshot}. Clients rebuild cached
 * widgets only when it changes, so it must ignore values that tick every heartbeat: member
 * health, the health ratio and the server clock. Those are read from the newest snapshot at
 * render time instead.
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
        return Integer.toUnsignedLong(Objects.hash(faction, formationId, ownSquad, leader,
                commander, factionCount, enemyCount, rosterStructure(squads), markers,
                permissions, quotas, supportStructuralRevision));
    }

    /**
     * Roster projection: identity, relationships, class, connection and state of every member,
     * without the health fields.
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
                    List.copyOf(members)));
        }
        return List.copyOf(shapes);
    }

    public record SquadShape(SquadCallsign callsign, UUID leaderId, int capacity,
                             List<MemberShape> members) {
    }

    public record MemberShape(UUID playerId, String name, boolean online, boolean alive,
                              MemberState state, boolean leader, boolean commander,
                              SquadCallsign squad, String classId) {
    }
}
