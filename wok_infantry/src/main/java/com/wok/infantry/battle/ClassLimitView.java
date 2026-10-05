package com.wok.infantry.battle;

import java.util.Objects;

/**
 * One class quota of one squad as the faction sees it (battle protocol 20): the per-squad limit
 * from the shared formation and how many of that squad's members currently hold the class.
 * Display names are not repeated here; clients resolve them from the viewer's
 * {@link BattleSnapshot#classQuotas()} (see {@code SquadLabels.className}).
 */
public record ClassLimitView(String classId, int limit, int used) {
    public ClassLimitView {
        classId = Objects.requireNonNullElse(classId, BattleRules.DEFAULT_CLASS_ID);
        if (limit < 0 || used < 0) {
            throw new IllegalArgumentException("Class limit values cannot be negative");
        }
    }

    /** Free slots left in the squad, never negative. */
    public int remaining() {
        return Math.max(0, limit - used);
    }

    /** The formation gives this squad no slot for the class at all. */
    public boolean closed() {
        return limit <= 0;
    }

    /** Every configured slot is taken (a closed class is not "full"). */
    public boolean full() {
        return limit > 0 && used >= limit;
    }
}
