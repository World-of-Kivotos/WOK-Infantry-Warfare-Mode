package com.wok.infantry.client.screen;

import com.wok.infantry.client.ClientBattleState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Drawing helpers shared by the battle screens and HUD. All colours now live in
 * {@link TacticalBoardTheme}; the public constants below are kept, deprecated, until the HUD
 * overlays migrate to the shared tokens. The bright ones the HUD still reads are aliases of the
 * finalised bright tokens; the old dark-panel colours have no tablet equivalent, are no longer
 * used by the core and keep their old values only for source compatibility.
 */
public final class BattleUiTheme {
    /** @deprecated old dark-panel colour, unused; use {@link TacticalBoardTheme#HUD_PLATE_SOLID}. */
    @Deprecated
    public static final int BACKGROUND = 0xEE0B1016;
    /** @deprecated old dark-panel colour, unused; use {@link TacticalBoardTheme#HUD_PLATE_SOLID}. */
    @Deprecated
    public static final int PANEL = 0xE618212A;
    /** @deprecated old dark-panel colour, unused; use {@link TacticalBoardTheme#HUD_PLATE}. */
    @Deprecated
    public static final int PANEL_ALT = 0xE6202C36;
    /** @deprecated old dark-panel colour, unused; use {@link TacticalBoardTheme#ROW_HOVER}. */
    @Deprecated
    public static final int PANEL_HOVER = 0xEE2B3B48;
    /** @deprecated old dark-panel outline, unused; use {@link TacticalBoardTheme#HUD_EDGE}. */
    @Deprecated
    public static final int BORDER = 0xFF4C5E6B;
    /** @deprecated old dark own-side blue, unused; use {@link TacticalBoardTheme#SELECT}. */
    @Deprecated
    public static final int FRIENDLY_DARK = 0xFF286784;
    /** @deprecated use {@link TacticalBoardTheme#ACCENT_B}. */
    @Deprecated
    public static final int ACCENT = TacticalBoardTheme.ACCENT_B;
    /** @deprecated use {@link TacticalBoardTheme#HUD_FRIENDLY} or {@link TacticalBoardTheme#MAP_FRIENDLY}. */
    @Deprecated
    public static final int FRIENDLY = TacticalBoardTheme.HUD_FRIENDLY;
    /** @deprecated use {@link TacticalBoardTheme#LIGHT}. */
    @Deprecated
    public static final int TEXT = TacticalBoardTheme.LIGHT;
    /** @deprecated use {@link TacticalBoardTheme#LIGHT_MUTED}. */
    @Deprecated
    public static final int MUTED_TEXT = TacticalBoardTheme.LIGHT_MUTED;
    /** @deprecated use {@link TacticalBoardTheme#DANGER_B}. */
    @Deprecated
    public static final int DANGER = TacticalBoardTheme.DANGER_B;
    /** @deprecated use {@link TacticalBoardTheme#SUCCESS_B}. */
    @Deprecated
    public static final int SUCCESS = TacticalBoardTheme.SUCCESS_B;

    private BattleUiTheme() {
    }

    /** @deprecated old dark panel, unused by the core; HUD plates come with the HUD batch. */
    @Deprecated
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

    /** Battle feedback banner centred in the region; long messages end in "…". */
    public static void feedback(GuiGraphics graphics, Font font,
                                int regionLeft, int regionRight, int y) {
        ClientBattleState.BattleFeedback feedback = ClientBattleState.feedback();
        if (feedback == null || feedback.message().isBlank()) {
            return;
        }
        int safeLeft = Math.max(0, Math.min(regionLeft, regionRight - 1));
        int safeRight = Math.max(safeLeft + 1, regionRight);
        int regionWidth = safeRight - safeLeft;
        TextFit.Fitted visible = TextFit.fit(font, feedback.message(),
                Math.max(1, regionWidth - 24));
        int textWidth = visible.width();
        int center = safeLeft + regionWidth / 2;
        int left = Math.max(safeLeft + 6, center - textWidth / 2 - 6);
        int right = Math.min(safeRight - 6, center + (textWidth + 1) / 2 + 6);
        int color = feedback.success()
                ? TacticalBoardTheme.SUCCESS_B : TacticalBoardTheme.DANGER_B;
        graphics.fill(left, y - 3, right, y + 11, TacticalBoardTheme.FEEDBACK_BG);
        outline(graphics, left, y - 3, right, y + 11, color);
        drawCenteredText(graphics, font, visible.text(), center, y, color);
    }
}
