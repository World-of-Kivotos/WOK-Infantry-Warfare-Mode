package com.wok.infantry.client.screen;

import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.client.hud.TacticalHud;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Static drawing primitives of the tactical-tablet style, ported from the layout preview
 * ({@code ui-preview/kit/ui.js}): panel, section, well, list rows, cards, the keyboard focus ring,
 * item slots, chips, meters, LEDs, empty states, key-value lines, scrollbars, pagers and the
 * text-field frame. Interactive parts built on them are {@link TacticalList},
 * {@link TacticalBoardSlider}, {@link TacticalStepper} and {@link TacticalTextField}.
 *
 * <p>Everything is given in the logical coordinates of the current pose (for a
 * {@link TacticalScreen} that is the layout space after the minimum 2x), takes its colours from
 * {@link TacticalBoardTheme}, draws text through {@link TextFit} without a shadow and never draws
 * outside the rectangle it was given, except where noted (a panel's 1px drop shadow and the focus
 * ring 2px outside its control). Geometry decisions live in pure methods so they can be tested
 * without a client.
 */
public final class TacticalDraw {
    /** Pitch of stacked text lines (unifont glyphs are 8px tall). */
    public static final int LINE_HEIGHT = 10;
    /** Height of a status chip. */
    public static final int CHIP_HEIGHT = 11;
    /** Side of an item slot at scale 1. */
    public static final int SLOT_SIZE = 18;
    /** Side of a status LED. */
    public static final int LED_SIZE = 4;
    /** Smallest scrollbar thumb. */
    public static final int MIN_THUMB = 6;
    /** Light top line of a status LED (preview {@code UI.led}). */
    static final int LED_GLINT = 0x40FFFFFF;

    private TacticalDraw() {
    }

    // ---- panels ---------------------------------------------------------------------------------

    /**
     * Options of a raised board panel (preview {@code UI.panel}): optional section title with a
     * right-aligned meta text, the section accent, the panel fill and the content padding
     * ({@link #DEFAULT_PAD} = the size class's {@code pad}).
     */
    public record PanelStyle(Component title, Component meta, int metaColor, int accent, int fill,
                             int pad) {
        public static final int DEFAULT_PAD = -1;

        /** Untitled panel on {@link TacticalBoardTheme#BOARD_ALT}. */
        public static PanelStyle plain() {
            return new PanelStyle(null, null, TacticalBoardTheme.LIGHT_MUTED,
                    TacticalBoardTheme.SECTION, TacticalBoardTheme.BOARD_ALT, DEFAULT_PAD);
        }

        /** Panel with an orange section title. */
        public static PanelStyle titled(Component title) {
            return plain().withTitle(title);
        }

        public PanelStyle withTitle(Component newTitle) {
            return new PanelStyle(newTitle, meta, metaColor, accent, fill, pad);
        }

        public PanelStyle withMeta(Component newMeta) {
            return new PanelStyle(title, newMeta, metaColor, accent, fill, pad);
        }

        public PanelStyle withMeta(Component newMeta, int color) {
            return new PanelStyle(title, newMeta, color, accent, fill, pad);
        }

        public PanelStyle withAccent(int color) {
            return new PanelStyle(title, meta, metaColor, color, fill, pad);
        }

        public PanelStyle withFill(int color) {
            return new PanelStyle(title, meta, metaColor, accent, color, pad);
        }

        public PanelStyle withPad(int newPad) {
            return new PanelStyle(title, meta, metaColor, accent, fill, newPad);
        }

        public boolean hasTitle() {
            return title != null && !title.getString().isEmpty();
        }
    }

    /**
     * Pure: content rectangle of a panel drawn in {@code bounds} (what {@link #panel} returns),
     * below the optional section strip and inside the padding.
     */
    public static UiRect panelContent(UiRect bounds, TacticalShellLayout.Metrics metrics,
                                      PanelStyle style) {
        PanelStyle safe = style == null ? PanelStyle.plain() : style;
        int pad = safe.pad() >= 0 ? safe.pad() : metrics.pad();
        int top = safe.hasTitle() ? bounds.top() + 1 + metrics.sectionHeight() + 1 : bounds.top() + 2;
        return new UiRect(bounds.left() + pad, top + pad - 1, bounds.right() - pad,
                bounds.bottom() - pad);
    }

