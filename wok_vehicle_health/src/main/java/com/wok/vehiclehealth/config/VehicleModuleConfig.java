package com.wok.vehiclehealth.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class VehicleModuleConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue OPTICS_HIT_RADIUS;
    public static final ForgeConfigSpec.IntValue OPTICS_INTERFERENCE_TICKS;
    public static final ForgeConfigSpec.DoubleValue OPTICS_MAX_HEALTH_RATIO;
    public static final ForgeConfigSpec.DoubleValue OPTICS_MIN_MAX_HEALTH;
    public static final ForgeConfigSpec.DoubleValue OPTICS_PASSIVE_REPAIR_RATE;
    public static final ForgeConfigSpec.DoubleValue TURRET_DAMAGED_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue TURRET_DESTROYED_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue STOP_ON_SINGLE_TRACK_DESTROYED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("vehicle_modules");
        OPTICS_HIT_RADIUS = builder
                .comment("Radius in blocks around the model-derived gunner optic position.")
                .defineInRange("opticsHitRadius", 0.8D, 0.2D, 3.0D);
        OPTICS_INTERFERENCE_TICKS = builder
                .comment("Base duration of the snow/static screen effect after an optics hit.")
                .defineInRange("opticsInterferenceTicks", 60, 10, 400);
        OPTICS_MAX_HEALTH_RATIO = builder
                .comment("Optics maximum health as a fraction of Superb Warfare turret max health.")
                .defineInRange("opticsMaxHealthRatio", 0.5D, 0.05D, 2.0D);
        OPTICS_MIN_MAX_HEALTH = builder
                .comment("Minimum optics maximum health when a vehicle has a very small turret health pool.")
                .defineInRange("opticsMinMaxHealth", 30.0D, 1.0D, 10000.0D);
        OPTICS_PASSIVE_REPAIR_RATE = builder
                .comment("Fraction of optics max health repaired each tick. 0.0005 restores a fully destroyed optic in about 100 seconds.")
                .defineInRange("opticsPassiveRepairRate", 0.0005D, 0.0D, 0.1D);
        TURRET_DAMAGED_SPEED_MULTIPLIER = builder
                .comment("Turret rotation multiplier after the turret ring has taken meaningful damage.")
                .defineInRange("turretDamagedSpeedMultiplier", 0.45D, 0.01D, 1.0D);
        TURRET_DESTROYED_SPEED_MULTIPLIER = builder
                .comment("Turret rotation multiplier after the turret ring reaches zero health.")
                .defineInRange("turretDestroyedSpeedMultiplier", 0.08D, 0.0D, 1.0D);
        STOP_ON_SINGLE_TRACK_DESTROYED = builder
                .comment("When true, either destroyed track immobilizes the vehicle.")
                .define("stopOnSingleTrackDestroyed", true);
        builder.pop();

        SPEC = builder.build();
    }

    private VehicleModuleConfig() {
    }
}
