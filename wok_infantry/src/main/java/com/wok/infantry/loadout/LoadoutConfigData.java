package com.wok.infantry.loadout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class LoadoutConfigData {
    public static final int MAX_CLASSES = 64;
    private static final int MAX_ENTRIES_PER_SLOT = 64;
    private int version = 3;
    private List<LoadoutClassDefinition> classes = new ArrayList<>();

    public int version() { return version; }

    public List<LoadoutClassDefinition> classes() {
        if (classes == null) {
            classes = new ArrayList<>();
        }
        return classes;
    }

    public Optional<LoadoutClassDefinition> findClass(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return classes().stream().filter(definition -> id.equals(definition.id())).findFirst();
    }

    public void normalize() {
        boolean migratingFixedSlots = version < 3;
        version = Math.max(version, 3);
        Set<String> classIds = new HashSet<>();
        classes().removeIf(definition -> definition == null
                || !validId(definition.id()) || !classIds.add(definition.id()));
        if (classes().size() > MAX_CLASSES) {
            classes().subList(MAX_CLASSES, classes().size()).clear();
        }
        classes().forEach(definition -> {
            definition.ensureSlots();
            if (migratingFixedSlots) {
                definition.migrateLegacyRequirements();
            }
            for (LoadoutSlotDefinition slot : definition.slotDefinitions()) {
                Set<String> entryIds = new HashSet<>();
                List<LoadoutEntry> entries = definition.entries(slot.id());
                entries.removeIf(entry -> entry == null || !validEntryId(entry.id())
                        || !entryIds.add(entry.id()));
                if (entries.size() > MAX_ENTRIES_PER_SLOT) {
                    entries.subList(MAX_ENTRIES_PER_SLOT, entries.size()).clear();
                }
                entries.forEach(LoadoutEntry::normalizeAmmoReserveLimit);
            }
        });
        if (classes.isEmpty()) {
            classes.addAll(defaultConfig().classes());
        }
        findClass("assault").ifPresent(LoadoutConfigData::ensurePortableAmmoCrate);
    }

    private static boolean validId(String id) {
        if (id == null || id.isBlank() || id.length() > 64) {
            return false;
        }
        for (int index = 0; index < id.length(); index++) {
            char value = id.charAt(index);
            if (!(value >= 'a' && value <= 'z') && !(value >= '0' && value <= '9')
                    && value != '_' && value != '-' && value != '.') {
                return false;
            }
        }
        return true;
    }

    private static boolean validEntryId(String id) {
        return id != null && id.matches("[a-z0-9_./-]{1,64}");
    }

    public LoadoutConfigData copy() {
        LoadoutConfigData result = new LoadoutConfigData();
        result.version = version;
        result.classes.clear();
        classes().stream().map(LoadoutClassDefinition::copy).forEach(result.classes::add);
        return result;
    }

    public static LoadoutConfigData defaultConfig() {
        LoadoutConfigData data = new LoadoutConfigData();
        data.classes.add(defaultClass("assault", "突击兵", 8));
        data.classes.add(defaultClass("support", "支援兵", 2));
        data.classes.add(defaultClass("engineer", "工程兵", 2));
        data.classes.add(defaultClass("recon", "侦察兵", 1));
        return data;
    }

    /** Keeps the portable 100-point crate available to the rifleman/assault role. */
    private static void ensurePortableAmmoCrate(LoadoutClassDefinition definition) {
        if (definition.findSlot(LoadoutSlot.GADGET_TWO.id()).isEmpty()) {
            return;
        }
        List<LoadoutEntry> entries = definition.entries(LoadoutSlot.GADGET_TWO.id());
        boolean present = entries.stream().anyMatch(entry ->
                "wok_infantry:ammo_supply_crate".equals(entry.itemId()));
        if (!present && entries.size() < MAX_ENTRIES_PER_SLOT) {
            entries.add(new LoadoutEntry("portable_ammo_crate", "小型弹药箱",
                    "wok_infantry:ammo_supply_crate", 1, ""));
        }
    }

    private static LoadoutClassDefinition defaultClass(String id, String name, int squadLimit) {
        LoadoutClassDefinition definition = new LoadoutClassDefinition(id, name, true, squadLimit);
        definition.entries(LoadoutSlot.PRIMARY).add(
                new LoadoutEntry("empty_primary", "未配置", "minecraft:air", 1, ""));
        definition.entries(LoadoutSlot.SECONDARY).add(
                new LoadoutEntry("empty_secondary", "未配置", "minecraft:air", 1, ""));
        if ("assault".equals(id)) {
            ensurePortableAmmoCrate(definition);
        }
        return definition;
    }
}
