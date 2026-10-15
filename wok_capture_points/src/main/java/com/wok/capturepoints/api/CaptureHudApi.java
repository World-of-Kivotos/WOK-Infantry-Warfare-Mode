package com.wok.capturepoints.api;

import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import com.wok.capturepoints.client.CaptureHudModel;
import com.wok.capturepoints.client.CaptureHudOverlay;
import com.wok.capturepoints.client.ClientCaptureState;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Client-only, read-only HUD API of WOK步战附属-占点 (since 0.1.0-alpha.4), meant to be called by
 * reflection: WOK步战核心 0.5.0-beta.2+ reads {@link #currentPoint()} and shows the point the
 * viewer stands in as a tile in its battle strip; while it does
 * ({@code InfantryHudApi.rendersCapturePoints()} is true) this add-on draws nothing itself.
 * Every value is a JDK type except {@link #STATUS}, a {@code Component}; nothing here needs a
 * caller to link against this add-on's classes. Call from the client render thread only.
 */
public final class CaptureHudApi {
    /** Version of the map's contract; raised only when a key changes meaning or goes away. */
    public static final int VERSION = 1;

    /** {@code Integer}: {@link #VERSION}. */
    public static final String API_VERSION = "version";
    /** {@code String}: point id (lower case). */
    public static final String ID = "id";
    /** {@code String}: display name set by the administrator. */
    public static final String NAME = "name";
    /** {@code String}: short name for tight places (an id of up to 3 characters in upper case). */
    public static final String SHORT_NAME = "shortName";
    /** {@code Double}: control, −1 (red owns) … 0 (neutral) … 1 (blue owns). */
    public static final String CONTROL = "control";
    /** {@code Integer}: progress number 0–100, {@code |control|}, 100 only when owned. */
    public static final String PERCENT = "percent";
    /** {@code String}: side the control leans to, {@code neutral|blue|red}. */
    public static final String LEADING = "leading";
    /** {@code String}: owner, {@code neutral|blue|red}. */
    public static final String OWNER = "owner";
    /** {@code String}: side taking the point now, {@code neutral|blue|red}. */
    public static final String CAPTURING = "capturing";
    /** {@code Integer}: counted players of each side standing in the point. */
    public static final String BLUE_PLAYERS = "bluePlayers";
    public static final String RED_PLAYERS = "redPlayers";
    /** {@code Boolean}: the point is enabled. */
    public static final String ENABLED = "enabled";
    /** {@code Boolean}: each side may take the point now (sequential capture order). */
    public static final String BLUE_ALLOWED = "blueAllowed";
    public static final String RED_ALLOWED = "redAllowed";
    /** {@code Integer}: capture speed multiplier (0 while nobody takes it). */
    public static final String SPEED = "speed";
    /** {@code Integer}: seconds a full swing from one owner to the other takes at speed 1. */
    public static final String CAPTURE_SECONDS = "captureSeconds";
    /** {@code Integer}: seconds until the taking side owns it, or −1. */
    public static final String REMAINING_SECONDS = "remainingSeconds";
    /**
     * {@code String}: {@code disabled|contested|locked|capturing|secured|neutral}
     * ({@link CaptureHudModel.State}).
     */
    public static final String STATE = "state";
    /** {@code Component}: the state in words, translated by this add-on's language files. */
    public static final String STATUS = "status";

    private static List<CapturePointView> cachedPoints;
    private static String cachedInside;
    private static Map<String, Object> cached;

    private CaptureHudApi() {
    }

    /**
     * The point the local player stands in as an unmodifiable map (keys above), or null when
     * there is none. The same map instance is returned until the next synchronization changes
     * the points or the point the player stands in.
     */
    public static Map<String, Object> currentPoint() {
        List<CapturePointView> points = ClientCaptureState.points();
        String inside = ClientCaptureState.insidePointId();
        if (points == cachedPoints && Objects.equals(inside, cachedInside)) {
            return cached;
        }
        CapturePointView point = null;
        if (inside != null) {
            for (CapturePointView candidate : points) {
                if (candidate.id().equals(inside)) {
                    point = candidate;
                    break;
                }
            }
        }
        cached = point == null ? null : describe(point);
        cachedPoints = points;
        cachedInside = inside;
        return cached;
    }

    /** The map {@link #currentPoint} returns for {@code point}. */
    public static Map<String, Object> describe(CapturePointView point) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(API_VERSION, VERSION);
        map.put(ID, point.id());
        map.put(NAME, point.displayName());
        map.put(SHORT_NAME, CaptureHudModel.shortName(point));
        map.put(CONTROL, point.control());
        map.put(PERCENT, CaptureHudModel.percent(point.control()));
        map.put(LEADING, CaptureHudModel.leading(point.control()).id());
        map.put(OWNER, team(point.owner()));
        map.put(CAPTURING, team(point.activeTeam()));
        map.put(BLUE_PLAYERS, point.bluePlayers());
        map.put(RED_PLAYERS, point.redPlayers());
        map.put(ENABLED, point.enabled());
        map.put(BLUE_ALLOWED, point.blueAllowed());
        map.put(RED_ALLOWED, point.redAllowed());
        map.put(SPEED, point.speedMultiplier());
        map.put(CAPTURE_SECONDS, point.captureSeconds());
        map.put(REMAINING_SECONDS, CaptureHudModel.remainingSeconds(point));
        map.put(STATE, CaptureHudModel.state(point).id());
        map.put(STATUS, CaptureHudModel.status(point));
        return Collections.unmodifiableMap(map);
    }

    /**
     * This add-on's own strip as {@code {left, top, width, height}} GUI pixels while it is drawn,
     * otherwise null — also while WOK步战核心 shows the point in its battle strip. The core
     * (0.4.0-beta.1+) keeps its HUD and the vanilla boss bars clear of this rectangle.
     */
    public static int[] panelRect(int guiWidth, int guiHeight) {
        return CaptureHudOverlay.panelRect(guiWidth, guiHeight);
    }

    private static String team(CaptureTeam team) {
        return (team == null ? CaptureTeam.NEUTRAL : team).id();
    }
}
