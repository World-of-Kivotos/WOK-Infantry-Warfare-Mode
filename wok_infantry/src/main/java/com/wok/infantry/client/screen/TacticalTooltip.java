package com.wok.infantry.client.screen;

import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Tablet tooltip of the preview's {@code UI.tooltip}: dark plate, thin outline, orange top
 * edge, first line light and the rest muted, no text shadow, always kept inside the screen.
 * {@link TacticalScreen} draws every widget tooltip with it, so {@code setTooltip(...)} calls need
 * no change.
 */
public final class TacticalTooltip {
    /** Maximum text width of wrapped tooltip lines. */
    public static final int MAX_TEXT_WIDTH = 200;
    /** Z offset above items and modal layers, as vanilla tooltips. */
    static final float Z = 400.0F;
    private static final int LINE_HEIGHT = 10;
    private static final int EDGE = 2;

    private TacticalTooltip() {
    }

    /** Box of a tooltip on screen. */
    public record Placement(int left, int top, int width, int height) {
        public UiRect rect() {
            return UiRect.ofSize(left, top, width, height);
        }
    }

    /** Box size for text of {@code textWidth} and {@code lineCount} lines. */
    static int boxWidth(int textWidth) {
        return textWidth + 8;
    }

    static int boxHeight(int lineCount) {
        return lineCount * LINE_HEIGHT + 5;
    }

    /**
     * Pure placement next to the mouse: right of and slightly above the pointer, flipped to the
     * left when it would leave the screen, moved up at the bottom edge, clamped to the screen.
     */
    static Placement placeAtMouse(int mouseX, int mouseY, int boxWidth, int boxHeight,
                                  int screenWidth, int screenHeight) {
        int left = mouseX + 10;
        int top = mouseY - 4;
        if (left + boxWidth > screenWidth - EDGE) {
            left = mouseX - boxWidth - 6;
        }
        if (top + boxHeight > screenHeight - EDGE) {
            top = screenHeight - boxHeight - EDGE;
        }
        return clamp(left, top, boxWidth, boxHeight, screenWidth, screenHeight);
    }

    /** Pure placement for keyboard focus: below {@code anchor}, or above it when there is no room. */
    static Placement placeAtAnchor(UiRect anchor, int boxWidth, int boxHeight,
                                   int screenWidth, int screenHeight) {
        int left = anchor.left();
        int top = anchor.bottom() + 3;
        if (top + boxHeight > screenHeight - EDGE) {
            top = anchor.top() - boxHeight - 3;
        }
        return clamp(left, top, boxWidth, boxHeight, screenWidth, screenHeight);
    }

    private static Placement clamp(int left, int top, int boxWidth, int boxHeight,
                                   int screenWidth, int screenHeight) {
        int safeLeft = Math.max(EDGE, Math.min(left, screenWidth - boxWidth - EDGE));
        int safeTop = Math.max(EDGE, Math.min(top, screenHeight - boxHeight - EDGE));
        return new Placement(safeLeft, safeTop, boxWidth, boxHeight);
    }

    /** Wraps {@code text} for a tooltip no wider than the screen allows. */
    public static List<FormattedCharSequence> lines(Font font, Component text, int screenWidth) {
        int maxWidth = Math.max(40, Math.min(MAX_TEXT_WIDTH, screenWidth - 12));
        return font.split(text, maxWidth);
    }

    /** Draws {@code lines} next to the mouse. */
    public static void renderAtMouse(GuiGraphics graphics, Font font,
                                     List<FormattedCharSequence> lines, int mouseX, int mouseY,
                                     int screenWidth, int screenHeight) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        Placement placement = placeAtMouse(mouseX, mouseY, boxWidth(maxWidth(font, lines)),
                boxHeight(lines.size()), screenWidth, screenHeight);
        render(graphics, font, lines, placement);
    }

    /** Draws {@code lines} below (or above) a keyboard-focused control. */
    public static void renderAtAnchor(GuiGraphics graphics, Font font,
                                      List<FormattedCharSequence> lines, UiRect anchor,
                                      int screenWidth, int screenHeight) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        Placement placement = placeAtAnchor(anchor, boxWidth(maxWidth(font, lines)),
                boxHeight(lines.size()), screenWidth, screenHeight);
        render(graphics, font, lines, placement);
    }

    private static int maxWidth(Font font, List<FormattedCharSequence> lines) {
        int width = 0;
        for (FormattedCharSequence line : lines) {
            width = Math.max(width, font.width(line));
        }
        return width;
    }

    private static void render(GuiGraphics graphics, Font font, List<FormattedCharSequence> lines,
                               Placement placement) {
        int left = placement.left();
        int top = placement.top();
        int right = left + placement.width();
        int bottom = top + placement.height();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, Z);
        // Not solid: a tooltip floats above whatever it points at.
        UiLayoutProbe.begin(graphics, "tooltip", left, top, right, bottom, false);
        graphics.fill(left, top, right, bottom, TacticalBoardTheme.TOOLTIP_BG);
        BattleUiTheme.outline(graphics, left, top, right, bottom, TacticalBoardTheme.TOOLTIP_EDGE);
        graphics.fill(left, top, right, top + 1, TacticalBoardTheme.ACCENT_B);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), left + 4, top + 3 + index * LINE_HEIGHT,
                    index == 0 ? TacticalBoardTheme.LIGHT : TacticalBoardTheme.LIGHT_MUTED, false);
            UiLayoutProbe.rawText(graphics, font, lines.get(index), left + 4,
                    top + 3 + index * LINE_HEIGHT);
        }
        UiLayoutProbe.end(graphics);
        graphics.pose().popPose();
    }
}
