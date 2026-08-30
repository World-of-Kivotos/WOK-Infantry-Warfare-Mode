package com.wok.infantry.network.battle;

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
import com.wok.infantry.support.SupportMissionView;
import com.wok.infantry.support.SupportOptionView;
import com.wok.infantry.support.SupportTarget;
import com.wok.infantry.support.SupportTargetMode;
import com.wok.infantry.support.SupportView;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** A bounded binary codec for the already faction-filtered {@link BattleSnapshot}. */
public final class BattleSnapshotCodec {
    private BattleSnapshotCodec() {
    }

    public static void encode(FriendlyByteBuf buffer, BattleSnapshot snapshot) {
        validateRosterRelations(snapshot.viewerId(), snapshot.faction(), snapshot.ownSquad(),
                snapshot.squadLeader(), snapshot.commander(), snapshot.factionMemberCount(),
                snapshot.factionCapacity(), snapshot.squadCapacity(), snapshot.squads(),
                snapshot.alliedPositions());
        buffer.writeUUID(snapshot.viewerId());
        writeNullableEnumId(buffer, snapshot.faction(), Faction::id);
        writeNullableEnumId(buffer, snapshot.ownSquad(), SquadCallsign::id);
        buffer.writeBoolean(snapshot.squadLeader());
        buffer.writeBoolean(snapshot.commander());
        writeBoundedVarInt(buffer, snapshot.factionMemberCount(),
                BattleNetworkLimits.MAX_FACTION_CAPACITY, "faction member count");
        writeBoundedVarInt(buffer, snapshot.enemyFactionMemberCount(),
                BattleNetworkLimits.MAX_FACTION_CAPACITY, "enemy faction member count");
        writeBoundedVarInt(buffer, snapshot.factionCapacity(),
                BattleNetworkLimits.MAX_FACTION_CAPACITY, "faction capacity");
        writeBoundedVarInt(buffer, snapshot.squadCapacity(),
                BattleNetworkLimits.MAX_SQUAD_CAPACITY, "squad capacity");

        writeListSize(buffer, snapshot.squads().size(), BattleNetworkLimits.MAX_SQUADS, "squads");
        int totalMembers = 0;
        for (SquadView squad : snapshot.squads()) {
            writeSquad(buffer, squad);
            totalMembers += squad.members().size();
        }
        requireRange(totalMembers, 0, BattleNetworkLimits.MAX_ALLIED_POSITIONS,
                "total squad members");

        writeListSize(buffer, snapshot.alliedPositions().size(),
                BattleNetworkLimits.MAX_ALLIED_POSITIONS, "allied positions");
        snapshot.alliedPositions().forEach(position -> writePosition(buffer, position));

        writeListSize(buffer, snapshot.markers().size(), BattleNetworkLimits.MAX_MARKERS, "markers");
        validateMarkerFactions(snapshot.faction(), snapshot.markers());
        snapshot.markers().forEach(marker -> writeMarker(buffer, marker));

        writePermissions(buffer, snapshot.permissions());

        writeListSize(buffer, snapshot.classQuotas().size(),
                BattleNetworkLimits.MAX_CLASS_QUOTAS, "class quotas");
        snapshot.classQuotas().forEach(quota -> writeClassQuota(buffer, quota));

        writeSupport(buffer, snapshot.support());
        writeDeployment(buffer, snapshot.faction(), snapshot.deployment());

        requireNonNegative(snapshot.serverTimeMillis(), "server time");
        requireNonNegative(snapshot.revision(), "revision");
        buffer.writeLong(snapshot.serverTimeMillis());
        buffer.writeLong(snapshot.revision());
    }

