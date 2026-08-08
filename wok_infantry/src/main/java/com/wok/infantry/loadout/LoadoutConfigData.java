package com.wok.infantry.loadout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LoadoutConfigData {
    private int version = 1;
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
        classes().removeIf(definition -> definition == null || definition.id() == null);
        classes().forEach(LoadoutClassDefinition::ensureSlots);
        if (classes.isEmpty()) {
            classes.addAll(defaultConfig().classes());
        }
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
        data.classes.add(defaultClass("assault", "突击兵"));
        data.classes.add(defaultClass("support", "支援兵"));
        data.classes.add(defaultClass("engineer", "工程兵"));
        data.classes.add(defaultClass("recon", "侦察兵"));
        return data;
    }

    private static LoadoutClassDefinition defaultClass(String id, String name) {
        LoadoutClassDefinition definition = new LoadoutClassDefinition(id, name, true);
        definition.entries(LoadoutSlot.PRIMARY).add(
                new LoadoutEntry("empty_primary", "未配置", "minecraft:air", 1, ""));
        definition.entries(LoadoutSlot.SECONDARY).add(
                new LoadoutEntry("empty_secondary", "未配置", "minecraft:air", 1, ""));
        return definition;
    }
}
