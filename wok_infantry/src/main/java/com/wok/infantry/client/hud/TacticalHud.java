package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.BattleUiTheme;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TextFit;
import com.wok.infantry.stamina.StaminaRules;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Drawing primitives of the tactical-tablet HUD (preview {@code kit/ui.js} {@code HUD.plate},
 * {@code HUD.readout}, {@code HUD.healthColor}, {@code UI.meter} and the shared helpers of
 * {@code kit/hud-parts.js}), for the HUD overlays: translucent field plates with an accent edge,
 * meters on the HUD track, numeric readouts, key caps and status dots.
 *
 * <p>Colours come only from {@link TacticalBoardTheme} and text is never shadowed. Decisions
 * (geometry, colours, what fits) are pure static methods, so they are unit-tested without a
 * game. Layout slots (where a part goes) belong to the HUD layout, not here. Add-on MODs must
 * not hard-depend on this class; copy the values they need.
 */
public final class TacticalHud {
    /** Side of a plate that carries the accent edge. */
    public enum Edge {
        /** 2px bar on the left (anchored to the left screen edge: roster, vitals, toasts). */
        LEFT,
        /** 1px line on top (top-centre strips: battle strip, ballot, downed panel). */
        TOP,
        /** 2px bar on the right. */
        RIGHT,
        NONE
    }

    /** Semantic tone of a notice. Waiting and to-do states are {@link #NEUTRAL}, never orange. */
    public enum Tone {
        SUCCESS,
        INFO,
        DANGER,
        NEUTRAL
    }

    /** Integer rectangle [left, right) × [top, bottom). */
    public record Box(int left, int top, int right, int bottom) {
        public int width() {
            return right - left;
        }

        public int height() {
            return bottom - top;
        }

        public boolean isEmpty() {
            return right <= left || bottom <= top;
        }
    }

    /** Inline segment of a HUD line: coloured text, or a key cap such as the bound key's name. */
    public record Segment(Component text, int color, boolean key) {
        public static Segment text(Component text, int color) {
            return new Segment(text, color, false);
        }

        public static Segment key(Component keyName) {
            return new Segment(keyName, TacticalBoardTheme.LIGHT, true);
        }
    }

    /** Where a segment of a line lands: its x, the width it may take and whether it was cut. */
    public record PlacedSegment(int index, int x, int room, boolean truncated) {
    }

    /** Text row distance from a top-edge plate's top (clear of every theme's top band). */
    public static final int TEXT_INSET_TOP_EDGE = 5;
    /** Text column distance from a left-edge plate's left. */
    public static final int TEXT_INSET_LEFT_EDGE = 3;
    /** Room kept under the last text row of a plate. */
    public static final int TEXT_INSET_BOTTOM = 4;
    /** Minimum gap between two plates (3 on tight screens): a theme may frame a plate 2px outside. */
    public static final int PLATE_GAP = 4;
    public static final int PLATE_GAP_TIGHT = 3;
    /** Divider ticks on a meter. */
    public static final int METER_TICK = 0x66000000;
    /** Wash behind the viewer's own roster row. */
    public static final int SELF_ROW_TINT = withAlpha(TacticalBoardTheme.SELECT_B, 0x30);
    /** Edge length of a roster status dot. */
    public static final int STATUS_DOT_SIZE = 3;
    /** Health above this ratio is green, above {@link #HEALTH_LOW} orange, otherwise red. */
    public static final float HEALTH_GOOD = 0.55F;
    public static final float HEALTH_LOW = 0.25F;
    /** Stamina at or below this (of 100) is red. */
    public static final float STAMINA_CRITICAL = 15.0F;

    private TacticalHud() {
    }

    // ---- plates ------------------------------------------------------------------------------

