package com.wok.infantry.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Board slider of the tactical-tablet style (preview {@code UI.slider}): orange = adjustable.
 *
 * <p><b>Look.</b> Label in {@link TacticalBoardTheme#TEXT} on the left, value in the adjustable
 * orange on the right. A slider lower than {@value #COMPACT_HEIGHT}px puts label, track and value
 * on one line; a taller one writes the text above the track. When a one-line track would be
 * narrower than {@value #MIN_TRACK}px the label is dropped first, then the value text (both stay
 * in the hover tooltip). Disabled sliders are gray throughout and ignore hover; keyboard focus
 * gets the focus ring.
 *
 * <p><b>Values.</b> A {@link TacticalSliderScale} maps values to the track: a continuous range
 * snapped to a grid (the legacy constructor), a custom mapping such as
 * {@link TacticalSliderScale#LOGARITHMIC}, or whole detents ("整档", e.g. ammo packages) where the
 * knob always rests on a detent and one key press moves one detent. The mouse is mapped onto the
 * track exactly as it is drawn, so both ends are always reachable.
 *
 * <p><b>Keys.</b> ←/→ move one step (Shift: {@value #COARSE_STEPS} steps), Home/End jump to the
 * ends. The legacy constructor keeps its signature, its percent read-out and its snapping.
 */
public final class TacticalBoardSlider extends AbstractSliderButton {
    /** Sliders lower than this draw label, track and value on one line. */
    public static final int COMPACT_HEIGHT = 18;
    /** Narrowest one-line track kept before the label, then the value text, is dropped. */
    public static final int MIN_TRACK = 16;
    /** Steps of one Shift + arrow press. */
    public static final int COARSE_STEPS = 5;

    /** Legacy read-out: {@code 125%}. */
    public static final DoubleFunction<Component> PERCENT =
            value -> Component.literal(Math.round(value * 100.0D) + "%");
    /** Whole number read-out: {@code 120}. */
    public static final DoubleFunction<Component> INTEGER =
            value -> Component.literal(String.valueOf(Math.round(value)));

    /** Read-out through a translation with the rounded value as its only argument. */
    public static DoubleFunction<Component> translated(String key) {
        return value -> Component.translatable(key, Math.round(value));
    }

    /**
     * Pure placement of a slider's parts in [left, right) x [top, bottom).
     *
     * @param labelRoom width the label may take (0: label not drawn)
     * @param valueRoom width of the value text (0: value not drawn)
     * @param trackY    top of the 2px track
     */
    public record Layout(boolean compact, int textY, int labelX, int labelRoom, int valueX,
                         int valueRoom, int trackLeft, int trackRight, int trackY) {
        public boolean labelShown() {
            return labelRoom > 0;
        }

        public boolean valueShown() {
            return valueRoom > 0;
        }

        /** Knob centre for a track position 0–1. */
        public int knobX(double position) {
            double p = Double.isFinite(position) ? Math.max(0.0D, Math.min(1.0D, position)) : 0.0D;
            return trackLeft + (int) Math.round(p * (trackRight - trackLeft));
        }

        /** Track position 0–1 under the pointer (clamped, so both ends are reachable). */
        public double positionAt(double mouseX) {
            int span = trackRight - trackLeft;
            if (span <= 0 || !Double.isFinite(mouseX)) {
                return 0.0D;
            }
            return Math.max(0.0D, Math.min(1.0D, (mouseX - trackLeft) / span));
        }
    }

    /**
     * Pure layout (preview {@code UI.slider}). {@code labelWidth}/{@code valueWidth} are the
     * natural text widths; 0 means there is no such text.
     */
    public static Layout layout(int left, int top, int right, int bottom, int labelWidth,
                                int valueWidth) {
        int width = Math.max(0, right - left);
        int height = Math.max(0, bottom - top);
        int label = Math.max(0, labelWidth);
        int value = Math.max(0, valueWidth);
        if (height < COMPACT_HEIGHT) {
            int textY = top + Math.floorDiv(height - 8, 2);
            int trackY = top + height / 2 - 1;
            int valueRoom = value;
            int trackRight = value > 0 ? right - value - 10 : right - 5;
            int trackLeft = label > 0 ? left + 4 + Math.min(label + 6, width * 45 / 100) : left + 5;
            int labelRoom = label > 0 ? Math.max(0, trackLeft - left - 8) : 0;
            if (trackRight - trackLeft < MIN_TRACK && labelRoom > 0) {
                trackLeft = left + 5;
                labelRoom = 0;
            }
            if (trackRight - trackLeft < MIN_TRACK && valueRoom > 0) {
                trackRight = right - 5;
                valueRoom = 0;
            }
            trackRight = Math.max(trackLeft, trackRight);
            return new Layout(true, textY, left + 4, labelRoom, right - valueRoom - 4, valueRoom,
                    trackLeft, trackRight, trackY);
        }
        int valueRoom = Math.min(value, Math.max(0, width - 8));
        int labelRoom = label > 0 ? Math.max(0, width - valueRoom - 14) : 0;
        int trackLeft = left + 5;
        int trackRight = Math.max(trackLeft, right - 5);
        return new Layout(false, top + 3, left + 4, labelRoom, right - valueRoom - 4, valueRoom,
                trackLeft, trackRight, bottom - 6);
    }

    /**
     * Draws a slider in {@code bounds} with an explicit state (the widget and the uiTest gallery
     * use it; nothing here reads the mouse).
     *
     * @param position      knob position 0–1
     * @param tickPositions positions 0–1 of small tick marks under the track (may be empty)
     * @param valueWidth    width reserved for the value text (at least its own width)
     * @return whether the label or the value had to be shortened or hidden
     */
    public static boolean draw(GuiGraphics graphics, Font font, UiRect bounds, Component label,
                               Component value, int valueWidth, double position,
                               double[] tickPositions, boolean hovered, boolean enabled,
                               boolean focusRing) {
        if (bounds.isEmpty()) {
            return false;
        }
        Component safeLabel = label == null ? Component.empty() : label;
        Component safeValue = value == null ? Component.empty() : value;
        int labelWidth = font.width(safeLabel);
        int ownValueWidth = font.width(safeValue);
        Layout layout = layout(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(),
                labelWidth, Math.max(valueWidth, ownValueWidth));
        boolean hover = hovered && enabled;
        int l = bounds.left();
        int t = bounds.top();
        int r = bounds.right();
        int b = bounds.bottom();
        graphics.fill(l, t, r, b, !enabled ? TacticalBoardTheme.CARD_DISABLED
                : hover ? TacticalBoardTheme.CARD_HOVER : TacticalBoardTheme.CARD);
        BattleUiTheme.outline(graphics, l, t, r, b, !enabled ? TacticalBoardTheme.DISABLED_EDGE
                : hover ? TacticalBoardTheme.BORDER_DARK : TacticalBoardTheme.BORDER);
        boolean truncated = false;
        if (layout.labelShown()) {
            truncated = TextFit.draw(graphics, font, safeLabel, layout.labelX(), layout.textY(),
                    layout.labelRoom(), enabled ? TacticalBoardTheme.TEXT
                            : TacticalBoardTheme.DISABLED_TEXT, TextFit.Align.LEFT).truncated();
        } else if (labelWidth > 0) {
            truncated = true;
        }
        if (layout.valueShown()) {
            truncated |= TextFit.draw(graphics, font, safeValue, layout.valueX(), layout.textY(),
                    layout.valueRoom(), enabled ? TacticalBoardTheme.ADJUST
                            : TacticalBoardTheme.DISABLED_TEXT, TextFit.Align.RIGHT).truncated();
        } else if (ownValueWidth > 0) {
            truncated = true;
        }
        int trackY = layout.trackY();
        graphics.fill(layout.trackLeft(), trackY, layout.trackRight(), trackY + 2,
                TacticalBoardTheme.WELL);
        int knobX = layout.knobX(position);
        graphics.fill(layout.trackLeft(), trackY, knobX, trackY + 2,
                enabled ? TacticalBoardTheme.ADJUST : TacticalBoardTheme.FAINT);
        if (tickPositions != null) {
            for (double tick : tickPositions) {
                int tickX = layout.knobX(tick);
                graphics.fill(tickX, trackY + 3, tickX + 1, trackY + 5, TacticalBoardTheme.MUTED);
            }
        }
        graphics.fill(knobX - 2, trackY - 3, knobX + 3, trackY + 5, TacticalBoardTheme.FRAME);
        graphics.fill(knobX - 1, trackY - 2, knobX + 2, trackY + 4, !enabled
                ? TacticalBoardTheme.FAINT
                : hover ? TacticalBoardTheme.ADJUST_SOFT : TacticalBoardTheme.ADJUST);
        if (focusRing) {
            TacticalButtonStyle.focusRing(graphics, l, t, r, b);
        }
        return truncated;
    }

    private final Component label;
    private final DoubleFunction<Component> formatter;
    private final DoubleConsumer onChanged;
    private final double[] tickValues;
    private final boolean detentTicks;
    private final TruncationTooltip truncationTooltip = new TruncationTooltip();
    private TacticalSliderScale scale;
    private double current;
    private boolean touched;

    /**
     * Legacy constructor: continuous {@code minimum}–{@code maximum} snapped to {@code step}
     * (0 = no snapping), percent read-out.
     */
    TacticalBoardSlider(int x, int y, int width, int height, Component label,
                        double minimum, double maximum, double step,
                        double currentValue, DoubleConsumer onChanged) {
        this(x, y, width, height, label, TacticalSliderScale.linear(minimum, maximum, step),
                currentValue, PERCENT, onChanged, new double[0], false);
    }

    private TacticalBoardSlider(int x, int y, int width, int height, Component label,
                                TacticalSliderScale scale, double value,
                                DoubleFunction<Component> formatter, DoubleConsumer onChanged,
                                double[] tickValues, boolean detentTicks) {
        super(x, y, width, height, Component.empty(), 0.0D);
        this.label = label == null ? Component.empty() : label;
        this.formatter = formatter == null ? PERCENT : formatter;
        this.onChanged = onChanged == null ? ignored -> { } : onChanged;
        this.tickValues = tickValues == null ? new double[0] : tickValues.clone();
        this.detentTicks = detentTicks;
        this.scale = scale == null ? TacticalSliderScale.linear(0.0D, 1.0D, 0.0D) : scale;
        this.current = this.scale.snap(value);
        this.value = this.scale.toPosition(current);
        updateMessage();
    }

    /** Starts a slider; the default scale is 0–1 continuous with a percent read-out. */
    public static Builder builder(Component label, DoubleConsumer onChanged) {
        return new Builder(label, onChanged);
    }

    // ---- value ----------------------------------------------------------------------------------

    /** Current value (always valid for the scale: on the grid or on a detent). */
    public double selectedValue() {
        return current;
    }

    /** {@link #selectedValue()} rounded to a whole number (detent scales of rounds, counts). */
    public int selectedInt() {
        return (int) Math.round(current);
    }

    /** Sets the value (snapped) and reports it, as the screen's own reset buttons do. */
    public void setSelectedValue(double selected) {
        current = scale.snap(selected);
        value = scale.toPosition(current);
        updateMessage();
        onChanged.accept(current);
    }

    public TacticalSliderScale scale() {
        return scale;
    }

    /**
     * Replaces the scale (for example a new ammo maximum after a refresh) and keeps
     * {@code selected} as far as the new scale allows; nothing is reported.
     *
     * @return the value the slider now holds
     */
    public double setScale(TacticalSliderScale newScale, double selected) {
        if (newScale != null) {
            scale = newScale;
        }
        current = scale.snap(selected);
        value = scale.toPosition(current);
        updateMessage();
        return current;
    }

    /** Whether the player has moved the slider (mouse or keys) since it was created. */
    public boolean touched() {
        return touched;
    }

    /** Value text as drawn on the right. */
    public Component valueText() {
        return formatter.apply(current);
    }

    private boolean usable() {
        return active && visible && !scale.empty();
    }

    /** Applies a value chosen by the player; reports only real changes. */
    private void userSet(double next) {
        double snapped = scale.snap(next);
        if (Double.compare(snapped, current) == 0) {
            return;
        }
        touched = true;
        current = snapped;
        value = scale.toPosition(current);
        updateMessage();
        onChanged.accept(current);
    }

    @Override
    protected void updateMessage() {
        Component text = formatter.apply(current);
        if (label.getString().isEmpty()) {
            setMessage(text);
        } else {
            setMessage(label.copy().append("  ").append(text));
        }
    }

    /** Vanilla fallback ({@code setValue} from the base class): snap the position it set. */
    @Override
    protected void applyValue() {
        double next = scale.fromPosition(value);
        value = scale.toPosition(next);
        if (Double.compare(next, current) != 0) {
            current = next;
            updateMessage();
            onChanged.accept(current);
        }
    }

    // ---- input ----------------------------------------------------------------------------------

    @Override
    public void onClick(double mouseX, double mouseY) {
        setFromPointer(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        setFromPointer(mouseX);
    }

    private void setFromPointer(double mouseX) {
        if (!usable()) {
            return;
        }
        userSet(scale.fromPosition(currentLayout().positionAt(mouseX)));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!usable()) {
            return false;
        }
        int steps = keyStep(keyCode, modifiers);
        if (steps != 0) {
            userSet(scale.offset(current, steps));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END) {
            userSet(keyCode == GLFW.GLFW_KEY_HOME ? scale.min() : scale.max());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Pure: steps of an arrow key (Shift multiplies by {@value #COARSE_STEPS}), 0 otherwise. */
    static int keyStep(int keyCode, int modifiers) {
        int direction = keyCode == GLFW.GLFW_KEY_LEFT ? -1 : keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : 0;
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? direction * COARSE_STEPS : direction;
    }

    // ---- drawing --------------------------------------------------------------------------------

    /**
     * Layout of the widget as it is drawn. The value column is as wide as the widest read-out
     * of the range, so the track does not move while the value changes.
     */
    Layout currentLayout() {
        Font font = font();
        int labelWidth = font == null ? 0 : font.width(label);
        return layout(getX(), getY(), getX() + width, getY() + height, labelWidth,
                valueColumnWidth(font));
    }

    private int valueColumnWidth(Font font) {
        if (font == null) {
            return 0;
        }
        return Math.max(font.width(formatter.apply(current)), Math.max(
                font.width(formatter.apply(scale.min())), font.width(formatter.apply(scale.max()))));
    }

    private double[] tickPositions() {
        double[] positions = Arrays.stream(tickValues).map(scale::toPosition).toArray();
        if (!detentTicks || !scale.detented() || scale.detentCount() < 2) {
            return positions;
        }
        int count = scale.detentCount();
        double[] all = Arrays.copyOf(positions, positions.length + count);
        for (int index = 0; index < count; index++) {
            all[positions.length + index] = (double) index / (count - 1);
        }
        return all;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = font();
        if (font == null) {
            return;
        }
        boolean enabled = active && !scale.empty();
        boolean truncated = draw(graphics, font, UiRect.ofSize(getX(), getY(), width, height),
                label, formatter.apply(current), valueColumnWidth(font), value, tickPositions(),
                enabled && TacticalButtonStyle.hovered(this), enabled,
                TacticalButtonStyle.keyboardFocused(this));
        truncationTooltip.sync(this, getMessage(), truncated);
    }

    private static Font font() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.font;
    }

    // ---- builder --------------------------------------------------------------------------------

    /** Fluent construction of a {@link TacticalBoardSlider}. */
    public static final class Builder {
        private final Component label;
        private final DoubleConsumer onChanged;
        private int x;
        private int y;
        private int width = 100;
        private int height = 20;
        private TacticalSliderScale scale = TacticalSliderScale.linear(0.0D, 1.0D, 0.0D);
        private double value;
        private DoubleFunction<Component> formatter = PERCENT;
        private double[] ticks = new double[0];
        private boolean detentTicks;

        private Builder(Component label, DoubleConsumer onChanged) {
            this.label = label;
            this.onChanged = onChanged;
        }

        public Builder bounds(int newX, int newY, int newWidth, int newHeight) {
            this.x = newX;
            this.y = newY;
            this.width = Math.max(0, newWidth);
            this.height = Math.max(0, newHeight);
            return this;
        }

        public Builder scale(TacticalSliderScale newScale) {
            if (newScale != null) {
                this.scale = newScale;
            }
            return this;
        }

        /** Continuous range snapped to {@code step} (0 = no snapping). */
        public Builder range(double min, double max, double step) {
            return scale(TacticalSliderScale.linear(min, max, step));
        }

        /** Whole detents {@code first, first + step, …} ({@code count} of them). */
        public Builder detents(double first, double step, int count) {
            return scale(TacticalSliderScale.detents(first, step, count));
        }

        /** Initial value (snapped to the scale). */
        public Builder value(double initial) {
            this.value = initial;
            return this;
        }

        /** Value read-out ({@link #PERCENT} by default, see {@link #INTEGER}, {@link #translated}). */
        public Builder formatter(DoubleFunction<Component> newFormatter) {
            if (newFormatter != null) {
                this.formatter = newFormatter;
            }
            return this;
        }

        /** Tick marks under the track at these values (e.g. the 100% reference). */
        public Builder ticks(double... values) {
            this.ticks = values == null ? new double[0] : values.clone();
            return this;
        }

        /** Draws a tick at every detent of a detent scale. */
        public Builder detentTicks(boolean show) {
            this.detentTicks = show;
            return this;
        }

        public TacticalBoardSlider build() {
            return new TacticalBoardSlider(x, y, width, height, label, scale, value, formatter,
                    onChanged, ticks, detentTicks);
        }
    }
}
