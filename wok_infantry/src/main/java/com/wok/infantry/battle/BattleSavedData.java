package com.wok.infantry.battle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** World-persistent backing state. All policy decisions remain in {@link BattleService}. */
final class BattleSavedData extends SavedData {
    private static final String DATA_NAME = "wok_infantry_battle";
    private static final int DATA_VERSION = 6;

    private final Map<UUID, StoredPlayer> players = new LinkedHashMap<>();
    private final Map<SquadKey, UUID> leaders = new LinkedHashMap<>();
    private final EnumMap<Faction, UUID> commanders = new EnumMap<>(Faction.class);
    private final Map<UUID, TacticalMarker> markers = new LinkedHashMap<>();
    private long revision;

    BattleSavedData() {
    }

    static BattleSavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(
                BattleSavedData::load, BattleSavedData::new, DATA_NAME);
    }

    static BattleSavedData load(CompoundTag root) {
        BattleSavedData data = new BattleSavedData();
        data.revision = Math.max(0L, root.getLong("Revision"));
        data.readPlayers(root.getList("Players", Tag.TAG_COMPOUND));
        data.readLeaders(root.getList("Leaders", Tag.TAG_COMPOUND));
        data.readCommanders(root.getList("Commanders", Tag.TAG_COMPOUND));
        data.readMarkers(root.getList("Markers", Tag.TAG_COMPOUND));
        data.normalize();
        // Loading is also the trust boundary for old/corrupt NBT. Rewrite the bounded,
        // canonical form on the next world save even when normalization made no later repair.
        data.setDirty();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putInt("Version", DATA_VERSION);
        root.putLong("Revision", revision);
        root.put("Players", writePlayers());
        root.put("Leaders", writeLeaders());
        root.put("Commanders", writeCommanders());
        root.put("Markers", writeMarkers());
        return root;
    }

    StoredPlayer player(UUID playerId) {
        return players.get(playerId);
    }

    Collection<StoredPlayer> players() {
        return players.values();
    }

    StoredPlayer addPlayer(UUID playerId, String name, long nowMillis) {
        if (players.size() >= BattleRules.MAX_PLAYER_RECORDS) {
            return null;
        }
        StoredPlayer created = new StoredPlayer(playerId);
        created.lastKnownName = truncate(Objects.requireNonNullElse(name, ""),
                BattleRules.MAX_PLAYER_NAME_LENGTH);
        created.assignedClassId = BattleRules.DEFAULT_CLASS_ID;
        created.firstJoinedAtMillis = nowMillis;
        created.lastSeenAtMillis = nowMillis;
        players.put(playerId, created);
        return created;
    }

    int playerCount() {
        return players.size();
    }

    /** Removes a historical identity and all defensive dangling references without bumping revision. */
    boolean removePlayer(UUID playerId) {
        if (playerId == null || players.remove(playerId) == null) {
            return false;
        }
        leaders.values().removeIf(playerId::equals);
        commanders.values().removeIf(playerId::equals);
        markers.values().removeIf(marker -> playerId.equals(marker.creatorId()));
        return true;
    }

    UUID leader(Faction faction, String formationId, SquadCallsign squad) {
        SquadKey key = SquadKey.create(faction, formationId, squad);
        return key == null ? null : leaders.get(key);
    }

    void setLeader(Faction faction, String formationId, SquadCallsign squad, UUID playerId) {
        SquadKey key = SquadKey.create(faction, formationId, squad);
        if (key == null) {
            return;
        }
        if (playerId == null) {
            leaders.remove(key);
        } else {
            StoredPlayer player = players.get(playerId);
            if (player == null || player.faction != key.faction()
                    || !key.formationId().equals(player.formationId)
                    || player.squad != key.squad()
                    || (!leaders.containsKey(key)
                    && leaders.size() >= BattleRules.MAX_PERSISTED_LEADERS)) {
                return;
            }
            leaders.put(key, playerId);
        }
    }

    /**
     * Compatibility lookup for old tests and callers. Ambiguous cross-formation calls fail closed.
     */
    UUID leader(Faction faction, SquadCallsign squad) {
        UUID result = null;
        boolean found = false;
        for (Map.Entry<SquadKey, UUID> entry : leaders.entrySet()) {
            if (entry.getKey().faction() != faction || entry.getKey().squad() != squad) {
                continue;
            }
            if (found) {
                return null;
            }
            result = entry.getValue();
            found = true;
        }
        return result;
    }

    /**
     * Compatibility mutation for old tests and callers. A non-null leader derives the complete
     * squad key from that player's authoritative record; ambiguous clears fail closed.
     */
    void setLeader(Faction faction, SquadCallsign squad, UUID playerId) {
        if (playerId != null) {
            StoredPlayer player = players.get(playerId);
            if (player != null && player.faction == faction && player.squad == squad
                    && player.formationId != null) {
                setLeader(faction, player.formationId, squad, playerId);
            }
            return;
        }
        SquadKey match = null;
        for (SquadKey key : leaders.keySet()) {
            if (key.faction() != faction || key.squad() != squad) {
                continue;
            }
            if (match != null) {
                return;
            }
            match = key;
        }
        if (match != null) {
            leaders.remove(match);
        }
    }

    UUID commander(Faction faction) {
        return commanders.get(faction);
    }

    void setCommander(Faction faction, UUID playerId) {
        if (playerId == null) {
            commanders.remove(faction);
        } else {
            commanders.put(faction, playerId);
        }
    }

    Collection<TacticalMarker> markers() {
        return markers.values();
    }

    TacticalMarker marker(UUID markerId) {
        return markers.get(markerId);
    }

    void addMarker(TacticalMarker marker) {
        markers.put(marker.id(), marker);
    }

    void removeMarker(UUID markerId) {
        markers.remove(markerId);
    }

    boolean removeExpiredMarkers(long nowMillis) {
        boolean removed = markers.values().removeIf(marker -> marker.expiredAt(nowMillis));
        if (removed) {
            changed();
        }
        return removed;
    }

    long revision() {
        return revision;
    }

    void changed() {
        if (revision < Long.MAX_VALUE) {
            revision++;
        }
        setDirty();
    }

    /** Persist timestamp-only heartbeats without forcing snapshot revision churn. */
    void touch() {
        setDirty();
    }

    void clear() {
        players.clear();
        leaders.clear();
        commanders.clear();
        markers.clear();
        changed();
    }

    private void readPlayers(ListTag list) {
        long now = Math.max(0L, System.currentTimeMillis());
        int scanLimit = Math.min(list.size(), BattleRules.MAX_PLAYER_LOAD_SCAN);
        Map<UUID, StoredPlayer> candidates = new LinkedHashMap<>();
        for (int i = 0; i < scanLimit; i++) {
            StoredPlayer candidate = readPlayer(list.getCompound(i), now);
            if (candidate == null) {
                continue;
            }
            StoredPlayer existing = candidates.get(candidate.playerId);
            if (existing == null || (existing.faction == null && candidate.faction != null)) {
                candidates.put(candidate.playerId, candidate);
            }
        }

        // Valid assigned records are the authoritative live battle state, so retain them first.
        // A malformed save cannot crowd one faction (or identity history) past its hard capacity.
        Comparator<StoredPlayer> activeOrder = Comparator
                .comparingLong((StoredPlayer player) -> player.firstJoinedAtMillis)
                .thenComparing(player -> player.playerId);
        for (Faction faction : Faction.values()) {
            candidates.values().stream()
                    .filter(player -> player.faction == faction)
                    .sorted(activeOrder)
                    .limit(BattleRules.FACTION_CAPACITY)
                    .forEach(player -> players.putIfAbsent(player.playerId, player));
        }

        candidates.values().stream()
                .filter(player -> player.faction == null)
                .filter(player -> elapsedSince(now, player.lastSeenAtMillis)
                        < BattleRules.UNASSIGNED_HISTORY_TTL_MILLIS)
                .sorted(Comparator.comparingLong((StoredPlayer player) -> player.lastSeenAtMillis)
                        .reversed().thenComparing(player -> player.playerId))
                .limit(Math.max(0, BattleRules.MAX_PLAYER_RECORDS - players.size()))
                .forEach(player -> players.putIfAbsent(player.playerId, player));
    }

    private void readLeaders(ListTag list) {
        int scanLimit = Math.min(list.size(), BattleRules.MAX_LEADER_LOAD_SCAN);
        for (int i = 0; i < scanLimit
                && leaders.size() < BattleRules.MAX_PERSISTED_LEADERS; i++) {
            CompoundTag tag = list.getCompound(i);
            Faction faction = Faction.byId(tag.getString("Faction")).orElse(null);
            // Version-3 and older leader records had no formation. There is no safe way to infer
            // which same-callsign squad they governed, so fail closed and let normalize elect one.
            String formationId = tag.contains("Formation", Tag.TAG_STRING)
                    ? normalizeFormationId(tag.getString("Formation")) : null;
            SquadCallsign squad = SquadCallsign.byId(tag.getString("Squad")).orElse(null);
            UUID leaderId = readUuid(tag, "Player").orElse(null);
            SquadKey key = SquadKey.create(faction, formationId, squad);
            if (key != null && leaderId != null) {
                leaders.putIfAbsent(key, leaderId);
            }
        }
    }

    private void readCommanders(ListTag list) {
        int scanLimit = Math.min(list.size(), BattleRules.MAX_COMMANDER_LOAD_SCAN);
        for (int i = 0; i < scanLimit; i++) {
            CompoundTag tag = list.getCompound(i);
            Faction faction = Faction.byId(tag.getString("Faction")).orElse(null);
            UUID commanderId = readUuid(tag, "Player").orElse(null);
            if (faction != null && commanderId != null) {
                commanders.putIfAbsent(faction, commanderId);
            }
        }
    }

    private void readMarkers(ListTag list) {
        long now = Math.max(0L, System.currentTimeMillis());
        int scanLimit = Math.min(list.size(), BattleRules.MAX_MARKER_LOAD_SCAN);
        EnumMap<Faction, Integer> factionCounts = new EnumMap<>(Faction.class);
        Map<UUID, Integer> creatorCounts = new LinkedHashMap<>();
        for (int i = 0; i < scanLimit && markers.size() < BattleRules.MAX_PERSISTED_MARKERS; i++) {
            CompoundTag tag = list.getCompound(i);
            UUID markerId = readUuid(tag, "Id").orElse(null);
            UUID creatorId = readUuid(tag, "Creator").orElse(null);
            Faction faction = Faction.byId(tag.getString("Faction")).orElse(null);
            TacticalMarkerType type = TacticalMarkerType.byId(tag.getString("Type")).orElse(null);
            String dimensionId = tag.getString("Dimension");
            ResourceLocation dimension = parseDimension(dimensionId);
            SquadCallsign creatorSquad = SquadCallsign.byId(tag.getString("CreatorSquad")).orElse(null);
            double x = tag.getDouble("X");
            double y = tag.getDouble("Y");
            double z = tag.getDouble("Z");
            float direction = tag.getFloat("Direction");
            boolean hasPersistedEndpoint = tag.contains("EndX", Tag.TAG_ANY_NUMERIC)
                    && tag.contains("EndZ", Tag.TAG_ANY_NUMERIC);
            double endX = hasPersistedEndpoint ? tag.getDouble("EndX") : x;
            double endZ = hasPersistedEndpoint ? tag.getDouble("EndZ") : z;
            long rawCreatedAt = tag.getLong("CreatedAt");
            long rawExpiresAt = tag.getLong("ExpiresAt");
            long createdAt = Math.min(rawCreatedAt, now);
            long expiresAt = Math.min(rawExpiresAt,
                    saturatingAdd(createdAt, BattleRules.MAX_MARKER_TTL_MILLIS));
            StoredPlayer creator = creatorId == null ? null : players.get(creatorId);
            int factionCount = faction == null ? Integer.MAX_VALUE
                    : factionCounts.getOrDefault(faction, 0);
            int creatorCount = creatorId == null ? Integer.MAX_VALUE
                    : creatorCounts.getOrDefault(creatorId, 0);
            if (markerId == null || creatorId == null || faction == null || type == null
                    || dimension == null || !coordinatesFinite(x, y, z)
                    || (hasPersistedEndpoint && !coordinatesFinite(endX, y, endZ))
                    || (!hasPersistedEndpoint && type == TacticalMarkerType.ATTACK_DIRECTION
                    && !Float.isFinite(direction))
                    || rawCreatedAt < 0L || rawExpiresAt < 0L
                    || expiresAt <= now || expiresAt <= createdAt || markers.containsKey(markerId)
                    || creator == null || creator.faction != faction
                    || factionCount >= BattleRules.MAX_MARKERS_PER_FACTION
                    || creatorCount >= BattleRules.MAX_MARKERS_PER_CREATOR) {
                continue;
            }
            if (creatorSquad != null && creator.squad != creatorSquad) {
                creatorSquad = null;
            }
            TacticalMarker marker;
            try {
                marker = hasPersistedEndpoint
                        ? new TacticalMarker(markerId, faction, type, dimension,
                        x, y, z, endX, endZ, creatorId, creatorSquad, createdAt, expiresAt)
                        : new TacticalMarker(markerId, faction, type, dimension,
                        x, y, z, direction, creatorId, creatorSquad, createdAt, expiresAt);
            } catch (IllegalArgumentException invalidMarker) {
                continue;
            }
            markers.put(markerId, marker);
            factionCounts.put(faction, factionCount + 1);
            creatorCounts.put(creatorId, creatorCount + 1);
        }
    }

    private ListTag writePlayers() {
        ListTag list = new ListTag();
        Comparator<StoredPlayer> activeOrder = Comparator
                .comparingLong((StoredPlayer player) -> player.firstJoinedAtMillis)
                .thenComparing(player -> player.playerId);
        for (Faction faction : Faction.values()) {
            players.values().stream()
                    .filter(player -> player.faction == faction)
                    .sorted(activeOrder)
                    .forEach(player -> list.add(writePlayer(player)));
        }
        players.values().stream()
                .filter(player -> player.faction == null)
                .sorted(Comparator.comparingLong((StoredPlayer player) -> player.lastSeenAtMillis)
                        .reversed().thenComparing(player -> player.playerId))
                .forEach(player -> list.add(writePlayer(player)));
        return list;
    }

    private static CompoundTag writePlayer(StoredPlayer player) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", player.playerId);
        tag.putString("Name", truncate(player.lastKnownName, BattleRules.MAX_PLAYER_NAME_LENGTH));
        if (player.faction != null) {
            tag.putString("Faction", player.faction.id());
        }
        if (player.formationId != null && !player.formationId.isBlank()) {
            tag.putString("Formation", player.formationId);
        }
        if (player.squad != null) {
            tag.putString("Squad", player.squad.id());
        }
        tag.putString("Class", normalizeClassId(player.assignedClassId));
        tag.putBoolean("Admitted", player.admitted);
        tag.putLong("FirstJoined", Math.max(0L, player.firstJoinedAtMillis));
        tag.putLong("LastSeen", Math.max(0L, player.lastSeenAtMillis));
        tag.putLong("SquadJoined", Math.max(0L, player.squadJoinedAtMillis));
        return tag;
    }

    private ListTag writeLeaders() {
        ListTag list = new ListTag();
        List<Map.Entry<SquadKey, UUID>> ordered = new ArrayList<>(leaders.entrySet());
        ordered.sort(Map.Entry.comparingByKey(SquadKey.ORDER));
        for (Map.Entry<SquadKey, UUID> entry : ordered) {
            SquadKey key = entry.getKey();
            CompoundTag tag = new CompoundTag();
            tag.putString("Faction", key.faction().id());
            tag.putString("Formation", key.formationId());
            tag.putString("Squad", key.squad().id());
            tag.putUUID("Player", entry.getValue());
            list.add(tag);
        }
        return list;
    }

    private ListTag writeCommanders() {
        ListTag list = new ListTag();
        for (Map.Entry<Faction, UUID> entry : commanders.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Faction", entry.getKey().id());
            tag.putUUID("Player", entry.getValue());
            list.add(tag);
        }
        return list;
    }

    private ListTag writeMarkers() {
        ListTag list = new ListTag();
        for (TacticalMarker marker : markers.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", marker.id());
            tag.putString("Faction", marker.faction().id());
            tag.putString("Type", marker.type().id());
            tag.putString("Dimension", marker.dimension().toString());
            tag.putDouble("X", marker.x());
            tag.putDouble("Y", marker.y());
            tag.putDouble("Z", marker.z());
            tag.putDouble("EndX", marker.endX());
            tag.putDouble("EndZ", marker.endZ());
            // Retained as a derived field so old tools/readers still see a useful heading.
            tag.putFloat("Direction", marker.directionDegrees());
            tag.putUUID("Creator", marker.creatorId());
            if (marker.creatorSquad() != null) {
                tag.putString("CreatorSquad", marker.creatorSquad().id());
            }
            tag.putLong("CreatedAt", marker.createdAtMillis());
            tag.putLong("ExpiresAt", marker.expiresAtMillis());
            list.add(tag);
        }
        return list;
    }

    /** Repair redundant references and enforce hard capacities when loading potentially old data. */
    private boolean normalize() {
        boolean changed = false;
        long now = Math.max(0L, System.currentTimeMillis());
        for (StoredPlayer player : players.values()) {
            String normalizedName = truncate(player.lastKnownName,
                    BattleRules.MAX_PLAYER_NAME_LENGTH);
            if (!normalizedName.equals(player.lastKnownName)) {
                player.lastKnownName = normalizedName;
                changed = true;
            }
            String normalizedClass = normalizeClassId(player.assignedClassId);
            if (!normalizedClass.equals(player.assignedClassId)) {
                player.assignedClassId = normalizedClass;
                changed = true;
            }
            if (player.firstJoinedAtMillis <= 0L) {
                player.firstJoinedAtMillis = now;
                changed = true;
            }
            if (player.firstJoinedAtMillis > now) {
                player.firstJoinedAtMillis = now;
                changed = true;
            }
            if (player.lastSeenAtMillis < player.firstJoinedAtMillis) {
                player.lastSeenAtMillis = player.firstJoinedAtMillis;
                changed = true;
            }
            if (player.lastSeenAtMillis > now) {
                player.lastSeenAtMillis = now;
                changed = true;
            }
            String normalizedFormation = normalizeFormationId(player.formationId);
            if (!Objects.equals(normalizedFormation, player.formationId)) {
                player.formationId = normalizedFormation;
                changed = true;
            }
            // Version 6 intentionally permits a faction-only record while that faction votes.
            // A formation without a faction remains malformed and is cleared fail-closed.
            if (player.faction == null && player.formationId != null) {
                player.formationId = null;
                changed = true;
            }
            if ((player.faction == null || player.formationId == null)
                    && player.squad != null) {
                player.squad = null;
                player.squadJoinedAtMillis = 0L;
                player.assignedClassId = BattleRules.DEFAULT_CLASS_ID;
                changed = true;
            }
            if (player.squad == null && player.squadJoinedAtMillis != 0L) {
                player.squadJoinedAtMillis = 0L;
                changed = true;
            } else if (player.squad != null
                    && (player.squadJoinedAtMillis < player.firstJoinedAtMillis
                    || player.squadJoinedAtMillis > now)) {
                player.squadJoinedAtMillis = player.firstJoinedAtMillis;
                changed = true;
            }
        }

        for (Faction faction : Faction.values()) {
            List<StoredPlayer> factionPlayers = players.values().stream()
                    .filter(player -> player.faction == faction)
                    .sorted(Comparator.comparingLong((StoredPlayer player) -> player.firstJoinedAtMillis)
                            .thenComparing(player -> player.playerId))
                    .toList();
            for (int i = BattleRules.FACTION_CAPACITY; i < factionPlayers.size(); i++) {
                StoredPlayer overflow = factionPlayers.get(i);
                overflow.faction = null;
                overflow.formationId = null;
                overflow.squad = null;
                overflow.squadJoinedAtMillis = 0L;
                overflow.assignedClassId = BattleRules.DEFAULT_CLASS_ID;
                changed = true;
            }
        }

        Map<SquadKey, List<StoredPlayer>> membersBySquad = new LinkedHashMap<>();
        for (StoredPlayer player : players.values()) {
            SquadKey key = SquadKey.create(player.faction, player.formationId, player.squad);
            if (key != null) {
                membersBySquad.computeIfAbsent(key, ignored -> new ArrayList<>()).add(player);
            }
        }
        if (leaders.keySet().removeIf(key -> !membersBySquad.containsKey(key))) {
            changed = true;
        }
        List<SquadKey> squadKeys = new ArrayList<>(membersBySquad.keySet());
        squadKeys.sort(SquadKey.ORDER);
        for (SquadKey key : squadKeys) {
            List<StoredPlayer> members = membersBySquad.get(key);
            members.sort(Comparator.comparingLong((StoredPlayer player) -> player.squadJoinedAtMillis)
                    .thenComparingLong(player -> player.firstJoinedAtMillis)
                    .thenComparing(player -> player.playerId));
            for (int i = BattleRules.SQUAD_CAPACITY; i < members.size(); i++) {
                StoredPlayer overflow = members.get(i);
                overflow.squad = null;
                overflow.squadJoinedAtMillis = 0L;
                overflow.assignedClassId = BattleRules.DEFAULT_CLASS_ID;
                changed = true;
            }
            if (members.size() > BattleRules.SQUAD_CAPACITY) {
                members = members.subList(0, BattleRules.SQUAD_CAPACITY);
            }
            UUID currentLeader = leader(key.faction(), key.formationId(), key.squad());
            boolean leaderValid = currentLeader != null && members.stream()
                    .anyMatch(member -> member.playerId.equals(currentLeader));
            if (!leaderValid) {
                setLeader(key.faction(), key.formationId(), key.squad(), members.get(0).playerId);
                changed = true;
            }
        }

        for (Faction faction : Faction.values()) {
            UUID commanderId = commander(faction);
            StoredPlayer commander = commanderId == null ? null : player(commanderId);
            if (commanderId != null && (commander == null || commander.faction != faction)) {
                setCommander(faction, null);
                changed = true;
            }
        }

        Iterator<TacticalMarker> markerIterator = markers.values().iterator();
        EnumMap<Faction, Integer> factionMarkerCounts = new EnumMap<>(Faction.class);
        Map<UUID, Integer> creatorMarkerCounts = new LinkedHashMap<>();
        while (markerIterator.hasNext()) {
            TacticalMarker marker = markerIterator.next();
            int factionCount = factionMarkerCounts.getOrDefault(marker.faction(), 0);
            int creatorCount = creatorMarkerCounts.getOrDefault(marker.creatorId(), 0);
            StoredPlayer creator = player(marker.creatorId());
            long markerTtl = marker.expiresAtMillis() >= marker.createdAtMillis()
                    ? marker.expiresAtMillis() - marker.createdAtMillis() : Long.MAX_VALUE;
            if (marker.expiredAt(now) || marker.createdAtMillis() < 0L
                    || marker.createdAtMillis() > now || markerTtl > BattleRules.MAX_MARKER_TTL_MILLIS
                    || parseDimension(marker.dimension().toString()) == null
                    || !coordinatesFinite(marker.x(), marker.y(), marker.z())
                    || !coordinatesFinite(marker.endX(), marker.y(), marker.endZ())
                    || (marker.type() == TacticalMarkerType.ATTACK_DIRECTION
                    && !TacticalMarker.isValidAttackGeometry(marker.x(), marker.z(),
                    marker.endX(), marker.endZ()))
                    || !Float.isFinite(marker.directionDegrees())
                    || creator == null || creator.faction != marker.faction()
                    || factionCount >= BattleRules.MAX_MARKERS_PER_FACTION
                    || creatorCount >= BattleRules.MAX_MARKERS_PER_CREATOR) {
                markerIterator.remove();
                changed = true;
                continue;
            }
            factionMarkerCounts.put(marker.faction(), factionCount + 1);
            creatorMarkerCounts.put(marker.creatorId(), creatorCount + 1);
        }
        return changed;
    }

    private static StoredPlayer readPlayer(CompoundTag tag, long now) {
        UUID playerId = readUuid(tag, "Id").orElse(null);
        if (playerId == null) {
            return null;
        }
        StoredPlayer player = new StoredPlayer(playerId);
        player.lastKnownName = truncate(tag.getString("Name"),
                BattleRules.MAX_PLAYER_NAME_LENGTH);
        player.faction = Faction.byId(tag.getString("Faction")).orElse(null);
        player.formationId = normalizeFormationId(tag.getString("Formation"));
        player.squad = SquadCallsign.byId(tag.getString("Squad")).orElse(null);
        player.assignedClassId = normalizeClassId(tag.getString("Class"));
        player.admitted = !tag.contains("Admitted", Tag.TAG_BYTE) || tag.getBoolean("Admitted");

        long firstJoined = clampTimestamp(tag.getLong("FirstJoined"), now);
        long lastSeen = clampTimestamp(tag.getLong("LastSeen"), now);
        if (firstJoined == 0L) {
            firstJoined = lastSeen == 0L ? now : lastSeen;
        }
        if (lastSeen == 0L) {
            lastSeen = firstJoined;
        }
        if (lastSeen < firstJoined) {
            firstJoined = lastSeen;
        }
        player.firstJoinedAtMillis = firstJoined;
        player.lastSeenAtMillis = lastSeen;

        if (player.faction == null) {
            player.formationId = null;
            player.squad = null;
            player.assignedClassId = BattleRules.DEFAULT_CLASS_ID;
        } else if (player.formationId == null) {
            player.squad = null;
            player.assignedClassId = BattleRules.DEFAULT_CLASS_ID;
        }
        if (player.squad == null) {
            player.squadJoinedAtMillis = 0L;
        } else {
            long squadJoined = clampTimestamp(tag.getLong("SquadJoined"), now);
            player.squadJoinedAtMillis = squadJoined < firstJoined ? firstJoined : squadJoined;
        }
        return player;
    }

    private static Optional<UUID> readUuid(CompoundTag tag, String key) {
        if (!tag.hasUUID(key)) {
            return Optional.empty();
        }
        try {
            return Optional.of(tag.getUUID(key));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static String normalizeClassId(String classId) {
        if (classId == null || classId.isBlank()) {
            return BattleRules.DEFAULT_CLASS_ID;
        }
        String trimmed = classId.trim();
        return truncate(trimmed, BattleRules.MAX_CLASS_ID_LENGTH);
    }

    private static String normalizeFormationId(String formationId) {
        if (formationId == null || formationId.isBlank()) {
            return null;
        }
        String trimmed = formationId.trim();
        if (trimmed.length() > BattleRules.MAX_FORMATION_ID_LENGTH) {
            return null;
        }
        for (int index = 0; index < trimmed.length(); index++) {
            char value = trimmed.charAt(index);
            if (!(value >= 'a' && value <= 'z') && !(value >= '0' && value <= '9')
                    && value != '_' && value != '-' && value != '.') {
                return null;
            }
        }
        return trimmed;
    }

    private static boolean coordinatesFinite(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Math.abs(x) <= BattleRules.MAX_COORDINATE
                && Math.abs(y) <= BattleRules.MAX_COORDINATE
                && Math.abs(z) <= BattleRules.MAX_COORDINATE;
    }

    private static ResourceLocation parseDimension(String encoded) {
        if (encoded == null || encoded.isBlank()
                || encoded.length() > BattleRules.MAX_DIMENSION_ID_LENGTH) {
            return null;
        }
        try {
            ResourceLocation parsed = ResourceLocation.tryParse(encoded);
            return parsed != null
                    && parsed.toString().length() <= BattleRules.MAX_DIMENSION_ID_LENGTH
                    ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String truncate(String value, int maximumLength) {
        String safe = Objects.requireNonNullElse(value, "");
        return safe.length() <= maximumLength ? safe : safe.substring(0, maximumLength);
    }

    private static long clampTimestamp(long timestamp, long now) {
        return Math.min(Math.max(0L, timestamp), now);
    }

    private static long elapsedSince(long now, long timestamp) {
        return now >= timestamp ? now - timestamp : 0L;
    }

    private static long saturatingAdd(long base, long positiveDelta) {
        return base > Long.MAX_VALUE - positiveDelta ? Long.MAX_VALUE : base + positiveDelta;
    }

    private record SquadKey(Faction faction, String formationId, SquadCallsign squad) {
        private static final Comparator<SquadKey> ORDER =
                Comparator.comparingInt((SquadKey key) -> key.faction().ordinal())
                        .thenComparing(SquadKey::formationId)
                        .thenComparingInt(key -> key.squad().ordinal());

        private static SquadKey create(Faction faction, String formationId,
                                       SquadCallsign squad) {
            String normalizedFormation = normalizeFormationId(formationId);
            return faction == null || normalizedFormation == null || squad == null
                    ? null : new SquadKey(faction, normalizedFormation, squad);
        }
    }

    static final class StoredPlayer {
        final UUID playerId;
        String lastKnownName = "";
        Faction faction;
        String formationId;
        SquadCallsign squad;
        boolean admitted = true;
        String assignedClassId = BattleRules.DEFAULT_CLASS_ID;
        long firstJoinedAtMillis;
        long lastSeenAtMillis;
        long squadJoinedAtMillis;

        StoredPlayer(UUID playerId) {
            this.playerId = Objects.requireNonNull(playerId, "playerId");
        }

        PlayerRecord view() {
            return new PlayerRecord(playerId, lastKnownName, faction, formationId, squad,
                    assignedClassId,
                    firstJoinedAtMillis, lastSeenAtMillis);
        }
    }
}
