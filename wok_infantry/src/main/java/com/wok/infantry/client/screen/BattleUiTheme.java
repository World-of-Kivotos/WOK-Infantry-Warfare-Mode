package com.wok.infantry.client.screen;

import com.wok.infantry.client.ClientBattleState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class BattleUiTheme {
    public static final int BACKGROUND = 0xEE0B1016;
    public static final int PANEL = 0xE618212A;
    public static final int PANEL_ALT = 0xE6202C36;
    public static final int PANEL_HOVER = 0xEE2B3B48;
    public static final int BORDER = 0xFF4C5E6B;
    public static final int ACCENT = 0xFFF0A431;
    public static final int FRIENDLY = 0xFF62B5E5;
    public static final int FRIENDLY_DARK = 0xFF286784;
    public static final int TEXT = 0xFFF3F6F8;
    public static final int MUTED_TEXT = 0xFF9AA8B2;
    public static final int DANGER = 0xFFE15B4F;
    public static final int SUCCESS = 0xFF74C985;

    private BattleUiTheme() {
    }

    public static void panel(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, bottom, PANEL);
        outline(graphics, left, top, right, bottom, BORDER);
    }

    public static void outline(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    public static void drawCenteredText(GuiGraphics graphics, Font font,
                                        String text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2,
                y, color, false);
    }

    public static void drawCenteredText(GuiGraphics graphics, Font font,
                                        Component text, int centerX, int y, int color) {
        drawCenteredText(graphics, font, text.getVisualOrderText(),
                centerX, y, color);
    }

    public static void drawCenteredText(GuiGraphics graphics, Font font,
                                        FormattedCharSequence text,
                                        int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2,
                y, color, false);
    }

    public static void feedback(GuiGraphics graphics, Font font, int screenWidth, int y) {
        feedback(graphics, font, 0, screenWidth, y);
    }

    public static void feedback(GuiGraphics graphics, Font font,
                                int regionLeft, int regionRight, int y) {
        ClientBattleState.BattleFeedback feedback = ClientBattleState.feedback();
        if (feedback == null || feedback.message().isBlank()) {
            return;
        }
        int safeLeft = Math.max(0, Math.min(regionLeft, regionRight - 1));
        int safeRight = Math.max(safeLeft + 1, regionRight);
        int regionWidth = safeRight - safeLeft;
        String visibleMessage = font.plainSubstrByWidth(feedback.message(),
                Math.max(1, regionWidth - 24));
        int textWidth = font.width(visibleMessage);
        int center = safeLeft + regionWidth / 2;
        int left = Math.max(safeLeft + 6, center - textWidth / 2 - 6);
        int right = Math.min(safeRight - 6, center + (textWidth + 1) / 2 + 6);
        graphics.fill(left, y - 3, right, y + 11, 0xE610151B);
        outline(graphics, left, y - 3, right, y + 11,
                feedback.success() ? SUCCESS : DANGER);
        drawCenteredText(graphics, font, visibleMessage, center, y,
                feedback.success() ? SUCCESS : DANGER);
    }
}
