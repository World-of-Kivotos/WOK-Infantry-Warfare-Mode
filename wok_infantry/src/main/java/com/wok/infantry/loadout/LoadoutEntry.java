package com.wok.infantry.loadout;

import java.util.Objects;

public final class LoadoutEntry {
    public static final int MIN_AMMO_RESERVE_LIMIT = 1;
    public static final int MAX_AMMO_RESERVE_LIMIT = 4_096;
    public static final int DEFAULT_AMMO_RESERVE_LIMIT = 180;

    private String id;
    private String displayName;
    private String itemId;
    private int count;
    private String snbt;
    /** Maximum loose rounds supplied while this specific gun entry is equipped. */
    private int ammoReserveLimit = DEFAULT_AMMO_RESERVE_LIMIT;

    public LoadoutEntry() {
        this("entry", "新装备", "minecraft:air", 1, "");
    }

    public LoadoutEntry(String id, String displayName, String itemId, int count, String snbt) {
        this(id, displayName, itemId, count, snbt, DEFAULT_AMMO_RESERVE_LIMIT);
    }

    public LoadoutEntry(String id, String displayName, String itemId, int count, String snbt,
                        int ammoReserveLimit) {
        this.id = Objects.requireNonNullElse(id, "entry");
        this.displayName = Objects.requireNonNullElse(displayName, this.id);
        this.itemId = Objects.requireNonNullElse(itemId, "minecraft:air");
        this.count = count;
        this.snbt = Objects.requireNonNullElse(snbt, "");
        this.ammoReserveLimit = ammoReserveLimit;
    }

    public String id() { return Objects.requireNonNullElse(id, ""); }
    public String displayName() { return Objects.requireNonNullElse(displayName, id()); }
    public String itemId() { return Objects.requireNonNullElse(itemId, "minecraft:air"); }
    public int count() { return count; }
    public String snbt() { return Objects.requireNonNullElse(snbt, ""); }
    public int ammoReserveLimit() {
        if (ammoReserveLimit < MIN_AMMO_RESERVE_LIMIT) {
            return DEFAULT_AMMO_RESERVE_LIMIT;
        }
        return Math.min(MAX_AMMO_RESERVE_LIMIT, ammoReserveLimit);
    }
    public int configuredAmmoReserveLimit() {
        return ammoReserveLimit;
    }
    public boolean hasValidAmmoReserveLimit() {
        return ammoReserveLimit >= MIN_AMMO_RESERVE_LIMIT
                && ammoReserveLimit <= MAX_AMMO_RESERVE_LIMIT;
    }
    void normalizeAmmoReserveLimit() {
        ammoReserveLimit = ammoReserveLimit < MIN_AMMO_RESERVE_LIMIT
                ? DEFAULT_AMMO_RESERVE_LIMIT
                : Math.min(MAX_AMMO_RESERVE_LIMIT, ammoReserveLimit);
    }

    public LoadoutEntry copy() {
        return new LoadoutEntry(id(), displayName(), itemId(), count, snbt(),
                ammoReserveLimit());
    }
}
