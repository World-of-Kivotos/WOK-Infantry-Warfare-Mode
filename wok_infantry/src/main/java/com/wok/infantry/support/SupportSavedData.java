package com.wok.infantry.support;

import com.wok.infantry.battle.Faction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Overworld-owned faction cooldowns keyed by durable namespaced support ids.
 *
 * <p>Unknown but syntactically valid ids are intentionally retained. Temporarily removing an
 * optional provider therefore cannot reset its cooldown before the provider is reinstalled.
 * Active missions remain runtime-only to avoid replaying effects after a crash.</p>
 */
public final class SupportSavedData extends SavedData {
    public static final String DATA_NAME = "wok_infantry_support";
    public static final int DATA_VERSION = 2;
    public static final int MAX_COOLDOWN_ENTRIES = 4_096;

    private final EnumMap<Faction, LinkedHashMap<ResourceLocation, Long>> readyTicks =
            new EnumMap<>(Faction.class);
    private final EnumMap<Faction, Long> structuralRevisions =
            new EnumMap<>(Faction.class);

    public SupportSavedData() {
        for (Faction faction : Faction.values()) {
            readyTicks.put(faction, new LinkedHashMap<>());
            structuralRevisions.put(faction, 0L);
        }
    }

    public static SupportSavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(
                SupportSavedData::load, SupportSavedData::new, DATA_NAME);
    }

    public static SupportSavedData load(CompoundTag root) {
        SupportSavedData data = new SupportSavedData();
        int version = root.contains("Version", Tag.TAG_ANY_NUMERIC)
                ? root.getInt("Version") : 0;
        if (version != DATA_VERSION) {
            // Version 1 contained enum-only, non-namespaced ids for concrete prototype skills.
            // They have no unambiguous identity in the generic registry and are not migrated.
            data.setDirty();
            return data;
        }
        long legacyRevision = root.contains("StructuralRevision", Tag.TAG_ANY_NUMERIC)
                ? root.getLong("StructuralRevision") : 0L;
        CompoundTag factionRevisions = root.contains("FactionRevisions", Tag.TAG_COMPOUND)
                ? root.getCompound("FactionRevisions") : null;
        for (Faction faction : Faction.values()) {
            long revision = factionRevisions != null
                    && factionRevisions.contains(faction.id(), Tag.TAG_ANY_NUMERIC)
                    ? factionRevisions.getLong(faction.id()) : legacyRevision;
            data.structuralRevisions.put(faction, Math.max(0L, revision));
        }

        ListTag cooldowns = root.getList("Cooldowns", Tag.TAG_COMPOUND);
        int limit = Math.min(cooldowns.size(), MAX_COOLDOWN_ENTRIES);
        for (int index = 0; index < limit; index++) {
            CompoundTag tag = cooldowns.getCompound(index);
            Faction faction = Faction.byId(tag.getString("Faction")).orElse(null);
            ResourceLocation supportId = ResourceLocation.tryParse(tag.getString("SupportId"));
            long readyAt = tag.contains("ReadyAt", Tag.TAG_ANY_NUMERIC)
                    ? tag.getLong("ReadyAt") : -1L;
            if (faction == null || supportId == null || readyAt <= 0L) {
                continue;
            }
            // Duplicate/corrupt entries may never shorten a consumed cooldown.
            data.readyTicks.get(faction).merge(supportId, readyAt, Math::max);
        }
        // Rewrites duplicates and malformed entries canonically on the next save.
        data.setDirty();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putInt("Version", DATA_VERSION);
        CompoundTag factionRevisions = new CompoundTag();
        long legacyRevision = 0L;
        for (Faction faction : Faction.values()) {
            long revision = structuralRevisions.get(faction);
            factionRevisions.putLong(faction.id(), revision);
            legacyRevision = Math.max(legacyRevision, revision);
        }
        root.put("FactionRevisions", factionRevisions);
        // Retain the aggregate field solely so an earlier v2 build can still read this save.
        root.putLong("StructuralRevision", legacyRevision);
        ListTag cooldowns = new ListTag();
        for (Faction faction : Faction.values()) {
            readyTicks.get(faction).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(
                            Comparator.comparing(ResourceLocation::toString)))
                    .forEach(entry -> {
                        if (entry.getValue() <= 0L) {
                            return;
                        }
                        CompoundTag tag = new CompoundTag();
                        tag.putString("Faction", faction.id());
                        tag.putString("SupportId", entry.getKey().toString());
                        tag.putLong("ReadyAt", entry.getValue());
                        cooldowns.add(tag);
                    });
        }
        root.put("Cooldowns", cooldowns);
        return root;
    }

    public long readyAt(Faction faction, ResourceLocation supportId) {
        if (faction == null || supportId == null) {
            return 0L;
        }
        return readyTicks.get(faction).getOrDefault(supportId, 0L);
    }

    public Map<ResourceLocation, Long> cooldowns(Faction faction) {
        if (faction == null) {
            return Map.of();
        }
        return Map.copyOf(readyTicks.get(faction));
    }

    public boolean setReadyAt(Faction faction, ResourceLocation supportId,
                              long readyAtGameTick) {
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(supportId, "supportId");
        LinkedHashMap<ResourceLocation, Long> bySupport = readyTicks.get(faction);
        if (readyAtGameTick <= 0L) {
            if (bySupport.remove(supportId) == null) {
                return false;
            }
            touch(faction);
            return true;
        }
        long previous = bySupport.getOrDefault(supportId, 0L);
        if (previous == readyAtGameTick) {
            return false;
        }
        if (!bySupport.containsKey(supportId)
                && totalCooldownEntries() >= MAX_COOLDOWN_ENTRIES) {
            throw new IllegalStateException("Support cooldown entry limit exceeded");
        }
        bySupport.put(supportId, readyAtGameTick);
        touch(faction);
        return true;
    }

    /** Removes cooldowns that can no longer affect a request and returns the number removed. */
    public int pruneExpired(long currentGameTick) {
        long now = Math.max(0L, currentGameTick);
        int removed = 0;
        for (Map.Entry<Faction, LinkedHashMap<ResourceLocation, Long>> factionEntry
                : readyTicks.entrySet()) {
            Map<ResourceLocation, Long> bySupport = factionEntry.getValue();
            int before = bySupport.size();
            bySupport.entrySet().removeIf(entry -> entry.getValue() <= now);
            int factionRemoved = before - bySupport.size();
            if (factionRemoved > 0) {
                removed += factionRemoved;
                touch(factionEntry.getKey());
            }
        }
        return removed;
    }

    public void resetAll() {
        for (Map.Entry<Faction, LinkedHashMap<ResourceLocation, Long>> factionEntry
                : readyTicks.entrySet()) {
            if (!factionEntry.getValue().isEmpty()) {
                factionEntry.getValue().clear();
                touch(factionEntry.getKey());
            }
        }
    }

    /** Persistence mutation revision scoped to one faction; never exposes enemy timing. */
    public long structuralRevision(Faction faction) {
        return structuralRevisions.get(Objects.requireNonNull(faction, "faction"));
    }

    private int totalCooldownEntries() {
        return readyTicks.values().stream().mapToInt(Map::size).sum();
    }

    private void touch(Faction faction) {
        long current = structuralRevisions.get(faction);
        if (current < Long.MAX_VALUE) {
            structuralRevisions.put(faction, current + 1L);
        }
        setDirty();
    }

    /** Pure value key useful to deterministic tests and administrative diagnostics. */
    public record CooldownKey(Faction faction, ResourceLocation supportId) {
        public CooldownKey {
            Objects.requireNonNull(faction, "faction");
            Objects.requireNonNull(supportId, "supportId");
        }
    }
}