    /**
     * Raised board panel with an optional section title (preview {@code UI.panel}). The drop
     * shadow falls 1px right of and below {@code bounds}.
     *
     * @return the content rectangle (see {@link #panelContent})
     */
    public static UiRect panel(GuiGraphics graphics, Font font, UiRect bounds,
                               TacticalShellLayout.Metrics metrics, PanelStyle style) {
        PanelStyle safe = style == null ? PanelStyle.plain() : style;
        if (!bounds.isEmpty()) {
            UiLayoutProbe.box(graphics, "panel", safe.title(), bounds.left(), bounds.top(),
                    bounds.right(), bounds.bottom());
            TacticalBoardTheme.raisedPanel(graphics, bounds.left(), bounds.top(), bounds.right(),
                    bounds.bottom(), safe.fill());
            if (safe.hasTitle()) {
                section(graphics, font, new UiRect(bounds.left() + 1, bounds.top() + 1,
                                bounds.right() - 1, bounds.top() + 1 + metrics.sectionHeight()),
                        safe.title(), safe.meta(), safe.metaColor(), safe.accent());
            }
        }
        return panelContent(bounds, metrics, safe);
    }

    /**
     * Section strip (preview {@code UI.section}): dark plate, 3px accent tab, light title that is
     * ellipsized before it reaches the right-aligned meta text (at most 40% of the width).
     */
    public static void section(GuiGraphics graphics, Font font, UiRect bounds, Component title,
                               Component meta, int metaColor, int accent) {
        TacticalBoardTheme.sectionHeader(graphics, font, title == null ? Component.empty() : title,
                meta == null ? Component.empty() : meta, metaColor, bounds.left(), bounds.top(),
                bounds.right(), bounds.bottom(), accent);
    }

    /** Section strip with the orange section accent and no meta text. */
    public static void section(GuiGraphics graphics, Font font, UiRect bounds, Component title) {
        section(graphics, font, bounds, title, null, TacticalBoardTheme.LIGHT_MUTED,
                TacticalBoardTheme.SECTION);
    }

    /**
     * Dark recessed well for lists, previews and maps (preview {@code UI.well}): the top edge is a
     * dark line, the bottom and right edges a lighter one.
     */
    public static void well(GuiGraphics graphics, UiRect bounds) {
        if (bounds.isEmpty()) {
            return;
        }
        int l = bounds.left();
        int t = bounds.top();
        int r = bounds.right();
        int b = bounds.bottom();
        graphics.fill(l, t, r, b, TacticalBoardTheme.WELL);
        graphics.fill(l, t, r, t + 1, TacticalBoardTheme.RIVET);
        graphics.fill(l, b - 1, r, b, TacticalBoardTheme.WELL_LIGHT_EDGE);
        graphics.fill(r - 1, t, r, b, TacticalBoardTheme.WELL_LIGHT_EDGE);
    }

    /** Inside of a well, clear of its edge lines. */
    public static UiRect wellInner(UiRect bounds) {
        return bounds.inset(1);
    }

    /** Translucent 1px hairline separator ({@link TacticalBoardTheme#EDGE}) on boards and panels. */
    public static void divider(GuiGraphics graphics, int left, int right, int y) {
        if (right > left) {
            graphics.fill(left, y, right, y + 1, TacticalBoardTheme.EDGE);
        }
    }

    // ---- list rows ------------------------------------------------------------------------------

    /** Interaction state of a list row inside a well. */
    public record RowState(boolean selected, boolean hovered, boolean disabled, boolean alt) {
        public static final RowState NORMAL = new RowState(false, false, false, false);

        public RowState withSelected(boolean value) {
            return new RowState(value, hovered, disabled, alt);
        }

        public RowState withHovered(boolean value) {
            return new RowState(selected, value, disabled, alt);
        }

        public RowState withDisabled(boolean value) {
            return new RowState(selected, hovered, value, alt);
        }

        public RowState withAlt(boolean value) {
            return new RowState(selected, hovered, disabled, value);
        }

        /** Hover only shows on a row that is neither selected nor disabled. */
        public boolean showsHover() {
            return hovered && !selected && !disabled;
        }
    }

