package com.wok.infantry.client.screen;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;
import java.util.Optional;

/**
 * The 9×9 monochrome UI icons of the tactical-tablet style (preview {@code kit/icons.js}
 * {@code MONO_ICONS}), drawn from {@link TacticalTextures#UI_ICONS} and tinted at draw time:
 * keys use their text colour, rows {@link TacticalBoardTheme#LIGHT_MUTED}, empty states the
 * muted colour of their surface.
 *
 * <p>The constants are in {@code MONO_ICONS} order, which is also the order of the generated
 * sheet: icon {@code n} sits at column {@code n % 16}, row {@code n / 16} on a 10px pitch.
 * Append new icons at the end (and in the preview) so existing UVs never move.
 */
public enum TacticalIcon {
    CLOSE,
    BACK,
    NEXT,
    UP,
    DOWN,
    PLUS,
    MINUS,
    CHECK,
    WARN,
    LOCK,
    UNLOCK,
    GEAR,
    MAP,
    SQUAD,
    PERSON,
    LEADER,
    COMMANDER,
    FLAG,
    CROSSHAIR,
    RADIO,
    BOX,
    REFRESH,
    EYE,
    TRASH,
    COPY,
    PASTE,
    IMPORT,
    EXPORT,
    CLOCK,
    LINK,
    WRENCH,
    FILE,
    FOLDER,
    BULLET,
    TANK,
    PLANE,
    SATELLITE,
    LAYERS,
    PIN,
    CENTER,
    INFO,
    DOT,
    DEPLOY,
    HEAL,
    SHIELD;

    /** Width and height of every icon in GUI units. */
    public static final int SIZE = 9;
    /** Distance between two icons on the sheet (one transparent texel between them). */
    public static final int PITCH = 10;
    /** Icons per sheet row. */
    public static final int COLUMNS = 16;
    /** Horizontal room an icon takes in front of a label (icon plus a 2px gap). */
    public static final int ADVANCE = SIZE + 2;

    /** Preview name, e.g. {@code "check"}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Left texel of this icon on the sheet. */
    public int u() {
        return ordinal() % COLUMNS * PITCH;
    }

    /** Top texel of this icon on the sheet. */
    public int v() {
        return ordinal() / COLUMNS * PITCH;
    }

    public static Optional<TacticalIcon> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (TacticalIcon icon : values()) {
            if (icon.id().equals(id)) {
                return Optional.of(icon);
            }
        }
        return Optional.empty();
    }

    /** Draws the icon with its top-left corner at (x, y), tinted with {@code argb}. */
    public void draw(GuiGraphics graphics, int x, int y, int argb) {
        TacticalTextures.blitTinted(graphics, TacticalTextures.UI_ICONS, x, y, SIZE, SIZE,
                u(), v(), SIZE, SIZE, TacticalTextures.UI_ICONS_WIDTH,
                TacticalTextures.UI_ICONS_HEIGHT, argb);
    }

    /** Draws the icon centred in [left, right) × [top, bottom), rounding towards the top-left. */
    public void drawCentered(GuiGraphics graphics, int left, int top, int right, int bottom,
                             int argb) {
        draw(graphics, centeredStart(left, right), centeredStart(top, bottom), argb);
    }

    /** Null-safe form of {@link #draw(GuiGraphics, int, int, int)}. */
    public static void draw(GuiGraphics graphics, TacticalIcon icon, int x, int y, int argb) {
        if (icon != null) {
            icon.draw(graphics, x, y, argb);
        }
    }

    /** First coordinate of a 9px icon centred in [start, end) (floor, as the preview). */
    public static int centeredStart(int start, int end) {
        return start + Math.floorDiv(end - start - SIZE, 2);
    }
}
