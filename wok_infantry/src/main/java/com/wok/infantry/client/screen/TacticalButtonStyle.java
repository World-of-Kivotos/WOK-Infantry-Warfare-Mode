package com.wok.infantry.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * One state table for every WOK步战 key: normal, hover, selected, disabled, danger, success
 * and adjustable control (the seven states of the UI rules), plus "current" for the selected
 * entry that is intentionally not clickable (current tab, current class, current slot).
 *
 * <p>Priority is fixed: disabled &gt; current/selected &gt; kind &gt; hover. Disabled and current
 * keys never react to hover; hover only follows the mouse ({@code isHovered() && active}),
 * never the keyboard focus, which gets its own focus ring instead.
 *
 * <p>Every state carries a shape cue besides its colour (preview {@code 18-device-livery.js}
 * {@code UIX.button}, all palettes): selected = solid fill with a light left bar
 * ({@link #barWidth}) and no raised bevel; danger = a hazard tab of 45° stripes inside the left
 * edge ({@link #hazardWidth}); success = an automatic check icon when the key has no icon and it
 * fits; disabled = the diagonal hatch; control = the orange underline. Colours are read from
 * {@link TacticalBoardTheme} when a key is drawn, so a faction palette applied around the screen's
 * render is followed without caching.
 */
public final class TacticalButtonStyle {
    /**
     * Marker for "no colour" in {@link Palette}; as an {@link Options#badgeColor()} it means
     * "{@link TacticalBoardTheme#MUTED} of the palette active while the key is drawn".
     */
    public static final int NONE = 0;
    /** Inset behind a count badge on a selected or current key (darker than the fill). */
    public static final int BADGE_INSET_SELECTED = 0x40000000;
    /** Inset behind a count badge on a disabled key (lighter than the gray fill). */
    public static final int BADGE_INSET_DISABLED = 0x33FFFFFF;
    /** Most fills one hazard tab may take (preview: no texture, a few rects per key). */
    public static final int MAX_HAZARD_FILLS = 32;
    /**
     * Most stripe rows of a hazard tab: {@code rows + ceil(rows / 4)} runs at worst stays within
     * {@link #MAX_HAZARD_FILLS}; a taller key gets a centred band of this many rows.
     */
    static final int MAX_HAZARD_ROWS = 25;
    /** Period of the hazard stripes; the first half of each period is ink. */
    private static final int HAZARD_PERIOD = 4;

    /** Semantic kind a key is built with. */
    public enum Variant {
        NORMAL,
        CONTROL,
        DANGER,
        SUCCESS
    }

    /** Drawn state after applying the priority rules. */
    public enum State {
        NORMAL,
        HOVER,
        SELECTED,
        CURRENT,
        DISABLED,
        DANGER,
        /** Danger key while hovered or armed (second step of a destructive action). */
        DANGER_ARMED,
        SUCCESS,
        CONTROL
    }

    /** Resolved state; {@code hovered} is always false for states that ignore hover. */
    public record Look(State state, boolean hovered) {
    }

    /**
     * Colours of one look. {@code bar} is the light selection bar ({@link #barWidth} wide),
     * {@code underline} the control line, {@code lip} the raised-key bevel pair (top
     * {@link TacticalBoardTheme#BEVEL}, bottom lip), {@code stripes} the ink of the danger hazard
     * tab ({@link #hazardWidth} wide); {@link #NONE} means "not drawn".
     */
    public record Palette(int fill, int edge, int text, int bar, int underline, int lip,
                          boolean hatch, boolean darkFill, int stripes) {
        public boolean raised() {
            return lip != NONE;
        }

        /** Whether the key carries the danger hazard tab. */
        public boolean hazard() {
            return stripes != NONE;
        }
    }

    /**
     * Optional content settings: label alignment, a right-side badge, the focus ring and a 9×9
     * icon in front of the label. An {@code iconOnly} key draws just the icon, centred on the
     * whole key; its label is still the key's name for narration and is offered as a tooltip.
     * A {@code badgeColor} of {@link #NONE} is {@link TacticalBoardTheme#MUTED}, read when the
     * key is drawn (a colour taken when the options are built would miss the faction palette).
     */
    public record Options(TextFit.Align align, Component badge, int badgeColor,
                          boolean focusRing, TacticalIcon icon, boolean iconOnly) {
        public static final Options DEFAULT = new Options(TextFit.Align.CENTER, null, NONE,
                false);

        /** Options without an icon (the B2a form). */
        public Options(TextFit.Align align, Component badge, int badgeColor, boolean focusRing) {
            this(align, badge, badgeColor, focusRing, null, false);
        }

        public Options withFocusRing(boolean focus) {
            return focus == focusRing ? this
                    : new Options(align, badge, badgeColor, focus, icon, iconOnly);
        }

        /** Icon drawn in front of the label, in the label colour. */
        public Options withIcon(TacticalIcon newIcon) {
            return new Options(align, badge, badgeColor, focusRing, newIcon, false);
        }

        /** Icon-only key: the label is not drawn. */
        public Options withIconOnly(TacticalIcon newIcon) {
            return new Options(align, badge, badgeColor, focusRing, newIcon, newIcon != null);
        }
    }

    /**
     * Where the content of a key goes (preview {@code UI.button}: [icon][label][badge]).
     *
     * @param iconX    left of the 9×9 icon (meaningless without an icon)
     * @param iconY    top of the icon
     * @param textX    left of the label as drawn
     * @param textRoom width the label may take before it is ellipsized
     */
    public record Content(int iconX, int iconY, int textX, int textRoom) {
    }

    /** Card look used by preview cards (loadout candidates). */
    public enum CardState {
        NORMAL,
        HOVER,
        SELECTED,
        DISABLED
    }

    private TacticalButtonStyle() {
    }

    /**
     * Applies the fixed priority "disabled &gt; current/selected &gt; kind &gt; hover". "Current" is
     * the one explicit exception to "disabled first": the caller marks a selected entry that is
     * deliberately not clickable (current tab, current class), and only that keeps the selected
     * look while inactive; any other inactive key is drawn disabled.
     *
     * @param active   whether the key can be clicked
     * @param selected whether the key shows the current selection
     * @param current  whether the key is the selected entry that is deliberately not clickable;
     *                 it keeps the selected look even though it is inactive and ignores hover
     * @param hovered  mouse over the key (callers pass {@code isHovered()}, never focus)
     * @param armed    danger key in its confirmation step
     */
    public static Look resolve(boolean active, boolean selected, boolean current,
                               Variant variant, boolean hovered, boolean armed) {
        if (current) {
            return new Look(State.CURRENT, false);
        }
        if (!active) {
            return new Look(State.DISABLED, false);
        }
        if (selected) {
            return new Look(State.SELECTED, hovered);
        }
        return switch (variant == null ? Variant.NORMAL : variant) {
            case DANGER -> new Look(hovered || armed ? State.DANGER_ARMED : State.DANGER, hovered);
            case SUCCESS -> new Look(State.SUCCESS, hovered);
            case CONTROL -> new Look(State.CONTROL, hovered);
            case NORMAL -> new Look(hovered ? State.HOVER : State.NORMAL, hovered);
        };
    }

    /**
     * Colours of a look in the palette active right now (preview {@code UIX.button}): selected
     * keys are solid with the light bar and no bevel (one step lighter on hover); a danger key is
     * a light key with red outline, red text and red hazard stripes, and when hovered or armed
     * solid red with {@link TacticalBoardTheme#DANGER_DEEP} stripes and
     * {@link TacticalBoardTheme#ON_FILL} text; success writes {@code ON_FILL} on green inside
     * {@link TacticalBoardTheme#SUCCESS_EDGE}; disabled keys are gray and hatched inside
     * {@link TacticalBoardTheme#DISABLED_EDGE}.
     */
    public static Palette palette(Look look) {
        boolean hover = look.hovered();
        return switch (look.state()) {
            case NORMAL -> new Palette(TacticalBoardTheme.CARD, TacticalBoardTheme.BORDER,
                    TacticalBoardTheme.TEXT, NONE, NONE, TacticalBoardTheme.CARD_LIP, false, false,
                    NONE);
            case HOVER -> new Palette(TacticalBoardTheme.CARD_HOVER, TacticalBoardTheme.BORDER_DARK,
                    TacticalBoardTheme.TEXT, NONE, NONE, TacticalBoardTheme.CARD_LIP_HOVER,
                    false, false, NONE);
            case SELECTED -> new Palette(hover ? TacticalBoardTheme.SELECT_HOVER
                    : TacticalBoardTheme.SELECT, TacticalBoardTheme.SELECT_EDGE,
                    TacticalBoardTheme.ON_SELECT, TacticalBoardTheme.SELECT_BAR, NONE, NONE,
                    false, true, NONE);
            case CURRENT -> new Palette(TacticalBoardTheme.SELECT, TacticalBoardTheme.SELECT_EDGE,
                    TacticalBoardTheme.ON_SELECT, TacticalBoardTheme.SELECT_BAR, NONE, NONE,
                    false, true, NONE);
            case DISABLED -> new Palette(TacticalBoardTheme.CARD_DISABLED,
                    TacticalBoardTheme.DISABLED_EDGE, TacticalBoardTheme.DISABLED_TEXT,
                    NONE, NONE, NONE, true, false, NONE);
            case DANGER -> new Palette(TacticalBoardTheme.CARD, TacticalBoardTheme.DANGER,
                    TacticalBoardTheme.DANGER, NONE, NONE, TacticalBoardTheme.CARD_LIP, false,
                    false, TacticalBoardTheme.DANGER);
            case DANGER_ARMED -> new Palette(TacticalBoardTheme.DANGER, TacticalBoardTheme.DANGER,
                    TacticalBoardTheme.ON_FILL, NONE, NONE, NONE, false, true,
                    TacticalBoardTheme.DANGER_DEEP);
            case SUCCESS -> new Palette(hover ? TacticalBoardTheme.SUCCESS_HOVER
                    : TacticalBoardTheme.SUCCESS, TacticalBoardTheme.SUCCESS_EDGE,
                    TacticalBoardTheme.ON_FILL, NONE, NONE, NONE, false, true, NONE);
            case CONTROL -> new Palette(hover ? TacticalBoardTheme.CARD_HOVER
                    : TacticalBoardTheme.CARD, hover ? TacticalBoardTheme.BORDER_DARK
                    : TacticalBoardTheme.BORDER, TacticalBoardTheme.TEXT, NONE,
                    TacticalBoardTheme.ADJUST, hover ? TacticalBoardTheme.CARD_LIP_HOVER
                    : TacticalBoardTheme.CARD_LIP, false, false, NONE);
        };
    }

    /** Width of the light selection bar: 3px on keys and rows at least 16 tall, 2px below. */
    public static int barWidth(int height) {
        return height >= 16 ? 3 : 2;
    }

    /** Width of the danger hazard tab: 4px on keys at least 16 tall, 3px below. */
    public static int hazardWidth(int height) {
        return height >= 16 ? 4 : 3;
    }

    /**
     * Left padding of the label on a key {@code height} tall: clear of the hazard tab
     * ({@code hz + 3}) or of the selection bar ({@code barW + 3}), otherwise 3.
     */
    public static int labelPadLeft(Palette palette, int height) {
        if (palette.hazard()) {
            return hazardWidth(height) + 3;
        }
        return palette.bar() != NONE ? barWidth(height) + 3 : 3;
    }

    /**
     * Inset drawn behind a count badge: darker on a selected or current key, lighter on a disabled
     * one, otherwise the faint dark wash (preview {@code UIX.button}).
     */
    public static int badgeInset(State state) {
        return switch (state == null ? State.NORMAL : state) {
            case SELECTED, CURRENT -> BADGE_INSET_SELECTED;
            case DISABLED -> BADGE_INSET_DISABLED;
            default -> TacticalBoardTheme.BADGE_ON_CARD;
        };
    }

    /**
     * Colour of a count badge: {@link TacticalBoardTheme#ON_SELECT} on a selected or current key,
     * {@link TacticalBoardTheme#ON_FILL} on a solid success or armed danger fill, gray on a
     * disabled key, otherwise the caller's colour ({@link #NONE}: {@code MUTED}).
     */
    public static int badgeText(State state, int badgeColor) {
        return switch (state == null ? State.NORMAL : state) {
            case SELECTED, CURRENT -> TacticalBoardTheme.ON_SELECT;
            case SUCCESS, DANGER_ARMED -> TacticalBoardTheme.ON_FILL;
            case DISABLED -> TacticalBoardTheme.DISABLED_TEXT;
            default -> badgeColor != NONE ? badgeColor : TacticalBoardTheme.MUTED;
        };
    }

    /**
     * Whether a success key gets the automatic check icon: it has no icon of its own, it has a
     * label, and label plus icon still fit without shortening the label (preview: label width
     * + 11 &le; key width − 6 on a key without badge).
     *
     * @param badgeWidth width of the badge including its padding, 0 for none
     */
    public static boolean autoCheck(State state, boolean hasIcon, int labelWidth, int keyWidth,
                                    int badgeWidth) {
        if (state != State.SUCCESS || hasIcon || labelWidth <= 0) {
            return false;
        }
        return labelWidth + TacticalIcon.ADVANCE <= labelRoom(0, keyWidth, 3, false, badgeWidth);
    }

    /** Receives one horizontal run {@code [left, right)} of row {@code y}. */
    @FunctionalInterface
    public interface RunSink {
        void run(int left, int right, int y);
    }

    /**
     * Pure: the danger hazard tab of a key in [left, …) x [top, bottom) as horizontal runs (45°
     * stripes, period 4: pixel {@code (x, y)} is ink when {@code ((x − left) + (y − top)) % 4 < 2}),
     * {@code width} columns from {@code left + 1}, on the rows inside the outline. At most
     * {@link #MAX_HAZARD_FILLS} runs: a key taller than {@code MAX_HAZARD_ROWS + 2} gets a centred
     * band of {@link #MAX_HAZARD_ROWS} rows.
     *
     * @return the number of runs
     */
    public static int hazardRuns(int left, int top, int bottom, int width, RunSink sink) {
        int firstRow = top + 1;
        int endRow = bottom - 1;
        if (width <= 0 || endRow <= firstRow) {
            return 0;
        }
        if (endRow - firstRow > MAX_HAZARD_ROWS) {
            firstRow += (endRow - firstRow - MAX_HAZARD_ROWS) / 2;
            endRow = firstRow + MAX_HAZARD_ROWS;
        }
        int count = 0;
        int endColumn = left + 1 + width;
        for (int y = firstRow; y < endRow; y++) {
            int runStart = Integer.MIN_VALUE;
            for (int x = left + 1; x <= endColumn; x++) {
                boolean ink = x < endColumn
                        && Math.floorMod((x - left) + (y - top), HAZARD_PERIOD) < HAZARD_PERIOD / 2;
                if (ink && runStart == Integer.MIN_VALUE) {
                    runStart = x;
                } else if (!ink && runStart != Integer.MIN_VALUE) {
                    sink.run(runStart, x, y);
                    count++;
                    runStart = Integer.MIN_VALUE;
                }
            }
        }
        return count;
    }

    /** Mouse hover as the style table understands it: pointer over an enabled widget. */
    public static boolean hovered(AbstractWidget widget) {
        return widget.active && widget.isHovered();
    }

    /** Keyboard focus only; a mouse click leaves focus on a key but must not look like hover. */
    public static boolean keyboardFocused(AbstractWidget widget) {
        if (!widget.isFocused()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.getLastInputType().isKeyboard();
    }

    /**
     * Draws a key in [left, right) x [top, bottom), its optional icon and its fitted label.
     *
     * @return the fitted label, so the caller can offer the full text when it was truncated
     *         (an icon-only key with a name reports {@code truncated})
     */
    public static TextFit.Fitted render(GuiGraphics graphics, Font font, int left, int top,
                                        int right, int bottom, Component label, Look look,
                                        Options options) {
        Options safeOptions = options == null ? Options.DEFAULT : options;
        Palette palette = palette(look);
        int height = bottom - top;
        graphics.fill(left, top, right, bottom, palette.fill());
        BattleUiTheme.outline(graphics, left, top, right, bottom, palette.edge());
        if (palette.raised()) {
            graphics.fill(left + 1, top + 1, right - 1, top + 2, TacticalBoardTheme.BEVEL);
            graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, palette.lip());
        }
        if (palette.hatch()) {
            hatch(graphics, left + 1, top + 1, right - 1, bottom - 1);
        }
        if (palette.hazard()) {
            int ink = palette.stripes();
            hazardRuns(left, top, bottom, Math.min(hazardWidth(height), right - left - 2),
                    (runLeft, runRight, y) -> graphics.fill(runLeft, y, runRight, y + 1, ink));
        }
        if (palette.bar() != NONE) {
            graphics.fill(left + 1, top + 1, Math.min(right - 1, left + 1 + barWidth(height)),
                    bottom - 1, palette.bar());
        }
        if (palette.underline() != NONE) {
            graphics.fill(left + 3, bottom - 3, right - 3, bottom - 2, palette.underline());
        }
        if (safeOptions.focusRing()) {
            focusRing(graphics, left, top, right, bottom);
        }
        return renderLabel(graphics, font, left, top, right, bottom, label, look.state(), palette,
                safeOptions);
    }

    private static TextFit.Fitted renderLabel(GuiGraphics graphics, Font font, int left, int top,
                                              int right, int bottom, Component label, State state,
                                              Palette palette, Options options) {
        int padLeft = labelPadLeft(palette, bottom - top);
        int textY = top + Math.max(0, (bottom - top - 8) / 2);
        int badgeWidth = 0;
        Component badge = options.badge();
        if (badge != null && !badge.getString().isEmpty()) {
            badgeWidth = font.width(badge) + 6;
            int badgeLeft = right - LABEL_PAD_RIGHT - badgeWidth;
            graphics.fill(badgeLeft, textY - 1, badgeLeft + badgeWidth, textY + 8,
                    badgeInset(state));
            graphics.drawString(font, badge, badgeLeft + 3, textY,
                    badgeText(state, options.badgeColor()), false);
        }
        boolean hasLabel = label != null && !label.getString().isEmpty();
        TacticalIcon icon = options.icon();
        boolean iconOnly = icon != null && options.iconOnly();
        if (icon == null && hasLabel
                && autoCheck(state, false, font.width(label), right - left, badgeWidth)) {
            // Success never relies on green alone (UI rule 6): the check comes along when it fits.
            icon = TacticalIcon.CHECK;
        }
        boolean drawLabel = hasLabel && !iconOnly;
        int room = labelRoom(left, right, padLeft, icon != null, badgeWidth);
        TextFit.Fitted fitted = drawLabel ? TextFit.fit(font, label, room) : TextFit.Fitted.EMPTY;
        Content content = content(left, top, right, bottom, padLeft, icon != null,
                fitted.width(), badgeWidth, options.align(),
                palette.hazard() ? HAZARD_ICON_SHIFT : 0);
        if (icon != null) {
            icon.draw(graphics, content.iconX(), content.iconY(), palette.text());
        }
        if (!drawLabel) {
            // An icon-only key reports its hidden label as truncated, so the button offers its
            // name as a tooltip (and uiTest's "truncated needs a tooltip" check covers it).
            return iconOnly && hasLabel
                    ? new TextFit.Fitted(TextFit.Fitted.EMPTY.text(), 0, true)
                    : TextFit.Fitted.EMPTY;
        }
        return TextFit.drawFitted(graphics, font, fitted, content.textX(), textY,
                fitted.width(), palette.text(), TextFit.Align.LEFT);
    }

    /** Room left for the label after paddings, the icon and the badge. */
    static int labelRoom(int left, int right, int padLeft, boolean hasIcon, int badgeWidth) {
        return Math.max(0, right - left - padLeft - LABEL_PAD_RIGHT
                - (hasIcon ? TacticalIcon.ADVANCE : 0) - (badgeWidth > 0 ? badgeWidth + 2 : 0));
    }

    /**
     * Places icon and label like the preview's {@code UI.button}: centred as one group (or from
     * the left padding with {@link TextFit.Align#LEFT}); a key with an icon but no drawn label
     * centres the icon on the whole key.
     *
     * @param labelWidth width of the label as it will be drawn (already fitted), 0 for none
     * @param badgeWidth width of the badge including its padding, 0 for none
     */
    public static Content content(int left, int top, int right, int bottom, int padLeft,
                                  boolean hasIcon, int labelWidth, int badgeWidth,
                                  TextFit.Align align) {
        return content(left, top, right, bottom, padLeft, hasIcon, labelWidth, badgeWidth, align,
                0);
    }

    /**
     * {@link #content(int, int, int, int, int, boolean, int, int, TextFit.Align)} where an
     * icon-only key moves its centred icon {@code iconOnlyShift} px right (a danger key moves it
     * 2px off its hazard tab, as the preview's {@code UIX.button}).
     */
    public static Content content(int left, int top, int right, int bottom, int padLeft,
                                  boolean hasIcon, int labelWidth, int badgeWidth,
                                  TextFit.Align align, int iconOnlyShift) {
        int room = labelRoom(left, right, padLeft, hasIcon, badgeWidth);
        int iconAdvance = hasIcon ? TacticalIcon.ADVANCE : 0;
        int shown = Math.min(Math.max(0, labelWidth), room);
        int start;
        if (align == TextFit.Align.LEFT) {
            start = left + padLeft;
        } else {
            start = left + padLeft + Math.floorDiv(room + iconAdvance - (iconAdvance + shown), 2);
        }
        if (hasIcon && shown == 0) {
            start = TacticalIcon.centeredStart(left, right) + iconOnlyShift;
        }
        int iconY = TacticalIcon.centeredStart(top, bottom);
        return new Content(start, iconY, start + iconAdvance, room);
    }

    /**
     * Light-board card background (no label), as the preview's {@code UIX.card}: selected cards
     * are solid with the light left bar ({@link #barWidth}); every other card is raised (top bevel
     * and bottom lip), with a darker outline on hover and the gray fill when disabled.
     */
    public static void renderCard(GuiGraphics graphics, int left, int top, int right, int bottom,
                                  CardState state) {
        renderCard(graphics, left, top, right, bottom, state, false);
    }

    /**
     * {@link #renderCard(GuiGraphics, int, int, int, int, CardState)} with the pointer state: a
     * hovered selected card lights up one step ({@link TacticalBoardTheme#SELECT_HOVER}), like
     * keys and list rows; {@code hovered} on a normal card is the same as {@link CardState#HOVER}.
     */
    public static void renderCard(GuiGraphics graphics, int left, int top, int right, int bottom,
                                  CardState state, boolean hovered) {
        CardState safe = state == null ? CardState.NORMAL : state;
        graphics.fill(left, top, right, bottom, cardFill(safe, hovered));
        BattleUiTheme.outline(graphics, left, top, right, bottom, cardEdge(safe, hovered));
        if (safe == CardState.SELECTED) {
            graphics.fill(left + 1, top + 1, Math.min(right - 1, left + 1 + barWidth(bottom - top)),
                    bottom - 1, TacticalBoardTheme.SELECT_BAR);
        } else {
            graphics.fill(left + 1, top + 1, right - 1, top + 2, TacticalBoardTheme.BEVEL);
            graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, TacticalBoardTheme.CARD_LIP);
        }
    }

    /** Fill of a card; {@code hovered} brightens a selected or normal card, never a disabled one. */
    public static int cardFill(CardState state, boolean hovered) {
        return switch (state == null ? CardState.NORMAL : state) {
            case SELECTED -> hovered ? TacticalBoardTheme.SELECT_HOVER : TacticalBoardTheme.SELECT;
            case DISABLED -> TacticalBoardTheme.CARD_DISABLED;
            case HOVER -> TacticalBoardTheme.CARD_HOVER;
            case NORMAL -> hovered ? TacticalBoardTheme.CARD_HOVER : TacticalBoardTheme.CARD;
        };
    }

    /** Outline colour of a card in the given state. */
    public static int cardEdge(CardState state) {
        return cardEdge(state, false);
    }

    /** Outline colour of a card; a hovered normal card takes the hover outline. */
    public static int cardEdge(CardState state, boolean hovered) {
        return switch (state == null ? CardState.NORMAL : state) {
            case SELECTED -> TacticalBoardTheme.SELECT_EDGE;
            case HOVER -> TacticalBoardTheme.BORDER_DARK;
            case NORMAL -> hovered ? TacticalBoardTheme.BORDER_DARK : TacticalBoardTheme.BORDER;
            case DISABLED -> TacticalBoardTheme.BORDER;
        };
    }

    /**
     * Keyboard focus ring 2px outside the key, two-tone: the 1px {@link TacticalBoardTheme#FOCUS}
     * line of the preview's {@code UI.focusRing} with a 1px {@link TacticalBoardTheme#SELECT_EDGE}
     * line inside it, so one of the two reads on any ground. The near-white Academy and Caesar
     * {@code FOCUS} is only about 1.4:1 on their pale boards, where the dark line carries the ring
     * (7:1 and 10:1); on dark chrome and wells the light line does.
     */
    public static void focusRing(GuiGraphics graphics, int left, int top, int right, int bottom) {
        BattleUiTheme.outline(graphics, left - 2, top - 2, right + 2, bottom + 2,
                TacticalBoardTheme.FOCUS);
        BattleUiTheme.outline(graphics, left - 1, top - 1, right + 1, bottom + 1,
                TacticalBoardTheme.SELECT_EDGE);
    }

    /**
     * Diagonal 1px hatch ("\"), one line every 4px, inside [left, right) x [top, bottom), with
     * a line through the top-left corner. One blit tiles {@link TacticalTextures#UI_HATCH}
     * ({@link TacticalBoardTheme#HATCH}), whatever the size of the key.
     */
    public static void hatch(GuiGraphics graphics, int left, int top, int right, int bottom) {
        TacticalTextures.tile(graphics, TacticalTextures.UI_HATCH, left, top, right, bottom,
                TacticalTextures.UI_HATCH_SIZE, TacticalTextures.UI_HATCH_SIZE);
    }

    /** Whether the hatch covers the pixel {@code (dx, dy)} away from its region's top-left. */
    public static boolean hatchCovers(int dx, int dy) {
        return Math.floorMod(dx - dy, HATCH_SPACING) == 0;
    }

    /** Pitch of the disabled hatch in GUI pixels. */
    public static final int HATCH_SPACING = 4;
    /** Right padding of a key's content. */
    static final int LABEL_PAD_RIGHT = 3;
    /** How far an icon-only danger key moves its icon right, off the hazard tab. */
    static final int HAZARD_ICON_SHIFT = 2;
}
