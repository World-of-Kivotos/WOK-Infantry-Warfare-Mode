package com.wok.infantry.deployment;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadCallsign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** World-persistent faction deployment points; per-life state intentionally remains runtime-only. */
final class DeploymentSavedData extends SavedData {
    private static final String DATA_NAME = "wok_infantry_deployment";
    static final int DATA_VERSION = 5;
    static final int MAX_FIELD_POINTS_PER_FACTION = 15;
    private static final int MAX_FIELD_POINTS_TOTAL =
            MAX_FIELD_POINTS_PER_FACTION * Faction.values().length;
    private static final ResourceLocation HOLDING_DIMENSION =
            ResourceLocation.fromNamespaceAndPath("wok_infantry", "holding");
    private static final ResourceLocation LOBBY_DIMENSION =
            ResourceLocation.fromNamespaceAndPath("wok_infantry", "lobby");
    private static final Comparator<FieldDeploymentPoint> FIELD_POINT_ORDER =
            Comparator.comparing((FieldDeploymentPoint point) -> point.dimension().toString())
                    .thenComparingInt(point -> point.anchorPosition().getX())
                    .thenComparingInt(point -> point.anchorPosition().getY())
                    .thenComparingInt(point -> point.anchorPosition().getZ())
                    .thenComparing(FieldDeploymentPoint::id);

    private final EnumMap<Faction, DeploymentPoint> mainBases = new EnumMap<>(Faction.class);
    private final EnumMap<Faction, VehicleDeploymentPoint> vehiclePoints =
            new EnumMap<>(Faction.class);
    private final EnumMap<Faction, LinkedHashMap<UUID, FieldDeploymentPoint>> fieldPoints =
            new EnumMap<>(Faction.class);
    private final Map<UUID, FieldDeploymentPoint> fieldPointsById = new HashMap<>();
    private final Map<AnchorKey, UUID> fieldPointIdsByAnchor = new HashMap<>();
    private final Map<UUID, RallyDeploymentPoint> ralliesById = new HashMap<>();
    private final Map<AnchorKey, UUID> rallyIdsByAnchor = new HashMap<>();
    private final Map<RallyCooldownKey, Long> rallyCooldownReadyAt = new HashMap<>();

    DeploymentSavedData() {
        for (Faction faction : Faction.values()) {
            fieldPoints.put(faction, new LinkedHashMap<>());
        }
    }

