package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalIcon;
import com.wok.infantry.stamina.StaminaRules;

import java.util.Locale;

/**
 * What the stamina bar shows (preview {@code surfaces/16-stamina.js}, {@code stamina-a4}):
 * the state of one frame and the colours, marks and fill spans derived from it. Pure, so the
 * state rules are unit-tested without a client.
 *
 * <p>State rules (the stamina rules themselves are unchanged): normal neutral gray-green
 * ({@link TacticalBoardTheme#NEUTRAL_B}; silhouette and percentage
 * {@link TacticalBoardTheme#LIGHT_MUTED}); below 50 (sway starts) orange; at or below 15 red,
 * whose accent, mark and percentage blink at 2 Hz. While sprint is locked (legs ran out, until
 * they are back at the resume threshold) the empty legs groove turns dark red (a red empty slot,
 * not a full red bar), its inner shadow darker, its tick bright, the boot becomes a red lock;
 * for one second after the unlock the boot is a green check and the legs' accent green. On a
 * jumpable mount the legs groove shows the jump charge (light, arrow); seated in any other
 * vehicle the legs groove, boot and percentage are gray (sitting drains nothing). The value used
 * a moment ago stays as a faded remnant (fill colour at 40 %) and a recovering pool's fill ends
 * in a 1px light head.
 */
public final class StaminaBarModel {
    /** Empty legs groove while sprint is locked: DANGER and FRAME half and half. */
    public static final int LOCK_TRACK = TacticalHud.mix(TacticalBoardTheme.DANGER,
            TacticalBoardTheme.FRAME, 0.5D);
    /** Inner shadow of the locked legs groove. */
    public static final int LOCK_SHADOW = TacticalHud.mix(TacticalBoardTheme.DANGER,
            TacticalBoardTheme.FRAME, 0.75D);
    /** Alpha of the just-used remnant (40 %). */
    public static final int GHOST_ALPHA = 0x66;
    /** Ticks per blink phase: 5 on, 5 off = 2 Hz. */
    public static final int BLINK_TICKS = 5;

    private StaminaBarModel() {
    }

    /** How the player sits this frame. */
    public enum Mount {
        NONE,
        /** In a vehicle that cannot jump (boat, minecart, SBW vehicle …): legs gray. */
        SEATED,
        /** On a jumpable mount (horse, donkey, camel): legs show the jump charge. */
        JUMP,
        /** On a jumpable mount whose jump is cooling down (camel dash): charge gray. */
        JUMP_COOLDOWN
    }

    /**
     * One pool: {@code value} 0–100, {@code ghost} the highest value of the last moment (equal to
     * {@code value} when nothing was just used), {@code rising} while it recovers.
     */
    public record Pool(float value, float ghost, boolean rising) {
        public Pool {
            value = clamp(value);
            ghost = Math.max(value, clamp(ghost));
        }

        public static Pool of(float value) {
            return new Pool(value, value, false);
        }
    }

    /**
     * One frame: both pools, sprint locked ({@code StaminaSnapshot.sprintBlocked}), just
     * unlocked (the check shows for a second), the mount and its jump charge (0–1).
     */
    public record State(Pool arms, Pool legs, boolean locked, boolean unlocked, Mount mount,
                        float jumpCharge) {
        public State {
            arms = arms == null ? Pool.of(StaminaRules.MAX_STAMINA) : arms;
            legs = legs == null ? Pool.of(StaminaRules.MAX_STAMINA) : legs;
            mount = mount == null ? Mount.NONE : mount;
            jumpCharge = Float.isFinite(jumpCharge) ? Math.max(0.0F, Math.min(1.0F, jumpCharge))
                    : 0.0F;
            unlocked = unlocked && !locked;
        }

        /** Both pools at {@code arms} / {@code legs}, nothing moving, on foot. */
        public static State of(float arms, float legs) {
            return new State(Pool.of(arms), Pool.of(legs), false, false, Mount.NONE, 0.0F);
        }
    }

    /**
     * Presentation of one pool.
     *
     * @param pool        the values the groove shows (the jump charge on a jumpable mount)
     * @param fill        fill colour
     * @param accent      the ear's 2px status accent
     * @param icon        silhouette (hand / boot) or state mark (lock, check, jump arrow)
     * @param iconColor   its tint
     * @param numberColor colour of the percentage
     * @param tick        the threshold tick is drawn (not for the jump charge or while seated)
     * @param tickColor   its colour (bright while locked)
     * @param track       empty groove colour
     * @param shadow      the groove's top row (inner shadow)
     * @param warn        0 normal, 1 below 50, 2 at or below 15 / locked
     */
    public record Tone(Pool pool, int fill, int accent, TacticalIcon icon, int iconColor,
                       int numberColor, boolean tick, int tickColor, int track, int shadow,
                       int warn) {
        /** The just-used remnant: the fill colour at 40 %. */
        public int ghostColor() {
            return TacticalHud.withAlpha(fill, GHOST_ALPHA);
        }

        /** The percentage, e.g. {@code 42%}. */
        public String percent() {
            return StaminaBarModel.percent(pool.value());
        }
    }

