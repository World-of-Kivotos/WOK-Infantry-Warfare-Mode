package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalTextures;
import com.wok.infantry.client.screen.TextFit;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * HUD-only drawing on top of the shared {@link TacticalHud} primitives: the core HUD's scale
 * pose, notices (toasts) and formation emblems on their light backing. Colours come only from
 * {@link TacticalBoardTheme}; text is never shadowed and is ellipsized, never hard-cut.
 */
public final class HudPaint {
    /** Notice text inset from the plate's left edge and top. */
    public static final int TOAST_TEXT_X = 6;
    public static final int TOAST_TEXT_Y = 2;
    /** Emblem edge length; formation emblems are 256×256 textures drawn scaled. */
    public static final int EMBLEM_SIZE = 16;
    private static final int EMBLEM_TEXTURE_SIZE = 256;

    private HudPaint() {
    }

    /** Starts drawing in core HUD layout pixels: scales the pose by {@code factor} (pair with {@link #end}). */
    public static void begin(GuiGraphics graphics, int factor) {
        graphics.pose().pushPose();
        if (factor > 1) {
            graphics.pose().scale(factor, factor, 1.0F);
        }
    }

    public static void end(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    /**
     * Opens the layout-probe box of one HUD part (pair with {@link UiLayoutProbe#end}): the uiTest
     * acceptance checks that its texts stay inside and that no two parts overlap. A no-op outside
     * the probe; a null rectangle opens an empty box so begin and end stay paired.
     */
    public static void probeBox(GuiGraphics graphics, String id, UiRect rect) {
        UiRect safe = rect == null ? UiRect.EMPTY : rect;
        UiLayoutProbe.begin(graphics, id, safe.left(), safe.top(), safe.right(), safe.bottom(),
                true);
    }

    /**
     * One-line notice (preview {@code HUDP.toast}): HUD plate with the tone's colour on the left
     * edge, the text 6px in; a success notice also writes its text in green.
     */
    public static void toast(GuiGraphics graphics, Font font, UiRect rect, Component text,
                             TacticalHud.Tone tone) {
        if (rect == null || rect.isEmpty() || text == null) {
            return;
        }
        int accent = TacticalHud.toneColor(tone);
        TacticalHud.plate(graphics, rect.left(), rect.top(), rect.right(), rect.bottom(),
                TacticalHud.Edge.LEFT, accent, false);
        int color = tone == TacticalHud.Tone.SUCCESS ? TacticalBoardTheme.SUCCESS_B
                : TacticalBoardTheme.LIGHT;
        TextFit.draw(graphics, font, text, rect.left() + TOAST_TEXT_X, rect.top() + TOAST_TEXT_Y,
                rect.width() - TOAST_TEXT_X - 3, color, TextFit.Align.LEFT);
    }

    /**
     * Formation emblem at (x, y), {@value #EMBLEM_SIZE}px, on a {@link TacticalBoardTheme#BADGE_BG}
     * backing one pixel larger on every side: the emblems are dark navy artwork that would vanish
     * on a dark HUD plate.
     */
    public static void emblem(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.fill(x - 1, y - 1, x + EMBLEM_SIZE + 1, y + EMBLEM_SIZE + 1,
                TacticalBoardTheme.BADGE_BG);
        if (texture == null) {
            return;
        }
        TacticalTextures.begin(graphics);
        graphics.blit(texture, x, y, EMBLEM_SIZE, EMBLEM_SIZE, 0.0F, 0.0F, EMBLEM_TEXTURE_SIZE,
                EMBLEM_TEXTURE_SIZE, EMBLEM_TEXTURE_SIZE, EMBLEM_TEXTURE_SIZE);
        TacticalTextures.end(graphics);
    }

    /** Emblem texture of a formation icon id, or null when it is blank or malformed. */
    public static ResourceLocation emblemTexture(String iconId) {
        return iconId == null || iconId.isBlank() ? null : ResourceLocation.tryParse(iconId);
    }

    /** Plain text fitted into {@code maxWidth} with "…", no shadow; returns the x after it. */
    public static int text(GuiGraphics graphics, Font font, String text, int x, int y,
                           int maxWidth, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return x;
        }
        return x + TextFit.draw(graphics, font, text, x, y, maxWidth, color, TextFit.Align.LEFT)
                .width();
    }
}
