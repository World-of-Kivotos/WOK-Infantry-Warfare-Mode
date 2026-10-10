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
 */
public final class TacticalButtonStyle {
    /** Marker for "no colour" in {@link Palette}. */
    public static final int NONE = 0;

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
     * Colours of one look. {@code bar} is the 2px left stripe, {@code underline} the control
     * line, {@code lip} the raised-key bevel pair (top {@link TacticalBoardTheme#BEVEL}, bottom lip);
     * {@link #NONE} means "not drawn".
     */
    public record Palette(int fill, int edge, int text, int bar, int underline, int lip,
                          boolean hatch, boolean darkFill) {
        public boolean raised() {
            return lip != NONE;
        }
    }

    /**
     * Optional content settings: label alignment, a right-side badge, the focus ring and a 9×9
     * icon in front of the label. An {@code iconOnly} key draws just the icon, centred on the
     * whole key; its label is still the key's name for narration and is offered as a tooltip.
     */
    public record Options(TextFit.Align align, Component badge, int badgeColor,
                          boolean focusRing, TacticalIcon icon, boolean iconOnly) {
        /** Badge colour {@link TacticalBoardTheme#RENDER_MUTED}: MUTED of the palette drawn in. */
        public static final Options DEFAULT = new Options(TextFit.Align.CENTER, null,
                TacticalBoardTheme.RENDER_MUTED, false);

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

    public static Palette palette(Look look) {
        boolean hover = look.hovered();
        return switch (look.state()) {
            case NORMAL -> new Palette(TacticalBoardTheme.CARD, TacticalBoardTheme.BORDER,
                    TacticalBoardTheme.TEXT, NONE, NONE, TacticalBoardTheme.CARD_LIP, false, false);
            case HOVER -> new Palette(TacticalBoardTheme.CARD_HOVER, TacticalBoardTheme.BORDER_DARK,
                    TacticalBoardTheme.TEXT, NONE, NONE, TacticalBoardTheme.CARD_LIP_HOVER,
                    false, false);
            case SELECTED -> new Palette(hover ? TacticalBoardTheme.SELECT_HOVER
                    : TacticalBoardTheme.SELECT, TacticalBoardTheme.SELECT_EDGE,
                    TacticalBoardTheme.ON_SELECT, TacticalBoardTheme.SELECT_BAR, NONE, NONE,
                    false, true);
            case CURRENT -> new Palette(TacticalBoardTheme.SELECT, TacticalBoardTheme.SELECT_EDGE,
                    TacticalBoardTheme.ON_SELECT, TacticalBoardTheme.SELECT_BAR, NONE, NONE,
                    false, true);
            case DISABLED -> new Palette(TacticalBoardTheme.CARD_DISABLED,
                    TacticalBoardTheme.DISABLED_EDGE, TacticalBoardTheme.DISABLED_TEXT,
                    NONE, NONE, NONE, true, false);
            case DANGER -> new Palette(TacticalBoardTheme.CARD, TacticalBoardTheme.DANGER,
                    TacticalBoardTheme.DANGER, TacticalBoardTheme.DANGER, NONE,
                    TacticalBoardTheme.CARD_LIP, false, false);
            case DANGER_ARMED -> new Palette(TacticalBoardTheme.DANGER, TacticalBoardTheme.DANGER,
                    TacticalBoardTheme.ON_FILL, NONE, NONE, NONE, false, true);
            case SUCCESS -> new Palette(hover ? TacticalBoardTheme.SUCCESS_HOVER
                    : TacticalBoardTheme.SUCCESS, TacticalBoardTheme.SUCCESS_EDGE,
                    TacticalBoardTheme.ON_FILL, NONE, NONE, NONE, false, true);
            case CONTROL -> new Palette(hover ? TacticalBoardTheme.CARD_HOVER
                    : TacticalBoardTheme.CARD, hover ? TacticalBoardTheme.BORDER_DARK
                    : TacticalBoardTheme.BORDER, TacticalBoardTheme.TEXT, NONE,
                    TacticalBoardTheme.ADJUST, hover ? TacticalBoardTheme.CARD_LIP_HOVER
                    : TacticalBoardTheme.CARD_LIP, false, false);
        };
    }

    /** Left padding of the label: wider when a left stripe is drawn. */
    public static int labelPadLeft(Palette palette) {
        return palette.bar() != NONE ? 5 : 3;
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
        graphics.fill(left, top, right, bottom, palette.fill());
        BattleUiTheme.outline(graphics, left, top, right, bottom, palette.edge());
        if (palette.raised()) {
            graphics.fill(left + 1, top + 1, right - 1, top + 2, TacticalBoardTheme.BEVEL);
            graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, palette.lip());
        }
        if (palette.hatch()) {
            hatch(graphics, left + 1, top + 1, right - 1, bottom - 1);
        }
        if (palette.bar() != NONE) {
            graphics.fill(left + 1, top + 1, left + 3, bottom - 1, palette.bar());
        }
        if (palette.underline() != NONE) {
            graphics.fill(left + 3, bottom - 3, right - 3, bottom - 2, palette.underline());
        }
        if (safeOptions.focusRing()) {
            focusRing(graphics, left, top, right, bottom);
        }
        return renderLabel(graphics, font, left, top, right, bottom, label, palette, safeOptions);
    }

    private static TextFit.Fitted renderLabel(GuiGraphics graphics, Font font, int left, int top,
                                              int right, int bottom, Component label,
                                              Palette palette, Options options) {
        int padLeft = labelPadLeft(palette);
        int textY = top + Math.max(0, (bottom - top - 8) / 2);
        int badgeWidth = 0;
        Component badge = options.badge();
        if (badge != null && !badge.getString().isEmpty()) {
            badgeWidth = font.width(badge) + 6;
            int badgeLeft = right - LABEL_PAD_RIGHT - badgeWidth;
            graphics.fill(badgeLeft, textY - 1, badgeLeft + badgeWidth, textY + 8,
                    palette.darkFill() ? TacticalBoardTheme.BADGE_ON_SELECT
                            : TacticalBoardTheme.BADGE_ON_CARD);
            int badgeColor = palette.darkFill() ? TacticalBoardTheme.ON_SELECT
                    : palette.hatch() ? TacticalBoardTheme.DISABLED_TEXT
                    : TacticalBoardTheme.orMuted(options.badgeColor());
            graphics.drawString(font, badge, badgeLeft + 3, textY, badgeColor, false);
        }
        boolean hasLabel = label != null && !label.getString().isEmpty();
        TacticalIcon icon = options.icon();
        boolean iconOnly = icon != null && options.iconOnly();
        boolean drawLabel = hasLabel && !iconOnly;
        int room = labelRoom(left, right, padLeft, icon != null, badgeWidth);
        TextFit.Fitted fitted = drawLabel ? TextFit.fit(font, label, room) : TextFit.Fitted.EMPTY;
        Content content = content(left, top, right, bottom, padLeft, icon != null,
                fitted.width(), badgeWidth, options.align());
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
            start = TacticalIcon.centeredStart(left, right);
        }
        int iconY = TacticalIcon.centeredStart(top, bottom);
        return new Content(start, iconY, start + iconAdvance, room);
    }

    /**
     * Light-board card background (no label), as the preview's {@code UI.card}: selected cards are
     * blue with the light left stripe; every other card is raised (top bevel and bottom lip), with
     * a darker outline on hover and the gray fill when disabled.
     */
    public static void renderCard(GuiGraphics graphics, int left, int top, int right, int bottom,
                                  CardState state) {
        CardState safe = state == null ? CardState.NORMAL : state;
        boolean selected = safe == CardState.SELECTED;
        boolean hover = safe == CardState.HOVER;
        int fill = selected ? TacticalBoardTheme.SELECT
                : safe == CardState.DISABLED ? TacticalBoardTheme.CARD_DISABLED
                : hover ? TacticalBoardTheme.CARD_HOVER : TacticalBoardTheme.CARD;
        graphics.fill(left, top, right, bottom, fill);
        BattleUiTheme.outline(graphics, left, top, right, bottom, cardEdge(safe));
        if (selected) {
            graphics.fill(left + 1, top + 1, left + 3, bottom - 1, TacticalBoardTheme.SELECT_BAR);
        } else {
            graphics.fill(left + 1, top + 1, right - 1, top + 2, TacticalBoardTheme.BEVEL);
            graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, TacticalBoardTheme.CARD_LIP);
        }
    }

    /** Outline colour of a card in the given state. */
    public static int cardEdge(CardState state) {
        return switch (state == null ? CardState.NORMAL : state) {
            case SELECTED -> TacticalBoardTheme.SELECT_EDGE;
            case HOVER -> TacticalBoardTheme.BORDER_DARK;
            case NORMAL, DISABLED -> TacticalBoardTheme.BORDER;
        };
    }

    /** Keyboard focus ring 2px outside the key. */
    public static void focusRing(GuiGraphics graphics, int left, int top, int right, int bottom) {
        BattleUiTheme.outline(graphics, left - 2, top - 2, right + 2, bottom + 2,
                TacticalBoardTheme.FOCUS);
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
}
