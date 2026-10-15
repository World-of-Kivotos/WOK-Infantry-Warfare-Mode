package com.wok.infantry.loadout;

import java.util.Objects;

/** Administrator-defined equipment category and its physical player-inventory destination. */
public final class LoadoutSlotDefinition {
    private String id;
    private String displayName;
    private LoadoutInventoryTarget target;
    private boolean required;

    public LoadoutSlotDefinition() {
        this("primary", "主武器", LoadoutInventoryTarget.HOTBAR_1, true);
    }

    public LoadoutSlotDefinition(String id, String displayName,
                                 LoadoutInventoryTarget target, boolean required) {
        this.id = Objects.requireNonNullElse(id, "primary");
        this.displayName = Objects.requireNonNullElse(displayName, this.id);
        this.target = Objects.requireNonNullElse(target, LoadoutInventoryTarget.HOTBAR_1);
        this.required = required;
    }

    public String id() {
        return Objects.requireNonNullElse(id, "");
    }

    public String displayName() {
        String value = Objects.requireNonNullElse(displayName, "").trim();
        return value.isBlank() ? id() : value;
    }

    public LoadoutInventoryTarget target() {
        return Objects.requireNonNullElse(target, LoadoutInventoryTarget.HOTBAR_1);
    }

    public boolean required() {
        return required;
    }

    public void update(String displayName, LoadoutInventoryTarget target, boolean required) {
        this.displayName = Objects.requireNonNullElse(displayName, id()).trim();
        this.target = Objects.requireNonNull(target, "target");
        this.required = required;
    }

    public LoadoutSlotDefinition copy() {
        return new LoadoutSlotDefinition(id(), displayName(), target(), required());
    }

    public static LoadoutSlotDefinition legacy(LoadoutSlot slot) {
        return new LoadoutSlotDefinition(slot.id(), slot.displayName(),
                LoadoutInventoryTarget.legacyTarget(slot), true);
    }
}
