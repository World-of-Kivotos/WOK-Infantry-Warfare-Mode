package com.wok.infantry.client.hud;

import com.wok.infantry.battle.CombatantStatus;
import com.wok.infantry.client.screen.UiRect;
import net.minecraft.world.entity.player.Player;

/**
 * Optional WOK步战附属-倒地 bridge (client only): where its downed panel sits while the viewer
 * is down, so the squad roster can end above it (the panel is drawn above all, the roster under
 * it). The core never links against the add-on: the viewer counts as down while it carries the
 * add-on's {@code wok_downed:downed} effect, looked up by id ({@link CombatantStatus#isDowned}),
 * which is also when the add-on draws its panel.
 *
 * <p>The rectangle mirrors {@code DownedOverlay} of 0.1.0-alpha.3 (lines 26–30): width
 * {@code min(260, max(190, w − 20))} centred, top {@code max(8, h − 76)}, 46 high. It is only
 * reserved for the roster: should a later version move the panel (for example into
 * {@link InfantryHudApi#CENTER_LOW}, which the roster keeps clear of as soon as an add-on asks
 * for it), the roster at worst gives up rows next to empty space while the viewer is down.
 * {@code DownedHudBridgeTest} pins these numbers.
 */
final class DownedHudBridge {
    static final int PANEL_HEIGHT = 46;
    /** Room kept under the panel: top = h − 46 − 30. */
    static final int PANEL_BOTTOM_INSET = 30;
    static final int PANEL_MIN_TOP = 8;
    static final int PANEL_MIN_WIDTH = 190;
    static final int PANEL_MAX_WIDTH = 260;
    static final int PANEL_MARGIN = 20;

    private DownedHudBridge() {
    }

    /** The add-on's panel (GUI pixels) while {@code viewer} is down, otherwise null. */
    static UiRect panelRect(Player viewer, int guiWidth, int guiHeight) {
        return viewer != null && CombatantStatus.isDowned(viewer)
                ? mirroredPanel(guiWidth, guiHeight) : null;
    }

    /** The alpha.3 downed panel (GUI pixels) on a {@code guiWidth}×{@code guiHeight} screen. */
    static UiRect mirroredPanel(int guiWidth, int guiHeight) {
        int width = Math.min(PANEL_MAX_WIDTH, Math.max(PANEL_MIN_WIDTH, guiWidth - PANEL_MARGIN));
        int left = (guiWidth - width) / 2;
        int top = Math.max(PANEL_MIN_TOP, guiHeight - PANEL_HEIGHT - PANEL_BOTTOM_INSET);
        return UiRect.of(left, top, left + width, top + PANEL_HEIGHT);
    }
}
