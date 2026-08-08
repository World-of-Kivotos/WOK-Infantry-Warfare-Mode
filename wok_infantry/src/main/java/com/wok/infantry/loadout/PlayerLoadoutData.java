package com.wok.infantry.loadout;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PlayerLoadoutData {
    private String activeClassId = "assault";
    private Map<String, Map<String, String>> selections = new LinkedHashMap<>();

    public String activeClassId() {
        return Objects.requireNonNullElse(activeClassId, "assault");
    }

    public void setActiveClassId(String activeClassId) {
        this.activeClassId = Objects.requireNonNullElse(activeClassId, "assault");
    }

    public Map<String, String> selectionsFor(String classId) {
        if (selections == null) {
            selections = new LinkedHashMap<>();
        }
        return selections.computeIfAbsent(classId, ignored -> new LinkedHashMap<>());
    }

    public String selectedEntry(String classId, LoadoutSlot slot) {
        return selectionsFor(classId).getOrDefault(slot.id(), "");
    }

    public void select(String classId, LoadoutSlot slot, String entryId) {
        selectionsFor(classId).put(slot.id(), entryId);
    }

    public PlayerLoadoutData copy() {
        PlayerLoadoutData result = new PlayerLoadoutData();
        result.activeClassId = activeClassId();
        result.selections.clear();
        if (selections != null) {
            selections.forEach((classId, values) -> {
                if (classId != null && values != null) {
                    result.selections.put(classId, new LinkedHashMap<>(values));
                }
            });
        }
        return result;
    }
}
