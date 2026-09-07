package com.wok.infantry.stamina;

/** Small wire-safe view of the authoritative local player's current reserves. */
public record StaminaSnapshot(float arms, float legs, boolean enabled) {
    public StaminaSnapshot {
        arms = StaminaMath.clamp(arms, 0.0F, StaminaRules.MAX_STAMINA);
        legs = StaminaMath.clamp(legs, 0.0F, StaminaRules.MAX_STAMINA);
    }

    public float armRatio() {
        return StaminaMath.ratio(arms);
    }

    public float legRatio() {
        return StaminaMath.ratio(legs);
    }
}
