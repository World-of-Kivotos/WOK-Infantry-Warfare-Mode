package com.wok.infantry.loadout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class LoadoutClassDefinition {
    public static final int MAX_SLOTS = LoadoutInventoryTarget.values().length;

    private String id;
    private String displayName;
    private boolean enabled;
    private int squadLimit;
    /** Entry storage remains keyed by id so old version-2 JSON keeps every SNBT payload verbatim. */
    private Map<String, List<LoadoutEntry>> slots;
    /** Version-3 metadata makes the formerly fixed slot map ordered and administrator-editable. */
    private List<LoadoutSlotDefinition> slotDefinitions;

    public LoadoutClassDefinition() {
        this("assault", "突击兵", true, 8, true);
        // Gson calls this constructor before assigning fields. Keeping zero here lets
        // normalize() detect a version-1 config where squadLimit did not exist yet.
        this.squadLimit = 0;
    }

    public LoadoutClassDefinition(String id, String displayName, boolean enabled) {
        this(id, displayName, enabled, defaultSquadLimit(id), true);
    }

    public LoadoutClassDefinition(String id, String displayName, boolean enabled, int squadLimit) {
        this(id, displayName, enabled, squadLimit, true);
    }

    public LoadoutClassDefinition(String id, String displayName, boolean enabled, int squadLimit,
                                  boolean includeLegacySlots) {
        this.id = Objects.requireNonNullElse(id, "assault");
        this.displayName = Objects.requireNonNullElse(displayName, this.id);
        this.enabled = enabled;
        this.squadLimit = clampSquadLimit(squadLimit, this.id);
        this.slots = new LinkedHashMap<>();
        this.slotDefinitions = new ArrayList<>();
        if (includeLegacySlots) {
            for (LoadoutSlot slot : LoadoutSlot.values()) {
                slotDefinitions.add(LoadoutSlotDefinition.legacy(slot));
            }
        }
        ensureSlots();
    }

    public String id() { return id; }
    public String displayName() { return Objects.requireNonNullElse(displayName, id); }
    public boolean enabled() { return "assault".equals(id) || enabled; }
    public int squadLimit() {
        if ("assault".equals(id)) {
            squadLimit = 8;
            return squadLimit;
        }
        squadLimit = clampSquadLimit(squadLimit, id);
        return squadLimit;
    }

    public List<LoadoutSlotDefinition> slotDefinitions() {
        ensureSlots();
        return slotDefinitions;
    }

    public Optional<LoadoutSlotDefinition> findSlot(String slotId) {
        if (slotId == null) {
            return Optional.empty();
        }
        return slotDefinitions().stream().filter(slot -> slot.id().equals(slotId)).findFirst();
    }

    public List<LoadoutEntry> entries(String slotId) {
        ensureSlots();
        List<LoadoutEntry> entries = slots.get(Objects.requireNonNullElse(slotId, ""));
        return entries == null ? List.of() : entries;
    }

    /** Compatibility accessor for tests and legacy defaults; runtime code uses string ids. */
    public List<LoadoutEntry> entries(LoadoutSlot slot) {
        return entries(slot.id());
    }

    public void updateMetadata(String displayName, boolean enabled, int squadLimit) {
        this.displayName = Objects.requireNonNullElse(displayName, id).trim();
        this.enabled = "assault".equals(id) || enabled;
        this.squadLimit = "assault".equals(id) ? 8 : clampSquadLimit(squadLimit, id);
    }

    /** Version-2 required every fixed slot, even when an administrator left it empty. */
    public void migrateLegacyRequirements() {
        ensureSlots();
        for (LoadoutSlotDefinition slot : slotDefinitions) {
            boolean hasRealEntry = entries(slot.id()).stream()
                    .anyMatch(entry -> entry != null
                            && !"minecraft:air".equals(entry.itemId()));
            slot.update(slot.displayName(), slot.target(), hasRealEntry);
        }
    }

    /** Migrates old fixed maps and preserves unknown hand-edited keys while a free target exists. */
    public void ensureSlots() {
        if (slots == null) {
            slots = new LinkedHashMap<>();
        }
        if (slotDefinitions == null) {
            slotDefinitions = new ArrayList<>();
            for (LoadoutSlot slot : LoadoutSlot.values()) {
                slotDefinitions.add(LoadoutSlotDefinition.legacy(slot));
            }
        }
        squadLimit = clampSquadLimit(squadLimit, id);
        if ("assault".equals(id)) {
            enabled = true;
            squadLimit = 8;
        }

        Set<String> ids = new HashSet<>();
        Set<LoadoutInventoryTarget> targets = new HashSet<>();
        List<LoadoutSlotDefinition> normalizedSlots = new ArrayList<>();
        for (LoadoutSlotDefinition slot : slotDefinitions) {
            if (normalizedSlots.size() >= MAX_SLOTS || slot == null
                    || !validSlotId(slot.id()) || ids.contains(slot.id())
                    || targets.contains(slot.target())) {
                continue;
            }
            normalizedSlots.add(slot);
            ids.add(slot.id());
            targets.add(slot.target());
        }
        slotDefinitions = normalizedSlots;

        for (String slotId : new ArrayList<>(slots.keySet())) {
            if (slotDefinitions.size() >= MAX_SLOTS || !validSlotId(slotId) || ids.contains(slotId)) {
                continue;
            }
            LoadoutInventoryTarget free = firstFreeTarget(targets);
            if (free == null) {
                break;
            }
            slotDefinitions.add(new LoadoutSlotDefinition(slotId, slotId, free, true));
            ids.add(slotId);
            targets.add(free);
        }
        slotDefinitions.forEach(slot -> slots.computeIfAbsent(slot.id(), ignored -> new ArrayList<>()));
        slots.keySet().removeIf(slotId -> !ids.contains(slotId));
    }

    public boolean addSlot(LoadoutSlotDefinition slot) {
        ensureSlots();
        if (slot == null || slotDefinitions.size() >= MAX_SLOTS
                || !validSlotId(slot.id()) || findSlot(slot.id()).isPresent()
                || targetInUse(slot.target(), "")) {
            return false;
        }
        slotDefinitions.add(slot.copy());
        slots.put(slot.id(), new ArrayList<>());
        return true;
    }

    public boolean updateSlot(String slotId, String displayName,
                              LoadoutInventoryTarget target, boolean required) {
        LoadoutSlotDefinition slot = findSlot(slotId).orElse(null);
        if (slot == null || target == null || targetInUse(target, slotId)) {
            return false;
        }
        slot.update(displayName, target, required);
        return true;
    }

    public boolean moveSlot(String slotId, int direction) {
        ensureSlots();
        int index = -1;
        for (int candidate = 0; candidate < slotDefinitions.size(); candidate++) {
            if (slotDefinitions.get(candidate).id().equals(slotId)) {
                index = candidate;
                break;
            }
        }
        int destination = index + Integer.signum(direction);
        if (index < 0 || destination < 0 || destination >= slotDefinitions.size()) {
            return false;
        }
        LoadoutSlotDefinition moved = slotDefinitions.remove(index);
        slotDefinitions.add(destination, moved);
        return true;
    }

    public boolean deleteSlot(String slotId) {
        ensureSlots();
        boolean removed = slotDefinitions.removeIf(slot -> slot.id().equals(slotId));
        if (removed) {
            slots.remove(slotId);
        }
        return removed;
    }

    private boolean targetInUse(LoadoutInventoryTarget target, String ignoredSlotId) {
        return slotDefinitions().stream().anyMatch(slot -> !slot.id().equals(ignoredSlotId)
                && slot.target() == target);
    }

    public LoadoutClassDefinition copy() {
        LoadoutClassDefinition result = new LoadoutClassDefinition(id, displayName(), enabled(),
                squadLimit(), false);
        result.slotDefinitions.clear();
        result.slots.clear();
        for (LoadoutSlotDefinition slot : slotDefinitions()) {
            result.slotDefinitions.add(slot.copy());
            List<LoadoutEntry> copies = new ArrayList<>();
            entries(slot.id()).stream().map(LoadoutEntry::copy).forEach(copies::add);
            result.slots.put(slot.id(), copies);
        }
        return result;
    }

    public static boolean validSlotId(String slotId) {
        if (slotId == null || slotId.isBlank() || slotId.length() > 64) {
            return false;
        }
        for (int index = 0; index < slotId.length(); index++) {
            char value = slotId.charAt(index);
            if (!(value >= 'a' && value <= 'z') && !(value >= '0' && value <= '9')
                    && value != '_' && value != '-' && value != '.') {
                return false;
            }
        }
        return true;
    }

    private static LoadoutInventoryTarget firstFreeTarget(Set<LoadoutInventoryTarget> used) {
        for (LoadoutInventoryTarget target : LoadoutInventoryTarget.values()) {
            if (!used.contains(target)) {
                return target;
            }
        }
        return null;
    }

    private static int clampSquadLimit(int value, String classId) {
        if (value <= 0) {
            return defaultSquadLimit(classId);
        }
        return Math.min(8, value);
    }

    public static int defaultSquadLimit(String classId) {
        return switch (Objects.requireNonNullElse(classId, "assault")) {
            case "support", "engineer" -> 2;
            case "recon" -> 1;
            default -> 8;
        };
    }
}