    /**
     * HUD field plate: translucent dark fill ({@link TacticalBoardTheme#HUD_PLATE}, or the more
     * opaque {@link TacticalBoardTheme#HUD_PLATE_SOLID}), a 1px {@link TacticalBoardTheme#HUD_EDGE}
     * outline and the accent edge on the anchored side.
     */
    public static void plate(GuiGraphics graphics, int left, int top, int right, int bottom,
                             Edge edge, int accent, boolean solid) {
        if (right <= left || bottom <= top) {
            return;
        }
        graphics.fill(left, top, right, bottom,
                solid ? TacticalBoardTheme.HUD_PLATE_SOLID : TacticalBoardTheme.HUD_PLATE);
        BattleUiTheme.outline(graphics, left, top, right, bottom, TacticalBoardTheme.HUD_EDGE);
        Box bar = accentBar(left, top, right, bottom, edge);
        if (bar != null) {
            graphics.fill(bar.left(), bar.top(), bar.right(), bar.bottom(), accent);
        }
    }

    /** Plate with the default own-side accent ({@link TacticalBoardTheme#HUD_FRIENDLY}) on the left. */
    public static void plate(GuiGraphics graphics, int left, int top, int right, int bottom) {
        plate(graphics, left, top, right, bottom, Edge.LEFT, TacticalBoardTheme.HUD_FRIENDLY, false);
    }

    /** Accent bar of a plate, drawn over its outline; {@code null} for {@link Edge#NONE}. */
    public static Box accentBar(int left, int top, int right, int bottom, Edge edge) {
        if (edge == null || edge == Edge.NONE || right <= left || bottom <= top) {
            return null;
        }
        return switch (edge) {
            case TOP -> new Box(left, top, right, top + 1);
            case RIGHT -> new Box(Math.max(left, right - 2), top, right, bottom);
            case LEFT -> new Box(left, top, Math.min(right, left + 2), bottom);
            case NONE -> null;
        };
    }

    // ---- meters ------------------------------------------------------------------------------

    /** Meter on the HUD track, filled from the left. */
    public static void meter(GuiGraphics graphics, int left, int top, int right, int bottom,
                             float ratio, int color) {
        meter(graphics, left, top, right, bottom, ratio, color, TacticalBoardTheme.HUD_TRACK, 0,
                false);
    }

    /**
     * Meter: {@code track} background, {@code color} fill of {@link #fillWidth} and, with
     * {@code ticks > 1}, dark dividers between equal parts. {@code fromRight} fills from the
     * right edge (the own side's ticket bar running out towards the centre).
     */
    public static void meter(GuiGraphics graphics, int left, int top, int right, int bottom,
                             float ratio, int color, int track, int ticks, boolean fromRight) {
        if (right <= left || bottom <= top) {
            return;
        }
        graphics.fill(left, top, right, bottom, track);
        int fill = fillWidth(right - left, ratio);
        if (fill > 0) {
            if (fromRight) {
                graphics.fill(right - fill, top, right, bottom, color);
            } else {
                graphics.fill(left, top, left + fill, bottom, color);
            }
        }
        for (int i = 1; i < ticks; i++) {
            int x = tickX(left, right - left, i, ticks);
            graphics.fill(x, top, x + 1, bottom, METER_TICK);
        }
    }

    /** Filled width of a meter: {@code round(width × ratio)}, ratio clamped to 0–1 (NaN = 0). */
    public static int fillWidth(int width, float ratio) {
        if (width <= 0 || !(ratio > 0.0F)) {
            return 0;
        }
        return Math.round(width * Math.min(1.0F, ratio));
    }

    /** x of divider {@code index} (1 … ticks − 1) on a meter of {@code width}. */
    public static int tickX(int left, int width, int index, int ticks) {
        return left + Math.round(width * (float) index / Math.max(1, ticks));
    }

