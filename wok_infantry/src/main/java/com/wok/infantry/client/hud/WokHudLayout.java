package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.config.InfantryClientConfig.HudRosterMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry of the WOK步战 battle HUD (preview {@code kit/ui.js} {@code HUD.layout} and the
 * {@code new} variant of {@code surfaces/10-hud.js}): one slot per core part, chosen so the parts
 * never overlap each other, the vanilla hotbar, the off-hand slot, the attack indicator, the
 * status-effect icons, or the WOK步战附属-占点 panel. No Minecraft class is touched, so every
 * tier is unit-tested.
 *
 * <p>Two coordinate spaces meet here. Vanilla parts and add-on HUDs are drawn in GUI-scaled
 * pixels; the core HUD is drawn at the {@code UiScale.hudFactor()} factor {@code f} (2 at GUI
 * scale 1 with a large window, otherwise 1), so its layout space is the GUI size divided by
 * {@code f}. Every
 * obstacle is given in GUI pixels and converted here; every result is in layout pixels, and
 * {@link Layout#toGui} converts a result back for add-ons.
 *
 * <p>Slots: squad roster at the top left; the battle strip (or, before the lock, the formation
 * ballot) at the top centre with notices under it; vitals at the bottom left under the chat and
 * left of the hotbar; a centre-low slot for the downed panel.
 */
public final class WokHudLayout {
    /** Narrower layouts use the narrow roster (116px, role symbols, class initial). */
    public static final int NARROW_WIDTH = 400;
    /** Lower layouts (or narrow ones) are tight: 2px edge, short rows and short wording. */
    public static final int TIGHT_HEIGHT = 280;
    public static final int ROSTER_WIDTH_NARROW = 116;
    public static final int ROSTER_WIDTH = 172;
    public static final int MAX_ROSTER_ROWS = 8;
    public static final int ROW_HEIGHT_TIGHT = 10;
    public static final int ROW_HEIGHT = 12;
    public static final int HEADER_HEIGHT_TIGHT = 11;
    public static final int HEADER_HEIGHT = 13;
    /** Narrow (one-line) battle strip; the 30px wide form is kept for objective data later. */
    public static final int STRIP_HEIGHT = 17;
    public static final int STRIP_MAX_WIDTH_TIGHT = 220;
    public static final int STRIP_MAX_WIDTH = 300;
    public static final int STRIP_MIN_CENTRED_TIGHT = 150;
    public static final int STRIP_MIN_CENTRED = 200;
    /**
     * Narrowest strip still worth drawing beside the status-effect icons; when giving way to the
     * icons would leave less, the strip moves below them instead.
     */
    public static final int STRIP_MIN_WIDTH_TIGHT = 100;
    public static final int STRIP_MIN_WIDTH = 120;
    public static final int TOAST_MIN_WIDTH = 60;
    public static final int TOAST_HEIGHT = 12;
    /** Notice width beyond its text: 6px before, 6px after. */
    public static final int TOAST_PADDING = 12;
    public static final int VOTE_HEIGHT = 27;
    public static final int VOTE_HEIGHT_METER = 32;
    public static final int VOTE_MAX_WIDTH_TIGHT = 220;
    public static final int VOTE_MAX_WIDTH = 300;
    /** The vitals slot starts this far above the bottom (under an unfocused chat at h − 40). */
    public static final int VITALS_TOP_FROM_BOTTOM = 37;
    public static final int VITALS_MAX_WIDTH_TIGHT = 64;
    public static final int VITALS_MAX_WIDTH = 110;
    /** Standalone stamina plate: two labelled rows (手 over 腿). */
    public static final int STAMINA_PLATE_HEIGHT = 27;
    /** One-row stamina plate ("手 ▬ 腿 ▬") where the stacked plate would reach into the chat. */
    public static final int STAMINA_ROW_HEIGHT = 15;
    /** Vanilla chat lines end this far above the bottom, GUI pixels (ChatComponent). */
    public static final int CHAT_BOTTOM_FROM_BOTTOM = 40;
    /** GUI pixels kept between the chat's last line and the vitals slot. */
    public static final int CHAT_CLEARANCE = 2;
    /** Room kept between a HUD part and a vanilla obstacle. */
    public static final int OBSTACLE_CLEARANCE = 4;
    public static final int CENTER_LOW_MAX_WIDTH = 200;
    public static final int CENTER_LOW_OFFSET = 16;
    public static final int CENTER_LOW_HEIGHT = 48;

    // ---- vanilla geometry, GUI pixels (Gui.renderHotbar / renderEffects / BossHealthOverlay) ----
    public static final int HOTBAR_HALF_WIDTH = 91;
    /** Off-hand slot left of the hotbar: x = width / 2 − 91 − 29. */
    public static final int OFFHAND_SLOT_WIDTH = 29;
    /** Attack indicator left of the hotbar: x = width / 2 − 91 − 22. */
    public static final int ATTACK_INDICATOR_OFFSET = 22;
    public static final int EFFECT_ICON_PITCH = 25;
    public static final int EFFECT_ICON_SIZE = 24;
    public static final int EFFECT_BENEFICIAL_TOP = 1;
    public static final int EFFECT_HARMFUL_TOP = 27;
    /** First boss bar title row. */
    public static final int BOSS_TITLE_TOP = 3;

    private WokHudLayout() {
    }

    /** How much of the squad roster to draw this frame. */
    public enum RosterPresence {
        HIDDEN,
        /** Title row only (squad name and member count). */
        COLLAPSED,
        FULL
    }

    /**
     * What the HUD shows this frame and where the vanilla obstacles are.
     *
     * @param guiWidth          GUI-scaled screen width (the overlay's width)
     * @param guiHeight         GUI-scaled screen height
     * @param factor            core HUD scale factor (1, or 2 at GUI scale 1)
     * @param rosterRows        roster member rows (0 = no roster); capped at {@link #MAX_ROSTER_ROWS}
     * @param rosterCollapsed   roster shows its title row only
     * @param strip             battle strip shown
     * @param toastTextWidths   text widths (layout pixels) of the notices under the strip, top first
     * @param voteContentWidth  natural width of the ballot plate (0 = none); replaces strip and notices
     * @param voteMeter         ballot plate carries the tally meter (32px instead of 27px)
     * @param stamina           standalone stamina plate shown (no body-health companion slot)
     * @param offhandLeft       the off-hand slot is drawn left of the hotbar
     * @param attackIndicatorLeft the hotbar attack indicator is drawn left of the hotbar
     * @param beneficialEffects status-effect icons in the top row
     * @param harmfulEffects    status-effect icons in the second row
     * @param effectOffsetX     x translation (GUI pixels) applied to the effect icons by another MOD
     * @param effectOffsetY     y translation (GUI pixels) applied to the effect icons by another MOD
     * @param capturePanel      WOK步战附属-占点 panel in GUI pixels, or null while it is hidden
     */
    public record Input(int guiWidth, int guiHeight, int factor,
                        int rosterRows, boolean rosterCollapsed,
                        boolean strip, List<Integer> toastTextWidths,
                        int voteContentWidth, boolean voteMeter,
                        boolean stamina,
                        boolean offhandLeft, boolean attackIndicatorLeft,
                        int beneficialEffects, int harmfulEffects,
                        int effectOffsetX, int effectOffsetY,
                        UiRect capturePanel) {
        public Input {
            factor = Math.max(1, factor);
            rosterRows = Math.max(0, Math.min(MAX_ROSTER_ROWS, rosterRows));
            toastTextWidths = List.copyOf(toastTextWidths == null ? List.of() : toastTextWidths);
            voteContentWidth = Math.max(0, voteContentWidth);
            beneficialEffects = Math.max(0, beneficialEffects);
            harmfulEffects = Math.max(0, harmfulEffects);
        }

        /** Empty HUD on a {@code guiWidth}×{@code guiHeight} screen drawn at {@code factor}. */
        public static Input screen(int guiWidth, int guiHeight, int factor) {
            return new Input(guiWidth, guiHeight, factor, 0, false, false, List.of(), 0, false,
                    false, false, false, 0, 0, 0, 0, null);
        }

        public Input withRoster(int rows, boolean collapsed) {
            return new Input(guiWidth, guiHeight, factor, rows, collapsed, strip, toastTextWidths,
                    voteContentWidth, voteMeter, stamina, offhandLeft, attackIndicatorLeft,
                    beneficialEffects, harmfulEffects, effectOffsetX, effectOffsetY, capturePanel);
        }

        public Input withStrip(boolean shown) {
            return new Input(guiWidth, guiHeight, factor, rosterRows, rosterCollapsed, shown,
                    toastTextWidths, voteContentWidth, voteMeter, stamina, offhandLeft,
                    attackIndicatorLeft, beneficialEffects, harmfulEffects, effectOffsetX,
                    effectOffsetY, capturePanel);
        }

        public Input withToasts(List<Integer> textWidths) {
            return new Input(guiWidth, guiHeight, factor, rosterRows, rosterCollapsed, strip,
                    textWidths, voteContentWidth, voteMeter, stamina, offhandLeft,
                    attackIndicatorLeft, beneficialEffects, harmfulEffects, effectOffsetX,
                    effectOffsetY, capturePanel);
        }

        public Input withVote(int contentWidth, boolean meter) {
            return new Input(guiWidth, guiHeight, factor, rosterRows, rosterCollapsed, strip,
                    toastTextWidths, contentWidth, meter, stamina, offhandLeft,
                    attackIndicatorLeft, beneficialEffects, harmfulEffects, effectOffsetX,
                    effectOffsetY, capturePanel);
        }

        public Input withStamina(boolean shown) {
            return new Input(guiWidth, guiHeight, factor, rosterRows, rosterCollapsed, strip,
                    toastTextWidths, voteContentWidth, voteMeter, shown, offhandLeft,
                    attackIndicatorLeft, beneficialEffects, harmfulEffects, effectOffsetX,
                    effectOffsetY, capturePanel);
        }

        public Input withHotbarNeighbours(boolean offhandOnLeft, boolean indicatorOnLeft) {
            return new Input(guiWidth, guiHeight, factor, rosterRows, rosterCollapsed, strip,
                    toastTextWidths, voteContentWidth, voteMeter, stamina, offhandOnLeft,
                    indicatorOnLeft, beneficialEffects, harmfulEffects, effectOffsetX,
                    effectOffsetY, capturePanel);
        }

        public Input withEffects(int beneficial, int harmful) {
            return new Input(guiWidth, guiHeight, factor, rosterRows, rosterCollapsed, strip,
                    toastTextWidths, voteContentWidth, voteMeter, stamina, offhandLeft,
                    attackIndicatorLeft, beneficial, harmful, effectOffsetX, effectOffsetY,
                    capturePanel);
        }

        /**
         * Translation (GUI pixels) another MOD applies to the vanilla effect icons, e.g.
         * JourneyMap moving them clear of its minimap; measured from the pose at render time.
         */
        public Input withEffectOffset(int dx, int dy) {
            return new Input(guiWidth, guiHeight, factor, rosterRows, rosterCollapsed, strip,
                    toastTextWidths, voteContentWidth, voteMeter, stamina, offhandLeft,
                    attackIndicatorLeft, beneficialEffects, harmfulEffects, dx, dy, capturePanel);
        }

        public Input withCapturePanel(UiRect guiRect) {
            return new Input(guiWidth, guiHeight, factor, rosterRows, rosterCollapsed, strip,
                    toastTextWidths, voteContentWidth, voteMeter, stamina, offhandLeft,
                    attackIndicatorLeft, beneficialEffects, harmfulEffects, effectOffsetX,
                    effectOffsetY, guiRect);
        }
    }

    /**
     * Slots of one frame in layout pixels ({@code null} = part not shown).
     *
     * @param band            the top-centre column: where the strip sits (or would sit) and
     *                        what notices centre in
     * @param vitals          bottom-left vitals slot (always present); it starts under the
     *                        vanilla chat, so at the 2× HUD it is only one row tall
     * @param staminaPlate    standalone stamina plate: the bottom of {@code vitals}, 27px
     *                        stacked or 15px in one row
     * @param staminaRow      the stamina plate is the one-row form
     * @param centerLow       slot for a centre-low panel such as the downed panel
     * @param topCenterNext   first free slot under the top-centre plates, for an add-on panel
     * @param topCenterBottom bottom of the lowest top-centre plate, or 0 when none is shown
     * @param bossShift       GUI pixels the vanilla boss bars move down (0 = none)
     * @param capturePanel    the WOK步战附属-占点 panel in layout pixels, or null
     */
    public record Layout(int width, int height, int factor, boolean tight, boolean narrow,
                         int edge, int gap,
                         UiRect roster, UiRect strip, List<UiRect> toasts, UiRect vote,
                         UiRect band, UiRect vitals, UiRect staminaPlate, boolean staminaRow,
                         UiRect centerLow, UiRect topCenterNext, int topCenterBottom,
                         int bossShift, UiRect capturePanel) {
        public Layout {
            toasts = List.copyOf(toasts);
        }

        /** {@code rect} in GUI pixels (add-ons draw there); null stays null. */
        public UiRect toGui(UiRect rect) {
            if (rect == null) {
                return null;
            }
            return new UiRect(rect.left() * factor, rect.top() * factor, rect.right() * factor,
                    rect.bottom() * factor);
        }

        /** Every core plate drawn this frame (roster, strip, notices, ballot, stamina). */
        public List<UiRect> plates() {
            List<UiRect> plates = new ArrayList<>();
            if (roster != null) {
                plates.add(roster);
            }
            if (strip != null) {
                plates.add(strip);
            }
            plates.addAll(toasts);
            if (vote != null) {
                plates.add(vote);
            }
            if (staminaPlate != null) {
                plates.add(staminaPlate);
            }
            return plates;
        }
    }

    /** Tight tier: narrow or low screens (2px edge, short rows, short wording). */
    public static boolean isTight(int width, int height) {
        return width < NARROW_WIDTH || height < TIGHT_HEIGHT;
    }

    /**
     * Roster presence: spectators and {@link HudRosterMode#HIDDEN} hide it; in
     * {@link HudRosterMode#AUTO} it shrinks to its title row on a tight screen with the chat
     * open, while the F3 screen is shown, or while the player-list key is held, so the vanilla
     * overlays drawn above the core HUD stay readable.
     */
    public static RosterPresence rosterPresence(HudRosterMode mode, boolean tight,
                                                boolean chatOpen, boolean debugScreen,
                                                boolean playerListHeld, boolean spectator) {
        HudRosterMode safe = mode == null ? HudRosterMode.AUTO : mode;
        if (spectator || safe == HudRosterMode.HIDDEN) {
            return RosterPresence.HIDDEN;
        }
        return switch (safe) {
            case FULL -> RosterPresence.FULL;
            case COLLAPSED -> RosterPresence.COLLAPSED;
            default -> (tight && chatOpen) || debugScreen || playerListHeld
                    ? RosterPresence.COLLAPSED : RosterPresence.FULL;
        };
    }

    /** Roster plate height for {@code rows} members (title row only when collapsed). */
    public static int rosterHeight(int rows, boolean tight, boolean collapsed) {
        int header = tight ? HEADER_HEIGHT_TIGHT : HEADER_HEIGHT;
        if (collapsed) {
            return header;
        }
        int safeRows = Math.max(0, Math.min(MAX_ROSTER_ROWS, rows));
        return header + safeRows * (tight ? ROW_HEIGHT_TIGHT : ROW_HEIGHT) + 2;
    }

    public static int rosterWidth(boolean narrow) {
        return narrow ? ROSTER_WIDTH_NARROW : ROSTER_WIDTH;
    }

    public static Layout compute(Input in) {
        int factor = in.factor();
        int width = Math.max(1, in.guiWidth() / factor);
        int height = Math.max(1, in.guiHeight() / factor);
        boolean tight = isTight(width, height);
        boolean narrow = width < NARROW_WIDTH;
        int edge = tight ? 2 : 4;
        int gap = tight ? TacticalHud.PLATE_GAP_TIGHT : TacticalHud.PLATE_GAP;
        int toastGap = tight ? 3 : 5;
        int dockGap = tight ? 5 : 8;
        UiRect capture = toLayout(in.capturePanel(), factor);
        List<UiRect> effects = effectRects(in);
        int rosterWidth = rosterWidth(narrow);

        UiRect roster = null;
        if (in.rosterRows() > 0) {
            roster = UiRect.ofSize(edge, edge, rosterWidth,
                    rosterHeight(in.rosterRows(), tight, in.rosterCollapsed()));
            roster = belowCapture(roster, capture, gap);
        }

        // Top centre: centre the strip unless that runs into the roster, then dock it right of
        // the roster (the roster width is reserved even while no roster is shown, so the strip
        // does not jump when the viewer joins a squad).
        int dockLeft = edge + rosterWidth + dockGap;
        int maxStrip = tight ? STRIP_MAX_WIDTH_TIGHT : STRIP_MAX_WIDTH;
        int minCentred = tight ? STRIP_MIN_CENTRED_TIGHT : STRIP_MIN_CENTRED;
        int bandWidth = Math.min(maxStrip, width - 2 * dockLeft);
        int bandLeft;
        if (bandWidth >= minCentred) {
            bandLeft = width / 2 - bandWidth / 2;
        } else {
            bandLeft = dockLeft;
            bandWidth = Math.max(0, Math.min(maxStrip, width - edge - dockLeft));
        }
        int bandMinLeft = Math.min(bandLeft, dockLeft);
        int stripMinWidth = tight ? STRIP_MIN_WIDTH_TIGHT : STRIP_MIN_WIDTH;
        UiRect band = place(UiRect.ofSize(bandLeft, edge, bandWidth, STRIP_HEIGHT), null,
                effects, gap, bandMinLeft, stripMinWidth);

        boolean voteShown = in.voteContentWidth() > 0;
        UiRect strip = null;
        List<UiRect> toasts = new ArrayList<>();
        UiRect vote = null;
        if (voteShown) {
            int maxVote = Math.min(tight ? VOTE_MAX_WIDTH_TIGHT : VOTE_MAX_WIDTH,
                    width - 2 * edge);
            int voteWidth = Math.max(0, Math.min(maxVote, in.voteContentWidth()));
            int voteHeight = in.voteMeter() ? VOTE_HEIGHT_METER : VOTE_HEIGHT;
            int voteLeft = width / 2 - voteWidth / 2;
            int minLeft = edge;
            if (roster != null && roster.top() < edge + voteHeight + gap) {
                minLeft = roster.right() + dockGap;
            }
            if (voteLeft < minLeft) {
                voteLeft = minLeft;
                voteWidth = Math.max(0, Math.min(voteWidth, width - edge - minLeft));
            }
            vote = place(UiRect.ofSize(voteLeft, edge, voteWidth, voteHeight), capture, effects,
                    gap, minLeft, Math.min(voteWidth, minCentred));
        } else {
            int cursor = edge;
            if (in.strip()) {
                strip = place(band, capture, effects, gap, bandMinLeft, stripMinWidth);
                cursor = strip.bottom() + toastGap;
            }
            for (int textWidth : in.toastTextWidths()) {
                int toastWidth = Math.max(0, Math.min(Math.max(0, textWidth) + TOAST_PADDING,
                        band.width()));
                int left = band.left() + (band.width() - toastWidth) / 2;
                UiRect toast = place(UiRect.ofSize(left, cursor, toastWidth, TOAST_HEIGHT),
                        capture, effects, gap, band.left(),
                        Math.min(toastWidth, TOAST_MIN_WIDTH));
                toasts.add(toast);
                cursor = toast.bottom() + toastGap;
            }
        }

        int topCenterBottom = 0;
        if (strip != null) {
            topCenterBottom = Math.max(topCenterBottom, strip.bottom());
        }
        for (UiRect toast : toasts) {
            topCenterBottom = Math.max(topCenterBottom, toast.bottom());
        }
        if (vote != null) {
            topCenterBottom = Math.max(topCenterBottom, vote.bottom());
        }
        int nextTop = topCenterBottom > 0 ? topCenterBottom + gap : edge;
        UiRect topCenterNext = UiRect.of(band.left(), nextTop, band.right(),
                Math.max(nextTop, height / 2));
        int bossShift = topCenterBottom > 0
                ? Math.max(0, topCenterBottom * factor + OBSTACLE_CLEARANCE - BOSS_TITLE_TOP) : 0;

        int vitalsRight = Math.max(edge, Math.min(
                Math.floorDiv(hotbarObstacleLeft(in), factor) - OBSTACLE_CLEARANCE,
                edge + (tight ? VITALS_MAX_WIDTH_TIGHT : VITALS_MAX_WIDTH)));
        // Under the vanilla chat, whose last line ends 40 GUI pixels above the bottom: at the
        // 2x HUD that leaves room for one row only.
        int chatClearTop = -Math.floorDiv(-(in.guiHeight() - CHAT_BOTTOM_FROM_BOTTOM
                + CHAT_CLEARANCE), factor);
        int vitalsTop = Math.min(Math.max(height - VITALS_TOP_FROM_BOTTOM, chatClearTop),
                height - edge - STAMINA_ROW_HEIGHT);
        UiRect vitals = UiRect.of(edge, vitalsTop, vitalsRight, height - edge);
        boolean staminaRow = vitals.height() < STAMINA_PLATE_HEIGHT;
        UiRect staminaPlate = in.stamina()
                ? UiRect.of(edge, height - edge - (staminaRow ? STAMINA_ROW_HEIGHT
                : STAMINA_PLATE_HEIGHT), vitalsRight, height - edge)
                : null;

        int lowWidth = Math.max(0, Math.min(CENTER_LOW_MAX_WIDTH, width - 2 * edge - 20));
        int lowTop = height / 2 + CENTER_LOW_OFFSET;
        UiRect centerLow = UiRect.of(width / 2 - lowWidth / 2, lowTop,
                width / 2 + (lowWidth + 1) / 2, lowTop + CENTER_LOW_HEIGHT);

        return new Layout(width, height, factor, tight, narrow, edge, gap, roster, strip, toasts,
                vote, band, vitals, staminaPlate, staminaRow, centerLow, topCenterNext,
                topCenterBottom, bossShift, capture);
    }

    /**
     * Leftmost vanilla part next to the hotbar's left edge, GUI pixels: the hotbar itself
     * (width / 2 − 91), the off-hand slot (− 120) and the attack indicator (− 113) when drawn
     * on the left.
     */
    public static int hotbarObstacleLeft(Input in) {
        int centre = in.guiWidth() / 2;
        int left = centre - HOTBAR_HALF_WIDTH;
        if (in.offhandLeft()) {
            left = Math.min(left, centre - HOTBAR_HALF_WIDTH - OFFHAND_SLOT_WIDTH);
        }
        if (in.attackIndicatorLeft()) {
            left = Math.min(left, centre - HOTBAR_HALF_WIDTH - ATTACK_INDICATOR_OFFSET);
        }
        return left;
    }

    /** Status-effect icon rows of {@code in} in layout pixels, offset included. */
    public static List<UiRect> effectRects(Input in) {
        return effectRects(in.guiWidth(), in.beneficialEffects(), in.harmfulEffects(),
                in.effectOffsetX(), in.effectOffsetY(), in.factor());
    }

    /**
     * Status-effect icon rows in layout pixels (vanilla Gui.renderEffects, demo offset ignored),
     * moved by the translation another MOD applies to them.
     */
    public static List<UiRect> effectRects(int guiWidth, int beneficial, int harmful,
                                           int offsetX, int offsetY, int factor) {
        List<UiRect> rows = new ArrayList<>(2);
        if (beneficial > 0) {
            rows.add(toLayout(UiRect.of(guiWidth - EFFECT_ICON_PITCH * beneficial,
                    EFFECT_BENEFICIAL_TOP, guiWidth - EFFECT_ICON_PITCH + EFFECT_ICON_SIZE,
                    EFFECT_BENEFICIAL_TOP + EFFECT_ICON_SIZE).offset(offsetX, offsetY), factor));
        }
        if (harmful > 0) {
            rows.add(toLayout(UiRect.of(guiWidth - EFFECT_ICON_PITCH * harmful,
                    EFFECT_HARMFUL_TOP, guiWidth - EFFECT_ICON_PITCH + EFFECT_ICON_SIZE,
                    EFFECT_HARMFUL_TOP + EFFECT_ICON_SIZE).offset(offsetX, offsetY), factor));
        }
        return rows;
    }

    /** GUI rectangle to layout pixels, rounding outwards so the result still covers it. */
    public static UiRect toLayout(UiRect gui, int factor) {
        if (gui == null) {
            return null;
        }
        int f = Math.max(1, factor);
        return new UiRect(Math.floorDiv(gui.left(), f), Math.floorDiv(gui.top(), f),
                -Math.floorDiv(-gui.right(), f), -Math.floorDiv(-gui.bottom(), f));
    }

    /** Moves {@code rect} under the capture panel when it would touch it (gap included). */
    static UiRect belowCapture(UiRect rect, UiRect capture, int gap) {
        if (capture == null || capture.isEmpty() || !rect.intersects(capture.inset(-gap))) {
            return rect;
        }
        return rect.offset(0, capture.bottom() + gap - rect.top());
    }

    /**
     * Places a top-centre plate: under the capture panel when it would touch it, then clear of
     * the status-effect icons (slid left and narrowed, or, when that would leave it narrower than
     * {@code minWidth}, moved below the icon rows), then under the capture panel again.
     */
    static UiRect place(UiRect rect, UiRect capture, List<UiRect> effects, int gap, int minLeft,
                        int minWidth) {
        UiRect placed = belowCapture(rect, capture, gap);
        UiRect cleared = clearOfEffects(placed, effects, gap, minLeft);
        if (cleared.width() < Math.min(minWidth, placed.width())) {
            cleared = belowEffects(placed, effects, gap);
        }
        return belowCapture(cleared, capture, gap);
    }

    /** Moves {@code rect} down below every effect row it would touch (gap included). */
    static UiRect belowEffects(UiRect rect, List<UiRect> effects, int gap) {
        UiRect moved = rect;
        for (int pass = 0; pass <= effects.size(); pass++) {
            boolean changed = false;
            for (UiRect row : effects) {
                if (moved.intersects(row.inset(-gap))) {
                    moved = moved.offset(0, row.bottom() + gap - moved.top());
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return moved;
    }

    /**
     * Keeps {@code rect} {@code gap} left of every status-effect row it shares a pixel row with
     * and reaches into: first slides it left (never past {@code minLeft}), then narrows it.
     */
    static UiRect clearOfEffects(UiRect rect, List<UiRect> effects, int gap, int minLeft) {
        int limit = Integer.MAX_VALUE;
        for (UiRect row : effects) {
            if (row.top() < rect.bottom() && row.bottom() > rect.top()
                    && row.right() + gap > rect.left()) {
                limit = Math.min(limit, row.left() - gap);
            }
        }
        if (rect.right() <= limit) {
            return rect;
        }
        int left = Math.max(Math.min(minLeft, rect.left()), limit - rect.width());
        int right = Math.min(left + rect.width(), limit);
        return new UiRect(left, rect.top(), Math.max(left, right), rect.bottom());
    }
}
