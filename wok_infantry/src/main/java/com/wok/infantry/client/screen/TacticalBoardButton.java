package com.wok.infantry.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Map-local button skin that keeps the vanilla input, focus, tooltip and narration behavior. */
final class TacticalBoardButton extends Button {
    enum Kind {
        NAVIGATION,
        CONTROL,
        TOGGLE,
        TOOL,
        DANGER
    }

    private final Kind kind;
    private final boolean engaged;
    private final int accentColor;

    TacticalBoardButton(int x, int y, int width, int height, Component message,
                        OnPress onPress, Kind kind, boolean engaged, int accentColor) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.kind = kind;
        this.engaged = engaged;
        this.accentColor = accentColor;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                float partialTick) {
        int left = getX();
        int top = getY();
        int right = left + width;
        int bottom = top + height;
        int fillColor;
        if (engaged) {
            fillColor = isHoveredOrFocused()
                    ? TacticalBoardTheme.SELECTED_HOVER : TacticalBoardTheme.SELECTED;
        } else if (!active) {
            fillColor = TacticalBoardTheme.CARD_DISABLED;
        } else if (isHoveredOrFocused()) {
            fillColor = TacticalBoardTheme.CARD_HOVER;
        } else {
            fillColor = TacticalBoardTheme.CARD;
        }
        graphics.fill(left, top, right, bottom, fillColor);
        int borderColor = kind == Kind.DANGER
                ? TacticalBoardTheme.DANGER
                : engaged ? accentColor
                : isHoveredOrFocused() ? TacticalBoardTheme.SELECTED
                : TacticalBoardTheme.BORDER;
        BattleUiTheme.outline(graphics, left, top, right, bottom, borderColor);
        if (engaged) {
            graphics.fill(left + 1, top + 1, left + 4, bottom - 1, accentColor);
        } else if (kind == Kind.CONTROL) {
            graphics.fill(left + 2, bottom - 3, right - 2, bottom - 2,
                    TacticalBoardTheme.ACCENT);
        }

        Component message = getMessage();
        if (!message.getString().isEmpty()) {
            Font font = Minecraft.getInstance().font;
            int available = Math.max(1, width - (engaged ? 10 : 6));
            String text = font.plainSubstrByWidth(message.getString(), available);
            int textColor = engaged ? TacticalBoardTheme.LIGHT_TEXT
                    : active ? TacticalBoardTheme.TEXT : TacticalBoardTheme.MUTED_TEXT;
            BattleUiTheme.drawCenteredText(graphics, font, text,
                    left + width / 2,
                    top + Math.max(0, (height - font.lineHeight) / 2) + 1,
                    textColor);
        }
    }
}
