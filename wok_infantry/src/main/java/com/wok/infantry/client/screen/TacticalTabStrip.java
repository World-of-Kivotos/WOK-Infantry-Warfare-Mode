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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Page tabs of a WOK步战 terminal, as one keyboard-focusable widget.
 *
 * <p>Two skins: {@link Skin#HEADER} sits in the dark shell header (selected tab = blue plate with a
 * light top bar, hover = {@code TAB_HOVER}); {@link Skin#BOARD} is a segmented row of board keys
 * drawn from the shared {@link TacticalButtonStyle} table. When the full names do not fit the
 * strip shows the short names, and when those do not fit either it becomes a
 * {@code ‹ name n/m ›} pager. The current tab is drawn as "current" and is not clickable;
 * disabled tabs show their reason as a tooltip.
 *
 * <p>Keyboard: {@code Ctrl+Tab} / {@code Ctrl+Shift+Tab} cycle through the enabled tabs from
 * anywhere in a {@link TacticalScreen} (the screen routes them here); plain {@code Tab} keeps the
 * vanilla focus navigation. While the strip itself has keyboard focus, ←/→ also move between
 * tabs.
 *
 * <p>Selecting only reports the index through the callback; the screen switches the page (in
 * place, or through {@link BattleTerminalNav} with replace semantics) and normally rebuilds the
 * strip with the new current index.
 */
public final class TacticalTabStrip extends AbstractWidget {
    public enum Skin {
        HEADER,
        BOARD
    }

    /** How the tabs are shown in the strip's width. */
    public enum Mode {
        FULL,
        SHORT,
        PAGER
    }

    /**
     * One tab. A non-null {@code disabledReason} disables it; {@code badge} is an optional small
     * tag such as {@code 3/4}.
     */
    public record Tab(String id, Component label, Component shortLabel, Component disabledReason,
                      Component badge, int badgeColor) {
        public Tab {
            Objects.requireNonNull(id, "id");
            label = label == null ? Component.literal(id) : label;
            shortLabel = shortLabel == null ? label : shortLabel;
        }

        public static Tab of(String id, Component label) {
            return new Tab(id, label, label, null, null, TacticalBoardTheme.MUTED);
        }

        public static Tab of(String id, Component label, Component shortLabel) {
            return new Tab(id, label, shortLabel, null, null, TacticalBoardTheme.MUTED);
        }

        public boolean enabled() {
            return disabledReason == null;
        }

        /** Copy that is disabled with {@code reason} (or enabled again when {@code null}). */
        public Tab withDisabledReason(Component reason) {
            return new Tab(id, label, shortLabel, reason, badge, badgeColor);
        }

        public Tab withBadge(Component value, int color) {
            return new Tab(id, label, shortLabel, disabledReason, value, color);
        }

        Component labelFor(Mode mode) {
            return mode == Mode.FULL ? label : shortLabel;
        }
    }

    private static final int GAP = 2;
    private static final int HEADER_PAD = 14;
    private static final int HEADER_PAD_COMPACT = 8;
    private static final int BOARD_PAD = 10;
    private static final int PAGER_KEY_MAX = 16;

    private final Skin skin;
    private final List<Tab> tabs;
    private final IntConsumer onSelect;
    private final Tooltip[] reasonTooltips;
    private final Tooltip[] labelTooltips;
    private int current;
    private boolean compact;

    public TacticalTabStrip(Skin skin, List<Tab> tabs, int current, IntConsumer onSelect) {
        this(0, 0, 0, 0, skin, tabs, current, onSelect);
    }

    public TacticalTabStrip(int x, int y, int width, int height, Skin skin, List<Tab> tabs,
                            int current, IntConsumer onSelect) {
        super(x, y, width, height, Component.translatable("screen.wok_infantry.tabs"));
        this.skin = skin == null ? Skin.BOARD : skin;
        this.tabs = List.copyOf(Objects.requireNonNull(tabs, "tabs"));
        this.onSelect = onSelect == null ? index -> { } : onSelect;
        this.current = this.tabs.isEmpty() ? -1 : Math.max(0, Math.min(this.tabs.size() - 1, current));
        this.reasonTooltips = new Tooltip[this.tabs.size()];
        this.labelTooltips = new Tooltip[this.tabs.size()];
    }

    public Skin skin() {
        return skin;
    }

    public List<Tab> tabs() {
        return tabs;
    }

    /** Index of the current tab, or -1 without tabs. */
    public int current() {
        return current;
    }

    /** Marks {@code index} as the current tab without firing the callback. */
    public void setCurrent(int index) {
        if (index >= 0 && index < tabs.size()) {
            current = index;
        }
    }

    public int indexOf(String id) {
        for (int index = 0; index < tabs.size(); index++) {
            if (tabs.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    /** Narrower header padding for the compact size class. */
    public void setCompact(boolean compact) {
        this.compact = compact;
    }

    public void setBounds(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        setWidth(Math.max(0, width));
        setHeight(Math.max(0, height));
    }

    // ---- selection ------------------------------------------------------------------------------

    /**
     * Requests tab {@code index}: fires the callback when it exists, is enabled and is not the
     * current tab. Returns whether the callback ran.
     */
    public boolean select(int index) {
        if (index < 0 || index >= tabs.size() || index == current || !tabs.get(index).enabled()) {
            return false;
        }
        onSelect.accept(index);
        return true;
    }

    /** Selects the next ({@code delta > 0}) or previous enabled tab, wrapping around. */
    public boolean cycle(int delta) {
        int target = nextEnabled(enabledFlags(), current, delta, true);
        return target >= 0 && select(target);
    }

    /**
     * Pure key rule: {@code +1} for Ctrl+Tab, {@code -1} for Ctrl+Shift+Tab, otherwise 0 (plain
     * Tab and Shift+Tab stay with the vanilla focus navigation). Uses the real Ctrl modifier on
     * every platform, so Cmd+Tab on macOS is never taken.
     */
    public static int navigationDelta(int keyCode, int modifiers) {
        if (keyCode != GLFW.GLFW_KEY_TAB || (modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
            return 0;
        }
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1;
    }

    /**
     * Pure: the next enabled index from {@code from} in direction {@code delta}, skipping
     * disabled entries; wraps around when {@code wrap}. Returns -1 when no other enabled entry
     * exists.
     */
    static int nextEnabled(boolean[] enabled, int from, int delta, boolean wrap) {
        int count = enabled.length;
        if (count == 0 || delta == 0) {
            return -1;
        }
        int step = delta > 0 ? 1 : -1;
        int index = from;
        for (int tries = 0; tries < count; tries++) {
            index += step;
            if (index < 0 || index >= count) {
                if (!wrap) {
                    return -1;
                }
                index = Math.floorMod(index, count);
            }
            if (index == from) {
                return -1;
            }
            if (enabled[index]) {
                return index;
            }
        }
        return -1;
    }

    private boolean[] enabledFlags() {
        boolean[] flags = new boolean[tabs.size()];
        for (int index = 0; index < flags.length; index++) {
            flags[index] = tabs.get(index).enabled();
        }
        return flags;
    }

    // ---- measuring and layout -------------------------------------------------------------------

    /** Width needed to show every tab with full or short names in this skin. */
    public int preferredWidth(Font font, boolean shortLabels, boolean compactHeader) {
        if (tabs.isEmpty()) {
            return 0;
        }
        Mode mode = shortLabels ? Mode.SHORT : Mode.FULL;
        int total = GAP * (tabs.size() - 1);
        for (Tab tab : tabs) {
            total += naturalWidth(font, tab, mode, compactHeader);
        }
        return total;
    }

    /** Pure mode rule: full names if they fit, else short names, else the pager. */
    static Mode chooseMode(int available, int fullWidth, int shortWidth) {
        if (fullWidth <= available) {
            return Mode.FULL;
        }
        return shortWidth <= available ? Mode.SHORT : Mode.PAGER;
    }

    public Mode mode(Font font) {
        return chooseMode(width, preferredWidth(font, false, compact),
                preferredWidth(font, true, compact));
    }

    private int naturalWidth(Font font, Tab tab, Mode mode, boolean compactHeader) {
        int pad = skin == Skin.HEADER ? (compactHeader ? HEADER_PAD_COMPACT : HEADER_PAD) : BOARD_PAD;
        return font.width(tab.labelFor(mode)) + pad + badgeWidth(font, tab);
    }

    private static int badgeWidth(Font font, Tab tab) {
        return tab.badge() == null || tab.badge().getString().isEmpty()
                ? 0 : font.width(tab.badge()) + 6 + 2;
    }

    /**
     * Cell of tab {@code index} in the current mode, or {@link UiRect#EMPTY} when that tab is not
     * on screen (pager mode shows only the current tab).
     */
    public UiRect tabBounds(int index) {
        return tabBounds(Minecraft.getInstance().font, index);
    }

    UiRect tabBounds(Font font, int index) {
        if (index < 0 || index >= tabs.size()) {
            return UiRect.EMPTY;
        }
        Mode mode = mode(font);
        if (mode == Mode.PAGER) {
            return index == current ? pagerParts().get(1) : UiRect.EMPTY;
        }
        return cells(font, mode).get(index);
    }

    private List<UiRect> cells(Font font, Mode mode) {
        UiRect strip = UiRect.ofSize(getX(), getY(), width, height);
        if (skin == Skin.BOARD) {
            UiRect.Size[] sizes = new UiRect.Size[tabs.size()];
            Arrays.fill(sizes, UiRect.Size.STAR);
            return strip.cols(GAP, sizes);
        }
        List<UiRect> cells = new ArrayList<>(tabs.size());
        int x = getX();
        for (Tab tab : tabs) {
            int cellWidth = naturalWidth(font, tab, mode, compact);
            int right = Math.min(strip.right(), x + cellWidth);
            cells.add(new UiRect(x, getY(), Math.max(x, right), getY() + height));
            x += cellWidth + GAP;
        }
        return cells;
    }

    /** Pager parts: previous key, label, next key. */
    private List<UiRect> pagerParts() {
        UiRect strip = UiRect.ofSize(getX(), getY(), width, height);
        int keyWidth = Math.max(1, Math.min(PAGER_KEY_MAX, width / 5));
        UiRect previous = strip.leftSlice(keyWidth);
        UiRect next = strip.rightSlice(keyWidth);
        UiRect label = new UiRect(previous.right() + GAP, strip.top(),
                Math.max(previous.right() + GAP, next.left() - GAP), strip.bottom());
        return List.of(previous, label, next);
    }

    /** Tab index the click at (x, y) asks for, or -1. Pager arrows map to the neighbour tab. */
    private int targetAt(Font font, double x, double y) {
        Mode mode = mode(font);
        if (mode == Mode.PAGER) {
            List<UiRect> parts = pagerParts();
            if (parts.get(0).contains(x, y)) {
                return nextEnabled(enabledFlags(), current, -1, false);
            }
            if (parts.get(2).contains(x, y)) {
                return nextEnabled(enabledFlags(), current, 1, false);
            }
            return -1;
        }
        List<UiRect> cells = cells(font, mode);
        for (int index = 0; index < cells.size(); index++) {
            if (cells.get(index).contains(x, y)) {
                return index;
            }
        }
        return -1;
    }

    private int hoveredIndex(Font font, int mouseX, int mouseY) {
        if (!isHovered() || mode(font) == Mode.PAGER) {
            return -1;
        }
        List<UiRect> cells = cells(font, mode(font));
        for (int index = 0; index < cells.size(); index++) {
            if (cells.get(index).contains(mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    // ---- input ----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !isValidClickButton(button) || !clicked(mouseX, mouseY)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int target = targetAt(minecraft.font, mouseX, mouseY);
        if (target >= 0 && target != current && tabs.get(target).enabled()) {
            playDownSound(minecraft.getSoundManager());
            select(target);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            int target = nextEnabled(enabledFlags(), current,
                    keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1, false);
            if (target >= 0) {
                select(target);
            }
            return true;
        }
        return false;
    }

    // ---- drawing --------------------------------------------------------------------------------

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (tabs.isEmpty() || width <= 0 || height <= 0) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        Mode mode = mode(font);
        boolean focusRing = TacticalButtonStyle.keyboardFocused(this);
        Tooltip tooltip = null;
        if (mode == Mode.PAGER) {
            tooltip = renderPager(graphics, font, mouseX, mouseY, focusRing);
        } else {
            List<UiRect> cells = cells(font, mode);
            int hovered = hoveredIndex(font, mouseX, mouseY);
            for (int index = 0; index < cells.size(); index++) {
                UiRect cell = cells.get(index);
                if (cell.isEmpty()) {
                    continue;
                }
                Tab tab = tabs.get(index);
                boolean truncated = renderTab(graphics, font, cell, tab, tab.labelFor(mode),
                        index == current, index == hovered && tab.enabled() && index != current,
                        focusRing && index == current);
                if (index == hovered) {
                    tooltip = tooltipFor(index, truncated);
                }
                if (UiLayoutProbe.recording()) {
                    probeTab(graphics, cell, tab, tab.labelFor(mode), index == current,
                            index == hovered, truncated);
                }
            }
        }
        setTooltip(tooltip);
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.widget(graphics, this, "tabs", mode.name(), false, focusRing);
        }
    }

    /**
     * Reports one tab cell to the uiTest layout probe as control {@code <strip uiId>/<tab id>}.
     * A shortened tab label is always offered in full on hover ({@link #tooltipFor}).
     */
    private void probeTab(GuiGraphics graphics, UiRect cell, Tab tab, Component label,
                          boolean isCurrent, boolean hovered, boolean truncated) {
        String state = isCurrent ? "CURRENT" : !tab.enabled() ? "DISABLED"
                : hovered ? "HOVER" : "NORMAL";
        UiLayoutProbe.part(graphics, this, tab.id(), "tab", state, cell.left(), cell.top(),
                cell.right(), cell.bottom(), tab.enabled(), label, tab.disabledReason(), truncated,
                true);
    }

    private Tooltip tooltipFor(int index, boolean truncated) {
        Tab tab = tabs.get(index);
        if (!tab.enabled()) {
            if (reasonTooltips[index] == null) {
                reasonTooltips[index] = Tooltip.create(Component.empty().append(tab.label())
                        .append("\n").append(tab.disabledReason()));
            }
            return reasonTooltips[index];
        }
        if (truncated) {
            if (labelTooltips[index] == null) {
                labelTooltips[index] = Tooltip.create(tab.label());
            }
            return labelTooltips[index];
        }
        return null;
    }

    /** Draws one tab cell; returns whether its label had to be ellipsized. */
    private boolean renderTab(GuiGraphics graphics, Font font, UiRect cell, Tab tab, Component label,
                              boolean isCurrent, boolean hovered, boolean focusRing) {
        int textColor;
        boolean darkFill;
        if (skin == Skin.HEADER) {
            if (isCurrent) {
                graphics.fill(cell.left(), cell.top(), cell.right(), cell.bottom(), TacticalBoardTheme.SELECT);
                graphics.fill(cell.left(), cell.top(), cell.right(), cell.top() + 1,
                        TacticalBoardTheme.SELECT_BAR);
                textColor = TacticalBoardTheme.ON_SELECT;
            } else if (hovered) {
                graphics.fill(cell.left(), cell.top(), cell.right(), cell.bottom(),
                        TacticalBoardTheme.TAB_HOVER);
                textColor = TacticalBoardTheme.LIGHT_MUTED;
            } else {
                textColor = tab.enabled() ? TacticalBoardTheme.LIGHT_MUTED
                        : TacticalBoardTheme.DISABLED_TEXT;
            }
            darkFill = true;
            if (focusRing) {
                TacticalButtonStyle.focusRing(graphics, cell.left(), cell.top(), cell.right(),
                        cell.bottom());
            }
        } else {
            TacticalButtonStyle.Look look = isCurrent
                    ? TacticalButtonStyle.resolve(false, true, true,
                    TacticalButtonStyle.Variant.NORMAL, false, false)
                    : TacticalButtonStyle.resolve(tab.enabled(), false, false,
                    TacticalButtonStyle.Variant.NORMAL, hovered, false);
            TacticalButtonStyle.render(graphics, font, cell.left(), cell.top(), cell.right(),
                    cell.bottom(), Component.empty(), look,
                    TacticalButtonStyle.Options.DEFAULT.withFocusRing(focusRing));
            TacticalButtonStyle.Palette palette = TacticalButtonStyle.palette(look);
            textColor = palette.text();
            darkFill = palette.darkFill();
        }
        return renderLabel(graphics, font, cell, label, tab, textColor, darkFill);
    }

    /**
     * Text baseline of a label in {@code cell}. Header tabs end on the header's 2px bottom rule, so
     * their labels sit on the title's baseline ({@code (h − 10) / 2}, as in the preview's
     * {@code UI.shell}); board keys centre the 8px glyphs.
     */
    static int labelTextY(Skin skin, UiRect cell) {
        int room = cell.height() - (skin == Skin.HEADER ? 10 : 8);
        return cell.top() + Math.max(0, room / 2);
    }

    private boolean renderLabel(GuiGraphics graphics, Font font, UiRect cell, Component label,
                                Tab tab, int textColor, boolean darkFill) {
        int textY = labelTextY(skin, cell);
        int right = cell.right() - 3;
        int badge = badgeWidth(font, tab);
        if (badge > 0) {
            int badgeLeft = right - (badge - 2);
            graphics.fill(badgeLeft, textY - 1, right, textY + 8,
                    darkFill ? TacticalBoardTheme.BADGE_ON_SELECT : TacticalBoardTheme.BADGE_ON_CARD);
            graphics.drawString(font, tab.badge(), badgeLeft + 3, textY,
                    darkFill ? TacticalBoardTheme.LIGHT : tab.badgeColor(), false);
            right = badgeLeft - 2;
        }
        int left = cell.left() + 3;
        TextFit.Fitted fitted = TextFit.draw(graphics, font, label, left, textY,
                Math.max(0, right - left), textColor, TextFit.Align.CENTER);
        return fitted.truncated();
    }

    private Tooltip renderPager(GuiGraphics graphics, Font font, int mouseX, int mouseY,
                                boolean focusRing) {
        List<UiRect> parts = pagerParts();
        boolean[] enabled = enabledFlags();
        boolean canBack = nextEnabled(enabled, current, -1, false) >= 0;
        boolean canNext = nextEnabled(enabled, current, 1, false) >= 0;
        renderPagerKey(graphics, font, parts.get(0), "‹", canBack,
                isHovered() && parts.get(0).contains(mouseX, mouseY));
        renderPagerKey(graphics, font, parts.get(2), "›", canNext,
                isHovered() && parts.get(2).contains(mouseX, mouseY));
        Tab tab = tabs.get(current);
        Component label = Component.empty().append(tab.shortLabel())
                .append("  " + (current + 1) + "/" + tabs.size());
        boolean truncated = renderTab(graphics, font, parts.get(1), tab, label, true, false,
                focusRing);
        if (UiLayoutProbe.recording()) {
            probeTab(graphics, parts.get(1), tab, label, true, false, truncated);
        }
        if (isHovered() && parts.get(1).contains(mouseX, mouseY)
                && (truncated || !tab.shortLabel().getString().equals(tab.label().getString()))) {
            if (labelTooltips[current] == null) {
                labelTooltips[current] = Tooltip.create(tab.label());
            }
            return labelTooltips[current];
        }
        return null;
    }

    private void renderPagerKey(GuiGraphics graphics, Font font, UiRect key, String glyph,
                                boolean enabled, boolean hovered) {
        if (key.isEmpty()) {
            return;
        }
        int textColor;
        if (skin == Skin.HEADER) {
            if (enabled && hovered) {
                graphics.fill(key.left(), key.top(), key.right(), key.bottom(),
                        TacticalBoardTheme.TAB_HOVER);
            }
            textColor = enabled ? TacticalBoardTheme.LIGHT_MUTED : TacticalBoardTheme.DISABLED_TEXT;
        } else {
            TacticalButtonStyle.Look look = TacticalButtonStyle.resolve(enabled, false, false,
                    TacticalButtonStyle.Variant.CONTROL, hovered, false);
            TacticalButtonStyle.render(graphics, font, key.left(), key.top(), key.right(),
                    key.bottom(), Component.empty(), look, TacticalButtonStyle.Options.DEFAULT);
            textColor = TacticalButtonStyle.palette(look).text();
        }
        int textY = labelTextY(skin, key);
        TextFit.draw(graphics, font, glyph, key.left(), textY, key.width(), textColor,
                TextFit.Align.CENTER);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (current < 0) {
            output.add(NarratedElementType.TITLE, getMessage());
            return;
        }
        Tab tab = tabs.get(current);
        output.add(NarratedElementType.TITLE, Component.translatable(
                "screen.wok_infantry.tabs.position", tab.label(), current + 1, tabs.size()));
    }
}
