package com.wok.infantry.ammo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LargeSupplyDetonationTimelineTest {
    @Test
    void raisesTheFireColumnBeforeThePrimaryBlast() {
        assertEquals(LargeSupplyDetonationTimeline.Phase.PREBLAST_FIRE_COLUMN,
                LargeSupplyDetonationTimeline.phaseAt(0));
        assertEquals(LargeSupplyDetonationTimeline.Phase.PREBLAST_FIRE_COLUMN,
                LargeSupplyDetonationTimeline.phaseAt(139));
        assertEquals(LargeSupplyDetonationTimeline.Phase.PRIMARY_BLAST,
                LargeSupplyDetonationTimeline.phaseAt(140));
    }

    @Test
    void fadesIntoAfterburnThenCompletesWithoutRepeatingTheBlast() {
        assertEquals(LargeSupplyDetonationTimeline.Phase.AFTERBURN,
                LargeSupplyDetonationTimeline.phaseAt(141));
        assertEquals(LargeSupplyDetonationTimeline.Phase.AFTERBURN,
                LargeSupplyDetonationTimeline.phaseAt(259));
        assertEquals(LargeSupplyDetonationTimeline.Phase.COMPLETE,
                LargeSupplyDetonationTimeline.phaseAt(260));
    }

    @Test
    void schedulesOnlyTheAuthoredPreBlastPopsAndAftershocks() {
        assertTrue(LargeSupplyDetonationTimeline.isPreBlastPop(14));
        assertTrue(LargeSupplyDetonationTimeline.isPreBlastPop(98));
        assertTrue(LargeSupplyDetonationTimeline.isPreBlastPop(105));
        assertTrue(LargeSupplyDetonationTimeline.isPreBlastPop(133));
        assertFalse(LargeSupplyDetonationTimeline.isPreBlastPop(13));
        assertFalse(LargeSupplyDetonationTimeline.isPreBlastPop(140));
        assertTrue(LargeSupplyDetonationTimeline.isAftershock(8));
        assertTrue(LargeSupplyDetonationTimeline.isAftershock(20));
        assertTrue(LargeSupplyDetonationTimeline.isAftershock(35));
        assertFalse(LargeSupplyDetonationTimeline.isAftershock(36));
    }
}
