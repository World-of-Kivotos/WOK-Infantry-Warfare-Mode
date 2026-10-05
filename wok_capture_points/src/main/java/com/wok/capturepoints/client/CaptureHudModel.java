package com.wok.capturepoints.client;

import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * What the capture HUD says about the point the viewer stands in, read from its synchronized
 * view: the state, the progress number, the time left and the status text. Pure (no client
 * class), shared by the thin strip ({@link CaptureHudOverlay}) and the read-only
 * {@code CaptureHudApi} that WOK步战核心 reads to show the point in its battle strip.
 *
 * <p>Control runs from −1 (red owns the point) through 0 (neutral) to 1 (blue owns it). The
 * progress number is {@code |control|} in percent and belongs to the side the control leans to,
 * so a point being taken back first counts the holder's share down to 0, then the taker's up.
 */
public final class CaptureHudModel {
    /** Status keys (zh_cn / en_us pairs in this add-on's language files). */
    public static final String DISABLED_KEY = "hud.wok_capture_points.disabled";
    public static final String CONTESTED_KEY = "hud.wok_capture_points.contested";
    public static final String LOCKED_KEY = "hud.wok_capture_points.locked";
    public static final String CAPTURING_KEY = "hud.wok_capture_points.capturing";
    public static final String CAPTURING_PLAIN_KEY = "hud.wok_capture_points.capturing_plain";
    public static final String SECURED_KEY = "hud.wok_capture_points.secured";
    public static final String NEUTRAL_KEY = "hud.wok_capture_points.neutral";
    public static final String REMAINING_KEY = "hud.wok_capture_points.remaining";
    public static final String TEAM_KEY_PREFIX = "team.wok_capture_points.";

    /** Control at or beyond this magnitude means the point is owned (as {@code CaptureMath}). */
    static final double OWNED = 0.999999D;

    /** HUD colours, the same values as WOK步战核心's HUD (copied: no hard dependency). */
    public static final int BLUE = 0xFF6FB1E6;
    public static final int RED = 0xFFE8695D;
    public static final int ORANGE = 0xFFF0A63A;
    public static final int LIGHT = 0xFFEEF3F0;
    public static final int MUTED = 0xFF9DAAA8;
    public static final int NEUTRAL = 0xFFB6C2C0;
    public static final int OFFLINE = 0xFF7D898A;

    private CaptureHudModel() {
    }

    /** State of a point as the HUD names it; earlier constants win. */
    public enum State {
        /** The point is switched off by an administrator. */
        DISABLED,
        /** Both sides stand in it and neither is taking it: progress is frozen. */
        CONTESTED,
        /** Someone stands in it whose side may not take it yet (sequential capture order). */
        LOCKED,
        /** One side is taking it (from the other side or from neutral). */
        CAPTURING,
        /** Owned, and nobody is taking it from the owner (its own players may stand in it). */
        SECURED,
        /** Not owned and nobody is taking it. */
        NEUTRAL;

        /** Lower-case id, e.g. {@code capturing}. */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static State state(CapturePointView point) {
        if (!point.enabled()) {
            return State.DISABLED;
        }
        CaptureTeam active = active(point);
        if (point.bluePlayers() > 0 && point.redPlayers() > 0 && active == CaptureTeam.NEUTRAL) {
            return State.CONTESTED;
        }
        if (active == CaptureTeam.BLUE && !point.blueAllowed()
                || active == CaptureTeam.RED && !point.redAllowed()
                || active == CaptureTeam.NEUTRAL
                && (point.bluePlayers() > 0 && !point.blueAllowed()
                || point.redPlayers() > 0 && !point.redAllowed())) {
            return State.LOCKED;
        }
        // The owner's own players keep "taking" a point they already own (CaptureMath): that is
        // holding it, not capturing it (alpha.3 called it "蓝方正在占领").
        if (active != CaptureTeam.NEUTRAL && active != owner(point)) {
            return State.CAPTURING;
        }
        return owner(point) != CaptureTeam.NEUTRAL ? State.SECURED : State.NEUTRAL;
    }

    /** Progress number 0–100: {@code |control|}, never 100 before the point is really owned. */
    public static int percent(double control) {
        double magnitude = magnitude(control);
        int percent = (int) Math.round(magnitude * 100.0D);
        return percent >= 100 && magnitude < OWNED ? 99 : Math.min(100, percent);
    }

    /** Side the control leans to, or neutral while the progress number reads 0. */
    public static CaptureTeam leading(double control) {
        if (percent(control) == 0) {
            return CaptureTeam.NEUTRAL;
        }
        return control > 0.0D ? CaptureTeam.BLUE : CaptureTeam.RED;
    }

