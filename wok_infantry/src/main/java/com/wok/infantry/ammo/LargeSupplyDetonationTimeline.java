package com.wok.infantry.ammo;

/** Pure timing contract for the large-station destruction sequence. */
public final class LargeSupplyDetonationTimeline {
    public static final int PREBLAST_FIRE_COLUMN_TICKS = 140;
    public static final int AFTERBURN_TICKS = 120;
    public static final int TOTAL_TICKS = PREBLAST_FIRE_COLUMN_TICKS + AFTERBURN_TICKS;

    private LargeSupplyDetonationTimeline() {
    }

    public static Phase phaseAt(int ageTicks) {
        if (ageTicks < PREBLAST_FIRE_COLUMN_TICKS) {
            return Phase.PREBLAST_FIRE_COLUMN;
        }
        if (ageTicks == PREBLAST_FIRE_COLUMN_TICKS) {
            return Phase.PRIMARY_BLAST;
        }
        if (ageTicks < TOTAL_TICKS) {
            return Phase.AFTERBURN;
        }
        return Phase.COMPLETE;
    }

    public static boolean isPreBlastPop(int ageTicks) {
        if (ageTicks <= 0 || ageTicks >= PREBLAST_FIRE_COLUMN_TICKS) {
            return false;
        }
        return ageTicks < 100 ? ageTicks % 14 == 0 : ageTicks % 7 == 0;
    }

    public static boolean isAftershock(int ticksAfterBlast) {
        return ticksAfterBlast == 8 || ticksAfterBlast == 20
                || ticksAfterBlast == 35;
    }

    public enum Phase {
        PREBLAST_FIRE_COLUMN,
        PRIMARY_BLAST,
        AFTERBURN,
        COMPLETE
    }
}
