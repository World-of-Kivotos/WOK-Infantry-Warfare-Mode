package com.wok.infantry.formation.vehicle;

import com.wok.infantry.battle.ActionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Overworld-owned durable allocation ledger for optional 卓越前线 vehicles.
 *
 * <p>This data is intentionally stricter than ordinary preference data. Dropping an unknown or
 * malformed record would make an unloaded vehicle look absent and permit a duplicate deployment,
 * so any structural ambiguity quarantines the complete ledger until an administrator explicitly
 * repairs it.</p>
 */
public final class VehicleAllocationSavedData extends SavedData {
    public static final String DATA_NAME = "wok_infantry_vehicle_allocations";
    public static final int DATA_VERSION = 1;
    static final int MAX_ENTRIES = 4096;

    private static final String VERSION_TAG = "Version";
    private static final String QUARANTINED_TAG = "Quarantined";
    private static final String QUARANTINE_REASON_TAG = "QuarantineReason";
    private static final String ALLOCATIONS_TAG = "Allocations";
    private static final String SESSION_TAG = "Session";
    private static final String FACTION_TAG = "Faction";
    private static final String FORMATION_TAG = "Formation";
    private static final String ALLOCATION_TAG = "Allocation";
    private static final String ENTITY_TAG = "Entity";
    private static final String ENTITY_TYPE_TAG = "EntityType";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String PENDING_RETIREMENT_TAG = "PendingRetirement";

    private static final int MAX_OWNERSHIP_ID_LENGTH = 256;
    private static final int MAX_ALLOCATION_ID_LENGTH = 96;
    private static final int MAX_RESOURCE_ID_LENGTH = 256;
    private static final int MAX_REASON_LENGTH = 512;
    private static final Pattern OWNERSHIP_ID = Pattern.compile("[a-z0-9_.:/-]+");
    private static final Pattern ALLOCATION_ID = Pattern.compile("[a-z0-9._/-]+");
    private static final Comparator<SuperbWarfareVehicleService.VehicleLedgerEntry> ENTRY_ORDER =
            Comparator.comparing((SuperbWarfareVehicleService.VehicleLedgerEntry entry) ->
                            entry.allocation().sessionId().toString())
                    .thenComparing(entry -> entry.allocation().factionId())
                    .thenComparing(entry -> entry.allocation().formationId())
                    .thenComparing(entry -> entry.allocation().allocationId())
                    .thenComparing(entry -> entry.entityId().toString());

    private final LinkedHashMap<VehicleAllocationKey,
            SuperbWarfareVehicleService.VehicleLedgerEntry> entries = new LinkedHashMap<>();
    private boolean quarantined;
    private String quarantineReason = "";

    public VehicleAllocationSavedData() {
    }

