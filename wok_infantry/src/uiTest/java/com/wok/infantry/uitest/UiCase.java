package com.wok.infantry.uitest;

import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
 * Screenshots are named {@code wok_ui_<surface>_<state>_<tier>.png} unless a legacy name is pinned
 * with {@link Builder#file}.
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
    }

    public static Builder builder(String surfaceId, String stateId) {
        return new Builder(surfaceId, stateId);
    }

    /** {@code <surface>.<state>}, e.g. {@code kit.confirm}. */
    public String id() {
        return surfaceId + "." + stateId;
    }

    public String surfaceId() {
        return surfaceId;
    }

    public String stateId() {
        return stateId;
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

    /** Screenshot of {@code tier}: a pinned legacy name or {@code wok_ui_<surface>_<state>_<tier>.png}. */
    public String fileName(UiTier tier) {
        String pinned = fileNames.get(tier);
        return pinned != null ? pinned
                : "wok_ui_" + surfaceId + "_" + stateId + "_" + tier.id() + ".png";
    }

    /** Whether {@code -PuiCases} selects this case (empty filter: every case). */
    public boolean selectedBy(String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        for (String token : filter.split(UiTier.LIST_SEPARATORS)) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty() && (trimmed.equals(group) || id().equals(trimmed)
                    || id().startsWith(trimmed + ".") || surfaceId.equals(trimmed))) {
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

        public UiCase build() {
            return new UiCase(this);
        }
    }
}
