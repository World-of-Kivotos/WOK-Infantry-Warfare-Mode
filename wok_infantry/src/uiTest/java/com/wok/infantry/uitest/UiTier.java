package com.wok.infantry.uitest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Screen sizes the UI acceptance captures, as a window size plus GUI scale. Required tiers fail
 * the run on layout violations of migrated surfaces; report tiers are captured and checked but
 * never fail it (方案 6.1 #16: 320×240、960×720、640×336 必过，480×270 只出报告).
 *
 * <p>640×336 is the user's 1920×1008 window at GUI 3; the automated run reaches the same logical
 * size with 1280×672 at GUI 2, and 480×270 (1080p at GUI 4) with 960×540 at GUI 2, so the window
 * always fits a 1080p monitor. At GUI 1 the WOK minimum 2x lays 960×720 out as 480×360, and the
 * 854×480 window (Minecraft's default size) as 427×240: the narrow layouts below 440 logical
 * pixels and the HUD's boss-bar shift at 427 wide are captured there (report tier).
 */
public enum UiTier {
    T320("320x240", 960, 720, 3, true, "320x240"),
    T960("960x720", 960, 720, 1, true, "960x720"),
    T640("640x336", 1280, 672, 2, true, "640x360"),
    T480("480x270", 960, 540, 2, false, "480x270"),
    T427("427x240", 854, 480, 1, false, "480x270");

    /** Required tiers, in capture order. */
    public static final List<UiTier> REQUIRED = List.of(T320, T960, T640);
    /** Every tier, in capture order. */
    public static final List<UiTier> ALL = List.of(T320, T960, T640, T480, T427);

    private final String id;
    private final int windowWidth;
    private final int windowHeight;
    private final int guiScale;
    private final boolean required;
    private final String previewTier;

    UiTier(String id, int windowWidth, int windowHeight, int guiScale, boolean required,
           String previewTier) {
        this.id = id;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.guiScale = guiScale;
        this.required = required;
        this.previewTier = previewTier;
    }

    /** Logical size id used in screenshot names, e.g. {@code 320x240}. */
    public String id() {
        return id;
    }

    public int windowWidth() {
        return windowWidth;
    }

    public int windowHeight() {
        return windowHeight;
    }

    public int guiScale() {
        return guiScale;
    }

    /** Whether layout violations of migrated surfaces fail the run on this tier. */
    public boolean required() {
        return required;
    }

    /** Closest tier of the layout preview ({@code ui-preview/kit/registry.js}). */
    public String previewTier() {
        return previewTier;
    }

    public int guiWidth() {
        return windowWidth / guiScale;
    }

    public int guiHeight() {
        return windowHeight / guiScale;
    }

    /** Separators of the {@code wok.ui.cases} / {@code wok.ui.tiers} lists. */
    public static final String LIST_SEPARATORS = "[,;+\\s]+";

    /**
     * Tiers selected by {@code wok.ui.tiers} (ids such as {@code 320x240} or {@code 320},
     * separated by {@code , ; +} or spaces), intersected with {@code wanted}; an empty filter
     * keeps {@code wanted}.
     */
    public static List<UiTier> filter(List<UiTier> wanted, String filter) {
        if (filter == null || filter.isBlank()) {
            return wanted;
        }
        List<UiTier> result = new ArrayList<>();
        for (UiTier tier : wanted) {
            for (String token : filter.split(LIST_SEPARATORS)) {
                String trimmed = token.trim().toLowerCase(Locale.ROOT);
                if (!trimmed.isEmpty() && (tier.id.equals(trimmed)
                        || tier.id.startsWith(trimmed + "x"))) {
                    result.add(tier);
                    break;
                }
            }
        }
        return result;
    }
}
