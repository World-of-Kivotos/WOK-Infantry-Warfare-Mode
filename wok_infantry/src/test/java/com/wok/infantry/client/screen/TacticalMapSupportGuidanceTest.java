package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure geometry behind the F-16C style inner designation zone on the tactical map. */
class TacticalMapSupportGuidanceTest {
    private static final double DEFAULT_ZOOM = 0.45D;
    private static final double MAX_ZOOM = 4.0D;

    @Test
    void innerZoneFollowsTheDrawnOuterAreaAndStaysInsideIt() {
        int outer = (int) Math.round(80.0D * DEFAULT_ZOOM);
        int inner = TacticalMapScreen.supportGuidanceRadiusPixels(64.0D, 80.0D, outer,
                DEFAULT_ZOOM);
        assertEquals(29, inner, "64m at the default zoom must match its true map scale");
        assertTrue(inner < outer, "the designation zone must stay inside the impact area");

        int clampedOuter = TacticalMapScreen.SUPPORT_RADIUS_MAX_PIXELS;
        int clampedInner = TacticalMapScreen.supportGuidanceRadiusPixels(64.0D, 80.0D,
                clampedOuter, MAX_ZOOM);
        assertEquals(154, clampedInner,
                "a clamped outer area must keep the 64/80 ratio instead of merging rings");
        assertTrue(clampedInner < clampedOuter);
    }

    @Test
    void innerZoneRejectsUnusableInputsAndNeverExceedsTheDrawLimit() {
        assertEquals(0, TacticalMapScreen.supportGuidanceRadiusPixels(0.0D, 80.0D, 36,
                DEFAULT_ZOOM));
        assertEquals(0, TacticalMapScreen.supportGuidanceRadiusPixels(Double.NaN, 80.0D, 36,
                DEFAULT_ZOOM));
        assertEquals(29, TacticalMapScreen.supportGuidanceRadiusPixels(64.0D, 0.0D, 3,
                DEFAULT_ZOOM), "a radius-less support falls back to the map zoom");
        assertEquals(0, TacticalMapScreen.supportGuidanceRadiusPixels(64.0D, 0.0D, 3,
                Double.NaN));
        assertEquals(TacticalMapScreen.SUPPORT_RADIUS_MAX_PIXELS,
                TacticalMapScreen.supportGuidanceRadiusPixels(400.0D, 80.0D,
                        TacticalMapScreen.SUPPORT_RADIUS_MAX_PIXELS, MAX_ZOOM));
    }

    @Test
    void separateZoneTagOnlyAppearsWhenItClearsTheTargetCrosshair() {
        // 320x240 at GUI scale 3: 10px tag (half 5), 1px casing half-width, 2px gap,
        // 4px crosshair arm plus 2px clearance.
        assertTrue(clearsTarget(36, 5, 1, 2, 6),
                "the default-zoom F-16C area keeps a separate zone tag");
        assertTrue(clearsTarget(4, 5, 1, 2, 6));
        assertFalse(clearsTarget(3, 5, 1, 2, 6),
                "the clamped minimum area must fold the zone label into its range text");
        // 960x720 at GUI scale 1: 26px tag (half 13), 2px casing half-width, 4px gap,
        // 9px crosshair arm plus 4px clearance.
        assertTrue(clearsTarget(36, 13, 2, 4, 13));
        assertTrue(clearsTarget(9, 13, 2, 4, 13));
        assertFalse(clearsTarget(8, 13, 2, 4, 13),
                "a small area must fold the zone label into its range text instead");
        assertFalse(TacticalMapScreen.supportGuidanceLabelClearsTarget(20, 13, 13));
    }

    @Test
    void separateZoneTagNeverCoversTheInnerRingOrTheOuterOutline() {
        // Reported case: 960x720, GUI scale 1, one wheel step out to zoom 0.36.
        int outer = (int) Math.round(80.0D * 0.36D);
        int inner = TacticalMapScreen.supportGuidanceRadiusPixels(64.0D, 80.0D, outer, 0.36D);
        assertEquals(29, outer);
        assertEquals(23, inner);
        int offset = TacticalMapScreen.supportGuidanceLabelOffset(outer, inner, 2, 4, 13);
        assertTrue(offset - 13 >= inner + 2 + 4, "tag top must clear the casing of the inner ring");
        assertTrue(offset - 13 >= outer + 4, "tag top must clear the outer outline");

        // Every drawable outer size at both acceptance resolutions, including an inner zone
        // registered larger than the support radius.
        int[][] scales = {{5, 1, 2}, {13, 2, 4}};
        for (int[] scale : scales) {
            int half = scale[0];
            int ringHalf = scale[1];
            int gap = scale[2];
            for (int area = 3; area <= TacticalMapScreen.SUPPORT_RADIUS_MAX_PIXELS; area++) {
                for (double guidance : new double[]{16.0D, 64.0D, 120.0D}) {
                    int ring = TacticalMapScreen.supportGuidanceRadiusPixels(guidance, 80.0D,
                            area, 1.0D);
                    int top = TacticalMapScreen.supportGuidanceLabelOffset(area, ring,
                            ringHalf, gap, half) - half;
                    String label = area + "px/" + guidance + "m/half " + half;
                    assertTrue(top >= area + gap, label);
                    assertTrue(top >= ring + ringHalf + gap, label);
                }
            }
        }
    }

    private static boolean clearsTarget(int outerPixels, int labelHalfHeight, int ringHalfWidth,
                                        int gap, int centerClearance) {
        int inner = TacticalMapScreen.supportGuidanceRadiusPixels(64.0D, 80.0D, outerPixels,
                DEFAULT_ZOOM);
        int offset = TacticalMapScreen.supportGuidanceLabelOffset(outerPixels, inner,
                ringHalfWidth, gap, labelHalfHeight);
        return TacticalMapScreen.supportGuidanceLabelClearsTarget(offset, labelHalfHeight,
                centerClearance);
    }

    @Test
    void dashedRingUsesABoundedDashCount() {
        assertEquals(0, TacticalMapScreen.supportGuidanceDashCount(
                TacticalMapScreen.SUPPORT_GUIDANCE_MIN_RING_PIXELS - 1, 5));
        assertEquals(TacticalMapScreen.SUPPORT_GUIDANCE_MIN_DASHES,
                TacticalMapScreen.supportGuidanceDashCount(
                        TacticalMapScreen.SUPPORT_GUIDANCE_MIN_RING_PIXELS, 5));
        assertEquals(36, TacticalMapScreen.supportGuidanceDashCount(29, 5));
        assertEquals(TacticalMapScreen.SUPPORT_GUIDANCE_MAX_DASHES,
                TacticalMapScreen.supportGuidanceDashCount(
                        TacticalMapScreen.SUPPORT_RADIUS_MAX_PIXELS, 1));
    }
}