    public static BattleSnapshot decode(FriendlyByteBuf buffer) {
        UUID viewerId = buffer.readUUID();
        Faction faction = readNullableEnumId(buffer, Faction::byId, "faction");
        SquadCallsign ownSquad = readNullableEnumId(buffer, SquadCallsign::byId, "squad callsign");
        boolean squadLeader = buffer.readBoolean();
        boolean commander = buffer.readBoolean();
        int factionMemberCount = readBoundedVarInt(buffer,
                BattleNetworkLimits.MAX_FACTION_CAPACITY, "faction member count");
        int enemyFactionMemberCount = readBoundedVarInt(buffer,
                BattleNetworkLimits.MAX_FACTION_CAPACITY, "enemy faction member count");
        int factionCapacity = readBoundedVarInt(buffer,
                BattleNetworkLimits.MAX_FACTION_CAPACITY, "faction capacity");
        int squadCapacity = readBoundedVarInt(buffer,
                BattleNetworkLimits.MAX_SQUAD_CAPACITY, "squad capacity");

        int squadCount = readListSize(buffer, BattleNetworkLimits.MAX_SQUADS, "squads");
        List<SquadView> squads = new ArrayList<>(squadCount);
        int totalMembers = 0;
        for (int index = 0; index < squadCount; index++) {
            SquadView squad = readSquad(buffer);
            totalMembers += squad.members().size();
            if (totalMembers > BattleNetworkLimits.MAX_ALLIED_POSITIONS) {
                throw invalid("total squad members", totalMembers);
            }
            squads.add(squad);
        }

        int positionCount = readListSize(buffer,
                BattleNetworkLimits.MAX_ALLIED_POSITIONS, "allied positions");
        List<MemberPosition> positions = new ArrayList<>(positionCount);
        for (int index = 0; index < positionCount; index++) {
            positions.add(readPosition(buffer));
        }
        validateRosterRelations(viewerId, faction, ownSquad, squadLeader, commander,
                factionMemberCount, factionCapacity, squadCapacity, squads, positions);

        int markerCount = readListSize(buffer, BattleNetworkLimits.MAX_MARKERS, "markers");
        List<TacticalMarker> markers = new ArrayList<>(markerCount);
        for (int index = 0; index < markerCount; index++) {
            markers.add(readMarker(buffer));
        }
        validateMarkerFactions(faction, markers);

        PermissionView permissions = readPermissions(buffer);

        int quotaCount = readListSize(buffer,
                BattleNetworkLimits.MAX_CLASS_QUOTAS, "class quotas");
        List<ClassQuotaView> classQuotas = new ArrayList<>(quotaCount);
        for (int index = 0; index < quotaCount; index++) {
            classQuotas.add(readClassQuota(buffer));
        }

        SupportView support = readSupport(buffer);
        DeploymentView deployment = readDeployment(buffer, faction);

        long serverTimeMillis = readNonNegativeLong(buffer, "server time");
        long revision = readNonNegativeLong(buffer, "revision");
        return new BattleSnapshot(viewerId, faction, ownSquad, squadLeader, commander,
                factionMemberCount, enemyFactionMemberCount, factionCapacity, squadCapacity,
                squads, positions, markers, permissions, classQuotas, support, deployment,
                serverTimeMillis, revision);
    }

