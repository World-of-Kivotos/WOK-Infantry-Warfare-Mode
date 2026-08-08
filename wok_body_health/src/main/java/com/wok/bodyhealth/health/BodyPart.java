package com.wok.bodyhealth.health;

public enum BodyPart {
    HEAD("head", true, 0.0F),
    CHEST("chest", true, 0.0F),
    ABDOMEN("abdomen", false, 1.5F),
    LEFT_ARM("left_arm", false, 0.7F),
    RIGHT_ARM("right_arm", false, 0.7F),
    LEFT_LEG("left_leg", false, 1.0F),
    RIGHT_LEG("right_leg", false, 1.0F);

    private final String key;
    private final boolean critical;
    private final float overflowMultiplier;

    BodyPart(String key, boolean critical, float overflowMultiplier) {
        this.key = key;
        this.critical = critical;
        this.overflowMultiplier = overflowMultiplier;
    }

    public String key() {
        return key;
    }

    public boolean isCritical() {
        return critical;
    }

    public float overflowMultiplier() {
        return overflowMultiplier;
    }
}
