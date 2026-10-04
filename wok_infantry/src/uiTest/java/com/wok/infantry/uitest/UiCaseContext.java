package com.wok.infantry.uitest;

import com.wok.infantry.client.screen.TacticalShellLayout;
import com.wok.infantry.client.screen.UiScale;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * What a case sees while it runs on one tier: the client, the tier, per-step and per-case scratch
 * state, probe frames for uiId lookups and the observation log of the acceptance result.
 */
public final class UiCaseContext {
    /** Failure of a case step or check; the runner records it and moves to the next tier. */
    public static final class Failure extends RuntimeException {
        public Failure(String message) {
            super(message);
        }
    }

    private final Minecraft minecraft;
    private final UiCase uiCase;
    private final UiTier tier;
    private final Consumer<String> observer;
    private final Map<String, Object> caseState;
    private final Map<String, Object> stepState = new HashMap<>();
    private int stepTicks;
    private boolean probeRequested;
    private volatile UiLayoutFrame probe;

    UiCaseContext(Minecraft minecraft, UiCase uiCase, UiTier tier, Consumer<String> observer,
                  Map<String, Object> caseState) {
        this.minecraft = minecraft;
        this.uiCase = uiCase;
        this.tier = tier;
        this.observer = observer;
        this.caseState = caseState;
    }

    public Minecraft minecraft() {
        return minecraft;
    }

    public UiCase uiCase() {
        return uiCase;
    }

    /** Tier being captured ({@code null} while preparing the case). */
    public UiTier tier() {
        return tier;
    }

    public Screen screen() {
        return minecraft.screen;
    }

    /** Whether the current window lays out in the compact size class (320×240, 480×270). */
    public boolean tight() {
        int factor = UiScale.factor(minecraft);
        return TacticalShellLayout.density(
                UiScale.layoutSize(minecraft.getWindow().getGuiScaledWidth(), factor),
                UiScale.layoutSize(minecraft.getWindow().getGuiScaledHeight(), factor))
                == TacticalShellLayout.Density.COMPACT;
    }

    /** Adds a line to the acceptance result file. */
    public void observe(String line) {
        observer.accept(line);
    }

    /** Fails the current step or check. */
    public void fail(String message) {
        throw new Failure(message);
    }

    /** Fails unless {@code condition} holds. */
    public void require(boolean condition, String message) {
        if (!condition) {
            fail(message);
        }
    }

    /** Scratch state shared by every tier of the case (for example a prepared fixture). */
    public Map<String, Object> caseState() {
        return caseState;
    }

    /** Scratch state of the current step, cleared when the next step starts. */
    public Map<String, Object> stepState() {
        return stepState;
    }

    /** Ticks the current step has run. */
    public int stepTicks() {
        return stepTicks;
    }

    void startStep() {
        stepTicks = 0;
        stepState.clear();
        probeRequested = false;
        probe = null;
    }

    void tickStep() {
        stepTicks++;
    }

    /**
     * A probe frame recorded after the current step started; requests one on the first call and
     * returns {@code null} until it arrives.
     */
    public UiLayoutFrame freshProbe() {
        if (!probeRequested) {
            probeRequested = true;
            probe = null;
            UiCapture.probe(minecraft.screen, result -> probe = result.frame());
            return null;
        }
        UiLayoutFrame recorded = probe;
        if (recorded == null && !UiCapture.busy()) {
            // The request was dropped (screen changed): ask again.
            UiCapture.probe(minecraft.screen, result -> probe = result.frame());
        }
        return recorded;
    }

    /**
     * Runs {@code task} once on the integrated server (per step) and returns its answer when it
     * is ready, otherwise {@code null}.
     */
    @SuppressWarnings("unchecked")
    public String serverTask(String description, Function<MinecraftServer, String> task) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            fail(description + ": no integrated server");
        }
        String[] holder = (String[]) stepState.get("serverTask");
        if (holder == null) {
            String[] fresh = new String[1];
            stepState.put("serverTask", fresh);
            server.execute(() -> {
                String answer;
                try {
                    answer = task.apply(server);
                } catch (RuntimeException exception) {
                    answer = "ERROR: " + exception;
                }
                synchronized (fresh) {
                    fresh[0] = answer == null ? "ERROR: no answer" : answer;
                }
            });
            return null;
        }
        synchronized (holder) {
            if (holder[0] == null && stepTicks > UiStep.WAIT_LIMIT * 2) {
                fail(description + ": the server did not answer");
            }
            return holder[0];
        }
    }
}
