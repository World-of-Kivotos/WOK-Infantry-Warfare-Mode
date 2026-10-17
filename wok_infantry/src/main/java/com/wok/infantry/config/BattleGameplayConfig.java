package com.wok.infantry.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** New settings live separately so existing stamina/logistics configurations are untouched. */
public final class BattleGameplayConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue INITIAL_TICKETS, DEATH_COST, PRONE_SETTLE_TICKS;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("tickets");
        INITIAL_TICKETS = builder.comment("Starting manpower per side; applied only to a new round.")
                .defineInRange("initial", 500, 1, 1_000_000);
        DEATH_COST = builder.comment("Final player death or voluntary redeployment, never merely going down.")
                .defineInRange("deathCost", 1, 0, 1000);
        builder.pop().push("machineGun");
        PRONE_SETTLE_TICKS = builder.comment("Stationary prone ticks to reach the gun pack's full recoil reduction.")
                .defineInRange("proneSettleTicks", 40, 1, 400);
        builder.pop();
        SPEC = builder.build();
    }
    private BattleGameplayConfig() {}
}
