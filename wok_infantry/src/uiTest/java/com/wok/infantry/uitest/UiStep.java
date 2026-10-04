package com.wok.infantry.uitest;

import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * One step of a {@link UiCase}. The runner calls {@link #run} once per client tick until it
 * returns {@code true}; {@link UiCaseContext#stepTicks()} counts the ticks of the current step and
 * {@link UiCaseContext#stepState()} keeps state between them. Failing a step fails the tier.
 */
@FunctionalInterface
public interface UiStep {
    /** Ticks a step may wait for a probe frame or a condition before it fails. */
    int WAIT_LIMIT = 80;

    boolean run(UiCaseContext context) throws Exception;

    /** Waits {@code ticks} client ticks. */
    static UiStep waitTicks(int ticks) {
        return context -> context.stepTicks() >= ticks;
    }

    /** Runs {@code action} once. */
    static UiStep action(Consumer<UiCaseContext> action) {
        return context -> {
            action.accept(context);
            return true;
        };
    }

    /** Runs {@code step} only when {@code condition} holds for this tier. */
    static UiStep when(Predicate<UiCaseContext> condition, UiStep step) {
        return context -> !condition.test(context) || step.run(context);
    }

    /** Waits until {@code condition} holds. */
    static UiStep until(String description, Predicate<UiCaseContext> condition) {
        return context -> {
            if (condition.test(context)) {
                return true;
            }
            if (context.stepTicks() > WAIT_LIMIT * 4) {
                context.fail("Timed out waiting for " + description);
            }
            return false;
        };
    }

    /** Clicks the control {@code uiId} of the next recorded frame. */
    static UiStep click(String uiId) {
        return withControl(uiId, (context, control) ->
                UiInputDriver.click(context.minecraft(), control));
    }

    /** Hovers the control {@code uiId} of the next recorded frame. */
    static UiStep hover(String uiId) {
        return withControl(uiId, (context, control) ->
                UiInputDriver.hover(context.minecraft(), control));
    }

    /** Presses {@code keyCode} (GLFW) on the open screen. */
    static UiStep key(int keyCode, int modifiers) {
        return context -> {
            UiInputDriver.key(context.minecraft(), keyCode, modifiers);
            return true;
        };
    }

    /**
     * Runs {@code task} on the integrated server and waits for its answer: a result starting with
     * {@code OK} completes the step, anything else fails it.
     */
    static UiStep server(String description, Function<MinecraftServer, String> task) {
        return context -> {
            String result = context.serverTask(description, task);
            if (result == null) {
                return false;
            }
            if (!result.startsWith("OK")) {
                context.fail(description + ": " + result);
            }
            return true;
        };
    }

    /** {@link #server} with the UUID of the client's own player (operator grants, fixtures). */
    static UiStep serverForPlayer(String description,
                                  java.util.function.BiFunction<MinecraftServer, java.util.UUID,
                                          String> task) {
        return context -> {
            if (context.minecraft().player == null) {
                context.fail(description + ": no client player");
            }
            java.util.UUID playerId = context.minecraft().player.getUUID();
            String result = context.serverTask(description,
                    server -> task.apply(server, playerId));
            if (result == null) {
                return false;
            }
            if (!result.startsWith("OK")) {
                context.fail(description + ": " + result);
            }
            return true;
        };
    }

    /** Requests a probe frame, then hands the control {@code uiId} of that frame to {@code use}. */
    private static UiStep withControl(String uiId,
                                      java.util.function.BiConsumer<UiCaseContext,
                                              UiLayoutFrame.Control> use) {
        return context -> {
            UiLayoutFrame frame = context.freshProbe();
            if (frame == null) {
                if (context.stepTicks() > WAIT_LIMIT) {
                    context.fail("No probe frame to find " + uiId);
                }
                return false;
            }
            UiLayoutFrame.Control control = frame.control(uiId);
            if (control == null || !control.visible()) {
                context.fail("Control " + uiId + " is not on screen");
            }
            use.accept(context, control);
            return true;
        };
    }
}
