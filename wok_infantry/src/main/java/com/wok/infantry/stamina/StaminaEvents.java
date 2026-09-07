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
                RuntimeState.create(StaminaState.load(player)));
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
        boolean legExertion = player.isSprinting() && movingHorizontally;

        if (runtime.legsExhausted
                && legs < InfantryServerConfig.legExhaustedResumeThreshold()) {
            player.setSprinting(false);
            legExertion = false;
        } else if (runtime.legsExhausted) {
            runtime.legsExhausted = false;
        }

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
            if (legs <= 0.0F) {
                runtime.legsExhausted = true;
                player.setSprinting(false);
            }
        } else if (gameTime - runtime.lastLegExertionTick
                > InfantryServerConfig.staminaRecoveryDelayTicks()) {
            legs = StaminaMath.recover(legs, InfantryServerConfig.legRecoveryPerTick());
        }

        StaminaState after = new StaminaState(arms, legs);
        boolean changed = !after.equals(before);
        if (changed) {
            after.save(player);
        }
        syncIfDue(player, runtime, after, true, changed);
    }

    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isEnabled(player)) {
            return;
        }
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored ->
                RuntimeState.create(StaminaState.load(player)));
        StaminaState after = consumeJump(player);
        runtime.lastLegExertionTick = player.level().getGameTime();
        if (after.legs() <= 0.0F) {
            runtime.legsExhausted = true;
        }
        send(player, runtime, after, true);
    }

    static StaminaState consumeJump(ServerPlayer player) {
        StaminaState before = StaminaState.load(player);
        StaminaState after = new StaminaState(before.arms(),
                StaminaMath.drain(before.legs(), InfantryServerConfig.legJumpCost()));
        after.save(player);
        return after;
    }

    /** Permission checks live at the command boundary; this method owns runtime reset and sync. */
    public static StaminaState overwrite(ServerPlayer player, float arms, float legs) {
        StaminaState replacement = new StaminaState(arms, legs);
        replacement.save(player);
        RuntimeState runtime = RuntimeState.create(replacement);
        long gameTime = player.level().getGameTime();
        runtime.lastArmExertionTick = gameTime;
        runtime.lastLegExertionTick = gameTime;
        RUNTIME.put(player.getUUID(), runtime);
        send(player, runtime, replacement, isEnabled(player));
        return replacement;
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RuntimeState runtime = RuntimeState.create(StaminaState.load(player));
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
            RuntimeState runtime = RuntimeState.create(StaminaState.load(player));
            RUNTIME.put(player.getUUID(), runtime);
            send(player, runtime, StaminaState.load(player), isEnabled(player));
        }
    }

    private static boolean isEnabled(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.getAbilities().instabuild;
    }

    private static void syncIfDue(ServerPlayer player, RuntimeState runtime,
                                  StaminaState state, boolean enabled, boolean changed) {
        long gameTime = player.level().getGameTime();
        int interval = changed ? StaminaRules.ACTIVE_SYNC_INTERVAL_TICKS
                : StaminaRules.IDLE_SYNC_INTERVAL_TICKS;
        if (gameTime - runtime.lastSyncTick >= interval
                || runtime.lastEnabled != enabled) {
            send(player, runtime, state, enabled);
        }
    }

    private static void send(ServerPlayer player, RuntimeState runtime,
                             StaminaState state, boolean enabled) {
        if (player.connection != null) {
            StaminaNetwork.send(player, new StaminaSnapshot(state.arms(), state.legs(), enabled));
        }
        runtime.lastSyncTick = player.level().getGameTime();
        runtime.lastEnabled = enabled;
    }

    private static final class RuntimeState {
        private long lastArmExertionTick = Long.MIN_VALUE / 2;
        private long lastLegExertionTick = Long.MIN_VALUE / 2;
        private long lastSyncTick = Long.MIN_VALUE / 2;
        private boolean lastEnabled;
        private boolean legsExhausted;
        private final StaminaMovementTracker movementTracker = new StaminaMovementTracker();

        private static RuntimeState create(StaminaState state) {
            RuntimeState runtime = new RuntimeState();
            runtime.legsExhausted = state.legs() <= 0.0F;
            return runtime;
        }
    }
}
