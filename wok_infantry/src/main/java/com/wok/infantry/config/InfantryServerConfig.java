package com.wok.infantry.config;

import com.wok.infantry.ammo.transport.SupplyTransportRules;
import com.wok.infantry.ammo.transport.SupplyTransportProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Map;

/** Server-authoritative gameplay settings for physical infantry support assets. */
public final class InfantryServerConfig {
    public static final int SMALL_AMMO_SUPPLY_POINTS = 100;
    public static final int MEDIUM_AMMO_SUPPLY_POINTS = 500;
    public static final int LARGE_AMMO_SUPPLY_POINTS = 1_500;
    public static final int MIN_AMMO_RESERVE_LIMIT = 1;
    public static final int MAX_AMMO_RESERVE_LIMIT = 4_096;
    public static final int DEFAULT_AMMO_RESERVE_LIMIT = 180;
    public static final int DEFAULT_AMMO_SUPPLY_COOLDOWN_SECONDS = 3;
    public static final List<String> DEFAULT_SUPPLY_TRANSPORT_VEHICLES =
            List.of("superbwarfare:truck=large=3", "fcp:ural=large=3",
                    "fcp:hmmwv_unarmored_unarmed=medium=2",
                    "fcp:hmmwv_armored_unarmed=medium=1");
    public static final double DEFAULT_LARGE_AMMO_CRATE_CARRY_SPEED_MULTIPLIER = 0.25D;
    public static final double DEFAULT_ARM_ADS_DRAIN_PER_SECOND = 2.8D;
    public static final double DEFAULT_LEG_SPRINT_DRAIN_PER_SECOND = 8.0D;
    public static final double DEFAULT_LEG_JUMP_COST = 10.0D;
    public static final double DEFAULT_ARM_RECOVERY_PER_SECOND = 8.4D;
    public static final double DEFAULT_LEG_RECOVERY_PER_SECOND = 6.8D;
    public static final int DEFAULT_STAMINA_RECOVERY_DELAY_TICKS = 16;
    public static final double DEFAULT_LEG_EXHAUSTED_RESUME_THRESHOLD = 15.0D;

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue AMMO_RESERVE_LIMIT;
    private static final ForgeConfigSpec.IntValue AMMO_SUPPLY_COOLDOWN_SECONDS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>>
            SUPPLY_TRANSPORT_VEHICLES;
    private static final ForgeConfigSpec.DoubleValue LARGE_AMMO_CRATE_CARRY_SPEED_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue ARM_ADS_DRAIN_PER_SECOND;
    private static final ForgeConfigSpec.DoubleValue LEG_SPRINT_DRAIN_PER_SECOND;
    private static final ForgeConfigSpec.DoubleValue LEG_JUMP_COST;
    private static final ForgeConfigSpec.DoubleValue ARM_RECOVERY_PER_SECOND;
    private static final ForgeConfigSpec.DoubleValue LEG_RECOVERY_PER_SECOND;
    private static final ForgeConfigSpec.IntValue STAMINA_RECOVERY_DELAY_TICKS;
    private static final ForgeConfigSpec.DoubleValue LEG_EXHAUSTED_RESUME_THRESHOLD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Physical ammunition supply crate settings")
                .push("ammoSupply");
        AMMO_RESERVE_LIMIT = builder
                .comment("Maximum reserve rounds supplied for each detected TaCZ ammo type")
                .defineInRange("reserveRoundsPerAmmoType", DEFAULT_AMMO_RESERVE_LIMIT,
                        MIN_AMMO_RESERVE_LIMIT, MAX_AMMO_RESERVE_LIMIT);
        AMMO_SUPPLY_COOLDOWN_SECONDS = builder
                .comment("Successful supply cooldown per player, in seconds")
                .defineInRange("cooldownSeconds", DEFAULT_AMMO_SUPPLY_COOLDOWN_SECONDS,
                        0, 300);
        builder.pop();
        builder.comment("Vehicle-carried physical ammunition supply settings")
                .push("supplyTransport");
        SUPPLY_TRANSPORT_VEHICLES = builder
                .comment("Vehicle entity id, cargo type and full-load capacity entries",
                        "Format: entity_id=large|medium=capacity; legacy entity_id=capacity",
                        "is accepted as large cargo",
                        "Example: [\"superbwarfare:truck=large=3\",",
                        "\"fcp:hmmwv_unarmored_unarmed=medium=2\"]",
                        "Each newly seen configured vehicle starts full; saved cargo is not",
                        "refilled by a restart or by increasing the configured capacity")
                .defineList("vehicles", DEFAULT_SUPPLY_TRANSPORT_VEHICLES,
                        SupplyTransportRules::isValidEntry);
        LARGE_AMMO_CRATE_CARRY_SPEED_MULTIPLIER = builder
                .comment("Player movement-speed multiplier while carrying at least one large crate")
                .defineInRange("carrySpeedMultiplier",
                        DEFAULT_LARGE_AMMO_CRATE_CARRY_SPEED_MULTIPLIER, 0.05D, 1.0D);
        builder.pop();
        builder.comment("Server-authoritative split arm and leg stamina settings")
                .push("stamina");
        ARM_ADS_DRAIN_PER_SECOND = builder
                .comment("Arm stamina consumed per second while aiming a TaCZ gun")
                .defineInRange("armAdsDrainPerSecond", DEFAULT_ARM_ADS_DRAIN_PER_SECOND,
                        0.0D, 25.0D);
        LEG_SPRINT_DRAIN_PER_SECOND = builder
                .comment("Leg stamina consumed per second while actually sprinting")
                .defineInRange("legSprintDrainPerSecond", DEFAULT_LEG_SPRINT_DRAIN_PER_SECOND,
                        0.0D, 25.0D);
        LEG_JUMP_COST = builder
                .comment("Leg stamina consumed for each jump")
                .defineInRange("legJumpCost", DEFAULT_LEG_JUMP_COST, 0.0D, 100.0D);
        ARM_RECOVERY_PER_SECOND = builder
                .comment("Arm stamina recovered per second after the recovery delay")
                .defineInRange("armRecoveryPerSecond", DEFAULT_ARM_RECOVERY_PER_SECOND,
                        0.0D, 50.0D);
        LEG_RECOVERY_PER_SECOND = builder
                .comment("Leg stamina recovered per second after the recovery delay")
                .defineInRange("legRecoveryPerSecond", DEFAULT_LEG_RECOVERY_PER_SECOND,
                        0.0D, 50.0D);
        STAMINA_RECOVERY_DELAY_TICKS = builder
                .comment("Ticks without exertion before either stamina pool starts recovering")
                .defineInRange("recoveryDelayTicks", DEFAULT_STAMINA_RECOVERY_DELAY_TICKS,
                        0, 200);
        LEG_EXHAUSTED_RESUME_THRESHOLD = builder
                .comment("Leg stamina required before sprinting can resume after exhaustion")
                .defineInRange("legExhaustedResumeThreshold",
                        DEFAULT_LEG_EXHAUSTED_RESUME_THRESHOLD, 0.0D, 100.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private InfantryServerConfig() {
    }

    public static int ammoReserveLimit() {
        return SPEC.isLoaded() ? AMMO_RESERVE_LIMIT.get() : DEFAULT_AMMO_RESERVE_LIMIT;
    }

    public static int ammoSupplyCooldownTicks() {
        int seconds = SPEC.isLoaded() ? AMMO_SUPPLY_COOLDOWN_SECONDS.get()
                : DEFAULT_AMMO_SUPPLY_COOLDOWN_SECONDS;
        return seconds * 20;
    }

    public static Map<ResourceLocation, SupplyTransportProfile> supplyTransportVehicleProfiles() {
        List<? extends String> configured = SPEC.isLoaded()
                ? SUPPLY_TRANSPORT_VEHICLES.get() : DEFAULT_SUPPLY_TRANSPORT_VEHICLES;
        return SupplyTransportRules.parse(configured);
    }

    public static SupplyTransportProfile supplyTransportProfile(ResourceLocation entityId) {
        if (entityId == null) {
            return null;
        }
        return supplyTransportVehicleProfiles().get(entityId);
    }

    public static double largeAmmoCrateCarrySpeedMultiplier() {
        return configured(LARGE_AMMO_CRATE_CARRY_SPEED_MULTIPLIER,
                DEFAULT_LARGE_AMMO_CRATE_CARRY_SPEED_MULTIPLIER);
    }

    public static float armAdsDrainPerTick() {
        return (float) configured(ARM_ADS_DRAIN_PER_SECOND,
                DEFAULT_ARM_ADS_DRAIN_PER_SECOND) / 20.0F;
    }

    public static float legSprintDrainPerTick() {
        return (float) configured(LEG_SPRINT_DRAIN_PER_SECOND,
                DEFAULT_LEG_SPRINT_DRAIN_PER_SECOND) / 20.0F;
    }

    public static float legJumpCost() {
        return (float) configured(LEG_JUMP_COST, DEFAULT_LEG_JUMP_COST);
    }

    public static float armRecoveryPerTick() {
        return (float) configured(ARM_RECOVERY_PER_SECOND,
                DEFAULT_ARM_RECOVERY_PER_SECOND) / 20.0F;
    }

    public static float legRecoveryPerTick() {
        return (float) configured(LEG_RECOVERY_PER_SECOND,
                DEFAULT_LEG_RECOVERY_PER_SECOND) / 20.0F;
    }

    public static int staminaRecoveryDelayTicks() {
        return SPEC.isLoaded() ? STAMINA_RECOVERY_DELAY_TICKS.get()
                : DEFAULT_STAMINA_RECOVERY_DELAY_TICKS;
    }

    public static float legExhaustedResumeThreshold() {
        return (float) configured(LEG_EXHAUSTED_RESUME_THRESHOLD,
                DEFAULT_LEG_EXHAUSTED_RESUME_THRESHOLD);
    }

    private static double configured(ForgeConfigSpec.DoubleValue value,
                                     double defaultValue) {
        return SPEC.isLoaded() ? value.get() : defaultValue;
    }

    /** Updates and immediately persists the active world's server config. */
    public static boolean setAmmoReserveLimit(int rounds) {
        if (!SPEC.isLoaded() || rounds < MIN_AMMO_RESERVE_LIMIT
                || rounds > MAX_AMMO_RESERVE_LIMIT) {
            return false;
        }
        AMMO_RESERVE_LIMIT.set(rounds);
        SPEC.save();
        return true;
    }
}
