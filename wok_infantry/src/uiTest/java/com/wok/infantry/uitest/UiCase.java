package com.wok.infantry.uitest;

import com.wok.infantry.client.screen.TacticalLivery;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * One UI acceptance case: a surface in one state of the layout preview, captured on a set of
 * tiers. For each tier the runner sizes the window, opens the screen ({@link Opener}), runs the
 * steps (clicks by uiId, keys, waits), lets it settle, captures one frame with its layout probe,
 * checks the layout and runs the case's own checks.
 *
 * <p>{@code migrated} surfaces (rebuilt on the new shared components) fail the run on any layout
 * violation on a required tier; surfaces still waiting for their batch are only reported. The
 * runner also requires a migrated screen to report the case's surface id through
 * {@code UiSurfaceInfo} and to lay out as 480×360 on 960×720 at GUI 1.
 *
 * <p><b>Livery (0.5.0-beta.3).</b> A case that captures a tablet screen names the faction livery
 * it expects ({@link Builder#livery} or, when the case fixes it with
 * {@code TacticalLivery.pinForAcceptance}, {@link Builder#pinLivery}): its id and screenshot get
 * the suffix {@code -academy}, {@code -caesar} or {@code -neutral}, and the runner checks the
 * device the screen drew ({@link UiDeviceChecks}). Cases without a livery (HUD, legacy, the old
 * full-frame screens) keep their names.
 *
 * <p>Screenshots are named {@code wok_ui_<surface>_<state>[-<livery>]_<tier>.png} unless a legacy
 * name is pinned with {@link Builder#file}.
 */
public final class UiCase {
    /** Opens the screen of a tier; {@code null} captures the HUD with no screen open. */
    @FunctionalInterface
    public interface Opener {
        Screen open(UiCaseContext context) throws Exception;
    }

    /** Semantic check of a captured frame; throw (or call {@link UiCaseContext#fail}) to fail. */
    @FunctionalInterface
    public interface Check {
        void check(UiCaseContext context, UiCapture.Result capture) throws Exception;
    }

    /** Ticks a tier may take from opening to the end of its checks before it fails. */
    public static final int DEFAULT_BUDGET_TICKS = 200;
    /** Group of the cases that finish the 14 baseline screenshots of the live flow. */
    public static final String LEGACY_GROUP = "legacy";

    private final String surfaceId;
    private final String stateId;
    private final String group;
    private final List<UiTier> tiers;
    private final boolean migrated;
    private final Opener opener;
    private final List<UiStep> prepare;
    private final List<UiStep> steps;
    private final List<Check> checks;
    private final Map<UiTier, String> fileNames;
    private final int budgetTicks;
    private final boolean hudCapture;
    private final List<Consumer<UiCaseContext>> cleanups;
    private final TacticalLivery.Livery livery;
    private final boolean liveryPinned;
    private final boolean deviceChecks;

    private UiCase(Builder builder) {
        this.surfaceId = builder.surfaceId;
        this.stateId = builder.stateId;
        this.group = builder.group;
        this.tiers = List.copyOf(builder.tiers);
        this.migrated = builder.migrated;
        this.opener = Objects.requireNonNull(builder.opener, "opener");
        this.prepare = List.copyOf(builder.prepare);
        this.steps = List.copyOf(builder.steps);
        this.checks = List.copyOf(builder.checks);
        this.fileNames = Map.copyOf(builder.fileNames);
        this.budgetTicks = builder.budgetTicks;
        this.hudCapture = builder.hudCapture;
        this.cleanups = List.copyOf(builder.cleanups);
        this.livery = builder.livery;
        this.liveryPinned = builder.liveryPinned;
        this.deviceChecks = builder.deviceChecks;
    }

    public static Builder builder(String surfaceId, String stateId) {
        return new Builder(surfaceId, stateId);
    }

    /**
     * {@code <surface>.<state>[-<livery>]}, e.g. {@code kit.confirm-caesar}; cases without a livery
     * keep {@code <surface>.<state>}.
     */
    public String id() {
        return surfaceId + "." + stateId + liverySuffix(livery);
    }

    /**
     * The faction livery the captured tablet screen must be painted in, or {@code null} when the
     * case captures no tablet device (HUD, legacy and old full-frame screens).
     */
    public TacticalLivery.Livery livery() {
        return livery;
    }

    /**
     * Whether the runner pins {@link #livery()} with {@code TacticalLivery.pinForAcceptance} for
     * the case (gallery and formation fixtures); otherwise the livery is the viewer's own, as the
     * live resolver decides it from the battle and catalog caches (squad fixtures).
     */
    public boolean liveryPinned() {
        return liveryPinned;
    }

    /**
     * Whether the runner checks the captured tablet device ({@link UiDeviceChecks}) of a case with
     * a livery. Off for the tablet animation's frozen frames (0.5.0-beta.4): before READ the
     * screen is not drawn at all, and after it the device is drawn into a scaled rectangle.
     */
    public boolean deviceChecks() {
        return deviceChecks;
    }

    /** {@code academy}, {@code caesar}, {@code neutral}, or "" without a livery. */
    public String liveryId() {
        return liveryId(livery);
    }

    /** Lower-case id of {@code livery} ("" for {@code null}). */
    public static String liveryId(TacticalLivery.Livery livery) {
        return livery == null ? "" : livery.name().toLowerCase(Locale.ROOT);
    }

    /** {@code -academy}, {@code -caesar}, {@code -neutral}, or "" without a livery. */
    public static String liverySuffix(TacticalLivery.Livery livery) {
        return livery == null ? "" : "-" + liveryId(livery);
    }

    public String surfaceId() {
        return surfaceId;
    }

    public String stateId() {
        return stateId;
    }

    /**
     * {@code <state>[-<livery>]}, e.g. {@code squads-caesar}: the key observation lines use, so
     * the same state in two liveries never shares a line.
     */
    public String stateKey() {
        return stateId + liverySuffix(livery);
    }

    /** Selection group for {@code -PuiCases}, e.g. {@code kit} or {@code legacy}. */
    public String group() {
        return group;
    }

    public List<UiTier> tiers() {
        return tiers;
    }

    /**
     * Tiers this case runs on under {@code wok.ui.tiers}. Legacy cases keep their pinned tiers:
     * the live flow always waits for all 14 baseline screenshots, whatever the tier filter.
     */
    public List<UiTier> selectedTiers(String tierFilter) {
        return LEGACY_GROUP.equals(group) ? tiers : UiTier.filter(tiers, tierFilter);
    }

    public boolean migrated() {
        return migrated;
    }

    public Opener opener() {
        return opener;
    }

    public List<UiStep> prepare() {
        return prepare;
    }

    public List<UiStep> steps() {
        return steps;
    }

    public List<Check> checks() {
        return checks;
    }

    public int budgetTicks() {
        return budgetTicks;
    }

    /**
     * Whether the capture records the HUD (from {@code RenderGuiEvent.Pre}) even though a screen
     * such as the vanilla chat is open; the frame then ends after that screen.
     */
    public boolean hudCapture() {
        return hudCapture;
    }

    /** Run once after the case's last tier (or its failed preparation), to restore client state. */
    public List<Consumer<UiCaseContext>> cleanups() {
        return cleanups;
    }

    /**
     * Screenshot of {@code tier}: a pinned legacy name or
     * {@code wok_ui_<surface>_<state>[-<livery>]_<tier>.png}.
     */
    public String fileName(UiTier tier) {
        String pinned = fileNames.get(tier);
        return pinned != null ? pinned
                : "wok_ui_" + surfaceId + "_" + stateId + liverySuffix(livery) + "_" + tier.id()
                + ".png";
    }

    /**
     * Whether {@code -PuiCases} selects this case (empty filter: every case): its group, surface,
     * id, {@code <surface>.<state>} (every livery of a state) or its livery id ({@code caesar}).
     */
    public boolean selectedBy(String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        String state = surfaceId + "." + stateId;
        for (String token : filter.split(UiTier.LIST_SEPARATORS)) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty() && (trimmed.equals(group) || id().equals(trimmed)
                    || state.equals(trimmed) || id().startsWith(trimmed + ".")
                    || surfaceId.equals(trimmed)
                    || livery != null && liveryId().equals(trimmed))) {
                return true;
            }
        }
        return false;
    }

    public static final class Builder {
        private final String surfaceId;
        private final String stateId;
        private String group;
        private List<UiTier> tiers = UiTier.ALL;
        private boolean migrated;
        private Opener opener;
        private final List<UiStep> prepare = new ArrayList<>();
        private final List<UiStep> steps = new ArrayList<>();
        private final List<Check> checks = new ArrayList<>();
        private final Map<UiTier, String> fileNames = new EnumMap<>(UiTier.class);
        private int budgetTicks = DEFAULT_BUDGET_TICKS;
        private boolean hudCapture;
        private final List<Consumer<UiCaseContext>> cleanups = new ArrayList<>();
        private TacticalLivery.Livery livery;
        private boolean liveryPinned;
        private boolean deviceChecks = true;

        private Builder(String surfaceId, String stateId) {
            this.surfaceId = Objects.requireNonNull(surfaceId, "surfaceId");
            this.stateId = Objects.requireNonNull(stateId, "stateId");
            this.group = surfaceId;
        }

        public Builder group(String value) {
            this.group = value;
            return this;
        }

        public Builder tiers(List<UiTier> value) {
            this.tiers = List.copyOf(value);
            return this;
        }

        public Builder tiers(UiTier... value) {
            return tiers(List.of(value));
        }

        /** Rebuilt on the shared components: layout violations fail required tiers. */
        public Builder migrated(boolean value) {
            this.migrated = value;
            return this;
        }

        public Builder open(Opener value) {
            this.opener = value;
            return this;
        }

        /** Runs once before the first tier (server fixtures, permissions). */
        public Builder prepare(UiStep... value) {
            prepare.addAll(List.of(value));
            return this;
        }

        /** Runs on every tier after the screen opened, before the capture. */
        public Builder steps(UiStep... value) {
            steps.addAll(List.of(value));
            return this;
        }

        public Builder check(Check value) {
            checks.add(value);
            return this;
        }

        /** Pins a screenshot name (the 14 legacy captures keep theirs). */
        public Builder file(UiTier tier, String fileName) {
            fileNames.put(tier, fileName);
            return this;
        }

        public Builder budget(int ticks) {
            this.budgetTicks = Math.max(40, ticks);
            return this;
        }

        /** Captures the HUD under the opened screen (the chat state of the HUD cases). */
        public Builder hudCapture(boolean value) {
            this.hudCapture = value;
            return this;
        }

        /** Restores client state after the case (fixtures a case installed outside a screen). */
        public Builder cleanup(Consumer<UiCaseContext> value) {
            cleanups.add(value);
            return this;
        }

        /**
         * The livery the tablet screen resolves for itself from the fixture (battle side, or the
         * catalog faction): named and checked, never pinned.
         */
        public Builder livery(TacticalLivery.Livery value) {
            this.livery = value;
            this.liveryPinned = false;
            return this;
        }

        /** The livery the runner pins for the whole case ({@code pinForAcceptance}), and checks. */
        public Builder pinLivery(TacticalLivery.Livery value) {
            this.livery = value;
            this.liveryPinned = value != null;
            return this;
        }

        /** Whether the device checks run on a case with a livery (default true). */
        public Builder deviceChecks(boolean value) {
            this.deviceChecks = value;
            return this;
        }

        public UiCase build() {
            return new UiCase(this);
        }
    }
}