    /**
     * Fill spans of a groove {@code width} pixels wide, measured from its anchored end:
     * {@code fill} pixels of fill, the remnant up to {@code ghost}, and the light head on the
     * fill's last pixel while {@code head}.
     */
    public record Spans(int fill, int ghost, boolean head) {
    }

    public static Tone arms(State state, boolean blinkOn) {
        return normal(state.arms(), TacticalIcon.HAND, false, false, blinkOn);
    }

    public static Tone legs(State state, boolean blinkOn) {
        Pool legs = state.legs();
        switch (state.mount()) {
            case JUMP -> {
                int light = TacticalBoardTheme.LIGHT;
                return new Tone(Pool.of(state.jumpCharge() * StaminaRules.MAX_STAMINA), light,
                        light, TacticalIcon.JUMP, light, light, false, light,
                        TacticalBoardTheme.HUD_TRACK, TacticalBoardTheme.FRAME, 0);
            }
            case JUMP_COOLDOWN -> {
                int gray = TacticalBoardTheme.OFFLINE;
                return new Tone(Pool.of(state.jumpCharge() * StaminaRules.MAX_STAMINA), gray,
                        gray, TacticalIcon.JUMP, gray, gray, false, gray,
                        TacticalBoardTheme.HUD_TRACK, TacticalBoardTheme.FRAME, 0);
            }
            case SEATED -> {
                int gray = TacticalBoardTheme.OFFLINE;
                return new Tone(legs, gray, gray, TacticalIcon.BOOT, gray, gray, false, gray,
                        TacticalBoardTheme.HUD_TRACK, TacticalBoardTheme.FRAME, 0);
            }
            default -> {
                return normal(legs, TacticalIcon.BOOT, state.locked(), state.unlocked(), blinkOn);
            }
        }
    }

    private static Tone normal(Pool pool, TacticalIcon silhouette, boolean locked,
                               boolean unlocked, boolean blinkOn) {
        if (locked) {
            int red = TacticalBoardTheme.DANGER_B;
            int signal = blink(red, blinkOn);
            return new Tone(pool, red, signal, TacticalIcon.LOCK, signal, signal, true,
                    TacticalBoardTheme.LIGHT, LOCK_TRACK, LOCK_SHADOW, 2);
        }
        float value = pool.value();
        int color = TacticalHud.staminaColor(value);
        int warn = warnLevel(value);
        int signal = warn == 2 ? blink(color, blinkOn) : color;
        int quiet = warn > 0 ? signal : TacticalBoardTheme.LIGHT_MUTED;
        if (unlocked) {
            return new Tone(pool, color, TacticalBoardTheme.SUCCESS_B, TacticalIcon.CHECK,
                    TacticalBoardTheme.SUCCESS_B, quiet, true, TacticalBoardTheme.LIGHT_MUTED,
                    TacticalBoardTheme.HUD_TRACK, TacticalBoardTheme.FRAME, warn);
        }
        return new Tone(pool, color, warn > 0 ? signal : TacticalBoardTheme.NEUTRAL_B, silhouette,
                quiet, quiet, true, TacticalBoardTheme.LIGHT_MUTED, TacticalBoardTheme.HUD_TRACK,
                TacticalBoardTheme.FRAME, warn);
    }

    /** 0 normal, 1 below the sway threshold (50), 2 at or below 15 (NaN = 2, like the colour). */
    public static int warnLevel(float value) {
        if (!(value > TacticalHud.STAMINA_CRITICAL)) {
            return 2;
        }
        return value < StaminaRules.SWAY_START_STAMINA ? 1 : 0;
    }

    /** The dim half of the 2 Hz blink: the colour halfway to the frame. */
    public static int blink(int color, boolean on) {
        return on ? color : TacticalHud.mix(color, TacticalBoardTheme.FRAME, 0.5D);
    }

    /** Bright half of the 2 Hz blink for a client tick count. */
    public static boolean blinkOn(int tickCount) {
        return Math.floorMod(tickCount / BLINK_TICKS, 2) == 0;
    }

    /** Fill, remnant and head of a groove {@code width} pixels wide. */
    public static Spans spans(int width, Pool pool) {
        int fill = TacticalHud.fillWidth(width, pool.value() / StaminaRules.MAX_STAMINA);
        int ghost = Math.max(fill, TacticalHud.fillWidth(width,
                pool.ghost() / StaminaRules.MAX_STAMINA));
        return new Spans(fill, ghost, pool.rising() && fill > 1 && fill < width);
    }

    /** Offset of a threshold tick from the groove's anchored end: {@code round(width × ratio)}. */
    public static int tickOffset(int width, float ratio) {
        return Math.round(Math.max(0, width) * ratio);
    }

    /** {@code round(value)%}. */
    public static String percent(float value) {
        return String.format(Locale.ROOT, "%d%%", Math.round(clamp(value)));
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(StaminaRules.MAX_STAMINA, value));
    }
}
