package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.UiRect;

/**
 * Client-only, reflection-friendly HUD slots of WOK步战核心 for add-on HUDs, in the same style as
 * WOK步战附属-部位血量's {@code BodyHealthHudApi}. An add-on that finds the core asks for a slot
 * instead of hard-coding coordinates; without the core (or when this returns null) it keeps its
 * own placement, so every MOD stays installable on its own. Call only from client rendering
 * code; all values are GUI-scaled pixels, i.e. the overlay's own width and height space.
 *
 * <p>Slots:
 * <ul>
 *     <li>{@value #TOP_CENTER_NEXT}: the first free area under the core's top-centre plates
 *     (battle strip, notices, ballot), as wide as the strip column, down to the screen's middle;
 *     meant for WOK步战附属-占点's panel.</li>
 *     <li>{@value #CENTER_LOW}: a 200-wide (narrower on small screens) area from 16px under the
 *     screen's middle, clear of the action bar; meant for the downed panel.</li>
 *     <li>{@value #VITALS}: the bottom-left vitals area under the chat, left of the hotbar,
 *     off-hand slot and attack indicator.</li>
 * </ul>
 */
public final class InfantryHudApi {
    public static final String TOP_CENTER_NEXT = "top_center_next";
    public static final String CENTER_LOW = "center_low";
    public static final String VITALS = "vitals";

    private InfantryHudApi() {
    }

    /**
     * Returns {@code {left, top, width, height}} of slot {@code id} for this frame, or null for an
     * unknown id, without a local player, or while the core HUD is hidden (F1).
     */
    public static int[] slot(String id, int screenWidth, int screenHeight) {
        HudFrame frame = HudFrame.current(screenWidth, screenHeight);
        if (frame == null || frame.hidden()) {
            return null;
        }
        return slot(frame.layout(), id);
    }

    /**
     * GUI y of the first row under the core's top-centre plates, or 0 when none is shown, no
     * local player exists or the core HUD is hidden.
     */
    public static int topCenterBottom(int screenWidth, int screenHeight) {
        HudFrame frame = HudFrame.current(screenWidth, screenHeight);
        if (frame == null || frame.hidden()) {
            return 0;
        }
        return topCenterBottom(frame.layout());
    }

    static int[] slot(WokHudLayout.Layout layout, String id) {
        if (layout == null || id == null) {
            return null;
        }
        UiRect rect = switch (id) {
            case TOP_CENTER_NEXT -> layout.topCenterNext();
            case CENTER_LOW -> layout.centerLow();
            case VITALS -> layout.vitals();
            default -> null;
        };
        UiRect gui = layout.toGui(rect);
        return gui == null ? null : new int[]{gui.left(), gui.top(), gui.width(), gui.height()};
    }

    static int topCenterBottom(WokHudLayout.Layout layout) {
        return layout.topCenterBottom() <= 0 ? 0 : layout.topCenterBottom() * layout.factor();
    }
}
