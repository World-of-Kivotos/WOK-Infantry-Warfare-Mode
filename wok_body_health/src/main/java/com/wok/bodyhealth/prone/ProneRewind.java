package com.wok.bodyhealth.prone;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Picks the snapshot TaCZ or SBW would rewind to, rule for rule. */
public final class ProneRewind {
    /** Same arithmetic as both gun mods: {@code floor(latency / 1000.0 * 20.0 + 0.5)}. */
    public static int rewindTicks(int latencyMs) {
        return Mth.floor(latencyMs / 1000.0D * 20.0D + 0.5D);
    }

    /**
     * Two-tick displacement as stored by TaCZ and SBW history: {@code pos - prev2}, falling back
     * to {@code prev1} and then to zero while the history is still filling. Both may be null.
     */
    public static Vec3 velocity2(Vec3 pos, Vec3 prev1, Vec3 prev2) {
        Vec3 base = prev2 != null ? prev2 : prev1 != null ? prev1 : pos;
        return pos.subtract(base);
    }

    /** @param index history index used, or -1 for the live sample */
    public record Choice(ProneSample sample, Vec3 velocity, int index) {
    }

    /**
     * @param h                    the target's history, may be null
     * @param shooterLatencyMs     the shooter's {@code ServerPlayer.latency}, or -1 when the shooter
     *                             is not a ServerPlayer (no rewind then)
     * @param targetIsServerPlayer TaCZ and SBW only rewind ServerPlayer targets
     */
    public static Choice select(ProneHistory h, ProneSample live, Vec3 liveVelocity,
                                int shooterLatencyMs, boolean targetIsServerPlayer, RewindPolicy p) {
        int index = index(h, shooterLatencyMs, targetIsServerPlayer, p);
        if (index < 0) {
            return new Choice(live, liveVelocity, -1);
        }
        ProneSample sample = h.get(index);
        return new Choice(sample, sample.velocity(), index);
    }

    /** History index {@link #select} picks, or -1 for the live sample; same parameters. */
    public static int index(ProneHistory h, int shooterLatencyMs, boolean targetIsServerPlayer, RewindPolicy p) {
        if (h != null && p.latencyCompensation() && targetIsServerPlayer && shooterLatencyMs >= 0) {
            int usable = Math.min(h.size(), p.maxSaveTicks());
            if (usable > 0) {
                return Mth.clamp(rewindTicks(shooterLatencyMs), 0, usable - 1);
            }
        }
        return -1;
    }

    private ProneRewind() {
    }
}
