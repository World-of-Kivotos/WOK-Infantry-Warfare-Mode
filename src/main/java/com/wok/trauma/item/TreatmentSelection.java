package com.wok.trauma.item;

/** Client-selectable treatment target. Names match WOK Body Health's public API. */
public enum TreatmentSelection {
    AUTO("", "auto"),
    HEAD("HEAD", "head"),
    CHEST("CHEST", "chest"),
    ABDOMEN("ABDOMEN", "abdomen"),
    LEFT_ARM("LEFT_ARM", "left_arm"),
    RIGHT_ARM("RIGHT_ARM", "right_arm"),
    LEFT_LEG("LEFT_LEG", "left_leg"),
    RIGHT_LEG("RIGHT_LEG", "right_leg");

    private final String bodyPartName;
    private final String translationSuffix;

    TreatmentSelection(String bodyPartName, String translationSuffix) {
        this.bodyPartName = bodyPartName;
        this.translationSuffix = translationSuffix;
    }

    public String bodyPartName() {
        return bodyPartName;
    }

    public boolean isAutomatic() {
        return this == AUTO;
    }

    public String translationKey() {
        return "treatment_selection.wok_trauma." + translationSuffix;
    }

    public TreatmentSelection cycle(int direction) {
        TreatmentSelection[] selections = values();
        return selections[Math.floorMod(ordinal() + direction, selections.length)];
    }

    public static TreatmentSelection fromOrdinal(int ordinal) {
        TreatmentSelection[] selections = values();
        return ordinal >= 0 && ordinal < selections.length ? selections[ordinal] : null;
    }
}
