package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiRectTest {
    @Test
    void colsMixFixedAndWeightedCellsAndTheLastCellTakesTheRemainder() {
        UiRect row = UiRect.of(10, 0, 311, 20);

        List<UiRect> cells = row.cols(4, UiRect.Size.px(100), UiRect.Size.STAR, UiRect.Size.px(60));

        assertEquals(UiRect.of(10, 0, 110, 20), cells.get(0));
        // free = 301 - 160 - 8 = 133 for the single star
        assertEquals(UiRect.of(114, 0, 247, 20), cells.get(1));
        // the last cell runs to the right edge even though it asked for 60
        assertEquals(UiRect.of(251, 0, 311, 20), cells.get(2));
    }

    @Test
    void weightedStarsSplitFreeSpaceLikeThePreview() {
        List<UiRect> cells = UiRect.of(0, 0, 100, 10).cols("*,2*", 2);

        // free = 98; floor(98 * 1/3) = 32; the last cell takes the rest
        assertEquals(UiRect.of(0, 0, 32, 10), cells.get(0));
        assertEquals(UiRect.of(34, 0, 100, 10), cells.get(1));
    }

    @Test
    void rowsSplitVertically() {
        List<UiRect> rows = UiRect.of(0, 20, 50, 120).rows("14,*,14", 3);

        assertEquals(UiRect.of(0, 20, 50, 34), rows.get(0));
        assertEquals(UiRect.of(0, 37, 50, 103), rows.get(1));
        assertEquals(UiRect.of(0, 106, 50, 120), rows.get(2));
    }

    @Test
    void sizeParsingAcceptsThePreviewNotation() {
        assertEquals(UiRect.Size.px(120), UiRect.Size.parse("120"));
        assertEquals(UiRect.Size.STAR, UiRect.Size.parse("*"));
        assertEquals(UiRect.Size.star(2.0D), UiRect.Size.parse("2*"));
        assertThrows(IllegalArgumentException.class, () -> UiRect.Size.parse(" "));
    }

    @Test
    void insetSlicesAndIntersections() {
        UiRect rect = UiRect.of(10, 10, 50, 40);

        assertEquals(UiRect.of(12, 13, 48, 37), rect.inset(2, 3));
        assertEquals(UiRect.of(10, 10, 50, 14), rect.topSlice(4));
        assertEquals(UiRect.of(10, 36, 50, 40), rect.bottomSlice(4));
        assertEquals(UiRect.of(45, 10, 50, 40), rect.rightSlice(5));
        assertTrue(rect.contains(10, 10));
        assertFalse(rect.contains(50, 39), "right edge is exclusive");
        assertTrue(rect.intersects(UiRect.of(49, 39, 60, 60)));
        assertFalse(rect.intersects(UiRect.of(50, 10, 60, 40)), "touching edges do not overlap");
        assertEquals(UiRect.EMPTY, rect.intersection(UiRect.of(60, 60, 70, 70)));
        assertEquals(0, UiRect.of(10, 10, 5, 5).width(), "inverted rectangles have no size");
    }
}
