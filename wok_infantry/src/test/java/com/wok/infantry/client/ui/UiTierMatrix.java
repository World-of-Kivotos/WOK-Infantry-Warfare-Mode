package com.wok.infantry.client.ui;

import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.screen.UiScale;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test utility: the logical screen sizes WOK步战 UI is laid out for, with the minimum-2x rule, and
 * overlap/containment assertions for pure layout computations. Lets every screen batch test its
 * layout math against the same tiers without a client.
 *
 * <p>Tiers follow the layout preview ({@code ui-preview/kit/registry.js} {@code PREVIEW_TIERS}) plus
 * the user's own 640×336 (1920×1008 at GUI 3). At GUI 1 a 960×720 window lays out as 480×360.
 */
public final class UiTierMatrix {
    /**
     * One tier: a window size and GUI scale, with the GUI-scaled size and the layout size after
     * the minimum-2x rule.
     */
    public record Tier(String id, int windowWidth, int windowHeight, int guiScale,
                       boolean required) {
        public int guiWidth() {
            return windowWidth / guiScale;
        }

        public int guiHeight() {
            return windowHeight / guiScale;
        }

        /** {@link UiScale} factor with the client option on. */
        public int factor() {
            return UiScale.factorFor(guiScale, guiWidth(), guiHeight(), true);
        }

        public int layoutWidth() {
            return UiScale.layoutSize(guiWidth(), factor());
        }

        public int layoutHeight() {
            return UiScale.layoutSize(guiHeight(), factor());
        }

        @Override
        public String toString() {
            return id + " (" + windowWidth + "x" + windowHeight + " GUI" + guiScale + " -> layout "
                    + layoutWidth() + "x" + layoutHeight() + ")";
        }
    }

    public static final Tier T320 = new Tier("320x240", 960, 720, 3, true);
    public static final Tier T480 = new Tier("480x270", 1920, 1080, 4, false);
    public static final Tier T640 = new Tier("640x360", 1920, 1080, 3, false);
    public static final Tier T640_USER = new Tier("640x336", 1920, 1008, 3, true);
    public static final Tier T960X540 = new Tier("960x540", 1920, 1080, 2, false);
    public static final Tier T960 = new Tier("960x720", 960, 720, 1, true);
    /** Minecraft's default 854×480 window at GUI 1: laid out at 2x as 427×240 (narrow layouts). */
    public static final Tier T427 = new Tier("427x240", 854, 480, 1, false);

    /** The preview's five tiers. */
    public static final List<Tier> PREVIEW = List.of(T320, T480, T640, T960X540, T960);
    /** Preview tiers plus the user's 640×336 and the default window's 427×240. */
    public static final List<Tier> ALL = List.of(T320, T427, T480, T640_USER, T640, T960X540,
            T960);
    /** Tiers every migrated surface must pass (320×240, 960×720, 640×336). */
    public static final List<Tier> REQUIRED = List.of(T320, T960, T640_USER);

    private UiTierMatrix() {
    }

    /** Fails when two of the named regions overlap (touching edges are fine; empty ones skipped). */
    public static void assertNoSolidOverlap(String context, Map<String, UiRect> regions) {
        List<String> clashes = new ArrayList<>();
        List<Map.Entry<String, UiRect>> entries = new ArrayList<>(regions.entrySet());
        for (int first = 0; first < entries.size(); first++) {
            for (int second = first + 1; second < entries.size(); second++) {
                UiRect a = entries.get(first).getValue();
                UiRect b = entries.get(second).getValue();
                if (!a.isEmpty() && !b.isEmpty() && a.intersects(b)) {
                    clashes.add(entries.get(first).getKey() + " " + a + " overlaps "
                            + entries.get(second).getKey() + " " + b);
                }
            }
        }
        if (!clashes.isEmpty()) {
            fail(context + ": " + String.join("; ", clashes));
        }
    }

    /** Fails when a named region leaves {@code outer}. */
    public static void assertInside(String context, UiRect outer, Map<String, UiRect> regions) {
        regions.forEach((name, rect) -> assertTrue(rect.isEmpty() || outer.contains(rect),
                () -> context + ": " + name + " " + rect + " leaves " + outer));
    }
}