    public static VehicleAllocationSavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(
                VehicleAllocationSavedData::load, VehicleAllocationSavedData::new, DATA_NAME);
    }

    public static VehicleAllocationSavedData load(CompoundTag root) {
        VehicleAllocationSavedData data = new VehicleAllocationSavedData();
        if (root == null) {
            data.markQuarantined("SavedData root 缺失");
            return data;
        }
        try {
            if (!root.contains(VERSION_TAG, Tag.TAG_INT)) {
                data.markQuarantined("载具台账版本字段缺失或类型错误");
                return data;
            }
            int version = root.getInt(VERSION_TAG);
            if (version != DATA_VERSION) {
                data.markQuarantined("不支持的载具台账版本: " + version);
                return data;
            }
            if (!root.contains(QUARANTINED_TAG, Tag.TAG_BYTE)) {
                data.markQuarantined("载具台账 quarantine 字段缺失或类型错误");
                return data;
            }
            if (root.getBoolean(QUARANTINED_TAG)) {
                String reason = root.contains(QUARANTINE_REASON_TAG, Tag.TAG_STRING)
                        ? root.getString(QUARANTINE_REASON_TAG) : "已持久化的隔离状态";
                data.markQuarantined(reason);
                return data;
            }

            Tag rawAllocations = root.get(ALLOCATIONS_TAG);
            if (!(rawAllocations instanceof ListTag list)
                    || !list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND) {
                data.markQuarantined("载具台账记录列表缺失或类型错误");
                return data;
            }
            if (list.size() > MAX_ENTRIES) {
                data.markQuarantined("载具台账超过安全上限: " + list.size());
                return data;
            }

            LinkedHashMap<VehicleAllocationKey,
                    SuperbWarfareVehicleService.VehicleLedgerEntry> decoded =
                    new LinkedHashMap<>();
            Set<UUID> entityIds = new HashSet<>();
            for (int index = 0; index < list.size(); index++) {
                SuperbWarfareVehicleService.VehicleLedgerEntry entry =
                        decodeEntry(list.getCompound(index));
                if (entry == null) {
                    data.markQuarantined("载具台账第 " + index + " 条记录损坏");
                    return data;
                }
                if (decoded.putIfAbsent(entry.allocation(), entry) != null) {
                    data.markQuarantined("载具台账包含重复 allocation: "
                            + entry.allocation().allocationId());
                    return data;
                }
                if (!entityIds.add(entry.entityId())) {
                    data.markQuarantined("载具台账包含重复实体 UUID");
                    return data;
                }
            }
            data.entries.putAll(sorted(decoded.values()));
            // Rewrite a deterministic canonical ordering on the next normal world save.
            data.setDirty();
            return data;
        } catch (RuntimeException exception) {
            data.entries.clear();
            data.markQuarantined("载具台账解析异常: "
                    + exception.getClass().getSimpleName());
            return data;
        }
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putInt(VERSION_TAG, DATA_VERSION);
        root.putBoolean(QUARANTINED_TAG, quarantined);
        root.putString(QUARANTINE_REASON_TAG, quarantined ? quarantineReason : "");
        ListTag list = new ListTag();
        if (!quarantined) {
            for (SuperbWarfareVehicleService.VehicleLedgerEntry entry : entries.values()) {
                list.add(encodeEntry(entry));
            }
        }
        root.put(ALLOCATIONS_TAG, list);
        return root;
    }

    public ActionResult status() {
        if (quarantined) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 持久化台账已隔离: " + quarantineReason);
        }
        return ActionResult.ok("载具 allocation 持久化台账可用");
    }

    public boolean quarantined() {
        return quarantined;
    }

    public String quarantineReason() {
        return quarantineReason;
    }

    public List<SuperbWarfareVehicleService.VehicleLedgerEntry> entries() {
        return List.copyOf(entries.values());
    }

    /** Atomically replaces the durable view; rejection never weakens the prior ledger. */
    public ActionResult synchronize(
            List<SuperbWarfareVehicleService.VehicleLedgerEntry> snapshot) {
        ActionResult currentStatus = status();
        if (!currentStatus.success()) {
            return currentStatus;
        }
        if (snapshot == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 台账快照缺失");
        }
        if (snapshot.size() > MAX_ENTRIES) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具 allocation 台账超过安全上限");
        }

        LinkedHashMap<VehicleAllocationKey,
                SuperbWarfareVehicleService.VehicleLedgerEntry> replacement =
                new LinkedHashMap<>();
        Set<UUID> entityIds = new HashSet<>();
        for (SuperbWarfareVehicleService.VehicleLedgerEntry entry : snapshot) {
            if (!validEntry(entry)) {
                return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "载具 allocation 台账快照包含非法记录");
            }
            if (replacement.putIfAbsent(entry.allocation(), entry) != null) {
                return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "载具 allocation 台账快照包含重复 allocation");
            }
            if (!entityIds.add(entry.entityId())) {
                return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "载具 allocation 台账快照包含重复实体 UUID");
            }
        }

        LinkedHashMap<VehicleAllocationKey,
                SuperbWarfareVehicleService.VehicleLedgerEntry> canonical =
                sorted(replacement.values());
        if (entries.equals(canonical)) {
            return ActionResult.ok("载具 allocation 台账已是最新状态");
        }
        entries.clear();
        entries.putAll(canonical);
        setDirty();
        return ActionResult.ok("已同步载具 allocation 持久化台账");
    }

    /**
     * Permanently records a fail-closed state. There is deliberately no automatic clear path:
     * callers must not turn unreadable ownership state into an empty, deployable ledger.
     */
    public void markQuarantined(String reason) {
        quarantined = true;
        if (quarantineReason.isEmpty()) {
            quarantineReason = normalizeReason(reason);
        }
        entries.clear();
        setDirty();
    }

    private static CompoundTag encodeEntry(
            SuperbWarfareVehicleService.VehicleLedgerEntry entry) {
        CompoundTag tag = new CompoundTag();
        VehicleAllocationKey key = entry.allocation();
        tag.putUUID(SESSION_TAG, key.sessionId());
        tag.putString(FACTION_TAG, key.factionId());
        tag.putString(FORMATION_TAG, key.formationId());
        tag.putString(ALLOCATION_TAG, key.allocationId());
        tag.putUUID(ENTITY_TAG, entry.entityId());
        tag.putString(ENTITY_TYPE_TAG, entry.entityTypeId().toString());
        tag.putString(DIMENSION_TAG, entry.dimension().toString());
        tag.putBoolean(PENDING_RETIREMENT_TAG, entry.pendingRetirement());
        return tag;
    }

    private static SuperbWarfareVehicleService.VehicleLedgerEntry decodeEntry(CompoundTag tag) {
        if (tag == null || !tag.hasUUID(SESSION_TAG) || !tag.hasUUID(ENTITY_TAG)
                || !tag.contains(FACTION_TAG, Tag.TAG_STRING)
                || !tag.contains(FORMATION_TAG, Tag.TAG_STRING)
                || !tag.contains(ALLOCATION_TAG, Tag.TAG_STRING)
                || !tag.contains(ENTITY_TYPE_TAG, Tag.TAG_STRING)
                || !tag.contains(DIMENSION_TAG, Tag.TAG_STRING)
                || !tag.contains(PENDING_RETIREMENT_TAG, Tag.TAG_BYTE)) {
            return null;
        }
        ResourceLocation entityTypeId = parseResourceId(tag.getString(ENTITY_TYPE_TAG));
        ResourceLocation dimension = parseResourceId(tag.getString(DIMENSION_TAG));
        if (entityTypeId == null || dimension == null) {
            return null;
        }
        try {
            VehicleAllocationKey allocation = new VehicleAllocationKey(
                    tag.getUUID(SESSION_TAG), tag.getString(FACTION_TAG),
                    tag.getString(FORMATION_TAG), tag.getString(ALLOCATION_TAG));
            SuperbWarfareVehicleService.VehicleLedgerEntry entry =
                    new SuperbWarfareVehicleService.VehicleLedgerEntry(allocation,
                            tag.getUUID(ENTITY_TAG), entityTypeId, dimension,
                            tag.getBoolean(PENDING_RETIREMENT_TAG));
            return validEntry(entry) ? entry : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean validEntry(
            SuperbWarfareVehicleService.VehicleLedgerEntry entry) {
        if (entry == null || entry.allocation() == null || entry.entityId() == null
                || entry.entityTypeId() == null || entry.dimension() == null) {
            return false;
        }
        VehicleAllocationKey key = entry.allocation();
        return key.sessionId() != null
                && validOwnershipId(key.factionId())
                && validOwnershipId(key.formationId())
                && validAllocationId(key.allocationId())
                && SuperbWarfareVehicleGate.supportsEntity(entry.entityTypeId())
                && entry.entityTypeId().toString().length() <= MAX_RESOURCE_ID_LENGTH
                && entry.dimension().toString().length() <= MAX_RESOURCE_ID_LENGTH;
    }

    private static ResourceLocation parseResourceId(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_RESOURCE_ID_LENGTH) {
            return null;
        }
        return ResourceLocation.tryParse(value);
    }

    private static boolean validOwnershipId(String value) {
        return value != null && !value.isBlank() && value.equals(value.trim())
                && value.length() <= MAX_OWNERSHIP_ID_LENGTH
                && OWNERSHIP_ID.matcher(value).matches();
    }

    private static boolean validAllocationId(String value) {
        return value != null && !value.isBlank() && value.equals(value.trim())
                && value.length() <= MAX_ALLOCATION_ID_LENGTH
                && ALLOCATION_ID.matcher(value).matches();
    }

    private static LinkedHashMap<VehicleAllocationKey,
            SuperbWarfareVehicleService.VehicleLedgerEntry> sorted(
            Iterable<SuperbWarfareVehicleService.VehicleLedgerEntry> source) {
        List<SuperbWarfareVehicleService.VehicleLedgerEntry> ordered = new ArrayList<>();
        source.forEach(ordered::add);
        ordered.sort(ENTRY_ORDER);
        LinkedHashMap<VehicleAllocationKey,
                SuperbWarfareVehicleService.VehicleLedgerEntry> result =
                new LinkedHashMap<>();
        for (SuperbWarfareVehicleService.VehicleLedgerEntry entry : ordered) {
            result.put(entry.allocation(), entry);
        }
        return result;
    }

    private static String normalizeReason(String reason) {
        String normalized = Objects.requireNonNullElse(reason, "").trim();
        if (normalized.isEmpty()) {
            normalized = "未知持久化错误";
        }
        return normalized.length() <= MAX_REASON_LENGTH
                ? normalized : normalized.substring(0, MAX_REASON_LENGTH);
    }
}
