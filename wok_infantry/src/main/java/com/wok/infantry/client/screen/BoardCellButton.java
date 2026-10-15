package com.wok.infantry.client.screen;

import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A clickable, keyboard-focusable cell whose content the screen draws itself (the squad strip of
 * the battle terminal's wide layout). It is a vanilla button underneath, so Enter/Space, focus,
 * narration and tooltips behave like every other key; a disabled cell must carry its reason as
 * the tooltip. The content callback gets the cell's bounds and its interaction state.
 */
final class BoardCellButton extends AbstractButton {
    /** Draws the cell content. */
    @FunctionalInterface
    interface Painter {
        void paint(GuiGraphics graphics, UiRect bounds, TacticalDraw.RowState state,
                   boolean keyboardFocus);
    }

    private final Painter painter;
    private final Runnable onPress;
    private final boolean selected;

    BoardCellButton(UiRect bounds, Component name, boolean selected, Painter painter,
                    Runnable onPress) {
        super(bounds.left(), bounds.top(), bounds.width(), bounds.height(), name);
        this.painter = painter;
        this.onPress = onPress;
        this.selected = selected;
    }

    /** Disables the cell; {@code reason} becomes its tooltip (null enables it again). */
    BoardCellButton withDisabledReason(Component reason) {
        active = reason == null;
        if (reason != null) {
            setTooltip(Tooltip.create(reason));
        }
        return this;
    }

    @Override
    public void onPress() {
        if (active && onPress != null) {
            onPress.run();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiRect bounds = UiRect.ofSize(getX(), getY(), width, height);
        boolean keyboard = TacticalButtonStyle.keyboardFocused(this);
        TacticalDraw.RowState state = new TacticalDraw.RowState(selected,
                isHovered() && active, !active, false);
        painter.paint(graphics, bounds, state, keyboard);
        if (keyboard) {
            TacticalDraw.focusRing(graphics, bounds);
        }
        UiLayoutProbe.widget(graphics, this, "cell", selected ? "SELECTED" : !active
                ? "DISABLED" : state.showsHover() ? "HOVER" : "NORMAL", false, keyboard);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
