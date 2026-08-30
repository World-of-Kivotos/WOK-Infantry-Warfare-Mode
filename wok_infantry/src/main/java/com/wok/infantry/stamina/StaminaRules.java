package com.wok.infantry.stamina;

/** Default server-authoritative tuning for the split arm/leg stamina model. */
public final class StaminaRules {
    public static final float MAX_STAMINA = 100.0F;
    public static final int ACTIVE_SYNC_INTERVAL_TICKS = 2;
    public static final int IDLE_SYNC_INTERVAL_TICKS = 20;

    private StaminaRules() {
    }
}