    static DeploymentSavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(
                DeploymentSavedData::load, DeploymentSavedData::new, DATA_NAME);
    }

    static DeploymentSavedData load(CompoundTag root) {
        DeploymentSavedData data = new DeploymentSavedData();
        int version = root.contains("Version", Tag.TAG_ANY_NUMERIC)
                ? root.getInt("Version") : 1;
        if (version < 1 || version > DATA_VERSION) {
            data.setDirty();
            return data;
        }

        ListTag list = root.getList("MainBases", Tag.TAG_COMPOUND);
        int limit = Math.min(list.size(), Faction.values().length * 2);
        Set<UUID> acceptedIds = new HashSet<>();
        for (int index = 0; index < limit; index++) {
            CompoundTag tag = list.getCompound(index);
            Faction faction = Faction.byId(tag.getString("Faction")).orElse(null);
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
            UUID id = tag.hasUUID("Id") ? tag.getUUID("Id") : null;
            int x = tag.getInt("X");
            int y = tag.getInt("Y");
            int z = tag.getInt("Z");
            float yaw = tag.getFloat("Yaw");
            int radius = tag.getInt("SupplyRadius");
            if (faction == null || dimension == null || internalDimension(dimension)
                    || id == null || acceptedIds.contains(id)
                    || !Float.isFinite(yaw) || yaw < 0.0F || yaw >= 360.0F
                    || dimension.toString().length() > BattleRules.MAX_DIMENSION_ID_LENGTH
                    || !validCoordinate(x) || !validCoordinate(y) || !validCoordinate(z)) {
                continue;
            }
            if (radius < 1 || radius > 64) {
                radius = DeploymentPoint.DEFAULT_SUPPLY_RADIUS;
            }
            if (!data.mainBases.containsKey(faction)) {
                data.mainBases.put(faction, new DeploymentPoint(id, faction, dimension,
                        new BlockPos(x, y, z), yaw, radius));
                acceptedIds.add(id);
            }
        }

        if (version >= 2) {
            ListTag fieldList = root.getList("FieldPoints", Tag.TAG_COMPOUND);
            int fieldLimit = Math.min(fieldList.size(), MAX_FIELD_POINTS_TOTAL * 2);
            for (int index = 0; index < fieldLimit; index++) {
                FieldDeploymentPoint point = readFieldPoint(fieldList.getCompound(index));
                if (point == null || acceptedIds.contains(point.id())
                        || !data.insertNewFieldPoint(point, false)) {
                    continue;
                }
                acceptedIds.add(point.id());
            }
        }
        if (version >= 3) {
            ListTag vehicleList = root.getList("VehiclePoints", Tag.TAG_COMPOUND);
            int vehicleLimit = Math.min(vehicleList.size(), Faction.values().length * 2);
            for (int index = 0; index < vehicleLimit; index++) {
                VehicleDeploymentPoint point = readVehiclePoint(
                        vehicleList.getCompound(index));
                if (point != null) {
                    data.insertVehiclePoint(point, false);
                }
            }
        }
        if (version >= 4) {
            ListTag rallyList = root.getList("Rallies", Tag.TAG_COMPOUND);
            int rallyLimit = Math.min(rallyList.size(), 256);
            for (int index = 0; index < rallyLimit; index++) {
                RallyDeploymentPoint point = readRallyPoint(rallyList.getCompound(index));
                if (point != null && !acceptedIds.contains(point.id())
                        && data.putRally(point, false)) {
                    acceptedIds.add(point.id());
                }
            }
        }
        if (version >= 5) {
            ListTag cooldownList = root.getList("RallyCooldowns", Tag.TAG_COMPOUND);
            int cooldownLimit = Math.min(cooldownList.size(), 2_048);
            for (int index = 0; index < cooldownLimit
                    && data.rallyCooldownReadyAt.size() < 1_024; index++) {
                CompoundTag tag = cooldownList.getCompound(index);
                Faction faction = Faction.byId(tag.getString("Faction")).orElse(null);
                SquadCallsign squad = SquadCallsign.byId(tag.getString("Squad"))
                        .orElse(null);
                String formation = tag.getString("Formation").trim();
                long readyAt = tag.getLong("ReadyAt");
                if (faction == null || squad == null || formation.isEmpty()
                        || formation.length() > 64 || readyAt <= 0L) {
                    continue;
                }
                data.rallyCooldownReadyAt.put(
                        new RallyCooldownKey(faction, formation, squad), readyAt);
            }
        }
        // Canonicalize corrupt/old input on the next world save.
        data.setDirty();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putInt("Version", DATA_VERSION);
        ListTag list = new ListTag();
        for (Map.Entry<Faction, DeploymentPoint> entry : mainBases.entrySet()) {
            DeploymentPoint point = entry.getValue();
            CompoundTag tag = new CompoundTag();
            tag.putString("Faction", entry.getKey().id());
            tag.putUUID("Id", point.id());
            tag.putString("Dimension", point.dimension().toString());
            tag.putInt("X", point.position().getX());
            tag.putInt("Y", point.position().getY());
            tag.putInt("Z", point.position().getZ());
            tag.putFloat("Yaw", point.yaw());
            tag.putInt("SupplyRadius", point.supplyRadius());
            list.add(tag);
        }
        root.put("MainBases", list);

        ListTag fieldList = new ListTag();
        allFieldPoints().forEach(point -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", point.id());
            tag.putString("Faction", point.faction().id());
            tag.putString("Dimension", point.dimension().toString());
            tag.putInt("AnchorX", point.anchorPosition().getX());
            tag.putInt("AnchorY", point.anchorPosition().getY());
            tag.putInt("AnchorZ", point.anchorPosition().getZ());
            tag.putInt("SpawnX", point.spawnPosition().getX());
            tag.putInt("SpawnY", point.spawnPosition().getY());
            tag.putInt("SpawnZ", point.spawnPosition().getZ());
            tag.putFloat("Yaw", point.yaw());
            fieldList.add(tag);
        });
        root.put("FieldPoints", fieldList);

        ListTag vehicleList = new ListTag();
        for (Faction faction : Faction.values()) {
            VehicleDeploymentPoint point = vehiclePoints.get(faction);
            if (point == null) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("Faction", point.faction().id());
            tag.putString("Dimension", point.dimension().toString());
            tag.putInt("X", point.anchorPosition().getX());
            tag.putInt("Y", point.anchorPosition().getY());
            tag.putInt("Z", point.anchorPosition().getZ());
            tag.putString("Facing", point.facing().getName());
            vehicleList.add(tag);
        }
        root.put("VehiclePoints", vehicleList);

        ListTag rallyList = new ListTag();
        ralliesById.values().stream().sorted(Comparator.comparing(
                RallyDeploymentPoint::id)).forEach(point -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", point.id());
            tag.putString("Faction", point.faction().id());
            tag.putString("Formation", point.formationId());
            tag.putString("Squad", point.squad().id());
            tag.putString("Dimension", point.dimension().toString());
            tag.putInt("AnchorX", point.anchorPosition().getX());
            tag.putInt("AnchorY", point.anchorPosition().getY());
            tag.putInt("AnchorZ", point.anchorPosition().getZ());
            tag.putInt("SpawnX", point.spawnPosition().getX());
            tag.putInt("SpawnY", point.spawnPosition().getY());
            tag.putInt("SpawnZ", point.spawnPosition().getZ());
            tag.putFloat("Yaw", point.yaw());
            rallyList.add(tag);
        });
        root.put("Rallies", rallyList);

        ListTag cooldownList = new ListTag();
        rallyCooldownReadyAt.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("Faction", entry.getKey().faction().id());
                    tag.putString("Formation", entry.getKey().formationId());
                    tag.putString("Squad", entry.getKey().squad().id());
                    tag.putLong("ReadyAt", entry.getValue());
                    cooldownList.add(tag);
                });
        root.put("RallyCooldowns", cooldownList);
        return root;
    }

    long rallyCooldownRemainingTicks(Faction faction, String formationId,
                                     SquadCallsign squad, long gameTime) {
        RallyCooldownKey key = RallyCooldownKey.of(faction, formationId, squad);
        if (key == null) {
            return 0L;
        }
        Long readyAt = rallyCooldownReadyAt.get(key);
        if (readyAt == null) {
            return 0L;
        }
        if (readyAt <= gameTime) {
            rallyCooldownReadyAt.remove(key);
            setDirty();
            return 0L;
        }
        return readyAt - gameTime;
    }

    void startRallyCooldown(Faction faction, String formationId,
                            SquadCallsign squad, long readyAt) {
        RallyCooldownKey key = RallyCooldownKey.of(faction, formationId, squad);
        if (key == null || readyAt <= 0L) {
            throw new IllegalArgumentException("Invalid rally cooldown identity or deadline");
        }
        if (!Objects.equals(rallyCooldownReadyAt.put(key, readyAt), readyAt)) {
            setDirty();
        }
    }

    Optional<DeploymentPoint> mainBase(Faction faction) {
        return Optional.ofNullable(faction == null ? null : mainBases.get(faction));
    }

    void setMainBase(DeploymentPoint point) {
        Objects.requireNonNull(point, "point");
        DeploymentPoint current = mainBases.get(point.faction());
        if ((current == null || !current.id().equals(point.id()))
                && containsPointId(point.id())) {
            throw new IllegalArgumentException("Deployment point UUID is already in use");
        }
        mainBases.put(point.faction(), point);
        setDirty();
    }

    boolean clearMainBase(Faction faction) {
        if (faction == null || mainBases.remove(faction) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    Optional<VehicleDeploymentPoint> vehiclePoint(Faction faction) {
        return Optional.ofNullable(faction == null ? null : vehiclePoints.get(faction));
    }

    Optional<VehicleDeploymentPoint> vehiclePointAt(ResourceLocation dimension,
                                                     BlockPos anchor) {
        if (dimension == null || anchor == null) {
            return Optional.empty();
        }
        return vehiclePoints.values().stream().filter(point ->
                point.dimension().equals(dimension)
                        && point.anchorPosition().equals(anchor)).findFirst();
    }

    /** Binds or rotates one anchor; each faction and each physical anchor remain unique. */
    boolean bindVehiclePoint(VehicleDeploymentPoint point) {
        return insertVehiclePoint(Objects.requireNonNull(point, "point"), true);
    }

    Optional<VehicleDeploymentPoint> removeVehiclePoint(ResourceLocation dimension,
                                                         BlockPos anchor) {
        VehicleDeploymentPoint current = vehiclePointAt(dimension, anchor).orElse(null);
        if (current == null || !vehiclePoints.remove(current.faction(), current)) {
            return Optional.empty();
        }
        setDirty();
        return Optional.of(current);
    }

    List<FieldDeploymentPoint> fieldPoints(Faction faction) {
        LinkedHashMap<UUID, FieldDeploymentPoint> byId = faction == null
                ? null : fieldPoints.get(faction);
        if (byId == null || byId.isEmpty()) {
            return List.of();
        }
        List<FieldDeploymentPoint> points = new ArrayList<>(byId.values());
        points.sort(FIELD_POINT_ORDER);
        return List.copyOf(points);
    }

    Optional<FieldDeploymentPoint> fieldPoint(UUID id) {
        return Optional.ofNullable(id == null ? null : fieldPointsById.get(id));
    }

    Optional<FieldDeploymentPoint> fieldPointAt(ResourceLocation dimension, BlockPos anchor) {
        if (dimension == null || anchor == null) {
            return Optional.empty();
        }
        UUID id = fieldPointIdsByAnchor.get(new AnchorKey(dimension, anchor));
        return Optional.ofNullable(id == null ? null : fieldPointsById.get(id));
    }

    boolean containsPointId(UUID id) {
        if (id == null) {
            return false;
        }
        return fieldPointsById.containsKey(id) || ralliesById.containsKey(id)
                || mainBases.values().stream().anyMatch(point -> id.equals(point.id()));
    }

    List<RallyDeploymentPoint> rallies(Faction faction, String formationId,
                                        SquadCallsign squad) {
        if (faction == null || formationId == null || squad == null) return List.of();
        return ralliesById.values().stream()
                .filter(point -> point.faction() == faction
                        && point.formationId().equals(formationId)
                        && point.squad() == squad)
                .sorted(Comparator.comparing((RallyDeploymentPoint point) ->
                                point.dimension().toString())
                        .thenComparing(point -> point.anchorPosition().asLong()))
                .toList();
    }

    Optional<RallyDeploymentPoint> rally(UUID id) {
        return Optional.ofNullable(id == null ? null : ralliesById.get(id));
    }

    Optional<RallyDeploymentPoint> rallyAt(ResourceLocation dimension, BlockPos anchor) {
        if (dimension == null || anchor == null) return Optional.empty();
        UUID id = rallyIdsByAnchor.get(new AnchorKey(dimension, anchor));
        return Optional.ofNullable(id == null ? null : ralliesById.get(id));
    }

    boolean putRally(RallyDeploymentPoint point) {
        return putRally(point, true);
    }

    private boolean putRally(RallyDeploymentPoint point, boolean dirty) {
        if (!validRallyPoint(point) || containsPointId(point.id())) return false;
        AnchorKey key = new AnchorKey(point.dimension(), point.anchorPosition());
        if (rallyIdsByAnchor.containsKey(key) || fieldPointIdsByAnchor.containsKey(key)
                || vehiclePointAt(point.dimension(), point.anchorPosition()).isPresent()) {
            return false;
        }
        ralliesById.put(point.id(), point);
        rallyIdsByAnchor.put(key, point.id());
        if (dirty) setDirty();
        return true;
    }

    Optional<RallyDeploymentPoint> removeRally(ResourceLocation dimension, BlockPos anchor) {
        if (dimension == null || anchor == null) return Optional.empty();
        AnchorKey key = new AnchorKey(dimension, anchor);
        UUID id = rallyIdsByAnchor.remove(key);
        RallyDeploymentPoint removed = id == null ? null : ralliesById.remove(id);
        if (removed != null) setDirty();
        return Optional.ofNullable(removed);
    }

    /** Adds a new record; only an exact replay is accepted as an idempotent success. */
    boolean putFieldPoint(FieldDeploymentPoint point) {
        return insertNewFieldPoint(Objects.requireNonNull(point, "point"), true);
    }

    /** Compare-and-replaces one anchor without opening UUID or anchor uniqueness races. */
    boolean replaceFieldPointAtAnchor(FieldDeploymentPoint point, UUID expectedCurrentId) {
        Objects.requireNonNull(point, "point");
        if (!validFieldPoint(point)) {
            return false;
        }
        AnchorKey key = AnchorKey.of(point);
        if (vehiclePointAt(point.dimension(), point.anchorPosition()).isPresent()
                || rallyIdsByAnchor.containsKey(key)) {
            return false;
        }
        UUID anchoredId = fieldPointIdsByAnchor.get(key);
        if (!Objects.equals(anchoredId, expectedCurrentId)) {
            return false;
        }
        FieldDeploymentPoint current = anchoredId == null
                ? null : fieldPointsById.get(anchoredId);
        FieldDeploymentPoint withReplacementId = fieldPointsById.get(point.id());
        if (mainBaseUsesId(point.id()) || withReplacementId != null
                && (current == null || !withReplacementId.id().equals(current.id()))) {
            return false;
        }
        if (current != null && current.faction() != point.faction()
                && current.id().equals(point.id())) {
            return false;
        }
        if (current != null && current.equals(point)) {
            return true;
        }
        int factionCount = fieldPoints.get(point.faction()).size();
        boolean replacesSameFaction = current != null && current.faction() == point.faction();
        if (!replacesSameFaction && factionCount >= MAX_FIELD_POINTS_PER_FACTION) {
            return false;
        }
        if (current != null) {
            removeIndexed(current);
        }
        addIndexed(point);
        setDirty();
        return true;
    }

    Optional<FieldDeploymentPoint> removeFieldPoint(ResourceLocation dimension, BlockPos anchor,
                                                      UUID expectedId) {
        if (dimension == null || anchor == null || expectedId == null) {
            return Optional.empty();
        }
        AnchorKey key = new AnchorKey(dimension, anchor);
        UUID indexedId = fieldPointIdsByAnchor.get(key);
        if (!expectedId.equals(indexedId)) {
            return Optional.empty();
        }
        FieldDeploymentPoint removed = fieldPointsById.get(expectedId);
        if (removed == null || !key.equals(AnchorKey.of(removed))) {
            return Optional.empty();
        }
        removeIndexed(removed);
        setDirty();
        return Optional.of(removed);
    }

    private boolean insertNewFieldPoint(FieldDeploymentPoint point, boolean dirty) {
        if (!validFieldPoint(point)) {
            return false;
        }
        AnchorKey key = AnchorKey.of(point);
        UUID anchoredId = fieldPointIdsByAnchor.get(key);
        FieldDeploymentPoint withId = fieldPointsById.get(point.id());
        if (mainBaseUsesId(point.id())
                || vehiclePointAt(point.dimension(), point.anchorPosition()).isPresent()
                || rallyIdsByAnchor.containsKey(key)) {
            return false;
        }
        if (anchoredId != null || withId != null) {
            return withId != null && withId.equals(point)
                    && point.id().equals(anchoredId);
        }
        if (fieldPoints.get(point.faction()).size() >= MAX_FIELD_POINTS_PER_FACTION) {
            return false;
        }
        addIndexed(point);
        if (dirty) {
            setDirty();
        }
        return true;
    }

    private void addIndexed(FieldDeploymentPoint point) {
        fieldPoints.get(point.faction()).put(point.id(), point);
        fieldPointsById.put(point.id(), point);
        fieldPointIdsByAnchor.put(AnchorKey.of(point), point.id());
    }

    private void removeIndexed(FieldDeploymentPoint point) {
        fieldPoints.get(point.faction()).remove(point.id());
        fieldPointsById.remove(point.id());
        fieldPointIdsByAnchor.remove(AnchorKey.of(point), point.id());
    }

    private boolean insertVehiclePoint(VehicleDeploymentPoint point, boolean dirty) {
        if (!validVehiclePoint(point)
                || fieldPointIdsByAnchor.containsKey(new AnchorKey(point.dimension(),
                point.anchorPosition()))
                || rallyIdsByAnchor.containsKey(new AnchorKey(point.dimension(),
                point.anchorPosition()))) {
            return false;
        }
        VehicleDeploymentPoint factionPoint = vehiclePoints.get(point.faction());
        VehicleDeploymentPoint anchorPoint = vehiclePointAt(point.dimension(),
                point.anchorPosition()).orElse(null);
        if (factionPoint != null && !sameAnchor(factionPoint, point)) {
            return false;
        }
        if (anchorPoint != null && anchorPoint.faction() != point.faction() && !dirty) {
            // Corrupt or hand-edited saves keep the first valid record instead of
            // making load order silently transfer one physical block between sides.
            return false;
        }
        if (anchorPoint != null && anchorPoint.faction() != point.faction()) {
            vehiclePoints.remove(anchorPoint.faction(), anchorPoint);
        }
        if (point.equals(factionPoint)) {
            return true;
        }
        vehiclePoints.put(point.faction(), point);
        if (dirty) {
            setDirty();
        }
        return true;
    }

    private List<FieldDeploymentPoint> allFieldPoints() {
        List<FieldDeploymentPoint> points = new ArrayList<>(fieldPointsById.values());
        points.sort(FIELD_POINT_ORDER);
        return points;
    }

    private boolean mainBaseUsesId(UUID id) {
        return mainBases.values().stream().anyMatch(point -> id.equals(point.id()));
    }

    private static FieldDeploymentPoint readFieldPoint(CompoundTag tag) {
        if (!tag.hasUUID("Id")
                || !tag.contains("Faction", Tag.TAG_STRING)
                || !tag.contains("Dimension", Tag.TAG_STRING)
                || !tag.contains("AnchorX", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("AnchorY", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("AnchorZ", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("SpawnX", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("SpawnY", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("SpawnZ", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("Yaw", Tag.TAG_ANY_NUMERIC)) {
            return null;
        }
        UUID id = tag.getUUID("Id");
        Faction faction = Faction.byId(tag.getString("Faction")).orElse(null);
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        int anchorX = tag.getInt("AnchorX");
        int anchorY = tag.getInt("AnchorY");
        int anchorZ = tag.getInt("AnchorZ");
        int spawnX = tag.getInt("SpawnX");
        int spawnY = tag.getInt("SpawnY");
        int spawnZ = tag.getInt("SpawnZ");
        float yaw = tag.getFloat("Yaw");
        if (faction == null || dimension == null || internalDimension(dimension)
                || dimension.toString().length() > BattleRules.MAX_DIMENSION_ID_LENGTH
                || !validCoordinate(anchorX) || !validCoordinate(anchorY)
                || !validCoordinate(anchorZ) || !validCoordinate(spawnX)
                || !validCoordinate(spawnY) || !validCoordinate(spawnZ)
                || !Float.isFinite(yaw) || yaw < 0.0F || yaw >= 360.0F) {
            return null;
        }
        if (Math.abs(spawnX - anchorX) > 2 || Math.abs(spawnZ - anchorZ) > 2
                || spawnY - anchorY < 1 || spawnY - anchorY > 3) {
            return null;
        }
        return new FieldDeploymentPoint(id, faction, dimension,
                new BlockPos(anchorX, anchorY, anchorZ),
                new BlockPos(spawnX, spawnY, spawnZ), yaw);
    }

    private static VehicleDeploymentPoint readVehiclePoint(CompoundTag tag) {
        if (!tag.contains("Faction", Tag.TAG_STRING)
                || !tag.contains("Dimension", Tag.TAG_STRING)
                || !tag.contains("X", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("Y", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("Z", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("Facing", Tag.TAG_STRING)) {
            return null;
        }
        Faction faction = Faction.byId(tag.getString("Faction")).orElse(null);
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        Direction facing = Direction.byName(tag.getString("Facing"));
        int x = tag.getInt("X");
        int y = tag.getInt("Y");
        int z = tag.getInt("Z");
        if (faction == null || dimension == null || facing == null
                || !facing.getAxis().isHorizontal() || internalDimension(dimension)
                || dimension.toString().length() > BattleRules.MAX_DIMENSION_ID_LENGTH
                || !validCoordinate(x) || !validCoordinate(y) || !validCoordinate(z)) {
            return null;
        }
        return new VehicleDeploymentPoint(faction, dimension, new BlockPos(x, y, z), facing);
    }

    private static RallyDeploymentPoint readRallyPoint(CompoundTag tag) {
        if (!tag.hasUUID("Id") || !tag.contains("Faction", Tag.TAG_STRING)
                || !tag.contains("Formation", Tag.TAG_STRING)
                || !tag.contains("Squad", Tag.TAG_STRING)
                || !tag.contains("Dimension", Tag.TAG_STRING)) return null;
        Faction faction = Faction.byId(tag.getString("Faction")).orElse(null);
        SquadCallsign squad = SquadCallsign.byId(tag.getString("Squad")).orElse(null);
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        String formation = tag.getString("Formation").trim();
        int anchorX = tag.getInt("AnchorX");
        int anchorY = tag.getInt("AnchorY");
        int anchorZ = tag.getInt("AnchorZ");
        int spawnX = tag.getInt("SpawnX");
        int spawnY = tag.getInt("SpawnY");
        int spawnZ = tag.getInt("SpawnZ");
        float yaw = tag.getFloat("Yaw");
        if (faction == null || squad == null || dimension == null || formation.isEmpty()
                || internalDimension(dimension) || !Float.isFinite(yaw)
                || yaw < 0.0F || yaw >= 360.0F
                || !validCoordinate(anchorX) || !validCoordinate(anchorY)
                || !validCoordinate(anchorZ) || !validCoordinate(spawnX)
                || !validCoordinate(spawnY) || !validCoordinate(spawnZ)
                || Math.abs(spawnX - anchorX) > 2 || Math.abs(spawnZ - anchorZ) > 2
                || spawnY - anchorY < 1 || spawnY - anchorY > 3) return null;
        return new RallyDeploymentPoint(tag.getUUID("Id"), faction, formation, squad,
                dimension, new BlockPos(anchorX, anchorY, anchorZ),
                new BlockPos(spawnX, spawnY, spawnZ), yaw);
    }

    private static boolean validCoordinate(int coordinate) {
        return Math.abs((double) coordinate) <= BattleRules.MAX_COORDINATE;
    }

    private static boolean validFieldPoint(FieldDeploymentPoint point) {
        return !internalDimension(point.dimension())
                && point.dimension().toString().length() <= BattleRules.MAX_DIMENSION_ID_LENGTH
                && validCoordinate(point.anchorPosition().getX())
                && validCoordinate(point.anchorPosition().getY())
                && validCoordinate(point.anchorPosition().getZ())
                && validCoordinate(point.spawnPosition().getX())
                && validCoordinate(point.spawnPosition().getY())
                && validCoordinate(point.spawnPosition().getZ());
    }

    private static boolean validVehiclePoint(VehicleDeploymentPoint point) {
        return point != null && !internalDimension(point.dimension())
                && point.facing().getAxis().isHorizontal()
                && point.dimension().toString().length()
                <= BattleRules.MAX_DIMENSION_ID_LENGTH
                && validCoordinate(point.anchorPosition().getX())
                && validCoordinate(point.anchorPosition().getY())
                && validCoordinate(point.anchorPosition().getZ());
    }

    private static boolean validRallyPoint(RallyDeploymentPoint point) {
        return point != null && !internalDimension(point.dimension())
                && point.dimension().toString().length() <= BattleRules.MAX_DIMENSION_ID_LENGTH
                && point.formationId().length() <= 64
                && validCoordinate(point.anchorPosition().getX())
                && validCoordinate(point.anchorPosition().getY())
                && validCoordinate(point.anchorPosition().getZ())
                && validCoordinate(point.spawnPosition().getX())
                && validCoordinate(point.spawnPosition().getY())
                && validCoordinate(point.spawnPosition().getZ());
    }

    private static boolean sameAnchor(VehicleDeploymentPoint left,
                                      VehicleDeploymentPoint right) {
        return left.dimension().equals(right.dimension())
                && left.anchorPosition().equals(right.anchorPosition());
    }

    private static boolean internalDimension(ResourceLocation dimension) {
        return HOLDING_DIMENSION.equals(dimension) || LOBBY_DIMENSION.equals(dimension);
    }

    private record AnchorKey(ResourceLocation dimension, BlockPos position) {
        private AnchorKey {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(position, "position");
            position = position.immutable();
        }

        static AnchorKey of(FieldDeploymentPoint point) {
            return new AnchorKey(point.dimension(), point.anchorPosition());
        }
    }

    private record RallyCooldownKey(Faction faction, String formationId,
                                    SquadCallsign squad)
            implements Comparable<RallyCooldownKey> {
        private RallyCooldownKey {
            Objects.requireNonNull(faction, "faction");
            Objects.requireNonNull(formationId, "formationId");
            Objects.requireNonNull(squad, "squad");
        }

        static RallyCooldownKey of(Faction faction, String formationId,
                                   SquadCallsign squad) {
            String normalizedFormation = formationId == null ? "" : formationId.trim();
            if (faction == null || squad == null || normalizedFormation.isEmpty()
                    || normalizedFormation.length() > 64) {
                return null;
            }
            return new RallyCooldownKey(faction, normalizedFormation, squad);
        }

        @Override
        public int compareTo(RallyCooldownKey other) {
            int factionOrder = faction.id().compareTo(other.faction.id());
            if (factionOrder != 0) {
                return factionOrder;
            }
            int formationOrder = formationId.compareTo(other.formationId);
            if (formationOrder != 0) {
                return formationOrder;
            }
            return squad.id().compareTo(other.squad.id());
        }
    }
}
