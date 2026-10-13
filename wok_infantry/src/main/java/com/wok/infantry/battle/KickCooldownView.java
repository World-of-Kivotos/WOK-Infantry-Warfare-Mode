package com.wok.infantry.battle;

import java.util.Objects;

/**
 * The viewer was kicked from {@code squad} and may not create or rejoin it before
 * {@code expiresAtMillis} (server wall clock, the same clock as
 * {@link BattleSnapshot#serverTimeMillis()}). Only the viewer's own cooldowns are sent.
 */
public record KickCooldownView(SquadCallsign squad, long expiresAtMillis) {
    public KickCooldownView {
        Objects.requireNonNull(squad, "squad");
        if (expiresAtMillis < 0L) {
            throw new IllegalArgumentException("Kick cooldown expiry cannot be negative");
        }
    }

    /** Milliseconds left at {@code nowServerMillis}; 0 once expired. */
    public long remainingMillis(long nowServerMillis) {
        return Math.max(0L, expiresAtMillis - nowServerMillis);
    }

    /** Whole seconds left at {@code nowServerMillis}, rounded up; 0 once expired. */
    public int remainingSeconds(long nowServerMillis) {
        long millis = remainingMillis(nowServerMillis);
        return millis <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, (millis + 999L) / 1_000L);
    }
}
