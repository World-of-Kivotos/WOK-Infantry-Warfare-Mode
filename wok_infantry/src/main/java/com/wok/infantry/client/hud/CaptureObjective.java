package com.wok.infantry.client.hud;

import com.wok.infantry.battle.Faction;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;

/**
 * The WOK步战附属-占点 point the viewer stands in, as the add-on (0.1.0-alpha.4+) describes it in
 * {@code com.wok.capturepoints.api.CaptureHudApi.currentPoint()}: a map of JDK values plus one
 * status {@link Component}. Parsed here without linking against the add-on; {@link #fromMap}
 * rejects a map that breaks the contract, and the bridge then stops reading it.
 *
 * <p>Control runs from −1 (red owns the point) through 0 to 1 (blue owns it); {@link #percent}
 * is {@code |control|} in percent and belongs to the {@link #leading} side.
 *
 * @param state            the add-on's own state id ({@code capturing}, {@code locked}, …)
 * @param status           the state in words, translated by the add-on
 * @param remainingSeconds seconds until the taking side owns the point, or −1
 */
public record CaptureObjective(String id, String name, String shortName, double control,
                               int percent, Side leading, Side owner, Side capturing,
                               int bluePlayers, int redPlayers, boolean enabled,
                               boolean blueAllowed, boolean redAllowed, int speed, String state,
                               Component status, int remainingSeconds) {
    /** Map contract version this core understands ({@code CaptureHudApi.VERSION}). */
    public static final int API_VERSION = 1;
    /** Longest name kept from the map (display names are capped at 96 by the add-on). */
    static final int MAX_NAME_LENGTH = 96;

    /** A side of the point, as the add-on names it. */
    public enum Side {
        NEUTRAL,
        BLUE,
        RED;

        /** {@code neutral|blue|red}; anything else is null. */
        public static Side byId(String id) {
            if (id == null) {
                return null;
            }
            return switch (id.trim().toLowerCase(Locale.ROOT)) {
                case "neutral" -> NEUTRAL;
                case "blue" -> BLUE;
                case "red" -> RED;
                default -> null;
            };
        }

        /** The core faction of this side, or null for neutral. */
        public Faction faction() {
            return this == BLUE ? Faction.BLUE : this == RED ? Faction.RED : null;
        }
    }

    /** How the battle strip's objective tile looks, decided by the point's state. */
    public enum Look {
        /** One side is taking the point: the tile's edge in that side's colour. */
        CAPTURING,
        /** Both sides stand in it and neither takes it: orange, progress frozen. */
        CONTESTED,
        /** Owned and nobody takes it: the owner's colour. */
        SECURED,
        /** Not owned and nobody takes it. */
        NEUTRAL,
        /** Switched off by an administrator: gray, translucent, "停用" instead of a number. */
        DISABLED
    }

    public CaptureObjective {
        id = clean(id, "");
        name = clean(name, id);
        shortName = clean(shortName, name);
        control = Double.isFinite(control) ? Math.max(-1.0D, Math.min(1.0D, control)) : 0.0D;
        percent = Math.max(0, Math.min(100, percent));
        leading = leading == null ? Side.NEUTRAL : leading;
        owner = owner == null ? Side.NEUTRAL : owner;
        capturing = capturing == null ? Side.NEUTRAL : capturing;
        bluePlayers = Math.max(0, bluePlayers);
        redPlayers = Math.max(0, redPlayers);
        speed = Math.max(0, speed);
        state = state == null ? "" : state.trim().toLowerCase(Locale.ROOT);
        status = status == null ? Component.empty() : status;
        remainingSeconds = Math.max(-1, remainingSeconds);
    }

    /**
     * Reads the add-on's map. Required: {@code id}, {@code control}, {@code owner},
     * {@code capturing}, {@code enabled}; a known {@code version}. The rest falls back to
     * neutral defaults (the percent and leading side are derived from the control).
     *
     * @throws IllegalArgumentException when a required value is missing or of the wrong type
     */
    public static CaptureObjective fromMap(Map<?, ?> map) {
        if (map == null) {
            throw new IllegalArgumentException("no capture point map");
        }
        Object version = map.get("version");
        if (version != null && !(version instanceof Number number
                && number.intValue() == API_VERSION)) {
            throw new IllegalArgumentException("unsupported capture HUD API version " + version);
        }
        String id = required(map, "id", String.class);
        double control = required(map, "control", Number.class).doubleValue();
        Side owner = side(map, "owner", true);
        Side capturing = side(map, "capturing", true);
        boolean enabled = required(map, "enabled", Boolean.class);
        Number percentValue = optional(map, "percent", Number.class);
        int percent = percentValue == null ? percentOf(control) : percentValue.intValue();
        Side leading = side(map, "leading", false);
        if (leading == null) {
            leading = percent == 0 ? Side.NEUTRAL : control > 0.0D ? Side.BLUE : Side.RED;
        }
        return new CaptureObjective(id, optional(map, "name", String.class),
                optional(map, "shortName", String.class), control, percent, leading, owner,
                capturing, intValue(map, "bluePlayers", 0), intValue(map, "redPlayers", 0),
                enabled, booleanValue(map, "blueAllowed"), booleanValue(map, "redAllowed"),
                intValue(map, "speed", 0), optional(map, "state", String.class),
                optional(map, "status", Component.class), intValue(map, "remainingSeconds", -1));
    }

    /** {@code |control|} in percent, never 100 before the point is really owned. */
    public static int percentOf(double control) {
        double magnitude = Double.isFinite(control) ? Math.min(1.0D, Math.abs(control)) : 0.0D;
        int percent = (int) Math.round(magnitude * 100.0D);
        return percent >= 100 && magnitude < 0.999999D ? 99 : Math.min(100, percent);
    }

    /**
     * The tile's look; earlier states win (disabled, contested, capturing, secured). The owner's
     * own players standing in an owned point keep "taking" it in the add-on's numbers; that is
     * holding it, so it looks secured.
     */
    public Look look() {
        if (!enabled) {
            return Look.DISABLED;
        }
        if (bluePlayers > 0 && redPlayers > 0 && capturing == Side.NEUTRAL) {
            return Look.CONTESTED;
        }
        if (capturing != Side.NEUTRAL && capturing != owner) {
            return Look.CAPTURING;
        }
        return owner != Side.NEUTRAL ? Look.SECURED : Look.NEUTRAL;
    }

    /**
     * Whether the viewer's side may not take this point yet (sequential capture order). Without
     * a known side, whether the add-on itself calls the point locked. Never for a disabled point.
     */
    public boolean lockedFor(Faction viewer) {
        if (!enabled) {
            return false;
        }
        if (viewer == null) {
            return "locked".equals(state);
        }
        return viewer == Faction.BLUE ? !blueAllowed : !redAllowed;
    }

    /** {@code m:ss}, or {@code h:mm:ss} from an hour on. */
    public static String time(int seconds) {
        int safe = Math.max(0, seconds);
        int hours = safe / 3600;
        int minutes = safe / 60 % 60;
        int rest = safe % 60;
        return hours > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest)
                : String.format(Locale.ROOT, "%d:%02d", minutes, rest);
    }

    private static <T> T required(Map<?, ?> map, String key, Class<T> type) {
        Object value = map.get(key);
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("capture point map: '" + key + "' is "
                    + (value == null ? "missing" : value.getClass().getName()));
        }
        return type.cast(value);
    }

    private static <T> T optional(Map<?, ?> map, String key, Class<T> type) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("capture point map: '" + key + "' is "
                    + value.getClass().getName());
        }
        return type.cast(value);
    }

    private static Side side(Map<?, ?> map, String key, boolean required) {
        String value = required ? required(map, key, String.class)
                : optional(map, key, String.class);
        if (value == null) {
            return null;
        }
        Side side = Side.byId(value);
        if (side == null) {
            throw new IllegalArgumentException("capture point map: '" + key + "' is " + value);
        }
        return side;
    }

    private static int intValue(Map<?, ?> map, String key, int fallback) {
        Number value = optional(map, key, Number.class);
        return value == null ? fallback : value.intValue();
    }

    private static boolean booleanValue(Map<?, ?> map, String key) {
        Boolean value = optional(map, key, Boolean.class);
        return value == null || value;
    }

    private static String clean(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
    }
}
