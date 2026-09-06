package com.wok.downed.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DownedConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue BLEEDOUT_SECONDS;
    public static final ForgeConfigSpec.IntValue RESCUE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue RESCUE_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue MOVEMENT_TOLERANCE;
    public static final ForgeConfigSpec.DoubleValue REVIVE_HEALTH;
    public static final ForgeConfigSpec.DoubleValue BODY_HEALTH_REVIVE_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue DRAG_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DRAG_FOLLOW_DISTANCE;
    public static final ForgeConfigSpec.IntValue FINISHING_DAMAGE_GRACE_SECONDS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_FINISHING_DAMAGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("downed");
        ENABLED = builder.comment("Enable interception of player death.")
                .define("enabled", true);
        BLEEDOUT_SECONDS = builder.comment("Seconds a casualty can wait before dying.")
                .defineInRange("bleedoutSeconds", 120, 5, 1800);
        RESCUE_SECONDS = builder.comment("Seconds a stationary rescuer needs to complete treatment.")
                .defineInRange("rescueSeconds", 8, 1, 120);
        RESCUE_DISTANCE = builder.comment("Maximum rescuer-to-casualty distance in blocks.")
                .defineInRange("rescueDistance", 3.0D, 1.0D, 12.0D);
        MOVEMENT_TOLERANCE = builder.comment("Maximum movement from the rescue start position.")
                .defineInRange("movementTolerance", 0.75D, 0.0D, 4.0D);
        REVIVE_HEALTH = builder.comment("Vanilla health restored when rescue completes.")
                .defineInRange("reviveHealth", 6.0D, 1.0D, 1024.0D);
        BODY_HEALTH_REVIVE_AMOUNT = builder.comment(
                        "Amount restored to each body part when WOK Body Health is installed.")
                .defineInRange("bodyHealthReviveAmount", 8.0D, 0.0D, 1024.0D);
        DRAG_SPEED_MULTIPLIER = builder.comment(
                        "Movement-speed multiplier applied while dragging a casualty.")
                .defineInRange("dragSpeedMultiplier", 0.55D, 0.1D, 1.0D);
        DRAG_FOLLOW_DISTANCE = builder.comment(
                        "Distance in blocks that a dragged casualty follows behind the rescuer.")
                .defineInRange("dragFollowDistance", 1.15D, 0.4D, 2.5D);
        FINISHING_DAMAGE_GRACE_SECONDS = builder.comment(
                        "Seconds after going down during which follow-up damage cannot finish a casualty.")
                .defineInRange("finishingDamageGraceSeconds", 2, 0, 30);
        ALLOW_FINISHING_DAMAGE = builder.comment(
                        "Allow any new damage after the grace period to finish a downed casualty.")
                .define("allowFinishingDamage", true);
        builder.pop();
        SPEC = builder.build();
    }

    private DownedConfig() {
    }
}
