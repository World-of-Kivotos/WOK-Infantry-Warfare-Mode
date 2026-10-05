package com.wok.infantry.client.hud;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.tickets.TicketNetwork;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.UiRect;
import net.minecraft.network.chat.Component;

import java.util.function.ToIntFunction;

/**
 * Content of the top battle strip (preview {@code kit/hud-parts.js} {@code battle}) and its round
 * result notice. Pure, so the wording and geometry are unit-tested.
 *
 * <p>Blue's manpower is always on the left and red's on the right; the viewer's own side is
 * drawn in the HUD friendly blue and the other in the hostile red. Both bars are anchored at the
 * centre line and reach out towards their own number (preview {@code battle}), so a side losing
 * manpower shrinks back towards the centre.
 *
 * <p>While the viewer stands in a WOK步战附属-占点 point (0.5.0-beta.2), the objective tile sits
 * on the centre line between the bars: a small solid plate "B 62%" whose left edge says who is
 * taking the point ({@link #objective}); the bars then end 4px short of it. On non-tight screens
 * the strip takes its 30px wide form and the second row names the point, its status and the time
 * left ({@link #LINE_KEY}, {@link #LINE_TIME_KEY}).
 */
public final class BattleStripModel {
    public static final String VICTORY_KEY = "hud.wok_infantry.round.victory";
    public static final String DEFEAT_KEY = "hud.wok_infantry.round.defeat";
    public static final String DRAW_KEY = "hud.wok_infantry.round.draw";
    public static final String SIDE_VICTORY_KEY = "hud.wok_infantry.round.side_victory";
    /** Second row of the wide strip: "point name · status" and "… · 剩余 m:ss". */
    public static final String LINE_KEY = "hud.wok_infantry.capture.line";
    public static final String LINE_TIME_KEY = "hud.wok_infantry.capture.line_time";
    /** Second row without the point name ("status · 剩余 m:ss"), when the full row is too wide. */
    public static final String STATUS_TIME_KEY = "hud.wok_infantry.capture.status_time";
    /** Objective tile word for a disabled point, in place of its percentage. */
    public static final String DISABLED_KEY = "hud.wok_infantry.capture.disabled";
    /** Number x from the strip's edges; the first text row sits 5px under the top edge. */
    public static final int TEXT_INSET = 4;
    public static final int TEXT_TOP = 5;
    /** Bars keep 2px from their number and 2px from the centre line. */
    public static final int BAR_GAP = 2;
    /** Objective tile: 3px under the strip's top, 11px tall (preview {@code battle}). */
    public static final int TILE_TOP = 3;
    public static final int TILE_HEIGHT = 11;
    /** Name x inside the tile: after the 2px edge and 3px, or after the 9px lock icon. */
    public static final int TILE_NAME_INSET = 5;
    public static final int TILE_ICON_INSET = 4;
    public static final int TILE_NAME_AFTER_ICON = TILE_ICON_INSET + 9 + 2;
    /** Gap between the name and the number, and room after the number. */
    public static final int TILE_GAP = 3;
    public static final int TILE_PAD_RIGHT = 4;
    /** Ticket bars end this far from the tile (themes may frame a plate 2px outside). */
    public static final int TILE_MARGIN = 4;
    /** Shortest ticket bar kept beside the tile; a long point name is shortened instead. */
    public static final int MIN_TRACK = 6;
    /** Second text row of the 30px wide strip. */
    public static final int LINE_TOP = 18;

    private BattleStripModel() {
    }

    /** Round result as the viewer reads it. */
    public enum Outcome {
        NONE,
        OWN_VICTORY,
        OWN_DEFEAT,
        /** Both sides ran out (reachable only through saved data or a configuration change). */
        DRAW,
        /** The viewer's side is unknown: name the winning side. */
        BLUE_VICTORY,
        RED_VICTORY
    }

    /** Numbers, colours and bar shares of one strip. */
    public record Sides(String blueText, String redText, int blueColor, int redColor,
                        float blueRatio, float redRatio) {
    }

    /** Where the strip's numbers and its two centre-anchored tracks go. */
    public record Geometry(int blueTextX, int redTextX, int textY, UiRect blueTrack,
                           UiRect redTrack) {
    }

    /**
     * What the objective tile and the wide strip's second row show for one point.
     *
     * @param name       short point name ("B")
     * @param value      "62%", or "停用" for a disabled point
     * @param line       second-row text: name, status and (while it is being taken) time left
     * @param shortLine  the second row without the name, used when {@code line} is too wide
     * @param edge       the tile's left edge: the side taking it, orange when contested, the
     *                   owner when secured, gray when neutral or disabled
     * @param valueColor the side the control leans to (light at 0), orange when contested
     * @param solid      solid tile plate (a disabled point's tile is translucent)
     * @param lock       the viewer's side may not take the point yet: a lock icon before the name
     */
    public record Objective(Component name, Component value, Component line, Component shortLine,
                            int edge, int nameColor, int valueColor, boolean solid, boolean lock,
                            CaptureObjective.Look look) {
    }

