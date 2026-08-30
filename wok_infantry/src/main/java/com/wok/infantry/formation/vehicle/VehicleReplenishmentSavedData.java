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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Durable combat-loss and replenishment cooldown ledger for formation vehicle slots. */
public final class VehicleReplenishmentSavedData extends SavedData {
    public static final String DATA_NAME = "wok_infantry_vehicle_replenishment";
    public static final long NEVER_REPLENISH = -1L;
    public static final int MAX_ENTRIES = 4096;
    private static final int DATA_VERSION = 1;
    private static final Pattern OWNERSHIP_ID = Pattern.compile("[a-z0-9_.:/-]+");
    private static final Pattern ALLOCATION_ID = Pattern.compile("[a-z0-9._/-]+");
    private static final Comparator<Entry> ENTRY_ORDER = Comparator
            .comparing((Entry entry) -> entry.key().sessionId().toString())
            .thenComparing(entry -> entry.key().factionId())
            .thenComparing(entry -> entry.key().formationId())
            .thenComparing(entry -> entry.key().allocationId());

    private final LinkedHashMap<VehicleAllocationKey, Entry> entries = new LinkedHashMap<>();
    private boolean quarantined;
    private String quarantineReason = "";

    public static VehicleReplenishmentSavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(
                VehicleReplenishmentSavedData::load,
                VehicleReplenishmentSavedData::new, DATA_NAME);
    }

    public static VehicleReplenishmentSavedData load(CompoundTag root) {
        VehicleReplenishmentSavedData data = new VehicleReplenishmentSavedData();
        if (root == null || !root.contains("Version", Tag.TAG_INT)
                || root.getInt("Version") != DATA_VERSION
                || !root.contains("Quarantined", Tag.TAG_BYTE)) {
            data.quarantine("invalid or unsupported replenishment ledger header");
            return data;
        }
        if (root.getBoolean("Quarantined")) {
            data.quarantine(root.contains("QuarantineReason", Tag.TAG_STRING)
                    ? root.getString("QuarantineReason") : "persisted quarantine");
            return data;
        }
        Tag raw = root.get("Entries");
        if (!(raw instanceof ListTag list)
                || !list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND
                || list.size() > MAX_ENTRIES) {
            data.quarantine("invalid replenishment entry list");
            return data;
        }
        for (int index = 0; index < list.size(); index++) {
            Entry entry = decode(list.getCompound(index));
            if (entry == null || data.entries.putIfAbsent(entry.key(), entry) != null) {
                data.entries.clear();
                data.quarantine("invalid or duplicate replenishment entry at " + index);
                return data;
            }
        }
        data.canonicalize();
        data.setDirty();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putInt("Version", DATA_VERSION);
        root.putBoolean("Quarantined", quarantined);
        root.putString("QuarantineReason", quarantined ? quarantineReason : "");
        ListTag list = new ListTag();
        if (!quarantined) {
            entries.values().stream().sorted(ENTRY_ORDER)
                    .map(VehicleReplenishmentSavedData::encode).forEach(list::add);
        }
        root.put("Entries", list);
        return root;
    }

    public ActionResult status() {
        return quarantined
                ? ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                "载具补充台账已隔离：" + quarantineReason)
                : ActionResult.ok("载具补充台账可用");
    }

    public boolean contains(VehicleAllocationKey key) {
        return key != null && entries.containsKey(key);
    }

    public Entry entry(VehicleAllocationKey key) {
        return key == null ? null : entries.get(key);
    }

    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    /** Records a combat loss without ever shortening an already consumed cooldown. */
    public ActionResult recordLoss(VehicleAllocationKey key, ResourceLocation entityTypeId,
                                   long readyAtTick) {
        ActionResult available = status();
        if (!available.success()) {
            return available;
        }
        Entry candidate = new Entry(key, entityTypeId, readyAtTick);
        if (!valid(candidate)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具损失记录不合法");
        }
        Entry current = entries.get(key);
        if (current != null && !current.entityTypeId().equals(entityTypeId)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具损失记录与已有实体类型冲突");
        }
        if (current == null && entries.size() >= MAX_ENTRIES) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具补充台账已达安全上限");
        }
        long merged = current == null ? readyAtTick
                : mergeReadyAt(current.readyAtTick(), readyAtTick);
        entries.put(key, new Entry(key, entityTypeId, merged));
        canonicalize();
        setDirty();
        return ActionResult.ok(merged == NEVER_REPLENISH
                ? "已记录不可再生载具损失"
                : "已记录载具补充冷却");
    }

    public void remove(VehicleAllocationKey key) {
        if (key != null && entries.remove(key) != null) {
            setDirty();
        }
    }

    public void clearSession(UUID sessionId) {
        if (sessionId != null && entries.entrySet().removeIf(entry ->
                sessionId.equals(entry.getKey().sessionId()))) {
            setDirty();
        }
    }

    /** Drops stale sessions, deleted slots, and type-changed slots after a catalog reload. */
    public ActionResult reconcile(UUID activeSession,
                                  Map<VehicleAllocationKey, ResourceLocation> expected) {
        ActionResult available = status();
        if (!available.success()) {
            return available;
        }
        if (activeSession == null || expected == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具补充调和参数缺失");
        }
        boolean changed = entries.entrySet().removeIf(entry -> {
            ResourceLocation wanted = expected.get(entry.getKey());
            return !activeSession.equals(entry.getKey().sessionId())
                    || wanted == null || !wanted.equals(entry.getValue().entityTypeId());
        });
        if (changed) {
            canonicalize();
            setDirty();
        }
        return ActionResult.ok("载具补充台账已与当前编制调和");
    }

    public List<Entry> ready(long nowTick, int limit) {
        if (quarantined || limit <= 0) {
            return List.of();
        }
        return entries.values().stream()
                .filter(entry -> entry.readyAtTick() >= 0L && entry.readyAtTick() <= nowTick)
                .sorted(ENTRY_ORDER).limit(limit).toList();
    }

    private static long mergeReadyAt(long left, long right) {
        return left == NEVER_REPLENISH || right == NEVER_REPLENISH
                ? NEVER_REPLENISH : Math.max(left, right);
    }

    private void canonicalize() {
        List<Entry> sorted = new ArrayList<>(entries.values());
        sorted.sort(ENTRY_ORDER);
        entries.clear();
        sorted.forEach(entry -> entries.put(entry.key(), entry));
    }

    private void quarantine(String reason) {
        quarantined = true;
        quarantineReason = Objects.requireNonNullElse(reason, "unknown error");
        if (quarantineReason.length() > 512) {
            quarantineReason = quarantineReason.substring(0, 512);
        }
        entries.clear();
        setDirty();
    }

    private static CompoundTag encode(Entry entry) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Session", entry.key().sessionId());
        tag.putString("Faction", entry.key().factionId());
        tag.putString("Formation", entry.key().formationId());
        tag.putString("Allocation", entry.key().allocationId());
        tag.putString("EntityType", entry.entityTypeId().toString());
        tag.putLong("ReadyAt", entry.readyAtTick());
        return tag;
    }

    private static Entry decode(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("Session")
                || !tag.contains("Faction", Tag.TAG_STRING)
                || !tag.contains("Formation", Tag.TAG_STRING)
                || !tag.contains("Allocation", Tag.TAG_STRING)
                || !tag.contains("EntityType", Tag.TAG_STRING)
                || !tag.contains("ReadyAt", Tag.TAG_LONG)) {
            return null;
        }
        ResourceLocation entityType = ResourceLocation.tryParse(tag.getString("EntityType"));
        try {
            Entry entry = new Entry(new VehicleAllocationKey(tag.getUUID("Session"),
                    tag.getString("Faction"), tag.getString("Formation"),
                    tag.getString("Allocation")), entityType, tag.getLong("ReadyAt"));
            return valid(entry) ? entry : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean valid(Entry entry) {
        if (entry == null || entry.key() == null
                || !SuperbWarfareVehicleGate.supportsEntity(entry.entityTypeId())
                || entry.readyAtTick() < NEVER_REPLENISH) {
            return false;
        }
        VehicleAllocationKey key = entry.key();
        return key.sessionId() != null
                && validId(key.factionId(), OWNERSHIP_ID, 256)
                && validId(key.formationId(), OWNERSHIP_ID, 256)
                && validId(key.allocationId(), ALLOCATION_ID, 96);
    }

    private static boolean validId(String value, Pattern pattern, int maxLength) {
        return value != null && !value.isBlank() && value.equals(value.trim())
                && value.length() <= maxLength && pattern.matcher(value).matches();
    }

    public record Entry(VehicleAllocationKey key, ResourceLocation entityTypeId,
                        long readyAtTick) {
    }
}
