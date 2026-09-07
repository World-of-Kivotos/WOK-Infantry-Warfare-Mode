package com.wok.infantry.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;

/** Compact board-styled slider with a visible physical control knob. */
final class TacticalBoardSlider extends AbstractSliderButton {
    private final Component label;
    private final double minimum;
    private final double maximum;
    private final double step;
    private final DoubleConsumer onChanged;

    TacticalBoardSlider(int x, int y, int width, int height, Component label,
                        double minimum, double maximum, double step,
                        double currentValue, DoubleConsumer onChanged) {
        super(x, y, width, height, Component.empty(),
                normalized(currentValue, minimum, maximum));
        this.label = label;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = Math.max(0.0D, step);
        this.onChanged = onChanged;
        snapValue();
        updateMessage();
    }

    double selectedValue() {
        return minimum + value * (maximum - minimum);
    }

    void setSelectedValue(double selected) {
        value = normalized(selected, minimum, maximum);
        applyValue();
    }

    @Override
    protected void updateMessage() {
        int percent = (int) Math.round(selectedValue() * 100.0D);
        if (label.getString().isEmpty()) {
            setMessage(Component.literal(percent + "%"));
        } else {
            setMessage(label.copy().append("  " + percent + "%"));
        }
    }

    @Override
    protected void applyValue() {
        snapValue();
        updateMessage();
        onChanged.accept(selectedValue());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (active && step > 0.0D
                && (keyCode == GLFW.GLFW_KEY_LEFT
                || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            double direction = keyCode == GLFW.GLFW_KEY_LEFT ? -1.0D : 1.0D;
            value = normalized(selectedValue() + direction * step,
                    minimum, maximum);
            applyValue();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                             float partialTick) {
        int left = getX();
        int top = getY();
        int right = left + width;
        int bottom = top + height;
        int fill = active
                ? isHoveredOrFocused() ? TacticalBoardTheme.CARD_HOVER
                : TacticalBoardTheme.CARD
                : TacticalBoardTheme.CARD_DISABLED;
        graphics.fill(left, top, right, bottom, fill);
        BattleUiTheme.outline(graphics, left, top, right, bottom,
                isHoveredOrFocused() ? TacticalBoardTheme.SELECTED
                        : TacticalBoardTheme.BORDER);

        Font font = Minecraft.getInstance().font;
        String text = font.plainSubstrByWidth(getMessage().getString(),
                Math.max(1, width - 10));
        BattleUiTheme.drawCenteredText(graphics, font, text,
                left + width / 2, top + 2,
                active ? TacticalBoardTheme.TEXT : TacticalBoardTheme.MUTED_TEXT);

        // AbstractSliderButton maps mouse input across x + 4 .. right - 4.
        // Keep the custom track on the exact same range so both endpoints are reachable.
        int trackLeft = left + 4;
        int trackRight = right - 4;
        int trackY = bottom - 5;
        graphics.fill(trackLeft, trackY, trackRight, trackY + 2,
                TacticalBoardTheme.INSET);
        int knobX = trackLeft + (int) Math.round(value * (trackRight - trackLeft));
        graphics.fill(trackLeft, trackY, knobX + 1, trackY + 2,
                TacticalBoardTheme.SELECTED);
        graphics.fill(knobX - 2, trackY - 3, knobX + 3, trackY + 5,
                TacticalBoardTheme.DEVICE_FRAME);
        graphics.fill(knobX - 1, trackY - 2, knobX + 2, trackY + 4,
                isHoveredOrFocused() ? TacticalBoardTheme.ACCENT
                        : TacticalBoardTheme.SELECTED);
    }

    private void snapValue() {
        double selected = minimum + value * (maximum - minimum);
        if (step > 0.0D) {
            selected = minimum + Math.round((selected - minimum) / step) * step;
        }
        selected = Math.max(minimum, Math.min(maximum, selected));
        value = normalized(selected, minimum, maximum);
    }

    private static double normalized(double selected, double minimum, double maximum) {
        if (!Double.isFinite(selected) || maximum <= minimum) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D,
                (selected - minimum) / (maximum - minimum)));
    }
}