    /**
     * Seconds until the side taking the point owns it at the current speed, or −1 when nobody
     * is taking it (also while it is disabled or already owned by the taker). A full swing from
     * one owner to the other ({@code 2.0} of control) takes {@code captureSeconds / speed}.
     */
    public static int remainingSeconds(CapturePointView point) {
        CaptureTeam active = active(point);
        if (!point.enabled() || active == CaptureTeam.NEUTRAL || point.speedMultiplier() <= 0
                || point.captureSeconds() <= 0) {
            return -1;
        }
        double control = Double.isFinite(point.control())
                ? Math.max(-1.0D, Math.min(1.0D, point.control())) : 0.0D;
        double distance = Math.abs(active.direction() - control);
        if (distance <= 1.0D - OWNED) {
            return -1;
        }
        return (int) Math.ceil(distance * point.captureSeconds()
                / (2.0D * point.speedMultiplier()) - 1.0E-9D);
    }

    /** {@code m:ss}, or {@code h:mm:ss} from an hour on; negative values read 0:00. */
    public static String time(int seconds) {
        int safe = Math.max(0, seconds);
        int hours = safe / 3600;
        int minutes = safe / 60 % 60;
        int rest = safe % 60;
        return hours > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest)
                : String.format(Locale.ROOT, "%d:%02d", minutes, rest);
    }

    /**
     * Short name for tight places such as WOK步战核心's objective tile: an id of up to three
     * characters in upper case ({@code b} → {@code B}), otherwise the display name.
     */
    public static String shortName(CapturePointView point) {
        String id = point.id() == null ? "" : point.id().trim();
        if (!id.isEmpty() && id.length() <= 3) {
            return id.toUpperCase(Locale.ROOT);
        }
        String name = point.displayName() == null ? "" : point.displayName().trim();
        return name.isEmpty() ? id : name;
    }

    /** Status text: the state in words (the taking side and, above 1, its speed). */
    public static Component status(CapturePointView point) {
        return switch (state(point)) {
            case DISABLED -> Component.translatable(DISABLED_KEY);
            case CONTESTED -> Component.translatable(CONTESTED_KEY);
            case LOCKED -> Component.translatable(LOCKED_KEY);
            case CAPTURING -> point.speedMultiplier() > 1
                    ? Component.translatable(CAPTURING_KEY, team(active(point)),
                    point.speedMultiplier())
                    : Component.translatable(CAPTURING_PLAIN_KEY, team(active(point)));
            case SECURED -> Component.translatable(SECURED_KEY, team(owner(point)));
            case NEUTRAL -> Component.translatable(NEUTRAL_KEY);
        };
    }

    /** {@link #status} followed by the time left while a side is taking the point. */
    public static Component statusWithTime(CapturePointView point) {
        int remaining = remainingSeconds(point);
        Component status = status(point);
        return remaining < 0 ? status
                : Component.translatable(REMAINING_KEY, status, time(remaining));
    }

    /** Team name ("蓝方"). */
    public static Component team(CaptureTeam team) {
        return Component.translatable(TEAM_KEY_PREFIX + team.id());
    }

    /** Colour of a side: blue, red, or the light text colour for neutral. */
    public static int teamColor(CaptureTeam team) {
        return switch (team) {
            case BLUE -> BLUE;
            case RED -> RED;
            case NEUTRAL -> LIGHT;
        };
    }

    /**
     * Accent of the thin strip's top edge: the taking side, orange while contested, the owner
     * while secured, gray otherwise.
     */
    public static int accent(CapturePointView point) {
        return switch (state(point)) {
            case CAPTURING -> teamColor(active(point));
            case CONTESTED -> ORANGE;
            case SECURED -> teamColor(owner(point));
            case LOCKED, NEUTRAL -> NEUTRAL;
            case DISABLED -> OFFLINE;
        };
    }

    /** Colour of the status text: the taking side, orange, the owner, or muted. */
    public static int statusColor(CapturePointView point) {
        return switch (state(point)) {
            case CAPTURING -> teamColor(active(point));
            case CONTESTED -> ORANGE;
            case SECURED -> teamColor(owner(point));
            case LOCKED, NEUTRAL, DISABLED -> MUTED;
        };
    }

    private static CaptureTeam active(CapturePointView point) {
        return point.activeTeam() == null ? CaptureTeam.NEUTRAL : point.activeTeam();
    }

    private static CaptureTeam owner(CapturePointView point) {
        return point.owner() == null ? CaptureTeam.NEUTRAL : point.owner();
    }

    private static double magnitude(double control) {
        return Double.isFinite(control) ? Math.min(1.0D, Math.abs(control)) : 0.0D;
    }
}
