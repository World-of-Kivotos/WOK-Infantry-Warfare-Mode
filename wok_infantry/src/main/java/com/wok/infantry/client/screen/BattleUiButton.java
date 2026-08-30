package com.wok.infantry.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Tactical-tablet button with a crisp, shadowless label. Input, focus, tooltip,
 * scrolling and narration behavior remain inherited from the vanilla button.
 */
final class BattleUiButton extends Button {
    private static final int TEXT_MARGIN = 2;
    enum Kind {
        NORMAL,
        CONTROL,
        DANGER,
        SUCCESS
    }

    private final boolean selected;
    private final Kind kind;
    private final int accentColor;

    private BattleUiButton(Button.Builder builder, boolean selected,
                           Kind kind, int accentColor) {
        super(builder);
        this.selected = selected;
        this.kind = kind;
        this.accentColor = accentColor;
    }

    public static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                float partialTick) {
        int left = getX();
        int top = getY();
        int right = left + width;
        int bottom = top + height;
        int fillColor;
        if (selected) {
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

        int borderColor = selected ? accentColor : !active
                ? TacticalBoardTheme.BORDER
                : switch (kind) {
                    case DANGER -> TacticalBoardTheme.DANGER;
                    case SUCCESS -> TacticalBoardTheme.SUCCESS;
                    default -> isHoveredOrFocused()
                            ? TacticalBoardTheme.SELECTED : TacticalBoardTheme.BORDER;
                };
        BattleUiTheme.outline(graphics, left, top, right, bottom, borderColor);
        if (selected) {
            graphics.fill(left + 1, top + 1, left + 4, bottom - 1, accentColor);
        } else if (kind == Kind.CONTROL && active) {
            graphics.fill(left + 2, bottom - 3, right - 2, bottom - 2,
                    TacticalBoardTheme.ACCENT);
        }

        int textColor = selected ? TacticalBoardTheme.LIGHT_TEXT
                : active ? TacticalBoardTheme.TEXT : TacticalBoardTheme.MUTED_TEXT;
        renderString(graphics, Minecraft.getInstance().font, textColor);
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        int left = getX() + TEXT_MARGIN;
        int right = getX() + getWidth() - TEXT_MARGIN;
        int textY = (getY() + getY() + getHeight() - 9) / 2 + 1;
        Component message = getMessage();
        int textWidth = font.width(message);
        int availableWidth = right - left;

        if (textWidth > availableWidth) {
            int overflow = textWidth - availableWidth;
            double seconds = Util.getMillis() / 1000.0D;
            double period = Math.max(overflow * 0.5D, 3.0D);
            double phase = Math.sin((Math.PI / 2.0D)
                    * Math.cos((Math.PI * 2.0D) * seconds / period)) / 2.0D + 0.5D;
            int scrollOffset = (int) Mth.lerp(phase, 0.0D, overflow);
            graphics.enableScissor(left, getY(), right, getY() + getHeight());
            graphics.drawString(font, message, left - scrollOffset, textY, color, false);
            graphics.disableScissor();
            return;
        }

        BattleUiTheme.drawCenteredText(graphics, font, message,
                (left + right) / 2, textY, color);
    }

    static final class Builder extends Button.Builder {
        private boolean selected;
        private Kind kind = Kind.NORMAL;
        private int accentColor = TacticalBoardTheme.SELECTED;

        private Builder(Component message, OnPress onPress) {
            super(message, onPress);
        }

        Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        Builder kind(Kind kind) {
            this.kind = kind == null ? Kind.NORMAL : kind;
            return this;
        }

        Builder accentColor(int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        @Override
        public BattleUiButton build() {
            return (BattleUiButton) super.build(builder -> new BattleUiButton(
                    builder, selected, kind, accentColor));
        }
    }
}