    private static void writeSquad(FriendlyByteBuf buffer, SquadView squad) {
        buffer.writeUtf(squad.callsign().id(), BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        writeNullableUuid(buffer, squad.leaderId());
        writeListSize(buffer, squad.members().size(),
                BattleNetworkLimits.MAX_MEMBERS_PER_SQUAD, "squad members");
        squad.members().forEach(member -> writeMember(buffer, member));
        writeBoundedVarInt(buffer, squad.capacity(),
                BattleNetworkLimits.MAX_SQUAD_CAPACITY, "squad capacity");
    }

    private static SquadView readSquad(FriendlyByteBuf buffer) {
        SquadCallsign callsign = readRequiredEnumId(buffer, SquadCallsign::byId, "squad callsign");
        UUID leaderId = readNullableUuid(buffer);
        int memberCount = readListSize(buffer,
                BattleNetworkLimits.MAX_MEMBERS_PER_SQUAD, "squad members");
        List<MemberView> members = new ArrayList<>(memberCount);
        for (int index = 0; index < memberCount; index++) {
            members.add(readMember(buffer));
        }
        int capacity = readBoundedVarInt(buffer,
                BattleNetworkLimits.MAX_SQUAD_CAPACITY, "squad capacity");
        return new SquadView(callsign, leaderId, members, capacity);
    }

    private static void writeMember(FriendlyByteBuf buffer, MemberView member) {
        buffer.writeUUID(member.playerId());
        buffer.writeUtf(member.name(), BattleNetworkLimits.MAX_PLAYER_NAME_LENGTH);
        buffer.writeBoolean(member.online());
        buffer.writeBoolean(member.alive());
        writeFiniteFloat(buffer, member.health(), "member health");
        writeFiniteFloat(buffer, member.maxHealth(), "member max health");
        buffer.writeBoolean(member.leader());
        buffer.writeBoolean(member.commander());
        writeNullableEnumId(buffer, member.squad(), SquadCallsign::id);
        buffer.writeUtf(member.classId(), BattleNetworkLimits.MAX_CLASS_ID_LENGTH);
    }

    private static MemberView readMember(FriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        String name = buffer.readUtf(BattleNetworkLimits.MAX_PLAYER_NAME_LENGTH);
        boolean online = buffer.readBoolean();
        boolean alive = buffer.readBoolean();
        float health = readFiniteFloat(buffer, "member health");
        float maxHealth = readFiniteFloat(buffer, "member max health");
        if (health < 0.0F || maxHealth < 1.0F || health > maxHealth * 100.0F) {
            throw invalid("member health range", health + "/" + maxHealth);
        }
        boolean leader = buffer.readBoolean();
        boolean commander = buffer.readBoolean();
        SquadCallsign squad = readNullableEnumId(buffer, SquadCallsign::byId, "member squad");
        String classId = buffer.readUtf(BattleNetworkLimits.MAX_CLASS_ID_LENGTH);
        return new MemberView(playerId, name, online, alive, health, maxHealth,
                leader, commander, squad, classId);
    }

    private static void writePosition(FriendlyByteBuf buffer, MemberPosition position) {
        buffer.writeUUID(position.playerId());
        writeResourceLocation(buffer, position.dimension());
        writeCoordinate(buffer, position.x(), "position x");
        writeCoordinate(buffer, position.y(), "position y");
        writeCoordinate(buffer, position.z(), "position z");
        writeFiniteFloat(buffer, position.yaw(), "position yaw");
    }

    private static MemberPosition readPosition(FriendlyByteBuf buffer) {
        return new MemberPosition(buffer.readUUID(), readResourceLocation(buffer),
                readCoordinate(buffer, "position x"), readCoordinate(buffer, "position y"),
                readCoordinate(buffer, "position z"), readFiniteFloat(buffer, "position yaw"));
    }

    private static void writeMarker(FriendlyByteBuf buffer, TacticalMarker marker) {
        buffer.writeUUID(marker.id());
        buffer.writeUtf(marker.faction().id(), BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        buffer.writeUtf(marker.type().id(), BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        writeResourceLocation(buffer, marker.dimension());
        writeCoordinate(buffer, marker.x(), "marker x");
        writeCoordinate(buffer, marker.y(), "marker y");
        writeCoordinate(buffer, marker.z(), "marker z");
        writeCoordinate(buffer, marker.endX(), "marker end x");
        writeCoordinate(buffer, marker.endZ(), "marker end z");
        buffer.writeUUID(marker.creatorId());
        writeNullableEnumId(buffer, marker.creatorSquad(), SquadCallsign::id);
        requireNonNegative(marker.createdAtMillis(), "marker creation time");
        requireNonNegative(marker.expiresAtMillis(), "marker expiry time");
        if (marker.expiresAtMillis() < marker.createdAtMillis()) {
            throw invalid("marker expiry time", marker.expiresAtMillis());
        }
        buffer.writeLong(marker.createdAtMillis());
        buffer.writeLong(marker.expiresAtMillis());
    }

    private static TacticalMarker readMarker(FriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        Faction faction = readRequiredEnumId(buffer, Faction::byId, "marker faction");
        TacticalMarkerType type = readRequiredEnumId(buffer,
                TacticalMarkerType::byId, "marker type");
        ResourceLocation dimension = readResourceLocation(buffer);
        double x = readCoordinate(buffer, "marker x");
        double y = readCoordinate(buffer, "marker y");
        double z = readCoordinate(buffer, "marker z");
        double endX = readCoordinate(buffer, "marker end x");
        double endZ = readCoordinate(buffer, "marker end z");
        UUID creatorId = buffer.readUUID();
        SquadCallsign creatorSquad = readNullableEnumId(buffer,
                SquadCallsign::byId, "marker creator squad");
        long createdAt = readNonNegativeLong(buffer, "marker creation time");
        long expiresAt = readNonNegativeLong(buffer, "marker expiry time");
        if (expiresAt < createdAt) {
            throw invalid("marker expiry time", expiresAt);
        }
        return new TacticalMarker(id, faction, type, dimension, x, y, z, endX, endZ,
                creatorId, creatorSquad, createdAt, expiresAt);
    }

    private static void validateMarkerFactions(Faction viewerFaction,
                                                List<TacticalMarker> markers) {
        for (TacticalMarker marker : markers) {
            if (viewerFaction == null || marker.faction() != viewerFaction) {
                throw invalid("marker faction visibility", marker.faction().id());
            }
        }
    }

    /**
     * Fails closed when a structurally bounded snapshot contains contradictory squad identity.
     * The client uses these relationships for HUD and world markers, so duplicate or mismatched
     * records must never be interpreted as a valid same-squad relationship.
     */
    private static void validateRosterRelations(UUID viewerId, Faction viewerFaction,
                                                SquadCallsign ownSquad,
                                                boolean viewerIsLeader,
                                                boolean viewerIsCommander,
                                                int factionMemberCount,
                                                int factionCapacity,
                                                int squadCapacity,
                                                List<SquadView> squads,
                                                List<MemberPosition> positions) {
        requireRange(factionCapacity, 1, BattleNetworkLimits.MAX_FACTION_CAPACITY,
                "faction capacity");
        requireRange(factionMemberCount, 0, factionCapacity, "faction member count");
        requireRange(squadCapacity, 1, BattleNetworkLimits.MAX_SQUAD_CAPACITY,
                "squad capacity");

        Set<SquadCallsign> callsigns = new HashSet<>();
        Set<UUID> memberIds = new HashSet<>();
        MemberView viewerMember = null;
        SquadCallsign viewerContainer = null;
        int totalMembers = 0;
        int commanderCount = 0;

        for (SquadView squad : squads) {
            if (!callsigns.add(squad.callsign())) {
                throw invalid("duplicate squad callsign", squad.callsign());
            }
            requireRange(squad.capacity(), 1, squadCapacity, "squad capacity");
            if (squad.members().size() > squad.capacity()) {
                throw invalid("squad members exceed capacity", squad.callsign());
            }

            Set<UUID> squadMemberIds = new HashSet<>();
            int leaderFlags = 0;
            MemberView declaredLeader = null;
            for (MemberView member : squad.members()) {
                if (member.squad() != squad.callsign()) {
                    throw invalid("member squad relationship", member.playerId());
                }
                if (!memberIds.add(member.playerId())) {
                    throw invalid("duplicate squad member", member.playerId());
                }
                squadMemberIds.add(member.playerId());
                if (member.leader()) {
                    leaderFlags++;
                }
                if (member.commander()) {
                    commanderCount++;
                }
                if (member.playerId().equals(squad.leaderId())) {
                    declaredLeader = member;
                }
                if (member.playerId().equals(viewerId)) {
                    viewerMember = member;
                    viewerContainer = squad.callsign();
                }
            }

            if (squad.leaderId() == null) {
                if (leaderFlags != 0) {
                    throw invalid("squad leader relationship", squad.callsign());
                }
            } else if (!squadMemberIds.contains(squad.leaderId())
                    || declaredLeader == null || !declaredLeader.leader()
                    || leaderFlags != 1) {
                throw invalid("squad leader relationship", squad.callsign());
            }
            totalMembers += squad.members().size();
        }

        if (totalMembers > factionMemberCount) {
            throw invalid("squad members exceed faction count", totalMembers);
        }
        if (commanderCount > 1) {
            throw invalid("duplicate faction commander", commanderCount);
        }

        Set<UUID> positionIds = new HashSet<>();
        for (MemberPosition position : positions) {
            if (!positionIds.add(position.playerId())) {
                throw invalid("duplicate allied position", position.playerId());
            }
        }
        if (positions.size() > factionMemberCount) {
            throw invalid("allied positions exceed faction count", positions.size());
        }

        if (viewerFaction == null) {
            if (factionMemberCount != 0 || !squads.isEmpty() || !positions.isEmpty()
                    || ownSquad != null || viewerIsLeader || viewerIsCommander) {
                throw invalid("unassigned viewer roster", viewerId);
            }
            return;
        }

        if (ownSquad == null) {
            if (viewerMember != null || viewerIsLeader || viewerIsCommander) {
                throw invalid("viewer squad relationship", viewerId);
            }
            return;
        }
        if (viewerMember == null || viewerContainer != ownSquad
                || viewerMember.squad() != ownSquad
                || viewerMember.leader() != viewerIsLeader
                || viewerMember.commander() != viewerIsCommander) {
            throw invalid("viewer squad relationship", viewerId);
        }
    }

    private static void writePermissions(FriendlyByteBuf buffer, PermissionView permissions) {
        buffer.writeBoolean(permissions.canCreateSquad());
        buffer.writeBoolean(permissions.canJoinSquad());
        buffer.writeBoolean(permissions.canLeaveSquad());
        buffer.writeBoolean(permissions.canManageSquad());
        buffer.writeBoolean(permissions.canCreateMarkers());
        buffer.writeBoolean(permissions.canRemoveAnyMarker());
        buffer.writeBoolean(permissions.canClaimCommander());
    }

    private static PermissionView readPermissions(FriendlyByteBuf buffer) {
        return new PermissionView(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    private static void writeClassQuota(FriendlyByteBuf buffer, ClassQuotaView quota) {
        buffer.writeUtf(quota.classId(), BattleNetworkLimits.MAX_CLASS_ID_LENGTH);
        buffer.writeUtf(quota.displayName(),
                BattleNetworkLimits.MAX_CLASS_DISPLAY_NAME_LENGTH);
        writeBoundedVarInt(buffer, quota.limit(),
                BattleNetworkLimits.MAX_CLASS_QUOTA_VALUE, "class quota limit");
        writeBoundedVarInt(buffer, quota.used(),
                BattleNetworkLimits.MAX_CLASS_QUOTA_VALUE, "class quota usage");
    }

    private static ClassQuotaView readClassQuota(FriendlyByteBuf buffer) {
        String classId = buffer.readUtf(BattleNetworkLimits.MAX_CLASS_ID_LENGTH);
        String displayName = buffer.readUtf(
                BattleNetworkLimits.MAX_CLASS_DISPLAY_NAME_LENGTH);
        int limit = readBoundedVarInt(buffer,
                BattleNetworkLimits.MAX_CLASS_QUOTA_VALUE, "class quota limit");
        int used = readBoundedVarInt(buffer,
                BattleNetworkLimits.MAX_CLASS_QUOTA_VALUE, "class quota usage");
        return new ClassQuotaView(classId, displayName, limit, used);
    }

    private static void writeSupport(FriendlyByteBuf buffer, SupportView support) {
        requireNonNegative(support.serverGameTick(), "support server tick");
        requireNonNegative(support.structuralRevision(), "support revision");
        if (!support.serviceAvailable()
                && (!support.options().isEmpty() || !support.activeMissions().isEmpty())) {
            throw invalid("unavailable support service contains catalog state",
                    support.options().size() + "/" + support.activeMissions().size());
        }
        buffer.writeBoolean(support.serviceAvailable());
        buffer.writeUtf(support.serviceMessage(),
                BattleNetworkLimits.MAX_SUPPORT_SERVICE_MESSAGE_LENGTH);
        writeListSize(buffer, support.options().size(),
                BattleNetworkLimits.MAX_SUPPORT_OPTIONS, "support options");
        Map<ResourceLocation, SupportOptionView> optionsById = new HashMap<>();
        Set<ResourceLocation> activeOptionIds = new HashSet<>();
        for (SupportOptionView option : support.options()) {
            if (optionsById.putIfAbsent(option.id(), option) != null) {
                throw invalid("duplicate support option", option.id());
            }
            if (option.active()) {
                if (!option.providerAvailable()) {
                    throw invalid("active unavailable support option", option.id());
                }
                activeOptionIds.add(option.id());
            }
            writeSupportId(buffer, option.id());
            buffer.writeUtf(option.translationKey(),
                    BattleNetworkLimits.MAX_SUPPORT_TRANSLATION_KEY_LENGTH);
            buffer.writeUtf(option.fallbackName(),
                    BattleNetworkLimits.MAX_SUPPORT_FALLBACK_NAME_LENGTH);
            buffer.writeUtf(option.shortName(),
                    BattleNetworkLimits.MAX_SUPPORT_SHORT_NAME_LENGTH);
            buffer.writeUtf(option.targetMode().name().toLowerCase(Locale.ROOT),
                    BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
            requireSupportRadius(option.radius());
            buffer.writeDouble(option.radius());
            buffer.writeBoolean(option.providerAvailable());
            buffer.writeUtf(option.availabilityReason(),
                    BattleNetworkLimits.MAX_SUPPORT_REASON_LENGTH);
            requireNonNegative(option.readyAtGameTick(), "support ready tick");
            buffer.writeLong(option.readyAtGameTick());
            buffer.writeBoolean(option.active());
        }

        writeListSize(buffer, support.activeMissions().size(),
                BattleNetworkLimits.MAX_ACTIVE_SUPPORT_MISSIONS, "support missions");
        Set<UUID> missionIds = new HashSet<>();
        Set<ResourceLocation> missionSupportIds = new HashSet<>();
        for (SupportMissionView mission : support.activeMissions()) {
            if (!missionIds.add(mission.callId())) {
                throw invalid("duplicate support mission", mission.callId());
            }
            SupportOptionView option = optionsById.get(mission.supportId());
            if (option == null) {
                throw invalid("support mission without option", mission.supportId());
            }
            if (!missionSupportIds.add(mission.supportId())) {
                throw invalid("duplicate support mission id", mission.supportId());
            }
            requireValidSupportMissionGeometry(mission, option);
            buffer.writeUUID(mission.callId());
            writeSupportId(buffer, mission.supportId());
            writeResourceLocation(buffer, mission.dimension());
            writeCoordinate(buffer, mission.startX(), "support start x");
            writeCoordinate(buffer, mission.startZ(), "support start z");
            writeCoordinate(buffer, mission.endX(), "support end x");
            writeCoordinate(buffer, mission.endZ(), "support end z");
            requireNonNegative(mission.executeAtGameTick(), "support execution tick");
            buffer.writeLong(mission.executeAtGameTick());
            writeBoundedVarInt(buffer, mission.remainingSteps(),
                    BattleNetworkLimits.MAX_SUPPORT_STEPS, "support remaining steps");
        }
        if (!activeOptionIds.equals(missionSupportIds)) {
            throw invalid("support active option mismatch",
                    activeOptionIds + "/" + missionSupportIds);
        }
        buffer.writeLong(support.serverGameTick());
        buffer.writeLong(support.structuralRevision());
    }

    private static SupportView readSupport(FriendlyByteBuf buffer) {
        boolean serviceAvailable = buffer.readBoolean();
        String serviceMessage = buffer.readUtf(
                BattleNetworkLimits.MAX_SUPPORT_SERVICE_MESSAGE_LENGTH);
        int optionCount = readListSize(buffer, BattleNetworkLimits.MAX_SUPPORT_OPTIONS,
                "support options");
        List<SupportOptionView> options = new ArrayList<>(optionCount);
        Map<ResourceLocation, SupportOptionView> optionsById = new HashMap<>();
        Set<ResourceLocation> activeOptionIds = new HashSet<>();
        for (int index = 0; index < optionCount; index++) {
            ResourceLocation id = readSupportId(buffer);
            String translationKey = buffer.readUtf(
                    BattleNetworkLimits.MAX_SUPPORT_TRANSLATION_KEY_LENGTH);
            String fallbackName = buffer.readUtf(
                    BattleNetworkLimits.MAX_SUPPORT_FALLBACK_NAME_LENGTH);
            String shortName = buffer.readUtf(
                    BattleNetworkLimits.MAX_SUPPORT_SHORT_NAME_LENGTH);
            SupportTargetMode targetMode = readRequiredEnumId(buffer,
                    encoded -> java.util.Arrays.stream(SupportTargetMode.values())
                            .filter(mode -> mode.name().equalsIgnoreCase(encoded))
                            .findFirst(), "support target mode");
            double radius = buffer.readDouble();
            requireSupportRadius(radius);
            boolean providerAvailable = buffer.readBoolean();
            String reason = buffer.readUtf(BattleNetworkLimits.MAX_SUPPORT_REASON_LENGTH);
            if (providerAvailable && !reason.isBlank()) {
                throw invalid("available support option has a failure reason", id);
            }
            long readyAt = readNonNegativeLong(buffer, "support ready tick");
            boolean active = buffer.readBoolean();
            if (active) {
                if (!providerAvailable) {
                    throw invalid("active unavailable support option", id);
                }
                activeOptionIds.add(id);
            }
            SupportOptionView option = new SupportOptionView(id, translationKey, fallbackName,
                    shortName, targetMode, radius, providerAvailable, reason, readyAt, active);
            if (optionsById.putIfAbsent(id, option) != null) {
                throw invalid("duplicate support option", id);
            }
            options.add(option);
        }

        int missionCount = readListSize(buffer,
                BattleNetworkLimits.MAX_ACTIVE_SUPPORT_MISSIONS, "support missions");
        List<SupportMissionView> missions = new ArrayList<>(missionCount);
        Set<UUID> missionIds = new HashSet<>();
        Set<ResourceLocation> missionSupportIds = new HashSet<>();
        for (int index = 0; index < missionCount; index++) {
            UUID callId = buffer.readUUID();
            if (!missionIds.add(callId)) {
                throw invalid("duplicate support mission", callId);
            }
            ResourceLocation supportId = readSupportId(buffer);
            SupportOptionView option = optionsById.get(supportId);
            if (option == null) {
                throw invalid("support mission without option", supportId);
            }
            if (!missionSupportIds.add(supportId)) {
                throw invalid("duplicate support mission id", supportId);
            }
            ResourceLocation dimension = readResourceLocation(buffer);
            double startX = readCoordinate(buffer, "support start x");
            double startZ = readCoordinate(buffer, "support start z");
            double endX = readCoordinate(buffer, "support end x");
            double endZ = readCoordinate(buffer, "support end z");
            long executeAt = readNonNegativeLong(buffer, "support execution tick");
            int remainingSteps = readBoundedVarInt(buffer,
                    BattleNetworkLimits.MAX_SUPPORT_STEPS, "support remaining steps");
            SupportMissionView mission = new SupportMissionView(callId, supportId, dimension,
                    startX, startZ, endX, endZ, executeAt, remainingSteps);
            requireValidSupportMissionGeometry(mission, option);
            missions.add(mission);
        }
        if (!activeOptionIds.equals(missionSupportIds)) {
            throw invalid("support active option mismatch",
                    activeOptionIds + "/" + missionSupportIds);
        }
        long serverGameTick = readNonNegativeLong(buffer, "support server tick");
        long structuralRevision = readNonNegativeLong(buffer, "support revision");
        if (!serviceAvailable && (!options.isEmpty() || !missions.isEmpty())) {
            throw invalid("unavailable support service contains catalog state",
                    options.size() + "/" + missions.size());
        }
        return new SupportView(options, missions, serverGameTick, structuralRevision,
                serviceAvailable, serviceMessage);
    }

    private static void requireValidSupportMissionGeometry(SupportMissionView mission,
                                                           SupportOptionView option) {
        if (!option.directional()) {
            if (Double.compare(mission.startX(), mission.endX()) != 0
                    || Double.compare(mission.startZ(), mission.endZ()) != 0) {
                throw invalid("non-normalized point support mission", mission.callId());
            }
            return;
        }
        if (!SupportTarget.isValidDirection(mission.startX(), mission.startZ(),
                mission.endX(), mission.endZ())) {
            throw invalid("directional support mission geometry", mission.callId());
        }
    }

    private static void requireSupportRadius(double radius) {
        if (!Double.isFinite(radius) || radius < 0.0D
                || radius > com.wok.infantry.support.SupportDefinition.MAX_RADIUS) {
            throw invalid("support radius", radius);
        }
    }

    private static void writeSupportId(FriendlyByteBuf buffer, ResourceLocation id) {
        buffer.writeUtf(id.toString(), BattleNetworkLimits.MAX_SUPPORT_ID_LENGTH);
    }

    private static ResourceLocation readSupportId(FriendlyByteBuf buffer) {
        String encoded = buffer.readUtf(BattleNetworkLimits.MAX_SUPPORT_ID_LENGTH);
        ResourceLocation id = ResourceLocation.tryParse(encoded);
        if (id == null) {
            throw invalid("support id", encoded);
        }
        return id;
    }

    private static void writeDeployment(FriendlyByteBuf buffer, Faction viewerFaction,
                                        DeploymentView deployment) {
        requireNonNegative(deployment.revision(), "deployment revision");
        requireNonNegative(deployment.serverGameTick(), "deployment server tick");
        requireNonNegative(deployment.eligibleGameTick(), "deployment eligible tick");
        requireNonNegative(deployment.nextResupplyGameTick(), "deployment resupply tick");
        validateDeploymentPoints(viewerFaction, deployment.points(),
                deployment.selectedPointId());

        buffer.writeUtf(deployment.phase().name().toLowerCase(Locale.ROOT),
                BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        buffer.writeLong(deployment.revision());
        buffer.writeLong(deployment.serverGameTick());
        buffer.writeLong(deployment.eligibleGameTick());
        buffer.writeLong(deployment.nextResupplyGameTick());
        writeNullableUuid(buffer, deployment.selectedPointId());
        buffer.writeBoolean(deployment.canChangeClass());
        buffer.writeBoolean(deployment.canChangeSquad());
        buffer.writeBoolean(deployment.canDeploy());
        buffer.writeBoolean(deployment.canResupply());
        writeListSize(buffer, deployment.points().size(),
                BattleNetworkLimits.MAX_DEPLOYMENT_POINTS, "deployment points");
        deployment.points().forEach(point -> writeDeploymentPoint(buffer, point));
    }

    private static DeploymentView readDeployment(FriendlyByteBuf buffer,
                                                  Faction viewerFaction) {
        String encodedPhase = buffer.readUtf(BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        DeploymentPhase phase;
        try {
            phase = DeploymentPhase.valueOf(encodedPhase.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid("deployment phase", encodedPhase);
        }
        long deploymentRevision = readNonNegativeLong(buffer, "deployment revision");
        long serverGameTick = readNonNegativeLong(buffer, "deployment server tick");
        long eligibleGameTick = readNonNegativeLong(buffer, "deployment eligible tick");
        long nextResupplyGameTick = readNonNegativeLong(buffer, "deployment resupply tick");
        UUID selectedPointId = readNullableUuid(buffer);
        boolean canChangeClass = buffer.readBoolean();
        boolean canChangeSquad = buffer.readBoolean();
        boolean canDeploy = buffer.readBoolean();
        boolean canResupply = buffer.readBoolean();

        int pointCount = readListSize(buffer, BattleNetworkLimits.MAX_DEPLOYMENT_POINTS,
                "deployment points");
        List<DeploymentPoint> points = new ArrayList<>(pointCount);
        for (int index = 0; index < pointCount; index++) {
            points.add(readDeploymentPoint(buffer));
        }
        validateDeploymentPoints(viewerFaction, points, selectedPointId);
        return new DeploymentView(phase, deploymentRevision, serverGameTick, eligibleGameTick,
                nextResupplyGameTick, selectedPointId, canChangeClass, canChangeSquad,
                canDeploy, canResupply, points);
    }

    private static void writeDeploymentPoint(FriendlyByteBuf buffer, DeploymentPoint point) {
        buffer.writeUUID(point.id());
        buffer.writeUtf(point.faction().id(), BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        buffer.writeUtf(point.kind().id(), BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        writeResourceLocation(buffer, point.dimension());
        writeBlockCoordinate(buffer, point.position().getX(), "deployment point x");
        writeBlockCoordinate(buffer, point.position().getY(), "deployment point y");
        writeBlockCoordinate(buffer, point.position().getZ(), "deployment point z");
        requireDeploymentYaw(point.yaw());
        buffer.writeFloat(point.yaw());
        requireRange(point.supplyRadius(), 1,
                BattleNetworkLimits.MAX_DEPLOYMENT_SUPPLY_RADIUS,
                "deployment point supply radius");
        buffer.writeVarInt(point.supplyRadius());
    }

    private static DeploymentPoint readDeploymentPoint(FriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        Faction faction = readRequiredEnumId(buffer, Faction::byId,
                "deployment point faction");
        DeploymentPointKind kind = readRequiredEnumId(buffer, DeploymentPointKind::byId,
                "deployment point kind");
        ResourceLocation dimension = readResourceLocation(buffer);
        int x = readBlockCoordinate(buffer, "deployment point x");
        int y = readBlockCoordinate(buffer, "deployment point y");
        int z = readBlockCoordinate(buffer, "deployment point z");
        float yaw = readFiniteFloat(buffer, "deployment point yaw");
        requireDeploymentYaw(yaw);
        int supplyRadius = buffer.readVarInt();
        requireRange(supplyRadius, 1, BattleNetworkLimits.MAX_DEPLOYMENT_SUPPLY_RADIUS,
                "deployment point supply radius");
        return new DeploymentPoint(id, faction, dimension, new BlockPos(x, y, z), yaw,
                supplyRadius, kind);
    }

    private static void validateDeploymentPoints(Faction viewerFaction,
                                                 List<DeploymentPoint> points,
                                                 UUID selectedPointId) {
        requireRange(points.size(), 0, BattleNetworkLimits.MAX_DEPLOYMENT_POINTS,
                "deployment points");
        Set<UUID> pointIds = new HashSet<>();
        for (DeploymentPoint point : points) {
            if (viewerFaction == null || point.faction() != viewerFaction) {
                throw invalid("deployment point faction", point.faction());
            }
            if (!pointIds.add(point.id())) {
                throw invalid("duplicate deployment point", point.id());
            }
        }
        if (selectedPointId != null && !pointIds.contains(selectedPointId)) {
            throw invalid("selected deployment point", selectedPointId);
        }
    }

    private static void writeBlockCoordinate(FriendlyByteBuf buffer, int value, String field) {
        requireBlockCoordinate(value, field);
        buffer.writeInt(value);
    }

    private static int readBlockCoordinate(FriendlyByteBuf buffer, String field) {
        int value = buffer.readInt();
        requireBlockCoordinate(value, field);
        return value;
    }

    private static void requireBlockCoordinate(int value, String field) {
        if (Math.abs((long) value) > (long) BattleNetworkLimits.MAX_COORDINATE) {
            throw invalid(field, value);
        }
    }

    private static void requireDeploymentYaw(float yaw) {
        if (!Float.isFinite(yaw) || yaw < 0.0F || yaw >= 360.0F) {
            throw invalid("deployment point yaw", yaw);
        }
    }

    private static void writeResourceLocation(FriendlyByteBuf buffer, ResourceLocation id) {
        buffer.writeUtf(id.toString(), BattleNetworkLimits.MAX_DIMENSION_ID_LENGTH);
    }

    private static ResourceLocation readResourceLocation(FriendlyByteBuf buffer) {
        String encoded = buffer.readUtf(BattleNetworkLimits.MAX_DIMENSION_ID_LENGTH);
        ResourceLocation id = ResourceLocation.tryParse(encoded);
        if (id == null) {
            throw invalid("resource location", encoded);
        }
        return id;
    }

    private static void writeCoordinate(FriendlyByteBuf buffer, double value, String field) {
        requireCoordinate(value, field);
        buffer.writeDouble(value);
    }

    private static double readCoordinate(FriendlyByteBuf buffer, String field) {
        double value = buffer.readDouble();
        requireCoordinate(value, field);
        return value;
    }

    private static void requireCoordinate(double value, String field) {
        if (!Double.isFinite(value) || Math.abs(value) > BattleNetworkLimits.MAX_COORDINATE) {
            throw invalid(field, value);
        }
    }

    private static void writeFiniteFloat(FriendlyByteBuf buffer, float value, String field) {
        if (!Float.isFinite(value)) {
            throw invalid(field, value);
        }
        buffer.writeFloat(value);
    }

    private static float readFiniteFloat(FriendlyByteBuf buffer, String field) {
        float value = buffer.readFloat();
        if (!Float.isFinite(value)) {
            throw invalid(field, value);
        }
        return value;
    }

    private static void writeListSize(FriendlyByteBuf buffer, int size, int maximum, String field) {
        writeBoundedVarInt(buffer, size, maximum, field);
    }

    private static int readListSize(FriendlyByteBuf buffer, int maximum, String field) {
        return readBoundedVarInt(buffer, maximum, field);
    }

    private static void writeBoundedVarInt(FriendlyByteBuf buffer, int value,
                                           int maximum, String field) {
        requireRange(value, 0, maximum, field);
        buffer.writeVarInt(value);
    }

    private static int readBoundedVarInt(FriendlyByteBuf buffer, int maximum, String field) {
        int value = buffer.readVarInt();
        requireRange(value, 0, maximum, field);
        return value;
    }

    private static void requireRange(int value, int minimum, int maximum, String field) {
        if (value < minimum || value > maximum) {
            throw invalid(field, value);
        }
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0L) {
            throw invalid(field, value);
        }
    }

    private static long readNonNegativeLong(FriendlyByteBuf buffer, String field) {
        long value = buffer.readLong();
        requireNonNegative(value, field);
        return value;
    }

    private static void writeNullableUuid(FriendlyByteBuf buffer, UUID value) {
        buffer.writeBoolean(value != null);
        if (value != null) {
            buffer.writeUUID(value);
        }
    }

    private static UUID readNullableUuid(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readUUID() : null;
    }

    private static <T> void writeNullableEnumId(FriendlyByteBuf buffer, T value,
                                                 Function<T, String> idGetter) {
        buffer.writeBoolean(value != null);
        if (value != null) {
            buffer.writeUtf(idGetter.apply(value), BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        }
    }

    private static <T> T readNullableEnumId(FriendlyByteBuf buffer,
                                             Function<String, java.util.Optional<T>> parser,
                                             String field) {
        return buffer.readBoolean() ? readRequiredEnumId(buffer, parser, field) : null;
    }

    private static <T> T readRequiredEnumId(FriendlyByteBuf buffer,
                                             Function<String, java.util.Optional<T>> parser,
                                             String field) {
        String id = buffer.readUtf(BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        return parser.apply(id).orElseThrow(() -> invalid(field, id));
    }

    private static IllegalArgumentException invalid(String field, Object value) {
        return new IllegalArgumentException("Invalid " + field + ": " + value);
    }
}
