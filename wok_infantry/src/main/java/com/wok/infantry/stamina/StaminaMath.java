package com.wok.infantry.stamina;

/** Pure stamina math kept independent from Forge so balancing remains unit-testable. */
public final class StaminaMath {
    private static final float SWAY_START_RATIO = 0.35F;

    private StaminaMath() {
    }

    public static float drain(float current, float amount) {
        return clamp(current - Math.max(0.0F, amount), 0.0F, StaminaRules.MAX_STAMINA);
    }

    public static float recover(float current, float amount) {
        return clamp(current + Math.max(0.0F, amount), 0.0F, StaminaRules.MAX_STAMINA);
    }

    /**
     * Returns a normalized weapon-instability value. Arm fatigue is the primary source, while
     * depleted legs add a smaller post-sprint breathing penalty even when the arms are fresh.
     */
    public static float swayIntensity(float armStamina, float legStamina) {
        float armRatio = ratio(armStamina);
        float legRatio = ratio(legStamina);
        float armFatigue = belowThreshold(armRatio);
        float legFatigue = belowThreshold(legRatio) * 0.35F;
        return clamp(Math.max(armFatigue, legFatigue), 0.0F, 1.0F);
    }

    public static float ratio(float stamina) {
        return clamp(stamina / StaminaRules.MAX_STAMINA, 0.0F, 1.0F);
    }

    public static float clamp(float value, float minimum, float maximum) {
        if (!Float.isFinite(value)) {
            return maximum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float belowThreshold(float ratio) {
        if (ratio >= SWAY_START_RATIO) {
            return 0.0F;
        }
        float normalized = (SWAY_START_RATIO - ratio) / SWAY_START_RATIO;
        // Ease in gently near the threshold, then become severe near exhaustion.
        return normalized * normalized * (3.0F - 2.0F * normalized);
    }
}