    /** Where the objective tile and its parts go (layout pixels). */
    public record Tile(UiRect plate, int iconX, int iconY, int nameX, int nameRoom, int valueX,
                       int textY) {
    }

    public static Outcome outcome(int blue, int red, Faction viewer) {
        if (blue > 0 && red > 0) {
            return Outcome.NONE;
        }
        if (blue <= 0 && red <= 0) {
            return Outcome.DRAW;
        }
        Faction winner = blue <= 0 ? Faction.RED : Faction.BLUE;
        if (viewer == null) {
            return winner == Faction.BLUE ? Outcome.BLUE_VICTORY : Outcome.RED_VICTORY;
        }
        return viewer == winner ? Outcome.OWN_VICTORY : Outcome.OWN_DEFEAT;
    }

    /** Result notice text, or null while the round runs. */
    public static Component outcomeText(Outcome outcome) {
        if (outcome == null) {
            return null;
        }
        return switch (outcome) {
            case NONE -> null;
            case OWN_VICTORY -> Component.translatable(VICTORY_KEY);
            case OWN_DEFEAT -> Component.translatable(DEFEAT_KEY);
            case DRAW -> Component.translatable(DRAW_KEY);
            case BLUE_VICTORY -> Component.translatable(SIDE_VICTORY_KEY,
                    Component.translatable("faction.wok_infantry." + Faction.BLUE.id()));
            case RED_VICTORY -> Component.translatable(SIDE_VICTORY_KEY,
                    Component.translatable("faction.wok_infantry." + Faction.RED.id()));
        };
    }

    /** Notice tone: own victory green, own defeat red, everything else neutral. */
    public static TacticalHud.Tone outcomeTone(Outcome outcome) {
        if (outcome == Outcome.OWN_VICTORY) {
            return TacticalHud.Tone.SUCCESS;
        }
        return outcome == Outcome.OWN_DEFEAT ? TacticalHud.Tone.DANGER : TacticalHud.Tone.NEUTRAL;
    }

    public static Sides sides(TicketNetwork.Snapshot tickets, Faction viewer) {
        boolean ownRed = viewer == Faction.RED;
        int friendly = TacticalBoardTheme.HUD_FRIENDLY;
        int hostile = TacticalBoardTheme.HUD_HOSTILE;
        return new Sides(String.valueOf(tickets.blue()), String.valueOf(tickets.red()),
                ownRed ? hostile : friendly, ownRed ? friendly : hostile,
                tickets.ratio(tickets.blue()), tickets.ratio(tickets.red()));
    }

    /**
     * Strip geometry: numbers 4px in from each edge on the first text row, the blue track from
     * 2px after the blue number to 2px before the centre line, the red track mirrored.
     */
    public static Geometry geometry(UiRect strip, int blueTextWidth, int redTextWidth) {
        return geometry(strip, blueTextWidth, redTextWidth, null);
    }

    /**
     * {@link #geometry(UiRect, int, int)} with the objective tile on the centre line: the tracks
     * then end {@value #TILE_MARGIN}px short of the tile instead of 2px short of the centre.
     */
    public static Geometry geometry(UiRect strip, int blueTextWidth, int redTextWidth,
                                    UiRect tile) {
        int y = strip.top() + TEXT_TOP;
        int centre = (strip.left() + strip.right()) / 2;
        int blueX = strip.left() + TEXT_INSET;
        int redX = strip.right() - TEXT_INSET - redTextWidth;
        int blueTrackLeft = blueX + blueTextWidth + BAR_GAP;
        int redTrackRight = redX - BAR_GAP;
        int blueEnd = tile == null ? centre - BAR_GAP : tile.left() - TILE_MARGIN;
        int redStart = tile == null ? centre + BAR_GAP : tile.right() + TILE_MARGIN;
        UiRect blueTrack = UiRect.of(blueTrackLeft, y + 2,
                Math.max(blueTrackLeft, blueEnd), y + 5);
        UiRect redTrack = UiRect.of(Math.min(redStart, redTrackRight), y + 2,
                redTrackRight, y + 5);
        return new Geometry(blueX, redX, y, blueTrack, redTrack);
    }

    // ---- objective tile (WOK步战附属-占点) --------------------------------------------------------

