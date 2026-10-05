package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.config.InfantryClientConfig;

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
 *     meant for WOK步战附属-占点's own strip while the core does not show the point
 *     ({@link #rendersCapturePoints()} false). A capture HUD placed here must also be reported by
 *     {@code CaptureHudApi.panelRect}, or the core keeps moving its plates out of the way of where
 *     0.1.0-alpha.3 drew its panel (see {@code CaptureHudBridge}); reported, it also moves the
 *     vanilla boss bars below it.</li>
 *     <li>{@value #CENTER_LOW}: a 200-wide (narrower on small screens) area from 16px under the
 *     screen's middle, clear of the action bar; meant for the downed panel. Asking for it
 *     reserves it: from the next frame on, as long as it is asked for every frame, the squad
 *     roster (pushed down by the capture panel on low screens) ends above it. Ask only in the
 *     frames the panel is drawn.</li>
 *     <li>{@value #VITALS}: the bottom-left vitals area under the chat, left of the hotbar,
 *     off-hand slot, attack indicator and the stamina bar's left ear. Since 0.5.0-beta.1 the
 *     core's stamina is drawn above the hotbar, so the core itself leaves this area empty.</li>
 * </ul>
 *
 * <p>The core's stamina bar takes the vanilla experience row above the hotbar and no longer draws
 * into WOK步战附属-部位血量's companion strip ({@link #usesBodyHealthCompanionSlot()}).
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
        if (CENTER_LOW.equals(id)) {
            HudFrame.centerLowAsked();
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

    /**
     * Whether the core draws into the companion strip WOK步战附属-部位血量 reserves under its
     * figure ({@code BodyHealthHudApi.companionSlot}): {@code false} since the stamina bar moved
     * above the hotbar. A body-health release may call this reflectively and stop reserving the
     * strip when it returns false; when the method is missing (an older core that still draws
     * there) it should keep reserving it. Independent of whether stamina is enabled.
     */
    public static boolean usesBodyHealthCompanionSlot() {
        return false;
    }

    /**
     * Whether the core shows the WOK步战附属-占点 point the viewer stands in as the objective tile
     * of its battle strip (since 0.5.0-beta.2): true while the client config
     * {@code hud.showBattleStrip} is on and the core can read the add-on's
     * {@code CaptureHudApi.currentPoint()}. The add-on (0.1.0-alpha.4+) calls this reflectively
     * and draws nothing itself while it is true; when it is false, or the method is missing (an
     * older core), the add-on draws its own strip, in the {@link #TOP_CENTER_NEXT} slot when it
     * gets one. Safe to call every frame; it never computes a HUD frame.
     */
    public static boolean rendersCapturePoints() {
        return rendersCapturePoints(InfantryClientConfig.showBattleStrip(),
                CaptureHudBridge.readsObjective());
    }

    static boolean rendersCapturePoints(boolean stripOn, boolean readsObjective) {
        return stripOn && readsObjective;
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
