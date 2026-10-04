package com.wok.infantry.stamina;

import com.wok.infantry.config.InfantryServerConfig;
import com.wok.infantry.integration.tacz.TaczStaminaAdapter;
import com.wok.infantry.network.stamina.StaminaNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative stamina drain, recovery, persistence, and local-player synchronization. */
public final class StaminaEvents {
    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private StaminaEvents() {
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored ->
                new RuntimeState(player.level().getGameTime()));
        boolean movingHorizontally = runtime.movementTracker.sample(player.getX(), player.getZ());
        boolean enabled = isEnabled(player);
        if (!enabled) {
            syncIfDue(player, runtime, StaminaState.load(player), false, false);
            return;
        }

        long gameTime = player.level().getGameTime();
        StaminaState before = StaminaState.load(player);
        float arms = before.arms();
        float legs = before.legs();
        boolean armExertion = TaczStaminaAdapter.isAiming(player);
        boolean sprintBlocked = StaminaMath.shouldBlockSprint(legs, before.sprintBlocked(),
                InfantryServerConfig.legExhaustedResumeThreshold());
        if (sprintBlocked) {
            player.setSprinting(false);
        }
        boolean legExertion = !sprintBlocked && player.isSprinting() && movingHorizontally;

        if (armExertion) {
            arms = StaminaMath.drain(arms, InfantryServerConfig.armAdsDrainPerTick());
            runtime.lastArmExertionTick = gameTime;
        } else if (gameTime - runtime.lastArmExertionTick
                > InfantryServerConfig.staminaRecoveryDelayTicks()) {
            arms = StaminaMath.recover(arms, InfantryServerConfig.armRecoveryPerTick());
        }

        if (legExertion) {
            legs = StaminaMath.drain(legs, InfantryServerConfig.legSprintDrainPerTick());
            runtime.lastLegExertionTick = gameTime;
        } else if (gameTime - runtime.lastLegExertionTick
                > InfantryServerConfig.staminaRecoveryDelayTicks()) {
            legs = StaminaMath.recover(legs, InfantryServerConfig.legRecoveryPerTick());
        }

        sprintBlocked = StaminaMath.shouldBlockSprint(legs, sprintBlocked,
                InfantryServerConfig.legExhaustedResumeThreshold());
        StaminaState after = new StaminaState(arms, legs, sprintBlocked);
        boolean changed = !after.equals(before);
        if (changed) {
            after.save(player);
        }
        if (sprintBlocked) {
            player.setSprinting(false);
        }
        syncIfDue(player, runtime, after, true, changed);
    }

    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isEnabled(player)) {
            return;
        }
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored ->
                new RuntimeState(player.level().getGameTime()));
        StaminaState after = consumeJump(player);
        runtime.lastLegExertionTick = player.level().getGameTime();
        send(player, runtime, after, true);
    }

    /** Tactical Mantle teleports the player upward without posting a jump event. */
    public static boolean canAffordMantle(ServerPlayer player) {
        if (!isEnabled(player)) {
            return true;
        }
        StaminaState state = StaminaState.load(player);
        return !state.sprintBlocked() && state.legs() >= InfantryServerConfig.legMantleCost();
    }

    public static void onMantleStarted(ServerPlayer player) {
        if (!isEnabled(player)) {
            return;
        }
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored ->
                new RuntimeState(player.level().getGameTime()));
        StaminaState after = consumeLegs(player, InfantryServerConfig.legMantleCost());
        runtime.lastLegExertionTick = player.level().getGameTime();
        send(player, runtime, after, true);
    }

    static StaminaState consumeJump(ServerPlayer player) {
        return consumeLegs(player, InfantryServerConfig.legJumpCost());
    }

    private static StaminaState consumeLegs(ServerPlayer player, float cost) {
        StaminaState before = StaminaState.load(player);
        float legs = StaminaMath.drain(before.legs(), cost);
        StaminaState after = new StaminaState(before.arms(), legs,
                StaminaMath.shouldBlockSprint(legs, before.sprintBlocked(),
                        InfantryServerConfig.legExhaustedResumeThreshold()));
        after.save(player);
        if (after.sprintBlocked()) {
            player.setSprinting(false);
        }
        return after;
    }

    /** Permission checks live at the command boundary; this method owns runtime reset and sync. */
    public static StaminaState overwrite(ServerPlayer player, float arms, float legs) {
        StaminaState replacement = new StaminaState(arms, legs);
        replacement.save(player);
        if (replacement.sprintBlocked() && isEnabled(player)) {
            player.setSprinting(false);
        }
        long gameTime = player.level().getGameTime();
        RuntimeState runtime = new RuntimeState(gameTime);
        RUNTIME.put(player.getUUID(), runtime);
        send(player, runtime, replacement, isEnabled(player));
        return replacement;
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RuntimeState runtime = new RuntimeState(player.level().getGameTime());
            RUNTIME.put(player.getUUID(), runtime);
            send(player, runtime, StaminaState.load(player), isEnabled(player));
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        RUNTIME.remove(event.getEntity().getUUID());
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer replacement) {
            StaminaState.copy(original, replacement);
            RUNTIME.remove(original.getUUID());
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RuntimeState runtime = new RuntimeState(player.level().getGameTime());
            RUNTIME.put(player.getUUID(), runtime);
            send(player, runtime, StaminaState.load(player), isEnabled(player));
        }
    }

    private static boolean isEnabled(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.getAbilities().instabuild;
    }

    /** Used at the vanilla sprint setter, including repeated START_SPRINTING packets. */
    public static boolean isSprintBlocked(ServerPlayer player) {
        if (!isEnabled(player)) {
            return false;
        }
        StaminaState state = StaminaState.load(player);
        return StaminaMath.shouldBlockSprint(state.legs(), state.sprintBlocked(),
                InfantryServerConfig.legExhaustedResumeThreshold());
    }

    private static void syncIfDue(ServerPlayer player, RuntimeState runtime,
                                  StaminaState state, boolean enabled, boolean changed) {
        long gameTime = player.level().getGameTime();
        int interval = changed ? StaminaRules.ACTIVE_SYNC_INTERVAL_TICKS
                : StaminaRules.IDLE_SYNC_INTERVAL_TICKS;
        if (gameTime - runtime.lastSyncTick >= interval
                || runtime.lastEnabled != enabled
                || runtime.lastSprintBlocked != (enabled && state.sprintBlocked())) {
            send(player, runtime, state, enabled);
        }
    }

    private static void send(ServerPlayer player, RuntimeState runtime,
                             StaminaState state, boolean enabled) {
        if (player.connection != null) {
            StaminaNetwork.send(player, new StaminaSnapshot(state.arms(), state.legs(), enabled,
                    state.sprintBlocked()));
        }
        runtime.lastSyncTick = player.level().getGameTime();
        runtime.lastEnabled = enabled;
        runtime.lastSprintBlocked = enabled && state.sprintBlocked();
    }

    private static final class RuntimeState {
        private long lastArmExertionTick;
        private long lastLegExertionTick;
        private long lastSyncTick = Long.MIN_VALUE / 2;
        private boolean lastEnabled;
        private boolean lastSprintBlocked;
        private final StaminaMovementTracker movementTracker = new StaminaMovementTracker();

        private RuntimeState(long gameTime) {
            // Runtime is recreated on login/respawn: always wait a full cooldown instead of
            // treating an absent exertion timestamp as permission to regenerate immediately.
            lastArmExertionTick = gameTime;
            lastLegExertionTick = gameTime;
        }
    }
}