    /**
     * One-row labelled bar such as stamina "手 ▬▬▬": the label in
     * {@link TacticalBoardTheme#LIGHT_MUTED} at (x, y), then a 3px bar on the text's mid-line
     * from 2px after the label to {@code right}. The bar is left out when no room remains.
     *
     * @return {@code right}
     */
    public static int labelledBar(GuiGraphics graphics, Font font, Component label, float ratio,
                                  int color, int x, int y, int right) {
        int barLeft = x;
        if (label != null && !label.getString().isEmpty()) {
            graphics.drawString(font, label, x, y, TacticalBoardTheme.LIGHT_MUTED, false);
            barLeft = x + font.width(label) + 2;
        }
        Box bar = labelledBarTrack(barLeft, y, right);
        if (bar != null) {
            meter(graphics, bar.left(), bar.top(), bar.right(), bar.bottom(), ratio, color);
        }
        return right;
    }

    /** Track of a {@link #labelledBar} starting at {@code barLeft}; {@code null} without room. */
    public static Box labelledBarTrack(int barLeft, int textY, int right) {
        return right - barLeft < 1 ? null : new Box(barLeft, textY + 2, right, textY + 5);
    }

    // ---- readouts and text -------------------------------------------------------------------

    /**
     * Primary numeric readout (ticket counts, capture %, totals, roster count, countdown): the
     * one hook for HUD numbers, drawn without a shadow.
     *
     * @return the x just after the text
     */
    public static int readout(GuiGraphics graphics, Font font, String text, int x, int y,
                              int color) {
        if (text == null || text.isEmpty()) {
            return x;
        }
        graphics.drawString(font, text, x, y, color, false);
        return x + font.width(text);
    }

    /** {@link #readout(GuiGraphics, Font, String, int, int, int)} for a component. */
    public static int readout(GuiGraphics graphics, Font font, Component text, int x, int y,
                              int color) {
        if (text == null) {
            return x;
        }
        graphics.drawString(font, text, x, y, color, false);
        return x + font.width(text);
    }

    /** Total readout "value/max", or just "value" when that does not fit: numbers are never ellipsized. */
    public static String totalText(Font font, int value, int max, int maxWidth) {
        return totalTextPlain(value, max, maxWidth, font::width);
    }

    public static String totalTextPlain(int value, int max, int maxWidth,
                                        ToIntFunction<String> width) {
        String full = value + "/" + max;
        return width.applyAsInt(full) <= maxWidth ? full : String.valueOf(value);
    }

    // ---- colours -----------------------------------------------------------------------------

    /** Health colour: above 55 % green, above 25 % orange, otherwise red (NaN = red). */
    public static int healthColor(float ratio) {
        if (ratio > HEALTH_GOOD) {
            return TacticalBoardTheme.SUCCESS_B;
        }
        return ratio > HEALTH_LOW ? TacticalBoardTheme.ACCENT_B : TacticalBoardTheme.DANGER_B;
    }

    /**
     * Stamina colour for a value of 0–100: at or below 15 red, below the sway threshold
     * ({@link StaminaRules#SWAY_START_STAMINA}, 50) orange, otherwise the neutral bright gray,
     * so stamina never looks like the blue "current selection".
     */
    public static int staminaColor(float stamina) {
        if (!(stamina > STAMINA_CRITICAL)) {
            return TacticalBoardTheme.DANGER_B;
        }
        return stamina < StaminaRules.SWAY_START_STAMINA
                ? TacticalBoardTheme.ACCENT_B : TacticalBoardTheme.NEUTRAL_B;
    }

    /** Accent / text colour of a notice tone. */
    public static int toneColor(Tone tone) {
        if (tone == null) {
            return TacticalBoardTheme.NEUTRAL_B;
        }
        return switch (tone) {
            case SUCCESS -> TacticalBoardTheme.SUCCESS_B;
            case INFO -> TacticalBoardTheme.ACCENT_B;
            case DANGER -> TacticalBoardTheme.DANGER_B;
            case NEUTRAL -> TacticalBoardTheme.NEUTRAL_B;
        };
    }

