package com.wok.trauma.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TraumaConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue PAIN_DURATION_TICKS;
    public static final ForgeConfigSpec.DoubleValue BLEEDING_CHANCE;
    public static final ForgeConfigSpec.DoubleValue MAJOR_BLEEDING_CHANCE;
    public static final ForgeConfigSpec.DoubleValue TREMOR_SPREAD_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue TREMOR_ADS_TIME_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CONCUSSION_CHANCE;
    public static final ForgeConfigSpec.IntValue CONCUSSION_DURATION_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("gunshot_trauma");

        PAIN_DURATION_TICKS = builder
                .comment("Pain duration after a TaCZ gunshot, in ticks (20 ticks = 1 second).")
                .defineInRange("painDurationTicks", 600, 20, 20 * 60 * 10);
        BLEEDING_CHANCE = builder
                .comment("Chance of normal bleeding after a gunshot. Evaluated after major bleeding.")
                .defineInRange("bleedingChance", 0.35D, 0.0D, 1.0D);
        MAJOR_BLEEDING_CHANCE = builder
                .comment("Chance of major bleeding after a gunshot.")
                .defineInRange("majorBleedingChance", 0.10D, 0.0D, 1.0D);
        TREMOR_SPREAD_MULTIPLIER = builder
                .comment("TaCZ inaccuracy multiplier while trembling.")
                .defineInRange("tremorSpreadMultiplier", 1.50D, 1.0D, 10.0D);
        TREMOR_ADS_TIME_MULTIPLIER = builder
                .comment("TaCZ ADS time multiplier while trembling. Larger values mean slower aiming.")
                .defineInRange("tremorAdsTimeMultiplier", 1.35D, 1.0D, 10.0D);

        builder.pop();

        builder.push("explosion_trauma");
        CONCUSSION_CHANCE = builder
                .comment("Chance of concussion after taking explosion damage.")
                .defineInRange("concussionChance", 0.35D, 0.0D, 1.0D);
        CONCUSSION_DURATION_TICKS = builder
                .comment("Concussion duration in ticks (20 ticks = 1 second).")
                .defineInRange("concussionDurationTicks", 20 * 20, 20, 20 * 60 * 10);
        builder.pop();

        SPEC = builder.build();
    }

    private TraumaConfig() {
    }
}
