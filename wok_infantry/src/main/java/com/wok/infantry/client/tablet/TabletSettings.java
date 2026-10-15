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

    /**
     * A freeze request: hold {@code p} on an opening, or on a close when {@code close}; optionally
     * with the held item ({@code hand}, {@code null} = the real one) and the path ({@code path}
     * B2D forces scheme B, {@code null} = the normal choice).
     */
    public record Freeze(double p, boolean close, TabletHand hand, TabletPath path) {
        public Freeze(double p, boolean close) {
            this(p, close, null, null);
        }
    }

    private TabletSettings() {
    }

    /** The mode a property value names, or {@code null} (unset or invalid). */
    public static TabletMode parseMode(String value) {
        return TabletMode.parse(value);
    }

    /**
     * {@code 0.87} → opening at 0.87; {@code close:0.66} → closing at 0.66; options may follow
     * after commas: {@code hand=gun|item|empty} and {@code path=a|b} (for example
     * {@code close:0.66,hand=empty,path=b}). Anything invalid gives {@code null}.
     */
    public static Freeze parseFreeze(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(",");
        Freeze base = parseFreezePoint(parts[0]);
        if (base == null) {
            return null;
        }
        TabletHand hand = null;
        TabletPath path = null;
        for (int index = 1; index < parts.length; index++) {
            String option = parts[index].strip().toLowerCase(Locale.ROOT);
            int eq = option.indexOf('=');
            if (eq <= 0) {
                return null;
            }
            String key = option.substring(0, eq).strip();
            String val = option.substring(eq + 1).strip();
            switch (key) {
                case "hand" -> {
                    if (!val.equals("gun") && !val.equals("item") && !val.equals("empty")) {
                        return null;
                    }
                    hand = TabletHand.byPreviewName(val);
                }
                case "path" -> {
                    if (val.equals("a")) {
                        path = TabletPath.A3D;
                    } else if (val.equals("b")) {
                        path = TabletPath.B2D;
                    } else {
                        return null;
                    }
                }
                default -> {
                    return null;
                }
            }
        }
        return new Freeze(base.p(), base.close(), hand, path);
    }

    private static Freeze parseFreezePoint(String value) {
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