    /** {@code argb} with its alpha replaced by {@code alpha} (0–255). */
    public static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
    }

    // ---- small parts -------------------------------------------------------------------------

    /**
     * Roster status dot, {@value #STATUS_DOT_SIZE}×{@value #STATUS_DOT_SIZE} at (x, y): filled,
     * or hollow for "waiting to deploy".
     */
    public static void statusDot(GuiGraphics graphics, int x, int y, int color, boolean hollow) {
        int right = x + STATUS_DOT_SIZE;
        int bottom = y + STATUS_DOT_SIZE;
        if (hollow) {
            BattleUiTheme.outline(graphics, x, y, right, bottom, color);
        } else {
            graphics.fill(x, y, right, bottom, color);
        }
    }

    /** Width of a key cap: the key name plus 2px on each side. */
    public static int keyCapWidth(Font font, Component key) {
        return keyCapWidth(font.width(key));
    }

    public static int keyCapWidth(int textWidth) {
        return textWidth + 4;
    }

    /**
     * Key cap like the terminal footer's: {@link TacticalBoardTheme#FRAME_MID} fill,
     * {@link TacticalBoardTheme#KEYCAP_EDGE} outline from y − 1 to y + 8 and the key name in
     * {@link TacticalBoardTheme#LIGHT}. Pass the bound key's real name, never a hard-coded "K".
     *
     * @return the x just after the cap
     */
    public static int keyCap(GuiGraphics graphics, Font font, Component key, int x, int y) {
        int width = keyCapWidth(font, key);
        graphics.fill(x, y - 1, x + width, y + 8, TacticalBoardTheme.FRAME_MID);
        BattleUiTheme.outline(graphics, x, y - 1, x + width, y + 8, TacticalBoardTheme.KEYCAP_EDGE);
        graphics.drawString(font, key, x + 2, y, TacticalBoardTheme.LIGHT, false);
        return x + width;
    }

    /** Natural width of a line of segments. */
    public static int segmentsWidth(Font font, List<Segment> segments) {
        int total = 0;
        for (Segment segment : segments) {
            total += segmentWidth(font, segment);
        }
        return total;
    }

    /**
     * Draws a line of segments from x, never past {@code right}: a key cap is drawn whole or
     * not at all, a text segment that does not fit ends in "…" and ends the line.
     *
     * @return the x just after the last drawn segment
     */
    public static int drawSegments(GuiGraphics graphics, Font font, List<Segment> segments,
                                   int x, int y, int right) {
        int end = x;
        for (PlacedSegment placed : layoutSegments(segments, x, right,
                segment -> segmentWidth(font, segment))) {
            Segment segment = segments.get(placed.index());
            if (segment.key()) {
                end = keyCap(graphics, font, segment.text(), placed.x(), y);
            } else {
                end = placed.x() + TextFit.draw(graphics, font, segment.text(), placed.x(), y,
                        placed.room(), segment.color(), TextFit.Align.LEFT).width();
            }
        }
        return end;
    }

    /**
     * Pure layout of {@link #drawSegments}: places segments left to right; stops before a key
     * cap that does not fit whole, and after a text segment that had to be cut.
     */
    public static List<PlacedSegment> layoutSegments(List<Segment> segments, int x, int right,
                                                     ToIntFunction<Segment> width) {
        List<PlacedSegment> placed = new ArrayList<>();
        int cursor = x;
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            if (segment == null || segment.text() == null) {
                continue;
            }
            int natural = width.applyAsInt(segment);
            int room = right - cursor;
            if (segment.key()) {
                if (natural > room) {
                    break;
                }
                placed.add(new PlacedSegment(i, cursor, natural, false));
                cursor += natural;
                continue;
            }
            if (room <= 0) {
                break;
            }
            if (natural <= room) {
                placed.add(new PlacedSegment(i, cursor, natural, false));
                cursor += natural;
            } else {
                placed.add(new PlacedSegment(i, cursor, room, true));
                break;
            }
        }
        return placed;
    }

    private static int segmentWidth(Font font, Segment segment) {
        if (segment == null || segment.text() == null) {
            return 0;
        }
        int textWidth = font.width(segment.text());
        return segment.key() ? keyCapWidth(textWidth) : textWidth;
    }
}
