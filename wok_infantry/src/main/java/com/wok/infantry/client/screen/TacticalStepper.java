package com.wok.infantry.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * {@code − value +} stepper of the tactical-tablet style (preview {@code UI.stepper}): two
 * adjustable (orange-underlined) icon keys around a dark value well with the value in
 * {@link TacticalBoardTheme#ACCENT_B}. A key turns disabled at its end of the range.
 *
 * <p>Values come from a {@link TacticalSliderScale} (continuous with a grid, or whole detents),
 * so a stepper and a slider over the same scale always agree. Input: click − / +, mouse wheel,
 * ←/→ (Shift: {@value TacticalBoardSlider#COARSE_STEPS} steps), the −/+ keys (one step),
 * Home/End for the ends. One focusable widget: Tab reaches it once, the arrows then adjust it.
 */
public final class TacticalStepper extends AbstractWidget {
    /** Widest − / + key; narrower steppers use their height (and never more than a third). */
    public static final int MAX_KEY = 16;

    /** Parts of a stepper. */
    public record Layout(UiRect minus, UiRect value, UiRect plus) {
    }

    /** Pure: keys {@code min(h, 16)} wide (at most a third of the width), 1px from the well. */
    public static Layout layout(UiRect bounds) {
        int key = Math.max(0, Math.min(Math.min(bounds.height(), MAX_KEY), bounds.width() / 3));
        UiRect minus = bounds.leftSlice(key);
        UiRect plus = bounds.rightSlice(key);
        UiRect well = new UiRect(minus.right() + 1, bounds.top(),
                Math.max(minus.right() + 1, plus.left() - 1), bounds.bottom());
        return new Layout(minus, well, plus);
    }

    /** Pure: -1 on the − key, +1 on the + key, 0 elsewhere (including the value well). */
    public static int hit(UiRect bounds, double x, double y) {
        Layout layout = layout(bounds);
        if (layout.minus().contains(x, y)) {
            return -1;
        }
        return layout.plus().contains(x, y) ? 1 : 0;
    }

    /**
     * Pure: steps of a key press, 0 for other keys. Arrows and the keypad −/+ take
     * {@value TacticalBoardSlider#COARSE_STEPS} steps with Shift. The main-row −/= keys always
     * take one step: on a US layout "+" itself is Shift + "=", so "+" and "−" stay symmetric.
     */
    static int keyStep(int keyCode, int modifiers) {
        boolean coarse = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        return switch (keyCode) {
            case GLFW.GLFW_KEY_MINUS -> -1;
            case GLFW.GLFW_KEY_EQUAL -> 1;
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_KP_SUBTRACT ->
                    coarse ? -TacticalBoardSlider.COARSE_STEPS : -1;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_KP_ADD ->
                    coarse ? TacticalBoardSlider.COARSE_STEPS : 1;
            default -> 0;
        };
    }

    /**
     * Draws a stepper with an explicit state (the widget and the uiTest gallery use it).
     *
     * @param hoveredPart -1 − key hovered, +1 + key hovered, 0 none
     * @return whether the value text had to be shortened
     */
    public static boolean draw(GuiGraphics graphics, Font font, UiRect bounds, Component value,
                               boolean canDecrease, boolean canIncrease, int hoveredPart,
                               boolean enabled, boolean focusRing) {
        if (bounds.isEmpty()) {
            return false;
        }
        Layout layout = layout(bounds);
        TacticalDraw.iconKey(graphics, font, layout.minus(), TacticalIcon.MINUS,
                enabled && canDecrease, hoveredPart < 0);
        TacticalDraw.iconKey(graphics, font, layout.plus(), TacticalIcon.PLUS,
                enabled && canIncrease, hoveredPart > 0);
        UiRect well = layout.value();
        boolean truncated = false;
        if (!well.isEmpty()) {
            graphics.fill(well.left(), well.top(), well.right(), well.bottom(),
                    TacticalBoardTheme.WELL);
            truncated = TextFit.draw(graphics, font, value == null ? Component.empty() : value,
                    well.left() + 2, well.top() + Math.floorDiv(well.height() - 8, 2),
                    Math.max(0, well.width() - 4),
                    enabled ? TacticalBoardTheme.ACCENT_B : TacticalBoardTheme.FAINT,
                    TextFit.Align.CENTER).truncated();
        }
        if (focusRing) {
            TacticalButtonStyle.focusRing(graphics, bounds.left(), bounds.top(), bounds.right(),
                    bounds.bottom());
        }
        return truncated;
    }

    private final Component label;
    private final DoubleFunction<Component> formatter;
    private final DoubleConsumer onChanged;
    private final TruncationTooltip truncationTooltip = new TruncationTooltip();
    private TacticalSliderScale scale;
    private double current;
    private boolean touched;

    /**
     * @param label     name for narration and the tooltip (not drawn)
     * @param scale     values the stepper can take
     * @param formatter value read-out ({@link TacticalBoardSlider#INTEGER} and friends)
     * @param onChanged receives every value the player picks
     */
    public TacticalStepper(int x, int y, int width, int height, Component label,
                           TacticalSliderScale scale, double value,
                           DoubleFunction<Component> formatter, DoubleConsumer onChanged) {
        super(x, y, width, height, Component.empty());
        this.label = label == null ? Component.empty() : label;
        this.scale = scale == null ? TacticalSliderScale.linear(0.0D, 1.0D, 0.0D) : scale;
        this.formatter = formatter == null ? TacticalBoardSlider.INTEGER : formatter;
        this.onChanged = onChanged == null ? ignored -> { } : onChanged;
        this.current = this.scale.snap(value);
        updateMessage();
    }

    // ---- value ----------------------------------------------------------------------------------

    public double selectedValue() {
        return current;
    }

    public int selectedInt() {
        return (int) Math.round(current);
    }

    /** Sets the value (snapped) and reports it. */
    public void setSelectedValue(double selected) {
        current = scale.snap(selected);
        updateMessage();
        onChanged.accept(current);
    }

    public TacticalSliderScale scale() {
        return scale;
    }

    /** Replaces the scale, keeping {@code selected} as far as it allows; nothing is reported. */
    public double setScale(TacticalSliderScale newScale, double selected) {
        if (newScale != null) {
            scale = newScale;
        }
        current = scale.snap(selected);
        updateMessage();
        return current;
    }

    /** Whether the player has changed the value since the stepper was created. */
    public boolean touched() {
        return touched;
    }

    public boolean canDecrease() {
        return !scale.empty() && scale.offset(current, -1) < current;
    }

    public boolean canIncrease() {
        return !scale.empty() && scale.offset(current, 1) > current;
    }

    /** Moves {@code steps} steps; returns whether the value changed. */
    public boolean step(int steps) {
        if (!usable() || steps == 0) {
            return false;
        }
        double next = scale.offset(current, steps);
        if (Double.compare(next, current) == 0) {
            return false;
        }
        touched = true;
        current = next;
        updateMessage();
        onChanged.accept(current);
        return true;
    }

    private boolean usable() {
        return active && visible && !scale.empty();
    }

    private void updateMessage() {
        Component text = formatter.apply(current);
        setMessage(label.getString().isEmpty() ? text : label.copy().append("  ").append(text));
    }

    // ---- input ----------------------------------------------------------------------------------

    @Override
    public void onClick(double mouseX, double mouseY) {
        int part = hit(bounds(), mouseX, mouseY);
        if (part != 0) {
            step(part);
        }
    }

    /** A press anywhere takes the focus; only a press that changes the value clicks. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !isValidClickButton(button) || !clicked(mouseX, mouseY)) {
            return false;
        }
        int part = hit(bounds(), mouseX, mouseY);
        if (part != 0 && step(part)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                playDownSound(minecraft.getSoundManager());
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!usable() || delta == 0.0D || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        step(delta > 0.0D ? 1 : -1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!usable()) {
            return false;
        }
        int steps = keyStep(keyCode, modifiers);
        if (steps != 0) {
            step(steps);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END) {
            double target = keyCode == GLFW.GLFW_KEY_HOME ? scale.min() : scale.max();
            double snapped = scale.snap(target);
            if (Double.compare(snapped, current) != 0) {
                touched = true;
                current = snapped;
                updateMessage();
                onChanged.accept(current);
            }
            return true;
        }
        return false;
    }

    // ---- drawing --------------------------------------------------------------------------------

    private UiRect bounds() {
        return UiRect.ofSize(getX(), getY(), width, height);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        boolean enabled = active && !scale.empty();
        int hovered = enabled && isHovered() ? hit(bounds(), mouseX, mouseY) : 0;
        boolean truncated = draw(graphics, minecraft.font, bounds(), formatter.apply(current),
                canDecrease(), canIncrease(), hovered, enabled,
                TacticalButtonStyle.keyboardFocused(this));
        truncationTooltip.sync(this, getMessage(), truncated);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE,
                Component.translatable("gui.narrate.slider", getMessage()));
        if (active) {
            output.add(NarratedElementType.USAGE,
                    Component.translatable("screen.wok_infantry.stepper.usage"));
        }
    }
}
