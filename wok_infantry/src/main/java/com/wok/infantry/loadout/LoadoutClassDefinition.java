package com.wok.infantry.loadout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class LoadoutClassDefinition {
    private String id;
    private String displayName;
    private boolean enabled;
    private Map<String, List<LoadoutEntry>> slots;

    public LoadoutClassDefinition() {
        this("assault", "突击兵", true);
    }

    public LoadoutClassDefinition(String id, String displayName, boolean enabled) {
        this.id = Objects.requireNonNullElse(id, "assault");
        this.displayName = Objects.requireNonNullElse(displayName, this.id);
        this.enabled = enabled;
        this.slots = new LinkedHashMap<>();
        ensureSlots();
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public boolean enabled() { return enabled; }

    public List<LoadoutEntry> entries(LoadoutSlot slot) {
        ensureSlots();
        return slots.get(slot.id());
    }

    public void updateMetadata(String displayName, boolean enabled) {
        this.displayName = Objects.requireNonNullElse(displayName, id).trim();
        this.enabled = enabled;
    }

    public void ensureSlots() {
        if (slots == null) {
            slots = new LinkedHashMap<>();
        }
        for (LoadoutSlot slot : LoadoutSlot.values()) {
            slots.computeIfAbsent(slot.id(), ignored -> new ArrayList<>());
        }
    }

    public LoadoutClassDefinition copy() {
        LoadoutClassDefinition result = new LoadoutClassDefinition(id, displayName, enabled);
        for (LoadoutSlot slot : LoadoutSlot.values()) {
            result.entries(slot).clear();
            entries(slot).stream().map(LoadoutEntry::copy).forEach(result.entries(slot)::add);
        }
        return result;
    }
}