    /**
     * Content of a default list row (preview {@code UI.row}): title, optional second line (only
     * drawn when the row is at least 20px tall), right-aligned value, a 9×9 icon or an item, a 2px
     * lead colour, and why the row is disabled ({@code null} = enabled) or an extra tooltip.
     * Colour fields use 0 for "default".
     */
    public record RowSpec(Component title, Component sub, Component right, int rightColor,
                          TacticalIcon icon, int iconColor, ItemStack item, int lead,
                          Component disabledReason, Component tooltip) {
        public static RowSpec of(Component title) {
            return new RowSpec(title == null ? Component.empty() : title, null, null, 0, null, 0,
                    null, 0, null, null);
        }

        public static RowSpec of(String title) {
            return of(Component.literal(title == null ? "" : title));
        }

        public RowSpec withSub(Component value) {
            return new RowSpec(title, value, right, rightColor, icon, iconColor, item, lead,
                    disabledReason, tooltip);
        }

        public RowSpec withRight(Component value) {
            return withRight(value, 0);
        }

        public RowSpec withRight(Component value, int color) {
            return new RowSpec(title, sub, value, color, icon, iconColor, item, lead,
                    disabledReason, tooltip);
        }

        public RowSpec withIcon(TacticalIcon value) {
            return withIcon(value, 0);
        }

        public RowSpec withIcon(TacticalIcon value, int color) {
            return new RowSpec(title, sub, right, rightColor, value, color, item, lead,
                    disabledReason, tooltip);
        }

        public RowSpec withItem(ItemStack value) {
            return new RowSpec(title, sub, right, rightColor, icon, iconColor, value, lead,
                    disabledReason, tooltip);
        }

        /** 2px colour stripe at the left edge (hidden while the row is selected). */
        public RowSpec withLead(int color) {
            return new RowSpec(title, sub, right, rightColor, icon, iconColor, item, color,
                    disabledReason, tooltip);
        }

        /** Disables the row; the reason is offered as its tooltip. {@code null} enables it. */
        public RowSpec withDisabledReason(Component reason) {
            return new RowSpec(title, sub, right, rightColor, icon, iconColor, item, lead, reason,
                    tooltip);
        }

        public RowSpec withTooltip(Component value) {
            return new RowSpec(title, sub, right, rightColor, icon, iconColor, item, lead,
                    disabledReason, value);
        }

        public boolean disabled() {
            return disabledReason != null;
        }

        boolean hasItem() {
            return item != null && !item.isEmpty();
        }

        boolean hasSub() {
            return sub != null && !sub.getString().isEmpty();
        }

        boolean hasRight() {
            return right != null && !right.getString().isEmpty();
        }
    }

    /**
     * Pure placement of a row's content.
     *
     * @param contentLeft left of the title (after the icon or item)
     * @param titleY      text top of the title
     * @param subY        text top of the second line (meaningful only with {@code twoLines})
     * @param titleRoom   width the title may take before it is ellipsized
     * @param subRoom     width of the second line
     * @param rightX      left of the right-aligned value
     * @param rightRoom   width of the right value (at most 45% of the row)
     * @param iconY       top of the 9×9 icon
     * @param itemY       top of the 16×16 item
     */
    public record RowLayout(int contentLeft, int titleY, int subY, int titleRoom, int subRoom,
                            int rightX, int rightRoom, boolean twoLines, int iconY, int itemY) {
    }

    /** What {@link #row} had to shorten, so the caller can offer the full text as a tooltip. */
    public record RowText(RowLayout layout, boolean titleTruncated, boolean subTruncated,
                          boolean rightTruncated) {
        public boolean truncated() {
            return titleTruncated || subTruncated || rightTruncated;
        }
    }

    /**
     * Pure: background colour of a well row: selected blue, hovered (not when disabled), empty
     * slot, alternate stripe, or the plain row colour.
     */
    public static int rowFill(RowState state, boolean empty) {
        RowState safe = state == null ? RowState.NORMAL : state;
        if (safe.selected()) {
            return TacticalBoardTheme.SELECT;
        }
        if (safe.showsHover()) {
            return TacticalBoardTheme.ROW_HOVER;
        }
        if (empty) {
            return TacticalBoardTheme.WELL;
        }
        return safe.alt() ? TacticalBoardTheme.WELL_ROW_ALT : TacticalBoardTheme.WELL_ROW;
    }

