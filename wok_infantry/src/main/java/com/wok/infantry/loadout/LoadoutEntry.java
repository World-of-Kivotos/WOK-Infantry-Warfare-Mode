package com.wok.infantry.loadout;

import java.util.Objects;

public final class LoadoutEntry {
    private String id;
    private String displayName;
    private String itemId;
    private int count;
    private String snbt;

    public LoadoutEntry() {
        this("entry", "新装备", "minecraft:air", 1, "");
    }

    public LoadoutEntry(String id, String displayName, String itemId, int count, String snbt) {
        this.id = Objects.requireNonNullElse(id, "entry");
        this.displayName = Objects.requireNonNullElse(displayName, this.id);
        this.itemId = Objects.requireNonNullElse(itemId, "minecraft:air");
        this.count = count;
        this.snbt = Objects.requireNonNullElse(snbt, "");
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String itemId() { return itemId; }
    public int count() { return count; }
    public String snbt() { return snbt; }

    public LoadoutEntry copy() {
        return new LoadoutEntry(id, displayName, itemId, count, snbt);
    }
}
