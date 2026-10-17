package com.wok.infantry.client.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TacticalMapSegmentClipperTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void preservesHorizontalDirectionWhileClippingBothEnds() {
        assertSegment(TacticalMapSegmentClipper.clip(
                20.0D, 5.0D, -10.0D, 5.0D,
                0.0D, 0.0D, 10.0D, 10.0D),
                10.0D, 5.0D, 0.0D, 5.0D);
    }

    @Test
    void clipsVerticalSegment() {
        assertSegment(TacticalMapSegmentClipper.clip(
                4.0D, -8.0D, 4.0D, 18.0D,
                0.0D, 0.0D, 10.0D, 10.0D),
                4.0D, 0.0D, 4.0D, 10.0D);
    }

    @Test
    void clipsDiagonalSegmentAtRectangleCorners() {
        assertSegment(TacticalMapSegmentClipper.clip(
                -5.0D, -5.0D, 15.0D, 15.0D,
                0.0D, 0.0D, 10.0D, 10.0D),
                0.0D, 0.0D, 10.0D, 10.0D);
    }

    @Test
    void keepsInsideSegmentAndCornerTangentStable() {
        assertSegment(TacticalMapSegmentClipper.clip(
                2.0D, 3.0D, 8.0D, 9.0D,
                0.0D, 0.0D, 10.0D, 10.0D),
                2.0D, 3.0D, 8.0D, 9.0D);
        assertSegment(TacticalMapSegmentClipper.clip(
                -2.0D, 2.0D, 2.0D, -2.0D,
                0.0D, 0.0D, 10.0D, 10.0D),
                0.0D, 0.0D, 0.0D, 0.0D);
    }

    @Test
    void rejectsSegmentsCompletelyOutsideViewport() {
        assertNull(TacticalMapSegmentClipper.clip(
                -5.0D, -2.0D, 15.0D, -2.0D,
                0.0D, 0.0D, 10.0D, 10.0D));
        assertNull(TacticalMapSegmentClipper.clip(
                -5.0D, 9.0D, 9.0D, 15.0D,
                0.0D, 0.0D, 10.0D, 10.0D));
    }

    @Test
    void retainsOnlyVisiblePartOfSegmentCrossingViewport() {
        assertSegment(TacticalMapSegmentClipper.clip(
                -100_000.0D, 3.0D, 100_000.0D, 7.0D,
                0.0D, 0.0D, 10.0D, 10.0D),
                0.0D, 5.0D, 10.0D, 5.0002D);
    }

    @Test
    void handlesDegenerateSegments() {
        assertSegment(TacticalMapSegmentClipper.clip(
                5.0D, 5.0D, 5.0D, 5.0D,
                0.0D, 0.0D, 10.0D, 10.0D),
                5.0D, 5.0D, 5.0D, 5.0D);
        assertNull(TacticalMapSegmentClipper.clip(
                11.0D, 5.0D, 11.0D, 5.0D,
                0.0D, 0.0D, 10.0D, 10.0D));
    }

    @Test
    void rejectsNonFiniteInputsAndInvalidRectangles() {
        assertNull(TacticalMapSegmentClipper.clip(
                Double.NaN, 0.0D, 1.0D, 1.0D,
                0.0D, 0.0D, 10.0D, 10.0D));
        assertNull(TacticalMapSegmentClipper.clip(
                0.0D, 0.0D, 1.0D, 1.0D,
                10.0D, 0.0D, 0.0D, 10.0D));
    }

    private static void assertSegment(TacticalMapSegmentClipper.ClippedSegment segment,
                                      double startX, double startY,
                                      double endX, double endY) {
        assertNotNull(segment);
        assertEquals(startX, segment.startX(), EPSILON);
        assertEquals(startY, segment.startY(), EPSILON);
        assertEquals(endX, segment.endX(), EPSILON);
        assertEquals(endY, segment.endY(), EPSILON);
    }
}