    /** Background of a custom-drawn well row (preview {@code UI.rowBg}); {@code lead} 0 = none. */
    public static void rowBg(GuiGraphics graphics, UiRect bounds, RowState state, int lead) {
        rowBg(graphics, bounds, state, lead, false);
    }

    /** {@link #rowBg(GuiGraphics, UiRect, RowState, int)} with the empty-slot fill. */
    public static void rowBg(GuiGraphics graphics, UiRect bounds, RowState state, int lead,
                             boolean empty) {
        if (bounds.isEmpty()) {
            return;
        }
        RowState safe = state == null ? RowState.NORMAL : state;
        graphics.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(),
                rowFill(safe, empty));
        int stripe = safe.selected() ? TacticalBoardTheme.SELECT_BAR : lead;
        if (stripe != 0) {
            graphics.fill(bounds.left(), bounds.top(), Math.min(bounds.right(), bounds.left() + 2),
                    bounds.bottom(), stripe);
        }
    }

    /**
     * Pure layout of a default row (preview {@code UI.row}); {@code rightWidth} is the natural
     * width of the right value (0 for none).
     */
    public static RowLayout rowLayout(UiRect bounds, boolean hasIcon, boolean hasItem,
                                      boolean hasSub, int rightWidth) {
        int x = bounds.left() + 5;
        if (hasItem) {
            x += 18;
        } else if (hasIcon) {
            x += 12;
        }
        int width = bounds.width();
        int rightRoom = rightWidth > 0 ? Math.min(rightWidth, width * 45 / 100) : 0;
        boolean twoLines = hasSub && bounds.height() >= 20;
        int centeredY = bounds.top() + Math.floorDiv(bounds.height() - 8, 2);
        // Preview: the title keeps 8px clear of the right edge (or of the right value).
        int titleRoom = bounds.right() - x - rightRoom - 8;
        return new RowLayout(x, twoLines ? bounds.top() + 2 : centeredY, bounds.top() + 11,
                Math.max(0, titleRoom), Math.max(0, bounds.right() - x - 4),
                bounds.right() - rightRoom - 4, rightRoom, twoLines,
                bounds.top() + Math.floorDiv(bounds.height() - TacticalIcon.SIZE, 2),
                bounds.top() + Math.floorDiv(bounds.height() - 16, 2));
    }

    /**
     * Default list row inside a well (preview {@code UI.row}): background, lead or selection
     * stripe, icon or item, title, optional second line and right value. Selected rows use
     * {@link TacticalBoardTheme#ON_SELECT}, disabled rows {@link TacticalBoardTheme#FAINT}.
     */
    public static RowText row(GuiGraphics graphics, Font font, UiRect bounds, RowSpec spec,
                              RowState state) {
        RowState safe = state == null ? RowState.NORMAL : state;
        boolean disabled = safe.disabled() || spec.disabled();
        RowState drawn = safe.withDisabled(disabled);
        rowBg(graphics, bounds, drawn, spec.lead());
        int rightWidth = spec.hasRight() ? font.width(spec.right()) : 0;
        RowLayout layout = rowLayout(bounds, spec.icon() != null, spec.hasItem(), spec.hasSub(),
                rightWidth);
        if (spec.hasItem()) {
            int x = bounds.left() + 5;
            graphics.fill(x - 2, Math.max(bounds.top(), layout.itemY() - 1), x + 16,
                    Math.min(bounds.bottom(), layout.itemY() + 17), TacticalBoardTheme.CELL);
            graphics.renderItem(spec.item(), x - 1, layout.itemY());
        } else if (spec.icon() != null) {
            int iconColor = disabled ? TacticalBoardTheme.FAINT
                    : spec.iconColor() != 0 ? spec.iconColor() : TacticalBoardTheme.LIGHT_MUTED;
            spec.icon().draw(graphics, bounds.left() + 5, layout.iconY(), iconColor);
        }
        int main = disabled ? TacticalBoardTheme.FAINT
                : drawn.selected() ? TacticalBoardTheme.ON_SELECT : TacticalBoardTheme.LIGHT;
        boolean titleCut = TextFit.draw(graphics, font, spec.title(), layout.contentLeft(),
                layout.titleY(), layout.titleRoom(), main, TextFit.Align.LEFT).truncated();
        boolean subCut = false;
        if (layout.twoLines()) {
            subCut = TextFit.draw(graphics, font, spec.sub(), layout.contentLeft(), layout.subY(),
                    layout.subRoom(), drawn.selected() ? TacticalBoardTheme.SELECT_SUB
                            : TacticalBoardTheme.LIGHT_MUTED, TextFit.Align.LEFT).truncated();
        } else if (spec.hasSub()) {
            subCut = true;
        }
        boolean rightCut = false;
        if (spec.hasRight()) {
            int rightColor = drawn.selected() ? TacticalBoardTheme.ON_SELECT
                    : spec.rightColor() != 0 ? spec.rightColor() : TacticalBoardTheme.LIGHT_MUTED;
            int centeredY = bounds.top() + Math.floorDiv(bounds.height() - 8, 2);
            rightCut = TextFit.draw(graphics, font, spec.right(), layout.rightX(), centeredY,
                    layout.rightRoom(), rightColor, TextFit.Align.LEFT).truncated();
        }
        return new RowText(layout, titleCut, subCut, rightCut);
    }

    // ---- cards and focus ------------------------------------------------------------------------

    /** Selectable light-board card background (preview {@code UI.card}). */
    public static void card(GuiGraphics graphics, UiRect bounds, TacticalButtonStyle.CardState state) {
        if (!bounds.isEmpty()) {
            TacticalButtonStyle.renderCard(graphics, bounds.left(), bounds.top(), bounds.right(),
                    bounds.bottom(), state);
        }
    }

    /**
     * Keyboard focus marker 2px outside {@code bounds} (preview {@code UI.focusRing}); custom-drawn
     * controls call this instead of drawing their own frame.
     */
    public static void focusRing(GuiGraphics graphics, UiRect bounds) {
        TacticalButtonStyle.focusRing(graphics, bounds.left(), bounds.top(), bounds.right(),
                bounds.bottom());
    }

    /** Draws the focus ring around {@code widget} only while it has keyboard focus. */
    public static void focusRingIfKeyboard(GuiGraphics graphics, AbstractWidget widget) {
        if (TacticalButtonStyle.keyboardFocused(widget)) {
            TacticalButtonStyle.focusRing(graphics, widget.getX(), widget.getY(),
                    widget.getX() + widget.getWidth(), widget.getY() + widget.getHeight());
        }
    }

    // ---- input field ----------------------------------------------------------------------------

    /**
     * Pure: outline of a text field (preview {@code UI.textField}): an error wins, then a field
     * that cannot be edited, then the focus; otherwise {@link TacticalBoardTheme#INPUT_EDGE}.
     */
    public static int inputEdge(boolean focused, boolean error, boolean editable) {
        if (error) {
            return TacticalBoardTheme.DANGER_B;
        }
        if (!editable) {
            return TacticalBoardTheme.WELL_EDGE;
        }
        return focused ? TacticalBoardTheme.SELECT_B : TacticalBoardTheme.INPUT_EDGE;
    }

    /** Dark text-field frame: {@link TacticalBoardTheme#WELL} fill and a 1px {@code edge}. */
    public static void inputFrame(GuiGraphics graphics, UiRect bounds, int edge) {
        if (bounds.isEmpty()) {
            return;
        }
        graphics.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(),
                TacticalBoardTheme.WELL);
        BattleUiTheme.outline(graphics, bounds.left(), bounds.top(), bounds.right(),
                bounds.bottom(), edge);
    }

    // ---- scrollbar and pager --------------------------------------------------------------------

    /**
     * Pure: the scrollbar thumb inside {@code track} for {@code visible} of {@code total} units
     * scrolled by {@code offset}; {@link UiRect#EMPTY} when everything is visible. The thumb is
     * at least {@value #MIN_THUMB}px tall (never taller than the track).
     */
    public static UiRect scrollThumb(UiRect track, int total, int visible, int offset) {
        if (total <= visible || total <= 0 || track.isEmpty()) {
            return UiRect.EMPTY;
        }
        int height = track.height();
        int thumb = Math.min(height, Math.max(MIN_THUMB,
                (int) Math.floor((double) height * Math.max(0, visible) / total)));
        int range = Math.max(1, total - visible);
        int clamped = Math.max(0, Math.min(range, offset));
        int top = track.top() + (int) Math.floor((double) (height - thumb) * clamped / range);
        return new UiRect(track.left(), top, track.right(), top + thumb);
    }

    /** Scrollbar (preview {@code UI.scrollbar}); nothing is drawn when everything is visible. */
    public static void scrollbar(GuiGraphics graphics, UiRect track, int total, int visible,
                                 int offset) {
        UiRect thumb = scrollThumb(track, total, visible, offset);
        if (thumb.isEmpty()) {
            return;
        }
        graphics.fill(track.left(), track.top(), track.right(), track.bottom(),
                TacticalBoardTheme.SCROLL_TRACK);
        graphics.fill(thumb.left(), thumb.top(), thumb.right(), thumb.bottom(),
                TacticalBoardTheme.ACCENT);
    }

    /** Parts of a {@code ‹ n / m ›} pager. */
    public record PagerLayout(UiRect back, UiRect label, UiRect next) {
    }

    /** Pure: pager keys are {@code min(h + 2, 18)} wide (preview {@code UI.pager}). */
    public static PagerLayout pagerLayout(UiRect bounds) {
        int key = Math.max(0, Math.min(Math.min(bounds.height() + 2, 18), bounds.width() / 2));
        UiRect back = bounds.leftSlice(key);
        UiRect next = bounds.rightSlice(key);
        return new PagerLayout(back, new UiRect(back.right(), bounds.top(),
                Math.max(back.right(), next.left()), bounds.bottom()), next);
    }

    /** Pure: -1 on the back key, +1 on the next key, 0 elsewhere. */
    public static int pagerHit(UiRect bounds, double x, double y) {
        PagerLayout layout = pagerLayout(bounds);
        if (layout.back().contains(x, y)) {
            return -1;
        }
        return layout.next().contains(x, y) ? 1 : 0;
    }

    /**
     * Static {@code ‹ n / m ›} pager on a board (preview {@code UI.pager}): orange-underlined
     * control keys that turn disabled at either end.
     *
     * @param page    zero-based page
     * @param hovered -1 back key hovered, +1 next key hovered, 0 none
     */
    public static void pager(GuiGraphics graphics, Font font, UiRect bounds, int page, int pages,
                             int hovered) {
        PagerLayout layout = pagerLayout(bounds);
        int safePages = Math.max(1, pages);
        int safePage = Math.max(0, Math.min(safePages - 1, page));
        iconKey(graphics, font, layout.back(), TacticalIcon.BACK, safePage > 0, hovered < 0);
        iconKey(graphics, font, layout.next(), TacticalIcon.NEXT, safePage < safePages - 1,
                hovered > 0);
        TextFit.draw(graphics, font, (safePage + 1) + " / " + safePages, layout.label().left(),
                bounds.top() + Math.floorDiv(bounds.height() - 8, 2), layout.label().width(),
                TacticalBoardTheme.TEXT, TextFit.Align.CENTER);
    }

    /** Icon-only control key: orange underline when usable, disabled look otherwise. */
    static void iconKey(GuiGraphics graphics, Font font, UiRect bounds, TacticalIcon icon,
                        boolean enabled, boolean hovered) {
        if (bounds.isEmpty()) {
            return;
        }
        TacticalButtonStyle.Look look = TacticalButtonStyle.resolve(enabled, false, false,
                TacticalButtonStyle.Variant.CONTROL, enabled && hovered, false);
        TacticalButtonStyle.render(graphics, font, bounds.left(), bounds.top(), bounds.right(),
                bounds.bottom(), Component.empty(), look,
                TacticalButtonStyle.Options.DEFAULT.withIconOnly(icon));
    }

    // ---- small parts ----------------------------------------------------------------------------

    /** Width of a status chip for {@code text}. */
    public static int chipWidth(Font font, Component text) {
        return font.width(text) + 6;
    }

    /**
     * Status chip such as {@code 名额 2/4} (preview {@code UI.chip}): coloured outline, a light
     * wash of the colour on boards or the dark feedback plate on dark surfaces.
     *
     * @return the x just after the chip
     */
    public static int chip(GuiGraphics graphics, Font font, int x, int y, Component text, int color,
                           boolean onDark) {
        int width = chipWidth(font, text);
        graphics.fill(x, y, x + width, y + CHIP_HEIGHT,
                onDark ? TacticalBoardTheme.FEEDBACK_BG : TacticalHud.withAlpha(color, 0x26));
        BattleUiTheme.outline(graphics, x, y, x + width, y + CHIP_HEIGHT, color);
        graphics.drawString(font, text, x + 3, y + 2,
                onDark ? color : TacticalHud.mix(color, 0xFF000000, 0.25D), false);
        UiLayoutProbe.rawText(graphics, font, text, x + 3, y + 2);
        return x + width;
    }

    /** Meter on the HUD track (preview {@code UI.meter}). */
    public static void meter(GuiGraphics graphics, UiRect bounds, float ratio, int color) {
        meter(graphics, bounds, ratio, color, TacticalBoardTheme.HUD_TRACK, 0);
    }

    /** Meter with its own track colour and {@code ticks} equal parts (dividers when &gt; 1). */
    public static void meter(GuiGraphics graphics, UiRect bounds, float ratio, int color, int track,
                             int ticks) {
        TacticalHud.meter(graphics, bounds.left(), bounds.top(), bounds.right(), bounds.bottom(),
                ratio, color, track, ticks, false);
    }

    /** 4×4 status light with a faint top glint (preview {@code UI.led}). */
    public static void led(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + LED_SIZE, y + LED_SIZE, color);
        graphics.fill(x, y, x + LED_SIZE, y + 1, LED_GLINT);
    }

    /**
     * Key-value line of a detail card (preview {@code UI.kv}): muted key (at most 45% of the
     * width), value right-aligned in {@code valueColor} (0 = {@link TacticalBoardTheme#TEXT}).
     */
    public static void kv(GuiGraphics graphics, Font font, int x, int y, int width, Component key,
                          Component value, int valueColor) {
        int keyWidth = Math.min(font.width(key), width * 45 / 100);
        TextFit.draw(graphics, font, key, x, y, keyWidth, TacticalBoardTheme.MUTED,
                TextFit.Align.LEFT);
        TextFit.draw(graphics, font, value, x + keyWidth + 4, y, Math.max(0, width - keyWidth - 4),
                valueColor != 0 ? valueColor : TacticalBoardTheme.TEXT, TextFit.Align.RIGHT);
    }

    /**
     * Wrapped paragraph without shadow, at most {@code maxLines} lines (0 = no limit); a cut
     * paragraph ends in "…".
     *
     * @return the number of lines drawn
     */
    public static int paragraph(GuiGraphics graphics, Font font, String text, int x, int y,
                                int width, int color, int maxLines) {
        if (text == null || text.isBlank() || width <= 0) {
            return 0;
        }
        List<String> lines = TextFit.wrap(font, text, width, maxLines);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), x, y + index * LINE_HEIGHT, color, false);
            UiLayoutProbe.rawText(graphics, font, lines.get(index), x, y + index * LINE_HEIGHT);
        }
        return lines.size();
    }

    // ---- item slots -----------------------------------------------------------------------------

    /**
     * 18×18 item slot (preview {@code UI.slot}): cell fill, blue when selected, red outline with a
     * warning icon when it has an error and no item, the real item and a shadowless count.
     */
    public static void slot(GuiGraphics graphics, Font font, int x, int y, ItemStack item,
                            boolean selected, boolean error, int count) {
        slot(graphics, font, x, y, 1, item, selected, error, count);
    }

    /** {@link #slot} enlarged by an integer {@code scale} (the item is drawn at 16·scale px). */
    public static void slot(GuiGraphics graphics, Font font, int x, int y, int scale, ItemStack item,
                            boolean selected, boolean error, int count) {
        int k = Math.max(1, scale);
        int size = SLOT_SIZE * k;
        graphics.fill(x, y, x + size, y + size,
                selected ? TacticalBoardTheme.SELECT : TacticalBoardTheme.CELL);
        BattleUiTheme.outline(graphics, x, y, x + size, y + size, slotEdge(selected, error));
        boolean hasItem = item != null && !item.isEmpty();
        if (error && !hasItem) {
            int inset = slotIconInset(size);
            TacticalIcon.WARN.draw(graphics, x + inset, y + inset,
                    selected ? TacticalBoardTheme.ON_SELECT : TacticalBoardTheme.DANGER_B);
        }
        if (hasItem) {
            if (k == 1) {
                graphics.renderItem(item, x + 1, y + 1);
            } else {
                graphics.pose().pushPose();
                graphics.pose().translate(x + k, y + k, 0.0F);
                graphics.pose().scale(k, k, 1.0F);
                graphics.renderItem(item, 0, 0);
                graphics.pose().popPose();
            }
        }
        if (count > 1) {
            String text = String.valueOf(count);
            graphics.pose().pushPose();
            // Above the item model, as vanilla's own stack count.
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            graphics.drawString(font, text, x + size - font.width(text), y + size - 8,
                    TacticalBoardTheme.LIGHT, false);
            if (UiLayoutProbe.recording()) {
                UiLayoutProbe.rawText(graphics, font, text, x + size - font.width(text),
                        y + size - 8);
            }
            graphics.pose().popPose();
        }
    }

    /**
     * Pure: offset of the 9×9 warning icon in an empty slot of side {@code size}, rounded towards
     * the bottom-right like the preview ({@code UI.slot} draws it at {@code x + 5} in 18px).
     */
    static int slotIconInset(int size) {
        return Math.max(0, Math.floorDiv(size - TacticalIcon.SIZE + 1, 2));
    }

    /** Pure: slot outline colour; an error wins over the selection. */
    public static int slotEdge(boolean selected, boolean error) {
        if (error) {
            return TacticalBoardTheme.DANGER_B;
        }
        return selected ? TacticalBoardTheme.SELECT_BAR : TacticalBoardTheme.CELL_EDGE;
    }

    // ---- empty state ----------------------------------------------------------------------------

    /**
     * Pure placement of an empty state: icon on top, title, then {@code hintLines} hint lines,
     * centred vertically (at least 4px from the top).
     */
    public record EmptyLayout(int iconX, int iconY, int titleY, int firstHintY, int hintLines) {
    }

    /** Pure: how many hint lines fit below the icon and title inside {@code bounds}. */
    public static int emptyHintCapacity(UiRect bounds) {
        // 4 top + 12 icon + 11 title, then lines on a 10px pitch whose 8px glyphs must fit.
        int room = bounds.height() - 4 - 12 - 11 - 8;
        return room < 0 ? 0 : room / LINE_HEIGHT + 1;
    }

    public static EmptyLayout emptyLayout(UiRect bounds, int hintLines) {
        int lines = Math.max(0, Math.min(hintLines, emptyHintCapacity(bounds)));
        int height = 12 + 10 + lines * LINE_HEIGHT;
        int top = bounds.top() + Math.max(4, Math.floorDiv(bounds.height() - height, 2));
        return new EmptyLayout(bounds.left() + bounds.width() / 2 - 4, top, top + 12, top + 23,
                lines);
    }

    /**
     * Empty or waiting state inside a region (preview {@code UI.empty}): a 9×9 icon, a centred
     * title and a centred, wrapped hint. Hint lines that do not fit are cut, the last kept line
     * ending in "…". {@code onLight} picks board colours instead of well colours.
     */
    public static void empty(GuiGraphics graphics, Font font, UiRect bounds, TacticalIcon icon,
                             Component title, Component hint, boolean onLight) {
        if (bounds.isEmpty()) {
            return;
        }
        int textWidth = Math.max(1, bounds.width() - 12);
        String hintText = hint == null ? "" : hint.getString();
        List<String> lines = hintText.isBlank() ? List.of()
                : TextFit.wrap(font, hintText, textWidth, 0);
        EmptyLayout layout = emptyLayout(bounds, lines.size());
        if (layout.hintLines() < lines.size()) {
            lines = layout.hintLines() == 0 ? List.of()
                    : TextFit.wrap(font, hintText, textWidth, layout.hintLines());
        }
        int muted = onLight ? TacticalBoardTheme.MUTED : TacticalBoardTheme.LIGHT_MUTED;
        (icon == null ? TacticalIcon.INFO : icon).draw(graphics, layout.iconX(), layout.iconY(),
                muted);
        if (layout.titleY() + 8 <= bounds.bottom()) {
            TextFit.draw(graphics, font, title == null ? Component.empty() : title,
                    bounds.left() + 6, layout.titleY(), textWidth,
                    onLight ? TacticalBoardTheme.TEXT : TacticalBoardTheme.LIGHT, TextFit.Align.CENTER);
        }
        int y = layout.firstHintY();
        for (String line : lines) {
            TextFit.draw(graphics, font, line, bounds.left() + 6, y, textWidth, muted,
                    TextFit.Align.CENTER);
            y += LINE_HEIGHT;
        }
    }
}
