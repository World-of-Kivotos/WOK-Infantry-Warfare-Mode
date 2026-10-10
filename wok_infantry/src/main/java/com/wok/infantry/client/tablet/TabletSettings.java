package com.wok.infantry.client.tablet;

import com.wok.infantry.config.InfantryClientConfig;

import java.util.Locale;

/**
 * The animation's settings: the client options {@code ui.tabletAnimation} / {@code ui.tabletSounds}
 * and two system properties (IMPL_PLAN D18, DESIGN 2.7):
 * <ul>
 *   <li>{@value #MODE_PROPERTY}{@code =OFF|QUICK|FULL} overrides the option (uiTest runs with OFF);</li>
 *   <li>{@value #FREEZE_PROPERTY}{@code =<p>} or {@code close:<p>} freezes the animation at p for
 *   manual screenshots.</li>
 * </ul>
 * The parsing is pure; invalid values are ignored.
 */
public final class TabletSettings {
    /** System property that overrides {@code ui.tabletAnimation}. */
    public static final String MODE_PROPERTY = "wok.ui.tabletAnimation";
    /** System property that freezes the animation at one progress. */
    public static final String FREEZE_PROPERTY = "wok.ui.tabletFreeze";

    /** A freeze request: hold {@code p} on an opening, or on a close when {@code close}. */
    public record Freeze(double p, boolean close) {
    }

    private TabletSettings() {
    }

    /** The mode a property value names, or {@code null} (unset or invalid). */
    public static TabletMode parseMode(String value) {
        return TabletMode.parse(value);
    }

    /** {@code 0.87} → opening at 0.87; {@code close:0.66} → closing at 0.66; else {@code null}. */
    public static Freeze parseFreeze(String value) {
        if (value == null) {
            return null;
        }
        String text = value.strip().toLowerCase(Locale.ROOT);
        boolean close = false;
        if (text.startsWith("close:")) {
            close = true;
            text = text.substring("close:".length()).strip();
        } else if (text.startsWith("open:")) {
            text = text.substring("open:".length()).strip();
        }
        if (text.isEmpty()) {
            return null;
        }
        double p;
        try {
            p = Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            return null;
        }
        if (!Double.isFinite(p) || p < 0.0D || p > 1.0D) {
            return null;
        }
        return new Freeze(p, close);
    }

    /** The mode in effect: the system property when it names one, else the client option. */
    public static TabletMode mode() {
        return effectiveMode(System.getProperty(MODE_PROPERTY), InfantryClientConfig.tabletAnimation());
    }

    /** Pure: {@code property} when it names a mode, otherwise {@code configured} (FULL if null). */
    public static TabletMode effectiveMode(String property, TabletMode configured) {
        TabletMode override = parseMode(property);
        if (override != null) {
            return override;
        }
        return configured == null ? TabletMode.FULL : configured;
    }

    /** Whether the twelve sounds play ({@code ui.tabletSounds}). */
    public static boolean sounds() {
        return InfantryClientConfig.tabletSounds();
    }

    /** The freeze requested by {@value #FREEZE_PROPERTY}, or {@code null}. */
    public static Freeze freeze() {
        return parseFreeze(System.getProperty(FREEZE_PROPERTY));
    }
}