    /**
     * What the tile and the second row show for {@code point}, coloured for {@code viewer}
     * (null: blue is friendly, as {@link #sides}).
     */
    public static Objective objective(CaptureObjective point, Faction viewer) {
        CaptureObjective.Look look = point.look();
        Component value = look == CaptureObjective.Look.DISABLED
                ? Component.translatable(DISABLED_KEY)
                : Component.literal(point.percent() + "%");
        boolean timed = point.remainingSeconds() >= 0;
        String time = timed ? CaptureObjective.time(point.remainingSeconds()) : "";
        Component line = timed
                ? Component.translatable(LINE_TIME_KEY, point.name(), point.status(), time)
                : Component.translatable(LINE_KEY, point.name(), point.status());
        Component shortLine = timed
                ? Component.translatable(STATUS_TIME_KEY, point.status(), time)
                : point.status();
        int leading = sideColor(point.leading(), viewer);
        int edge;
        int nameColor = TacticalBoardTheme.LIGHT;
        int valueColor = leading;
        boolean solid = true;
        switch (look) {
            case CAPTURING -> edge = sideColor(point.capturing(), viewer);
            case CONTESTED -> {
                edge = TacticalBoardTheme.ACCENT_B;
                valueColor = TacticalBoardTheme.ACCENT_B;
            }
            case SECURED -> edge = sideColor(point.owner(), viewer);
            case DISABLED -> {
                edge = TacticalBoardTheme.OFFLINE;
                nameColor = TacticalBoardTheme.LIGHT_MUTED;
                valueColor = TacticalBoardTheme.LIGHT_MUTED;
                solid = false;
            }
            default -> edge = TacticalBoardTheme.NEUTRAL_B;
        }
        return new Objective(Component.literal(point.shortName()), value, line, shortLine, edge,
                nameColor, valueColor, solid, point.lockedFor(viewer), look);
    }

    /**
     * The second row that fits {@code room}: the full row, else the row without the point name
     * (the tile already shows its short name); that one is shortened with "…" only when even it
     * does not fit.
     */
    public static Component line(Objective objective, int room, ToIntFunction<Component> width) {
        return width.applyAsInt(objective.line()) <= room ? objective.line()
                : objective.shortLine();
    }

    /** HUD colour of a side for the viewer: own side friendly blue, the other hostile red. */
    public static int sideColor(CaptureObjective.Side side, Faction viewer) {
        if (side == null || side == CaptureObjective.Side.NEUTRAL) {
            return TacticalBoardTheme.LIGHT;
        }
        boolean own = viewer == null ? side == CaptureObjective.Side.BLUE
                : side.faction() == viewer;
        return own ? TacticalBoardTheme.HUD_FRIENDLY : TacticalBoardTheme.HUD_HOSTILE;
    }

    /**
     * Widest objective tile that still leaves both ticket numbers and a {@value #MIN_TRACK}px
     * track on each side of it (the tile stays centred, so the wider number counts twice); the
     * strip's width less its insets when no tickets are shown.
     */
    public static int tileMaxWidth(UiRect strip, int blueTextWidth, int redTextWidth,
                                   boolean tickets) {
        if (!tickets) {
            return Math.max(0, strip.width() - 2 * TEXT_INSET);
        }
        int side = TEXT_INSET + Math.max(blueTextWidth, redTextWidth) + BAR_GAP + MIN_TRACK
                + TILE_MARGIN;
        return Math.max(0, strip.width() - 2 * side);
    }

    /**
     * The objective tile centred on the strip's centre line: {@value #TILE_TOP}px under its top,
     * {@value #TILE_HEIGHT}px tall, "[lock] name number" with the number right-aligned and never
     * cut; the name gets what is left of {@code maxWidth} (0 = left out).
     */
    public static Tile tile(UiRect strip, int nameWidth, int valueWidth, boolean lock,
                            int maxWidth) {
        int lead = lock ? TILE_NAME_AFTER_ICON : TILE_NAME_INSET;
        int fixed = lead + TILE_GAP + Math.max(0, valueWidth) + TILE_PAD_RIGHT;
        int natural = fixed + Math.max(0, nameWidth);
        int width = Math.max(fixed, Math.min(natural, maxWidth));
        int centre = (strip.left() + strip.right()) / 2;
        int left = centre - width / 2;
        int top = strip.top() + TILE_TOP;
        UiRect plate = UiRect.of(left, top, left + width, top + TILE_HEIGHT);
        return new Tile(plate, left + TILE_ICON_INSET, top + 1, left + lead, width - fixed,
                plate.right() - TILE_PAD_RIGHT - Math.max(0, valueWidth),
                strip.top() + TEXT_TOP);
    }
}
