package com.wok.infantry.client.screen;

import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Dark-well list of the tactical-tablet style: one keyboard-focusable widget that shows rows (and
 * optional group titles) of a list of items.
 *
 * <p><b>Whole rows only.</b> The view is cut between rows, never through one. When the rows do not
 * fit, the last line of the well says how many items are hidden ({@code ↓ 还有 n 项}, or
 * {@code ↑ 上方还有 n 项} at the end of the list) and a thin scrollbar shows the position
 * ({@link Overflow}). A group title never ends the view without its first row.
 *
 * <p><b>Input.</b> Mouse wheel scrolls by one row; clicking a row selects it (disabled rows too, so
 * the screen can explain why, the callback decides what happens); clicking the "more" line pages
 * on; the scrollbar can be clicked and dragged. With keyboard focus ↑/↓ move the selection, Home /
 * End jump to the ends, Page Up / Page Down move a page and Enter / Space activate the selected
 * row. ↑ on the first row and ↓ on the last are not consumed, so arrow navigation can leave.
 *
 * <p><b>Rows.</b> A presenter turns each item into a {@link TacticalDraw.RowSpec} (title, second
 * line, right value, icon or item, lead colour, disabled reason, tooltip) that is drawn with
 * {@link TacticalDraw#row}; a {@link RowRenderer} can draw the row itself and still uses the spec
 * for narration and tooltips. A hovered row shows its disabled reason, its tooltip or, when the
 * title was ellipsized, the full title.
 *
 * <p>All coordinates are logical (inside a {@link TacticalScreen} that is the layout space after the
 * minimum 2x); clipping goes through {@link UiScale#enableScissor}.
 */
public final class TacticalList<T> extends AbstractWidget {
    /** How an overflowing list shows that rows are hidden. */
    public enum Overflow {
        /** Thin scrollbar only. */
        SCROLLBAR,
        /** Reserved last line "↓ 还有 n 项" only. */
        MORE_LINE,
        /** Both (the default, as the preview's formation details). */
        BOTH
    }

    /**
     * Draws one whole item row inside {@code bounds} (already clipped to the list), usually starting
     * with {@link TacticalDraw#rowBg}. {@code state} carries selection, hover, disabled and stripe.
     */
    @FunctionalInterface
    public interface RowRenderer<T> {
        void render(GuiGraphics graphics, Font font, UiRect bounds, T item,
                    TacticalDraw.RowSpec spec, TacticalDraw.RowState state);
    }

    /** Receives an item and its index in {@link #items()}. */
    @FunctionalInterface
    public interface ItemCallback<T> {
        void accept(int index, T item);
    }

    /**
     * Pure result of fitting entries into the viewport.
     *
     * @param first       first visible entry
     * @param count       visible entries (only whole ones)
     * @param hiddenAbove items (not group titles) above the view
     * @param hiddenBelow items below the view
     * @param overflow    whether the entries do not all fit (the "more" line is reserved)
     * @param maxFirst    largest valid {@code first}
     */
    public record Window(int first, int count, int hiddenAbove, int hiddenBelow, boolean overflow,
                         int maxFirst) {
        /** Entry index just after the last visible one. */
        public int end() {
            return first + count;
        }
    }

    /** Height of the "↓ 还有 n 项" line. */
    public static final int MORE_LINE_HEIGHT = 12;
    /** Width of the scrollbar inside the well. */
    public static final int SCROLLBAR_WIDTH = 2;
    /** Group title height in the compact size class and otherwise (preview formation list). */
    public static final int HEADER_HEIGHT_TIGHT = 11;
    public static final int HEADER_HEIGHT = 13;

    private final Function<? super T, TacticalDraw.RowSpec> presenter;
    private List<T> items = List.of();
    private List<Entry> entries = List.of();
    private int[] entryOfItem = new int[0];
    private int rowHeight = 18;
    private int headerHeight = HEADER_HEIGHT;
    private Function<? super T, Component> groupLabel;
    private Function<? super T, ?> keyOf = Function.identity();
    private RowRenderer<T> renderer;
    private Overflow overflow = Overflow.BOTH;
    private boolean alternate = true;
    private ItemCallback<T> onSelect = (index, item) -> { };
    private ItemCallback<T> onActivate = (index, item) -> { };
    private TacticalIcon emptyIcon = TacticalIcon.INFO;
    private Component emptyTitle = Component.translatable("screen.wok_infantry.list.empty");
    private Component emptyHint;
    private int selected = -1;
    private int first;
    /** Cached fitting input of {@link #heights()} / {@link #headerFlags()}; null = stale. */
    private int[] heightsCache;
    private boolean[] headerFlagsCache;
    private boolean draggingScrollbar;
    private Tooltip ownTooltip;
    private Tooltip rowTooltip;
    private String rowTooltipText = "";

    /** Group title row: label and the number of items in the group. */
    private record Entry(int item, Component header, int groupSize) {
        boolean isHeader() {
            return item < 0;
        }
    }

    /**
     * @param name      the list's name for narration
     * @param presenter content of each item's row
     */
    public TacticalList(int x, int y, int width, int height, Component name,
                        Function<? super T, TacticalDraw.RowSpec> presenter) {
        super(x, y, width, height, name == null ? Component.empty() : name);
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    // ---- configuration ----------------------------------------------------------------------------

    /** Row pitch in pixels (each row is drawn 1px shorter, leaving a dark seam). */
    public TacticalList<T> rowHeight(int pitch) {
        this.rowHeight = Math.max(9, pitch);
        invalidateFitting();
        return this;
    }

    public TacticalList<T> headerHeight(int pitch) {
        this.headerHeight = Math.max(9, pitch);
        invalidateFitting();
        return this;
    }

    /** Row and group-title heights of a size class ({@code rowH}; 11 or 13 for titles). */
    public TacticalList<T> metrics(TacticalShellLayout.Metrics metrics) {
        rowHeight(metrics.rowHeight());
        return headerHeight(metrics.tight() ? HEADER_HEIGHT_TIGHT : HEADER_HEIGHT);
    }

    /**
     * Inserts a group title before every run of consecutive items with the same label (sort the
     * items by group first); {@code null} removes grouping.
     */
    public TacticalList<T> groupBy(Function<? super T, Component> label) {
        this.groupLabel = label;
        rebuildEntries();
        return this;
    }

    /**
     * Identity used to keep the selection when {@link #setItems} replaces the items (for example a
     * stable id of a record that is re-created by every snapshot). Default: {@code equals}.
     */
    public TacticalList<T> keyedBy(Function<? super T, ?> key) {
        this.keyOf = key == null ? Function.identity() : key;
        return this;
    }

    public TacticalList<T> renderer(RowRenderer<T> rowRenderer) {
        this.renderer = rowRenderer;
        return this;
    }

    public TacticalList<T> overflow(Overflow style) {
        this.overflow = style == null ? Overflow.BOTH : style;
        return this;
    }

    /** Alternate row stripes (default on). */
    public TacticalList<T> alternate(boolean value) {
        this.alternate = value;
        return this;
    }

    /** Called when a row is clicked (also a disabled one) or the keyboard moves the selection. */
    public TacticalList<T> onSelect(ItemCallback<T> callback) {
        this.onSelect = callback == null ? (index, item) -> { } : callback;
        return this;
    }

    /** Called for Enter / Space on the selected row (also a disabled one; check it). */
    public TacticalList<T> onActivate(ItemCallback<T> callback) {
        this.onActivate = callback == null ? (index, item) -> { } : callback;
        return this;
    }

    /** Empty state shown when there are no items ({@code title == null}: an empty well). */
    public TacticalList<T> emptyState(TacticalIcon icon, Component title, Component hint) {
        this.emptyIcon = icon == null ? TacticalIcon.INFO : icon;
        this.emptyTitle = title;
        this.emptyHint = hint;
        return this;
    }

    public void setBounds(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        setWidth(Math.max(0, width));
        setHeight(Math.max(0, height));
    }

    // ---- data -------------------------------------------------------------------------------------

    public List<T> items() {
        return items;
    }

    /**
     * Replaces the items. The selection follows its key ({@link #keyedBy}) or is cleared; the scroll
     * position is kept (and clamped), so a refreshed snapshot does not jump back to the top.
     */
    public void setItems(List<? extends T> newItems) {
        Object selectedKey = selected >= 0 ? keyOf.apply(items.get(selected)) : null;
        items = newItems == null ? List.of() : List.copyOf(newItems);
        rebuildEntries();
        selected = -1;
        if (selectedKey != null) {
            for (int index = 0; index < items.size(); index++) {
                if (Objects.equals(keyOf.apply(items.get(index)), selectedKey)) {
                    selected = index;
                    break;
                }
            }
        }
        first = currentWindow().first();
    }

    /** Index of the selected item, or -1. */
    public int selectedIndex() {
        return selected;
    }

    /** The selected item, or {@code null}. */
    public T selected() {
        return selected >= 0 ? items.get(selected) : null;
    }

    /** Selects {@code index} (-1 clears) without a callback and scrolls it into view. */
    public void setSelectedIndex(int index) {
        selected = index >= 0 && index < items.size() ? index : -1;
        if (selected >= 0) {
            ensureVisible(selected);
        }
    }

    /** First visible entry (group titles count as entries). */
    public int firstVisible() {
        return currentWindow().first();
    }

    /** Scrolls so that entry {@code entry} is the first one (clamped). */
    public void setFirstVisible(int entry) {
        first = Math.max(0, entry);
        first = currentWindow().first();
    }

    /** Scrolls by {@code entries} rows (positive: down); returns whether the view moved. */
    public boolean scrollBy(int rows) {
        int before = currentWindow().first();
        first = before + rows;
        first = currentWindow().first();
        return first != before;
    }

    /** Scrolls the least amount that shows item {@code index} completely. */
    public void ensureVisible(int index) {
        if (index < 0 || index >= items.size()) {
            return;
        }
        first = scrollToReveal(heights(), headerFlags(), entryOfItem[index], first,
                viewport().height(), moreLineHeight());
    }

    /** Whether item {@code index} is disabled by its presenter. */
    public boolean isDisabled(int index) {
        return index >= 0 && index < items.size() && presenter.apply(items.get(index)).disabled();
    }

    // ---- geometry (logical coordinates) -------------------------------------------------------------

    /** Inside of the well. */
    public UiRect viewport() {
        return TacticalDraw.wellInner(UiRect.ofSize(getX(), getY(), width, height));
    }

    /** The current fit of rows into the viewport. */
    public Window currentWindow() {
        return window(heights(), headerFlags(), first, viewport().height(), moreLineHeight());
    }

    /** Bounds of item {@code index}'s row, or {@link UiRect#EMPTY} when it is not fully visible. */
    public UiRect rowBounds(int index) {
        if (index < 0 || index >= items.size()) {
            return UiRect.EMPTY;
        }
        Window window = currentWindow();
        int entry = entryOfItem[index];
        if (entry < window.first() || entry >= window.end()) {
            return UiRect.EMPTY;
        }
        UiRect inner = viewport();
        int y = inner.top();
        for (int at = window.first(); at < entry; at++) {
            y += heightOf(entries.get(at));
        }
        return new UiRect(inner.left(), y, rowRight(inner, window), y + rowHeight - 1);
    }

    /** Line with "↓ 还有 n 项", or {@link UiRect#EMPTY} when nothing is hidden or it is not shown. */
    public UiRect moreLineBounds() {
        Window window = currentWindow();
        if (!window.overflow() || overflow == Overflow.SCROLLBAR) {
            return UiRect.EMPTY;
        }
        UiRect inner = viewport();
        return new UiRect(inner.left(), inner.bottom() - MORE_LINE_HEIGHT, rowRight(inner, window),
                inner.bottom());
    }

    /** Item under (x, y), or -1 (group titles, the "more" line and the scrollbar are no items). */
    public int itemAt(double x, double y) {
        UiRect inner = viewport();
        Window window = currentWindow();
        if (!inner.contains(x, y) || x >= rowRight(inner, window)) {
            return -1;
        }
        int top = inner.top();
        for (int at = window.first(); at < window.end(); at++) {
            Entry entry = entries.get(at);
            int pitch = heightOf(entry);
            if (y >= top && y < top + pitch) {
                return entry.isHeader() ? -1 : entry.item();
            }
            top += pitch;
        }
        return -1;
    }

    private int rowRight(UiRect inner, Window window) {
        return window.overflow() && overflow != Overflow.MORE_LINE
                ? inner.right() - SCROLLBAR_WIDTH - 1 : inner.right();
    }

    private UiRect scrollTrack(UiRect inner) {
        return new UiRect(inner.right() - SCROLLBAR_WIDTH, inner.top(), inner.right(), inner.bottom());
    }

    private int moreLineHeight() {
        return overflow == Overflow.SCROLLBAR ? 0 : MORE_LINE_HEIGHT;
    }

    private int heightOf(Entry entry) {
        return entry.isHeader() ? headerHeight : rowHeight;
    }

    /**
     * Pitch of every entry; cached until the entries or a pitch change (the fitting runs several
     * times per frame). Read-only for callers.
     */
    private int[] heights() {
        int[] heights = heightsCache;
        if (heights == null) {
            heights = new int[entries.size()];
            for (int index = 0; index < heights.length; index++) {
                heights[index] = heightOf(entries.get(index));
            }
            heightsCache = heights;
        }
        return heights;
    }

    /** Whether each entry is a group title; cached like {@link #heights()}. Read-only. */
    private boolean[] headerFlags() {
        boolean[] flags = headerFlagsCache;
        if (flags == null) {
            flags = new boolean[entries.size()];
            for (int index = 0; index < flags.length; index++) {
                flags[index] = entries.get(index).isHeader();
            }
            headerFlagsCache = flags;
        }
        return flags;
    }

    private void invalidateFitting() {
        heightsCache = null;
        headerFlagsCache = null;
    }

    private void rebuildEntries() {
        List<Entry> built = new ArrayList<>(items.size() + 4);
        int[] map = new int[items.size()];
        String currentGroup = null;
        int headerAt = -1;
        for (int index = 0; index < items.size(); index++) {
            if (groupLabel != null) {
                Component label = groupLabel.apply(items.get(index));
                String key = label == null ? "" : label.getString();
                if (!key.equals(currentGroup)) {
                    currentGroup = key;
                    headerAt = built.size();
                    built.add(new Entry(-1, label == null ? Component.empty() : label, 0));
                }
                Entry header = built.get(headerAt);
                built.set(headerAt, new Entry(-1, header.header(), header.groupSize() + 1));
            }
            map[index] = built.size();
            built.add(new Entry(index, null, 0));
        }
        entries = List.copyOf(built);
        entryOfItem = map;
        invalidateFitting();
    }

    // ---- pure fitting -------------------------------------------------------------------------------

    /**
     * Fits entries of the given pitches into a viewport of {@code viewportHeight}: when they do not
     * all fit, {@code moreLineHeight} is reserved at the bottom, {@code requestedFirst} is clamped
     * so the last entry can reach the bottom, only whole entries are counted and a trailing group
     * title is dropped.
     */
    public static Window window(int[] heights, boolean[] header, int requestedFirst,
                                int viewportHeight, int moreLineHeight) {
        int count = heights.length;
        long total = 0;
        for (int height : heights) {
            total += height;
        }
        if (count == 0 || total <= viewportHeight) {
            return new Window(0, count, 0, 0, false, 0);
        }
        int available = Math.max(0, viewportHeight - Math.max(0, moreLineHeight));
        int maxFirst = count;
        int tail = 0;
        while (maxFirst > 0 && tail + heights[maxFirst - 1] <= available) {
            tail += heights[maxFirst - 1];
            maxFirst--;
        }
        maxFirst = Math.min(maxFirst, count - 1);
        int firstShown = Math.max(0, Math.min(maxFirst, requestedFirst));
        int shown = 0;
        int used = 0;
        while (firstShown + shown < count && used + heights[firstShown + shown] <= available) {
            used += heights[firstShown + shown];
            shown++;
        }
        while (shown > 0 && header[firstShown + shown - 1]) {
            shown--;
        }
        int above = 0;
        int below = 0;
        for (int index = 0; index < count; index++) {
            if (header[index]) {
                continue;
            }
            if (index < firstShown) {
                above++;
            } else if (index >= firstShown + shown) {
                below++;
            }
        }
        return new Window(firstShown, shown, above, below, true, maxFirst);
    }

    /**
     * Smallest scroll change from {@code first} that shows entry {@code target} completely; when
     * scrolling up onto the first row of a group, its title comes along.
     */
    public static int scrollToReveal(int[] heights, boolean[] header, int target, int first,
                                     int viewportHeight, int moreLineHeight) {
        Window window = window(heights, header, first, viewportHeight, moreLineHeight);
        if (!window.overflow() || target < 0 || target >= heights.length) {
            return window.first();
        }
        if (target < window.first()) {
            int top = target > 0 && header[target - 1] ? target - 1 : target;
            return Math.max(0, Math.min(window.maxFirst(), top));
        }
        if (target < window.end()) {
            return window.first();
        }
        int available = Math.max(0, viewportHeight - Math.max(0, moreLineHeight));
        int start = target + 1;
        int used = 0;
        while (start > 0 && used + heights[start - 1] <= available) {
            used += heights[start - 1];
            start--;
        }
        start = Math.min(start, target);
        return Math.max(window.first(), Math.min(window.maxFirst(), start));
    }

    // ---- drawing ------------------------------------------------------------------------------------

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiRect bounds = UiRect.ofSize(getX(), getY(), width, height);
        if (bounds.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        TacticalDraw.well(graphics, bounds);
        UiRect inner = viewport();
        Tooltip tooltip = null;
        if (items.isEmpty()) {
            if (emptyTitle != null) {
                TacticalDraw.empty(graphics, font, inner, emptyIcon, emptyTitle, emptyHint, false);
            }
        } else if (!inner.isEmpty()) {
            tooltip = renderRows(graphics, font, inner, mouseX, mouseY);
        }
        TacticalDraw.focusRingIfKeyboard(graphics, this);
        super.setTooltip(tooltip != null ? tooltip : ownTooltip);
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.widget(graphics, this, "list", active ? "NORMAL" : "DISABLED", false,
                    TacticalButtonStyle.keyboardFocused(this));
        }
    }

    /**
     * Tooltip of the list itself, shown while no row offers one (a hovered row's disabled
     * reason, tooltip or full title always wins).
     */
    @Override
    public void setTooltip(Tooltip tooltip) {
        ownTooltip = tooltip;
        super.setTooltip(tooltip);
    }

    private Tooltip renderRows(GuiGraphics graphics, Font font, UiRect inner, int mouseX,
                               int mouseY) {
        Window window = currentWindow();
        first = window.first();
        int right = rowRight(inner, window);
        boolean keyboard = TacticalButtonStyle.keyboardFocused(this);
        DrawnRow tipRow = null;
        UiScale.enableScissor(graphics, inner);
        try {
            int y = inner.top();
            for (int at = window.first(); at < window.end(); at++) {
                Entry entry = entries.get(at);
                int pitch = heightOf(entry);
                if (entry.isHeader()) {
                    renderHeader(graphics, font, new UiRect(inner.left(), y, right, y + pitch), entry);
                } else {
                    UiRect row = new UiRect(inner.left(), y, right, y + pitch - 1);
                    boolean hovered = isHovered() && mouseX >= row.left() && mouseX < row.right()
                            && mouseY >= y && mouseY < y + pitch;
                    DrawnRow drawn = renderItem(graphics, font, row, entry.item(), hovered);
                    if (UiLayoutProbe.recording()) {
                        probeRow(graphics, row, entry.item(), drawn, hovered);
                    }
                    // The hovered row wins; with keyboard focus the selected row speaks otherwise.
                    if (hovered || (tipRow == null && keyboard && entry.item() == selected)) {
                        tipRow = drawn;
                    }
                }
                y += pitch;
            }
            if (window.overflow()) {
                if (overflow != Overflow.SCROLLBAR) {
                    renderMoreLine(graphics, font, new UiRect(inner.left(),
                            inner.bottom() - MORE_LINE_HEIGHT, right, inner.bottom()), window);
                }
                if (overflow != Overflow.MORE_LINE) {
                    int[] heights = heights();
                    int total = 0;
                    int offset = 0;
                    for (int index = 0; index < heights.length; index++) {
                        total += heights[index];
                        if (index < window.first()) {
                            offset += heights[index];
                        }
                    }
                    TacticalDraw.scrollbar(graphics, scrollTrack(inner), total,
                            inner.height() - moreLineHeight(), offset);
                }
            }
        } finally {
            UiScale.disableScissor(graphics);
        }
        return tipRow == null ? null
                : tooltipFor(rowTooltipText(tipRow.spec(), tipRow.titleCut(), tipRow.subCut()));
    }

    /** A drawn item row and what had to be shortened in it. */
    private record DrawnRow(TacticalDraw.RowSpec spec, boolean titleCut, boolean subCut) {
    }

    /**
     * Reports a drawn row to the uiTest layout probe as control {@code <list uiId>/<index>}. A
     * default row offers its full text on hover (see {@link #rowTooltipText}); a custom renderer
     * only does so through the spec's tooltip.
     */
    private void probeRow(GuiGraphics graphics, UiRect row, int index, DrawnRow drawn,
                          boolean hovered) {
        TacticalDraw.RowSpec spec = drawn.spec();
        String state = index == selected ? "SELECTED" : spec.disabled() ? "DISABLED"
                : hovered ? "HOVER" : "NORMAL";
        UiLayoutProbe.part(graphics, this, Integer.toString(index), "list-row", state, row.left(),
                row.top(), row.right(), row.bottom(), !spec.disabled(), spec.title(),
                spec.disabledReason(), drawn.titleCut() || drawn.subCut(),
                renderer == null || spec.tooltip() != null);
    }

    private DrawnRow renderItem(GuiGraphics graphics, Font font, UiRect row, int index,
                                boolean hovered) {
        T item = items.get(index);
        TacticalDraw.RowSpec spec = presenter.apply(item);
        TacticalDraw.RowState state = new TacticalDraw.RowState(index == selected, hovered,
                spec.disabled(), alternate && index % 2 == 1);
        if (renderer != null) {
            renderer.render(graphics, font, row, item, spec, state);
            return new DrawnRow(spec, false, false);
        }
        TacticalDraw.RowText text = TacticalDraw.row(graphics, font, row, spec, state);
        return new DrawnRow(spec, text.titleTruncated() || text.rightTruncated(),
                text.subTruncated());
    }

    /** Pure: tooltip text of a row, or {@code null} when it needs none. */
    static Component rowTooltipText(TacticalDraw.RowSpec spec, boolean titleCut, boolean subCut) {
        Component reason = spec.disabledReason();
        if (reason != null) {
            Component unavailable = Component.translatable("screen.wok_infantry.list.unavailable",
                    reason);
            return titleCut ? Component.empty().append(spec.title()).append("\n").append(unavailable)
                    : unavailable;
        }
        if (spec.tooltip() != null) {
            return spec.tooltip();
        }
        if (titleCut || subCut) {
            Component text = Component.empty().append(spec.title());
            if (spec.right() != null && !spec.right().getString().isEmpty()) {
                text = Component.empty().append(text).append("  ").append(spec.right());
            }
            if (spec.sub() != null && !spec.sub().getString().isEmpty()) {
                text = Component.empty().append(text).append("\n").append(spec.sub());
            }
            return text;
        }
        return null;
    }

    private Tooltip tooltipFor(Component text) {
        if (text == null) {
            return null;
        }
        String key = text.getString();
        if (rowTooltip == null || !key.equals(rowTooltipText)) {
            rowTooltip = Tooltip.create(text);
            rowTooltipText = key;
        }
        return rowTooltip;
    }

    private static void renderHeader(GuiGraphics graphics, Font font, UiRect bounds, Entry entry) {
        int textY = bounds.top() + Math.floorDiv(bounds.height() - 8, 2);
        graphics.fill(bounds.left() + 2, bounds.top() + 3, bounds.left() + 4, bounds.bottom() - 2,
                TacticalBoardTheme.SECTION_B);
        String count = String.valueOf(entry.groupSize());
        int countWidth = font.width(count);
        graphics.drawString(font, count, bounds.right() - 4 - countWidth, textY,
                TacticalBoardTheme.LIGHT_MUTED, false);
        TextFit.draw(graphics, font, entry.header(), bounds.left() + 8, textY,
                Math.max(0, bounds.width() - 8 - countWidth - 8), TacticalBoardTheme.LIGHT_MUTED,
                TextFit.Align.LEFT);
    }

    private static void renderMoreLine(GuiGraphics graphics, Font font, UiRect bounds,
                                       Window window) {
        boolean below = window.hiddenBelow() > 0;
        int hidden = below ? window.hiddenBelow() : window.hiddenAbove();
        if (hidden <= 0) {
            return;
        }
        int textY = bounds.bottom() - 9;
        (below ? TacticalIcon.DOWN : TacticalIcon.UP).draw(graphics, bounds.left() + 2, textY - 1,
                TacticalBoardTheme.LIGHT_MUTED);
        TextFit.draw(graphics, font, moreLineText(window), bounds.left() + 14, textY,
                Math.max(0, bounds.width() - 16), TacticalBoardTheme.LIGHT_MUTED, TextFit.Align.LEFT);
    }

    /** Pure: text of the "more" line ("还有 n 项" below, "上方还有 n 项" at the end of the list). */
    static Component moreLineText(Window window) {
        return window.hiddenBelow() > 0
                ? Component.translatable("screen.wok_infantry.list.more_below", window.hiddenBelow())
                : Component.translatable("screen.wok_infantry.list.more_above", window.hiddenAbove());
    }

    // ---- input --------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Vanilla only delivers a release to the widget under the pointer, so a scrollbar drag
        // released outside the list must not survive into the next press.
        draggingScrollbar = false;
        if (!active || !visible || !isValidClickButton(button) || !clicked(mouseX, mouseY)) {
            return false;
        }
        UiRect inner = viewport();
        Window window = currentWindow();
        if (window.overflow() && overflow != Overflow.MORE_LINE
                && mouseX >= rowRight(inner, window) && inner.contains(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollToPointer(mouseY);
            return true;
        }
        UiRect more = moreLineBounds();
        if (!more.isEmpty() && more.contains(mouseX, mouseY)) {
            int page = Math.max(1, window.count() - 1);
            scrollBy(window.hiddenBelow() > 0 ? page : -page);
            return true;
        }
        int index = itemAt(mouseX, mouseY);
        if (index >= 0) {
            clickSound();
            selected = index;
            ensureVisible(index);
            onSelect.accept(index, items.get(index));
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX,
                                double dragY) {
        if (draggingScrollbar) {
            scrollToPointer(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = draggingScrollbar;
        draggingScrollbar = false;
        return wasDragging;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible || entries.isEmpty() || delta == 0.0D || !currentWindow().overflow()) {
            return false;
        }
        scrollBy(delta > 0.0D ? -1 : 1);
        // An overflowing list keeps the wheel even at either end, so it never leaks elsewhere.
        return true;
    }

    private void scrollToPointer(double mouseY) {
        UiRect inner = viewport();
        Window window = currentWindow();
        if (window.maxFirst() <= 0 || inner.height() <= 0) {
            return;
        }
        double ratio = (mouseY - inner.top()) / Math.max(1, inner.height());
        first = (int) Math.round(Math.max(0.0D, Math.min(1.0D, ratio)) * window.maxFirst());
        first = currentWindow().first();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible || items.isEmpty()) {
            return false;
        }
        int last = items.size() - 1;
        int page = Math.max(1, currentWindow().count() - 1);
        return switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> selected != 0 && moveSelection(selected < 0 ? 0 : selected - 1);
            case GLFW.GLFW_KEY_DOWN -> selected < last && moveSelection(selected + 1);
            case GLFW.GLFW_KEY_HOME -> moveSelection(0) || true;
            case GLFW.GLFW_KEY_END -> moveSelection(last) || true;
            case GLFW.GLFW_KEY_PAGE_UP -> moveSelection(Math.max(0, selected - page)) || true;
            case GLFW.GLFW_KEY_PAGE_DOWN -> moveSelection(Math.min(last, Math.max(0, selected + page)))
                    || true;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> activateSelected();
            default -> false;
        };
    }

    /** Moves the selection to {@code index}; returns whether it changed. */
    private boolean moveSelection(int index) {
        if (index < 0 || index >= items.size()) {
            return false;
        }
        ensureVisible(index);
        if (index == selected) {
            return false;
        }
        selected = index;
        onSelect.accept(index, items.get(index));
        return true;
    }

    private boolean activateSelected() {
        if (selected < 0) {
            return false;
        }
        clickSound();
        onActivate.accept(selected, items.get(selected));
        return true;
    }

    private void clickSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            playDownSound(minecraft.getSoundManager());
        }
    }

    // ---- narration ----------------------------------------------------------------------------------

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        if (selected < 0) {
            return;
        }
        TacticalDraw.RowSpec spec = presenter.apply(items.get(selected));
        output.add(NarratedElementType.POSITION, Component.translatable(
                "screen.wok_infantry.list.position", spec.title(), selected + 1, items.size()));
        if (spec.disabledReason() != null) {
            output.add(NarratedElementType.HINT, Component.translatable(
                    "screen.wok_infantry.list.unavailable", spec.disabledReason()));
        }
    }
}
