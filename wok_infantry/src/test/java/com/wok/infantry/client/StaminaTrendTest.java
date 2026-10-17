package com.wok.infantry.client;

import com.wok.infantry.stamina.StaminaSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaminaTrendTest {
    private static StaminaSnapshot snapshot(float arms, float legs, boolean blocked) {
        return new StaminaSnapshot(arms, legs, true, blocked);
    }

    @Test
    void remnantIsTheHighestValueOfTheLastSixTenthsOfASecond() {
        StaminaTrend trend = new StaminaTrend();
        // sprinting: the server sends a snapshot every 2 ticks, 0.8 legs each
        for (int step = 0; step <= 10; step++) {
            trend.record(snapshot(100.0F, 72.0F - 0.8F * step, false), 1000L + 100L * step);
        }
        StaminaTrend.View view = trend.view(2000L);
        assertEquals(68.8F, view.legsGhost(), 0.001F, "the value 0.6 s ago (step 4)");
        assertEquals(100.0F, view.armsGhost(), 0.001F, "arms unchanged: no remnant");
        assertFalse(view.legsRising());
        // the sprint stops: the remnant fades out within 0.6 s
        assertEquals(64.8F, trend.view(2500L).legsGhost(), 0.001F);
        assertEquals(64.0F, trend.view(2601L).legsGhost(), 0.001F);
    }

    @Test
    void risingWhileThePoolRecovered() {
        StaminaTrend trend = new StaminaTrend();
        trend.record(snapshot(40.0F, 30.0F, false), 0L);
        trend.record(snapshot(40.68F, 30.0F, false), 100L);
        StaminaTrend.View view = trend.view(150L);
        assertTrue(view.armsRising());
        assertFalse(view.legsRising());
        assertFalse(trend.view(501L).armsRising(), "no rise in the last 0.4 s");
        trend.record(snapshot(40.0F, 30.0F, false), 200L);
        assertFalse(trend.view(250L).armsRising(), "a drop ends the recovery");
        trend.record(snapshot(40.0F, 30.005F, false), 300L);
        assertFalse(trend.view(350L).legsRising(), "noise is not a recovery");
    }

    @Test
    void unlockIsShownForOneSecond() {
        StaminaTrend trend = new StaminaTrend();
        trend.record(snapshot(80.0F, 0.0F, true), 0L);
        trend.record(snapshot(80.0F, 14.9F, true), 1000L);
        assertFalse(trend.view(1000L).unlocked());
        trend.record(snapshot(80.0F, 15.0F, false), 1100L);
        assertTrue(trend.view(1100L).unlocked());
        assertTrue(trend.view(2099L).unlocked());
        assertFalse(trend.view(2100L).unlocked());
        trend.record(snapshot(80.0F, 0.0F, true), 1200L);
        assertFalse(trend.view(1200L).unlocked(), "locked again: no check");
    }

    @Test
    void disabledStaminaOrClearForgetsTheHistory() {
        StaminaTrend trend = new StaminaTrend();
        trend.record(snapshot(100.0F, 100.0F, false), 0L);
        trend.record(new StaminaSnapshot(50.0F, 50.0F, false), 100L);
        StaminaTrend.View view = trend.view(150L);
        assertEquals(50.0F, view.legsGhost(), 0.001F, "no remnant from before stamina went off");
        trend.record(snapshot(100.0F, 100.0F, false), 200L);
        trend.record(snapshot(100.0F, 60.0F, false), 300L);
        trend.clear();
        assertEquals(0.0F, trend.view(300L).legsGhost(), 0.001F);
        trend.record(snapshot(100.0F, 60.0F, false), 400L);
        assertEquals(60.0F, trend.view(400L).legsGhost(), 0.001F);
    }
}
