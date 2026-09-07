package com.wok.infantry.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Cold-gray physical command-board palette, isolated from the other battle screens. */
final class TacticalBoardTheme {
    static final int WORLD_SHADE = 0xF20A0E11;
    static final int DEVICE_SHADOW = 0xD0000000;
    static final int DEVICE_FRAME = 0xFF14191B;
    static final int DEVICE_MID = 0xFF313A3B;
    static final int DEVICE_EDGE = 0xFF77817F;
    static final int BOARD = 0xFFC0C9C6;
    static final int BOARD_ALT = 0xFFAAB6B3;
    static final int CARD = 0xFFD7DDDA;
    static final int CARD_HOVER = 0xFFE5EAE7;
    static final int CARD_DISABLED = 0xFF98A3A1;
    static final int INSET = 0xFF7F8D8B;
    static final int BORDER = 0xFF52605E;
    static final int BORDER_BRIGHT = 0xFFEBF0ED;
    static final int TEXT = 0xFF202C30;
    static final int MUTED_TEXT = 0xFF5E6D70;
    static final int LIGHT_TEXT = 0xFFF3F6F4;
    static final int SELECTED = 0xFF2E679C;
    static final int SELECTED_HOVER = 0xFF4381B9;
    static final int FRIENDLY = 0xFF2F75B5;
    static final int ACCENT = 0xFFB57D2C;
    static final int DANGER = 0xFFB84C45;
    static final int SUCCESS = 0xFF4F7C66;
    static final int MAP_WASH = 0x2495A7A2;
    static final int GRID_MINOR = 0x30405050;
    static final int GRID_MAJOR = 0x70404A49;

    private TacticalBoardTheme() {
    }

    static void raisedPanel(GuiGraphics graphics, int left, int top,
                            int right, int bottom, int fill) {
        graphics.fill(left + 2, top + 2, right + 2, bottom + 2, 0x60000000);
        graphics.fill(left, top, right, bottom, fill);
        BattleUiTheme.outline(graphics, left, top, right, bottom, BORDER);
        graphics.fill(left + 1, top + 1, right - 1, top + 2, BORDER_BRIGHT);
        graphics.fill(left + 1, top + 1, left + 2, bottom - 1, BORDER_BRIGHT);
    }

    static void insetPanel(GuiGraphics graphics, int left, int top,
                           int right, int bottom, int fill) {
        graphics.fill(left, top, right, bottom, fill);
        BattleUiTheme.outline(graphics, left, top, right, bottom, BORDER);
        graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, BORDER_BRIGHT);
        graphics.fill(right - 2, top + 1, right - 1, bottom - 1, BORDER_BRIGHT);
    }

    static void sectionHeader(GuiGraphics graphics, Font font, String text,
                              int left, int top, int right, int accent) {
        sectionHeader(graphics, font, Component.literal(text), left, top, right, accent);
    }

    static void sectionHeader(GuiGraphics graphics, Font font, Component text,
                              int left, int top, int right, int accent) {
        graphics.fill(left, top, right, top + 14, INSET);
        graphics.fill(left, top, left + 4, top + 14, accent);
        graphics.fill(left + 4, top + 13, right, top + 14, BORDER);
        graphics.drawString(font, text, left + 8, top + 3, LIGHT_TEXT, false);
    }

    static void cornerBrackets(GuiGraphics graphics, int left, int top,
                               int right, int bottom, int color) {
        int arm = Math.max(4, Math.min(14, Math.min(right - left, bottom - top) / 8));
        graphics.fill(left, top, left + arm, top + 2, color);
        graphics.fill(left, top, left + 2, top + arm, color);
        graphics.fill(right - arm, top, right, top + 2, color);
        graphics.fill(right - 2, top, right, top + arm, color);
        graphics.fill(left, bottom - 2, left + arm, bottom, color);
        graphics.fill(left, bottom - arm, left + 2, bottom, color);
        graphics.fill(right - arm, bottom - 2, right, bottom, color);
        graphics.fill(right - 2, bottom - arm, right, bottom, color);
    }

    static void rivet(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xFF080B0C);
        graphics.fill(x, y, x + 1, y + 1, DEVICE_EDGE);
    }
}
