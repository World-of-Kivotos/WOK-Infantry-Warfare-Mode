package com.wok.infantry.client.screen;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Gives a widget whose label was ellipsized a tooltip with the full text. Tooltips set by the
 * screen always win: the automatic one is only attached while the widget has none of its own,
 * and it is removed again once the label fits.
 */
final class TruncationTooltip {
    private Tooltip tooltip;
    private String tooltipText = "";

    void sync(AbstractWidget widget, Component fullText, boolean truncated) {
        Tooltip current = widget.getTooltip();
        boolean ours = current != null && current == tooltip;
        if (truncated && fullText != null && (current == null || ours)) {
            String text = fullText.getString();
            if (tooltip == null || !text.equals(tooltipText)) {
                tooltip = Tooltip.create(fullText);
                tooltipText = text;
            }
            if (current != tooltip) {
                widget.setTooltip(tooltip);
            }
        } else if (!truncated && ours) {
            widget.setTooltip(null);
        }
    }
}
