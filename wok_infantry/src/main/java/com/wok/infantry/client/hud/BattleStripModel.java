package com.wok.infantry.client.hud;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.tickets.TicketNetwork;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.UiRect;
import net.minecraft.network.chat.Component;

/**
 * Content of the top battle strip (preview {@code kit/hud-parts.js} {@code battle}, narrow
 * form) and its round result notice. Pure, so the wording and geometry are unit-tested.
 *
 * <p>Blue's manpower is always on the left and red's on the right; the viewer's own side is
 * drawn in the HUD friendly blue and the other in the hostile red. Both bars are anchored at the
 * centre line and reach out towards their own number (preview {@code battle}), so a side losing
 * manpower shrinks back towards the centre.
 */
public final class BattleStripModel {
    public static final String VICTORY_KEY = "hud.wok_infantry.round.victory";
    public static final String DEFEAT_KEY = "hud.wok_infantry.round.defeat";
    public static final String DRAW_KEY = "hud.wok_infantry.round.draw";
    public static final String SIDE_VICTORY_KEY = "hud.wok_infantry.round.side_victory";
    /** Number x from the strip's edges; the first text row sits 5px under the top edge. */
    public static final int TEXT_INSET = 4;
    public static final int TEXT_TOP = 5;
    /** Bars keep 2px from their number and 2px from the centre line. */
    public static final int BAR_GAP = 2;

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
        int y = strip.top() + TEXT_TOP;
        int centre = (strip.left() + strip.right()) / 2;
        int blueX = strip.left() + TEXT_INSET;
        int redX = strip.right() - TEXT_INSET - redTextWidth;
        int blueTrackLeft = blueX + blueTextWidth + BAR_GAP;
        int redTrackRight = redX - BAR_GAP;
        UiRect blueTrack = UiRect.of(blueTrackLeft, y + 2,
                Math.max(blueTrackLeft, centre - BAR_GAP), y + 5);
        UiRect redTrack = UiRect.of(Math.min(centre + BAR_GAP, redTrackRight), y + 2,
                redTrackRight, y + 5);
        return new Geometry(blueX, redX, y, blueTrack, redTrack);
    }
}
