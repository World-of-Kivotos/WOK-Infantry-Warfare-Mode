package com.wok.infantry.uitest;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.ui.probe.UiLayoutReport;
import com.wok.infantry.client.ui.probe.UiSurfaceInfo;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Runs {@link UiCase}s one tier at a time, driven by the harness' client tick:
 * PREPARE (once per case) → TIER → OPEN → STEPS → SETTLE → CAPTURE → CHECK.
 *
 * <p>A failing step, check or timeout fails only that tier; the runner continues with the next
 * tier, so one run reports every problem. The harness fails the whole run afterwards when a
 * blocking result failed ({@link #failures()}).
 */
public final class UiCaseRunner {
    /** Ticks a screen gets to settle after its steps, as the legacy captures. */
    public static final int SETTLE_TICKS = 8;
    private static final int TIER_TIMEOUT_TICKS = 100;
    private static final int CAPTURE_TIMEOUT_TICKS = 100;

    private enum Stage {
        NEXT_CASE,
        PREPARE,
        TIER,
        WAIT_TIER,
        OPEN,
        STEPS,
        SETTLE,
        CAPTURE,
        WAIT_CAPTURE,
        CHECK,
        DONE
    }

    private final List<UiCase> cases;
    private final String tierFilter;
    private final String language;
    private final boolean layoutStrict;
    private final Consumer<String> observer;
    private final List<UiCaseResult> results = new ArrayList<>();
    private Stage stage = Stage.NEXT_CASE;
    private int caseIndex = -1;
    private UiCase current;
    private List<UiTier> tiers = List.of();
    private int tierIndex;
    private Map<String, Object> caseState = new HashMap<>();
    private UiCaseContext context;
    private int stepIndex;
    private int stageTicks;
    private int tierTicks;
    private volatile UiCapture.Result captured;

    public UiCaseRunner(List<UiCase> cases, String tierFilter, String language,
                        boolean layoutStrict, Consumer<String> observer) {
        this.cases = List.copyOf(cases);
        this.tierFilter = tierFilter;
        this.language = language;
        this.layoutStrict = layoutStrict;
        this.observer = observer;
    }

    /** Upper bound of the ticks the cases may take (the harness' global timeout budget). */
    public static int budgetTicks(List<UiCase> cases, String tierFilter) {
        int total = 0;
        for (UiCase uiCase : cases) {
            total += 120 + uiCase.selectedTiers(tierFilter).size()
                    * (uiCase.budgetTicks() + TIER_TIMEOUT_TICKS);
        }
        return total;
    }

    public List<UiCaseResult> results() {
        return List.copyOf(results);
    }

    /** Results that fail the run. */
    public List<UiCaseResult> failures() {
        return results.stream().filter(UiCaseResult::failsRun).toList();
    }

    public boolean done() {
        return stage == Stage.DONE;
    }

    /** Where the runner is, for the harness' progress file. */
    public String position() {
        return current == null ? stage.name() : current.id() + "@"
                + (tierIndex < tiers.size() ? tiers.get(tierIndex).id() : "-") + " " + stage;
    }

    /** Advances by one client tick; returns whether every case has run. */
    public boolean tick(Minecraft minecraft) {
        stageTicks++;
        tierTicks++;
        UiInputDriver.applyHeld(minecraft);
        try {
            switch (stage) {
                case NEXT_CASE -> nextCase(minecraft);
                case PREPARE -> prepare();
                case TIER -> applyTier(minecraft);
                case WAIT_TIER -> waitTier(minecraft);
                case OPEN -> open(minecraft);
                case STEPS -> steps();
                case SETTLE -> {
                    if (stageTicks >= SETTLE_TICKS) {
                        to(Stage.CAPTURE);
                    }
                }
                case CAPTURE -> capture(minecraft);
                case WAIT_CAPTURE -> {
                    if (captured != null) {
                        to(Stage.CHECK);
                    } else if (stageTicks > CAPTURE_TIMEOUT_TICKS) {
                        failTier("no frame of the screen was captured");
                    }
                }
                case CHECK -> check(minecraft);
                case DONE -> {
                    return true;
                }
            }
            if (current != null && stage != Stage.NEXT_CASE && stage != Stage.PREPARE
                    && tierTicks > current.budgetTicks() + TIER_TIMEOUT_TICKS) {
                failTier("timed out in " + stage);
            }
        } catch (UiCaseContext.Failure failure) {
            if (stage == Stage.PREPARE) {
                failCase("prepare: " + failure.getMessage());
            } else {
                failTier(failure.getMessage());
            }
        } catch (Exception | LinkageError exception) {
            WokInfantryMod.LOGGER.error("[UI ACCEPTANCE] Case {} failed", position(), exception);
            if (stage == Stage.PREPARE) {
                failCase("prepare: " + exception);
            } else {
                failTier(String.valueOf(exception));
            }
        }
        return stage == Stage.DONE;
    }

    private void to(Stage next) {
        stage = next;
        stageTicks = 0;
    }

    private void nextCase(Minecraft minecraft) {
        caseIndex++;
        if (caseIndex >= cases.size()) {
            current = null;
            UiInputDriver.release();
            to(Stage.DONE);
            return;
        }
        current = cases.get(caseIndex);
        tiers = current.selectedTiers(tierFilter);
        tierIndex = 0;
        caseState = new HashMap<>();
        if (tiers.isEmpty()) {
            observer.accept("case[" + current.id() + "]=SKIPPED no selected tier");
            return;
        }
        WokInfantryMod.LOGGER.info("[UI ACCEPTANCE] Case {} on {}", current.id(), tiers);
        context = new UiCaseContext(minecraft, current, null, observer, caseState);
        stepIndex = 0;
        context.startStep();
        to(Stage.PREPARE);
    }

    private void prepare() throws Exception {
        if (runSteps(current.prepare())) {
            to(Stage.TIER);
            tierTicks = 0;
        }
    }

    /** Runs {@code steps} from {@link #stepIndex}; returns true when all completed. */
    private boolean runSteps(List<UiStep> steps) throws Exception {
        if (stepIndex >= steps.size()) {
            return true;
        }
        context.tickStep();
        if (steps.get(stepIndex).run(context)) {
            stepIndex++;
            context.startStep();
        }
        return stepIndex >= steps.size();
    }

    private void applyTier(Minecraft minecraft) {
        UiTier tier = tiers.get(tierIndex);
        tierTicks = 0;
        captured = null;
        resize(minecraft, tier);
        to(Stage.WAIT_TIER);
    }

    private static void resize(Minecraft minecraft, UiTier tier) {
        Window window = minecraft.getWindow();
        if (window.getWidth() != tier.windowWidth() || window.getHeight() != tier.windowHeight()) {
            window.setWindowed(tier.windowWidth(), tier.windowHeight());
        }
        minecraft.options.guiScale().set(tier.guiScale());
        minecraft.resizeDisplay();
    }

    private void waitTier(Minecraft minecraft) {
        UiTier tier = tiers.get(tierIndex);
        Window window = minecraft.getWindow();
        boolean ready = window.getWidth() == tier.windowWidth()
                && window.getHeight() == tier.windowHeight()
                && window.getGuiScaledWidth() == tier.guiWidth()
                && window.getGuiScaledHeight() == tier.guiHeight();
        if (ready && stageTicks >= 2) {
            to(Stage.OPEN);
            return;
        }
        if (stageTicks % 10 == 0) {
            resize(minecraft, tier);
        }
        if (stageTicks > TIER_TIMEOUT_TICKS) {
            failTier("window could not reach " + tier.windowWidth() + "x" + tier.windowHeight()
                    + " GUI " + tier.guiScale() + " (got " + window.getWidth() + "x"
                    + window.getHeight() + ", GUI " + window.getGuiScaledWidth() + "x"
                    + window.getGuiScaledHeight() + ")");
        }
    }

    private void open(Minecraft minecraft) throws Exception {
        UiTier tier = tiers.get(tierIndex);
        context = new UiCaseContext(minecraft, current, tier, observer, caseState);
        Screen screen = current.opener().open(context);
        minecraft.setScreen(screen);
        UiInputDriver.holdParked(minecraft);
        stepIndex = 0;
        context.startStep();
        to(Stage.STEPS);
    }

    private void steps() throws Exception {
        if (runSteps(current.steps())) {
            to(Stage.SETTLE);
        }
    }

    private void capture(Minecraft minecraft) {
        captured = null;
        UiCapture.arm(current.fileName(tiers.get(tierIndex)), minecraft.screen,
                result -> captured = result);
        to(Stage.WAIT_CAPTURE);
    }

    private void check(Minecraft minecraft) throws Exception {
        UiCapture.Result result = captured;
        List<UiLayoutReport.Violation> violations = UiLayoutReport.check(result.frame(),
                UiLayoutReport.Options.of(language));
        String failure = null;
        try {
            checkMigrated(result);
            for (UiCase.Check check : current.checks()) {
                check.check(context, result);
            }
        } catch (UiCaseContext.Failure caseFailure) {
            failure = caseFailure.getMessage();
        }
        record(result, violations, failure);
        nextTier();
    }

    /**
     * Built-in checks of a migrated surface (方案 3.2 B3 / 5.1): the captured screen reports the
     * case's preview surface through {@link UiSurfaceInfo}, and on 960×720 at GUI 1 it lays out
     * as 480×360 under the minimum 2x. A screen that is not a WOK surface (a vanilla chat screen
     * over a HUD case) is left to the case's own checks.
     */
    private void checkMigrated(UiCapture.Result result) {
        if (!current.migrated()) {
            return;
        }
        Screen screen = result.screen();
        if (screen != null && !(screen instanceof UiSurfaceInfo)) {
            return;
        }
        if (screen instanceof UiSurfaceInfo info) {
            context.require(current.surfaceId().equals(info.uiSurfaceId()),
                    "migrated surface reports surface id " + info.uiSurfaceId() + " instead of "
                            + current.surfaceId());
        }
        if (tiers.get(tierIndex) == UiTier.T960) {
            context.require(result.layoutWidth() == 480 && result.layoutHeight() == 360,
                    "960x720 at GUI 1 must lay out as 480x360 at 2x, got "
                            + result.layoutWidth() + "x" + result.layoutHeight() + " x"
                            + result.baseScale());
        }
    }

    private void record(UiCapture.Result result, List<UiLayoutReport.Violation> violations,
                        String failure) {
        UiTier tier = tiers.get(tierIndex);
        boolean strict = layoutStrict && current.migrated() && tier.required();
        UiCaseResult caseResult = new UiCaseResult(current.id(), current.surfaceId(),
                current.stateId(), current.group(), tier.id(), tier.previewTier(),
                result == null ? "" : current.fileName(tier), current.migrated(), strict,
                tier.required(),
                result == null || result.screen() == null ? "hud"
                        : result.screen().getClass().getSimpleName(),
                result == null ? 0 : result.layoutWidth(),
                result == null ? 0 : result.layoutHeight(),
                result == null ? 0 : result.baseScale(),
                result == null ? 0.0D : result.guiScale(),
                result == null ? null : result.frame(), violations, failure);
        results.add(caseResult);
        observer.accept(caseResult.summary());
        if (!"PASS".equals(caseResult.status())) {
            WokInfantryMod.LOGGER.warn("[UI ACCEPTANCE] {}", caseResult.summary());
            for (UiLayoutReport.Violation violation : violations) {
                WokInfantryMod.LOGGER.warn("[UI ACCEPTANCE]   {}", violation);
            }
        }
    }

    private void failTier(String message) {
        if (current == null || tierIndex >= tiers.size()) {
            to(Stage.NEXT_CASE);
            return;
        }
        UiCapture.cancel();
        UiCapture.Result result = captured;
        record(result, result == null ? List.of() : UiLayoutReport.check(result.frame(),
                UiLayoutReport.Options.of(language)), message);
        nextTier();
    }

    private void failCase(String message) {
        for (tierIndex = 0; tierIndex < tiers.size(); tierIndex++) {
            record(null, List.of(), message);
        }
        to(Stage.NEXT_CASE);
    }

    private void nextTier() {
        captured = null;
        tierIndex++;
        to(tierIndex < tiers.size() ? Stage.TIER : Stage.NEXT_CASE);
    }
}
