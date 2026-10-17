package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalSliderScaleTest {
    private static final double EPS = 1.0E-9D;

    @Test
    void linearScaleSnapsToTheGridAndKeepsTheLegacyResults() {
        TacticalSliderScale weapon = TacticalSliderScale.linear(0.25D, 2.0D, 0.05D);

        assertEquals(0.35D, weapon.snap(0.333D), EPS, "legacy rounding to the nearest 5%");
        assertEquals(0.3D, weapon.snap(0.3D), EPS, "no binary noise such as 0.30000000000000004");
        assertEquals(0.25D, weapon.snap(-4.0D), EPS);
        assertEquals(2.0D, weapon.snap(9.0D), EPS);
        assertEquals(0.25D, weapon.snap(Double.NaN), EPS, "NaN falls back to the minimum");
        assertEquals(0.0D, weapon.toPosition(0.25D), EPS);
        assertEquals(1.0D, weapon.toPosition(2.0D), EPS);
        assertEquals(2.0D, weapon.fromPosition(1.0D), EPS, "the end of the track is the maximum");
        assertEquals(0.25D, weapon.fromPosition(-1.0D), EPS);
    }

    @Test
    void maximumStaysReachableWhenItIsOffTheGrid() {
        TacticalSliderScale scale = TacticalSliderScale.linear(0.0D, 1.0D, 0.3D);

        assertEquals(1.0D, scale.snap(1.0D), EPS);
        assertEquals(1.0D, scale.snap(0.96D), EPS, "closer to max than to 0.9");
        assertEquals(0.9D, scale.snap(0.94D), EPS);
        assertEquals(1.0D, scale.fromPosition(1.0D), EPS);
        assertEquals(1.0D, scale.offset(0.9D, 1), EPS);
        assertEquals(0.9D, scale.offset(1.0D, -1), EPS, "back to the last grid line");
    }

    @Test
    void offsetMovesWholeGridStepsWithoutSkippingALine() {
        TacticalSliderScale scale = TacticalSliderScale.linear(0.75D, 1.75D, 0.05D);

        assertEquals(1.05D, scale.offset(1.0D, 1), EPS);
        assertEquals(1.25D, scale.offset(1.0D, 5), EPS);
        assertEquals(1.05D, scale.offset(1.03D, 1), EPS, "between lines: the next line up");
        assertEquals(1.0D, scale.offset(1.03D, -1), EPS, "between lines: the next line down");
        assertEquals(1.75D, scale.offset(1.7D, 10), EPS, "clamped");
        assertEquals(0.75D, scale.offset(0.8D, -10), EPS);
        TacticalSliderScale free = TacticalSliderScale.linear(0.0D, 200.0D, 0.0D);
        assertEquals(52.0D, free.offset(50.0D, 1), EPS, "no grid: one step is 1% of the range");
    }

    @Test
    void detentScaleSnapsEveryValueToAWholeDetent() {
        TacticalSliderScale rounds = TacticalSliderScale.roundDetents(30, 100);

        assertTrue(rounds.detented());
        assertEquals(3, rounds.detentCount(), "30, 60 and 90 rounds");
        assertEquals(30.0D, rounds.min(), EPS);
        assertEquals(90.0D, rounds.max(), EPS);
        assertEquals(60.0D, rounds.snap(70.0D), EPS);
        assertEquals(90.0D, rounds.snap(76.0D), EPS);
        assertEquals(30.0D, rounds.snap(1.0D), EPS);
        assertEquals(90.0D, rounds.snap(1000.0D), EPS);
        assertEquals(0.5D, rounds.toPosition(60.0D), EPS, "detents are evenly spaced");
        assertEquals(60.0D, rounds.fromPosition(0.49D), EPS);
        assertEquals(60.0D, rounds.fromPosition(0.74D), EPS);
        assertEquals(90.0D, rounds.fromPosition(0.76D), EPS);
        assertEquals(90.0D, rounds.offset(60.0D, 1), EPS, "one key press = one detent");
        assertEquals(90.0D, rounds.offset(60.0D, 5), EPS);
        assertEquals(30.0D, rounds.offset(60.0D, -1), EPS);
    }

    @Test
    void detentScaleWithoutAnyDetentIsEmpty() {
        TacticalSliderScale none = TacticalSliderScale.roundDetents(30, 20);

        assertTrue(none.empty());
        assertEquals(0, none.detentCount());
        assertEquals(0.0D, none.snap(25.0D), EPS);
        assertEquals(0.0D, none.offset(0.0D, 1), EPS);
        TacticalSliderScale single = TacticalSliderScale.detents(5.0D, 5.0D, 1);
        assertFalse(single.empty());
        assertEquals(5.0D, single.snap(100.0D), EPS);
        assertEquals(0.0D, single.toPosition(5.0D), EPS);
    }

    @Test
    void logarithmicMappingSpreadsRatiosEvenly() {
        TacticalSliderScale scale = TacticalSliderScale.mapped(0.25D, 16.0D, 0.0D,
                TacticalSliderScale.LOGARITHMIC);

        assertEquals(0.0D, scale.toPosition(0.25D), EPS);
        assertEquals(0.5D, scale.toPosition(2.0D), EPS, "geometric middle of 0.25 and 16");
        assertEquals(1.0D, scale.toPosition(16.0D), EPS);
        assertEquals(2.0D, scale.fromPosition(0.5D), 1.0E-6D);
        TacticalSliderScale invalid = TacticalSliderScale.mapped(0.0D, 10.0D, 0.0D,
                TacticalSliderScale.LOGARITHMIC);
        assertEquals(0.5D, invalid.toPosition(5.0D), EPS, "min 0 falls back to linear");
    }

    @Test
    void invertedOrNonFiniteBoundsNeverProduceInvalidValues() {
        TacticalSliderScale inverted = TacticalSliderScale.linear(5.0D, 1.0D, 1.0D);
        assertEquals(5.0D, inverted.max(), EPS, "max is lifted to min");
        assertEquals(5.0D, inverted.snap(3.0D), EPS);
        assertEquals(0.0D, inverted.toPosition(5.0D), EPS);

        TacticalSliderScale infinite = TacticalSliderScale.linear(Double.NaN,
                Double.POSITIVE_INFINITY, Double.NaN);
        assertEquals(0.0D, infinite.min(), EPS);
        assertEquals(0.0D, infinite.max(), EPS);
        assertEquals(0.0D, infinite.step(), EPS);
    }
}
