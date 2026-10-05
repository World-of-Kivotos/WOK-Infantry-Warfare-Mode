package com.wok.infantry.client.hud;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.screen.UiRect;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Optional WOK步战附属-部位血量 bridge; the core never links against it. Called only from the
 * client render thread.
 *
 * <p>Up to 0.4.0 the core drew its stamina into the strip body health reserves under its figure
 * (the companion slot). Since 0.5.0-beta.1 the stamina bar sits above the hotbar and the strip
 * stays empty ({@link InfantryHudApi#usesBodyHealthCompanionSlot()}); the slot is still asked for
 * because it locates the figure. The squad roster must stay clear of the figure column above that
 * strip ({@link #hudRect}). Preferred source is a future
 * {@code com.wok.bodyhealth.api.BodyHealthHudApi.hudRect(int, int)} returning
 * {@code {left, top, width, height}} in GUI pixels (null while hidden). Until the add-on
 * provides it, the column is derived from the companion slot with the geometry of
 * {@code BodyHealthHudLayout} / {@code BodyHealthOverlay} as of 0.1.0-beta.10 (unchanged since
 * the companion slot API was added): the figure starts {@link #FIGURE_TOP_ABOVE_COMPANION}
 * pixels above the slot, its head label chip one pixel higher still, and no label leaves
 * {@code [2, width / 2 − 95)}. Keep {@link #columnAbove} in step with the add-on;
 * {@code BodyHealthHudBridgeTest} pins these numbers.
 */
final class BodyHealthHudBridge {
    private static final String BODY_HEALTH_MOD_ID = "wok_body_health";
    private static final String API_CLASS = "com.wok.bodyhealth.api.BodyHealthHudApi";
    /**
     * Companion slot top minus the figure top: {@code FIGURE_HEIGHT + BOTTOM_RESERVED
     * − COMPANION_HEIGHT − 2} = 48 + 25 − 11 − 2.
     */
    static final int FIGURE_TOP_ABOVE_COMPANION = 60;
    /** The head label chip starts one pixel above the figure (text row 0, chip top −1). */
    static final int HEAD_CHIP_OVERHANG = 1;
    /** The column keeps this far left of the hotbar's left edge (width / 2 − 91). */
    static final int HOTBAR_CLEARANCE = 4;

    private static boolean resolved;
    private static Method companionSlot;
    private static Method hudRect;

    /**
     * The add-on's companion slot {@code {left, top, width, height}} (it locates the figure), or
     * null without body health, while its HUD is hidden or when the lookup failed.
     */
    static int[] companionSlot(int screenWidth, int screenHeight) {
        Method method = resolve();
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(null, screenWidth, screenHeight);
            return result instanceof int[] slot && slot.length == 4 ? slot : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            WokInfantryMod.LOGGER.error("WOK Body Health HUD bridge failed; the squad roster no "
                    + "longer keeps clear of the body-health figure.", exception);
            companionSlot = null;
            hudRect = null;
            return null;
        }
    }

    /**
     * The body-health HUD (figure, part labels, total and companion strip) in GUI pixels while it
     * is drawn, otherwise null.
     *
     * @param companion this frame's {@link #companionSlot}, or null while the HUD is hidden
     */
    static UiRect hudRect(int screenWidth, int screenHeight, int[] companion) {
        if (companion == null) {
            return null;
        }
        Method method = hudRect;
        if (method != null) {
            try {
                Object result = method.invoke(null, screenWidth, screenHeight);
                return result instanceof int[] rect && rect.length == 4 && rect[2] > 0
                        && rect[3] > 0 ? UiRect.ofSize(rect[0], rect[1], rect[2], rect[3]) : null;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                WokInfantryMod.LOGGER.error("WOK Body Health HUD area lookup failed; the squad "
                        + "roster keeps clear of the figure by its known geometry.", exception);
                hudRect = null;
            }
        }
        return columnAbove(companion, screenWidth, screenHeight);
    }

    /**
     * The figure column of body health 0.1.0-beta.10 for a companion slot
     * {@code {left, top, width, height}}: from the head label chip down to the screen's bottom,
     * from the screen's left edge to 4px left of the hotbar (or the slot's right edge, if that
     * is further right).
     */
    static UiRect columnAbove(int[] companion, int screenWidth, int screenHeight) {
        if (companion == null || companion.length != 4) {
            return null;
        }
        int top = companion[1] - FIGURE_TOP_ABOVE_COMPANION - HEAD_CHIP_OVERHANG;
        int right = Math.max(companion[0] + companion[2],
                screenWidth / 2 - WokHudLayout.HOTBAR_HALF_WIDTH - HOTBAR_CLEARANCE);
        int bottom = Math.max(screenHeight, companion[1] + companion[3]);
        return right <= 0 || top >= bottom ? null : UiRect.of(0, top, right, bottom);
    }

    private static Method resolve() {
        if (!resolved) {
            resolved = true;
            if (ModList.get().isLoaded(BODY_HEALTH_MOD_ID)) {
                try {
                    Class<?> api = Class.forName(API_CLASS);
                    companionSlot = api.getMethod("companionSlot", int.class, int.class);
                    try {
                        hudRect = api.getMethod("hudRect", int.class, int.class);
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                        hudRect = null;
                    }
                } catch (ReflectiveOperationException | LinkageError exception) {
                    WokInfantryMod.LOGGER.info("Installed WOK Body Health has no HUD slot API; "
                            + "the squad roster cannot locate its figure.");
                }
            }
        }
        return companionSlot;
    }

    private BodyHealthHudBridge() {
    }
}
