package com.wok.infantry.staminatest;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.config.InfantryServerConfig;
import com.wok.infantry.stamina.StaminaEvents;
import com.wok.infantry.stamina.StaminaState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Observe actual server ticks, without advancing the clock or replacing production logic. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, value = Dist.CLIENT)
public final class StaminaRecoveryAcceptance {
    private static ServerPlayer player;
    private static CompletableFuture<List<String>> result;
    private static final List<String> checks = new ArrayList<>();
    private static long started;
    private static int stage, delay;
    private static StaminaState baseline;

    static CompletableFuture<List<String>> start(ServerPlayer target) {
        player = target;
        result = new CompletableFuture<>();
        checks.clear();
        delay = InfantryServerConfig.staminaRecoveryDelayTicks();
        stage = 0;
        player.setSprinting(false);
        baseline = StaminaEvents.overwrite(player, 60, 60);
        started = player.level().getGameTime();
        return result;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player != player || result == null || result.isDone()) return;
        try {
            long elapsed = player.level().getGameTime() - started;
            if (elapsed <= 0) return;
            StaminaState state = StaminaState.load(player);
            if (stage == 0) {
                int jumpAt = delay / 2;
                require(elapsed <= delay ? state.arms() == 60 : state.arms() > 60,
                        "arm recovery must wait the entire cooldown: " + elapsed);
                if (elapsed < jumpAt) require(state.legs() == 60, "legs cannot recover before jump");
                if (elapsed == jumpAt) {
                    StaminaEvents.onPlayerJump(new LivingEvent.LivingJumpEvent(player));
                    baseline = StaminaState.load(player);
                    require(baseline.legs() == 60 - InfantryServerConfig.legJumpCost(), "jump consumes configured stamina");
                } else if (elapsed > jumpAt) {
                    require(elapsed <= jumpAt + delay ? state.legs() == baseline.legs() : state.legs() > baseline.legs(),
                            "jump restarts the complete leg cooldown: " + elapsed);
                }
                if (elapsed == delay) checks.add("PASS neither pool regenerates during its cooldown");
                if (elapsed == delay + 1) checks.add("PASS arm recovery starts after 60 ticks while legs still wait after jump");
                if (elapsed > jumpAt + delay) {
                    checks.add("PASS jump resets the leg cooldown without restarting the arm cooldown");
                    // Exercise the exact runtime recreation callbacks with the saved reserves intact.
                    baseline = state;
                    StaminaEvents.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(player));
                    StaminaEvents.onPlayerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(player));
                    stage = 1;
                    started = player.level().getGameTime();
                }
            } else {
                require(elapsed <= delay
                                ? state.arms() == baseline.arms() && state.legs() == baseline.legs()
                                : state.arms() > baseline.arms() && state.legs() > baseline.legs(),
                        "runtime recreation must preserve a full cooldown, stage " + stage + ", tick " + elapsed);
                if (elapsed > delay) {
                    checks.add("PASS " + (stage == 1 ? "login" : "respawn") + " waits all 60 ticks then resumes recovery");
                    if (stage == 2) {
                        result.complete(List.copyOf(checks));
                        return;
                    }
                    baseline = state;
                    StaminaEvents.onPlayerRespawn(new PlayerEvent.PlayerRespawnEvent(player, false));
                    stage = 2;
                    started = player.level().getGameTime();
                }
            }
        } catch (Throwable failure) { result.completeExceptionally(failure); }
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalStateException(message);
    }
}
