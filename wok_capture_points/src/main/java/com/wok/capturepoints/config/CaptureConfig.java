package com.wok.capturepoints.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class CaptureConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue DEFAULT_CAPTURE_SECONDS;
    public static final ForgeConfigSpec.BooleanValue SEQUENTIAL_CAPTURE;
    public static final ForgeConfigSpec.IntValue TICK_INTERVAL;
    public static final ForgeConfigSpec.IntValue SYNC_INTERVAL;
    public static final ForgeConfigSpec.IntValue MAX_SPEED_PLAYERS;
    public static final ForgeConfigSpec.EnumValue<ContestMode> CONTEST_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> SELECTOR_ITEM;
    public static final ForgeConfigSpec.IntValue MAX_POINTS;
    public static final ForgeConfigSpec.IntValue MAX_REGION_AXIS;
    public static final ForgeConfigSpec.BooleanValue COUNT_CREATIVE;
    public static final ForgeConfigSpec.BooleanValue ANNOUNCE_CHANGES;
    public static final ForgeConfigSpec.ConfigValue<String> BLUE_SCOREBOARD_TEAM;
    public static final ForgeConfigSpec.ConfigValue<String> RED_SCOREBOARD_TEAM;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLUE_TAGS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RED_TAGS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("capture");
        DEFAULT_CAPTURE_SECONDS = builder.comment("Seconds to move a point from enemy-owned to owned.")
                .defineInRange("defaultCaptureSeconds", 90, 1, 3600);
        SEQUENTIAL_CAPTURE = builder.comment("BLUE captures low-to-high order; RED high-to-low.")
                .define("sequentialCapture", false);
        TICK_INTERVAL = builder.defineInRange("calculationIntervalTicks", 5, 1, 20);
        SYNC_INTERVAL = builder.defineInRange("clientSyncIntervalTicks", 10, 2, 100);
        MAX_SPEED_PLAYERS = builder.comment("Maximum capture speed multiplier from player advantage.")
                .defineInRange("maxSpeedPlayers", 4, 1, 64);
        CONTEST_MODE = builder.comment("FREEZE stops progress if both teams are present; ADVANTAGE uses the difference.")
                .defineEnum("contestMode", ContestMode.ADVANTAGE);
        COUNT_CREATIVE = builder.define("countCreativePlayers", true);
        ANNOUNCE_CHANGES = builder.define("announceNeutralizedAndCaptured", true);
        builder.pop();

        builder.push("administration");
        SELECTOR_ITEM = builder.comment("Namespaced item id used for left/right corner selection.")
                .define("selectorItem", "minecraft:stick", value -> value instanceof String text
                        && text.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"));
        MAX_POINTS = builder.defineInRange("maxPoints", 64, 1, 512);
        MAX_REGION_AXIS = builder.defineInRange("maxRegionAxisBlocks", 512, 1, 4096);
        builder.pop();

        builder.push("teams");
        BLUE_SCOREBOARD_TEAM = builder.define("blueScoreboardTeam", "blue");
        RED_SCOREBOARD_TEAM = builder.define("redScoreboardTeam", "red");
        BLUE_TAGS = builder.defineListAllowEmpty(List.of("bluePlayerTags"),
                () -> List.of("wok_capture_blue"), value -> value instanceof String);
        RED_TAGS = builder.defineListAllowEmpty(List.of("redPlayerTags"),
                () -> List.of("wok_capture_red"), value -> value instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    private CaptureConfig() {
    }

    public enum ContestMode {
        FREEZE,
        ADVANTAGE
    }
}
